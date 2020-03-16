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
import com.github.victools.jsonschema.generator.FieldScope;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    private final Map<ResolvedType, ObjectNode> definitions = new HashMap<>();
    private final Map<ResolvedType, List<ObjectNode>> references = new HashMap<>();
    private final Map<ResolvedType, List<ObjectNode>> nullableReferences = new HashMap<>();

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
     */
    public void parseType(ResolvedType type) {
        this.traverseGenericType(type, null, false);
    }

    /**
     * Add the given type's definition to this context.
     *
     * @param javaType type to which the definition belongs
     * @param definitionNode definition to remember
     * @return this context (for chaining)
     */
    SchemaGenerationContextImpl putDefinition(ResolvedType javaType, ObjectNode definitionNode) {
        this.definitions.put(javaType, definitionNode);
        return this;
    }

    /**
     * Whether this context (already) contains a definition for the specified type.
     *
     * @param javaType type to check for
     * @return whether a definition for the given type is already present
     */
    public boolean containsDefinition(ResolvedType javaType) {
        return this.definitions.containsKey(javaType);
    }

    /**
     * Retrieve the previously added definition for the specified type.
     *
     * @param javaType type for which to retrieve the stored definition
     * @return JSON schema definition (or null if none is present)
     * @see #putDefinition(ResolvedType, ObjectNode)
     */
    public ObjectNode getDefinition(ResolvedType javaType) {
        return this.definitions.get(javaType);
    }

    /**
     * Retrieve the set of all types for which a definition has been remembered in this context.
     *
     * @return types for which a definition is present
     */
    public Set<ResolvedType> getDefinedTypes() {
        return Collections.unmodifiableSet(this.definitions.keySet());
    }

    /**
     * Remember for the specified type that the given node is supposed to either include or reference the type's associated schema.
     *
     * @param javaType type for which to remember a reference
     * @param referencingNode node that should (later) include either the type's respective inline definition or a "$ref" to the definition
     * @param isNullable whether the reference may be null
     * @return this context (for chaining)
     */
    SchemaGenerationContextImpl addReference(ResolvedType javaType, ObjectNode referencingNode, boolean isNullable) {
        Map<ResolvedType, List<ObjectNode>> targetMap = isNullable ? this.nullableReferences : this.references;
        List<ObjectNode> valueList = targetMap.get(javaType);
        if (valueList == null) {
            valueList = new ArrayList<>();
            targetMap.put(javaType, valueList);
        }
        valueList.add(referencingNode);
        return this;
    }

    /**
     * Getter for the nodes representing not-nullable references to the given type.
     *
     * @param javaType target type
     * @return not-nullable nodes to be populated with the schema of the given type
     */
    public List<ObjectNode> getReferences(ResolvedType javaType) {
        return Collections.unmodifiableList(this.references.getOrDefault(javaType, Collections.emptyList()));
    }

    /**
     * Getter for the nodes representing nullable references to the given type.
     *
     * @param javaType target type
     * @return nullable nodes to be populated with the schema of the given type
     */
    public List<ObjectNode> getNullableReferences(ResolvedType javaType) {
        return Collections.unmodifiableList(this.nullableReferences.getOrDefault(javaType, Collections.emptyList()));
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
        this.traverseGenericType(targetType, definition, false, true, ignoredDefinitionProvider);
        return definition;
    }

    @Override
    public ObjectNode createStandardDefinitionReference(ResolvedType targetType, CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        ObjectNode definition = this.generatorConfig.createObjectNode();
        this.traverseGenericType(targetType, definition, false, false, ignoredDefinitionProvider);
        return definition;
    }

    /**
     * Preparation Step: add the given targetType.
     *
     * @param targetType (possibly generic) type to add
     * @param targetNode node in the JSON schema that should represent the targetType
     * @param isNullable whether the field/method's return value is allowed to be null in the declaringType in this particular scenario
     */
    protected void traverseGenericType(ResolvedType targetType, ObjectNode targetNode, boolean isNullable) {
        this.traverseGenericType(targetType, targetNode, isNullable, false, null);
    }

    /**
     * Preparation Step: add the given targetType. Also catering for forced inline-definitions and ignoring custom definitions
     *
     * @param targetType (possibly generic) type to add
     * @param targetNode node in the JSON schema that should represent the targetType
     * @param isNullable whether the field/method's return value is allowed to be null in the declaringType in this particular scenario
     * @param forceInlineDefinition whether to generate an inline definition without registering it in this context
     * @param ignoredDefinitionProvider custom definition provider to ignore
     */
    private void traverseGenericType(ResolvedType targetType, ObjectNode targetNode, boolean isNullable, boolean forceInlineDefinition,
            CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        if (!forceInlineDefinition && this.containsDefinition(targetType)) {
            logger.debug("adding reference to existing definition of {}", targetType);
            this.addReference(targetType, targetNode, isNullable);
            // nothing more to be done
            return;
        }
        final ObjectNode definition;
        boolean includeTypeAttributes = true;
        final CustomDefinition customDefinition = this.generatorConfig.getCustomDefinition(targetType, this, ignoredDefinitionProvider);
        if (customDefinition != null && customDefinition.isMeantToBeInline()) {
            if (targetNode == null) {
                logger.debug("storing configured custom inline type for {} as definition (since it is the main schema \"#\")", targetType);
                definition = customDefinition.getValue();
                this.putDefinition(targetType, definition);
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
                this.putDefinition(targetType, definition);
                if (targetNode != null) {
                    // targetNode is only null for the main class for which the schema is being generated
                    this.addReference(targetType, targetNode, isNullable);
                }
            }
            if (customDefinition != null) {
                logger.debug("applying configured custom definition for {}", targetType);
                definition.setAll(customDefinition.getValue());
            } else if (isContainerType) {
                logger.debug("generating definition for {}", targetType);
                this.generateArrayDefinition(targetType, definition, isNullable);
            } else {
                logger.debug("generating definition for {}", targetType);
                includeTypeAttributes = !this.addSubtypeReferencesInDefinition(targetType, definition);
            }
        }
        TypeScope scope = this.typeContext.createTypeScope(targetType);
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
                .forEach(override -> override.overrideTypeAttributes(definition, scope, this.generatorConfig));
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
        if (subtypes.size() == 1) {
            // avoid unnecessary "anyOf" by making the definition a direct reference to the subtype's definition
            this.traverseGenericType(subtypes.get(0), definition, false);
        } else {
            ArrayNode anyOfArrayNode = this.generatorConfig.createArrayNode();
            for (ResolvedType subtype : subtypes) {
                ObjectNode subtypeSchema = this.generatorConfig.createObjectNode();
                this.traverseGenericType(subtype, subtypeSchema, false);
                anyOfArrayNode.add(subtypeSchema);
            }
            definition.set(this.getKeyword(SchemaKeyword.TAG_ANYOF), anyOfArrayNode);
        }
        return true;
    }

    /**
     * Collect the specified value(s) from the given definition's "{@value SchemaConstants#TAG_TYPE}" attribute.
     *
     * @param definition type definition to extract specified "{@value SchemaConstants#TAG_TYPE}" values from
     * @return extracted "{@value SchemaConstants#TAG_TYPE}" values (may be empty)
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
     * @param targetType (possibly generic) array type to add
     * @param definition node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value the targetType refers to is allowed to be null in the declaring type
     */
    private void generateArrayDefinition(ResolvedType targetType, ObjectNode definition, boolean isNullable) {
        if (isNullable) {
            ArrayNode typeArray = this.generatorConfig.createArrayNode()
                    .add(this.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY))
                    .add(this.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
            definition.set(this.getKeyword(SchemaKeyword.TAG_TYPE), typeArray);
        } else {
            definition.put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY));
        }
        ObjectNode arrayItemTypeRef = this.generatorConfig.createObjectNode();
        definition.set(this.getKeyword(SchemaKeyword.TAG_ITEMS), arrayItemTypeRef);
        ResolvedType itemType = this.typeContext.getContainerItemType(targetType);
        this.traverseGenericType(itemType, arrayItemTypeRef, false);
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be anything but an array type).
     *
     * @param targetType object type to add
     * @param definition node in the JSON schema to which all collected attributes should be added
     */
    private void generateObjectDefinition(ResolvedType targetType, ObjectNode definition) {
        definition.put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));

        final Map<String, JsonNode> targetFields = new TreeMap<>();
        final Map<String, JsonNode> targetMethods = new TreeMap<>();
        final Set<String> requiredProperties = new HashSet<>();

        this.collectObjectProperties(targetType, targetFields, targetMethods, requiredProperties);

        if (!targetFields.isEmpty() || !targetMethods.isEmpty()) {
            ObjectNode propertiesNode = this.generatorConfig.createObjectNode();
            propertiesNode.setAll(targetFields);
            propertiesNode.setAll(targetMethods);
            definition.set(this.getKeyword(SchemaKeyword.TAG_PROPERTIES), propertiesNode);

            if (!requiredProperties.isEmpty()) {
                ArrayNode requiredNode = this.generatorConfig.createArrayNode();
                requiredProperties.forEach(requiredNode::add);
                definition.set(this.getKeyword(SchemaKeyword.TAG_REQUIRED), requiredNode);
            }
        }
    }

    /**
     * Recursively collect all properties of the given object type and add them to the respective maps.
     *
     * @param targetType the type for which to collect fields and methods
     * @param targetFields map of named JSON schema nodes representing individual fields
     * @param targetMethods map of named JSON schema nodes representing individual methods
     * @param requiredProperties set of properties value required
     */
    private void collectObjectProperties(ResolvedType targetType, Map<String, JsonNode> targetFields, Map<String, JsonNode> targetMethods,
            Set<String> requiredProperties) {
        logger.debug("collecting non-static fields and methods from {}", targetType);
        final ResolvedTypeWithMembers targetTypeWithMembers = this.typeContext.resolveWithMembers(targetType);
        // member fields and methods are being collected from the targeted type as well as its super types
        this.populateFields(targetTypeWithMembers, ResolvedTypeWithMembers::getMemberFields, targetFields, requiredProperties);
        this.populateMethods(targetTypeWithMembers, ResolvedTypeWithMembers::getMemberMethods, targetMethods, requiredProperties);

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
                    this.populateFields(hierarchyTypeMembers, ResolvedTypeWithMembers::getStaticFields, targetFields, requiredProperties);
                }
                if (includeStaticMethods) {
                    this.populateMethods(hierarchyTypeMembers, ResolvedTypeWithMembers::getStaticMethods, targetMethods, requiredProperties);
                }
            }
        }
    }

    /**
     * Preparation Step: add the designated fields to the specified {@link Map}.
     *
     * @param declaringTypeMembers the type declaring the fields to populate
     * @param fieldLookup retrieval function for getter targeted fields from {@code declaringTypeMembers}
     * @param collectedFields property nodes in the JSON schema to which the field sub schemas should be added
     * @param requiredProperties set of properties value required
     */
    private void populateFields(ResolvedTypeWithMembers declaringTypeMembers, Function<ResolvedTypeWithMembers, ResolvedField[]> fieldLookup,
            Map<String, JsonNode> collectedFields, Set<String> requiredProperties) {
        Stream.of(fieldLookup.apply(declaringTypeMembers))
                .map(declaredField -> this.typeContext.createFieldScope(declaredField, declaringTypeMembers))
                .filter(fieldScope -> !this.generatorConfig.shouldIgnore(fieldScope))
                .forEach(fieldScope -> this.populateField(fieldScope, collectedFields, requiredProperties));
    }

    /**
     * Preparation Step: add the designated methods to the specified {@link Map}.
     *
     * @param declaringTypeMembers the type declaring the methods to populate
     * @param methodLookup retrieval function for getter targeted methods from {@code declaringTypeMembers}
     * @param collectedMethods property nodes in the JSON schema to which the method sub schemas should be added
     * @param requiredProperties set of properties value required
     */
    private void populateMethods(ResolvedTypeWithMembers declaringTypeMembers, Function<ResolvedTypeWithMembers, ResolvedMethod[]> methodLookup,
            Map<String, JsonNode> collectedMethods, Set<String> requiredProperties) {
        Stream.of(methodLookup.apply(declaringTypeMembers))
                .map(declaredMethod -> this.typeContext.createMethodScope(declaredMethod, declaringTypeMembers))
                .filter(methodScope -> !this.generatorConfig.shouldIgnore(methodScope))
                .forEach(methodScope -> this.populateMethod(methodScope, collectedMethods, requiredProperties));
    }

    /**
     * Preparation Step: add the given field to the specified {@link Map}.
     *
     * @param field declared field that should be added to the specified node
     * @param collectedFields node in the JSON schema to which the field's sub schema should be added as property
     * @param requiredProperties set of properties value required
     */
    private void populateField(FieldScope field, Map<String, JsonNode> collectedFields, Set<String> requiredProperties) {
        String propertyNameOverride = this.generatorConfig.resolvePropertyNameOverride(field);
        FieldScope fieldWithNameOverride = propertyNameOverride == null ? field : field.withOverriddenName(propertyNameOverride);
        String propertyName = fieldWithNameOverride.getSchemaPropertyName();
        if (this.generatorConfig.isRequired(field)) {
            requiredProperties.add(propertyName);
        }
        if (collectedFields.containsKey(propertyName)) {
            logger.debug("ignoring overridden {}.{}", fieldWithNameOverride.getDeclaringType(), fieldWithNameOverride.getDeclaredName());
            return;
        }

        List<ResolvedType> typeOverrides = this.generatorConfig.resolveTargetTypeOverrides(fieldWithNameOverride);
        List<FieldScope> fieldOptions;
        if (typeOverrides == null || typeOverrides.isEmpty()) {
            fieldOptions = Collections.singletonList(fieldWithNameOverride);
        } else {
            fieldOptions = typeOverrides.stream()
                    .map(fieldWithNameOverride::withOverriddenType)
                    .collect(Collectors.toList());
        }
        // consider declared type (instead of overridden one) for determining null-ability
        boolean isNullable = !field.getRawMember().isEnumConstant() && this.generatorConfig.isNullable(field);
        if (fieldOptions.size() == 1) {
            collectedFields.put(propertyName, this.createFieldSchema(fieldOptions.get(0), isNullable));
        } else {
            ObjectNode subSchema = this.generatorConfig.createObjectNode();
            collectedFields.put(propertyName, subSchema);
            ArrayNode anyOfArray = subSchema.withArray(this.getKeyword(SchemaKeyword.TAG_ANYOF));
            if (isNullable) {
                anyOfArray.add(this.generatorConfig.createObjectNode()
                        .put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_NULL)));
            }
            fieldOptions.forEach(option -> anyOfArray.add(this.createFieldSchema(option, false)));
        }
    }

    /**
     * Preparation Step: create a node for a schema representing the given field's associated value type.
     *
     * @param field field/property to populate the schema node for
     * @param isNullable whether the field/property's value may be null
     * @return schema node representing the given field/property
     */
    private ObjectNode createFieldSchema(FieldScope field, boolean isNullable) {
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ObjectNode fieldAttributes = AttributeCollector.collectFieldAttributes(field, this);
        this.populateSchema(field.getType(), subSchema, isNullable, fieldAttributes);
        return subSchema;
    }

    /**
     * Preparation Step: add the given method to the specified {@link Map}.
     *
     * @param method declared method that should be added to the specified node
     * @param collectedMethods node in the JSON schema to which the method's (and its return value's) sub schema should be added as property
     * @param requiredProperties set of properties value required
     */
    private void populateMethod(MethodScope method, Map<String, JsonNode> collectedMethods, Set<String> requiredProperties) {
        String propertyNameOverride = this.generatorConfig.resolvePropertyNameOverride(method);
        MethodScope methodWithNameOverride = propertyNameOverride == null ? method : method.withOverriddenName(propertyNameOverride);
        String propertyName = methodWithNameOverride.getSchemaPropertyName();
        if (this.generatorConfig.isRequired(method)) {
            requiredProperties.add(propertyName);
        }
        if (collectedMethods.containsKey(propertyName)) {
            logger.debug("ignoring overridden {}.{}", methodWithNameOverride.getDeclaringType(), methodWithNameOverride.getDeclaredName());
            return;
        }

        List<ResolvedType> typeOverrides = this.generatorConfig.resolveTargetTypeOverrides(methodWithNameOverride);
        List<MethodScope> methodOptions;
        if (typeOverrides == null || typeOverrides.isEmpty()) {
            methodOptions = Collections.singletonList(methodWithNameOverride);
        } else {
            methodOptions = typeOverrides.stream()
                    .map(methodWithNameOverride::withOverriddenType)
                    .collect(Collectors.toList());
        }
        // consider declared type (instead of overridden one) for determining null-ability
        boolean isNullable = methodWithNameOverride.isVoid() || this.generatorConfig.isNullable(methodWithNameOverride);
        if (methodOptions.size() == 1) {
            collectedMethods.put(propertyName, this.createMethodSchema(methodOptions.get(0), isNullable));
        } else {
            ObjectNode subSchema = this.generatorConfig.createObjectNode();
            collectedMethods.put(propertyName, subSchema);
            ArrayNode anyOfArray = subSchema.withArray(this.getKeyword(SchemaKeyword.TAG_ANYOF));
            if (isNullable) {
                anyOfArray.add(this.generatorConfig.createObjectNode()
                        .put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_NULL)));
            }
            methodOptions.forEach(option -> anyOfArray.add(this.createMethodSchema(option, false)));
        }
    }

    /**
     * Preparation Step: create a node for a schema representing the given method's associated return type.
     *
     * @param method method to populate the schema node for
     * @param isNullable whether the method's return value may be null
     * @return schema node representing the given method's return type
     */
    private JsonNode createMethodSchema(MethodScope method, boolean isNullable) {
        if (method.isVoid()) {
            return BooleanNode.FALSE;
        }
        ObjectNode subSchema = this.generatorConfig.createObjectNode();
        ObjectNode methodAttributes = AttributeCollector.collectMethodAttributes(method, this);
        this.populateSchema(method.getType(), subSchema, isNullable, methodAttributes);
        return subSchema;
    }

    /**
     * Preparation Step: combine the collected attributes and the javaType's definition in the given targetNode.
     *
     * @param javaType field's type or method return value's type that should be represented by the given targetNode
     * @param targetNode node in the JSON schema that should represent the associated javaType and include the separately collected attributes
     * @param isNullable whether the field/method's return value the javaType refers to is allowed to be null in the declaringType
     * @param collectedAttributes separately collected attribute for the field/method in their respective declaring type
     * @see #populateField(FieldScope, Map, Set)
     * @see #populateMethod(MethodScope, Map, Set)
     */
    private void populateSchema(ResolvedType javaType, ObjectNode targetNode, boolean isNullable, ObjectNode collectedAttributes) {
        final CustomDefinition customDefinition = this.generatorConfig.getCustomDefinition(javaType, this, null);
        if (customDefinition != null && customDefinition.isMeantToBeInline()) {
            targetNode.setAll(customDefinition.getValue());
            if (collectedAttributes != null && collectedAttributes.size() > 0) {
                targetNode.setAll(collectedAttributes);
            }
            if (isNullable) {
                this.makeNullable(targetNode);
            }
        } else {
            // create an "allOf" wrapper for the attributes related to this particular field and its general type
            final ObjectNode referenceContainer;
            if (collectedAttributes == null || collectedAttributes.size() == 0) {
                // no need for the allOf, can use the sub-schema instance directly as reference
                referenceContainer = targetNode;
            } else if (customDefinition == null && this.typeContext.isContainerType(javaType)) {
                // same as above, but the collected attributes should be applied also for containers/arrays
                referenceContainer = targetNode;
                referenceContainer.setAll(collectedAttributes);
            } else {
                // avoid mixing potential "$ref" element with contextual attributes by introducing an "allOf" wrapper
                referenceContainer = this.generatorConfig.createObjectNode();
                targetNode.set(this.getKeyword(SchemaKeyword.TAG_ALLOF), this.generatorConfig.createArrayNode()
                        .add(referenceContainer)
                        .add(collectedAttributes));
            }
            // only add reference for separate definition if it is not a fixed type that should be in-lined
            try {
                this.traverseGenericType(javaType, referenceContainer, isNullable);
            } catch (UnsupportedOperationException ex) {
                logger.warn("Skipping type definition due to error", ex);
            }
        }
    }

    @Override
    public ObjectNode makeNullable(ObjectNode node) {
        if (node.has(this.getKeyword(SchemaKeyword.TAG_REF))
                || node.has(this.getKeyword(SchemaKeyword.TAG_ALLOF))
                || node.has(this.getKeyword(SchemaKeyword.TAG_ANYOF))
                || node.has(this.getKeyword(SchemaKeyword.TAG_ONEOF))) {
            // cannot be sure what is specified in those other schema parts, instead simply create a oneOf wrapper
            ObjectNode nullSchema = this.generatorConfig.createObjectNode()
                    .put(this.getKeyword(SchemaKeyword.TAG_TYPE), this.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
            ArrayNode oneOf = this.generatorConfig.createArrayNode()
                    // one option in the oneOf should be null
                    .add(nullSchema)
                    // the other option is the given (assumed to be) not-nullable node
                    .add(this.generatorConfig.createObjectNode().setAll(node));
            // replace all existing (and already copied properties with the oneOf wrapper
            node.removeAll();
            node.set(this.getKeyword(SchemaKeyword.TAG_ONEOF), oneOf);
        } else {
            // given node is a simple schema, we can simply adjust its "type" attribute
            JsonNode fixedJsonSchemaType = node.get(this.getKeyword(SchemaKeyword.TAG_TYPE));
            if (fixedJsonSchemaType instanceof ArrayNode) {
                // there are already multiple "type" values
                ArrayNode arrayOfTypes = (ArrayNode) fixedJsonSchemaType;
                // one of the existing "type" values could be null
                boolean alreadyContainsNull = false;
                for (JsonNode arrayEntry : arrayOfTypes) {
                    alreadyContainsNull = alreadyContainsNull || this.getKeyword(SchemaKeyword.TAG_TYPE_NULL).equals(arrayEntry.textValue());
                }

                if (!alreadyContainsNull) {
                    // null "type" was not mentioned before, we simply add it to the existing list
                    arrayOfTypes.add(this.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
                }
            } else if (fixedJsonSchemaType instanceof TextNode
                    && !this.getKeyword(SchemaKeyword.TAG_TYPE_NULL).equals(fixedJsonSchemaType.textValue())) {
                // add null as second "type" option
                node.replace(this.getKeyword(SchemaKeyword.TAG_TYPE), this.generatorConfig.createArrayNode()
                        .add(fixedJsonSchemaType)
                        .add(this.getKeyword(SchemaKeyword.TAG_TYPE_NULL)));
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
