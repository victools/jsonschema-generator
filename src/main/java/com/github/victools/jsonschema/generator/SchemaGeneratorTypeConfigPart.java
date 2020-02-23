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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generic collection of reflection based analysis for populating a JSON Schema.
 *
 * @param <S> type of the scope/type representation to analyse
 */
public class SchemaGeneratorTypeConfigPart<S extends TypeScope> {

    /**
     * Helper function for invoking a given function with the provided inputs or returning null no function returning anything but null themselves.
     *
     * @param <S> type of the targeted scope/type representation (to be forwarded as parameter to the given function)
     * @param <R> type of the expected return value (of the given function)
     * @param resolvers functions to invoke and return the first non-null result from
     * @param scope targeted scope (to be forwarded as first argument to a given function)
     * @return return value of successfully invoked function or null
     */
    protected static <S extends TypeScope, R> R getFirstDefinedValue(List<ConfigFunction<S, R>> resolvers, S scope) {
        return resolvers.stream()
                .map(resolver -> resolver.apply(scope))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /*
     * General fields independent of "type".
     */
    private final List<ConfigFunction<S, String>> titleResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, String>> descriptionResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, Object>> defaultResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, Collection<?>>> enumResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, Type>> additionalPropertiesResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, Map<String, Type>>> patternPropertiesResolvers = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "string".
     */
    private final List<ConfigFunction<S, Integer>> stringMinLengthResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, Integer>> stringMaxLengthResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, String>> stringFormatResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, String>> stringPatternResolvers = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "integer" or "number".
     */
    private final List<ConfigFunction<S, BigDecimal>> numberInclusiveMinimumResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, BigDecimal>> numberExclusiveMinimumResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, BigDecimal>> numberInclusiveMaximumResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, BigDecimal>> numberExclusiveMaximumResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, BigDecimal>> numberMultipleOfResolvers = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "array".
     */
    private final List<ConfigFunction<S, Integer>> arrayMinItemsResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, Integer>> arrayMaxItemsResolvers = new ArrayList<>();
    private final List<ConfigFunction<S, Boolean>> arrayUniqueItemsResolvers = new ArrayList<>();

    /**
     * Setter for "title" resolver.
     *
     * @param resolver how to determine the "title" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withTitleResolver(ConfigFunction<S, String> resolver) {
        this.titleResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "title" of a given scope/type representation.
     *
     * @param scope scope to determine "title" value for
     * @return "title" in a JSON Schema (may be null)
     */
    public String resolveTitle(S scope) {
        return getFirstDefinedValue(this.titleResolvers, scope);
    }

    /**
     * Setter for "description" resolver.
     *
     * @param resolver how to determine the "description" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withDescriptionResolver(ConfigFunction<S, String> resolver) {
        this.descriptionResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "description" of a given scope/type representation.
     *
     * @param scope scope to determine "description" value for
     * @return "description" in a JSON Schema (may be null)
     */
    public String resolveDescription(S scope) {
        return getFirstDefinedValue(this.descriptionResolvers, scope);
    }

    /**
     * Setter for "default" resolver.
     *
     * @param resolver how to determine the "default" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withDefaultResolver(ConfigFunction<S, Object> resolver) {
        this.defaultResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "default" of a given scope/type representation.
     *
     * @param scope scope to determine "default" value for
     * @return "default" in a JSON Schema (may be null)
     */
    public Object resolveDefault(S scope) {
        return getFirstDefinedValue(this.defaultResolvers, scope);
    }

    /**
     * Setter for "enum"/"const" resolver.
     *
     * @param resolver how to determine the "enum"/"const" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withEnumResolver(ConfigFunction<S, Collection<?>> resolver) {
        this.enumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "enum"/"const" of a given scope/type representation.
     *
     * @param scope scope to determine "enum"/"const" value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    public Collection<?> resolveEnum(S scope) {
        return getFirstDefinedValue(this.enumResolvers, scope);
    }

    /**
     * Setter for "additionalProperties" resolver. If the returned type is {@link Void} "false" will be set, otherwise an appropriate sub-schema.
     *
     * @param resolver how to determine the "additionalProperties" of a JSON Schema, returning {@link Void} will result in "false"
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withAdditionalPropertiesResolver(ConfigFunction<S, Type> resolver) {
        this.additionalPropertiesResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "additionalProperties" of a given scope/type representation.
     *
     * @param scope scope to determine "additionalProperties" value for
     * @return "additionalProperties" in a JSON Schema (may be null)
     */
    public Type resolveAdditionalProperties(S scope) {
        return getFirstDefinedValue(this.additionalPropertiesResolvers, scope);
    }

    /**
     * Setter for "patternProperties" resolver. The map's keys are representing the patterns and the mapped values their corresponding types.
     *
     * @param resolver how to determine the "patternProperties" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withPatternPropertiesResolver(ConfigFunction<S, Map<String, Type>> resolver) {
        this.patternPropertiesResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "patternProperties" of a given scope/type representation.
     *
     * @param scope scope to determine "patternProperties" value for
     * @return "patternProperties" in a JSON Schema (may be null), the keys representing the patterns and the mapped values their corresponding types
     */
    public Map<String, Type> resolvePatternProperties(S scope) {
        return getFirstDefinedValue(this.patternPropertiesResolvers, scope);
    }

    /**
     * Setter for "minLength" resolver.
     *
     * @param resolver how to determine the "minLength" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withStringMinLengthResolver(ConfigFunction<S, Integer> resolver) {
        this.stringMinLengthResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minLength" of a given scope/type representation.
     *
     * @param scope scope to determine "minLength" value for
     * @return "minLength" in a JSON Schema (may be null)
     */
    public Integer resolveStringMinLength(S scope) {
        return getFirstDefinedValue(this.stringMinLengthResolvers, scope);
    }

    /**
     * Setter for "maxLength" resolver.
     *
     * @param resolver how to determine the "maxLength" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withStringMaxLengthResolver(ConfigFunction<S, Integer> resolver) {
        this.stringMaxLengthResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maxLength" of a given scope/type representation.
     *
     * @param scope scope to determine "maxLength" value for
     * @return "maxLength" in a JSON Schema (may be null)
     */
    public Integer resolveStringMaxLength(S scope) {
        return getFirstDefinedValue(this.stringMaxLengthResolvers, scope);
    }

    /**
     * Setter for "format" resolver.
     *
     * @param resolver how to determine the "format" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withStringFormatResolver(ConfigFunction<S, String> resolver) {
        this.stringFormatResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "format" of a given scope/type representation.
     *
     * @param scope scope to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    public String resolveStringFormat(S scope) {
        return getFirstDefinedValue(this.stringFormatResolvers, scope);
    }

    /**
     * Setter for "format" resolver.
     *
     * @param resolver how to determine the "format" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withStringPatternResolver(ConfigFunction<S, String> resolver) {
        this.stringPatternResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "format" of a given scope/type representation.
     *
     * @param scope scope to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    public String resolveStringPattern(S scope) {
        return getFirstDefinedValue(this.stringPatternResolvers, scope);
    }

    /**
     * Setter for "minimum" resolver.
     *
     * @param resolver how to determine the "minimum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withNumberInclusiveMinimumResolver(ConfigFunction<S, BigDecimal> resolver) {
        this.numberInclusiveMinimumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minimum" of a given scope/type representation.
     *
     * @param scope scope to determine "minimum" value for
     * @return "minimum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberInclusiveMinimum(S scope) {
        return getFirstDefinedValue(this.numberInclusiveMinimumResolvers, scope);
    }

    /**
     * Setter for "exclusiveMinimum" resolver.
     *
     * @param resolver how to determine the "exclusiveMinimum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withNumberExclusiveMinimumResolver(ConfigFunction<S, BigDecimal> resolver) {
        this.numberExclusiveMinimumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "exclusiveMinimum" of a given scope/type representation.
     *
     * @param scope scope to determine "exclusiveMinimum" value for
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberExclusiveMinimum(S scope) {
        return getFirstDefinedValue(this.numberExclusiveMinimumResolvers, scope);
    }

    /**
     * Setter for "maximum" resolver.
     *
     * @param resolver how to determine the "maximum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withNumberInclusiveMaximumResolver(ConfigFunction<S, BigDecimal> resolver) {
        this.numberInclusiveMaximumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maximum" of a given scope/type representation.
     *
     * @param scope scope to determine "maximum" value for
     * @return "maximum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberInclusiveMaximum(S scope) {
        return getFirstDefinedValue(this.numberInclusiveMaximumResolvers, scope);
    }

    /**
     * Setter for "exclusiveMaximum" resolver.
     *
     * @param resolver how to determine the "exclusiveMaximum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withNumberExclusiveMaximumResolver(ConfigFunction<S, BigDecimal> resolver) {
        this.numberExclusiveMaximumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "exclusiveMaximum" of a given scope/type representation.
     *
     * @param scope scope to determine "exclusiveMaximum" value for
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberExclusiveMaximum(S scope) {
        return getFirstDefinedValue(this.numberExclusiveMaximumResolvers, scope);
    }

    /**
     * Setter for "multipleOf" resolver.
     *
     * @param resolver how to determine the "multipleOf" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withNumberMultipleOfResolver(ConfigFunction<S, BigDecimal> resolver) {
        this.numberMultipleOfResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "multipleOf" of a given scope/type representation.
     *
     * @param scope scope to determine "multipleOf" value for
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberMultipleOf(S scope) {
        return getFirstDefinedValue(this.numberMultipleOfResolvers, scope);
    }

    /**
     * Setter for "minItems" resolver.
     *
     * @param resolver how to determine the "minItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withArrayMinItemsResolver(ConfigFunction<S, Integer> resolver) {
        this.arrayMinItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minItems" of a given scope/type representation.
     *
     * @param scope scope to determine "minItems" value for
     * @return "minItems" in a JSON Schema (may be null)
     */
    public Integer resolveArrayMinItems(S scope) {
        return getFirstDefinedValue(this.arrayMinItemsResolvers, scope);
    }

    /**
     * Setter for "maxItems" resolver.
     *
     * @param resolver how to determine the "maxItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withArrayMaxItemsResolver(ConfigFunction<S, Integer> resolver) {
        this.arrayMaxItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maxItems" of a given scope/type representation.
     *
     * @param scope scope to determine "maxItems" value for
     * @return "maxItems" in a JSON Schema (may be null)
     */
    public Integer resolveArrayMaxItems(S scope) {
        return getFirstDefinedValue(this.arrayMaxItemsResolvers, scope);
    }

    /**
     * Setter for "uniqueItems" resolver.
     *
     * @param resolver how to determine the "uniqueItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorTypeConfigPart<S> withArrayUniqueItemsResolver(ConfigFunction<S, Boolean> resolver) {
        this.arrayUniqueItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "uniqueItems" of a given scope/type representation.
     *
     * @param scope scope to determine "uniqueItems" value for
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    public Boolean resolveArrayUniqueItems(S scope) {
        return getFirstDefinedValue(this.arrayUniqueItemsResolvers, scope);
    }
}
