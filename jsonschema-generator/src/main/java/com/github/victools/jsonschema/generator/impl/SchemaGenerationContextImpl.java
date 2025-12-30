/*
 * Copyright 2019 VicTools.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.TypeScope;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

/**
 * Generation context in which to collect definitions of traversed types and remember where they are being referenced.
 */
public class SchemaGenerationContextImpl implements SchemaGenerationContext {

    private static final Logger logger = LoggerFactory.getLogger(SchemaGenerationContextImpl.class);

    private final SchemaGeneratorConfig generatorConfig;
    private final TypeContext typeContext;
    private final Map<DefinitionKey, ObjectNode> definitions = new LinkedHashMap<>();
    private final Map<DefinitionKey, List<ObjectNode>> references = new HashMap<>();
    private final Map<DefinitionKey, List<ObjectNode>> nullableReferences = new HashMap<>();
    private final Set<DefinitionKey> neverInlinedDefinitions  = new HashSet<>();

    /**
     * Constructor initialising type resolution context.
     *
     * @param generatorConfig applicable configuration(s)
     * @param typeContext type resolution/introspection context to be used
     */
    public SchemaGenerationContextImpl(SchemaGeneratorConfig generatorConfig, TypeContext typeContext) {
        this.generatorConfig = generatorConfig;
        this.typeContext = typeContext;
    }

    @Override
    public SchemaGeneratorConfig getGeneratorConfig() {
        return this.generatorConfig;
    }

    @Override
    public TypeContext getTypeContext() {
        return this.typeContext;
    }

    /**
     * Parse the given (possibly generic) type and populate this context. This is intended to be used only once, for the schema's main target type.
     *
     * @param type (possibly generic) type to analyse and populate this context with
     * @return definition key identifying the given entry point
     */
    public DefinitionKey parseType(ResolvedType type) {
        this.traverseGenericType(type, null);
        return new DefinitionKey(type, null);
    }

    /**
     * Add the given type's definition to this context.
     *
     * @param javaType type to which the definition belongs
     * @param definitionNode definition to remember
     * @param ignoredDefinitionProvider first custom definition provider that was ignored when creating the definition (is null in most cases)
     * @return this context (for chaining)
     */
    SchemaGenerationContextImpl putDefinition(ResolvedType javaType, ObjectNode definitionNode,
            CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        this.definitions.put(new DefinitionKey(javaType, ignoredDefinitionProvider), definitionNode);
        return this;
    }

    /**
     * Based on the given custom definition for the given type, potentially mark it as never to be inlined, i.e., that it should always be included in
     * the {@link SchemaKeyword#TAG_DEFINITIONS}.
     *
     * @param customDefinition custom definition to potentially mark as never to be inlined
     * @param javaType type to which the definition belongs
     * @param ignoredDefinitionProvider first custom definition provider that was ignored when creating the definition (is null in most cases)
     * @return this context (for chaining)
     *
     * @since 4.27.0
     */
    SchemaGenerationContextImpl markDefinitionAsNeverInlinedIfRequired(CustomDefinition customDefinition, ResolvedType javaType,
            CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        if (customDefinition.shouldAlwaysProduceDefinition()) {
            this.neverInlinedDefinitions.add(new DefinitionKey(javaType, ignoredDefinitionProvider));
        }
        return this;
    }

    /**
     * Whether this context (already) contains a definition for the specified type, considering custom definition providers after the specified one.
     *
     * @param javaType type to check for
     * @param ignoredDefinitionProvider first custom definition provider that was ignored when creating the definition (is null in most cases)
     * @return whether a definition for the given type is already present
     */
    public boolean containsDefinition(ResolvedType javaType, CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        return this.definitions.containsKey(new DefinitionKey(javaType, ignoredDefinitionProvider));
    }

    /**
     * Retrieve the previously added definition for the specified type.
     *
     * @param key definition key to look-up associated definition for
     * @return JSON schema definition (or null if none is present)
     * @see #putDefinition(ResolvedType, ObjectNode, CustomDefinitionProviderV2)
     */
    public ObjectNode getDefinition(DefinitionKey key) {
        return this.definitions.get(key);
    }

    /**
     * Retrieve the set of all types for which a definition has been remembered in this context.
     *
     * @return types for which a definition is present
     */
    public Set<DefinitionKey> getDefinedTypes() {
        return Collections.unmodifiableSet(this.definitions.keySet());
    }

