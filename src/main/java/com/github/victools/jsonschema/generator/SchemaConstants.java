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

/**
 * JSON Schema properties and their values.
 */
public interface SchemaConstants {

    String TAG_SCHEMA = "$schema";
    String TAG_SCHEMA_DRAFT7 = "http://json-schema.org/draft-07/schema#";
    String TAG_DEFINITIONS = "definitions";
    String TAG_REF = "$ref";
    String TAG_REF_MAIN = "#";
    String TAG_REF_PREFIX = "#/definitions/";

    String TAG_TYPE = "type";
    String TAG_TYPE_NULL = "null";
    String TAG_TYPE_ARRAY = "array";
    String TAG_TYPE_OBJECT = "object";
    String TAG_TYPE_BOOLEAN = "boolean";
    String TAG_TYPE_STRING = "string";
    String TAG_TYPE_INTEGER = "integer";
    String TAG_TYPE_NUMBER = "number";

    String TAG_PROPERTIES = "properties";
    String TAG_ITEMS = "items";
    String TAG_REQUIRED = "required";
    String TAG_ADDITIONAL_PROPERTIES = "additionalProperties";

    String TAG_ALLOF = "allOf";
    String TAG_ANYOF = "anyOf";
    String TAG_ONEOF = "oneOf";

    String TAG_TITLE = "title";
    String TAG_DESCRIPTION = "description";
    String TAG_CONST = "const";
    String TAG_ENUM = "enum";
    String TAG_DEFAULT = "default";

    String TAG_LENGTH_MIN = "minLength";
    String TAG_LENGTH_MAX = "maxLength";
    String TAG_FORMAT = "format";
    String TAG_PATTERN = "pattern";

    String TAG_MINIMUM = "minimum";
    String TAG_MINIMUM_EXCLUSIVE = "exclusiveMinimum";
    String TAG_MAXIMUM = "maximum";
    String TAG_MAXIMUM_EXCLUSIVE = "exclusiveMaximum";
    String TAG_MULTIPLE_OF = "multipleOf";

    String TAG_ITEMS_MIN = "minItems";
    String TAG_ITEMS_MAX = "maxItems";
    String TAG_ITEMS_UNIQUE = "uniqueItems";
}
