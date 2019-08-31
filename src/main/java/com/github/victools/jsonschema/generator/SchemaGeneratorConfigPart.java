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

import com.fasterxml.classmate.ResolvedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic collection of reflection based analysis for populating a JSON Schema from a certain kind of member.
 *
 * @param <M> type of the (resolved) member to analyse
 */
public class SchemaGeneratorConfigPart<M extends MemberScope<?, ?>> {

    /**
     * Helper function for invoking a given function with the provided inputs or returning null no function returning anything but null themselves.
     *
     * @param <M> type of the provided member (to be forwarded as parameter to the given function)
     * @param <R> type of the expected return value (of the given function)
     * @param resolvers functions to invoke and return the first non-null result from
     * @param member member (to be forwarded as first argument to a given function)
     * @return return value of successfully invoked function or null
     */
    private static <M extends MemberScope<?, ?>, R> R getFirstDefinedValue(List<ConfigFunction<M, R>> resolvers, M member) {
        return resolvers.stream()
                .map(resolver -> resolver.apply(member))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private final List<InstanceAttributeOverride<M>> instanceAttributeOverrides = new ArrayList<>();

    /*
     * Customising options for properties in a schema with "type": object;
     * either skipping them completely, marking them as required or also allowing for "type": "null".
     */
    private final List<Predicate<M>> ignoreChecks = new ArrayList<>();
    private final List<Predicate<M>> requiredChecks = new ArrayList<>();
    private final List<ConfigFunction<M, Boolean>> nullableChecks = new ArrayList<>();

    /*
     * Customising options for the names of properties in a schema with "type": "object".
     */
    private final List<ConfigFunction<M, ResolvedType>> targetTypeOverrideResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, String>> propertyNameOverrideResolvers = new ArrayList<>();

    /*
     * General fields independent of "type".
     */
    private final List<ConfigFunction<M, String>> titleResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, String>> descriptionResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, Object>> defaultResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, Collection<?>>> enumResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, Map<String, String>>> metadatas = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "string".
     */
    private final List<ConfigFunction<M, Integer>> stringMinLengthResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, Integer>> stringMaxLengthResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, String>> stringFormatResolvers = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "integer" or "number".
     */
    private final List<ConfigFunction<M, BigDecimal>> numberInclusiveMinimumResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, BigDecimal>> numberExclusiveMinimumResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, BigDecimal>> numberInclusiveMaximumResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, BigDecimal>> numberExclusiveMaximumResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, BigDecimal>> numberMultipleOfResolvers = new ArrayList<>();

    /*
     * Validation fields relating to a schema with "type": "array".
     */
    private final List<ConfigFunction<M, Integer>> arrayMinItemsResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, Integer>> arrayMaxItemsResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, Boolean>> arrayUniqueItemsResolvers = new ArrayList<>();

    /**
     * Setter for override of attributes on a given JSON Schema node in the respective member.
     *
     * @param override override of a given JSON Schema node's instance attributes
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withInstanceAttributeOverride(InstanceAttributeOverride<M> override) {
        this.instanceAttributeOverrides.add(override);
        return this;
    }

    /**
     * Getter for the applicable instance attribute overrides.
     *
     * @return overrides of a given JSON Schema node's instance attributes
     */
    public List<InstanceAttributeOverride<M>> getInstanceAttributeOverrides() {
        return Collections.unmodifiableList(this.instanceAttributeOverrides);
    }

    /**
     * Setter for ignore check.
     *
     * @param check how to determine whether a given reference should be ignored
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withIgnoreCheck(Predicate<M> check) {
        this.ignoreChecks.add(check);
        return this;
    }

    /**
     * Determine whether a given member should be included – ignoring member if any inclusion check returns false.
     *
     * @param member member to check
     * @return whether the member should be ignored (defaults to false)
     */
    public boolean shouldIgnore(M member) {
        return this.ignoreChecks.stream().anyMatch(check -> check.test(member));
    }

    /**
     * Setter for required check.
     *
     * @param check how to determine whether a given reference should have required value
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withRequiredCheck(Predicate<M> check) {
        this.requiredChecks.add(check);
        return this;
    }

    /**
     * Determine whether a given member should be indicated as being required in its declaring type.
     *
     * @param member member to check
     * @return whether the member is required (defaults to false)
     */
    public boolean isRequired(M member) {
        return this.requiredChecks.stream().anyMatch(check -> check.test(member));
    }

    /**
     * Setter for nullable check.
     *
     * @param check how to determine whether a given reference should be nullable
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withNullableCheck(ConfigFunction<M, Boolean> check) {
        this.nullableChecks.add(check);
        return this;
    }

    /**
     * Determine whether a given member is nullable.
     *
     * @param member member to check
     * @return whether the member is nullable (may be null if not specified)
     */
    public Boolean isNullable(M member) {
        Set<Boolean> result = this.nullableChecks.stream()
                .map(check -> check.apply(member))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return result.isEmpty() ? null : result.stream().anyMatch(value -> value);
    }

    /**
     * Setter for target type resolver, expecting the respective member and the default type as inputs.
     *
     * @param resolver how to determine the alternative target type
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withTargetTypeOverrideResolver(ConfigFunction<M, ResolvedType> resolver) {
        this.targetTypeOverrideResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the alternative target type from a given member.
     *
     * @param member member to determine the target type override for
     * @return target type of member (may be null)
     * @see MemberScope#getDeclaredType()
     * @see MemberScope#getOverriddenType()
     */
    public ResolvedType resolveTargetTypeOverride(M member) {
        return getFirstDefinedValue(this.targetTypeOverrideResolvers, member);
    }

    /**
     * Setter for property name resolver, expecting the respective member and the default name as inputs.
     *
     * @param resolver how to determine the alternative name in a parent JSON Schema's "properties"
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withPropertyNameOverrideResolver(ConfigFunction<M, String> resolver) {
        this.propertyNameOverrideResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the alternative name in a parent JSON Schema's "properties" from a given member.
     *
     * @param member member to determine the property name for
     * @return name in a parent JSON Schema's "properties" (may be null, thereby falling back on the default value)
     */
    public String resolvePropertyNameOverride(M member) {
        return getFirstDefinedValue(this.propertyNameOverrideResolvers, member);
    }

    /**
     * Setter for "title" resolver.
     *
     * @param resolver how to determine the "title" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withTitleResolver(ConfigFunction<M, String> resolver) {
        this.titleResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "title" of a given member.
     *
     * @param member member to determine "title" value for
     * @return "title" in a JSON Schema (may be null)
     */
    public String resolveTitle(M member) {
        return getFirstDefinedValue(this.titleResolvers, member);
    }

    /**
     * Setter for "description" resolver.
     *
     * @param resolver how to determine the "description" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withDescriptionResolver(ConfigFunction<M, String> resolver) {
        this.descriptionResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "description" of a given member.
     *
     * @param member member to determine "description" value for
     * @return "description" in a JSON Schema (may be null)
     */
    public String resolveDescription(M member) {
        return getFirstDefinedValue(this.descriptionResolvers, member);
    }

    /**
     * Setter for "default" resolver.
     *
     * @param resolver how to determine the "default" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withDefaultResolver(ConfigFunction<M, Object> resolver) {
        this.defaultResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "default" of a given member.
     *
     * @param member member to determine "default" value for
     * @return "default" in a JSON Schema (may be null)
     */
    public Object resolveDefault(M member) {
        return getFirstDefinedValue(this.defaultResolvers, member);
    }

    /**
     * Setter for "enum"/"const" resolver.
     *
     * @param resolver how to determine the "enum"/"const" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withEnumResolver(ConfigFunction<M, Collection<?>> resolver) {
        this.enumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "enum"/"const" of a given member.
     *
     * @param member member to determine "enum"/"const" value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    public Collection<?> resolveEnum(M member) {
        return getFirstDefinedValue(this.enumResolvers, member);
    }

    /**
     * Setter for "minLength" resolver.
     *
     * @param resolver how to determine the "minLength" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withStringMinLengthResolver(ConfigFunction<M, Integer> resolver) {
        this.stringMinLengthResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minLength" of a given member.
     *
     * @param member member to determine "minLength" value for
     * @return "minLength" in a JSON Schema (may be null)
     */
    public Integer resolveStringMinLength(M member) {
        return getFirstDefinedValue(this.stringMinLengthResolvers, member);
    }

    /**
     * Setter for "maxLength" resolver.
     *
     * @param resolver how to determine the "maxLength" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withStringMaxLengthResolver(ConfigFunction<M, Integer> resolver) {
        this.stringMaxLengthResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maxLength" of a given member.
     *
     * @param member member to determine "maxLength" value for
     * @return "maxLength" in a JSON Schema (may be null)
     */
    public Integer resolveStringMaxLength(M member) {
        return getFirstDefinedValue(this.stringMaxLengthResolvers, member);
    }

    /**
     * Setter for "format" resolver.
     *
     * @param resolver how to determine the "format" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withStringFormatResolver(ConfigFunction<M, String> resolver) {
        this.stringFormatResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "format" of a given member.
     *
     * @param member member to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    public String resolveStringFormat(M member) {
        return getFirstDefinedValue(this.stringFormatResolvers, member);
    }

    /**
     * Setter for "minimum" resolver.
     *
     * @param resolver how to determine the "minimum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withNumberInclusiveMinimumResolver(ConfigFunction<M, BigDecimal> resolver) {
        this.numberInclusiveMinimumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minimum" of a given member.
     *
     * @param member member to determine "minimum" value for
     * @return "minimum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberInclusiveMinimum(M member) {
        return getFirstDefinedValue(this.numberInclusiveMinimumResolvers, member);
    }

    /**
     * Setter for "exclusiveMinimum" resolver.
     *
     * @param resolver how to determine the "exclusiveMinimum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withNumberExclusiveMinimumResolver(ConfigFunction<M, BigDecimal> resolver) {
        this.numberExclusiveMinimumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "exclusiveMinimum" of a given member.
     *
     * @param member member to determine "exclusiveMinimum" value for
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberExclusiveMinimum(M member) {
        return getFirstDefinedValue(this.numberExclusiveMinimumResolvers, member);
    }

    /**
     * Setter for "maximum" resolver.
     *
     * @param resolver how to determine the "maximum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withNumberInclusiveMaximumResolver(ConfigFunction<M, BigDecimal> resolver) {
        this.numberInclusiveMaximumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maximum" of a given member.
     *
     * @param member member to determine "maximum" value for
     * @return "maximum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberInclusiveMaximum(M member) {
        return getFirstDefinedValue(this.numberInclusiveMaximumResolvers, member);
    }

    /**
     * Setter for "exclusiveMaximum" resolver.
     *
     * @param resolver how to determine the "exclusiveMaximum" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withNumberExclusiveMaximumResolver(ConfigFunction<M, BigDecimal> resolver) {
        this.numberExclusiveMaximumResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "exclusiveMaximum" of a given member.
     *
     * @param member member to determine "exclusiveMaximum" value for
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberExclusiveMaximum(M member) {
        return getFirstDefinedValue(this.numberExclusiveMaximumResolvers, member);
    }

    /**
     * Setter for "multipleOf" resolver.
     *
     * @param resolver how to determine the "multipleOf" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withNumberMultipleOfResolver(ConfigFunction<M, BigDecimal> resolver) {
        this.numberMultipleOfResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "multipleOf" of a given member.
     *
     * @param member member to determine "multipleOf" value for
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    public BigDecimal resolveNumberMultipleOf(M member) {
        return getFirstDefinedValue(this.numberMultipleOfResolvers, member);
    }

    /**
     * Setter for "minItems" resolver.
     *
     * @param resolver how to determine the "minItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withArrayMinItemsResolver(ConfigFunction<M, Integer> resolver) {
        this.arrayMinItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "minItems" of a given member.
     *
     * @param member member to determine "minItems" value for
     * @return "minItems" in a JSON Schema (may be null)
     */
    public Integer resolveArrayMinItems(M member) {
        return getFirstDefinedValue(this.arrayMinItemsResolvers, member);
    }

    /**
     * Setter for "maxItems" resolver.
     *
     * @param resolver how to determine the "maxItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withArrayMaxItemsResolver(ConfigFunction<M, Integer> resolver) {
        this.arrayMaxItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "maxItems" of a given member.
     *
     * @param member member to determine "maxItems" value for
     * @return "maxItems" in a JSON Schema (may be null)
     */
    public Integer resolveArrayMaxItems(M member) {
        return getFirstDefinedValue(this.arrayMaxItemsResolvers, member);
    }

    /**
     * Setter for "uniqueItems" resolver.
     *
     * @param resolver how to determine the "uniqueItems" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withArrayUniqueItemsResolver(ConfigFunction<M, Boolean> resolver) {
        this.arrayUniqueItemsResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "uniqueItems" of a given member.
     *
     * @param member member to determine "uniqueItems" value for
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    public Boolean resolveArrayUniqueItems(M member) {
        return getFirstDefinedValue(this.arrayUniqueItemsResolvers, member);
    }
}
