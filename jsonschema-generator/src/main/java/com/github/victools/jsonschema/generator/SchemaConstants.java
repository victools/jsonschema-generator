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

    String TAG_SCHEMA = SchemaKeyword.TAG_SCHEMA.forVersion(SchemaVersion.DRAFT_7);
    String TAG_SCHEMA_DRAFT7 = SchemaKeyword.TAG_SCHEMA_VALUE.forVersion(SchemaVersion.DRAFT_7);
    String TAG_DEFINITIONS = SchemaKeyword.TAG_DEFINITIONS.forVersion(SchemaVersion.DRAFT_7);
    String TAG_REF = SchemaKeyword.TAG_REF.forVersion(SchemaVersion.DRAFT_7);
    String TAG_REF_MAIN = SchemaKeyword.TAG_REF_MAIN.forVersion(SchemaVersion.DRAFT_7);
    String TAG_REF_PREFIX = SchemaKeyword.TAG_REF_PREFIX.forVersion(SchemaVersion.DRAFT_7);

    String TAG_TYPE = SchemaKeyword.TAG_TYPE.forVersion(SchemaVersion.DRAFT_7);
    String TAG_TYPE_NULL = SchemaKeyword.TAG_TYPE_NULL.forVersion(SchemaVersion.DRAFT_7);
    String TAG_TYPE_ARRAY = SchemaKeyword.TAG_TYPE_ARRAY.forVersion(SchemaVersion.DRAFT_7);
    String TAG_TYPE_OBJECT = SchemaKeyword.TAG_TYPE_OBJECT.forVersion(SchemaVersion.DRAFT_7);
    String TAG_TYPE_BOOLEAN = SchemaKeyword.TAG_TYPE_BOOLEAN.forVersion(SchemaVersion.DRAFT_7);
    String TAG_TYPE_STRING = SchemaKeyword.TAG_TYPE_STRING.forVersion(SchemaVersion.DRAFT_7);
    String TAG_TYPE_INTEGER = SchemaKeyword.TAG_TYPE_INTEGER.forVersion(SchemaVersion.DRAFT_7);
    String TAG_TYPE_NUMBER = SchemaKeyword.TAG_TYPE_NUMBER.forVersion(SchemaVersion.DRAFT_7);

    String TAG_PROPERTIES = SchemaKeyword.TAG_PROPERTIES.forVersion(SchemaVersion.DRAFT_7);
    String TAG_ITEMS = SchemaKeyword.TAG_ITEMS.forVersion(SchemaVersion.DRAFT_7);
    String TAG_REQUIRED = SchemaKeyword.TAG_REQUIRED.forVersion(SchemaVersion.DRAFT_7);
    String TAG_ADDITIONAL_PROPERTIES = SchemaKeyword.TAG_ADDITIONAL_PROPERTIES.forVersion(SchemaVersion.DRAFT_7);
    String TAG_PATTERN_PROPERTIES = SchemaKeyword.TAG_PATTERN_PROPERTIES.forVersion(SchemaVersion.DRAFT_7);

    String TAG_ALLOF = SchemaKeyword.TAG_ALLOF.forVersion(SchemaVersion.DRAFT_7);
    String TAG_ANYOF = SchemaKeyword.TAG_ANYOF.forVersion(SchemaVersion.DRAFT_7);
    String TAG_ONEOF = SchemaKeyword.TAG_ONEOF.forVersion(SchemaVersion.DRAFT_7);

    String TAG_TITLE = SchemaKeyword.TAG_TITLE.forVersion(SchemaVersion.DRAFT_7);
    String TAG_DESCRIPTION = SchemaKeyword.TAG_DESCRIPTION.forVersion(SchemaVersion.DRAFT_7);
    String TAG_CONST = SchemaKeyword.TAG_CONST.forVersion(SchemaVersion.DRAFT_7);
    String TAG_ENUM = SchemaKeyword.TAG_ENUM.forVersion(SchemaVersion.DRAFT_7);
    String TAG_DEFAULT = SchemaKeyword.TAG_DEFAULT.forVersion(SchemaVersion.DRAFT_7);

    String TAG_LENGTH_MIN = SchemaKeyword.TAG_LENGTH_MIN.forVersion(SchemaVersion.DRAFT_7);
    String TAG_LENGTH_MAX = SchemaKeyword.TAG_LENGTH_MAX.forVersion(SchemaVersion.DRAFT_7);
    String TAG_FORMAT = SchemaKeyword.TAG_FORMAT.forVersion(SchemaVersion.DRAFT_7);
    String TAG_PATTERN = SchemaKeyword.TAG_PATTERN.forVersion(SchemaVersion.DRAFT_7);

    String TAG_MINIMUM = SchemaKeyword.TAG_MINIMUM.forVersion(SchemaVersion.DRAFT_7);
    String TAG_MINIMUM_EXCLUSIVE = SchemaKeyword.TAG_MINIMUM_EXCLUSIVE.forVersion(SchemaVersion.DRAFT_7);
    String TAG_MAXIMUM = SchemaKeyword.TAG_MAXIMUM.forVersion(SchemaVersion.DRAFT_7);
    String TAG_MAXIMUM_EXCLUSIVE = SchemaKeyword.TAG_MAXIMUM_EXCLUSIVE.forVersion(SchemaVersion.DRAFT_7);
    String TAG_MULTIPLE_OF = SchemaKeyword.TAG_MULTIPLE_OF.forVersion(SchemaVersion.DRAFT_7);

    String TAG_ITEMS_MIN = SchemaKeyword.TAG_ITEMS_MIN.forVersion(SchemaVersion.DRAFT_7);
    String TAG_ITEMS_MAX = SchemaKeyword.TAG_ITEMS_MAX.forVersion(SchemaVersion.DRAFT_7);
    String TAG_ITEMS_UNIQUE = SchemaKeyword.TAG_ITEMS_UNIQUE.forVersion(SchemaVersion.DRAFT_7);
}
