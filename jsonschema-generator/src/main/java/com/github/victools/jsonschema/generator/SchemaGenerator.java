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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.impl.SchemaCleanUpUtils;
import com.github.victools.jsonschema.generator.impl.SchemaGenerationContextImpl;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Generator for JSON Schema definitions via reflection based analysis of a given class.
 */
public class SchemaGenerator {

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
        SchemaGenerationContextImpl generationContext = new SchemaGenerationContextImpl(this.config, this.typeContext);
        ResolvedType mainType = this.typeContext.resolve(mainTargetType, typeParameters);
        DefinitionKey mainKey = generationContext.parseType(mainType);

        ObjectNode jsonSchemaResult = this.config.createObjectNode();
        if (this.config.shouldIncludeSchemaVersionIndicator()) {
            jsonSchemaResult.put(this.config.getKeyword(SchemaKeyword.TAG_SCHEMA),
                    this.config.getKeyword(SchemaKeyword.TAG_SCHEMA_VALUE));
        }
        boolean createDefinitionForMainSchema = this.config.shouldCreateDefinitionForMainSchema();
        if (createDefinitionForMainSchema) {
            generationContext.addReference(mainType, jsonSchemaResult, null, false);
        }
        ObjectNode definitionsNode = this.buildDefinitionsAndResolveReferences(mainKey, generationContext);
        if (definitionsNode.size() > 0) {
            jsonSchemaResult.set(this.config.getKeyword(SchemaKeyword.TAG_DEFINITIONS), definitionsNode);
        }
        if (!createDefinitionForMainSchema) {
            ObjectNode mainSchemaNode = generationContext.getDefinition(mainKey);
            jsonSchemaResult.setAll(mainSchemaNode);
        }
        SchemaCleanUpUtils cleanUpUtils = new SchemaCleanUpUtils(this.config);
        if (this.config.shouldCleanupUnnecessaryAllOfElements()) {
            cleanUpUtils.reduceAllOfNodes(jsonSchemaResult);
        }
        cleanUpUtils.reduceAnyOfNodes(jsonSchemaResult);

        return jsonSchemaResult;
    }

    /**
     * Finalisation Step: collect the entries for the generated schema's "definitions" and ensure that all references are either pointing to the
     * appropriate definition or contain the respective (sub) schema directly inline.
     *
     * @param mainSchemaKey definition key identifying the main type for which generateSchema() was invoked
     * @param generationContext context containing all definitions of (sub) schemas and the list of references to them
     * @return node representing the main schema's "definitions" (may be empty)
     */
    private ObjectNode buildDefinitionsAndResolveReferences(DefinitionKey mainSchemaKey, SchemaGenerationContextImpl generationContext) {
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
                    && !mainSchemaKey.equals(definitionKey);
            if (referenceInline) {
                // it is a simple type, just in-line the sub-schema everywhere
                ObjectNode definition = generationContext.getDefinition(definitionKey);
                references.forEach(node -> AttributeCollector.mergeMissingAttributes(node, definition));
                referenceKey = null;
            } else {
                // the same sub-schema is referenced in multiple places
                if (createDefinitionForMainSchema || !mainSchemaKey.equals(definitionKey)) {
                    // add it to the definitions (unless it is the main schema that is not explicitly moved there via an Option)
                    definitionsNode.set(definitionName, generationContext.getDefinition(definitionKey));
                    referenceKey = this.config.getKeyword(SchemaKeyword.TAG_REF_PREFIX) + definitionName;
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
                            this.config.getKeyword(SchemaKeyword.TAG_REF_PREFIX) + nullableDefinitionName));
                } else {
                    nullableReferences.forEach(node -> AttributeCollector.mergeMissingAttributes(node, definition));
                }
            }
        }
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
        // ensure that the type description is converted into an URI-compatible format
        String uriCompatibleName = schemaDefinitionName
                // removing white-spaces
                .replaceAll("[ ]+", "")
                // marking arrays with an asterisk instead of square brackets
                .replaceAll("\\[\\]", "*")
                // indicating generics in parentheses instead of angled brackets
                .replaceAll("<", "(")
                .replaceAll(">", ")");
        return uriCompatibleName;
    }
}
