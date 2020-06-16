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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility for cleaning up generated schemas.
 */
public class SchemaCleanUpUtils {

    private final SchemaGeneratorConfig config;

    /**
     * Constructor.
     *
     * @param config configuration to be applied
     */
    public SchemaCleanUpUtils(SchemaGeneratorConfig config) {
        this.config = config;
    }

    /**
     * Remove and merge {@link SchemaKeyword#TAG_ALLOF} parts when there are no conflicts between the sub-schemas.
     *
     * @param jsonSchemas generated schemas that may contain unnecessary {@link SchemaKeyword#TAG_ALLOF} nodes
     */
    public void reduceAllOfNodes(List<ObjectNode> jsonSchemas) {
        String allOfTagName = this.config.getKeyword(SchemaKeyword.TAG_ALLOF);
        this.finaliseSchemaParts(jsonSchemas, nodeToCheck -> this.mergeAllOfPartsIfPossible(nodeToCheck, allOfTagName));
    }

    /**
     * Reduce nested {@link SchemaKeyword#TAG_ANYOF} parts when one contains an entry with only another {@link SchemaKeyword#TAG_ANYOF} inside.
     *
     * @param jsonSchemas generated schemas that may contain unnecessary nested {@link SchemaKeyword#TAG_ANYOF} nodes
     */
    public void reduceAnyOfNodes(List<ObjectNode> jsonSchemas) {
        String anyOfTagName = this.config.getKeyword(SchemaKeyword.TAG_ANYOF);
        this.finaliseSchemaParts(jsonSchemas, nodeToCheck -> this.reduceAnyOfWrappersIfPossible(nodeToCheck, anyOfTagName));
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
     * @param schemaNodes generated schemas to clean-up
     * @param performCleanUpOnSingleSchemaNode clean up task to execute before looking for deeper nested sub-schemas for which to apply the same
     */
    private void finaliseSchemaParts(List<ObjectNode> schemaNodes, Consumer<ObjectNode> performCleanUpOnSingleSchemaNode) {
        List<ObjectNode> nextNodesToCheck = new ArrayList<>(schemaNodes);
        Consumer<JsonNode> addNodeToCheck = node -> {
            if (node instanceof ObjectNode) {
                nextNodesToCheck.add((ObjectNode) node);
            }
        };

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
        if ((this.config.getSchemaVersion() == SchemaVersion.DRAFT_6 || this.config.getSchemaVersion() == SchemaVersion.DRAFT_7)
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

    /**
     * Replace characters in the given definition key that are deemed incompatible within a URI (as expected by JSON Schema).
     *
     * @param definitionKey {@code SchemaDefinitionNamingStrategy} output
     * @return URI compatible version of the given definition key
     */
    public String ensureDefinitionKeyIsUriCompatible(String definitionKey) {
        return definitionKey
                // marking arrays with an asterisk instead of square brackets
                .replaceAll("\\[\\]", "*")
                // indicating generics in parentheses instead of angled brackets
                .replaceAll("<", "(")
                .replaceAll(">", ")")
                // removing white-spaces and any other remaining invalid characters
                .replaceAll("[^a-zA-Z0-9\\.\\-_\\$\\*\\(\\),]+", "");
    }

    /**
     * Replace characters in the given definition key that are neither alphanumeric nor a dot, dash or underscore (as expected by OpenAPI).
     *
     * @param definitionKey {@code SchemaDefinitionNamingStrategy} output
     * @return simplified version of the given definition key
     */
    public String ensureDefinitionKeyIsPlain(String definitionKey) {
        return definitionKey
                // avoid dollar symbols for inner types
                .replaceAll("\\$", "-")
                // marking arrays with three dots instead of square brackets
                .replaceAll("\\[\\]", "...")
                // indicating generics in underscores instead of angled brackets
                .replaceAll("[<>]", "_")
                // use dots instead of commas between type parameters
                .replaceAll(",", ".")
                // removing white-spaces and any other remaining invalid characters
                .replaceAll("[^a-zA-Z0-9\\.\\-_]+", "");
    }
}