    /**
     * Remember for the specified type that the given node is supposed to either include or reference the type's associated schema.
     *
     * @param javaType type for which to remember a reference
     * @param referencingNode node that should (later) include either the type's respective inline definition or a "$ref" to the definition
     * @param ignoredDefinitionProvider first custom definition provider that was ignored when creating the definition (is null in most cases)
     * @param isNullable whether the reference may be null
     * @return this context (for chaining)
     */
    public SchemaGenerationContextImpl addReference(ResolvedType javaType, ObjectNode referencingNode,
            CustomDefinitionProviderV2 ignoredDefinitionProvider, boolean isNullable) {
        if (referencingNode == null) {
            // referencingNode should only be null for the main class for which the schema is being generated
            return this;
        }
        Map<DefinitionKey, List<ObjectNode>> targetMap = isNullable ? this.nullableReferences : this.references;
        DefinitionKey key = new DefinitionKey(javaType, ignoredDefinitionProvider);
        List<ObjectNode> valueList = targetMap.computeIfAbsent(key, k -> new ArrayList<>());
        valueList.add(referencingNode);
        return this;
    }

    /**
     * Getter for the nodes representing not-nullable references to the given type.
     *
     * @param key definition key to look-up collected references for
     * @return not-nullable nodes to be populated with the schema of the given type
     */
    public List<ObjectNode> getReferences(DefinitionKey key) {
        return Collections.unmodifiableList(this.references.getOrDefault(key, Collections.emptyList()));
    }

    /**
     * Getter for the nodes representing nullable references to the given type.
     *
     * @param key definition key to look-up collected references for
     * @return nullable nodes to be populated with the schema of the given type
     */
    public List<ObjectNode> getNullableReferences(DefinitionKey key) {
        return Collections.unmodifiableList(this.nullableReferences.getOrDefault(key, Collections.emptyList()));
    }

