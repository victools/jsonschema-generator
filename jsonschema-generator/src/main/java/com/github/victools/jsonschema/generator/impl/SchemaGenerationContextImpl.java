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
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.HierarchicType;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
        this.traverseGenericType(type, null, false);
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
        this.traverseGenericType(scope, definition, false, true, ignoredDefinitionProvider);
        return definition;
    }

    @Override
    public ObjectNode createStandardDefinition(FieldScope targetScope, CustomPropertyDefinitionProvider<FieldScope> ignoredDefinitionProvider) {
        return this.createFieldSchema(targetScope, false, true, ignoredDefinitionProvider);
    }

    @Override
    public JsonNode createStandardDefinition(MethodScope targetScope, CustomPropertyDefinitionProvider<MethodScope> ignoredDefinitionProvider) {
        return this.createMethodSchema(targetScope, false, true, ignoredDefinitionProvider);
    }

    @Override
    public ObjectNode createStandardDefinitionReference(ResolvedType targetType, CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        ObjectNode definition = this.generatorConfig.createObjectNode();
        TypeScope scope = this.typeContext.createTypeScope(targetType);
        this.traverseGenericType(scope, definition, false, false, ignoredDefinitionProvider);
        return definition;
    }

    @Override
    public ObjectNode createStandardDefinitionReference(FieldScope targetScope,
            CustomPropertyDefinitionProvider<FieldScope> ignoredDefinitionProvider) {
        return this.createFieldSchema(targetScope, false, false, ignoredDefinitionProvider);
    }

    @Override
    public JsonNode createStandardDefinitionReference(MethodScope targetScope,
            CustomPropertyDefinitionProvider<MethodScope> ignoredDefinitionProvider) {
        return this.createMethodSchema(targetScope, false, false, ignoredDefinitionProvider);
    }

    /**
     * Preparation Step: add the given targetType.
     *
     * @param targetType (possibly generic) type to add
     * @param targetNode node in the JSON schema that should represent the targetType
     * @param isNullable whether the field/method's return value is allowed to be null in the declaringType in this particular scenario
     */
    protected void traverseGenericType(ResolvedType targetType, ObjectNode targetNode, boolean isNullable) {
        TypeScope scope = this.typeContext.createTypeScope(targetType);
        this.traverseGenericType(scope, targetNode, isNullable, false, null);
    }

    /**
     * Preparation Step: add the given targetType. Also catering for forced inline-definitions and ignoring custom definitions
     *
     * @param scope targeted scope to add
     * @param targetNode node in the JSON schema that should represent the targetType
     * @param isNullable whether the field/method's return value is allowed to be null in the declaringType in this particular scenario
     * @param forceInlineDefinition whether to generate an inline definition without registering it in this context
     * @param ignoredDefinitionProvider first custom definition provider to ignore
     */
    private void traverseGenericType(TypeScope scope, ObjectNode targetNode, boolean isNullable, boolean forceInlineDefinition,
            CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        ResolvedType targetType = scope.getType();
        if (!forceInlineDefinition && this.containsDefinition(targetType, ignoredDefinitionProvider)) {
            logger.debug("adding reference to existing definition of {}", targetType);
            this.addReference(targetType, targetNode, ignoredDefinitionProvider, isNullable);
            // nothing more to be done
            return;
        }
        final ObjectNode definition;
        final boolean includeTypeAttributes;
        final CustomDefinition customDefinition = this.generatorConfig.getCustomDefinition(targetType, this, ignoredDefinitionProvider);
        if (customDefinition != null && (customDefinition.isMeantToBeInline() || forceInlineDefinition)) {
            includeTypeAttributes = customDefinition.shouldIncludeAttributes();
            if (targetNode == null) {
                logger.debug("storing configured custom inline type for {} as definition (since it is the main schema \"#\")", targetType);
                definition = customDefinition.getValue();
                this.putDefinition(targetType, definition, ignoredDefinitionProvider);
                // targetNode will be populated at the end, in buildDefinitionsAndResolveReferences()
            } else {
                logger.debug("directly applying configured custom inline type for {}", targetType);
                targetNode.setAll(customDefinition.getValue());
                definition = targetNode;
            }
            if (isNullable) {
                this.makeNullable(definition);
            }
        } else {
            boolean isContainerType = this.typeContext.isContainerType(targetType);
            if (forceInlineDefinition || isContainerType && targetNode != null && customDefinition == null) {
                // always inline array types
                definition = targetNode;
            } else {
                definition = this.generatorConfig.createObjectNode();
                this.putDefinition(targetType, definition, ignoredDefinitionProvider);
                if (targetNode != null) {
                    // targetNode is only null for the main class for which the schema is being generated
                    this.addReference(targetType, targetNode, ignoredDefinitionProvider, isNullable);
                }
            }
            if (customDefinition != null) {
                logger.debug("applying configured custom definition for {}", targetType);
                definition.setAll(customDefinition.getValue());
                includeTypeAttributes = customDefinition.shouldIncludeAttributes();
            } else if (isContainerType) {
                logger.debug("generating array definition for {}", targetType);
                this.generateArrayDefinition(scope, definition, isNullable);
                includeTypeAttributes = true;
            } else {
                logger.debug("generating definition for {}", targetType);
                includeTypeAttributes = !this.addSubtypeReferencesInDefinition(targetType, definition);
            }
        }
        if (includeTypeAttributes) {
            Set<String> allowedSchemaTypes = this.collectAllowedSchemaTypes(definition);
            ObjectNode typeAttributes = AttributeCollector.collectTypeAttributes(scope, this, allowedSchemaTypes);
            // ensure no existing attributes in the 'definition' are replaced, by way of first overriding any conflicts the other way around
            typeAttributes.setAll(definition);
            // apply merged attributes
            definition.setAll(typeAttributes);
        }
        // apply overrides as the very last step
        this.generatorConfig.getTypeAttributeOverrides()
                .forEach(override -> override.overrideTypeAttributes(definition, scope, this));
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
        subtypes.forEach(subtype -> this.traverseGenericType(subtype, subtypeDefinitionArrayNode.addObject(), false));
        return true;
    }

    /**
     * Collect the specified value(s) from the given definition's {@link SchemaKeyword#TAG_TYPE} attribute.
     *
     * @param definition type definition to extract specified {@link SchemaKeyword#TAG_TYPE} values from
     * @return extracted {@link SchemaKeyword#TAG_TYPE} – values (may be empty)
     */
    private Set<String> collectAllowedSchemaTypes(ObjectNode definition) {
        JsonNode declaredTypes = definition.get(this.getKeyword(SchemaKeyword.TAG_TYPE));
        final Set<String> allowedSchemaTypes;
        if (declaredTypes == null) {
            allowedSchemaTypes = Collections.emptySet();
        } else if (declaredTypes.isTextual()) {
            allowedSchemaTypes = Collections.singleton(declaredTypes.textValue());
        } else {
            allowedSchemaTypes = StreamSupport.stream(declaredTypes.spliterator(), false)
                    .map(JsonNode::textValue)
                    .collect(Collectors.toSet());
        }
        return allowedSchemaTypes;
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be an array type).
     *
     * @param targetScope (possibly generic) array type to add
     * @param definition node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value the targetType refers to is allowed to be null in the declaring type
     */
    private void generateArrayDefinition(TypeScope targetScope, ObjectNode definition, boolean isNullable) {
        if (isNullable) {
            ArrayNode typeArray = this.generatorConfig.createArrayNode()
                    .add(this.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY))
                    .add(this.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
            definition.set(this.getKeyword(SchemaKeyword.TAG_TYPE), typeArray);
        } else {
            definition.put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY));
        }
        if (targetScope instanceof MemberScope<?, ?> && !((MemberScope<?, ?>) targetScope).isFakeContainerItemScope()) {
            MemberScope<?, ?> fakeArrayItemMember = ((MemberScope<?, ?>) targetScope).asFakeContainerItemScope();
            JsonNode fakeItemDefinition;
            if (targetScope instanceof FieldScope) {
                fakeItemDefinition = this.populateFieldSchema((FieldScope) fakeArrayItemMember);
            } else if (targetScope instanceof MethodScope) {
                fakeItemDefinition = this.populateMethodSchema((MethodScope) fakeArrayItemMember);
            } else {
                throw new IllegalStateException("Unsupported member type: " + targetScope.getClass().getName());
            }
            definition.set(this.getKeyword(SchemaKeyword.TAG_ITEMS), fakeItemDefinition);
        } else {
            ObjectNode arrayItemTypeRef = this.generatorConfig.createObjectNode();
            definition.set(this.getKeyword(SchemaKeyword.TAG_ITEMS), arrayItemTypeRef);
            this.traverseGenericType(targetScope.getContainerItemType(), arrayItemTypeRef, false);
        }
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be anything but an array type).
     *
     * @param targetType object type to add
     * @param definition node in the JSON schema to which all collected attributes should be added
     */
    private void generateObjectDefinition(ResolvedType targetType, ObjectNode definition) {
        definition.put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));

        /*
         * Collecting fields and methods according to their order in the Java Byte Code.
         * Depending on the compiler implementation, that order may or may not match the declaration order in the source code.
         */
        final Map<String, MemberScope<?, ?>> targetProperties = new LinkedHashMap<>();
        final Set<String> requiredProperties = new HashSet<>();

        this.collectObjectProperties(targetType, targetProperties, requiredProperties);

        if (!targetProperties.isEmpty()) {
            ObjectNode propertiesNode = this.generatorConfig.createObjectNode();
            List<MemberScope<?, ?>> sortedProperties = targetProperties.values().stream()
                    .sorted(this.generatorConfig::sortProperties)
                    .collect(Collectors.toList());
            for (MemberScope<?, ?> property : sortedProperties) {
                JsonNode subSchema;
                if (property instanceof FieldScope) {
                    subSchema = this.populateFieldSchema((FieldScope) property);
                } else if (property instanceof MethodScope) {
                    subSchema = this.populateMethodSchema((MethodScope) property);
                } else {
                    throw new IllegalStateException("Unsupported member scope of type " + property.getClass());
                }
                propertiesNode.set(property.getSchemaPropertyName(), subSchema);
            }
            definition.set(this.getKeyword(SchemaKeyword.TAG_PROPERTIES), propertiesNode);

            if (!requiredProperties.isEmpty()) {
                ArrayNode requiredNode = this.generatorConfig.createArrayNode();
                // list required properties in the same order as the property
                sortedProperties.stream()
                        .map(MemberScope::getSchemaPropertyName)
                        .filter(requiredProperties::contains)
                        .forEach(requiredNode::add);
                definition.set(this.getKeyword(SchemaKeyword.TAG_REQUIRED), requiredNode);
            }
        }
    }

    /**
     * Recursively collect all properties of the given object type and add them to the respective maps.
     *
     * @param targetType the type for which to collect fields and methods
     * @param targetProperties map of named fields and methods
     * @param requiredProperties set of properties value required
     */
    private void collectObjectProperties(ResolvedType targetType, Map<String, MemberScope<?, ?>> targetProperties, Set<String> requiredProperties) {
        logger.debug("collecting non-static fields and methods from {}", targetType);
        final ResolvedTypeWithMembers targetTypeWithMembers = this.typeContext.resolveWithMembers(targetType);
        // member fields and methods are being collected from the targeted type as well as its super types
        this.collectFields(targetTypeWithMembers, ResolvedTypeWithMembers::getMemberFields, targetProperties, requiredProperties);
        this.collectMethods(targetTypeWithMembers, ResolvedTypeWithMembers::getMemberMethods, targetProperties, requiredProperties);

        final boolean includeStaticFields = this.generatorConfig.shouldIncludeStaticFields();
        final boolean includeStaticMethods = this.generatorConfig.shouldIncludeStaticMethods();
        if (includeStaticFields || includeStaticMethods) {
            // static fields and methods are being collected only for the targeted type itself, i.e. need to iterate over super types specifically
            for (HierarchicType singleHierarchy : targetTypeWithMembers.allTypesAndOverrides()) {
                ResolvedType hierachyType = singleHierarchy.getType();
                logger.debug("collecting static fields and methods from {}", hierachyType);
                if ((!includeStaticFields || hierachyType.getStaticFields().isEmpty())
                        && (!includeStaticMethods || hierachyType.getStaticMethods().isEmpty())) {
                    // no static members to look-up for this (super) type
                    continue;
                }
                final ResolvedTypeWithMembers hierarchyTypeMembers;
                if (hierachyType == targetType) {
                    // avoid looking up the main type again
                    hierarchyTypeMembers = targetTypeWithMembers;
                } else {
                    hierarchyTypeMembers = this.typeContext.resolveWithMembers(hierachyType);
                }
                if (includeStaticFields) {
                    this.collectFields(hierarchyTypeMembers, ResolvedTypeWithMembers::getStaticFields, targetProperties, requiredProperties);
                }
                if (includeStaticMethods) {
                    this.collectMethods(hierarchyTypeMembers, ResolvedTypeWithMembers::getStaticMethods, targetProperties, requiredProperties);
                }
            }
        }
    }

    /**
     * Preparation Step: add the designated fields to the specified {@link Map}.
     *
     * @param declaringTypeMembers the type declaring the fields to populate
     * @param fieldLookup retrieval function for getter targeted fields from {@code declaringTypeMembers}
     * @param collectedProperties collected fields and methods for which sub schemas should be added
     * @param requiredProperties set of properties value required
     */
    private void collectFields(ResolvedTypeWithMembers declaringTypeMembers, Function<ResolvedTypeWithMembers, ResolvedField[]> fieldLookup,
            Map<String, MemberScope<?, ?>> collectedProperties, Set<String> requiredProperties) {
        Stream.of(fieldLookup.apply(declaringTypeMembers))
                .map(declaredField -> this.typeContext.createFieldScope(declaredField, declaringTypeMembers))
                .filter(fieldScope -> !this.generatorConfig.shouldIgnore(fieldScope))
                .forEach(fieldScope -> this.collectField(fieldScope, collectedProperties, requiredProperties));
    }

    /**
     * Preparation Step: add the designated methods to the specified {@link Map}.
     *
     * @param declaringTypeMembers the type declaring the methods to populate
     * @param methodLookup retrieval function for getter targeted methods from {@code declaringTypeMembers}
     * @param collectedProperties collected fields and methods for which sub schemas should be added
     * @param requiredProperties set of properties value required
     */
    private void collectMethods(ResolvedTypeWithMembers declaringTypeMembers, Function<ResolvedTypeWithMembers, ResolvedMethod[]> methodLookup,
            Map<String, MemberScope<?, ?>> collectedProperties, Set<String> requiredProperties) {
        Stream.of(methodLookup.apply(declaringTypeMembers))
                .map(declaredMethod -> this.typeContext.createMethodScope(declaredMethod, declaringTypeMembers))
                .filter(methodScope -> !this.generatorConfig.shouldIgnore(methodScope))
                .forEach(methodScope -> this.collectMethod(methodScope, collectedProperties, requiredProperties));
    }

    /**
     * Preparation Step: add the given field to the specified {@link Map}.
     *
     * @param field declared field that should be added
     * @param collectedProperties collection of fields/methods to be added as properties
     * @param requiredProperties set of required properties
     */
    private void collectField(FieldScope field, Map<String, MemberScope<?, ?>> collectedProperties, Set<String> requiredProperties) {
        final FieldScope fieldWithNameOverride;
        final String propertyName;
        if (field.isFakeContainerItemScope()) {
            fieldWithNameOverride = field;
            propertyName = field.getSchemaPropertyName();
        } else {
            String propertyNameOverride = this.generatorConfig.resolvePropertyNameOverride(field);
            fieldWithNameOverride = propertyNameOverride == null ? field : field.withOverriddenName(propertyNameOverride);
            propertyName = fieldWithNameOverride.getSchemaPropertyName();
            if (this.generatorConfig.isRequired(field)) {
                requiredProperties.add(propertyName);
            }
            if (collectedProperties.containsKey(propertyName)) {
                logger.debug("ignoring overridden {}.{}", fieldWithNameOverride.getDeclaringType(), fieldWithNameOverride.getDeclaredName());
                return;
            }
        }
        collectedProperties.put(propertyName, fieldWithNameOverride);
    }

    /**
     * Preparation Step: create a node for a schema representing the given field's associated value type.
     *
     * @param field field/property to populate the schema node for
     * @return schema node representing the given field/property
     */
    private ObjectNode populateFieldSchema(FieldScope field) {
        List<ResolvedType> typeOverrides = this.generatorConfig.resolveTargetTypeOverrides(field);
        if (typeOverrides == null) {
            typeOverrides = this.generatorConfig.resolveSubtypes(field.getType(), this);
        }
        List<FieldScope> fieldOptions;
        if (typeOverrides == null || typeOverrides.isEmpty()) {
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
            return this.createFieldSchema(fieldOptions.get(0), isNullable, false, null);
        }
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ArrayNode anyOfArray = subSchema.withArray(this.getKeyword(SchemaKeyword.TAG_ANYOF));
        if (isNullable) {
            anyOfArray.addObject()
                    .put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
        }
        fieldOptions.forEach(option -> anyOfArray.add(this.createFieldSchema(option, false, false, null)));
        return subSchema;
    }

    /**
     * Preparation Step: create a node for a schema representing the given field's associated value type.
     *
     * @param field field/property to populate the schema node for
     * @param isNullable whether the field/property's value may be null
     * @param forceInlineDefinition whether to generate an inline definition without registering it in this context
     * @param ignoredDefinitionProvider first custom definition provider to ignore
     * @return schema node representing the given field/property
     */
    private ObjectNode createFieldSchema(FieldScope field, boolean isNullable, boolean forceInlineDefinition,
            CustomPropertyDefinitionProvider<FieldScope> ignoredDefinitionProvider) {
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ObjectNode fieldAttributes = AttributeCollector.collectFieldAttributes(field, this);
        this.populateMemberSchema(field, subSchema, isNullable, forceInlineDefinition, fieldAttributes, ignoredDefinitionProvider);
        return subSchema;
    }

    /**
     * Preparation Step: add the given method to the specified {@link Map}.
     *
     * @param method declared method that should be added to the specified node
     * @param collectedProperties collection of fields/methods to be added as properties
     * @param requiredProperties set of properties value required
     */
    private void collectMethod(MethodScope method, Map<String, MemberScope<?, ?>> collectedProperties, Set<String> requiredProperties) {
        final MethodScope methodWithNameOverride;
        final String propertyName;
        if (method.isFakeContainerItemScope()) {
            methodWithNameOverride = method;
            propertyName = method.getSchemaPropertyName();
        } else {
            String propertyNameOverride = this.generatorConfig.resolvePropertyNameOverride(method);
            methodWithNameOverride = propertyNameOverride == null ? method : method.withOverriddenName(propertyNameOverride);
            propertyName = methodWithNameOverride.getSchemaPropertyName();
            if (this.generatorConfig.isRequired(method)) {
                requiredProperties.add(propertyName);
            }
            if (collectedProperties.containsKey(propertyName)) {
                logger.debug("ignoring overridden {}.{}", methodWithNameOverride.getDeclaringType(), methodWithNameOverride.getDeclaredName());
                return;
            }
        }
        collectedProperties.put(propertyName, methodWithNameOverride);
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
        if (typeOverrides == null || typeOverrides.isEmpty()) {
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
            return this.createMethodSchema(methodOptions.get(0), isNullable, false, null);
        }
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ArrayNode anyOfArray = subSchema.withArray(this.getKeyword(SchemaKeyword.TAG_ANYOF));
        if (isNullable) {
            anyOfArray.add(this.generatorConfig.createObjectNode()
                    .put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_NULL)));
        }
        methodOptions.forEach(option -> anyOfArray.add(this.createMethodSchema(option, false, false, null)));
        return subSchema;
    }

    /**
     * Preparation Step: create a node for a schema representing the given method's associated return type.
     *
     * @param method method to populate the schema node for
     * @param isNullable whether the method's return value may be null
     * @param forceInlineDefinition whether to generate an inline definition without registering it in this context
     * @param ignoredDefinitionProvider first custom definition provider to ignore
     * @return schema node representing the given method's return type
     */
    private JsonNode createMethodSchema(MethodScope method, boolean isNullable, boolean forceInlineDefinition,
            CustomPropertyDefinitionProvider<MethodScope> ignoredDefinitionProvider) {
        if (method.isVoid()) {
            return BooleanNode.FALSE;
        }
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ObjectNode methodAttributes = AttributeCollector.collectMethodAttributes(method, this);
        this.populateMemberSchema(method, subSchema, isNullable, forceInlineDefinition, methodAttributes, ignoredDefinitionProvider);
        return subSchema;
    }

    /**
     * Preparation Step: combine the collected attributes and the javaType's definition in the given targetNode.
     *
     * @param <M> type of target scope, i.e. either a field or method
     * @param scope field's type or method return value's type that should be represented by the given targetNode
     * @param targetNode node in the JSON schema that should represent the associated javaType and include the separately collected attributes
     * @param isNullable whether the field/method's return value the javaType refers to is allowed to be null in the declaringType
     * @param forceInlineDefinition whether to generate an inline definition without registering it in this context
     * @param collectedAttributes separately collected attribute for the field/method in their respective declaring type
     * @param ignoredDefinitionProvider first custom definition provider to ignore
     * @see #populateField(FieldScope, Map, Set)
     * @see #collectMethod(MethodScope, Map, Set)
     */
    private <M extends MemberScope<?, ?>> void populateMemberSchema(M scope, ObjectNode targetNode, boolean isNullable, boolean forceInlineDefinition,
            ObjectNode collectedAttributes, CustomPropertyDefinitionProvider<M> ignoredDefinitionProvider) {
        final CustomDefinition customDefinition = this.generatorConfig.getCustomDefinition(scope, this, ignoredDefinitionProvider);
        if (customDefinition != null && customDefinition.isMeantToBeInline()) {
            targetNode.setAll(customDefinition.getValue());
            if (customDefinition.shouldIncludeAttributes()) {
                AttributeCollector.mergeMissingAttributes(targetNode, collectedAttributes);
                Set<String> allowedSchemaTypes = this.collectAllowedSchemaTypes(targetNode);
                ObjectNode typeAttributes = AttributeCollector.collectTypeAttributes(scope, this, allowedSchemaTypes);
                AttributeCollector.mergeMissingAttributes(targetNode, typeAttributes);
            }
            if (isNullable) {
                this.makeNullable(targetNode);
            }
        } else {
            // create an "allOf" wrapper for the attributes related to this particular field and its general type
            final ObjectNode referenceContainer;
            if (customDefinition != null && !customDefinition.shouldIncludeAttributes()
                    || collectedAttributes == null || collectedAttributes.size() == 0) {
                // no need for the allOf, can use the sub-schema instance directly as reference
                referenceContainer = targetNode;
            } else if (customDefinition == null && scope.isContainerType()) {
                // same as above, but the collected attributes should be applied also for containers/arrays
                referenceContainer = targetNode;
                AttributeCollector.mergeMissingAttributes(targetNode, collectedAttributes);
            } else {
                // avoid mixing potential "$ref" element with contextual attributes by introducing an "allOf" wrapper
                // this is only relevant for DRAFT_7 and is being cleaned-up afterwards for newer DRAFT versions
                referenceContainer = this.generatorConfig.createObjectNode();
                targetNode.set(this.getKeyword(SchemaKeyword.TAG_ALLOF), this.generatorConfig.createArrayNode()
                        .add(referenceContainer)
                        .add(collectedAttributes));
            }
            // only add reference for separate definition if it is not a fixed type that should be in-lined
            try {
                this.traverseGenericType(scope, referenceContainer, isNullable, forceInlineDefinition, null);
            } catch (UnsupportedOperationException ex) {
                logger.warn("Skipping type definition due to error", ex);
            }
        }
    }

    @Override
    public ObjectNode makeNullable(ObjectNode node) {
        final String nullTypeName = this.getKeyword(SchemaKeyword.TAG_TYPE_NULL);
        if (node.has(this.getKeyword(SchemaKeyword.TAG_REF))
                || node.has(this.getKeyword(SchemaKeyword.TAG_ALLOF))
                || node.has(this.getKeyword(SchemaKeyword.TAG_ANYOF))
                || node.has(this.getKeyword(SchemaKeyword.TAG_ONEOF))
                // since version 4.21.0
                || node.has(this.getKeyword(SchemaKeyword.TAG_CONST))
                || node.has(this.getKeyword(SchemaKeyword.TAG_ENUM))) {
            // cannot be sure what is specified in those other schema parts, instead simply create an anyOf wrapper
            ObjectNode nullSchema = this.generatorConfig.createObjectNode()
                    .put(this.getKeyword(SchemaKeyword.TAG_TYPE), nullTypeName);
            ArrayNode anyOf = this.generatorConfig.createArrayNode()
                    // one option in the anyOf should be null
                    .add(nullSchema)
                    // the other option is the given (assumed to be) not-nullable node
                    .add(this.generatorConfig.createObjectNode().setAll(node));
            // replace all existing (and already copied properties with the anyOf wrapper
            node.removeAll();
            node.set(this.getKeyword(SchemaKeyword.TAG_ANYOF), anyOf);
        } else {
            // given node is a simple schema, we can simply adjust its "type" attribute
            JsonNode fixedJsonSchemaType = node.get(this.getKeyword(SchemaKeyword.TAG_TYPE));
            if (fixedJsonSchemaType instanceof ArrayNode) {
                // there are already multiple "type" values
                ArrayNode arrayOfTypes = (ArrayNode) fixedJsonSchemaType;
                // one of the existing "type" values could be null
                boolean alreadyContainsNull = false;
                for (JsonNode arrayEntry : arrayOfTypes) {
                    alreadyContainsNull = alreadyContainsNull || nullTypeName.equals(arrayEntry.textValue());
                }

                if (!alreadyContainsNull) {
                    // null "type" was not mentioned before, to be safe we need to replace the old array and add the null entry
                    node.replace(this.getKeyword(SchemaKeyword.TAG_TYPE), this.generatorConfig.createArrayNode()
                            .addAll(arrayOfTypes)
                            .add(nullTypeName));
                }
            } else if (fixedJsonSchemaType instanceof TextNode && !nullTypeName.equals(fixedJsonSchemaType.textValue())) {
                // add null as second "type" option
                node.replace(this.getKeyword(SchemaKeyword.TAG_TYPE), this.generatorConfig.createArrayNode()
                        .add(fixedJsonSchemaType)
                        .add(nullTypeName));
            }
            // if no "type" is specified, null is allowed already
        }
        return node;
    }

    @Override
    public String getKeyword(SchemaKeyword keyword) {
        return this.generatorConfig.getKeyword(keyword);
    }
}
