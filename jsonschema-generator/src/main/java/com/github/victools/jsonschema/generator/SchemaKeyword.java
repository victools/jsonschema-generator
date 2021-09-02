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

import java.util.function.Function;

/**
 * JSON Schema properties and their values.
 */
public enum SchemaKeyword {
    TAG_SCHEMA("$schema"),
    TAG_SCHEMA_VALUE(SchemaVersion::getIdentifier),
    TAG_ID("$id"),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_ANCHOR("$anchor"),
    TAG_DEFINITIONS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "definitions" : "$defs"),
    /**
     * Before {@link SchemaVersion#DRAFT_2019_09} all other properties in the same sub-schema besides this one were ignored.
     */
    TAG_REF("$ref"),
    TAG_REF_MAIN("#"),
    /**
     * Common prefix of all standard {@link #TAG_REF} values.
     *
     * @deprecated is now implicitly created based on {@link SchemaKeyword#TAG_DEFINITIONS} or an explicit alternative definitions path
     */
    @Deprecated
    TAG_REF_PREFIX(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "#/definitions/" : "#/$defs/"),

    TAG_TYPE("type"),
    TAG_TYPE_NULL("null"),
    TAG_TYPE_ARRAY("array"),
    TAG_TYPE_OBJECT("object"),
    TAG_TYPE_BOOLEAN("boolean"),
    TAG_TYPE_STRING("string"),
    TAG_TYPE_INTEGER("integer"),
    TAG_TYPE_NUMBER("number"),

    TAG_PROPERTIES("properties"),
    TAG_ITEMS("items"),
    TAG_REQUIRED("required"),
    TAG_ADDITIONAL_PROPERTIES("additionalProperties"),
    TAG_PATTERN_PROPERTIES("patternProperties"),
    TAG_PROPERTIES_MIN("minProperties"),
    TAG_PROPERTIES_MAX("maxProperties"),

    TAG_ALLOF("allOf"),
    TAG_ANYOF("anyOf"),
    TAG_ONEOF("oneOf"),
    TAG_NOT("not"),

    TAG_TITLE("title"),
    TAG_DESCRIPTION("description"),
    TAG_CONST("const"),
    TAG_ENUM("enum"),
    TAG_DEFAULT("default"),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_READ_ONLY("readOnly"),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_WRITE_ONLY("writeOnly"),

    TAG_LENGTH_MIN("minLength"),
    TAG_LENGTH_MAX("maxLength"),
    TAG_FORMAT("format"),
    TAG_PATTERN("pattern"),

    TAG_MINIMUM("minimum"),
    TAG_MINIMUM_EXCLUSIVE("exclusiveMinimum"),
    TAG_MAXIMUM("maximum"),
    TAG_MAXIMUM_EXCLUSIVE("exclusiveMaximum"),
    TAG_MULTIPLE_OF("multipleOf"),

    TAG_ITEMS_MIN("minItems"),
    TAG_ITEMS_MAX("maxItems"),
    TAG_ITEMS_UNIQUE("uniqueItems"),

    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_IF("if"),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_THEN("then"),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_ELSE("else");

    private final Function<SchemaVersion, String> valueProvider;

    /**
     * Constructor.
     *
     * @param fixedValue single value applying regardless of schema version
     */
    private SchemaKeyword(String fixedValue) {
        this.valueProvider = _version -> fixedValue;
    }

    /**
     * Constructor.
     *
     * @param valueProvider dynamic value provider that may return different values base on specific JSON Schema versions
     */
    private SchemaKeyword(Function<SchemaVersion, String> valueProvider) {
        this.valueProvider = valueProvider;
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
}
