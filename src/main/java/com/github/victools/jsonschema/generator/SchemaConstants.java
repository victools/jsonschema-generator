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
 *
 * @deprecated this only covers Draft 7; use {@link SchemaKeyword} instead
 */
@Deprecated
public interface SchemaConstants {

    String TAG_SCHEMA = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_SCHEMA);
    String TAG_SCHEMA_DRAFT7 = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_SCHEMA_VALUE);
    String TAG_DEFINITIONS = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_DEFINITIONS);
    String TAG_REF = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_REF);
    String TAG_REF_MAIN = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_REF_MAIN);
    String TAG_REF_PREFIX = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_REF_PREFIX);

    String TAG_TYPE = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TYPE);
    String TAG_TYPE_NULL = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TYPE_NULL);
    String TAG_TYPE_ARRAY = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TYPE_ARRAY);
    String TAG_TYPE_OBJECT = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TYPE_OBJECT);
    String TAG_TYPE_BOOLEAN = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TYPE_BOOLEAN);
    String TAG_TYPE_STRING = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TYPE_STRING);
    String TAG_TYPE_INTEGER = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TYPE_INTEGER);
    String TAG_TYPE_NUMBER = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TYPE_NUMBER);

    String TAG_PROPERTIES = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_PROPERTIES);
    String TAG_ITEMS = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ITEMS);
    String TAG_REQUIRED = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_REQUIRED);
    String TAG_ADDITIONAL_PROPERTIES = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES);
    String TAG_PATTERN_PROPERTIES = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_PATTERN_PROPERTIES);

    String TAG_ALLOF = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ALLOF);
    String TAG_ANYOF = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ANYOF);
    String TAG_ONEOF = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ONEOF);

    String TAG_TITLE = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_TITLE);
    String TAG_DESCRIPTION = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_DESCRIPTION);
    String TAG_CONST = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_CONST);
    String TAG_ENUM = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ENUM);
    String TAG_DEFAULT = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_DEFAULT);

    String TAG_LENGTH_MIN = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_LENGTH_MIN);
    String TAG_LENGTH_MAX = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_LENGTH_MAX);
    String TAG_FORMAT = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_FORMAT);
    String TAG_PATTERN = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_PATTERN);

    String TAG_MINIMUM = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_MINIMUM);
    String TAG_MINIMUM_EXCLUSIVE = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_MINIMUM_EXCLUSIVE);
    String TAG_MAXIMUM = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_MAXIMUM);
    String TAG_MAXIMUM_EXCLUSIVE = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_MAXIMUM_EXCLUSIVE);
    String TAG_MULTIPLE_OF = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_MULTIPLE_OF);

    String TAG_ITEMS_MIN = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ITEMS_MIN);
    String TAG_ITEMS_MAX = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ITEMS_MAX);
    String TAG_ITEMS_UNIQUE = SchemaVersion.DRAFT_7.get(SchemaKeyword.TAG_ITEMS_UNIQUE);
}
