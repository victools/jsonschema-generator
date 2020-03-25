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

package com.github.victools.jsonschema.module.jackson;

/**
 * Flags to enable/disable certain aspects of the {@link JacksonModule}'s processing.
 */
public enum JacksonOption {
    /**
     * Use this option to treat enum types with a {@link com.fasterxml.jackson.annotation.JsonValue JsonValue} annotation on one of its methods as
     * plain strings in the generated schema. If no such annotation with {@code value = true} is present on exactly one argument-free method, it will
     * fall-back on following custom definitions (e.g. from one of the standard generator {@code Option}s).
     *
     * @see com.github.victools.jsonschema.generator.Option#FLATTENED_ENUMS
     * @see com.github.victools.jsonschema.generator.Option#FLATTENED_ENUMS_FROM_TOSTRING
     */
    FLATTENED_ENUMS_FROM_JSONVALUE;
}
