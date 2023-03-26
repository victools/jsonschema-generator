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

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * JSON Schema properties and their values.
 */
public enum SchemaKeyword {
    TAG_SCHEMA("$schema", EnumSet.of(TagContent.NON_SCHEMA)),
    TAG_SCHEMA_VALUE(SchemaVersion::getIdentifier, EnumSet.noneOf(TagContent.class), EnumSet.noneOf(SchemaType.class)),
    TAG_ID("$id", EnumSet.of(TagContent.NON_SCHEMA)),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_ANCHOR("$anchor", EnumSet.of(TagContent.NON_SCHEMA)),
    TAG_DEFINITIONS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "definitions" : "$defs",
            EnumSet.of(TagContent.NAMED_SCHEMAS), EnumSet.noneOf(SchemaType.class)),
    /**
     * Before {@link SchemaVersion#DRAFT_2019_09} all other properties in the same sub-schema besides this one were ignored.
     */
    TAG_REF("$ref", EnumSet.of(TagContent.NON_SCHEMA)),
    TAG_REF_MAIN("#"),
    /**
     * Common prefix of all standard {@link #TAG_REF} values.
     *
     * @deprecated is now implicitly created based on {@link SchemaKeyword#TAG_DEFINITIONS} or an explicit alternative definitions path
     */
    @Deprecated
    TAG_REF_PREFIX(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "#/definitions/" : "#/$defs/",
            EnumSet.noneOf(TagContent.class), EnumSet.noneOf(SchemaType.class)),

    TAG_TYPE("type", EnumSet.of(TagContent.NON_SCHEMA)),
    TAG_TYPE_NULL(SchemaType.NULL.getSchemaKeywordValue()),
    TAG_TYPE_ARRAY(SchemaType.ARRAY.getSchemaKeywordValue()),
    TAG_TYPE_OBJECT(SchemaType.OBJECT.getSchemaKeywordValue()),
    TAG_TYPE_BOOLEAN(SchemaType.BOOLEAN.getSchemaKeywordValue()),
    TAG_TYPE_STRING(SchemaType.STRING.getSchemaKeywordValue()),
    TAG_TYPE_INTEGER(SchemaType.INTEGER.getSchemaKeywordValue()),
    TAG_TYPE_NUMBER(SchemaType.NUMBER.getSchemaKeywordValue()),

    TAG_PROPERTIES("properties", EnumSet.of(TagContent.NAMED_SCHEMAS), EnumSet.of(SchemaType.OBJECT)),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_UNEVALUATED_PROPERTIES("unevaluatedProperties", EnumSet.of(TagContent.SCHEMA), EnumSet.of(SchemaType.OBJECT)),
    TAG_ITEMS("items", EnumSet.of(TagContent.SCHEMA, TagContent.ARRAY_OF_SCHEMAS), EnumSet.of(SchemaType.ARRAY)),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_PREFIX_ITEMS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "items" : "prefixItems",
            EnumSet.of(TagContent.ARRAY_OF_SCHEMAS), EnumSet.of(SchemaType.ARRAY)),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_UNEVALUATED_ITEMS("unevaluatedItems", EnumSet.of(TagContent.SCHEMA), EnumSet.of(SchemaType.ARRAY)),
    TAG_REQUIRED("required", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.OBJECT)),
    /**
     * Prior to {@link SchemaVersion#DRAFT_2019_09}, this had the same name as {@link #TAG_DEPENDENT_REQUIRED}.
     */
    TAG_DEPENDENT_SCHEMAS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7
            ? "dependencies" : "dependentSchemas", EnumSet.of(TagContent.NAMED_SCHEMAS), EnumSet.of(SchemaType.OBJECT)),
    /**
     * Prior to {@link SchemaVersion#DRAFT_2019_09}, this had the same name as {@link #TAG_DEPENDENT_SCHEMAS}.
     */
    TAG_DEPENDENT_REQUIRED(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7
            ? "dependencies" : "dependentRequired", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.OBJECT)),
    TAG_ADDITIONAL_PROPERTIES("additionalProperties", EnumSet.of(TagContent.SCHEMA), EnumSet.of(SchemaType.OBJECT)),
    TAG_PATTERN_PROPERTIES("patternProperties", EnumSet.of(TagContent.NAMED_SCHEMAS), EnumSet.of(SchemaType.OBJECT)),
    TAG_PROPERTIES_MIN("minProperties", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.OBJECT)),
    TAG_PROPERTIES_MAX("maxProperties", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.OBJECT)),

    TAG_ALLOF("allOf", EnumSet.of(TagContent.ARRAY_OF_SCHEMAS)),
    TAG_ANYOF("anyOf", EnumSet.of(TagContent.ARRAY_OF_SCHEMAS)),
    TAG_ONEOF("oneOf", EnumSet.of(TagContent.ARRAY_OF_SCHEMAS)),
    TAG_NOT("not", EnumSet.of(TagContent.SCHEMA)),

    TAG_TITLE("title", EnumSet.of(TagContent.NON_SCHEMA)),
    TAG_DESCRIPTION("description", EnumSet.of(TagContent.NON_SCHEMA)),
    TAG_CONST("const", EnumSet.of(TagContent.NON_SCHEMA)),
    TAG_ENUM("enum", EnumSet.of(TagContent.NON_SCHEMA)),
    TAG_DEFAULT("default", EnumSet.of(TagContent.NON_SCHEMA)),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_READ_ONLY("readOnly", EnumSet.of(TagContent.NON_SCHEMA)),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_WRITE_ONLY("writeOnly", EnumSet.of(TagContent.NON_SCHEMA)),

    TAG_LENGTH_MIN("minLength", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.STRING)),
    TAG_LENGTH_MAX("maxLength", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.STRING)),
    TAG_FORMAT("format", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.STRING)),
    TAG_PATTERN("pattern", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.STRING)),

    TAG_MINIMUM("minimum", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.INTEGER, SchemaType.NUMBER)),
    TAG_MINIMUM_EXCLUSIVE("exclusiveMinimum", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.INTEGER, SchemaType.NUMBER)),
    TAG_MAXIMUM("maximum", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.INTEGER, SchemaType.NUMBER)),
    TAG_MAXIMUM_EXCLUSIVE("exclusiveMaximum", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.INTEGER, SchemaType.NUMBER)),
    TAG_MULTIPLE_OF("multipleOf", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.INTEGER, SchemaType.NUMBER)),

    TAG_ITEMS_MIN("minItems", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.ARRAY)),
    TAG_ITEMS_MAX("maxItems", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.ARRAY)),
    TAG_ITEMS_UNIQUE("uniqueItems", EnumSet.of(TagContent.NON_SCHEMA), EnumSet.of(SchemaType.ARRAY)),

    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_IF("if", EnumSet.of(TagContent.SCHEMA)),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_THEN("then", EnumSet.of(TagContent.SCHEMA)),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_ELSE("else", EnumSet.of(TagContent.SCHEMA));

    private final Function<SchemaVersion, String> valueProvider;
    private final EnumSet<TagContent> contentTypes;
    private final EnumSet<SchemaType> impliedTypes;

    /**
     * Constructor.
     *
     * @param fixedValue single value applying regardless of schema version
     */
    SchemaKeyword(String fixedValue) {
        this(fixedValue, EnumSet.noneOf(TagContent.class), EnumSet.noneOf(SchemaType.class));
    }

    /**
     * Constructor.
     *
     * @param fixedValue single value applying regardless of schema version
     * @param contentTypes what kind of values can be expected under this keyword (empty when this keyword represents such a value)
     */
    SchemaKeyword(String fixedValue, EnumSet<TagContent> contentTypes) {
        this(_version -> fixedValue, contentTypes, EnumSet.noneOf(SchemaType.class));
    }

    /**
     * Constructor.
     *
     * @param fixedValue single value applying regardless of schema version
     * @param contentTypes what kind of values can be expected under this keyword (empty when this keyword represents such a value)
     * @param impliedTypes values of the {@link #TAG_TYPE} being implied by the presence of this keyword in a schema
     */
    SchemaKeyword(String fixedValue, EnumSet<TagContent> contentTypes, EnumSet<SchemaType> impliedTypes) {
        this(_version -> fixedValue, contentTypes, impliedTypes);
    }

    /**
     * Constructor.
     *
     * @param valueProvider dynamic value provider that may return different values base on specific JSON Schema versions
     * @param contentTypes what kind of values can be expected under this keyword (empty when this keyword represents such a value)
     * @param impliedTypes values of the {@link #TAG_TYPE} being implied by the presence of this keyword in a schema
     */
    SchemaKeyword(Function<SchemaVersion, String> valueProvider, EnumSet<TagContent> contentTypes, EnumSet<SchemaType> impliedTypes) {
        this.valueProvider = valueProvider;
        this.contentTypes = contentTypes;
        this.impliedTypes = impliedTypes;
    }

    /**
     * Determine which (if any) specific {@link #TAG_TYPE} values this keyword implies.
     *
     * @return implied type values or an empty list if this keyword is not type specific
     */
    public EnumSet<SchemaType> getImpliedTypes() {
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
