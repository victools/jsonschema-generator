/*
 * Copyright 2020 VicTools.
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.impl.SchemaCleanupUtils;
import com.github.victools.jsonschema.generator.impl.SchemaGenerationContextImpl;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builder for a single schema being generated.
 */
public class SchemaBuilder {

    /**
     * Generate an {@link ObjectNode} containing the JSON Schema representation of the given type.
     *
     * @param config configuration to be applied
     * @param typeContext type resolution/introspection context to be used during schema generation
     * @param mainTargetType type for which to generate the JSON Schema
     * @param typeParameters optional type parameters (in case of the {@code mainTargetType} being a parameterised type)
     * @return generated JSON Schema
     */
    static ObjectNode createSingleTypeSchema(SchemaGeneratorConfig config, TypeContext typeContext,
            Type mainTargetType, Type... typeParameters) {
        SchemaBuilder instance = new SchemaBuilder(config, typeContext);
        return instance.createSchemaForSingleType(mainTargetType, typeParameters);
    }

    /**
     * Initialise a multi-type schema builder.
     *
     * @param config configuration to be applied
     * @param typeContext type resolution/introspection context to be used during schema generation
     * @return builder instance
     * @see #createSchemaReference(Type, Type...) : adding a single type to the builder instance
     * @see #collectDefinitions(String) : generate an {@link ObjectNode} listing the common schema definitions
     */
    static SchemaBuilder forMultipleTypes(SchemaGeneratorConfig config, TypeContext typeContext) {
        return new SchemaBuilder(config, typeContext);
    }

    private final SchemaGeneratorConfig config;
    private final TypeContext typeContext;
    private final SchemaGenerationContextImpl generationContext;
    private final List<ObjectNode> schemaNodes;
    private final Function<String, String> definitionKeyCleanup;

    /**
     * Constructor.
     *
     * @param config configuration to be applied
     * @param typeContext type resolution/introspection context to be used during schema generation
     */
    SchemaBuilder(SchemaGeneratorConfig config, TypeContext typeContext) {
        this.config = config;
        this.typeContext = typeContext;
        this.generationContext = new SchemaGenerationContextImpl(this.config, this.typeContext);
        this.schemaNodes = new ArrayList<>();

        SchemaCleanupUtils cleanupUtils = new SchemaCleanupUtils(config);
        this.definitionKeyCleanup = config.shouldUsePlainDefinitionKeys()
                ? cleanupUtils::ensureDefinitionKeyIsPlain
                : cleanupUtils::ensureDefinitionKeyIsUriCompatible;
    }

    /**
     * Generate an {@link ObjectNode} containing the JSON Schema representation of the given type.
     *
     * @param mainTargetType type for which to generate the JSON Schema
     * @param typeParameters optional type parameters (in case of the {@code mainTargetType} being a parameterised type)
     * @return generated JSON Schema
     */
    private ObjectNode createSchemaForSingleType(Type mainTargetType, Type... typeParameters) {
        ResolvedType mainType = this.typeContext.resolve(mainTargetType, typeParameters);
        DefinitionKey mainKey = this.generationContext.parseType(mainType);

        ObjectNode jsonSchemaResult = this.config.createObjectNode();
        if (this.config.shouldIncludeSchemaVersionIndicator()) {
            jsonSchemaResult.put(this.config.getKeyword(SchemaKeyword.TAG_SCHEMA),
                    this.config.getKeyword(SchemaKeyword.TAG_SCHEMA_VALUE));
        }
        boolean createDefinitionForMainSchema = this.config.shouldCreateDefinitionForMainSchema();
        if (createDefinitionForMainSchema) {
            this.generationContext.addReference(mainType, jsonSchemaResult, null, false);
        }
        String definitionsTagName = this.config.getKeyword(SchemaKeyword.TAG_DEFINITIONS);
        ObjectNode definitionsNode = this.buildDefinitionsAndResolveReferences(definitionsTagName, mainKey, this.generationContext);
        if (definitionsNode.size() > 0) {
            jsonSchemaResult.set(definitionsTagName, definitionsNode);
        }
        if (!createDefinitionForMainSchema) {
            ObjectNode mainSchemaNode = this.generationContext.getDefinition(mainKey);
            jsonSchemaResult.setAll(mainSchemaNode);
            this.schemaNodes.add(jsonSchemaResult);
        }
        this.performCleanup();
        return jsonSchemaResult;
    }

