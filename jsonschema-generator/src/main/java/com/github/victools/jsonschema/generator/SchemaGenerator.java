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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.impl.SchemaGenerationContextImpl;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
        ObjectNode definitionsNode = this.buildDefinitionsAndResolveReferences(mainKey, generationContext);
        if (definitionsNode.size() > 0) {
            jsonSchemaResult.set(this.config.getKeyword(SchemaKeyword.TAG_DEFINITIONS), definitionsNode);
        }
        ObjectNode mainSchemaNode = generationContext.getDefinition(mainKey);
        jsonSchemaResult.setAll(mainSchemaNode);
        if (this.config.shouldCleanupUnnecessaryAllOfElements()) {
            String allOfTagName = this.config.getKeyword(SchemaKeyword.TAG_ALLOF);
            this.finaliseSchemaParts(jsonSchemaResult, nodeToCheck -> this.mergeAllOfPartsIfPossible(nodeToCheck, allOfTagName));
        }
        String anyOfTagName = this.config.getKeyword(SchemaKeyword.TAG_ANYOF);
        this.finaliseSchemaParts(jsonSchemaResult, nodeToCheck -> this.reduceAnyOfWrappersIfPossible(nodeToCheck, anyOfTagName));

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
                if (mainSchemaKey.equals(definitionKey)) {
                    referenceKey = this.config.getKeyword(SchemaKeyword.TAG_REF_MAIN);
                } else {
                    // add it to the definitions (unless it is the main schema)
                    definitionsNode.set(definitionName, generationContext.getDefinition(definitionKey));
                    referenceKey = this.config.getKeyword(SchemaKeyword.TAG_REF_PREFIX) + definitionName;
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
        Map<String, List<DefinitionKey>> aliases = generationContext.getDefinedTypes().stream()
                .collect(Collectors.groupingBy(this::getSchemaBaseDefinitionName, TreeMap::new, Collectors.toList()));
        Map<DefinitionKey, String> referenceKeys = new LinkedHashMap<>();
        for (Map.Entry<String, List<DefinitionKey>> group : aliases.entrySet()) {
            List<DefinitionKey> definitionKeys = group.getValue();
            if (definitionKeys.size() == 1 || (definitionKeys.size() == 2 && definitionKeys.contains(mainSchemaKey))) {
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

    /**
     * Collect names of schema tags that may contain sub-schemas, i.e. {@link SchemaKeyword#TAG_ADDITIONAL_PROPERTIES} and
     * {@link SchemaKeyword#TAG_ITEMS}.
     *
     * @return names of eligible tags as per the designated JSON Schema version
     * @see #discardUnnecessaryAllOfWrappers(ObjectNode)
     */
    private Set<String> getTagNamesContainingSchema() {
        SchemaVersion schemaVersion = this.config.getSchemaVersion();
        return Stream.of(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES, SchemaKeyword.TAG_ITEMS)
                .map(keyword -> keyword.forVersion(schemaVersion))
                .collect(Collectors.toSet());
    }

    /**
     * Collect names of schema tags that may contain arrays of sub-schemas, i.e. {@link SchemaKeyword#TAG_ALLOF}, {@link SchemaKeyword#TAG_ANYOF} and
     * {@link SchemaKeyword#TAG_ONEOF}.
     *
     * @return names of eligible tags as per the designated JSON Schema version
     * @see #discardUnnecessaryAllOfWrappers(ObjectNode)
     */
    private Set<String> getTagNamesContainingSchemaArray() {
        SchemaVersion schemaVersion = this.config.getSchemaVersion();
        return Stream.of(SchemaKeyword.TAG_ALLOF, SchemaKeyword.TAG_ANYOF, SchemaKeyword.TAG_ONEOF)
                .map(keyword -> keyword.forVersion(schemaVersion))
                .collect(Collectors.toSet());
    }

    /**
     * Collect names of schema tags that may contain objects with sub-schemas as values, i.e. {@link SchemaKeyword#TAG_PATTERN_PROPERTIES} and
     * {@link SchemaKeyword#TAG_PROPERTIES}.
     *
     * @return names of eligible tags as per the designated JSON Schema version
     * @see #discardUnnecessaryAllOfWrappers(ObjectNode)
     */
    private Set<String> getTagNamesContainingSchemaObject() {
        SchemaVersion schemaVersion = this.config.getSchemaVersion();
        return Stream.of(SchemaKeyword.TAG_PATTERN_PROPERTIES, SchemaKeyword.TAG_PROPERTIES)
                .map(keyword -> keyword.forVersion(schemaVersion))
                .collect(Collectors.toSet());
    }

    /**
     * Iterate through a generated and fully populated schema and remove extraneous {@link SchemaKeyword#TAG_ANYOF} nodes, where one entry of the
     * array is again a {@link SchemaKeyword#TAG_ANYOF} wrapper and nothing else. This makes for more readable schemas being generated but has the
     * side-effect that any manually added {@link SchemaKeyword#TAG_ANYOF} (e.g. through a custom definition of attribute overrides) may be removed as
     * well if it isn't strictly speaking necessary.
     *
     * @param schemaNode generated schema to clean-up
     * @param performCleanUpOnSingleSchemaNode clean up task to execute before looking for deeper nested sub-schemas for which to apply the same
     */
    private void finaliseSchemaParts(ObjectNode schemaNode, Consumer<ObjectNode> performCleanUpOnSingleSchemaNode) {
        List<ObjectNode> nextNodesToCheck = new ArrayList<>();
        Consumer<JsonNode> addNodeToCheck = node -> {
            if (node instanceof ObjectNode) {
                nextNodesToCheck.add((ObjectNode) node);
            }
        };
        nextNodesToCheck.add(schemaNode);
        Optional.ofNullable(schemaNode.get(this.config.getKeyword(SchemaKeyword.TAG_DEFINITIONS)))
                .filter(definitions -> definitions instanceof ObjectNode)
                .ifPresent(definitions -> ((ObjectNode) definitions).forEach(addNodeToCheck));

        Set<String> tagsWithSchemas = this.getTagNamesContainingSchema();
        Set<String> tagsWithSchemaArrays = this.getTagNamesContainingSchemaArray();
        Set<String> tagsWithSchemaObjects = this.getTagNamesContainingSchemaObject();
        do {
            List<ObjectNode> currentNodesToCheck = new ArrayList<>(nextNodesToCheck);
            nextNodesToCheck.clear();
            for (ObjectNode nodeToCheck : currentNodesToCheck) {
                performCleanUpOnSingleSchemaNode.accept(nodeToCheck);
                tagsWithSchemas.stream().map(nodeToCheck::get).forEach(addNodeToCheck);
                tagsWithSchemaArrays.stream()
                        .map(nodeToCheck::get)
                        .filter(possibleArrayNode -> possibleArrayNode instanceof ArrayNode)
                        .forEach(arrayNode -> arrayNode.forEach(addNodeToCheck));
                tagsWithSchemaObjects.stream()
                        .map(nodeToCheck::get)
                        .filter(possibleObjectNode -> possibleObjectNode instanceof ObjectNode)
                        .forEach(objectNode -> objectNode.forEach(addNodeToCheck));
            }
        } while (!nextNodesToCheck.isEmpty());
    }

    /**
     * Check whether the given schema node and its {@link SchemaKeyword#TAG_ALLOF} elements (if there are any) are distinct. If yes, remove the
     * {@link SchemaKeyword#TAG_ALLOF} node and merge all its elements with the given schema node instead.
     * <br>
     * This makes for more readable schemas being generated but has the side-effect that manually added {@link SchemaKeyword#TAG_ALLOF} (e.g. from a
     * custom definition or attribute overrides) may be removed as well if it isn't strictly speaking necessary.
     *
     * @param schemaNode single node representing a sub-schema to consolidate contained {@link SchemaKeyword#TAG_ALLOF} for (if present)
     * @param allOfTagName name of the {@link SchemaKeyword#TAG_ALLOF} in the designated JSON Schema version
     */
    private void mergeAllOfPartsIfPossible(JsonNode schemaNode, String allOfTagName) {
        if (!(schemaNode instanceof ObjectNode)) {
            return;
        }
        JsonNode allOfTag = schemaNode.get(allOfTagName);
        if (!(allOfTag instanceof ArrayNode)) {
            return;
        }
        allOfTag.forEach(part -> this.mergeAllOfPartsIfPossible(part, allOfTagName));

        List<JsonNode> allOfElements = new ArrayList<>();
        allOfTag.forEach(allOfElements::add);
        if (allOfElements.stream().anyMatch(part -> !(part instanceof ObjectNode) && !part.asBoolean())) {
            return;
        }
        List<ObjectNode> parts = allOfElements.stream()
                .filter(part -> part instanceof ObjectNode)
                .map(part -> (ObjectNode) part)
                .collect(Collectors.toList());

        // collect all defined attributes from the separate parts and check whether there are incompatible differences
        Map<String, List<JsonNode>> fieldsFromAllParts = Stream.concat(Stream.of(schemaNode), parts.stream())
                .flatMap(part -> StreamSupport.stream(((Iterable<Map.Entry<String, JsonNode>>) () -> part.fields()).spliterator(), false))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        if (this.config.getSchemaVersion() == SchemaVersion.DRAFT_7
                && fieldsFromAllParts.containsKey(this.config.getKeyword(SchemaKeyword.TAG_REF))) {
            // in Draft 7, any other attributes besides the $ref keyword were ignored
            return;
        }
        String ifTagName = this.config.getKeyword(SchemaKeyword.TAG_IF);
        for (Map.Entry<String, List<JsonNode>> fieldEntries : fieldsFromAllParts.entrySet()) {
            if (fieldEntries.getValue().size() == 1) {
                // no conflicts, no further checks
                continue;
            }
            if (ifTagName.equals(fieldEntries.getKey())) {
                // "if"/"then"/"else" tags should remain isolated in their sub-schemas
                return;
            }
            int offset;
            if (!allOfTagName.equals(fieldEntries.getKey())) {
                offset = 0;
            } else if (fieldEntries.getValue().size() == 2) {
                // we can ignore the "allOf" tag in the target node (the one we are trying to remove here)
                continue;
            } else {
                offset = 1;
            }
            if (!fieldEntries.getValue().stream().skip(offset + 1).allMatch(fieldEntries.getValue().get(offset)::equals)) {
                // different values for the same tag: be conservative for now and not merge anything
                // later, we may want to decide based on the tag how to merge them (e.g. take the highest "minLength" and the lowest "maximum")
                return;
            }
        }
        // all attributes are either distinct or have equal values in all occurrences
        ObjectNode schemaObjectNode = (ObjectNode) schemaNode;
        schemaObjectNode.remove(allOfTagName);
        parts.forEach(schemaObjectNode::setAll);
    }

    /**
     * Check whether the given schema node contains a {@link SchemaKeyword#TAG_ANYOF} element which in turn contains an entry with only another
     * {@link SchemaKeyword#TAG_ANYOF} inside. If yes, move the entries from the inner array up to the outer one.
     * <br>
     * This makes for more readable schemas being generated but has the side-effect that manually added {@link SchemaKeyword#TAG_ANYOF} entries (e.g.
     * from a custom definition or attribute overrides) may be removed as well if it isn't strictly speaking necessary.
     *
     * @param schemaNode single node representing a sub-schema to consolidate contained {@link SchemaKeyword#TAG_ANYOF} for (if present)
     * @param anyOfTagName name of the {@link SchemaKeyword#TAG_ANYOF} in the designated JSON Schema version
     */
    private void reduceAnyOfWrappersIfPossible(JsonNode schemaNode, String anyOfTagName) {
        if (!(schemaNode instanceof ObjectNode)) {
            return;
        }
        JsonNode anyOfTag = schemaNode.get(anyOfTagName);
        if (!(anyOfTag instanceof ArrayNode)) {
            return;
        }
        anyOfTag.forEach(part -> this.reduceAnyOfWrappersIfPossible(part, anyOfTagName));

        for (int index = anyOfTag.size() - 1; index > -1; index--) {
            JsonNode arrayEntry = anyOfTag.get(index);
            if (!(arrayEntry instanceof ObjectNode) || arrayEntry.size() != 1) {
                continue;
            }
            JsonNode nestedAnyOf = arrayEntry.get(anyOfTagName);
            if (!(nestedAnyOf instanceof ArrayNode)) {
                continue;
            }
            ((ArrayNode) anyOfTag).remove(index);
            for (int nestedEntryIndex = nestedAnyOf.size() - 1; nestedEntryIndex > -1; nestedEntryIndex--) {
                ((ArrayNode) anyOfTag).insert(index, nestedAnyOf.get(nestedEntryIndex));
            }
        }
    }
}
