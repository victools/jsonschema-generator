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
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
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

        SchemaDefinitionNamingStrategy baseNamingStrategy = Optional.ofNullable(config.getDefinitionNamingStrategy())
                .orElseGet(DefaultSchemaDefinitionNamingStrategy::new);
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
        String referenceKeyPrefix = this.getReferenceKeyPrefix(definitionsTagName);
        ObjectNode definitionsNode = this.buildDefinitionsAndResolveReferences(referenceKeyPrefix, mainKey);
        if (!definitionsNode.isEmpty()) {
            jsonSchemaResult.set(definitionsTagName, definitionsNode);
        }
        if (!createDefinitionForMainSchema) {
            ObjectNode mainSchemaNode = this.generationContext.getDefinition(mainKey);
            jsonSchemaResult.setAll(mainSchemaNode);
            this.schemaNodes.add(jsonSchemaResult);
        }
        this.performCleanup(definitionsNode, referenceKeyPrefix);
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
        String referenceKeyPrefix = this.getReferenceKeyPrefix(designatedDefinitionPath);
        ObjectNode definitionsNode = this.buildDefinitionsAndResolveReferences(referenceKeyPrefix, null);
        this.performCleanup(definitionsNode, referenceKeyPrefix);
        return definitionsNode;
    }

    private String getReferenceKeyPrefix(String designatedDefinitionPath) {
        return this.config.getKeyword(SchemaKeyword.TAG_REF_MAIN) + '/' + designatedDefinitionPath + '/';
    }

    /**
     * Reduce unnecessary structures in the generated schema definitions. Assumption being that this method is being invoked as the very last action
     * of the schema generation.
     *
     * @param definitionsNode object node containing common schema definitions
     * @param referenceKeyPrefix designated prefix to the entries in the returned definitions node (i.e., on {@link SchemaKeyword#TAG_REF} values)
     *
     * @see SchemaGeneratorConfig#shouldCleanupUnnecessaryAllOfElements()
     * @see SchemaCleanUpUtils#reduceAllOfNodes(List)
     * @see SchemaCleanUpUtils#reduceAnyOfNodes(List)
     */
    private void performCleanup(ObjectNode definitionsNode, String referenceKeyPrefix) {
        SchemaCleanUpUtils cleanUpUtils = new SchemaCleanUpUtils(this.config);
        if (this.config.shouldCleanupUnnecessaryAllOfElements()) {
            cleanUpUtils.reduceAllOfNodes(this.schemaNodes);
        }
        cleanUpUtils.reduceAnyOfNodes(this.schemaNodes);
        if (this.config.shouldDiscardDuplicateMemberAttributes()) {
            cleanUpUtils.reduceRedundantMemberAttributes(this.schemaNodes, definitionsNode, referenceKeyPrefix);
        }
        if (this.config.shouldIncludeStrictTypeInfo()) {
            cleanUpUtils.setStrictTypeInfo(this.schemaNodes, true);
            // since version 4.37.0 as extraneous "anyOf" wrappers may have been introduced to support type "null"
            cleanUpUtils.reduceAnyOfNodes(this.schemaNodes);
        }
    }

    /**
     * Finalisation Step: collect the entries for the generated schema's "definitions" and ensure that all references are either pointing to the
     * appropriate definition or contain the respective (sub) schema directly inline.
     *
     * @param referenceKeyPrefix designated prefix to the entries in the returned definitions node (i.e., on {@link SchemaKeyword#TAG_REF} values)
     * @param mainSchemaKey definition key identifying the main type for which createSchemaReference() was invoked
     * @return node representing the main schema's "definitions" (may be empty)
     */
    private ObjectNode buildDefinitionsAndResolveReferences(String referenceKeyPrefix, DefinitionKey mainSchemaKey) {
        final ObjectNode definitionsNode = this.config.createObjectNode();

        final AtomicBoolean considerOnlyDirectReferences = new AtomicBoolean(false);
        Predicate<DefinitionKey> shouldProduceDefinition = this.getShouldProduceDefinitionCheck(mainSchemaKey, considerOnlyDirectReferences);
        DefinitionCollectionDetails definitionCollectionDetails = new DefinitionCollectionDetails(mainSchemaKey, referenceKeyPrefix,
                shouldProduceDefinition, definitionsNode);

        Map<DefinitionKey, String> baseReferenceKeys = this.getReferenceKeys(mainSchemaKey, shouldProduceDefinition);
        considerOnlyDirectReferences.set(true);

        for (Map.Entry<DefinitionKey, String> baseReferenceKey : baseReferenceKeys.entrySet()) {
            DefinitionKey definitionKey = baseReferenceKey.getKey();
            List<ObjectNode> references = this.generationContext.getReferences(definitionKey);
            List<ObjectNode> nullableReferences = this.generationContext.getNullableReferences(definitionKey);
            final String referenceKey = this.updateReferences(references, definitionCollectionDetails, baseReferenceKey);
            if (!nullableReferences.isEmpty()) {
                updateNullableReferences(nullableReferences, definitionCollectionDetails, referenceKey, baseReferenceKey);
            }
        }
        definitionsNode.forEach(node -> this.schemaNodes.add((ObjectNode) node));
        return definitionsNode;
    }

    private String updateReferences(List<ObjectNode> references, DefinitionCollectionDetails definitionCollectionDetails,
            Map.Entry<DefinitionKey, String> baseReferenceKey) {
        if (definitionCollectionDetails.shouldProduceDefinition(baseReferenceKey.getKey())) {
            final String referenceKey;
            if (definitionCollectionDetails.isMainSchemaKey(baseReferenceKey.getKey()) && !this.config.shouldCreateDefinitionForMainSchema()) {
                // no need to add the main schema into the definitions, unless explicitly configured to do so
                referenceKey = this.config.getKeyword(SchemaKeyword.TAG_REF_MAIN);
            } else {
                // add it to the definitions
                definitionCollectionDetails.getDefinitionsNode()
                        .set(baseReferenceKey.getValue(), this.generationContext.getDefinition(baseReferenceKey.getKey()));
                referenceKey = definitionCollectionDetails.getReferenceKey(baseReferenceKey.getValue());
            }
            references.forEach(node -> node.put(this.config.getKeyword(SchemaKeyword.TAG_REF), referenceKey));
            return referenceKey;
        }
        // in-line the sub-schema everywhere (assuming there is no complex hierarchy inside (especially no circular reference)
        ObjectNode definition = this.generationContext.getDefinition(baseReferenceKey.getKey());
        references.forEach(node -> AttributeCollector.mergeMissingAttributes(node, definition));
        return null;
    }

    private void updateNullableReferences(List<ObjectNode> nullableReferences, DefinitionCollectionDetails definitionCollectionDetails,
            String nonNullableReferenceKey, Map.Entry<DefinitionKey, String> baseReferenceKey) {
        DefinitionKey definitionKey = baseReferenceKey.getKey();
        ObjectNode definition;
        if (nonNullableReferenceKey == null) {
            definition = this.generationContext.getDefinition(definitionKey);
        } else {
            definition = this.config.createObjectNode().put(this.config.getKeyword(SchemaKeyword.TAG_REF), nonNullableReferenceKey);
        }
        this.generationContext.makeNullable(definition);
        if (this.shouldCreateNullableDefinition(this.generationContext, definitionKey, nullableReferences)) {
            String nullableDefinitionName = this.definitionNamingStrategy
                    .adjustNullableName(definitionKey, baseReferenceKey.getValue(), this.generationContext);
            definitionCollectionDetails.getDefinitionsNode().set(nullableDefinitionName, definition);
            nullableReferences.forEach(node -> node.put(this.config.getKeyword(SchemaKeyword.TAG_REF),
                    definitionCollectionDetails.getReferenceKey(nullableDefinitionName)));
        } else {
            nullableReferences.forEach(node -> AttributeCollector.mergeMissingAttributes(node, definition));
        }
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

    /**
     * Produce reusable predicate for checking whether a given type should produce an entry in the {@link SchemaKeyword#TAG_DEFINITIONS} or not.
     *
     * @param mainSchemaKey main type to consider
     * @param considerOnlyDirectReferences whether to ignore nullable references when determining about definition vs. inlining
     * @return reusable predicate
     */
    private Predicate<DefinitionKey> getShouldProduceDefinitionCheck(DefinitionKey mainSchemaKey, AtomicBoolean considerOnlyDirectReferences) {
        final boolean createDefinitionsForAll = this.config.shouldCreateDefinitionsForAllObjects();
        final boolean inlineAllSchemas = this.config.shouldInlineAllSchemas();
        return definitionKey -> {
            if (this.generationContext.shouldNeverInlineDefinition(definitionKey)) {
                // e.g. custom definition explicitly marked to always produce a definition
                return true;
            }
            if (inlineAllSchemas) {
                // global setting: always inline schemas by default
                return false;
            }
            if (createDefinitionsForAll || definitionKey.equals(mainSchemaKey)) {
                return true;
            }
            List<ObjectNode> references = this.generationContext.getReferences(definitionKey);
            if (considerOnlyDirectReferences.get() && references.isEmpty()) {
                return false;
            }
            return references.size() > 1
                    || (references.size() + this.generationContext.getNullableReferences(definitionKey).size()) > 1;
        };
    }

    /**
     * Derive the applicable keys for the collected entries for the {@link SchemaKeyword#TAG_DEFINITIONS} in the given context.
     *
     * @param mainSchemaKey special definition key for the main schema
     * @param shouldProduceDefinition filter to indicate whether a given key should be considered when determining definition names
     * @return encountered types with their corresponding reference keys
     */
    private Map<DefinitionKey, String> getReferenceKeys(DefinitionKey mainSchemaKey, Predicate<DefinitionKey> shouldProduceDefinition) {
        Function<DefinitionKey, String> definitionNameForKey = key -> this.definitionNamingStrategy.getDefinitionNameForKey(key,
                this.generationContext);
        Map<String, List<DefinitionKey>> aliases = this.generationContext.getDefinedTypes().stream()
                .collect(Collectors.groupingBy(definitionNameForKey, TreeMap::new, Collectors.toList()));
        Map<DefinitionKey, String> referenceKeys = new LinkedHashMap<>();
        for (Map.Entry<String, List<DefinitionKey>> group : aliases.entrySet()) {
            this.collectReferenceKeysFromGroup(referenceKeys, group, mainSchemaKey, shouldProduceDefinition);
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

    private void collectReferenceKeysFromGroup(Map<DefinitionKey, String> referenceKeys, Map.Entry<String, List<DefinitionKey>> group,
            DefinitionKey mainSchemaKey, Predicate<DefinitionKey> shouldProduceDefinition) {
        String baseDefinitionName = group.getKey();
        List<DefinitionKey> definitionKeys = group.getValue().stream()
                .peek(key -> referenceKeys.put(key, ""))
                .filter(shouldProduceDefinition)
                .collect(Collectors.toList());
        if (this.areDefinitionKeysDistinct(mainSchemaKey, definitionKeys)) {
            definitionKeys.forEach(key -> referenceKeys.put(key, baseDefinitionName));
        } else {
            Map<DefinitionKey, String> referenceKeyGroup = definitionKeys.stream()
                    .collect(Collectors.toMap(key -> key, _key -> baseDefinitionName, (val1, _val2) -> val1, LinkedHashMap::new));
            this.definitionNamingStrategy.adjustDuplicateNames(referenceKeyGroup, this.generationContext);
            if (definitionKeys.size() != referenceKeyGroup.size()) {
                throw new IllegalStateException(SchemaDefinitionNamingStrategy.class.getSimpleName()
                        + " of type " + this.definitionNamingStrategy.getClass().getSimpleName()
                        + " altered list of subschemas with duplicate names.");
            }
            referenceKeys.putAll(referenceKeyGroup);
        }
    }

    /**
     * Check whether the given key can be included with its standard name. Otherwise, dedicated names need to be assigned for each definition key.
     *
     * @param mainSchemaKey the main schema's key to ignore in the list if it's being in-lined (unless otherwise configured)
     * @param definitionKeys list of keys that have the same standard name in the "definitions"/"$defs"
     * @return whether the given key can be included with its standard name
     */
    private boolean areDefinitionKeysDistinct(DefinitionKey mainSchemaKey, List<DefinitionKey> definitionKeys) {
        return definitionKeys.size() == 1
               || (definitionKeys.size() == 2
                   && !this.config.shouldCreateDefinitionForMainSchema()
                   && definitionKeys.contains(mainSchemaKey));
    }

    /**
     * Helper type to group common method parameters in order to reduce overall parameter count and complexity in this class.
     */
    private static class DefinitionCollectionDetails {
        private final DefinitionKey mainSchemaKey;
        private final String referenceKeyPrefix;
        private final Predicate<DefinitionKey> shouldProduceDefinition;
        private final ObjectNode definitionsNode;

        DefinitionCollectionDetails(DefinitionKey mainSchemaKey, String referenceKeyPrefix,
                Predicate<DefinitionKey> shouldProduceDefinition, ObjectNode definitionsNode) {
            this.mainSchemaKey = mainSchemaKey;
            this.referenceKeyPrefix = referenceKeyPrefix;
            this.shouldProduceDefinition = shouldProduceDefinition;
            this.definitionsNode = definitionsNode;
        }

        boolean isMainSchemaKey(DefinitionKey key) {
            return key.equals(this.mainSchemaKey);
        }

        String getReferenceKey(String definitionName) {
            return this.referenceKeyPrefix + definitionName;
        }

        boolean shouldProduceDefinition(DefinitionKey key) {
            return this.shouldProduceDefinition.test(key);
        }

        ObjectNode getDefinitionsNode() {
            return this.definitionsNode;
        }
    }
}
