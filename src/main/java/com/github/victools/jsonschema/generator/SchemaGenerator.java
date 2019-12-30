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

package com.github.victools.jsonschema.generator;

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
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import com.github.victools.jsonschema.generator.impl.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generator for JSON Schema definitions via reflection based analysis of a given class.
 */
public class SchemaGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SchemaGenerator.class);

    private final SchemaGeneratorConfig config;
    private final TypeContext typeContext;

    /**
     * Constructor.
     *
     * @param config configuration to be applied
     */
    public SchemaGenerator(SchemaGeneratorConfig config) {
        this(config, TypeContextFactory.createDefaultTypeContext());
    }

    /**
     * Constructor.
     *
     * @param config configuration to be applied
     * @param context type resolution/introspection context to be used during schema generations (across multiple schema generations)
     */
    public SchemaGenerator(SchemaGeneratorConfig config, TypeContext context) {
        this.config = config;
        this.typeContext = context;
    }

    /**
     * Generate a {@link JsonNode} containing the JSON Schema representation of the given type.
     *
     * @param mainTargetType type for which to generate the JSON Schema
     * @param typeParameters optional type parameters (in case of the {@code mainTargetType} being a parameterised type)
     * @return generated JSON Schema
     */
    public JsonNode generateSchema(Type mainTargetType, Type... typeParameters) {
        SchemaGenerationContext generationContext = new SchemaGenerationContext(this.config, this.typeContext);
        ResolvedType mainType = generationContext.getTypeContext().resolve(mainTargetType, typeParameters);
        this.traverseGenericType(mainType, null, false, generationContext);

        ObjectNode jsonSchemaResult = this.config.createObjectNode();
        if (this.config.shouldIncludeSchemaVersionIndicator()) {
            jsonSchemaResult.put(SchemaConstants.TAG_SCHEMA, SchemaConstants.TAG_SCHEMA_DRAFT7);
        }
        ObjectNode definitionsNode = this.buildDefinitionsAndResolveReferences(mainType, generationContext);
        if (definitionsNode.size() > 0) {
            jsonSchemaResult.set(SchemaConstants.TAG_DEFINITIONS, definitionsNode);
        }
        ObjectNode mainSchemaNode = generationContext.getDefinition(mainType);
        jsonSchemaResult.setAll(mainSchemaNode);
        return jsonSchemaResult;
    }

    /**
     * Preparation Step: add the given targetType to the generation context.
     *
     * @param targetType (possibly generic) type to add to the generation context
     * @param targetNode node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value is allowed to be null in the declaringType in this particular scenario
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void traverseGenericType(ResolvedType targetType, ObjectNode targetNode, boolean isNullable,
            SchemaGenerationContext generationContext) {
        if (generationContext.containsDefinition(targetType)) {
            logger.debug("adding reference to existing definition of {}", targetType);
            generationContext.addReference(targetType, targetNode, isNullable);
            // nothing more to be done
            return;
        }
        final ObjectNode definition;
        if (generationContext.getTypeContext().isContainerType(targetType)) {
            definition = this.traverseArrayType(targetType, targetNode, isNullable, generationContext);
        } else {
            definition = this.traverseObjectType(targetType, targetNode, isNullable, generationContext);
        }
        this.config.getTypeAttributeOverrides()
                .forEach(override -> override.overrideTypeAttributes(definition, targetType, this.config));
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be an array type) to the generation context.
     *
     * @param targetType (possibly generic) array type to add to the generation context
     * @param targetNode node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value the targetType refers to is allowed to be null in the declaring type
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     * @return the created array node containing the array type's definition
     */
    private ObjectNode traverseArrayType(ResolvedType targetType, ObjectNode targetNode, boolean isNullable,
            SchemaGenerationContext generationContext) {
        final ObjectNode definition;
        if (targetNode == null) {
            // only if the main target schema is an array, do we need to store this node as definition
            definition = this.config.createObjectNode();
            generationContext.putDefinition(targetType, definition);
        } else {
            definition = targetNode;
        }
        if (isNullable) {
            definition.set(SchemaConstants.TAG_TYPE,
                    this.config.createArrayNode().add(SchemaConstants.TAG_TYPE_ARRAY).add(SchemaConstants.TAG_TYPE_NULL));
        } else {
            definition.put(SchemaConstants.TAG_TYPE, SchemaConstants.TAG_TYPE_ARRAY);
        }
        ObjectNode arrayItemTypeRef = this.config.createObjectNode();
        definition.set(SchemaConstants.TAG_ITEMS, arrayItemTypeRef);
        ResolvedType itemType = generationContext.getTypeContext().getContainerItemType(targetType);
        this.traverseGenericType(itemType, arrayItemTypeRef, false, generationContext);
        return definition;
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be anything but an array type) to the generation context.
     *
     * @param targetType object type to add to the generation context
     * @param targetNode node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value the targetType refers to is allowed to be null in the declaring type
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     * @return the created node containing the object type's definition
     */
    private ObjectNode traverseObjectType(ResolvedType targetType, ObjectNode targetNode, boolean isNullable,
            SchemaGenerationContext generationContext) {
        final ObjectNode definition;
        CustomDefinition customDefinition = this.config.getCustomDefinition(targetType, generationContext.getTypeContext());
        if (customDefinition != null && customDefinition.isMeantToBeInline()) {
            if (targetNode == null) {
                logger.debug("storing configured custom inline type for {} as definition (since it is the main schema \"#\")", targetType);
                definition = customDefinition.getValue();
                generationContext.putDefinition(targetType, definition);
                // targetNode will be populated at the end, in buildDefinitionsAndResolveReferences()
            } else {
                logger.debug("directly applying configured custom inline type for {}", targetType);
                targetNode.setAll(customDefinition.getValue());
                definition = targetNode;
            }
        } else {
            definition = this.config.createObjectNode();
            generationContext.putDefinition(targetType, definition);
            if (targetNode != null) {
                // targetNode is only null for the main class for which the schema is being generated
                generationContext.addReference(targetType, targetNode, isNullable);
            }
            if (customDefinition == null) {
                logger.debug("generating definition for {}", targetType);
                definition.put(SchemaConstants.TAG_TYPE, SchemaConstants.TAG_TYPE_OBJECT);

                final Map<String, JsonNode> targetFields = new TreeMap<>();
                final Map<String, JsonNode> targetMethods = new TreeMap<>();
                final Set<String> requiredProperties = new HashSet<>();

                this.collectObjectProperties(targetType, targetFields, targetMethods, requiredProperties, generationContext);

                if (!targetFields.isEmpty() || !targetMethods.isEmpty()) {
                    ObjectNode propertiesNode = this.config.createObjectNode();
                    propertiesNode.setAll(targetFields);
                    propertiesNode.setAll(targetMethods);
                    definition.set(SchemaConstants.TAG_PROPERTIES, propertiesNode);

                    if (!requiredProperties.isEmpty()) {
                        ArrayNode requiredNode = this.config.createArrayNode();
                        requiredProperties.forEach(requiredNode::add);
                        definition.set(SchemaConstants.TAG_REQUIRED, requiredNode);
                    }
                }
            } else {
                logger.debug("applying configured custom definition for {}", targetType);
                definition.setAll(customDefinition.getValue());
            }
        }
        return definition;
    }

    /**
     * Recursively collect all properties of the given object type and add them to the respective maps.
     *
     * @param targetType the type for which to collect fields and methods
     * @param targetFields map of named JSON schema nodes representing individual fields
     * @param targetMethods map of named JSON schema nodes representing individual methods
     * @param requiredProperties set of properties value required
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void collectObjectProperties(ResolvedType targetType, Map<String, JsonNode> targetFields, Map<String, JsonNode> targetMethods,
            Set<String> requiredProperties, SchemaGenerationContext generationContext) {
        logger.debug("collecting non-static fields and methods from {}", targetType);
        final ResolvedTypeWithMembers targetTypeWithMembers = generationContext.getTypeContext().resolveWithMembers(targetType);
        // member fields and methods are being collected from the targeted type as well as its super types
        this.populateFields(targetTypeWithMembers, ResolvedTypeWithMembers::getMemberFields, targetFields, requiredProperties, generationContext);
        this.populateMethods(targetTypeWithMembers, ResolvedTypeWithMembers::getMemberMethods, targetMethods, requiredProperties, generationContext);

        final boolean includeStaticFields = this.config.shouldIncludeStaticFields();
        final boolean includeStaticMethods = this.config.shouldIncludeStaticMethods();
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
                    hierarchyTypeMembers = generationContext.getTypeContext().resolveWithMembers(hierachyType);
                }
                if (includeStaticFields) {
                    this.populateFields(hierarchyTypeMembers, ResolvedTypeWithMembers::getStaticFields, targetFields,
                            requiredProperties, generationContext);
                }
                if (includeStaticMethods) {
                    this.populateMethods(hierarchyTypeMembers, ResolvedTypeWithMembers::getStaticMethods, targetMethods,
                            requiredProperties, generationContext);
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
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void populateFields(ResolvedTypeWithMembers declaringTypeMembers, Function<ResolvedTypeWithMembers, ResolvedField[]> fieldLookup,
            Map<String, JsonNode> collectedFields, Set<String> requiredProperties, SchemaGenerationContext generationContext) {
        Stream.of(fieldLookup.apply(declaringTypeMembers))
                .map(declaredField -> generationContext.getTypeContext().createFieldScope(declaredField, declaringTypeMembers))
                .filter(fieldScope -> !this.config.shouldIgnore(fieldScope))
                .forEach(fieldScope -> this.populateField(fieldScope, collectedFields, requiredProperties, generationContext));
    }

    /**
     * Preparation Step: add the designated methods to the specified {@link Map}.
     *
     * @param declaringTypeMembers the type declaring the methods to populate
     * @param methodLookup retrieval function for getter targeted methods from {@code declaringTypeMembers}
     * @param collectedMethods property nodes in the JSON schema to which the method sub schemas should be added
     * @param requiredProperties set of properties value required
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void populateMethods(ResolvedTypeWithMembers declaringTypeMembers, Function<ResolvedTypeWithMembers, ResolvedMethod[]> methodLookup,
            Map<String, JsonNode> collectedMethods, Set<String> requiredProperties, SchemaGenerationContext generationContext) {
        Stream.of(methodLookup.apply(declaringTypeMembers))
                .map(declaredMethod -> generationContext.getTypeContext().createMethodScope(declaredMethod, declaringTypeMembers))
                .filter(methodScope -> !this.config.shouldIgnore(methodScope))
                .forEach(methodScope -> this.populateMethod(methodScope, collectedMethods, requiredProperties, generationContext));
    }

    /**
     * Preparation Step: add the given field to the specified {@link Map}.
     *
     * @param field declared field that should be added to the specified node
     * @param collectedFields node in the JSON schema to which the field's sub schema should be added as property
     * @param requiredProperties set of properties value required
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void populateField(FieldScope field, Map<String, JsonNode> collectedFields, Set<String> requiredProperties,
            SchemaGenerationContext generationContext) {
        String propertyNameOverride = this.config.resolvePropertyNameOverride(field);
        FieldScope fieldWithOverride = propertyNameOverride == null ? field : field.withOverriddenName(propertyNameOverride);
        String propertyName = fieldWithOverride.getSchemaPropertyName();
        if (this.config.isRequired(field)) {
            requiredProperties.add(propertyName);
        }
        if (collectedFields.containsKey(propertyName)) {
            logger.debug("ignoring overridden {}.{}", fieldWithOverride.getDeclaringType(), fieldWithOverride.getDeclaredName());
            return;
        }
        ObjectNode subSchema = this.config.createObjectNode();
        collectedFields.put(propertyName, subSchema);

        ResolvedType typeOverride = this.config.resolveTargetTypeOverride(fieldWithOverride);
        fieldWithOverride = typeOverride == null ? fieldWithOverride : fieldWithOverride.withOverriddenType(typeOverride);

        ObjectNode fieldAttributes = AttributeCollector.collectFieldAttributes(fieldWithOverride, this.config);

        // consider declared type (instead of overridden one) for determining null-ability
        boolean isNullable = !fieldWithOverride.getRawMember().isEnumConstant() && this.config.isNullable(fieldWithOverride);
        this.populateSchema(fieldWithOverride.getType(), subSchema, isNullable, fieldAttributes, generationContext);
    }

    /**
     * Preparation Step: add the given method to the specified {@link Map}.
     *
     * @param method declared method that should be added to the specified node
     * @param collectedMethods node in the JSON schema to which the method's (and its return value's) sub schema should be added as property
     * @param requiredProperties set of properties value required
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void populateMethod(MethodScope method, Map<String, JsonNode> collectedMethods, Set<String> requiredProperties,
            SchemaGenerationContext generationContext) {
        String propertyNameOverride = this.config.resolvePropertyNameOverride(method);
        MethodScope methodWithOverride = propertyNameOverride == null ? method : method.withOverriddenName(propertyNameOverride);
        String propertyName = methodWithOverride.getSchemaPropertyName();
        if (this.config.isRequired(method)) {
            requiredProperties.add(propertyName);
        }
        if (collectedMethods.containsKey(propertyName)) {
            logger.debug("ignoring overridden {}.{}", methodWithOverride.getDeclaringType(), methodWithOverride.getDeclaredName());
            return;
        }

        ResolvedType typeOverride = this.config.resolveTargetTypeOverride(methodWithOverride);
        methodWithOverride = typeOverride == null ? methodWithOverride : methodWithOverride.withOverriddenType(typeOverride);

        if (methodWithOverride.isVoid()) {
            collectedMethods.put(propertyName, BooleanNode.FALSE);
        } else {
            ObjectNode subSchema = this.config.createObjectNode();
            collectedMethods.put(propertyName, subSchema);

            ObjectNode methodAttributes = AttributeCollector.collectMethodAttributes(methodWithOverride, this.config);

            // consider declared type (instead of overridden one) for determining null-ability
            boolean isNullable = this.config.isNullable(methodWithOverride);
            this.populateSchema(methodWithOverride.getType(), subSchema, isNullable, methodAttributes, generationContext);
        }
    }

    /**
     * Preparation Step: combine the collected attributes and the javaType's definition in the given targetNode.
     *
     * @param javaType field's type or method return value's type that should be represented by the given targetNode
     * @param targetNode node in the JSON schema that should represent the associated javaType and include the separately collected attributes
     * @param isNullable whether the field/method's return value the javaType refers to is allowed to be null in the declaringType
     * @param collectedAttributes separately collected attribute for the field/method in their respective declaring type
     * @param generationContext   context to add type definitions and their references to (to be resolved at the end of the schema generation)
     * @see #populateField(FieldScope, Map, Set, SchemaGenerationContext)
     * @see #populateMethod(MethodScope, Map, Set, SchemaGenerationContext)
     */
    private void populateSchema(ResolvedType javaType, ObjectNode targetNode, boolean isNullable,
            ObjectNode collectedAttributes, SchemaGenerationContext generationContext) {
        // create an "allOf" wrapper for the attributes related to this particular field and its general type
        ObjectNode referenceContainer;
        CustomDefinition customDefinition = this.config.getCustomDefinition(javaType, generationContext.getTypeContext());
        if (collectedAttributes == null
                || collectedAttributes.size() == 0
                || (customDefinition != null && customDefinition.isMeantToBeInline())) {
            // no need for the allOf, can use the sub-schema instance directly as reference
            referenceContainer = targetNode;
        } else if (generationContext.getTypeContext().isContainerType(javaType)) {
            // same as above, but the collected attributes should be applied also for containers/arrays
            referenceContainer = targetNode;
            referenceContainer.setAll(collectedAttributes);
        } else {
            // avoid mixing potential "$ref" element with contextual attributes by introducing an "allOf" wrapper
            referenceContainer = this.config.createObjectNode();
            targetNode.set(SchemaConstants.TAG_ALLOF, this.config.createArrayNode()
                    .add(referenceContainer)
                    .add(collectedAttributes));
        }
        if (customDefinition != null && customDefinition.isMeantToBeInline()) {
            referenceContainer.setAll(customDefinition.getValue());
            if (collectedAttributes != null && collectedAttributes.size() > 0) {
                referenceContainer.setAll(collectedAttributes);
            }
            if (isNullable) {
                this.makeNullable(referenceContainer);
            }
        } else {
            // only add reference for separate definition if it is not a fixed type that should be in-lined
            try {
                this.traverseGenericType(javaType, referenceContainer, isNullable, generationContext);
            } catch (UnsupportedOperationException ex) {
                logger.warn("Skipping type definition due to error", ex);
            }
        }
    }

    /**
     * Ensure that the JSON schema represented by the given node allows for it to be of "type" "null".
     *
     * @param node representation of a JSON schema (part) that should allow a value of "type" "null"
     */
    private void makeNullable(ObjectNode node) {
        if (node.has(SchemaConstants.TAG_REF)
                || node.has(SchemaConstants.TAG_ALLOF)
                || node.has(SchemaConstants.TAG_ANYOF)
                || node.has(SchemaConstants.TAG_ONEOF)) {
            // cannot be sure what is specified in those other schema parts, instead simply create a oneOf wrapper
            ObjectNode nullSchema = this.config.createObjectNode().put(SchemaConstants.TAG_TYPE, SchemaConstants.TAG_TYPE_NULL);
            ArrayNode oneOf = this.config.createArrayNode()
                    // one option in the oneOf should be null
                    .add(nullSchema)
                    // the other option is the given (assumed to be) not-nullable node
                    .add(this.config.createObjectNode().setAll(node));
            // replace all existing (and already copied properties with the oneOf wrapper
            node.removeAll();
            node.set(SchemaConstants.TAG_ONEOF, oneOf);
        } else {
            // given node is a simple schema, we can simply adjust its "type" attribute
            JsonNode fixedJsonSchemaType = node.get(SchemaConstants.TAG_TYPE);
            if (fixedJsonSchemaType instanceof ArrayNode) {
                // there are already multiple "type" values
                ArrayNode arrayOfTypes = (ArrayNode) fixedJsonSchemaType;
                // one of the existing "type" values could be null
                boolean alreadyContainsNull = false;
                for (JsonNode arrayEntry : arrayOfTypes) {
                    alreadyContainsNull = alreadyContainsNull || SchemaConstants.TAG_TYPE_NULL.equals(arrayEntry.textValue());
                }

                if (!alreadyContainsNull) {
                    // null "type" was not mentioned before, we simply add it to the existing list
                    arrayOfTypes.add(SchemaConstants.TAG_TYPE_NULL);
                }
            } else if (fixedJsonSchemaType instanceof TextNode && !SchemaConstants.TAG_TYPE_NULL.equals(fixedJsonSchemaType.textValue())) {
                // add null as second "type" option
                node.replace(SchemaConstants.TAG_TYPE, this.config.createArrayNode().add(fixedJsonSchemaType).add(SchemaConstants.TAG_TYPE_NULL));
            }
            // if no "type" is specified, null is allowed already
        }
    }

    /**
     * Finalisation Step: collect the entries for the generated schema's "definitions" and ensure that all references are either pointing to the
     * appropriate definition or contain the respective (sub) schema directly inline.
     *
     * @param mainSchemaTarget main type for which generateSchema() was invoked
     * @param generationContext context containing all definitions of (sub) schemas and the list of references to them
     * @return node representing the main schema's "definitions" (may be empty)
     */
    private ObjectNode buildDefinitionsAndResolveReferences(ResolvedType mainSchemaTarget, SchemaGenerationContext generationContext) {
        // determine short names to be used as definition names
        Map<String, List<ResolvedType>> aliases = generationContext.getDefinedTypes().stream()
                .collect(Collectors.groupingBy(generationContext.getTypeContext()::getSchemaDefinitionName, TreeMap::new, Collectors.toList()));
        // create the "definitions" node with the respective aliases as keys
        ObjectNode definitionsNode = this.config.createObjectNode();
        boolean createDefinitionsForAll = this.config.shouldCreateDefinitionsForAllObjects();
        for (Map.Entry<String, List<ResolvedType>> aliasEntry : aliases.entrySet()) {
            List<ResolvedType> types = aliasEntry.getValue();
            List<ObjectNode> referencingNodes = types.stream()
                    .flatMap(type -> generationContext.getReferences(type).stream())
                    .collect(Collectors.toList());
            List<ObjectNode> nullableReferences = types.stream()
                    .flatMap(type -> generationContext.getNullableReferences(type).stream())
                    .collect(Collectors.toList());
            String alias = aliasEntry.getKey();
            final String referenceKey;
            boolean referenceInline = !types.contains(mainSchemaTarget)
                    && (referencingNodes.isEmpty() || (!createDefinitionsForAll && (referencingNodes.size() + nullableReferences.size()) < 2));
            if (referenceInline) {
                // it is a simple type, just in-line the sub-schema everywhere
                referencingNodes.forEach(referenceNode -> referenceNode.setAll(generationContext.getDefinition(types.get(0))));
                referenceKey = null;
            } else {
                // the same sub-schema is referenced in multiple places
                if (types.contains(mainSchemaTarget)) {
                    referenceKey = SchemaConstants.TAG_REF_MAIN;
                } else {
                    // add it to the definitions (unless it is the main schema)
                    definitionsNode.set(alias, generationContext.getDefinition(types.get(0)));
                    referenceKey = SchemaConstants.TAG_REF_PREFIX + alias;
                }
                referencingNodes.forEach(referenceNode -> referenceNode.put(SchemaConstants.TAG_REF, referenceKey));
            }
            if (!nullableReferences.isEmpty()) {
                ObjectNode definition;
                if (referenceInline) {
                    definition = generationContext.getDefinition(types.get(0));
                } else {
                    definition = this.config.createObjectNode().put(SchemaConstants.TAG_REF, referenceKey);
                }
                this.makeNullable(definition);
                if (createDefinitionsForAll || nullableReferences.size() > 1) {
                    String nullableAlias = alias + " (nullable)";
                    String nullableReferenceKey = SchemaConstants.TAG_REF_PREFIX + nullableAlias;
                    definitionsNode.set(nullableAlias, definition);
                    nullableReferences.forEach(referenceNode -> referenceNode.put(SchemaConstants.TAG_REF, nullableReferenceKey));
                } else {
                    nullableReferences.forEach(referenceNode -> referenceNode.setAll(definition));
                }
            }
        }
        return definitionsNode;
    }
}
