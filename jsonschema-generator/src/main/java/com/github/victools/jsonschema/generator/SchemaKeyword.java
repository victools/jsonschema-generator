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
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * JSON Schema properties and their values.
 */
public enum SchemaKeyword {
    TAG_SCHEMA("$schema", Type.TAG),
    TAG_SCHEMA_VALUE(SchemaVersion::getIdentifier, Type.VALUE),
    TAG_ID("$id", Type.TAG),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_ANCHOR("$anchor", Type.TAG),
    TAG_DEFINITIONS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "definitions" : "$defs", Type.TAG),
    /**
     * Before {@link SchemaVersion#DRAFT_2019_09} all other properties in the same sub-schema besides this one were ignored.
     */
    TAG_REF("$ref", Type.TAG),
    TAG_REF_MAIN("#", Type.VALUE),
    /**
     * Common prefix of all standard {@link #TAG_REF} values.
     *
     * @deprecated is now implicitly created based on {@link SchemaKeyword#TAG_DEFINITIONS} or an explicit alternative definitions path
     */
    @Deprecated
    TAG_REF_PREFIX(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7 ? "#/definitions/" : "#/$defs/", Type.VALUE),

    TAG_TYPE("type", Type.TAG),
    TAG_TYPE_NULL("null", Type.VALUE),
    TAG_TYPE_ARRAY("array", Type.VALUE),
    TAG_TYPE_OBJECT("object", Type.VALUE),
    TAG_TYPE_BOOLEAN("boolean", Type.VALUE),
    TAG_TYPE_STRING("string", Type.VALUE),
    TAG_TYPE_INTEGER("integer", Type.VALUE),
    TAG_TYPE_NUMBER("number", Type.VALUE),

    TAG_PROPERTIES("properties", Type.TAG),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_UNEVALUATED_PROPERTIES("unevaluatedProperties", Type.TAG),
    TAG_ITEMS("items", Type.TAG),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_PREFIX_ITEMS("prefixItems", Type.TAG),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_2019_09}.
     */
    TAG_UNEVALUATED_ITEMS("unevaluatedItems", Type.TAG),
    TAG_REQUIRED("required", Type.TAG),
    /**
     * Prior to {@link SchemaVersion#DRAFT_2019_09}, this had the same name as {@link #TAG_DEPENDENT_SCHEMAS}.
     */
    TAG_DEPENDENT_REQUIRED(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7
            ? "dependencies" : "dependentRequired", Type.TAG),
    /**
     * Prior to {@link SchemaVersion#DRAFT_2019_09}, this had the same name as {@link #TAG_DEPENDENT_REQUIRED}.
     */
    TAG_DEPENDENT_SCHEMAS(version -> version == SchemaVersion.DRAFT_6 || version == SchemaVersion.DRAFT_7
            ? "dependencies" : "dependentSchemas", Type.TAG),
    TAG_ADDITIONAL_PROPERTIES("additionalProperties", Type.TAG),
    TAG_PATTERN_PROPERTIES("patternProperties", Type.TAG),
    TAG_PROPERTIES_MIN("minProperties", Type.TAG),
    TAG_PROPERTIES_MAX("maxProperties", Type.TAG),

    TAG_ALLOF("allOf", Type.TAG),
    TAG_ANYOF("anyOf", Type.TAG),
    TAG_ONEOF("oneOf", Type.TAG),
    TAG_NOT("not", Type.TAG),

    TAG_TITLE("title", Type.TAG),
    TAG_DESCRIPTION("description", Type.TAG),
    TAG_CONST("const", Type.TAG),
    TAG_ENUM("enum", Type.TAG),
    TAG_DEFAULT("default", Type.TAG),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_READ_ONLY("readOnly", Type.TAG),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_WRITE_ONLY("writeOnly", Type.TAG),

    TAG_LENGTH_MIN("minLength", Type.TAG),
    TAG_LENGTH_MAX("maxLength", Type.TAG),
    TAG_FORMAT("format", Type.TAG),
    TAG_PATTERN("pattern", Type.TAG),

    TAG_MINIMUM("minimum", Type.TAG),
    TAG_MINIMUM_EXCLUSIVE("exclusiveMinimum", Type.TAG),
    TAG_MAXIMUM("maximum", Type.TAG),
    TAG_MAXIMUM_EXCLUSIVE("exclusiveMaximum", Type.TAG),
    TAG_MULTIPLE_OF("multipleOf", Type.TAG),

    TAG_ITEMS_MIN("minItems", Type.TAG),
    TAG_ITEMS_MAX("maxItems", Type.TAG),
    TAG_ITEMS_UNIQUE("uniqueItems", Type.TAG),

    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_IF("if", Type.TAG),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_THEN("then", Type.TAG),
    /**
     * Beware that this keyword was only introduced in {@link SchemaVersion#DRAFT_7}.
     */
    TAG_ELSE("else", Type.TAG);

    private final Function<SchemaVersion, String> valueProvider;
    private final SchemaKeyword.Type type;

    /**
     * Constructor.
     *
     * @param fixedValue single value applying regardless of schema version
     * @param keywordType what kind of keyword this represents
     */
    private SchemaKeyword(String fixedValue, SchemaKeyword.Type keywordType) {
        this(_version -> fixedValue, keywordType);
    }

    /**
     * Constructor.
     *
     * @param valueProvider dynamic value provider that may return different values base on specific JSON Schema versions
     * @param keywordType what kind of keyword this represents
     */
    private SchemaKeyword(Function<SchemaVersion, String> valueProvider, SchemaKeyword.Type keywordType) {
        this.valueProvider = valueProvider;
        this.type = keywordType;
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

    public static Stream<SchemaKeyword> getTagStream() {
        return EnumSet.allOf(SchemaKeyword.class).stream()
                .filter(keyword -> keyword.type == SchemaKeyword.Type.TAG);
    }

    private enum Type {
        TAG, VALUE;
    }
}
