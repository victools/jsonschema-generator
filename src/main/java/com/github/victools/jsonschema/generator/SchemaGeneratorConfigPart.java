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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic collection of reflection based analysis for populating a JSON Schema from a certain kind of reference/context.
 *
 * @param <O> type of the reference/context to analyse
 */
public class SchemaGeneratorConfigPart<O> {

    /**
     * Helper function for invoking a given function with the provided input or returning null no function returning anything but null themselves.
     *
     * @param <O> type of the provided reference/context (to be forwarded as single parameter to the given function)
     * @param <R> type of the expected return value (of the given function)
     * @param resolvers functions to invoke and return the first non-null result from
     * @param origin reference/context (to be forwarded as single parameter to the given function)
     * @return return value of successfully invoked function or null
     */
    private static <O, R> R getFirstDefinedValue(List<Function<O, R>> resolvers, O origin) {
        return resolvers.stream()
                .map(resolver -> resolver.apply(origin))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Helper function for invoking a given function with the provided inputs or returning null no function returning anything but null themselves.
     *
     * @param <O> type of the provided reference/context (to be forwarded as first parameter to the given function)
     * @param <P> type of the second parameter
     * @param <R> type of the expected return value (of the given function)
     * @param resolvers functions to invoke and return the first non-null result from
     * @param origin reference/context (to be forwarded as single parameter to the given function)
     * @param secondParameter second parameter to forward (e.g. indicating the standard value to be applied if not overridden)
     * @return return value of successfully invoked function or null
     */
    private static <O, P, R> R getFirstDefinedValue(List<BiFunction<O, P, R>> resolvers, O origin, P secondParameter) {
        return resolvers.stream()
                .map(resolver -> resolver.apply(origin, secondParameter))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /*
     * Customising options for properties in a schema with "type": object; either skipping them completely or also allowing for "type": "null".
     */
    private final List<Predicate<O>> ignoreChecks = new ArrayList<>();
    private final List<Function<O, Boolean>> nullableChecks = new ArrayList<>();

    /*
     * Customising options for the names of properties in a schema with "type": "object".
     */
    private final List<BiFunction<O, Type, Type>> targetTypeOverrideResolvers = new ArrayList<>();
    private final List<BiFunction<O, String, String>> propertyNameOverrideResolvers = new ArrayList<>();

    /*
     * General fields independent of "type".
     */
    private final List<Function<O, String>> titleResolvers = new ArrayList<>();
    private final List<Function<O, String>> descriptionResolvers = new ArrayList<>();
    private final List<Function<O, Collection<?>>> enumResolvers = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "string".
     */
    private final List<Function<O, Integer>> stringMinLengthResolvers = new ArrayList<>();
    private final List<Function<O, Integer>> stringMaxLengthResolvers = new ArrayList<>();
    private final List<Function<O, String>> stringFormatResolvers = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "integer" or "number".
     */
    private final List<Function<O, BigDecimal>> numberInclusiveMinimumResolvers = new ArrayList<>();
    private final List<Function<O, BigDecimal>> numberExclusiveMinimumResolvers = new ArrayList<>();
    private final List<Function<O, BigDecimal>> numberInclusiveMaximumResolvers = new ArrayList<>();
    private final List<Function<O, BigDecimal>> numberExclusiveMaximumResolvers = new ArrayList<>();
    private final List<Function<O, BigDecimal>> numberMultipleOfResolvers = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "array".
     */
    private final List<Function<O, Integer>> arrayMinItemsResolvers = new ArrayList<>();
    private final List<Function<O, Integer>> arrayMaxItemsResolvers = new ArrayList<>();
    private final List<Function<O, Boolean>> arrayUniqueItemsResolvers = new ArrayList<>();

    /**
     * Setter for ignore check.
     *
     * @param check how to determine whether a given reference should be ignored
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addIgnoreCheck(Predicate<O> check) {
        this.ignoreChecks.add(check);
        return this;
    }

    /**
     * Determine whether a given reference/context should be ignored.
     *
     * @param origin reference/context to check
     * @return whether the reference/context should be ignored (defaults to false)
     */
    public boolean shouldIgnore(O origin) {
        return this.ignoreChecks.stream().anyMatch(check -> check.test(origin));
    }

    /**
     * Setter for nullable check.
     *
     * @param check how to determine whether a given reference should be nullable
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addNullableCheck(Function<O, Boolean> check) {
        this.nullableChecks.add(check);
        return this;
    }

    /**
     * Determine whether a given reference/context is nullable.
     *
     * @param origin reference/context to check
     * @return whether the reference/context is nullable (may be null if not specified)
     */
    public Boolean isNullable(O origin) {
        Set<Boolean> result = this.nullableChecks.stream()
                .map(check -> check.apply(origin))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return result.isEmpty() ? null : result.stream().anyMatch(value -> value);
    }

    /**
     * Setter for target type resolver, expecting the respective reference/context and the default type as inputs.
     *
     * @param resolver how to determine the alternative target type
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addTargetTypeOverrideResolver(BiFunction<O, Type, Type> resolver) {
        this.targetTypeOverrideResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the alternative target type from a given reference/context.
     *
     * @param origin reference/context to determine the target type for
     * @param defaultType default type to be used if no override value is being provided
     * @return target type of reference/context (may be null)
     */
    public Type resolveTargetTypeOverride(O origin, Type defaultType) {
        return getFirstDefinedValue(this.targetTypeOverrideResolvers, origin, defaultType);
    }

    /**
     * Setter for property name resolver, expecting the respective reference/context and the default name as inputs.
     *
     * @param resolver how to determine the alternative name in a parent JSON Schema's "properties"
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addPropertyNameOverrideResolver(BiFunction<O, String, String> resolver) {
        this.propertyNameOverrideResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the alternative name in a parent JSON Schema's "properties" from a given reference/context.
     *
     * @param origin reference/context to determine the property name for
     * @param defaultName default name to be set if no override value is being provided
     * @return name in a parent JSON Schema's "properties" (may be null, thereby falling back on the default value)
     */
    public String resolvePropertyNameOverride(O origin, String defaultName) {
        return getFirstDefinedValue(this.propertyNameOverrideResolvers, origin, defaultName);
    }

    /**
     * Setter for "title" resolver.
     *
     * @param resolver how to determine the "title" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addTitleResolver(Function<O, String> resolver) {
        this.titleResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "title" of a given reference/context.
     *
     * @param origin reference/context to determine "title" value for
     * @return "title" in a JSON Schema (may be null)
     */
    public String resolveTitle(O origin) {
        return getFirstDefinedValue(this.titleResolvers, origin);
    }

    /**
     * Setter for "description" resolver.
     *
     * @param resolver how to determine the "description" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addDescriptionResolver(Function<O, String> resolver) {
        this.descriptionResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "description" of a given reference/context.
     *
     * @param origin reference/context to determine "description" value for
     * @return "description" in a JSON Schema (may be null)
     */
    public String resolveDescription(O origin) {
        return getFirstDefinedValue(this.descriptionResolvers, origin);
    }

    /**
     * Setter for "enum"/"const" resolver.
     *
     * @param resolver how to determine the "enum"/"const" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addEnumResolver(Function<O, Collection<?>> resolver) {
        this.enumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "enum"/"const" of a given reference/context.
     *
     * @param origin reference/context to determine "enum"/"const" value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    public Collection<?> resolveEnum(O origin) {
        return getFirstDefinedValue(this.enumResolvers, origin);
    }

    /**
     * Setter for "minLength" resolver.
     *
     * @param resolver how to determine the "minLength" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addStringMinLengthResolver(Function<O, Integer> resolver) {
        this.stringMinLengthResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minLength" of a given reference/context.
     *
     * @param origin reference/context to determine "minLength" value for
     * @return "minLength" in a JSON Schema (may be null)
     */
    public Integer resolveStringMinLength(O origin) {
        return getFirstDefinedValue(this.stringMinLengthResolvers, origin);
    }

    /**
     * Setter for "maxLength" resolver.
     *
     * @param resolver how to determine the "maxLength" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addStringMaxLengthResolver(Function<O, Integer> resolver) {
        this.stringMaxLengthResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maxLength" of a given reference/context.
     *
     * @param origin reference/context to determine "maxLength" value for
     * @return "maxLength" in a JSON Schema (may be null)
     */
    public Integer resolveStringMaxLength(O origin) {
        return getFirstDefinedValue(this.stringMaxLengthResolvers, origin);
    }

    /**
     * Setter for "format" resolver.
     *
     * @param resolver how to determine the "format" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addStringFormatResolver(Function<O, String> resolver) {
        this.stringFormatResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "format" of a given reference/context.
     *
     * @param origin reference/context to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    public String resolveStringFormat(O origin) {
        return getFirstDefinedValue(this.stringFormatResolvers, origin);
    }

    /**
     * Setter for "minimum" resolver.
     *
     * @param resolver how to determine the "minimum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addNumberInclusiveMinimumResolver(Function<O, BigDecimal> resolver) {
        this.numberInclusiveMinimumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minimum" of a given reference/context.
     *
     * @param origin reference/context to determine "minimum" value for
     * @return "minimum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberInclusiveMinimum(O origin) {
        return getFirstDefinedValue(this.numberInclusiveMinimumResolvers, origin);
    }

    /**
     * Setter for "exclusiveMinimum" resolver.
     *
     * @param resolver how to determine the "exclusiveMinimum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addNumberExclusiveMinimumResolver(Function<O, BigDecimal> resolver) {
        this.numberExclusiveMinimumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "exclusiveMinimum" of a given reference/context.
     *
     * @param origin reference/context to determine "exclusiveMinimum" value for
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberExclusiveMinimum(O origin) {
        return getFirstDefinedValue(this.numberExclusiveMinimumResolvers, origin);
    }

    /**
     * Setter for "maximum" resolver.
     *
     * @param resolver how to determine the "maximum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addNumberInclusiveMaximumResolver(Function<O, BigDecimal> resolver) {
        this.numberInclusiveMaximumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maximum" of a given reference/context.
     *
     * @param origin reference/context to determine "maximum" value for
     * @return "maximum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberInclusiveMaximum(O origin) {
        return getFirstDefinedValue(this.numberInclusiveMaximumResolvers, origin);
    }

    /**
     * Setter for "exclusiveMaximum" resolver.
     *
     * @param resolver how to determine the "exclusiveMaximum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addNumberExclusiveMaximumResolver(Function<O, BigDecimal> resolver) {
        this.numberExclusiveMaximumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "exclusiveMaximum" of a given reference/context.
     *
     * @param origin reference/context to determine "exclusiveMaximum" value for
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberExclusiveMaximum(O origin) {
        return getFirstDefinedValue(this.numberExclusiveMaximumResolvers, origin);
    }

    /**
     * Setter for "multipleOf" resolver.
     *
     * @param resolver how to determine the "multipleOf" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addNumberMultipleOfResolver(Function<O, BigDecimal> resolver) {
        this.numberMultipleOfResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "multipleOf" of a given reference/context.
     *
     * @param origin reference/context to determine "multipleOf" value for
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberMultipleOf(O origin) {
        return getFirstDefinedValue(this.numberMultipleOfResolvers, origin);
    }

    /**
     * Setter for "minItems" resolver.
     *
     * @param resolver how to determine the "minItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addArrayMinItemsResolver(Function<O, Integer> resolver) {
        this.arrayMinItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minItems" of a given reference/context.
     *
     * @param origin reference/context to determine "minItems" value for
     * @return "minItems" in a JSON Schema (may be null)
     */
    public Integer resolveArrayMinItems(O origin) {
        return getFirstDefinedValue(this.arrayMinItemsResolvers, origin);
    }

    /**
     * Setter for "maxItems" resolver.
     *
     * @param resolver how to determine the "maxItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addArrayMaxItemsResolver(Function<O, Integer> resolver) {
        this.arrayMaxItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maxItems" of a given reference/context.
     *
     * @param origin reference/context to determine "maxItems" value for
     * @return "maxItems" in a JSON Schema (may be null)
     */
    public Integer resolveArrayMaxItems(O origin) {
        return getFirstDefinedValue(this.arrayMaxItemsResolvers, origin);
    }

    /**
     * Setter for "uniqueItems" resolver.
     *
     * @param resolver how to determine the "uniqueItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<O> addArrayUniqueItemsResolver(Function<O, Boolean> resolver) {
        this.arrayUniqueItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "uniqueItems" of a given reference/context.
     *
     * @param origin reference/context to determine "uniqueItems" value for
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    public Boolean resolveArrayUniqueItems(O origin) {
        return getFirstDefinedValue(this.arrayUniqueItemsResolvers, origin);
    }
}
