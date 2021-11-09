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
     * <br>
     * This can be enabled at the same time as the {@link #FLATTENED_ENUMS_FROM_JSONPROPERTY} option, with the {@code JsonValue} annotation taking
     * precedence.
     *
     * @see JacksonOption#FLATTENED_ENUMS_FROM_JSONPROPERTY
     * @see com.github.victools.jsonschema.generator.Option#FLATTENED_ENUMS
     * @see com.github.victools.jsonschema.generator.Option#FLATTENED_ENUMS_FROM_TOSTRING
     */
    FLATTENED_ENUMS_FROM_JSONVALUE,
    /**
     * Use this option to treat enum types with a {@link com.fasterxml.jackson.annotation.JsonProperty JsonProperty} annotation on each of its
     * constants as plain strings in the generated schema. If no such annotation is present on each enum constants, it will fall-back on following
     * custom definitions (e.g. from one of the standard generator {@code Option}s).
     * <br>
     * This can be enabled at the same time as the {@link #FLATTENED_ENUMS_FROM_JSONVALUE} option, with the {@code JsonValue} annotation taking
     * precedence.
     *
     * @see JacksonOption#FLATTENED_ENUMS_FROM_JSONVALUE
     * @see com.github.victools.jsonschema.generator.Option#FLATTENED_ENUMS
     * @see com.github.victools.jsonschema.generator.Option#FLATTENED_ENUMS_FROM_TOSTRING
     */
    FLATTENED_ENUMS_FROM_JSONPROPERTY,
    /**
     * Use this option to sort an object's properties according to associated
     * {@link com.fasterxml.jackson.annotation.JsonPropertyOrder JsonPropertyOrder} annotations. Fields and methods without such an annotation are
     * listed after annotated properties.
     */
    RESPECT_JSONPROPERTY_ORDER,
    /**
     * Use this option to ignore all methods that don't have a {@link com.fasterxml.jackson.annotation.JsonProperty JsonProperty} annotation
     * themselves or in case of getter methods on their associated field. When including non-getter methods with annotated property names, you'll
     * probably want to enable the general {@code Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS} as well, in order to avoid parentheses at the end.
     *
     * @see com.github.victools.jsonschema.generator.Option#FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS
     * @since 4.21.0
     */
    INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS,
    /**
     * Use this option to skip property name changes according to {@link com.fasterxml.jackson.databind.annotation.JsonNaming JsonNaming} annotations.
     */
    IGNORE_PROPERTY_NAMING_STRATEGY,
    /**
     * Use this option to skip the automatic look-up of subtypes according to {@code @JsonSubTypes} annotations.
     */
    SKIP_SUBTYPE_LOOKUP,
    /**
     * Use this option to skip the transformation according to {@code @JsonTypeInfo} annotations (typically used to identify specific subtypes).
     */
    IGNORE_TYPE_INFO_TRANSFORM,
    /**
     * Use this option to include fields annotated with {@code @JsonProperty(required = true)} in the containing type's list of "required" properties.
     *
     * @since 4.18.0
     */
    RESPECT_JSONPROPERTY_REQUIRED
}