    /**
     * Determine whether the definition for the given type should always be included in the {@link SchemaKeyword#TAG_DEFINITIONS}, even if only
     * occurring once.
     *
     * @param key definition key to determine desired definition behaviour for
     * @return whether to always produce a referenced definition for the given type
     *
     * @since 4.27.0
     */
    public boolean shouldNeverInlineDefinition(DefinitionKey key) {
        return this.neverInlinedDefinitions.contains(key);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Here comes the logic for traversing types and populating this context *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    @Override
    public ObjectNode createDefinition(ResolvedType targetType) {
        return this.createStandardDefinition(targetType, null);
    }

    @Override
    public ObjectNode createDefinitionReference(ResolvedType targetType) {
        return this.createStandardDefinitionReference(targetType, null);
    }

    @Override
    public ObjectNode createStandardDefinition(ResolvedType targetType, CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        ObjectNode definition = this.generatorConfig.createObjectNode();
        TypeScope scope = this.typeContext.createTypeScope(targetType);
        GenericTypeDetails typeDetails = new GenericTypeDetails(scope, false, true, ignoredDefinitionProvider);
        this.traverseGenericType(definition, typeDetails);
        return definition;
    }

    @Override
    public ObjectNode createStandardDefinition(FieldScope targetScope, CustomPropertyDefinitionProvider<FieldScope> ignoredDefinitionProvider) {
        return this.createFieldSchema(new MemberDetails<>(targetScope, false, true, ignoredDefinitionProvider));
    }

    @Override
    public JsonNode createStandardDefinition(MethodScope targetScope, CustomPropertyDefinitionProvider<MethodScope> ignoredDefinitionProvider) {
        return this.createMethodSchema(new MemberDetails<>(targetScope, false, true, ignoredDefinitionProvider));
    }

    @Override
    public ObjectNode createStandardDefinitionReference(ResolvedType targetType, CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        ObjectNode definition = this.generatorConfig.createObjectNode();
        TypeScope scope = this.typeContext.createTypeScope(targetType);
        GenericTypeDetails typeDetails = new GenericTypeDetails(scope, false, false, ignoredDefinitionProvider);
        this.traverseGenericType(definition, typeDetails);
        return definition;
    }

    @Override
    public ObjectNode createStandardDefinitionReference(FieldScope targetScope,
            CustomPropertyDefinitionProvider<FieldScope> ignoredDefinitionProvider) {
        return this.createFieldSchema(new MemberDetails<>(targetScope, false, false, ignoredDefinitionProvider));
    }

    @Override
    public JsonNode createStandardDefinitionReference(MethodScope targetScope,
            CustomPropertyDefinitionProvider<MethodScope> ignoredDefinitionProvider) {
        return this.createMethodSchema(new MemberDetails<>(targetScope, false, false, ignoredDefinitionProvider));
    }

    /**
     * Preparation Step: add the given targetType.
     *
     * @param targetType (possibly generic) type to add
     * @param targetNode node in the JSON schema that should represent the targetType
     */
    protected void traverseGenericType(ResolvedType targetType, ObjectNode targetNode) {
        TypeScope scope = this.typeContext.createTypeScope(targetType);
        GenericTypeDetails typeDetails = new GenericTypeDetails(scope, false, false, null);
        this.traverseGenericType(targetNode, typeDetails);
    }

    /**
     * Preparation Step: add the given target type. Also catering for forced inline-definitions and ignoring custom definitions.
     *
     * @param targetNode node in the JSON schema that should represent the targetType
     * @param typeDetails details of the target type to generate subschema for
     */
    private void traverseGenericType(ObjectNode targetNode, GenericTypeDetails typeDetails) {
        ResolvedType targetType = typeDetails.getScope().getType();
        if (shouldAddReferenceForExistingDefinition(typeDetails)) {
            logger.debug("adding reference to existing definition of {}", targetType);
            this.addReference(targetType, targetNode, typeDetails.getIgnoredDefinitionProvider(), typeDetails.isNullable());
            // nothing more to be done
            return;
        }
        final Map.Entry<ObjectNode, Boolean> definitionAndTypeAttributeInclusionFlag;
        final CustomDefinition customDefinition = this.generatorConfig.getCustomDefinition(targetType, this,
                typeDetails.getIgnoredDefinitionProvider());
        if (customDefinition == null) {
            // always inline array types
            GenericTypeDetails typeDetailsWithInlineArrays = typeDetails.withAlternativeReasonToInline(
                    this.typeContext.isContainerType(targetType) && targetNode != null);
            definitionAndTypeAttributeInclusionFlag = this.applyStandardDefinition(targetNode, typeDetailsWithInlineArrays);
        } else {
            GenericTypeDetails typeDetailsWithCustomPreference = typeDetails.withAlternativeReasonToInline(customDefinition.isMeantToBeInline());
            definitionAndTypeAttributeInclusionFlag = this.applyCustomDefinition(customDefinition, targetNode, typeDetailsWithCustomPreference);
        }
        final ObjectNode definition = definitionAndTypeAttributeInclusionFlag.getKey();
        if (definitionAndTypeAttributeInclusionFlag.getValue()) {
            Set<String> allowedSchemaTypes = this.collectAllowedSchemaTypes(definition);
            ObjectNode typeAttributes = AttributeCollector.collectTypeAttributes(typeDetails.getScope(), this, allowedSchemaTypes);
            // ensure no existing attributes in the 'definition' are replaced, by way of first overriding any conflicts the other way around
            typeAttributes.setAll(definition);
            // apply merged attributes
            definition.setAll(typeAttributes);
        }
        // apply overrides as the very last step
        this.generatorConfig.getTypeAttributeOverrides()
                .forEach(override -> override.overrideTypeAttributes(definition, typeDetails.getScope(), this));
    }

    private boolean shouldAddReferenceForExistingDefinition(GenericTypeDetails typeDetails) {
        return !typeDetails.isInlineDefinition()
               && this.containsDefinition(typeDetails.getScope().getType(), typeDetails.getIgnoredDefinitionProvider());
    }

    private Map.Entry<ObjectNode, Boolean> applyCustomDefinition(CustomDefinition customDefinition, ObjectNode targetNode,
            GenericTypeDetails typeDetails) {
        ResolvedType targetType = typeDetails.getScope().getType();
        if (!typeDetails.isInlineDefinition()) {
            ObjectNode definition = this.generatorConfig.createObjectNode();
            this.putDefinition(targetType, definition, typeDetails.getIgnoredDefinitionProvider());
            this.addReference(targetType, targetNode, typeDetails.getIgnoredDefinitionProvider(), typeDetails.isNullable());
            this.markDefinitionAsNeverInlinedIfRequired(customDefinition, targetType, typeDetails.getIgnoredDefinitionProvider());
            logger.debug("applying configured custom definition for {}", targetType);
            definition.setAll(customDefinition.getValue());
            return new AbstractMap.SimpleEntry<>(definition, customDefinition.shouldIncludeAttributes());
        }
        final ObjectNode definition;
        if (targetNode == null) {
            logger.debug("storing configured custom inline type for {} as definition (since it is the main schema \"#\")", targetType);
            definition = customDefinition.getValue();
            this.putDefinition(targetType, definition, typeDetails.getIgnoredDefinitionProvider());
            // targetNode will be populated at the end, in buildDefinitionsAndResolveReferences()
        } else {
            logger.debug("directly applying configured custom inline type for {}", targetType);
            targetNode.setAll(customDefinition.getValue());
            definition = targetNode;
        }
        if (typeDetails.isNullable()) {
            this.makeNullable(definition);
        }
        return new AbstractMap.SimpleEntry<>(definition, customDefinition.shouldIncludeAttributes());
    }

    private Map.Entry<ObjectNode, Boolean> applyStandardDefinition(ObjectNode targetNode, GenericTypeDetails typeDetails) {
        ResolvedType targetType = typeDetails.getScope().getType();
        final ObjectNode definition;
        if (typeDetails.isInlineDefinition()) {
            definition = targetNode;
        } else {
            definition = this.generatorConfig.createObjectNode();
            this.putDefinition(targetType, definition, typeDetails.getIgnoredDefinitionProvider());
            this.addReference(targetType, targetNode, typeDetails.getIgnoredDefinitionProvider(), typeDetails.isNullable());
        }
        final boolean includeTypeAttributes;
        if (this.typeContext.isContainerType(targetType)) {
            logger.debug("generating array definition for {}", targetType);
            this.generateArrayDefinition(typeDetails, definition);
            includeTypeAttributes = true;
        } else {
            logger.debug("generating definition for {}", targetType);
            includeTypeAttributes = !this.addSubtypeReferencesInDefinition(targetType, definition);
        }
        return new AbstractMap.SimpleEntry<>(definition, includeTypeAttributes);
    }

    /**
     * Check for any defined subtypes of the targeted java type to produce a definition for. If there are any configured subtypes, reference those
     * from within the definition being generated.
     *
     * @param targetType (possibly generic) type to add
     * @param definition node in the JSON schema to which all collected attributes should be added
     * @return whether any subtypes were found for which references were added to the given definition
     */
    private boolean addSubtypeReferencesInDefinition(ResolvedType targetType, ObjectNode definition) {
        List<ResolvedType> subtypes = this.generatorConfig.resolveSubtypes(targetType, this);
        if (subtypes.isEmpty()) {
            this.generateObjectDefinition(targetType, definition);
            return false;
        }
        // always wrap subtype definitions, in order to avoid pointing at the same definition node as the super type
        SchemaKeyword arrayNodeName = subtypes.size() == 1 ? SchemaKeyword.TAG_ALLOF : SchemaKeyword.TAG_ANYOF;
        ArrayNode subtypeDefinitionArrayNode = definition.withArray(this.getKeyword(arrayNodeName));
        subtypes.stream()
                .map(this::createDefinitionReference)
                .forEach(subtypeDefinitionArrayNode::add);
        return true;
    }

    /**
     * Collect the specified value(s) from the given definition's {@link SchemaKeyword#TAG_TYPE} attribute.
     *
     * @param definition type definition to extract specified {@link SchemaKeyword#TAG_TYPE} values from
     * @return extracted {@link SchemaKeyword#TAG_TYPE} - values (may be empty)
     */
    private Set<String> collectAllowedSchemaTypes(ObjectNode definition) {
        JsonNode declaredTypes = definition.get(this.getKeyword(SchemaKeyword.TAG_TYPE));
        final Set<String> allowedSchemaTypes;
        if (declaredTypes == null) {
            allowedSchemaTypes = Collections.emptySet();
        } else if (declaredTypes.isString()) {
            allowedSchemaTypes = Collections.singleton(declaredTypes.stringValue());
        } else {
            allowedSchemaTypes = StreamSupport.stream(declaredTypes.spliterator(), false)
                    .map(JsonNode::stringValue)
                    .collect(Collectors.toSet());
        }
        return allowedSchemaTypes;
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be an array type).
     *
     * @param typeDetails details around (possibly generic) array type to add subschema for
     * @param definition node in the JSON schema to which all collected attributes should be added
     */
    private void generateArrayDefinition(GenericTypeDetails typeDetails, ObjectNode definition) {
        definition.put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY));
        definition.set(this.getKeyword(SchemaKeyword.TAG_ITEMS), this.populateItemMemberSchema(typeDetails.getScope()));
        if (typeDetails.isNullable()) {
            this.makeNullable(definition);
        }
    }

    private JsonNode populateItemMemberSchema(TypeScope targetScope) {
        if (targetScope instanceof FieldScope && !((FieldScope) targetScope).isFakeContainerItemScope()) {
            return this.populateFieldSchema(((FieldScope) targetScope).asFakeContainerItemScope());
        }
        if (targetScope instanceof MethodScope && !((MethodScope) targetScope).isFakeContainerItemScope()) {
            return this.populateMethodSchema(((MethodScope) targetScope).asFakeContainerItemScope());
        }
        ObjectNode arrayItemDefinition = this.generatorConfig.createObjectNode();
        this.traverseGenericType(targetScope.getContainerItemType(), arrayItemDefinition);
        return arrayItemDefinition;
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be anything but an array type).
     *
     * @param targetType object type to add
     * @param definition node in the JSON schema to which all collected attributes should be added
     */
    private void generateObjectDefinition(ResolvedType targetType, ObjectNode definition) {
        definition.put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));

        MemberCollectionContextImpl memberCollectionContext = new MemberCollectionContextImpl(targetType, this.generatorConfig, this.typeContext);
        memberCollectionContext.collectProperties();

        List<MemberScope<?, ?>> sortedProperties = memberCollectionContext.getSortedProperties();
        if (!sortedProperties.isEmpty()) {
            this.addPropertiesToDefinition(definition, sortedProperties, memberCollectionContext.getRequiredPropertyNames());
        }
    }

    private void addPropertiesToDefinition(ObjectNode definition, List<MemberScope<?, ?>> sortedProperties, Set<String> requiredPropertyNames) {
        ObjectNode propertiesNode = definition.putObject(this.getKeyword(SchemaKeyword.TAG_PROPERTIES));
        Map<String, List<String>> dependentRequires = new LinkedHashMap<>();
        for (MemberScope<?, ?> property : sortedProperties) {
            this.addPropertiesEntry(propertiesNode, dependentRequires, property);
        }
        if (!requiredPropertyNames.isEmpty()) {
            ArrayNode requiredNode = definition.putArray(this.getKeyword(SchemaKeyword.TAG_REQUIRED));
            // list required properties in the same order as the property
            sortedProperties.stream()
                    .map(MemberScope::getSchemaPropertyName)
                    .filter(requiredPropertyNames::contains)
                    .forEach(requiredNode::add);
        }
        if (!dependentRequires.isEmpty()) {
            ObjectNode dependentRequiredNode = definition.putObject(this.getKeyword(SchemaKeyword.TAG_DEPENDENT_REQUIRED));
            dependentRequires.forEach((leadName, dependentNames) -> dependentNames
                    .forEach(dependentRequiredNode.withArray(leadName)::add));
        }
    }

    private void addPropertiesEntry(ObjectNode propertiesNode, Map<String, List<String>> dependentRequires, MemberScope<?, ?> property) {
        String propertyName = property.getSchemaPropertyName();

        JsonNode subSchema = this.typeContext.performActionOnMember(property, this::populateFieldSchema, this::populateMethodSchema);
        propertiesNode.set(propertyName, subSchema);

        List<String> dependentRequiredForProperty = this.typeContext.performActionOnMember(property,
                this.generatorConfig::resolveDependentRequires, this.generatorConfig::resolveDependentRequires);
        if (!Util.isNullOrEmpty(dependentRequiredForProperty)) {
            dependentRequires.put(propertyName, dependentRequiredForProperty);
        }
    }

    /**
     * Preparation Step: create a node for a schema representing the given field's associated value type.
     *
     * @param field field/property to populate the schema node for
     * @return schema node representing the given field/property
     */
    private JsonNode populateFieldSchema(FieldScope field) {
        List<ResolvedType> typeOverrides = this.generatorConfig.resolveTargetTypeOverrides(field);
        if (typeOverrides == null && this.generatorConfig.shouldTransparentlyResolveSubtypesOfMembers()) {
            typeOverrides = this.generatorConfig.resolveSubtypes(field.getType(), this);
        }
        List<FieldScope> fieldOptions;
        if (Util.isNullOrEmpty(typeOverrides)) {
            fieldOptions = Collections.singletonList(field);
        } else {
            fieldOptions = typeOverrides.stream()
                    .map(field::withOverriddenType)
                    .collect(Collectors.toList());
        }
        // consider declared type (instead of overridden one) for determining null-ability
        boolean isNullable = !field.getRawMember().isEnumConstant()
                && (!field.isFakeContainerItemScope() || this.generatorConfig.shouldAllowNullableArrayItems())
                && this.generatorConfig.isNullable(field);
        if (fieldOptions.size() == 1) {
            return this.createFieldSchema(new MemberDetails<>(fieldOptions.get(0), isNullable, false, null));
        }
        return this.createMemberSchemaWithMultipleOptions(fieldOptions, isNullable, this::createFieldSchema);
    }

    private <M extends MemberScope<?, ?>> JsonNode createMemberSchemaWithMultipleOptions(List<M> memberOptions, boolean isNullable,
            Function<MemberDetails<M>, JsonNode> createMemberSchema) {
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ArrayNode anyOfArray = subSchema.withArray(this.getKeyword(SchemaKeyword.TAG_ANYOF));
        if (isNullable) {
            anyOfArray.addObject()
                    .put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
        }
        memberOptions.stream()
                .map(option -> new MemberDetails<>(option, false, false, null))
                .map(createMemberSchema)
                .forEach(anyOfArray::add);
        return subSchema;
    }

    /**
     * Preparation Step: create a node for a schema representing the given field's associated value type.
     *
     * @param fieldDetails details for field to populate schema for
     * @return schema node representing the given field/property
     */
    private ObjectNode createFieldSchema(MemberDetails<FieldScope> fieldDetails) {
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ObjectNode fieldAttributes = AttributeCollector.collectFieldAttributes(fieldDetails.getScope(), this);
        this.populateMemberSchema(subSchema, fieldDetails, fieldAttributes);
        return subSchema;
    }

    /**
     * Preparation Step: create a node for a schema representing the given method's associated return type.
     *
     * @param method method to populate the schema node for
     * @return schema node representing the given method's return type
     */
    private JsonNode populateMethodSchema(MethodScope method) {
        List<ResolvedType> typeOverrides = this.generatorConfig.resolveTargetTypeOverrides(method);
        if (typeOverrides == null && !method.isVoid()) {
            typeOverrides = this.generatorConfig.resolveSubtypes(method.getType(), this);
        }
        List<MethodScope> methodOptions;
        if (Util.isNullOrEmpty(typeOverrides)) {
            methodOptions = Collections.singletonList(method);
        } else {
            methodOptions = typeOverrides.stream()
                    .map(method::withOverriddenType)
                    .collect(Collectors.toList());
        }
        // consider declared type (instead of overridden one) for determining null-ability
        boolean isNullable = method.isVoid()
                || (!method.isFakeContainerItemScope() || this.generatorConfig.shouldAllowNullableArrayItems())
                && this.generatorConfig.isNullable(method);
        if (methodOptions.size() == 1) {
            return this.createMethodSchema(new MemberDetails<>(methodOptions.get(0), isNullable, false, null));
        }
        return this.createMemberSchemaWithMultipleOptions(methodOptions, isNullable, this::createMethodSchema);
    }

    /**
     * Preparation Step: create a node for a schema representing the given method's associated return type.
     *
     * @param methodDetails details for method to populate schema for
     * @return schema node representing the given method's return type
     */
    private JsonNode createMethodSchema(MemberDetails<MethodScope> methodDetails) {
        if (methodDetails.getScope().isVoid()) {
            // since 4.35.0: support custom definitions for void methods
            CustomDefinition customDefinition = this.generatorConfig.getCustomDefinition(methodDetails.getScope(), this,
                    methodDetails.getIgnoredDefinitionProvider());
            if (customDefinition == null) {
                return BooleanNode.FALSE;
            }
        }
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ObjectNode methodAttributes = AttributeCollector.collectMethodAttributes(methodDetails.getScope(), this);
        this.populateMemberSchema(subSchema, methodDetails, methodAttributes);
        return subSchema;
    }

    /**
     * Preparation Step: combine the collected attributes and the javaType's definition in the given targetNode.
     *
     * @param <M> type of target scope, i.e. either a field or method
     * @param targetNode node in the JSON schema that should represent the associated javaType and include the separately collected attributes
     * @param memberDetails details for field/method to populate schema for
     * @param collectedMemberAttributes separately collected attribute for the field/method in their respective declaring type
     * @see #populateFieldSchema(FieldScope)
     */
    private <M extends MemberScope<?, ?>> void populateMemberSchema(ObjectNode targetNode, MemberDetails<M> memberDetails,
            ObjectNode collectedMemberAttributes) {
        CustomDefinition customDefinition = this.generatorConfig.getCustomDefinition(memberDetails.getScope(), this,
                memberDetails.getIgnoredDefinitionProvider());
        boolean isInlineCustomDefinition = customDefinition != null && customDefinition.isMeantToBeInline();
        GenericTypeDetails typeDetails = memberDetails.toTypeDetails().withAlternativeReasonToInline(isInlineCustomDefinition);
        if (isInlineCustomDefinition) {
            populateMemberSchemaWithInlineCustomDefinition(targetNode, typeDetails, collectedMemberAttributes, customDefinition);
        } else {
            populateMemberSchemaWithReference(targetNode, typeDetails, collectedMemberAttributes, customDefinition);
        }
    }

    private void populateMemberSchemaWithInlineCustomDefinition(ObjectNode targetNode, GenericTypeDetails typeDetails,
            ObjectNode collectedMemberAttributes, CustomDefinition customDefinition) {
        if (customDefinition.getValue().isEmpty()) {
            targetNode.withArray(this.getKeyword(SchemaKeyword.TAG_ALLOF))
                    .add(customDefinition.getValue());
        } else {
            targetNode.setAll(customDefinition.getValue());
        }
        if (customDefinition.shouldIncludeAttributes()) {
            AttributeCollector.mergeMissingAttributes(targetNode, collectedMemberAttributes);
            Set<String> allowedSchemaTypes = this.collectAllowedSchemaTypes(targetNode);
            ObjectNode typeAttributes = AttributeCollector.collectTypeAttributes(typeDetails.getScope(), this, allowedSchemaTypes);
            AttributeCollector.mergeMissingAttributes(targetNode, typeAttributes);
        }
        if (typeDetails.isNullable()) {
            this.makeNullable(targetNode);
        }
    }

    private void populateMemberSchemaWithReference(ObjectNode targetNode, GenericTypeDetails typeDetails,
            ObjectNode collectedMemberAttributes, CustomDefinition customDefinition) {
        // create an "allOf" wrapper for the attributes related to this particular field and its general type
        final ObjectNode referenceContainer;
        boolean ignoreCollectedAttributes = customDefinition != null && !customDefinition.shouldIncludeAttributes()
                || Util.isNullOrEmpty(collectedMemberAttributes);
        if (ignoreCollectedAttributes) {
            // no need for the allOf, can use the sub-schema instance directly as reference
            referenceContainer = targetNode;
        } else if (customDefinition == null && typeDetails.getScope().isContainerType()) {
            // same as above, but the collected attributes should be applied also for containers/arrays
            referenceContainer = targetNode;
            AttributeCollector.mergeMissingAttributes(targetNode, collectedMemberAttributes);
        } else {
            // avoid mixing potential "$ref" element with contextual attributes by introducing an "allOf" wrapper
            // this is only relevant for DRAFT_6 / DRAFT_7 and is being cleaned-up afterward for newer schema versions
            referenceContainer = this.generatorConfig.createObjectNode();
            targetNode.putArray(this.getKeyword(SchemaKeyword.TAG_ALLOF))
                    .add(referenceContainer)
                    .add(collectedMemberAttributes);
        }
        // only add reference for separate definition if it is not a fixed type that should be in-lined
        try {
            this.traverseGenericType(referenceContainer, typeDetails);
        } catch (UnsupportedOperationException ex) {
            logger.warn("Skipping type definition due to error", ex);
        }
    }

    @Override
    public String getKeyword(SchemaKeyword keyword) {
        return this.generatorConfig.getKeyword(keyword);
    }

    @Override
    public ObjectNode makeNullable(ObjectNode node) {
        return SchemaGenerationContextImpl.makeNullable(node, this.generatorConfig);
    }

    static ObjectNode makeNullable(ObjectNode node, SchemaGeneratorConfig config) {
        if (SchemaGenerationContextImpl.canExtendTypeDeclarationToIncludeNull(node, config)) {
            // given node is a simple schema, we can adjust its "type" attribute
            SchemaGenerationContextImpl.extendTypeDeclarationToIncludeNull(node, config);
        } else {
            SchemaGenerationContextImpl.addAnyOfNullSchema(node, config);
        }
        return node;
    }

    private static boolean canExtendTypeDeclarationToIncludeNull(ObjectNode node, SchemaGeneratorConfig config) {
        if (config.shouldAlwaysWrapNullSchemaInAnyOf()) {
            return false;
        }
        Stream<SchemaKeyword> requiringAnyOfWrapper = Stream.of(
                SchemaKeyword.TAG_REF, SchemaKeyword.TAG_ALLOF, SchemaKeyword.TAG_ANYOF, SchemaKeyword.TAG_ONEOF,
                // since version 4.21.0
                SchemaKeyword.TAG_CONST, SchemaKeyword.TAG_ENUM
        );
        return requiringAnyOfWrapper.map(config::getKeyword).noneMatch(node::has);
    }

    private static void extendTypeDeclarationToIncludeNull(ObjectNode node, SchemaGeneratorConfig config) {
        JsonNode fixedJsonSchemaType = node.get(config.getKeyword(SchemaKeyword.TAG_TYPE));
        final String nullTypeName = config.getKeyword(SchemaKeyword.TAG_TYPE_NULL);
        if (fixedJsonSchemaType instanceof ArrayNode) {
            // there are already multiple "type" values
            ArrayNode arrayOfTypes = (ArrayNode) fixedJsonSchemaType;
            // one of the existing "type" values could be null
            for (JsonNode arrayEntry : arrayOfTypes) {
                if (nullTypeName.equals(arrayEntry.stringValue())) {
                    return;
                }
            }
            // null "type" was not mentioned before, to be safe we need to replace the old array and add the null entry
            node.putArray(config.getKeyword(SchemaKeyword.TAG_TYPE))
                    .addAll(arrayOfTypes)
                    .add(nullTypeName);
        } else if (fixedJsonSchemaType instanceof StringNode && !nullTypeName.equals(fixedJsonSchemaType.stringValue())) {
            // add null as second "type" option
            node.putArray(config.getKeyword(SchemaKeyword.TAG_TYPE))
                    .add(fixedJsonSchemaType)
                    .add(nullTypeName);
        }
        // if no "type" is specified, null is allowed already
    }

    private static void addAnyOfNullSchema(ObjectNode node, SchemaGeneratorConfig config) {
        // cannot be sure what is specified in those other schema parts, instead simply create an anyOf wrapper
        ObjectNode nullSchema = config.createObjectNode()
                .put(config.getKeyword(SchemaKeyword.TAG_TYPE), config.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
        String anyOfTagName = config.getKeyword(SchemaKeyword.TAG_ANYOF);
        // reduce likelihood of nested duplicate null schema
        JsonNode existingAnyOf = node.get(anyOfTagName);
        if (existingAnyOf instanceof ArrayNode) {
            Iterator<JsonNode> anyOfIterator = existingAnyOf.iterator();
            while (anyOfIterator.hasNext()) {
                if (nullSchema.equals(anyOfIterator.next())) {
                    // the existing anyOf array contains a duplicate null schema, remove it
                    anyOfIterator.remove();
                    // unlikely that there are multiple
                    break;
                }
            }
        }
        ArrayNode newAnyOf = config.createArrayNode()
                // one option in the anyOf should be null
                .add(nullSchema)
                // the other option is the given (assumed to be) not-nullable node
                .add(config.createObjectNode().setAll(node));
        // replace all existing (and already copied properties with the anyOf wrapper
        node.removeAll();
        node.set(anyOfTagName, newAnyOf);
    }

    private static class GenericTypeDetails {
        private final TypeScope scope;
        private final boolean nullable;
        private final boolean inlineDefinition;
        private final CustomDefinitionProviderV2 ignoredDefinitionProvider;

        /**
         * Combine schema generation details for a generic type.
         *
         * @param scope targeted scope to create schema for
         * @param nullable whether the type is allowed to be null in the declaring type in this particular scenario
         * @param inlineDefinition whether to generate an inline definition without registering it in this context
         * @param ignoredDefinitionProvider first custom definition provider to ignore
         */
        GenericTypeDetails(TypeScope scope, boolean nullable, boolean inlineDefinition, CustomDefinitionProviderV2 ignoredDefinitionProvider) {
            this.scope = scope;
            this.nullable = nullable;
            this.inlineDefinition = inlineDefinition;
            this.ignoredDefinitionProvider = ignoredDefinitionProvider;
        }

        GenericTypeDetails withAlternativeReasonToInline(boolean alternativeReasonToInline) {
            return new GenericTypeDetails(this.getScope(), this.isNullable(),
                    this.isInlineDefinition() || alternativeReasonToInline,
                    this.getIgnoredDefinitionProvider());
        }

        public TypeScope getScope() {
            return this.scope;
        }

        public boolean isNullable() {
            return this.nullable;
        }

        public boolean isInlineDefinition() {
            return this.inlineDefinition;
        }

        public CustomDefinitionProviderV2 getIgnoredDefinitionProvider() {
            return this.ignoredDefinitionProvider;
        }
    }

    private static class MemberDetails<M extends MemberScope<?, ?>> {
        private final M scope;
        private final boolean nullable;
        private final boolean inlineDefinition;
        private final CustomPropertyDefinitionProvider<M> ignoredDefinitionProvider;

        /**
         * Combine schema generation details for a field or method.
         *
         * @param scope targeted scope to add schema for
         * @param nullable whether the field/method's return value is allowed to be null in the declaringType in this particular scenario
         * @param inlineDefinition whether to generate an inline definition without registering it in this context
         * @param ignoredDefinitionProvider first custom definition provider to ignore
         */
        MemberDetails(M scope, boolean nullable, boolean inlineDefinition, CustomPropertyDefinitionProvider<M> ignoredDefinitionProvider) {
            this.scope = scope;
            this.nullable = nullable;
            this.inlineDefinition = inlineDefinition;
            this.ignoredDefinitionProvider = ignoredDefinitionProvider;
        }

        GenericTypeDetails toTypeDetails() {
            return new GenericTypeDetails(this.getScope(), this.isNullable(), this.isInlineDefinition(), null);
        }

        public M getScope() {
            return this.scope;
        }

        public boolean isNullable() {
            return this.nullable;
        }

        public boolean isInlineDefinition() {
            return this.inlineDefinition;
        }

        public CustomPropertyDefinitionProvider<M> getIgnoredDefinitionProvider() {
            return this.ignoredDefinitionProvider;
        }
    }
}
