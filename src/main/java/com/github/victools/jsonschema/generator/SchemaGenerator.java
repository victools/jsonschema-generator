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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.impl.SchemaGenerationContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
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
        logger.debug("begin preparation of schema generation for {}", mainTargetType);
        SchemaGenerationContext context = new SchemaGenerationContext();
        this.traverseGenericType(mainTargetType, null, null, false, Collections.emptyMap(), context);

        logger.debug("completed preparation and now continuing with actual schema generation for {}", mainTargetType);
        ObjectNode jsonSchemaResult = this.config.createObjectNode();
        jsonSchemaResult.put(SchemaConstants.TAG_SCHEMA, SchemaConstants.TAG_SCHEMA_DRAFT7);
        ObjectNode definitionsNode = this.buildDefinitionsAndResolveReferences(mainTargetType, context);
        if (definitionsNode.size() > 0) {
            logger.debug("adding \"{}\" to schema with {} entries", SchemaConstants.TAG_DEFINITIONS, definitionsNode.size());
            jsonSchemaResult.set(SchemaConstants.TAG_DEFINITIONS, definitionsNode);
        }
        logger.debug("adding attributes of targeted main type {} to schema", mainTargetType);
        ObjectNode mainSchemaNode = context.getDefinition(mainTargetType);
        jsonSchemaResult.setAll(mainSchemaNode);
        logger.trace("successfully generated JSON schema for {}\n{}", mainTargetType, jsonSchemaResult);
        return jsonSchemaResult;
    }

    /**
     * Preparation Step: add the given targetType to the generation context.
     *
     * @param targetType (possibly generic) type to add to the generation context
     * @param declaringType parent type in which the given targetType is defined as field or return value of a method (is null for the main target)
     * @param targetNode node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value is allowed to be null in the declaringType in this particular scenario
     * @param generics mapping of generic type variables to their actual types (according the type arguments on the declaring type's declaring type)
     * @param context generation context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void traverseGenericType(Type targetType, Type declaringType, ObjectNode targetNode, boolean isNullable,
            Map<String, Type> generics, SchemaGenerationContext context) {
        logger.debug("traversing {}", targetType);
        Map<String, Type> declaringTypeGenerics;
        Type actualTargetType;
        if (declaringType instanceof ParameterizedType) {
            declaringTypeGenerics = new HashMap<>();
            ParameterizedType parameterizedParent = (ParameterizedType) declaringType;
            TypeVariable<?>[] genericParams = ((Class<?>) parameterizedParent.getRawType()).getTypeParameters();
            Type[] typeArguments = parameterizedParent.getActualTypeArguments();
            for (int index = 0; index < typeArguments.length; index++) {
                Type typeArgument = ReflectionUtils.resolveGenericTypePlaceholder(typeArguments[index], generics);
                declaringTypeGenerics.put(genericParams[index].getName(), typeArgument);
            }
            logger.debug("determined {} generic parameters", declaringTypeGenerics.size());
            actualTargetType = ReflectionUtils.resolveGenericTypePlaceholder(targetType, declaringTypeGenerics);
        } else {
            declaringTypeGenerics = Collections.emptyMap();
            actualTargetType = targetType;
        }
        if (context.containsDefinition(actualTargetType)) {
            logger.debug("adding reference to existing definition of {}", actualTargetType);
            context.addReference(actualTargetType, targetNode, isNullable);
            // nothing more to be done
        } else if (ReflectionUtils.isArrayType(actualTargetType)) {
            this.traverseArrayType(actualTargetType, targetNode, isNullable, declaringTypeGenerics, context);
        } else {
            this.traverseObjectType(actualTargetType, targetNode, isNullable, declaringTypeGenerics, context);
        }
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be an array type) to the generation context.
     *
     * @param targetType (possibly generic) array type to add to the generation context
     * @param targetNode node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value the targetType refers to is allowed to be null in the declaring type
     * @param generics mapping of generic type variables to their actual types (according the declaring type's type arguments)
     * @param context generation context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void traverseArrayType(Type targetType, ObjectNode targetNode, boolean isNullable,
            Map<String, Type> generics, SchemaGenerationContext context) {
        logger.debug("traversing array type {}", targetType);
        ObjectNode definition;
        if (targetNode == null) {
            // only if the main target schema is an array, do we need to store this node as definition
            definition = this.config.createObjectNode();
            context.putDefinition(targetType, definition);
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
        logger.debug("looking-up component type of array {}", targetType);
        Type genericItemType = ReflectionUtils.getArrayComponentType(targetType);
        logger.debug("resolve (possibly) generic array component type {}", genericItemType);
        Type itemType = ReflectionUtils.resolveGenericTypePlaceholder(genericItemType, generics);
        logger.debug("resolved array component type {}", itemType);
        this.traverseGenericType(itemType, targetType, arrayItemTypeRef, false, generics, context);
    }

    /**
     * Preparation Step: add the given targetType (which was previously determined to be anything but an array type) to the generation context.
     *
     * @param targetType object type to add to the generation context
     * @param targetNode node in the JSON schema to which all collected attributes should be added
     * @param isNullable whether the field/method's return value the targetType refers to is allowed to be null in the declaring type
     * @param generics mapping of generic type variables to their actual types (according the declaring type's type arguments)
     * @param context generation context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void traverseObjectType(Type targetType, ObjectNode targetNode, boolean isNullable,
            Map<String, Type> generics, SchemaGenerationContext context) {
        logger.debug("traversing object type {}", targetType);
        CustomDefinition customDefinition = this.config.getCustomDefinition(targetType);
        if (customDefinition != null && customDefinition.isMeantToBeInline()) {
            if (targetNode == null) {
                logger.debug("storing configured custom inline type for {} as definition (since it is the main schema \"#\")", targetType);
                context.putDefinition(targetType, customDefinition.getValue());
                // targetNode will be populated at the end, in buildDefinitionsAndResolveReferences()
            } else {
                logger.debug("directly applying configured custom inline type for {}", targetType);
                targetNode.setAll(customDefinition.getValue());
            }
        } else {
            logger.debug("initialising definition for {}", targetType);
            ObjectNode definition = this.config.createObjectNode();
            context.putDefinition(targetType, definition);
            if (targetNode != null) {
                logger.debug("adding reference to new definition of {}", targetType);
                // targetNode is only null for the main class for which the schema is being generated
                context.addReference(targetType, targetNode, isNullable);
            }
            if (customDefinition == null) {
                logger.debug("generating definition for {}", targetType);
                definition.put(SchemaConstants.TAG_TYPE, SchemaConstants.TAG_TYPE_OBJECT);

                Map<String, Field> fields = new TreeMap<>();
                Map<String, Method> methods = new TreeMap<>();
                final Class<?> rawTargetType = ReflectionUtils.getRawType(targetType);
                for (Class<?> currentTargetClass = rawTargetType;
                        currentTargetClass != null && currentTargetClass != Object.class;
                        currentTargetClass = currentTargetClass.getSuperclass()) {
                    logger.debug("iterating over declared fields from {}", currentTargetClass);
                    Stream.of(currentTargetClass.getDeclaredFields())
                            .filter(declaredField -> !this.config.shouldIgnore(declaredField))
                            .forEach(declaredField -> fields.putIfAbsent(declaredField.getName(), declaredField));
                    logger.debug("iterating over declared public methods from {}", currentTargetClass);
                    Stream.of(currentTargetClass.getDeclaredMethods())
                            .filter(declaredMethod -> (declaredMethod.getModifiers() & Modifier.PUBLIC) != 0)
                            .filter(declaredMethod -> !this.config.shouldIgnore(declaredMethod))
                            .forEach(declaredMethod -> methods.putIfAbsent(declaredMethod.getName(), declaredMethod));
                }
                if (rawTargetType != null) {
                    ObjectNode targetPropertiesContainer = this.config.createObjectNode();
                    fields.values()
                            .forEach(field -> this.populateField(field, targetType, targetPropertiesContainer, generics, context));
                    methods.values()
                            .forEach(method -> this.populateMethod(method, targetType, targetPropertiesContainer, generics, context));
                    if (targetPropertiesContainer.size() > 0) {
                        definition.set(SchemaConstants.TAG_PROPERTIES, targetPropertiesContainer);
                    }
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
     * @param declaringType type to which the field belongs (possibly a sub type of the actual class it is declared in)
     * @param parentProperties node in the JSON schema to which the field's sub schema should be added as property
     * @param generics mapping of generic type variables to their actual types (according the declaring type's type arguments)
     * @param context generation context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void populateField(Field field, Type declaringType, ObjectNode parentProperties,
            Map<String, Type> generics, SchemaGenerationContext context) {
        ObjectNode subSchema = this.config.createObjectNode();
        String defaultName = field.getName();
        String propertyName = Optional.ofNullable(this.config.resolvePropertyNameOverride(field, defaultName)).orElse(defaultName);
        logger.debug("adding field \"{}\" under name \"{}\" in schema for {}", defaultName, propertyName, declaringType);
        parentProperties.set(propertyName, subSchema);

        Type fieldType = ReflectionUtils.resolveGenericTypePlaceholder(field.getGenericType(), generics);
        fieldType = Optional.ofNullable(this.config.resolveTargetTypeOverride(field, fieldType)).orElse(fieldType);
        boolean isNullable = this.config.isNullable(field);
        ObjectNode fieldAttributes = this.collectFieldAttributes(field);

        this.populateSchema(fieldType, declaringType, subSchema, isNullable, generics, fieldAttributes, context);
    }

    /**
     * Preparation Step: add the given method to the specified {@link ObjectNode}.
     *
     * @param method declared method that should be added to the specified node
     * @param declaringType type to which the method belongs (possibly a sub type of the actual class it is declared in)
     * @param parentProperties node in the JSON schema to which the method's (and its return value's) sub schema should be added as property
     * @param generics mapping of generic type variables to their actual types (according the declaring type's type arguments)
     * @param context generation context to add type definitions and their references to (to be resolved at the end of the schema generation)
     */
    private void populateMethod(Method method, Type declaringType, ObjectNode parentProperties,
            Map<String, Type> generics, SchemaGenerationContext context) {
        ObjectNode subSchema = this.config.createObjectNode();
        String defaultName = this.buildMethodName(method);
        String propertyName = Optional.ofNullable(this.config.resolvePropertyNameOverride(method, defaultName)).orElse(defaultName);
        parentProperties.set(propertyName, subSchema);

        Type returnValueType = ReflectionUtils.resolveGenericTypePlaceholder(method.getGenericReturnType(), generics);
        returnValueType = Optional.ofNullable(this.config.resolveTargetTypeOverride(method, returnValueType))
                .orElse(returnValueType);
        boolean isNullable = this.config.isNullable(method);
        ObjectNode methodAttributes = this.collectMethodAttributes(method);

        this.populateSchema(returnValueType, declaringType, subSchema, isNullable, generics, methodAttributes, context);
    }

    /**
     * Build the standard name (may be overridden) for a method, including its input parameters. The latter should ensure in most cases, that the
     * generated method name is unique within its declaring type even if it is overloaded (same name but different parameters).
     *
     * @param method method for which to build the name string, including input parameters
     * @return standard name with which to represent the given method in a generated JSON schema
     */
    private String buildMethodName(Method method) {
        return method.getName() + Stream.of(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Preparation Step: combine the collected attributes and the javaType's definition in the given targetNode.
     *
     * @param javaType field's type or method return value's type that should be represented by the given targetNode
     * @param declaringType type to which the field/method belongs (possibly a sub type of the actual class it is declared in)
     * @param targetNode node in the JSON schema that should represent the associated javaType and include the separately collected attributes
     * @param isNullable whether the field/method's return value the javaType refers to is allowed to be null in the declaringType
     * @param generics mapping of generic type variables to their actual types (according the declaring type's type arguments)
     * @param collectedAttributes separately collected attribute for the field/method in their respective declaring type
     * @param context generation context to add type definitions and their references to (to be resolved at the end of the schema generation)
     * @see #populateField(Field, Type, ObjectNode, Map, SchemaGenerationContext)
     * @see #populateMethod(Method, Type, ObjectNode, Map, SchemaGenerationContext)
     */
    private void populateSchema(Type javaType, Type declaringType, ObjectNode targetNode, boolean isNullable, Map<String, Type> generics,
            ObjectNode collectedAttributes, SchemaGenerationContext context) {
        // create an "allOf" wrapper for the attributes related to this particular field and its general type
        ObjectNode referenceContainer;
        CustomDefinition customDefinition = this.config.getCustomDefinition(javaType);
        if (collectedAttributes == null
                || collectedAttributes.size() == 0
                || (customDefinition != null && customDefinition.isMeantToBeInline())
                || javaType == void.class
                || ReflectionUtils.isArrayType(javaType)) {
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
        } else if (javaType != void.class) {
            // only add reference for separate definition if it is not a fixed type that should be in-lined
            try {
                this.traverseGenericType(javaType, declaringType, referenceContainer, isNullable, generics, context);
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
     * Collect a field's contextual attributes (i.e. everything not related to the structure).
     *
     * @param field the field for which to collect JSON schema attributes
     * @return node holding all collected attributes (possibly empty)
     */
    private ObjectNode collectFieldAttributes(Field field) {
        ObjectNode node = this.config.createObjectNode();
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_TITLE, this.config.resolveTitle(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_DESCRIPTION, this.config.resolveDescription(field));
        Collection<?> enumValues = this.config.resolveEnum(field);
        if (enumValues != null && !enumValues.isEmpty()) {
            if (enumValues.size() == 1) {
                node.putPOJO(SchemaConstants.TAG_CONST, enumValues.iterator().next());
            } else {
                ArrayNode array = this.config.createArrayNode();
                enumValues.forEach(array::addPOJO);
                node.set(SchemaConstants.TAG_ENUM, array);
            }
        }
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_LENGTH_MIN, this.config.resolveStringMinLength(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_LENGTH_MAX, this.config.resolveStringMaxLength(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_FORMAT, this.config.resolveStringFormat(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MINIMUM, this.config.resolveNumberInclusiveMinimum(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MINIMUM_EXCLUSIVE, this.config.resolveNumberExclusiveMinimum(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MAXIMUM, this.config.resolveNumberInclusiveMaximum(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MAXIMUM_EXCLUSIVE, this.config.resolveNumberExclusiveMaximum(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MULTIPLE_OF, this.config.resolveNumberMultipleOf(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_ITEMS_MIN, this.config.resolveArrayMinItems(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_ITEMS_MAX, this.config.resolveArrayMaxItems(field));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_ITEMS_UNIQUE, this.config.resolveArrayUniqueItems(field));
        return node;
    }

    /**
     * Collect a method's contextual attributes (i.e. everything not related to the structure).
     *
     * @param method the method for which to collect JSON schema attributes
     * @return node holding all collected attributes (possibly empty)
     */
    private ObjectNode collectMethodAttributes(Method method) {
        ObjectNode node = this.config.createObjectNode();
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_TITLE, this.config.resolveTitle(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_DESCRIPTION, this.config.resolveDescription(method));
        Collection<?> enumValues = this.config.resolveEnum(method);
        if (enumValues != null && !enumValues.isEmpty()) {
            if (enumValues.size() == 1) {
                node.putPOJO(SchemaConstants.TAG_CONST, enumValues.iterator().next());
            } else {
                ArrayNode array = this.config.createArrayNode();
                enumValues.forEach(array::addPOJO);
                node.set(SchemaConstants.TAG_ENUM, array);
            }
        }
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_LENGTH_MIN, this.config.resolveStringMinLength(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_LENGTH_MAX, this.config.resolveStringMaxLength(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_FORMAT, this.config.resolveStringFormat(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MINIMUM, this.config.resolveNumberInclusiveMinimum(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MINIMUM_EXCLUSIVE, this.config.resolveNumberExclusiveMinimum(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MAXIMUM, this.config.resolveNumberInclusiveMaximum(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MAXIMUM_EXCLUSIVE, this.config.resolveNumberExclusiveMaximum(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_MULTIPLE_OF, this.config.resolveNumberMultipleOf(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_ITEMS_MIN, this.config.resolveArrayMinItems(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_ITEMS_MAX, this.config.resolveArrayMaxItems(method));
        JsonNodeUtils.setAttributeIfNotNull(node, SchemaConstants.TAG_ITEMS_UNIQUE, this.config.resolveArrayUniqueItems(method));
        return node;
    }

    /**
     * Finalisation Step: collect the entries for the generated schema's "definitions" and ensure that all references are either pointing to the
     * appropriate definition or contain the respective (sub) schema directly inline.
     *
     * @param mainSchemaTarget main type for which generateSchema() was invoked
     * @param context generation context containing all definitions of (sub) schemas and the list of references to them
     * @return node representing the main schema's "definitions" (may be empty)
     */
    private ObjectNode buildDefinitionsAndResolveReferences(Class<?> mainSchemaTarget, SchemaGenerationContext context) {
        // determine short names to be used as definition names
        Map<Type, String> aliases = context.getDefinedTypes().stream()
                .collect(Collectors.toMap(key -> key,
                        key -> (key instanceof Class<?>) ? ((Class<?>) key).getSimpleName() : key.getTypeName()));
        // check if there are any duplicate aliases
        if (aliases.size() > new HashSet<>(aliases.values()).size()) {
            Predicate<Map.Entry<Type, String>> hasEntryDuplicateAlias = entry -> aliases.values().stream()
                    .filter(singleValue -> singleValue.equals(entry.getValue()))
                    .count() > 1;
            // replace duplicate aliases with full class names (which are assumed to be unique)
            aliases.entrySet().stream()
                    .filter(hasEntryDuplicateAlias)
                    .forEach(entry -> entry.setValue(entry.getKey().getTypeName()));
        }
        // create the "definitions" node with the respective aliases as keys
        ObjectNode definitionsNode = this.config.createObjectNode();
        boolean createDefinitionsForAll = this.config.shouldCreateDefinitionsForAllObjects();
        for (Map.Entry<Type, String> aliasEntry : aliases.entrySet()) {
            Type type = aliasEntry.getKey();
            List<ObjectNode> referencingNodes = context.getReferences(type);
            List<ObjectNode> nullableReferences = context.getNullableReferences(type);
            String alias = aliasEntry.getValue();
            final String referenceKey;
            boolean referenceInline = type != mainSchemaTarget
                    && (referencingNodes.isEmpty() || (!createDefinitionsForAll && (referencingNodes.size() + nullableReferences.size()) < 2));
            if (referenceInline) {
                // it is a simple type, just in-line the sub-schema everywhere
                referencingNodes.forEach(referenceNode -> referenceNode.setAll(context.getDefinition(type)));
                referenceKey = null;
            } else {
                // the same sub-schema is referenced in multiple places
                if (type == mainSchemaTarget) {
                    referenceKey = SchemaConstants.TAG_REF_MAIN;
                } else {
                    // add it to the definitions (unless it is the main schema)
                    definitionsNode.set(alias, context.getDefinition(type));
                    referenceKey = SchemaConstants.TAG_REF_PREFIX + alias;
                }
                referencingNodes.forEach(referenceNode -> referenceNode.put(SchemaConstants.TAG_REF, referenceKey));
            }
            if (!nullableReferences.isEmpty()) {
                ObjectNode definition;
                if (referenceInline) {
                    definition = context.getDefinition(type);
                } else {
                    definition = this.config.createObjectNode().put(SchemaConstants.TAG_REF, referenceKey);
                }
                this.makeNullable(definition);
                if (!createDefinitionsForAll && nullableReferences.size() < 2) {
                    nullableReferences.forEach(referenceNode -> referenceNode.setAll(definition));
                } else {
                    if (createDefinitionsForAll || nullableReferences.size() > 1) {
                        String nullableAlias = "nullable-" + alias;
                        String nullableReferenceKey = SchemaConstants.TAG_REF_PREFIX + nullableAlias;
                        definitionsNode.set(nullableAlias, definition);
                        nullableReferences.forEach(referenceNode -> referenceNode.put(SchemaConstants.TAG_REF, nullableReferenceKey));
                    } else {
                        nullableReferences.forEach(referenceNode -> referenceNode.setAll(definition));
                    }
                }
            }
        }
        return definitionsNode;
    }
}
