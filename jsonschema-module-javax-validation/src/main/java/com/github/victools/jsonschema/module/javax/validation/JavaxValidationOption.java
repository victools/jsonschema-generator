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

package com.github.victools.jsonschema.module.javax.validation;

/**
 * Flags to enable/disable certain aspects of the {@link JavaxValidationModule}'s processing.
 */
public enum JavaxValidationOption {
    /**
     * Use this option to add not-nullable fields to their parent's list of "required" properties.
     */
    NOT_NULLABLE_FIELD_IS_REQUIRED,
    /**
     * Use this option to add not-nullable methods to their parent's list of "required" properties.
     */
    NOT_NULLABLE_METHOD_IS_REQUIRED,
    /**
     * Use this option to indicate the "idn-email" format instead of "email" when an {@code @Email} annotation is being found.
     */
    PREFER_IDN_EMAIL_FORMAT,
    /**
     * Use this option to include a string's "pattern" according to {@code @Pattern(regexp = "...")} or {@code @Email(regexp = "...")}.
     */
    INCLUDE_PATTERN_EXPRESSIONS;

}
