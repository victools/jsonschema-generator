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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JSON Schema properties and their values.
 */
public enum SchemaKeyword {
    TAG_SCHEMA("$schema", Collections.emptyList(), TagContent.NON_SCHEMA),
    TAG_SCHEMA_VALUE(SchemaVersion::getIdentifier, Collections.emptyList()),
    TAG_ID("$id", Collections.emptyList(), TagContent.NON_SCHEMA),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_ANCHOR("$anchor", Collections.emptyList(), TagContent.NON_SCHEMA),
    TAG_DEFINITIONS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "definitions" : "$defs",
            Collections.emptyList(), TagContent.NAMED_SCHEMAS),
    /**
     * Before {@link SchemaVersion#DRAFT_2019_09} all other properties in the same sub-schema besides this one were ignored.
     */
    TAG_REF("$ref", Collections.emptyList(), TagContent.NON_SCHEMA),
    TAG_REF_MAIN("#", Collections.emptyList()),
    /**
     * Common prefix of all standard {@link #TAG_REF} values.
     *
     * @deprecated is now implicitly created based on {@link SchemaKeyword#TAG_DEFINITIONS} or an explicit alternative definitions path
     */
    @Deprecated
    TAG_REF_PREFIX(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "#/definitions/" : "#/$defs/",
            Collections.emptyList()),

    TAG_TYPE("type", Collections.emptyList(), TagContent.NON_SCHEMA),
    TAG_TYPE_NULL(SchemaType.NULL.getSchemaKeywordValue(), Collections.emptyList()),
    TAG_TYPE_ARRAY(SchemaType.ARRAY.getSchemaKeywordValue(), Collections.emptyList()),
    TAG_TYPE_OBJECT(SchemaType.OBJECT.getSchemaKeywordValue(), Collections.emptyList()),
    TAG_TYPE_BOOLEAN(SchemaType.BOOLEAN.getSchemaKeywordValue(), Collections.emptyList()),
    TAG_TYPE_STRING(SchemaType.STRING.getSchemaKeywordValue(), Collections.emptyList()),
    TAG_TYPE_INTEGER(SchemaType.INTEGER.getSchemaKeywordValue(), Collections.emptyList()),
    TAG_TYPE_NUMBER(SchemaType.NUMBER.getSchemaKeywordValue(), Collections.emptyList()),

    TAG_PROPERTIES("properties", Collections.singletonList(SchemaType.OBJECT), TagContent.NAMED_SCHEMAS),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_UNEVALUATED_PROPERTIES("unevaluatedProperties", Collections.singletonList(SchemaType.OBJECT), TagContent.SCHEMA),
    TAG_ITEMS("items", Collections.singletonList(SchemaType.ARRAY), TagContent.SCHEMA, TagContent.ARRAY_OF_SCHEMAS),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_PREFIX_ITEMS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "items" : "prefixItems",
            Collections.singletonList(SchemaType.ARRAY), TagContent.ARRAY_OF_SCHEMAS),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_UNEVALUATED_ITEMS("unevaluatedItems", Collections.singletonList(SchemaType.ARRAY), TagContent.SCHEMA),
    TAG_REQUIRED("required", Collections.singletonList(SchemaType.OBJECT), TagContent.NON_SCHEMA),
    /**
     * Prior to {@link SchemaVersion#DRAFT_2019_09}, this had the same name as {@link #TAG_DEPENDENT_REQUIRED}.
     */
    TAG_DEPENDENT_SCHEMAS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7
            ? "dependencies" : "dependentSchemas", Collections.singletonList(SchemaType.OBJECT), TagContent.NAMED_SCHEMAS),
    /**
     * Prior to {@link SchemaVersion#DRAFT_2019_09}, this had the same name as {@link #TAG_DEPENDENT_SCHEMAS}.
     */
    TAG_DEPENDENT_REQUIRED(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7
            ? "dependencies" : "dependentRequired", Collections.singletonList(SchemaType.OBJECT), TagContent.NON_SCHEMA),
    TAG_ADDITIONAL_PROPERTIES("additionalProperties", Collections.singletonList(SchemaType.OBJECT), TagContent.SCHEMA),
    TAG_PATTERN_PROPERTIES("patternProperties", Collections.singletonList(SchemaType.OBJECT), TagContent.NAMED_SCHEMAS),
    TAG_PROPERTIES_MIN("minProperties", Collections.singletonList(SchemaType.OBJECT), TagContent.NON_SCHEMA),
    TAG_PROPERTIES_MAX("maxProperties", Collections.singletonList(SchemaType.OBJECT), TagContent.NON_SCHEMA),

    TAG_ALLOF("allOf", Collections.emptyList(), TagContent.ARRAY_OF_SCHEMAS),
    TAG_ANYOF("anyOf", Collections.emptyList(), TagContent.ARRAY_OF_SCHEMAS),
    TAG_ONEOF("oneOf", Collections.emptyList(), TagContent.ARRAY_OF_SCHEMAS),
    TAG_NOT("not", Collections.emptyList(), TagContent.SCHEMA),

    TAG_TITLE("title", Collections.emptyList(), TagContent.NON_SCHEMA),
    TAG_DESCRIPTION("description", Collections.emptyList(), TagContent.NON_SCHEMA),
    TAG_CONST("const", Collections.emptyList(), TagContent.NON_SCHEMA),
    TAG_ENUM("enum", Collections.emptyList(), TagContent.NON_SCHEMA),
    TAG_DEFAULT("default", Collections.emptyList(), TagContent.NON_SCHEMA),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_READ_ONLY("readOnly", Collections.emptyList(), TagContent.NON_SCHEMA),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_WRITE_ONLY("writeOnly", Collections.emptyList(), TagContent.NON_SCHEMA),

    TAG_LENGTH_MIN("minLength", Collections.singletonList(SchemaType.STRING), TagContent.NON_SCHEMA),
    TAG_LENGTH_MAX("maxLength", Collections.singletonList(SchemaType.STRING), TagContent.NON_SCHEMA),
    TAG_FORMAT("format", Collections.singletonList(SchemaType.STRING), TagContent.NON_SCHEMA),
    TAG_PATTERN("pattern", Collections.singletonList(SchemaType.STRING), TagContent.NON_SCHEMA),

    TAG_MINIMUM("minimum", Arrays.asList(SchemaType.INTEGER, SchemaType.NUMBER), TagContent.NON_SCHEMA),
    TAG_MINIMUM_EXCLUSIVE("exclusiveMinimum", Arrays.asList(SchemaType.INTEGER, SchemaType.NUMBER), TagContent.NON_SCHEMA),
    TAG_MAXIMUM("maximum", Arrays.asList(SchemaType.INTEGER, SchemaType.NUMBER), TagContent.NON_SCHEMA),
    TAG_MAXIMUM_EXCLUSIVE("exclusiveMaximum", Arrays.asList(SchemaType.INTEGER, SchemaType.NUMBER), TagContent.NON_SCHEMA),
    TAG_MULTIPLE_OF("multipleOf", Arrays.asList(SchemaType.INTEGER, SchemaType.NUMBER), TagContent.NON_SCHEMA),

    TAG_ITEMS_MIN("minItems", Collections.singletonList(SchemaType.ARRAY), TagContent.NON_SCHEMA),
    TAG_ITEMS_MAX("maxItems", Collections.singletonList(SchemaType.ARRAY), TagContent.NON_SCHEMA),
    TAG_ITEMS_UNIQUE("uniqueItems", Collections.singletonList(SchemaType.ARRAY), TagContent.NON_SCHEMA),

    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_IF("if", Collections.emptyList(), TagContent.SCHEMA),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_THEN("then", Collections.emptyList(), TagContent.SCHEMA),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_ELSE("else", Collections.emptyList(), TagContent.SCHEMA);

    private final Function<SchemaVersion, String> valueProvider;
    private final List<SchemaType> impliedTypes;
    private final List<TagContent> contentTypes;

    /**
     * Constructor.
     *
     * @param fixedValue single value applying regardless of schema version
     * @param impliedTypes values of the {@link #TAG_TYPE} being implied by the presence of this keyword in a schema
     * @param contentTypes what kind of values can be expected under this keyword (empty when this keyword represents such a value)
     */
    SchemaKeyword(String fixedValue, List<SchemaType> impliedTypes, TagContent... contentTypes) {
        this(_version -> fixedValue, impliedTypes, contentTypes);
    }

    /**
     * Constructor.
     *
     * @param valueProvider dynamic value provider that may return different values base on specific JSON Schema versions
     * @param impliedTypes values of the {@link #TAG_TYPE} being implied by the presence of this keyword in a schema
     * @param contentTypes what kind of values can be expected under this keyword (empty when this keyword represents such a value)
     */
    SchemaKeyword(Function<SchemaVersion, String> valueProvider, List<SchemaType> impliedTypes, TagContent... contentTypes) {
        this.valueProvider = valueProvider;
        this.impliedTypes = Collections.unmodifiableList(impliedTypes);
        this.contentTypes = Arrays.asList(contentTypes);
    }

    /**
     * Determine which (if any) specific {@link #TAG_TYPE} values this keyword implies.
     *
     * @return implied type values or an empty list if this keyword is not type specific
     */
    public List<SchemaType> getImpliedTypes() {
        return this.impliedTypes;
    }

    /**
     * Indicate what the given kind of values can be expected under this keyword (if it represents a tag). Returns always false for values.
     *
     * @param contentType type of content/values to be expected under a schema tag
     * @return whether the given tag content type is supported by this keyword
     */
    public boolean supportsContentType(TagContent contentType) {
        return this.contentTypes.contains(contentType);
    }

    /**
     * Provide the appropriate tag name/value, considering the specified schema version.
     *
     * @param version applicable JSON Schema version
     * @return corresponding tag name/value for this keyword in the indicated JSON Schema version
     */
    public String forVersion(SchemaVersion version) {
        return this.valueProvider.apply(version);
    }

    /**
     * Provide a map over all keywords, that represent a schema tag/property, i.e., not a value.
     *
     * @param version schema draft version to look-up keyword for
     * @param filter additional tag filter to apply
     * @return mapping of schema tag/property name to its corresponding tag
     */
    public static Map<String, SchemaKeyword> getReverseTagMap(SchemaVersion version, Predicate<SchemaKeyword> filter) {
        return EnumSet.allOf(SchemaKeyword.class).stream()
                .filter(keyword -> !keyword.contentTypes.isEmpty() && filter.test(keyword))
                .collect(Collectors.toMap(keyword -> keyword.forVersion(version), keyword -> keyword,
                        // when two keywords are mapped to the same tag name, stick to the first (as per declaration order in this enum)
                        (k1, k2) -> k1));
    }

    /**
     * Values of the {@link SchemaKeyword#TAG_TYPE}.
     */
    public enum SchemaType {
        NULL("null"),
        ARRAY("array"),
        OBJECT("object"),
        BOOLEAN("boolean"),
        STRING("string"),
        INTEGER("integer"),
        NUMBER("number");

        private final String schemaKeywordValue;

        SchemaType(String schemaKeywordValue) {
            this.schemaKeywordValue = schemaKeywordValue;
        }

        public String getSchemaKeywordValue() {
            return this.schemaKeywordValue;
        }
    }

    /**
     * Type of content/values to be expected under a schema tag.
     */
    public enum TagContent {
        SCHEMA, ARRAY_OF_SCHEMAS, NAMED_SCHEMAS, NON_SCHEMA;
    }
}
