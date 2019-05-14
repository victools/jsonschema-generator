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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import com.github.victools.jsonschema.generator.impl.ReflectionTypeUtils;
import com.github.victools.jsonschema.generator.impl.SchemaGenerationContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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

    /**
     * Constructor.
     *
     * @param config configuration to be applied
     */
    public SchemaGenerator(SchemaGeneratorConfig config) {
        this.config = config;
    }

    /**
     * Generate a {@link JsonNode} containing the JSON Schema representation of the given type.
     *
     * @param mainTargetType type for which to generate the JSON Schema
     * @return generated JSON Schema
     */
    public JsonNode generateSchema(Class<?> mainTargetType) {
        SchemaGenerationContext generationContext = new SchemaGenerationContext();
        JavaType mainType = new JavaType(mainTargetType, TypeVariableContext.EMPTY_SCOPE);
        this.traverseGenericType(mainType, null, false, generationContext);

        ObjectNode jsonSchemaResult = this.config.createObjectNode();
        jsonSchemaResult.put(SchemaConstants.TAG_SCHEMA, SchemaConstants.TAG_SCHEMA_DRAFT7);
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
    private void traverseGenericType(JavaType targetType, ObjectNode targetNode, boolean isNullable,
            SchemaGenerationContext generationContext) {
        if (generationContext.containsDefinition(targetType)) {
            logger.debug("adding reference to existing definition of {}", targetType);
            generationContext.addReference(targetType, targetNode, isNullable);
            // nothing more to be done
        } else if (ReflectionTypeUtils.isArrayType(targetType)) {
            this.traverseArrayType(targetType, targetNode, isNullable, generationContext);
        } else {
            this.traverseObjectType(targetType, targetNode, isNullable, generationContext);
        }
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be an array type) to the generation context.
     *
     * @param targetType (possibly generic) array type to add to the generation context
     * @param targetNode node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value the targetType refers to is allowed to be null in the declaring type
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void traverseArrayType(JavaType targetType, ObjectNode targetNode, boolean isNullable,
            SchemaGenerationContext generationContext) {
        ObjectNode definition;
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
        JavaType itemType = ReflectionTypeUtils.getArrayComponentType(targetType);
        logger.debug("resolved array component type {}", itemType);
        this.traverseGenericType(itemType, arrayItemTypeRef, false, generationContext);
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be anything but an array type) to the generation context.
     *
     * @param targetType object type to add to the generation context
     * @param targetNode node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value the targetType refers to is allowed to be null in the declaring type
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void traverseObjectType(JavaType targetType, ObjectNode targetNode, boolean isNullable,
            SchemaGenerationContext generationContext) {
        CustomDefinition customDefinition = this.config.getCustomDefinition(targetType);
        if (customDefinition != null && customDefinition.isMeantToBeInline()) {
            if (targetNode == null) {
                logger.debug("storing configured custom inline type for {} as definition (since it is the main schema \"#\")", targetType);
                generationContext.putDefinition(targetType, customDefinition.getValue());
                // targetNode will be populated at the end, in buildDefinitionsAndResolveReferences()
            } else {
                logger.debug("directly applying configured custom inline type for {}", targetType);
                targetNode.setAll(customDefinition.getValue());
            }
        } else {
            ObjectNode definition = this.config.createObjectNode();
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
                TypeVariableContext targetTypeVariables;
                JavaType currentTargetType = targetType;
                do {
                    targetTypeVariables = TypeVariableContext.forType(currentTargetType);
                    final TypeVariableContext currentTypeVariableScope = targetTypeVariables;
                    final Class<?> currentTargetClass = ReflectionTypeUtils.getRawType(currentTargetType.getResolvedType());
                    logger.debug("iterating over declared fields from {}", currentTargetClass);
                    Stream.of(currentTargetClass.getDeclaredFields())
                            .filter(declaredField -> !this.config.shouldIgnore(declaredField))
                            .forEach(field -> this.populateField(field, targetFields, currentTypeVariableScope, generationContext));
                    logger.debug("iterating over declared public methods from {}", currentTargetClass);
                    Stream.of(currentTargetClass.getDeclaredMethods())
                            .filter(declaredMethod -> (declaredMethod.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC)
                            .filter(declaredMethod -> !this.config.shouldIgnore(declaredMethod))
                            .forEach(method -> this.populateMethod(method, targetMethods, currentTypeVariableScope, generationContext));
                    currentTargetType = new JavaType(currentTargetClass.getGenericSuperclass(), currentTypeVariableScope);
                } while (currentTargetType.getResolvedType() != null && currentTargetType.getResolvedType() != Object.class);
                if (!targetFields.isEmpty() || !targetMethods.isEmpty()) {
                    ObjectNode propertiesNode = this.config.createObjectNode();
                    propertiesNode.setAll(targetFields);
                    propertiesNode.setAll(targetMethods);
                    definition.set(SchemaConstants.TAG_PROPERTIES, propertiesNode);
                }
            } else {
                logger.debug("applying configured custom definition for {}", targetType);
                definition.setAll(customDefinition.getValue());
            }
        }
    }

    /**
     * Preparation Step: add the given field to the specified {@link ObjectNode}.
     *
     * @param field declared field that should be added to the specified node
     * @param parentProperties node in the JSON schema to which the field's sub schema should be added as property
     * @param typeVariables mapping of generic type variables to their actual types (according the declaring type's type arguments)
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void populateField(Field field, Map<String, JsonNode> parentProperties, TypeVariableContext typeVariables,
            SchemaGenerationContext generationContext) {
        String defaultName = field.getName();
        String propertyName = Optional.ofNullable(this.config.resolvePropertyNameOverride(field, defaultName)).orElse(defaultName);
        if (parentProperties.containsKey(propertyName)) {
            return;
        }
        ObjectNode subSchema = this.config.createObjectNode();
        parentProperties.put(propertyName, subSchema);

        JavaType fieldType = typeVariables.resolveGenericTypePlaceholder(field.getGenericType());
        fieldType = Optional.ofNullable(this.config.resolveTargetTypeOverride(field, fieldType))
                .orElse(fieldType);
        boolean isNullable = this.config.isNullable(field, fieldType);
        ObjectNode fieldAttributes = new AttributeCollector(this.config).collectFieldAttributes(field, fieldType);

        this.populateSchema(fieldType, subSchema, isNullable, fieldAttributes, generationContext);
    }

    /**
     * Preparation Step: add the given method to the specified {@link ObjectNode}.
     *
     * @param method declared method that should be added to the specified node
     * @param parentProperties node in the JSON schema to which the method's (and its return value's) sub schema should be added as property
     * @param parentTypeVariables mapping of generic type variables to their actual types (according the declaring type's type arguments)
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void populateMethod(Method method, Map<String, JsonNode> parentProperties, TypeVariableContext parentTypeVariables,
            SchemaGenerationContext generationContext) {
        TypeVariableContext extendedTypeVariables = TypeVariableContext.forMethod(method, parentTypeVariables);
        JavaType returnValueType = extendedTypeVariables.resolveGenericTypePlaceholder(method.getGenericReturnType());
        returnValueType = Optional.ofNullable(this.config.resolveTargetTypeOverride(method, returnValueType))
                .orElse(returnValueType);

        String defaultName = method.getName() + Stream.of(method.getGenericParameterTypes())
                .map(parameter -> extendedTypeVariables.resolveGenericTypePlaceholder(parameter).toString())
                .collect(Collectors.joining(", ", "(", ")"));
        String propertyName = Optional.ofNullable(this.config.resolvePropertyNameOverride(method, defaultName)).orElse(defaultName);
        if (parentProperties.containsKey(propertyName)) {
            logger.debug("ignoring {}.{}", method.getDeclaringClass(), method);
            return;
        }

        if (returnValueType.getResolvedType() == void.class || returnValueType.getResolvedType() == Void.class) {
            parentProperties.put(propertyName, BooleanNode.FALSE);
        } else {
            ObjectNode subSchema = this.config.createObjectNode();
            parentProperties.put(propertyName, subSchema);

            boolean isNullable = this.config.isNullable(method, returnValueType);
            ObjectNode methodAttributes = new AttributeCollector(this.config).collectMethodAttributes(method, returnValueType);

            this.populateSchema(returnValueType, subSchema, isNullable, methodAttributes, generationContext);
        }
    }

    /**
     * Preparation Step: combine the collected attributes and the javaType's definition in the given targetNode.
     *
     * @param javaType field's type or method return value's type that should be represented by the given targetNode
     * @param targetNode node in the JSON schema that should represent the associated javaType and include the separately collected attributes
     * @param isNullable whether the field/method's return value the javaType refers to is allowed to be null in the declaringType
     * @param collectedAttributes separately collected attribute for the field/method in their respective declaring type
     * @param generationContext context to add type definitions and their references to (to be resolved at the end of the schema generation)
     * @see #populateField(Field, Type, ObjectNode, Map, SchemaGenerationContext)
     * @see #populateMethod(Method, Type, ObjectNode, Map, SchemaGenerationContext)
     */
    private void populateSchema(JavaType javaType, ObjectNode targetNode, boolean isNullable,
            ObjectNode collectedAttributes, SchemaGenerationContext generationContext) {
        // create an "allOf" wrapper for the attributes related to this particular field and its general type
        ObjectNode referenceContainer;
        CustomDefinition customDefinition = this.config.getCustomDefinition(javaType);
        if (collectedAttributes == null
                || collectedAttributes.size() == 0
                || (customDefinition != null && customDefinition.isMeantToBeInline())
                || ReflectionTypeUtils.isArrayType(javaType)) {
            // no need for the allOf, can use the sub-schema instance directly as reference
            referenceContainer = targetNode;
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
    private ObjectNode buildDefinitionsAndResolveReferences(JavaType mainSchemaTarget, SchemaGenerationContext generationContext) {
        // determine short names to be used as definition names
        Map<String, List<JavaType>> aliases = generationContext.getDefinedTypes().stream()
                .collect(Collectors.groupingBy(type -> type.toString(), TreeMap::new, Collectors.toList()));
        // create the "definitions" node with the respective aliases as keys
        ObjectNode definitionsNode = this.config.createObjectNode();
        boolean createDefinitionsForAll = this.config.shouldCreateDefinitionsForAllObjects();
        for (Map.Entry<String, List<JavaType>> aliasEntry : aliases.entrySet()) {
            List<JavaType> types = aliasEntry.getValue();
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