    /**
     * Generate an {@link ObjectNode} placeholder for the given type and add all referenced/encountered types to this builder instance.
     * <br>
     * This may be invoked multiple times (even for the same type) until the schema generation is being completed via
     * {@link #collectDefinitions(String)}.
     *
     * @param targetType type for which to generate the JSON Schema placeholder
     * @param typeParameters optional type parameters (in case of the {@code mainTargetType} being a parameterised type)
     * @return JSON Schema placeholder (maybe be empty until {@link #collectDefinitions(String)} is being invoked)
     * @see #collectDefinitions(String)
     */
    public ObjectNode createSchemaReference(Type targetType, Type... typeParameters) {
        ResolvedType resolvedTargetType = this.typeContext.resolve(targetType, typeParameters);
        ObjectNode node = this.generationContext.createDefinitionReference(resolvedTargetType);
        this.schemaNodes.add(node);
        return node;
    }

    /**
     * Completing the schema generation (after {@link #createSchemaReference(Type, Type...)} was invoked for all relevant types) by creating an
     * {@link ObjectNode} containing common schema definitions.
     * <p>
     * The given definition path (e.g. {@code "definitions"}, {@code "$defs"}, {@code "components/schemas"}) will be used in generated {@code "$ref"}
     * values (e.g. {@code "#/definitions/YourType"}, {@code "#/$defs/YourType"}, {@code "#/components/schemas/YourType"}).
     * </p>
     * This should only be invoked once at the very end of the schema generation process.
     *
     * @param designatedDefinitionPath the designated path to the returned definitions node, to be used in generated references
     * @return object node containing common schema definitions
     * @see #createSchemaReference(Type, Type...)
     */
    public ObjectNode collectDefinitions(String designatedDefinitionPath) {
        ObjectNode definitionsNode = this.buildDefinitionsAndResolveReferences(designatedDefinitionPath, null, this.generationContext);
        this.performCleanup();
        return definitionsNode;
    }

    /**
     * Reduce unnecessary structures in the generated schema definitions. Assumption being that this method is being invoked as the very last action
     * of the schema generation.
     *
     * @see SchemaGeneratorConfig#shouldCleanupUnnecessaryAllOfElements()
     * @see SchemaCleanupUtils#reduceAllOfNodes(List)
     * @see SchemaCleanupUtils#reduceAnyOfNodes(List)
     */
    private void performCleanup() {
        SchemaCleanupUtils cleanUpUtils = new SchemaCleanupUtils(this.config);
        if (this.config.shouldCleanupUnnecessaryAllOfElements()) {
            cleanUpUtils.reduceAllOfNodes(this.schemaNodes);
        }
        cleanUpUtils.reduceAnyOfNodes(this.schemaNodes);
    }

