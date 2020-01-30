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
import com.github.victools.jsonschema.generator.impl.SchemaGenerationContextImpl;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
        generationContext.parseType(mainType);

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
     * Finalisation Step: collect the entries for the generated schema's "definitions" and ensure that all references are either pointing to the
     * appropriate definition or contain the respective (sub) schema directly inline.
     *
     * @param mainSchemaTarget main type for which generateSchema() was invoked
     * @param generationContext context containing all definitions of (sub) schemas and the list of references to them
     * @return node representing the main schema's "definitions" (may be empty)
     */
    private ObjectNode buildDefinitionsAndResolveReferences(ResolvedType mainSchemaTarget, SchemaGenerationContextImpl generationContext) {
        // determine short names to be used as definition names
        Map<String, List<ResolvedType>> aliases = generationContext.getDefinedTypes().stream()
                .collect(Collectors.groupingBy(this.typeContext::getSchemaDefinitionName, TreeMap::new, Collectors.toList()));
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
            // ensure that the type description is converted into an URI-compatible format
            final String alias = aliasEntry.getKey()
                    // removing white-spaces
                    .replaceAll("[ ]+", "")
                    // marking arrays with an asterisk instead of square brackets
                    .replaceAll("\\[\\]", "*")
                    // indicating generics in parentheses instead of angled brackets
                    .replaceAll("<", "(")
                    .replaceAll(">", ")");
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
                generationContext.makeNullable(definition);
                if (createDefinitionsForAll || nullableReferences.size() > 1) {
                    String nullableAlias = alias + "-nullable";
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
