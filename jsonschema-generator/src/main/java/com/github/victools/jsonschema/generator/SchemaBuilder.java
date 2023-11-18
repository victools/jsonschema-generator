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
import com.github.victools.jsonschema.generator.impl.SchemaCleanUpUtils;
import com.github.victools.jsonschema.generator.impl.SchemaGenerationContextImpl;
import com.github.victools.jsonschema.generator.naming.CleanSchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.generator.naming.DefaultSchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
    private final CleanSchemaDefinitionNamingStrategy definitionNamingStrategy;

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

        SchemaDefinitionNamingStrategy baseNamingStrategy = config.getDefinitionNamingStrategy();
        if (baseNamingStrategy == null) {
            baseNamingStrategy = new DefaultSchemaDefinitionNamingStrategy();
        }
        SchemaCleanUpUtils cleanupUtils = new SchemaCleanUpUtils(config);
        Function<String, String> definitionCleanUpTask = config.shouldUsePlainDefinitionKeys()
                ? cleanupUtils::ensureDefinitionKeyIsPlain
                : cleanupUtils::ensureDefinitionKeyIsUriCompatible;

        this.definitionNamingStrategy = new CleanSchemaDefinitionNamingStrategy(baseNamingStrategy, definitionCleanUpTask);
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
        this.config.resetAfterSchemaGenerationFinished();
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

        this.config.resetAfterSchemaGenerationFinished();
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
     * @see SchemaCleanUpUtils#reduceAllOfNodes(List)
     * @see SchemaCleanUpUtils#reduceAnyOfNodes(List)
     */
    private void performCleanup() {
        SchemaCleanUpUtils cleanUpUtils = new SchemaCleanUpUtils(this.config);
        if (this.config.shouldCleanupUnnecessaryAllOfElements()) {
            cleanUpUtils.reduceAllOfNodes(this.schemaNodes);
        }
        cleanUpUtils.reduceAnyOfNodes(this.schemaNodes);
        if (this.config.shouldIncludeStrictTypeInfo()) {
            cleanUpUtils.setStrictTypeInfo(this.schemaNodes, true);
        }
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
        final ObjectNode definitionsNode = this.config.createObjectNode();

        final AtomicBoolean considerOnlyDirectReferences = new AtomicBoolean(false);
        Predicate<DefinitionKey> shouldProduceDefinition = this.getShouldProduceDefinitionCheck(mainSchemaKey, considerOnlyDirectReferences);
        Map<DefinitionKey, String> baseReferenceKeys = this.getReferenceKeys(mainSchemaKey, shouldProduceDefinition, generationContext);
        considerOnlyDirectReferences.set(true);
        for (Map.Entry<DefinitionKey, String> baseReferenceKey : baseReferenceKeys.entrySet()) {
            String definitionName = baseReferenceKey.getValue();
            DefinitionKey definitionKey = baseReferenceKey.getKey();
            List<ObjectNode> references = generationContext.getReferences(definitionKey);
            List<ObjectNode> nullableReferences = generationContext.getNullableReferences(definitionKey);
            final String referenceKey;
            boolean referenceInline = !shouldProduceDefinition.test(definitionKey);
            if (referenceInline) {
                // it is a simple type, just in-line the sub-schema everywhere
                ObjectNode definition = generationContext.getDefinition(definitionKey);
                references.forEach(node -> AttributeCollector.mergeMissingAttributes(node, definition));
                referenceKey = null;
            } else {
                // the same sub-schema is referenced in multiple places
                Supplier<String> addDefinitionAndReturnReferenceKey = () -> {
                    definitionsNode.set(definitionName, this.generationContext.getDefinition(definitionKey));
                    return this.config.getKeyword(SchemaKeyword.TAG_REF_MAIN) + '/' + designatedDefinitionPath + '/' + definitionName;
                };
                referenceKey = getReferenceKey(mainSchemaKey, definitionKey, addDefinitionAndReturnReferenceKey);
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
                if (this.shouldCreateNullableDefinition(generationContext, definitionKey, nullableReferences)) {
                    String nullableDefinitionName = this.definitionNamingStrategy
                            .adjustNullableName(definitionKey, definitionName, generationContext);
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

    private boolean shouldCreateNullableDefinition(SchemaGenerationContextImpl generationContext, DefinitionKey definitionKey,
            List<ObjectNode> nullableReferences) {
        if (this.config.shouldInlineNullableSchemas()) {
            return false;
        }
        if (generationContext.shouldNeverInlineDefinition(definitionKey)) {
            return true;
        }
        if (this.config.shouldInlineAllSchemas()) {
            return false;
        }
        return this.config.shouldCreateDefinitionsForAllObjects() || nullableReferences.size() > 1;
    }

    private String getReferenceKey(DefinitionKey mainSchemaKey, DefinitionKey definitionKey, Supplier<String> addDefinitionAndReturnReferenceKey) {
        final String referenceKey;
        if (definitionKey.equals(mainSchemaKey) && !this.config.shouldCreateDefinitionForMainSchema()) {
            // no need to add the main schema into the definitions, unless explicitly configured to do so
            referenceKey = this.config.getKeyword(SchemaKeyword.TAG_REF_MAIN);
        } else {
            // add it to the definitions
            referenceKey = addDefinitionAndReturnReferenceKey.get();
        }
        return referenceKey;
    }

    /**
     * Produce reusable predicate for checking whether a given type should produce an entry in the {@link SchemaKeyword#TAG_DEFINITIONS} or not.
     *
     * @param mainSchemaKey main type to consider
     * @param considerOnlyDirectReferences whether to ignore nullable references when determing about definition vs. inlining
     * @return reusable predicate
     */
    private Predicate<DefinitionKey> getShouldProduceDefinitionCheck(DefinitionKey mainSchemaKey, AtomicBoolean considerOnlyDirectReferences) {
        final boolean createDefinitionsForAll = this.config.shouldCreateDefinitionsForAllObjects();
        final boolean inlineAllSchemas = this.config.shouldInlineAllSchemas();
        return definitionKey -> {
            if (generationContext.shouldNeverInlineDefinition(definitionKey)) {
                // e.g. custom definition explicitly marked to always produce a definition
                return true;
            }
            if (inlineAllSchemas) {
                // global setting: always inline schemas by default
                return false;
            }
            if (definitionKey.equals(mainSchemaKey)) {
                return true;
            }
            List<ObjectNode> references = generationContext.getReferences(definitionKey);
            if (considerOnlyDirectReferences.get() && references.isEmpty()) {
                return false;
            }
            if (createDefinitionsForAll || references.size() > 1) {
                return true;
            }
            List<ObjectNode> nullableReferences = generationContext.getNullableReferences(definitionKey);
            return (references.size() + nullableReferences.size()) > 1;
        };
    }

    /**
     * Derive the applicable keys for the collected entries for the {@link SchemaKeyword#TAG_DEFINITIONS} in the given context.
     *
     * @param mainSchemaKey special definition key for the main schema
     * @param shouldProduceDefinition filter to indicate whether a given key should be considered when determining definition names
     * @param generationContext generation context in which all traversed types and their definitions have been collected
     * @return encountered types with their corresponding reference keys
     */
    private Map<DefinitionKey, String> getReferenceKeys(DefinitionKey mainSchemaKey, Predicate<DefinitionKey> shouldProduceDefinition,
            SchemaGenerationContextImpl generationContext) {
        boolean createDefinitionForMainSchema = this.config.shouldCreateDefinitionForMainSchema();
        Function<DefinitionKey, String> definitionNamesForKey = key -> this.definitionNamingStrategy.getDefinitionNameForKey(key, generationContext);
        Map<String, List<DefinitionKey>> aliases = generationContext.getDefinedTypes().stream()
                .collect(Collectors.groupingBy(definitionNamesForKey, TreeMap::new, Collectors.toList()));
        Map<DefinitionKey, String> referenceKeys = new LinkedHashMap<>();
        for (Map.Entry<String, List<DefinitionKey>> group : aliases.entrySet()) {
            group.getValue().forEach(key -> referenceKeys.put(key, ""));
            List<DefinitionKey> definitionKeys = group.getValue().stream()
                    .filter(shouldProduceDefinition)
                    .collect(Collectors.toList());
            if (definitionKeys.size() == 1
                    || (definitionKeys.size() == 2 && !createDefinitionForMainSchema && definitionKeys.contains(mainSchemaKey))) {
                definitionKeys.forEach(key -> referenceKeys.put(key, group.getKey()));
            } else {
                Map<DefinitionKey, String> referenceKeyGroup = definitionKeys.stream()
                        .collect(Collectors.toMap(key -> key, _key -> group.getKey(), (val1, _val2) -> val1, LinkedHashMap::new));
                this.definitionNamingStrategy.adjustDuplicateNames(referenceKeyGroup, generationContext);
                if (definitionKeys.size() != referenceKeyGroup.size()) {
                    throw new IllegalStateException(SchemaDefinitionNamingStrategy.class.getSimpleName()
                            + " of type " + this.definitionNamingStrategy.getClass().getSimpleName()
                            + " altered list of subschemas with duplicate names.");
                }
                referenceKeys.putAll(referenceKeyGroup);
            }
        }
        String remainingDuplicateKeys = referenceKeys.values().stream()
                .filter(value -> !value.isEmpty())
                .collect(Collectors.groupingBy(key -> key, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
        if (!remainingDuplicateKeys.isEmpty()) {
            throw new IllegalStateException(SchemaDefinitionNamingStrategy.class.getSimpleName()
                    + " of type " + this.definitionNamingStrategy.getClass().getSimpleName()
                    + " produced duplicate keys: " + remainingDuplicateKeys);
        }
        return referenceKeys;
    }
}