    /**
     * Finalisation Step: collect the entries for the generated schema's "definitions" and ensure that all references are either pointing to the
     * appropriate definition or contain the respective (sub) schema directly inline.
     *
     * @param designatedDefinitionPath designated path to the returned definitions node (to be incorporated in {@link SchemaKeyword#TAG_REF} values)
     * @param mainSchemaKey definition key identifying the main type for which createSchemaReference() was invoked
     * @param generationContext context containing all definitions of (sub) schemas and the list of references to them
     * @return node representing the main schema's "definitions" (may be empty)
     */
    private ObjectNode buildDefinitionsAndResolveReferences(String designatedDefinitionPath, DefinitionKey mainSchemaKey,
            SchemaGenerationContextImpl generationContext) {
        ObjectNode definitionsNode = this.config.createObjectNode();
        boolean createDefinitionsForAll = this.config.shouldCreateDefinitionsForAllObjects();
        boolean createDefinitionForMainSchema = this.config.shouldCreateDefinitionForMainSchema();
        boolean inlineAllSchemas = this.config.shouldInlineAllSchemas();
        for (Map.Entry<DefinitionKey, String> entry : this.getReferenceKeys(mainSchemaKey, generationContext).entrySet()) {
            String definitionName = entry.getValue();
            DefinitionKey definitionKey = entry.getKey();
            List<ObjectNode> references = generationContext.getReferences(definitionKey);
            List<ObjectNode> nullableReferences = generationContext.getNullableReferences(definitionKey);
            final String referenceKey;
            boolean referenceInline = inlineAllSchemas
                    || (references.isEmpty() || (!createDefinitionsForAll && (references.size() + nullableReferences.size()) < 2))
                    && !definitionKey.equals(mainSchemaKey);
            if (referenceInline) {
                // it is a simple type, just in-line the sub-schema everywhere
                ObjectNode definition = generationContext.getDefinition(definitionKey);
                references.forEach(node -> AttributeCollector.mergeMissingAttributes(node, definition));
                referenceKey = null;
            } else {
                // the same sub-schema is referenced in multiple places
                if (createDefinitionForMainSchema || !definitionKey.equals(mainSchemaKey)) {
                    // add it to the definitions (unless it is the main schema that is not explicitly moved there via an Option)
                    definitionsNode.set(definitionName, generationContext.getDefinition(definitionKey));
                    referenceKey = this.config.getKeyword(SchemaKeyword.TAG_REF_MAIN) + '/' + designatedDefinitionPath + '/' + definitionName;
                } else {
                    referenceKey = this.config.getKeyword(SchemaKeyword.TAG_REF_MAIN);
                }
                references.forEach(node -> node.put(this.config.getKeyword(SchemaKeyword.TAG_REF), referenceKey));
            }
            if (!nullableReferences.isEmpty()) {
                ObjectNode definition;
                if (referenceInline) {
                    definition = generationContext.getDefinition(definitionKey);
                } else {
                    definition = this.config.createObjectNode().put(this.config.getKeyword(SchemaKeyword.TAG_REF), referenceKey);
                }
                generationContext.makeNullable(definition);
                if (!inlineAllSchemas && (createDefinitionsForAll || nullableReferences.size() > 1)) {
                    String nullableDefinitionName = definitionName + "-nullable";
                    definitionsNode.set(nullableDefinitionName, definition);
                    nullableReferences.forEach(node -> node.put(this.config.getKeyword(SchemaKeyword.TAG_REF),
                            this.config.getKeyword(SchemaKeyword.TAG_REF_MAIN) + '/' + designatedDefinitionPath + '/' + nullableDefinitionName));
                } else {
                    nullableReferences.forEach(node -> AttributeCollector.mergeMissingAttributes(node, definition));
                }
            }
        }
        definitionsNode.forEach(node -> this.schemaNodes.add((ObjectNode) node));
        return definitionsNode;
    }

    /**
     * Derive the applicable keys for the collected entries for the {@link SchemaKeyword#TAG_DEFINITIONS} in the given context.
     *
     * @param mainSchemaKey special definition key for the main schema
     * @param generationContext generation context in which all traversed types and their definitions have been collected
     * @return encountered types with their corresponding reference keys
     */
    private Map<DefinitionKey, String> getReferenceKeys(DefinitionKey mainSchemaKey, SchemaGenerationContextImpl generationContext) {
        boolean createDefinitionForMainSchema = this.config.shouldCreateDefinitionForMainSchema();
        Map<String, List<DefinitionKey>> aliases = generationContext.getDefinedTypes().stream()
                .collect(Collectors.groupingBy(this::getSchemaBaseDefinitionName, TreeMap::new, Collectors.toList()));
        Map<DefinitionKey, String> referenceKeys = new LinkedHashMap<>();
        for (Map.Entry<String, List<DefinitionKey>> group : aliases.entrySet()) {
            List<DefinitionKey> definitionKeys = group.getValue();
            if (definitionKeys.size() == 1
                    || (definitionKeys.size() == 2 && !createDefinitionForMainSchema && definitionKeys.contains(mainSchemaKey))) {
                definitionKeys.forEach(key -> referenceKeys.put(key, group.getKey()));
            } else {
                AtomicInteger counter = new AtomicInteger(0);
                definitionKeys.forEach(key -> referenceKeys.put(key, group.getKey() + "-" + counter.incrementAndGet()));
            }
        }
        return referenceKeys;
    }

    /**
     * Returns the name to be associated with an entry in the generated schema's list of {@link SchemaKeyword#TAG_DEFINITIONS}.
     * <br>
     * Beware: if multiple types have the same name, the actual key in {@link SchemaKeyword#TAG_DEFINITIONS} may have a numeric counter appended to it
     *
     * @param key the definition key to be represented in the generated schema's {@link SchemaKeyword#TAG_DEFINITIONS}
     * @return name in {@link SchemaKeyword#TAG_DEFINITIONS}
     */
    private String getSchemaBaseDefinitionName(DefinitionKey key) {
        String schemaDefinitionName = this.typeContext.getSchemaDefinitionName(key.getType());
        // ensure that the type description is converted into a compatible format
        return this.definitionKeyCleanup.apply(schemaDefinitionName);
    }
}
