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
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
     * Remove and merge {@link SchemaKeyword#TAG_ALLOF} parts when there are no conflicts between the sub-schemas. This makes for more readable
     * schemas being generated but has the side-effect that any manually added {@link SchemaKeyword#TAG_ALLOF} (e.g. through a custom definition of
     * attribute overrides) may be removed as well if it isn't strictly speaking necessary.
     *
     * @param jsonSchemas generated schemas that may contain unnecessary {@link SchemaKeyword#TAG_ALLOF} nodes
     */
    public void reduceAllOfNodes(List<ObjectNode> jsonSchemas) {
        String allOfTagName = this.config.getKeyword(SchemaKeyword.TAG_ALLOF);
        Map<String, SchemaKeyword> reverseTagMap = SchemaKeyword.getReverseTagMap(this.config.getSchemaVersion(), _tag -> true);
        this.finaliseSchemaParts(jsonSchemas, nodeToCheck -> this.mergeAllOfPartsIfPossible(nodeToCheck, allOfTagName, reverseTagMap));
    }

    /**
     * Reduce nested {@link SchemaKeyword#TAG_ANYOF} parts when one contains an entry with only another {@link SchemaKeyword#TAG_ANYOF} inside. This
     * makes for more readable schemas being generated but has the side-effect that any manually added {@link SchemaKeyword#TAG_ANYOF} (e.g. through a
     * custom definition of attribute overrides) may be removed as well if it isn't strictly speaking necessary.
     *
     * @param jsonSchemas generated schemas that may contain unnecessary nested {@link SchemaKeyword#TAG_ANYOF} nodes
     */
    public void reduceAnyOfNodes(List<ObjectNode> jsonSchemas) {
        String anyOfTagName = this.config.getKeyword(SchemaKeyword.TAG_ANYOF);
        this.finaliseSchemaParts(jsonSchemas, nodeToCheck -> this.reduceAnyOfWrappersIfPossible(nodeToCheck, anyOfTagName));
    }

    /**
     * Go through all sub-schemas and look for those without a {@link SchemaKeyword#TAG_TYPE} indication. Then try to derive the appropriate type
     * indication from the other present tags (e.g., "properties" implies it is an "object").
     *
     * @param jsonSchemas sub-schemas to check and extend where required and possible
     * @param considerNullType whether to always include "null" as possible "type" in addition to the implied values
     *
     * @since 4.30.0
     */
    public void setStrictTypeInfo(List<ObjectNode> jsonSchemas, boolean considerNullType) {
        String typeTagName = this.config.getKeyword(SchemaKeyword.TAG_TYPE);
        Map<String, SchemaKeyword> reverseTagMap = SchemaKeyword.getReverseTagMap(this.config.getSchemaVersion(),
                tag -> !tag.getImpliedTypes().isEmpty());
        this.finaliseSchemaParts(jsonSchemas, nodeToCheck -> this.addTypeInfoWhereMissing(nodeToCheck, typeTagName, considerNullType, reverseTagMap));
    }

    /**
     * Collect names of schema tags that may contain the given type of content.
     *
     * @param contentType targeted type of content that can be expected under a returned tag
     * @return names of eligible tags as per the designated JSON Schema version
     */
    private Set<String> getTagNamesSupporting(SchemaKeyword.TagContent contentType) {
        return SchemaKeyword.getReverseTagMap(this.config.getSchemaVersion(), tag -> tag.supportsContentType(contentType)).keySet();
    }

    /**
     * Iterate through a generated and fully populated schema and perform the provided clean-up remove extraneous {@link SchemaKeyword#TAG_ANYOF}
     * nodes, where one entry of the array is again a {@link SchemaKeyword#TAG_ANYOF} wrapper and nothing else. This makes for more readable schemas
     * being generated but has the side-effect that any manually added {@link SchemaKeyword#TAG_ANYOF} (e.g. through a custom definition of attribute
     * overrides) may be removed as well if it isn't strictly speaking necessary.
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

        Set<String> tagsWithSchemas = this.getTagNamesSupporting(SchemaKeyword.TagContent.SCHEMA);
        Set<String> tagsWithSchemaArrays = this.getTagNamesSupporting(SchemaKeyword.TagContent.ARRAY_OF_SCHEMAS);
        Set<String> tagsWithSchemaObjects = this.getTagNamesSupporting(SchemaKeyword.TagContent.NAMED_SCHEMAS);
        do {
            List<ObjectNode> currentNodesToCheck = new ArrayList<>(nextNodesToCheck);
            nextNodesToCheck.clear();
            for (ObjectNode nodeToCheck : currentNodesToCheck) {
                performCleanUpOnSingleSchemaNode.accept(nodeToCheck);
                tagsWithSchemas.stream().map(nodeToCheck::get).forEach(addNodeToCheck);
                tagsWithSchemaArrays.stream()
                        .map(nodeToCheck::get)
                        .filter(ArrayNode.class::isInstance)
                        .forEach(arrayNode -> arrayNode.forEach(addNodeToCheck));
                tagsWithSchemaObjects.stream()
                        .map(nodeToCheck::get)
                        .filter(ObjectNode.class::isInstance)
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
     * @param reverseKeywordMap mapping from actual tag name in generated schema to underlying {@link SchemaKeyword}
     */
    private void mergeAllOfPartsIfPossible(JsonNode schemaNode, String allOfTagName, Map<String, SchemaKeyword> reverseKeywordMap) {
        if (!(schemaNode instanceof ObjectNode)) {
            return;
        }
        ObjectNode schemaObjectNode = (ObjectNode) schemaNode;
        JsonNode allOfTag = schemaObjectNode.get(allOfTagName);
        if (!(allOfTag instanceof ArrayNode)) {
            return;
        }
        allOfTag.forEach(part -> this.mergeAllOfPartsIfPossible(part, allOfTagName, reverseKeywordMap));

        List<JsonNode> allParts = new ArrayList<>(1 + allOfTag.size());
        allParts.add(schemaObjectNode);
        allOfTag.forEach(allParts::add);
        Supplier<ObjectNode> successfulMergeResultSupplier = this.mergeSchemas(schemaObjectNode, allParts, reverseKeywordMap);
        if (successfulMergeResultSupplier == null) {
            return;
        }
        // all attributes are either distinct or have equal values in all occurrences
        schemaObjectNode.remove(allOfTagName);
        schemaObjectNode.setAll(successfulMergeResultSupplier.get());
    }

    private Supplier<? extends JsonNode> getAllOfMergeFunctionFor(SchemaKeyword keyword, List<JsonNode> valuesToMerge,
            Map<String, SchemaKeyword> reverseKeywordMap) {
        if (valuesToMerge.size() == 1) {
            // no conflicts, no further checks
            return () -> valuesToMerge.get(0);
        }
        switch (keyword) {
        case TAG_ALLOF:
        case TAG_REQUIRED:
            return this.mergeArrays(valuesToMerge);
        case TAG_PROPERTIES:
        case TAG_DEPENDENT_SCHEMAS:
            if (this.config.getSchemaVersion() == SchemaVersion.DRAFT_6 || this.config.getSchemaVersion() == SchemaVersion.DRAFT_7) {
                // in Draft 6 and Draft 7, the "dependencies" keyword was covering both "dependentSchemas" and "dependentRequired" scenarios
                return Optional.ofNullable(this.mergeDependentRequiredNode(valuesToMerge))
                        .orElseGet(() -> this.mergeObjectProperties(valuesToMerge));
            } else {
                return this.mergeObjectProperties(valuesToMerge);
            }
        case TAG_DEPENDENT_REQUIRED:
            return this.mergeDependentRequiredNode(valuesToMerge);
        case TAG_ITEMS:
        case TAG_UNEVALUATED_ITEMS:
        case TAG_ADDITIONAL_PROPERTIES:
        case TAG_UNEVALUATED_PROPERTIES:
            return this.mergeSchemas(null, valuesToMerge, reverseKeywordMap);
        case TAG_TYPE:
            return this.returnOverlapOfStringsOrStringArrays(valuesToMerge);
        case TAG_ITEMS_MAX:
        case TAG_PROPERTIES_MAX:
        case TAG_MAXIMUM:
        case TAG_MAXIMUM_EXCLUSIVE:
        case TAG_LENGTH_MAX:
            return this.returnMinimumNumericValue(valuesToMerge);
        case TAG_ITEMS_MIN:
        case TAG_PROPERTIES_MIN:
        case TAG_MINIMUM:
        case TAG_MINIMUM_EXCLUSIVE:
        case TAG_LENGTH_MIN:
            return this.returnMaximumNumericValue(valuesToMerge);
        default:
            return this.returnOneIfAllEqual(valuesToMerge);
        }
    }

    private Supplier<JsonNode> mergeArrays(List<JsonNode> arrayNodesToMerge) {
        if (!arrayNodesToMerge.stream().allMatch(JsonNode::isArray)) {
            // at least one value is not an array as expected, abort merge
            return null;
        }
        return () -> {
            ArrayNode mergedArrayNode = this.config.createArrayNode();
            arrayNodesToMerge.forEach(node -> node.forEach(mergedArrayNode::add));
            return mergedArrayNode;
        };
    }

    private Supplier<JsonNode> mergeObjectProperties(List<JsonNode> objectNodesToMerge) {
        if (!objectNodesToMerge.stream().allMatch(JsonNode::isObject)) {
            // at least one value is not an object as expected, abort merge
            return null;
        }
        ObjectNode mergedObjectNode = this.config.createObjectNode();
        for (JsonNode singleObjectNode : objectNodesToMerge) {
            Iterator<Map.Entry<String, JsonNode>> it = singleObjectNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> singleField = it.next();
                if (!mergedObjectNode.has(singleField.getKey())) {
                    mergedObjectNode.set(singleField.getKey(), singleField.getValue());
                } else if (!mergedObjectNode.get(singleField.getKey()).equals(singleField.getValue())) {
                    // cannot consolidate two occurrences of the same property; abort merge (in the future: may want to be smarter here)
                    return null;
                }
            }
        }
        return () -> mergedObjectNode;
    }

    private Supplier<JsonNode> mergeDependentRequiredNode(List<JsonNode> dependentRequiredNodesToMerge) {
        if (!dependentRequiredNodesToMerge.stream().allMatch(JsonNode::isObject)) {
            // at least one value is not an object as expected, abort merge
            return null;
        }
        Map<String, Set<String>> mergedDependentRequiredNames = new LinkedHashMap<>();
        for (JsonNode singleDependentRequired : dependentRequiredNodesToMerge) {
            Iterator<Map.Entry<String, JsonNode>> it = singleDependentRequired.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> singleLeadingField = it.next();
                if (!singleLeadingField.getValue().isArray()) {
                    // cannot consolidate when anything but an array of other property names is being provided
                    return null;
                } else {
                    Set<String> propertyNames = mergedDependentRequiredNames.computeIfAbsent(singleLeadingField.getKey(),
                            (name) -> new LinkedHashSet<>());
                    Iterator<JsonNode> propertyNameIt = singleLeadingField.getValue().elements();
                    while (propertyNameIt.hasNext()) {
                        JsonNode propertyName = propertyNameIt.next();
                        if (!propertyName.isTextual()) {
                            // cannot consolidate when array contains anything but plain property names
                            return null;
                        }
                        propertyNames.add(propertyName.asText());
                    }
                }
            }
        }
        // merging is possible, now build corresponding object node
        return () -> {
            ObjectNode mergedDependentRequiredNode = this.config.createObjectNode();
            mergedDependentRequiredNames.forEach((leadName, dependentNames) -> dependentNames
                    .forEach(mergedDependentRequiredNode.withArray(leadName)::add));
            return mergedDependentRequiredNode;
        };
    }

    /**
     * Determine whether a given list of sub-schema nodes can be merged into a single schema node. If yes, providing a supplier that performs the
     * actual consolidation once called.
     *
     * @param mainNodeIncludingAllOf the node containing the "allOf" tag to be merged (may be {@code null} if all nodes have the same weight)
     * @param nodes full list of nodes to consider merging (including optional "mainNodeIncludingAllOf")
     * @param reverseKeywordMap look-up map from tag's name in given node to the known keyword it represents (passed in for improved performance)
     * @return supplier of the successfully merged schemas (into a new node) or {@code null} if merging the given nodes is not easily possible
     */
    private Supplier<ObjectNode> mergeSchemas(ObjectNode mainNodeIncludingAllOf, List<JsonNode> nodes, Map<String, SchemaKeyword> reverseKeywordMap) {
        if (nodes.stream().anyMatch(part -> !(part instanceof ObjectNode) && !(part.isBoolean() && part.asBoolean()))) {
            return null;
        }
        List<ObjectNode> parts = nodes.stream()
                .filter(ObjectNode.class::isInstance)
                .map(ObjectNode.class::cast)
                .collect(Collectors.toList());

        // collect all defined attributes from the separate parts and check whether there are incompatible differences
        Map<String, List<JsonNode>> fieldsFromAllParts = parts.stream()
                .flatMap(part -> StreamSupport.stream(((Iterable<Map.Entry<String, JsonNode>>) part::fields).spliterator(), false))
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        if ((this.config.getSchemaVersion() == SchemaVersion.DRAFT_6 || this.config.getSchemaVersion() == SchemaVersion.DRAFT_7)
                && fieldsFromAllParts.containsKey(this.config.getKeyword(SchemaKeyword.TAG_REF))
                && (mainNodeIncludingAllOf == null ? (parts.size() > 1) : (mainNodeIncludingAllOf.size() > 1 || parts.size() > 2))) {
            // in Draft 7, any other attributes besides the $ref keyword were ignored
            return null;
        }
        Map<String, List<JsonNode>> unsupportedTagValues = fieldsFromAllParts.entrySet().stream()
                .filter(entry -> !reverseKeywordMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, SchemaCleanUpUtils::throwingMerger, LinkedHashMap::new));
        if (unsupportedTagValues.entrySet().stream().anyMatch(entry -> entry.getValue().size() > 1)) {
            // unsupported tag with more than one occurrence: be conservative and don't merge
            return null;
        }
        Map<SchemaKeyword, Supplier<? extends JsonNode>> supportedTagValueSuppliers = this.collectSupportedTagValueSuppliers(fieldsFromAllParts,
                reverseKeywordMap, mainNodeIncludingAllOf);
        if (supportedTagValueSuppliers == null) {
            // unable to merge the values for some keyword; abort merge
            return null;
        }
        // all attributes are either distinct or have equal values in all occurrences
        return () -> {
            ObjectNode mergedNode = this.config.createObjectNode();
            // for the supported tags, there may be multiple occurrences that requiring individual merging
            supportedTagValueSuppliers.forEach((keyword, valueSupplier) -> mergedNode.set(this.config.getKeyword(keyword), valueSupplier.get()));
            // for the unsupported tags, there is only a single occurrence each
            unsupportedTagValues.forEach((tagName, valueList) -> mergedNode.set(tagName, valueList.get(0)));
            return mergedNode;
        };
    }

    private Map<SchemaKeyword, Supplier<? extends JsonNode>> collectSupportedTagValueSuppliers(Map<String, List<JsonNode>> fieldsFromAllParts,
            Map<String, SchemaKeyword> reverseKeywordMap, ObjectNode mainNodeIncludingAllOf) {
        Map<SchemaKeyword, List<JsonNode>> supportedTagValues = fieldsFromAllParts.entrySet().stream()
                .filter(entry -> reverseKeywordMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> reverseKeywordMap.get(entry.getKey()), Map.Entry::getValue,
                        SchemaCleanUpUtils::throwingMerger, LinkedHashMap::new));
        if (supportedTagValues.containsKey(SchemaKeyword.TAG_IF)) {
            // "if"/"then"/"else" tags should remain isolated in their sub-schemas
            return null;
        }
        Map<SchemaKeyword, Supplier<? extends JsonNode>> supportedTagValueSuppliers = new LinkedHashMap<>();
        for (Map.Entry<SchemaKeyword, List<JsonNode>> fieldEntries : supportedTagValues.entrySet()) {
            SchemaKeyword keyword = fieldEntries.getKey();
            List<JsonNode> valuesToMerge = fieldEntries.getValue();
            if (keyword == SchemaKeyword.TAG_ALLOF && mainNodeIncludingAllOf != null) {
                // we can ignore the "allOf" tag in the target node (the one we are trying to remove here)
                valuesToMerge = valuesToMerge.subList(1, valuesToMerge.size());
                if (valuesToMerge.isEmpty()) {
                    // no other "allOf" part left to merge
                    continue;
                }
            }
            Supplier<? extends JsonNode> mergeResultSupplier = this.getAllOfMergeFunctionFor(keyword, valuesToMerge, reverseKeywordMap);
            if (mergeResultSupplier == null) {
                // unable to merge given values for the specified keyword; abort merge
                return null;
            }
            supportedTagValueSuppliers.put(keyword, mergeResultSupplier);
        }
        return supportedTagValueSuppliers;
    }

    private Supplier<JsonNode> returnOverlapOfStringsOrStringArrays(List<JsonNode> nodes) {
        List<String> encounteredValues = this.getStringValuesFromStringOrStringArray(nodes.get(0));
        if (encounteredValues == null) {
            return null;
        }
        for (JsonNode nextNode : nodes.subList(1, nodes.size())) {
            List<String> nextValues = this.getStringValuesFromStringOrStringArray(nextNode);
            if (nextValues == null) {
                // invalid node; abort merge
                return null;
            }
            if (encounteredValues.size() > 1) {
                encounteredValues.retainAll(nextValues);
                if (encounteredValues.isEmpty()) {
                    // invalid merge result: no valid value remained; abort merge
                    return null;
                }
            } else if (!nextValues.contains(encounteredValues.get(0))) {
                // invalid merge result: no valid value remained; abort merge
                return null;
            }
        }
        if (encounteredValues.size() == 1) {
            return () -> new TextNode(encounteredValues.get(0));
        }
        return () -> this.config.createArrayNode()
                .addAll(encounteredValues.stream().map(TextNode::new).collect(Collectors.toList()));
    }

    private List<String> getStringValuesFromStringOrStringArray(JsonNode node) {
        if (node.isArray()) {
            List<String> result = new ArrayList<>();
            node.forEach(arrayItem -> result.add(arrayItem.asText(null)));
            if (result.contains(null)) {
                return null;
            }
            return result;
        }
        if (node.isTextual()) {
            return Collections.singletonList(node.asText());
        }
        // neither array nor text node; abort merge
        return null;
    }

    private Supplier<JsonNode> returnMinimumNumericValue(List<JsonNode> nodes) {
        if (nodes.stream().allMatch(JsonNode::isNumber)) {
            return () -> nodes.stream().reduce((a, b) -> a.asDouble() < b.asDouble() ? a : b).orElse(null);
        }
        return null;
    }

    private Supplier<JsonNode> returnMaximumNumericValue(List<JsonNode> nodes) {
        if (nodes.stream().allMatch(JsonNode::isNumber)) {
            return () -> nodes.stream().reduce((a, b) -> a.asDouble() < b.asDouble() ? b : a).orElse(null);
        }
        return null;
    }

    private Supplier<JsonNode> returnOneIfAllEqual(List<JsonNode> nodes) {
        JsonNode firstNode = nodes.get(0);
        if (nodes.subList(1, nodes.size()).stream().allMatch(firstNode::equals)) {
            return () -> firstNode;
        }
        return null;
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
     * Add the {@link SchemaKeyword#TAG_TYPE} where it is missing and it can be implied from other present tags.
     *
     * @param schemaNode sub-schema to check and extend, if required and possible
     * @param typeTagName name of the "type" tag
     * @param considerNullType whether to always include "null" as possible "type" in addition to the implied values
     * @param reverseTagMap mapping from tag name in the produced schema to their corresponding {@link SchemaKeyword} value
     */
    private void addTypeInfoWhereMissing(ObjectNode schemaNode, String typeTagName, boolean considerNullType,
            Map<String, SchemaKeyword> reverseTagMap) {
        if (schemaNode.has(typeTagName)) {
            // explicit type indication is already present
            return;
        }
        List<String> impliedTypes = reverseTagMap.entrySet().stream()
                .filter(entry -> schemaNode.has(entry.getKey()))
                .flatMap(entry -> entry.getValue().getImpliedTypes().stream())
                .distinct()
                .sorted()
                .map(SchemaKeyword.SchemaType::getSchemaKeywordValue)
                .collect(Collectors.toList());
        if (impliedTypes.isEmpty()) {
            return;
        }
        if (considerNullType) {
            impliedTypes.add(SchemaKeyword.SchemaType.NULL.getSchemaKeywordValue());
        }
        if (impliedTypes.size() == 1) {
            schemaNode.put(typeTagName, impliedTypes.get(0));
        } else {
            impliedTypes.forEach(schemaNode.putArray(typeTagName)::add);
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

    /**
     * Helper function, that represents a BinaryOperator for use in Collectors.toMap() that assumes that there are no duplicate keys.
     *
     * @param <T> key value type
     * @param one first key occurrence
     * @param two second key occurrence
     * @return nothing, as this always throws an IllegalStateException when invoked
     */
    private static <T> T throwingMerger(T one, T two) {
        throw new IllegalStateException("Duplicate key " + one);
    }
}
