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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility for cleaning up generated schemas.
 */
public class SchemaCleanUpUtils {

    private final SchemaGeneratorConfig config;
    private final Map<SchemaKeyword, BiFunction<List<JsonNode>, Map<String, SchemaKeyword>, Supplier<? extends JsonNode>>> allOfMergeFunctions;

    /**
     * Constructor.
     *
     * @param config configuration to be applied
     */
    public SchemaCleanUpUtils(SchemaGeneratorConfig config) {
        this.config = config;
        this.allOfMergeFunctions = this.prepareAllOfMergeFunctions();
    }

    private Map<SchemaKeyword, BiFunction<List<JsonNode>, Map<String, SchemaKeyword>, Supplier<? extends JsonNode>>> prepareAllOfMergeFunctions() {
        Map<SchemaKeyword, BiFunction<List<JsonNode>, Map<String, SchemaKeyword>, Supplier<? extends JsonNode>>> result = new HashMap<>();

        BiFunction<List<JsonNode>, Map<String, SchemaKeyword>, Supplier<? extends JsonNode>> mergeArrays =
                (valuesToMerge, _ignored) -> this.mergeArrays(valuesToMerge);
        Stream.of(SchemaKeyword.TAG_ALLOF, SchemaKeyword.TAG_REQUIRED)
                .forEach(keyword -> result.put(keyword, mergeArrays));

        result.put(SchemaKeyword.TAG_PROPERTIES,
                (valuesToMerge, _ignored) -> this.mergeObjectProperties(valuesToMerge));
        result.put(SchemaKeyword.TAG_DEPENDENT_REQUIRED,
                (valuesToMerge, _ignored) -> this.mergeDependentRequiredNode(valuesToMerge));
        result.put(SchemaKeyword.TAG_DEPENDENT_SCHEMAS,
                (valuesToMerge, _ignored) -> {
                    if (this.config.getSchemaVersion() == SchemaVersion.DRAFT_6 || this.config.getSchemaVersion() == SchemaVersion.DRAFT_7) {
                        // in Draft 6 and Draft 7, the "dependencies" keyword was covering both "dependentSchemas" and "dependentRequired" scenarios
                        return Optional.ofNullable(this.mergeDependentRequiredNode(valuesToMerge))
                                .orElseGet(() -> this.mergeObjectProperties(valuesToMerge));
                    } else {
                        return this.mergeObjectProperties(valuesToMerge);
                    }
                });

        BiFunction<List<JsonNode>, Map<String, SchemaKeyword>, Supplier<? extends JsonNode>> schemaMerge =
                (valuesToMerge, reverseKeywordMap) -> this.mergeSchemas(null, valuesToMerge, reverseKeywordMap);
        Stream.of(SchemaKeyword.TAG_ITEMS, SchemaKeyword.TAG_UNEVALUATED_ITEMS, SchemaKeyword.TAG_ADDITIONAL_PROPERTIES,
                SchemaKeyword.TAG_UNEVALUATED_PROPERTIES)
                .forEach(keyword -> result.put(keyword, schemaMerge));
        result.put(SchemaKeyword.TAG_TYPE,
                (valuesToMerge, _ignored) -> this.returnOverlapOfStringsOrStringArrays(valuesToMerge));

        BiFunction<List<JsonNode>, Map<String, SchemaKeyword>, Supplier<? extends JsonNode>> minimumNumeric =
                (valuesToMerge, _ignored) -> this.returnMinimumNumericValue(valuesToMerge);
        Stream.of(SchemaKeyword.TAG_ITEMS_MAX, SchemaKeyword.TAG_PROPERTIES_MAX, SchemaKeyword.TAG_MAXIMUM, SchemaKeyword.TAG_MAXIMUM_EXCLUSIVE,
                SchemaKeyword.TAG_LENGTH_MAX)
                .forEach(keyword -> result.put(keyword, minimumNumeric));

        BiFunction<List<JsonNode>, Map<String, SchemaKeyword>, Supplier<? extends JsonNode>> maximumNumeric =
                (valuesToMerge, _ignored) -> this.returnMaximumNumericValue(valuesToMerge);
        Stream.of(SchemaKeyword.TAG_ITEMS_MIN, SchemaKeyword.TAG_PROPERTIES_MIN, SchemaKeyword.TAG_MINIMUM, SchemaKeyword.TAG_MINIMUM_EXCLUSIVE,
                        SchemaKeyword.TAG_LENGTH_MIN)
                .forEach(keyword -> result.put(keyword, maximumNumeric));
        return result;
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
     * Discard attributes on member schemas, that also reference an entry in the common definitions, which contains those exact same attributes.
     *
     * @param jsonSchemas generated schemas that may contain redundant attributes in nested object member schemas
     * @param definitionsNode object node containing common schema definitions
     * @param referenceKeyPrefix designated prefix to the entries in the returned definitions node (i.e., on {@link SchemaKeyword#TAG_REF} values)
     */
    public void reduceRedundantMemberAttributes(List<ObjectNode> jsonSchemas, ObjectNode definitionsNode, String referenceKeyPrefix) {
        final Map<String, Map<String, JsonNode>> definitions = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> defIt = definitionsNode.fields(); defIt.hasNext(); ) {
            Map.Entry<String, JsonNode> definition = defIt.next();
            Map<String, JsonNode> attributes = new HashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> attIt = definition.getValue().fields(); attIt.hasNext(); ) {
                Map.Entry<String, JsonNode> attriibute = attIt.next();
                attributes.put(attriibute.getKey(), attriibute.getValue());
            }
            definitions.put(referenceKeyPrefix + definition.getKey(), attributes);
        }
        final String propertiesKeyword = this.config.getKeyword(SchemaKeyword.TAG_PROPERTIES);
        final String refKeyWord = this.config.getKeyword(SchemaKeyword.TAG_REF);
        this.finaliseSchemaParts(jsonSchemas,
                nodeToCheck -> this.reduceRedundantMemberAttributesIfPossible(nodeToCheck, propertiesKeyword, refKeyWord, definitions));
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
        BiFunction<List<JsonNode>, Map<String, SchemaKeyword>, Supplier<? extends JsonNode>> specificMergeFunction =
                this.allOfMergeFunctions.get(keyword);
        if (specificMergeFunction == null) {
            return this.returnOneIfAllEqual(valuesToMerge);
        }
        return specificMergeFunction.apply(valuesToMerge, reverseKeywordMap);
    }

    private Supplier<JsonNode> mergeArrays(List<JsonNode> arrayNodesToMerge) {
        if (!arrayNodesToMerge.stream().allMatch(JsonNode::isArray)) {
            // at least one value is not an array as expected, abort merge
            return null;
        }
        return () -> {
            Set<JsonNode> arrayItems = new LinkedHashSet<>();
            arrayNodesToMerge.forEach(node -> node.forEach(arrayItems::add));

            ArrayNode mergedArrayNode = this.config.createArrayNode();
            arrayItems.forEach(mergedArrayNode::add);
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
        Map<String, Set<String>> mergedDependentRequiredNames = new LinkedHashMap<>();
        for (JsonNode singleNode : dependentRequiredNodesToMerge) {
            if (!collectDependentRequiredNamesIfMergeAllowed(singleNode, mergedDependentRequiredNames)) {
                // some dependentRequired node does not comply with expected structure, abort merge
                return null;
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
     * Iterate over the given object node's properties, expecting an array of property names on each, which should be added to the provided map.
     *
     * @param objectNode single value of a {@link SchemaKeyword#TAG_DEPENDENT_REQUIRED} attribute
     * @param mergedDependentRequiredNames collected required property names per leading property name, that should be extended here
     * @return whether the given {@code objectNode} is eligible to be merged and its contents were successfully added to the given collection
     */
    private boolean collectDependentRequiredNamesIfMergeAllowed(JsonNode objectNode, Map<String, Set<String>> mergedDependentRequiredNames) {
        if (!objectNode.isObject()) {
            // dependentRequired node is not an object as expected, abort merge
            return false;
        }
        Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> dependentRequiredFieldsOfSingleLead = it.next();
            Set<String> propertyNames = this.collectTextItemsFromArrayNode(dependentRequiredFieldsOfSingleLead.getValue());
            if (propertyNames == null) {
                // cannot consolidate when anything but an array of other plain property names is being provided
                return false;
            }
            mergedDependentRequiredNames.computeIfAbsent(dependentRequiredFieldsOfSingleLead.getKey(), leadName -> new LinkedHashSet<>())
                    .addAll(propertyNames);
        }
        return true;
    }

    /**
     * Unpack the given array of texts.
     *
     * @param arrayNode the node to unpack
     * @return all contained text items; or {@code null} if given node is not an array or any item is not a text
     */
    private Set<String> collectTextItemsFromArrayNode(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return null;
        }
        Set<String> textItems = new LinkedHashSet<>();
        for (JsonNode item : arrayNode) {
            if (!item.isTextual()) {
                return null;
            }
            textItems.add(item.asText());
        }
        return textItems;
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
        if (nodes.stream().anyMatch(part -> part.isBoolean() && !part.asBoolean())) {
            return null;
        }
        List<ObjectNode> parts = nodes.stream()
                .filter(ObjectNode.class::isInstance)
                .map(ObjectNode.class::cast)
                .collect(Collectors.toList());

        // collect all defined attributes from the separate parts and check whether there are incompatible differences
        Map<String, List<JsonNode>> fieldsFromAllParts = this.getFieldsFromAllParts(parts);
        if (this.shouldSkipMergingAllOf(mainNodeIncludingAllOf, parts, fieldsFromAllParts)) {
            return null;
        }
        Map<String, List<JsonNode>> unsupportedTagValues = fieldsFromAllParts.entrySet().stream()
                .filter(entry -> !reverseKeywordMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, SchemaCleanUpUtils::throwingMerger, LinkedHashMap::new));
        if (unsupportedTagValues.values().stream().anyMatch(occurrences -> occurrences.size() > 1)) {
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

    /**
     * Collect all defined attributes from the separate parts.
     *
     * @param parts entries of the {@link SchemaKeyword#TAG_ALLOF} array to consider
     * @return flattened collection of all attributes in the given parts
     */
    private Map<String, List<JsonNode>> getFieldsFromAllParts(List<ObjectNode> parts) {
        return parts.stream()
                .flatMap(part -> StreamSupport.stream(((Iterable<Map.Entry<String, JsonNode>>) part::fields).spliterator(), false))
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    /**
     * Check whether the merging of the given node and it's allOf entries should be skipped due to a {@link SchemaKeyword#TAG_REF} being present.
     * Drafts 6 and 7 would ignore any other attributes besides the {@link SchemaKeyword#TAG_REF}.
     *
     * @param mainNode the main node containing an {@link SchemaKeyword#TAG_ALLOF} array (maybe {@code null})
     * @param parts entries of the {@link SchemaKeyword#TAG_ALLOF} array to consider
     * @param fieldsFromAllParts flattened collection of all attributes in the given parts
     * @return whether to block merging of the given {@link SchemaKeyword#TAG_ALLOF} candidate
     */
    private boolean shouldSkipMergingAllOf(ObjectNode mainNode, List<ObjectNode> parts, Map<String, List<JsonNode>> fieldsFromAllParts) {
        if (this.config.getSchemaVersion() != SchemaVersion.DRAFT_6 && this.config.getSchemaVersion() != SchemaVersion.DRAFT_7) {
            // later schema versions support mixing a reference with other attributes
            return false;
        }
        if (!fieldsFromAllParts.containsKey(this.config.getKeyword(SchemaKeyword.TAG_REF))) {
            // we are only concerned about references here; if none is present, merging should not be blocked
            return false;
        }
        if (mainNode == null) {
            // if there are multiple parts that could be conflicting, block merging
            return parts.size() > 1;
        }
        // if main node contains more than just the allOf or there are multiple parts that may be conflicting, block merging
        return mainNode.size() > 1 || parts.size() > 2;
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
                if (valuesToMerge.size() == 1) {
                    // no other "allOf" part left to merge
                    continue;
                }
                // we can ignore the "allOf" tag in the target node (the one we are trying to remove here)
                valuesToMerge = valuesToMerge.subList(1, valuesToMerge.size());
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
            encounteredValues.retainAll(nextValues);
            if (encounteredValues.isEmpty()) {
                // invalid merge result: no valid value remained; abort merge
                return null;
            }
        }
        if (encounteredValues.size() == 1) {
            return () -> new TextNode(encounteredValues.get(0));
        }
        return () -> {
            ArrayNode arrayNode = this.config.createArrayNode();
            encounteredValues.stream().map(TextNode::new).forEach(arrayNode::add);
            return arrayNode;
        };
    }

    private List<String> getStringValuesFromStringOrStringArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(arrayItem -> result.add(arrayItem.asText(null)));
            if (result.contains(null)) {
                return null;
            }
        } else if (node.isTextual()) {
            result.add(node.asText());
        } else {
            // neither array nor text node; abort merge
            return null;
        }
        return result;
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
     * Discard attributes on member schemas, that also reference an entry in the common definitions, which contains those exact same attributes.
     *
     * @param schemaNode single schema to check for properties, which in turn contain redundant attributes to be removed
     * @param propertiesKeyword keyword under which to find an object schema's properties
     * @param refKeyword keyword containing the reference to a common schema definition
     * @param definitions object node containing common schema definitions
     */
    private void reduceRedundantMemberAttributesIfPossible(ObjectNode schemaNode,
            String propertiesKeyword, String refKeyword, Map<String, Map<String, JsonNode>> definitions) {
        JsonNode propertiesNode = schemaNode.get(propertiesKeyword);
        if (propertiesNode == null || !propertiesNode.isObject()) {
            return;
        }
        for (Iterator<JsonNode> it = propertiesNode.elements(); it.hasNext(); ) {
            JsonNode memberSchema = it.next();
            JsonNode reference = memberSchema.get(refKeyword);
            if (reference == null || !(memberSchema instanceof ObjectNode)) {
                // only considering a property/member schema containing a direct reference to a common definition
                continue;
            }
            if (definitions.containsKey(reference.asText())) {
                this.reduceRedundantAttributesIfPossible((ObjectNode) memberSchema, definitions.get(reference.asText()));
            }
        }
    }

    /**
     * Discard attributes on the given member schema, if those same attributes are already contained in the common definition being referenced.
     *
     * @param memberSchema single schema to discard redundant attributes from
     * @param referencedDefinition attribute names and values in the common definition, which don't need to be repeated
     */
    private void reduceRedundantAttributesIfPossible(ObjectNode memberSchema, Map<String, JsonNode> referencedDefinition) {
        Set<String> skippedKeywords = new HashSet<>();
        String ifKeyword = this.config.getKeyword(SchemaKeyword.TAG_IF);
        String thenKeyword = this.config.getKeyword(SchemaKeyword.TAG_THEN);
        String elseKeyword = this.config.getKeyword(SchemaKeyword.TAG_ELSE);
        boolean shouldSkipConditionals = !Util.nullSafeEquals(memberSchema.get(ifKeyword), referencedDefinition.get(ifKeyword))
                || !Util.nullSafeEquals(memberSchema.get(thenKeyword), referencedDefinition.get(thenKeyword))
                || !Util.nullSafeEquals(memberSchema.get(elseKeyword), referencedDefinition.get(elseKeyword));
        if (shouldSkipConditionals) {
            skippedKeywords.add(ifKeyword);
            skippedKeywords.add(thenKeyword);
            skippedKeywords.add(elseKeyword);
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = memberSchema.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> memberAttribute = it.next();
            String keyword = memberAttribute.getKey();
            if (!skippedKeywords.contains(keyword) && memberAttribute.getValue().equals(referencedDefinition.get(keyword))) {
                // remove member attribute, that also exists on the referenced definition
                it.remove();
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
        List<String> impliedTypes = this.collectImpliedTypes(schemaNode, typeTagName, reverseTagMap);
        if (impliedTypes.isEmpty()) {
            return;
        }
        if (considerNullType && !this.config.shouldAlwaysWrapNullSchemaInAnyOf()) {
            impliedTypes.add(SchemaKeyword.SchemaType.NULL.getSchemaKeywordValue());
        }
        if (impliedTypes.size() == 1) {
            schemaNode.put(typeTagName, impliedTypes.get(0));
        } else {
            impliedTypes.forEach(schemaNode.putArray(typeTagName)::add);
        }
        if (considerNullType && this.config.shouldAlwaysWrapNullSchemaInAnyOf()) {
            // since version 4.37.0
            SchemaGenerationContextImpl.makeNullable(schemaNode, this.config);
        }
    }

    private List<String> collectImpliedTypes(ObjectNode schemaNode, String typeTagName, Map<String, SchemaKeyword> reverseTagMap) {
        if (schemaNode.has(typeTagName)) {
            // explicit type indication is already present
            return Collections.emptyList();
        }
        return reverseTagMap.entrySet().stream()
                .filter(entry -> schemaNode.has(entry.getKey()))
                .flatMap(entry -> entry.getValue().getImpliedTypes().stream())
                .distinct()
                .sorted()
                .map(SchemaKeyword.SchemaType::getSchemaKeywordValue)
                .collect(Collectors.toList());
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
