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

import com.github.victools.jsonschema.generator.impl.PropertySortUtils;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Generic collection of reflection based analysis for populating a JSON Schema targeting a specific type in general.
 */
public class SchemaGeneratorGeneralConfigPart extends SchemaGeneratorTypeConfigPart<TypeScope> {

    private Comparator<MemberScope<?, ?>> propertySorter = PropertySortUtils.DEFAULT_PROPERTY_ORDER;
    private SchemaDefinitionNamingStrategy definitionNamingStrategy = null;

    private final List<CustomDefinitionProviderV2> customDefinitionProviders = new ArrayList<>();
    private final List<SubtypeResolver> subtypeResolvers = new ArrayList<>();
    private final List<TypeAttributeOverrideV2> typeAttributeOverrides = new ArrayList<>();

    private final List<ConfigFunction<TypeScope, String>> idResolvers = new ArrayList<>();
    private final List<ConfigFunction<TypeScope, String>> anchorResolvers = new ArrayList<>();

    /**
     * Replacing the current sorting algorithm of properties (fields and methods).
     *
     * @param propertySorter sorting algorithm for an object's properties
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorGeneralConfigPart withPropertySorter(Comparator<MemberScope<?, ?>> propertySorter) {
        this.propertySorter = propertySorter;
        return this;
    }

    /**
     * Getter for the sorting algorithm for an object's properties (fields and methods).
     *
     * @return applicable {@link Comparator} for an object's properties
     */
    public Comparator<MemberScope<?, ?>> getPropertySorter() {
        return this.propertySorter;
    }

    /**
     * Replacing the current naming strategy for keys in the "definitions"/"$defs".
     *
     * @param namingStrategy naming strategy for "definitions"/"$defs" keys
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorGeneralConfigPart withDefinitionNamingStrategy(SchemaDefinitionNamingStrategy namingStrategy) {
        this.definitionNamingStrategy = namingStrategy;
        return this;
    }

    /**
     * Getter for the current naming strategy for keys in the "definitions"/"$defs".
     *
     * @return applicable naming strategy for "definitions"/"$defs" keys (or {@code null} if the a default strategy should be used)
     */
    public SchemaDefinitionNamingStrategy getDefinitionNamingStrategy() {
        return this.definitionNamingStrategy;
    }

    /**
     * Adding a custom schema provider – if it returns null for a given type, the next definition provider will be applied.
     * <br>
     * If all custom schema providers return null (or there is none), then the standard behaviour applies.
     *
     * @param definitionProvider provider of a custom definition to register, which may return null
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorGeneralConfigPart withCustomDefinitionProvider(CustomDefinitionProviderV2 definitionProvider) {
        this.customDefinitionProviders.add(definitionProvider);
        return this;
    }

    /**
     * Getter for the applicable custom definition provider.
     *
     * @return providers for certain custom definitions by-passing the default schema generation to some extent
     */
    public List<CustomDefinitionProviderV2> getCustomDefinitionProviders() {
        return Collections.unmodifiableList(this.customDefinitionProviders);
    }

    /**
     * Adding a subtype resolver – if it returns null for a given type, the next subtype resolver will be applied.
     * <br>
     * If all subtype resolvers return null, there is none or a resolver returns an empty list, then the standard behaviour applies.
     *
     * @param subtypeResolver resolver for looking up a declared type's subtypes in order to list those specifically
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorGeneralConfigPart withSubtypeResolver(SubtypeResolver subtypeResolver) {
        this.subtypeResolvers.add(subtypeResolver);
        return this;
    }

    /**
     * Getter for the applicable subtype resolvers.
     *
     * @return registered subtype resolvers
     */
    public List<SubtypeResolver> getSubtypeResolvers() {
        return Collections.unmodifiableList(this.subtypeResolvers);
    }

    /**
     * Adding an override for type attributes – all of the registered overrides will be applied in the order of having been added.
     *
     * @param override adding/removing attributes on a JSON Schema node – specifically intended for attributes relating to the type in general.
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorGeneralConfigPart withTypeAttributeOverride(TypeAttributeOverrideV2 override) {
        this.typeAttributeOverrides.add(override);
        return this;
    }

    /**
     * Getter for the applicable overrides for type attributes.
     *
     * @return registered overrides to be applied in the given order
     */
    public List<TypeAttributeOverrideV2> getTypeAttributeOverrides() {
        return Collections.unmodifiableList(this.typeAttributeOverrides);
    }

    /**
     * Setter for "$id" resolver.
     *
     * @param resolver how to determine the "$id" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorGeneralConfigPart withIdResolver(ConfigFunction<TypeScope, String> resolver) {
        this.idResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "$id" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "$id" value for
     * @return "$id" in a JSON Schema (may be null)
     */
    public String resolveId(TypeScope scope) {
        return SchemaGeneratorGeneralConfigPart.getFirstDefinedValue(this.idResolvers, scope);
    }

    /**
     * Setter for "$anchor" resolver.
     *
     * @param resolver how to determine the "$anchor" of a JSON Schema
     * @return this config part (for chaining)
     */
    public SchemaGeneratorGeneralConfigPart withAnchorResolver(ConfigFunction<TypeScope, String> resolver) {
        this.anchorResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the "$anchor" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "$anchor" value for
     * @return "$anchor" in a JSON Schema (may be null)
     */
    public String resolveAnchor(TypeScope scope) {
        return SchemaGeneratorGeneralConfigPart.getFirstDefinedValue(this.anchorResolvers, scope);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withTitleResolver(ConfigFunction<TypeScope, String> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withTitleResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withDescriptionResolver(ConfigFunction<TypeScope, String> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withDescriptionResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withDefaultResolver(ConfigFunction<TypeScope, Object> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withDefaultResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withEnumResolver(ConfigFunction<TypeScope, Collection<?>> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withEnumResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withAdditionalPropertiesResolver(ConfigFunction<TypeScope, Type> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withAdditionalPropertiesResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withPatternPropertiesResolver(ConfigFunction<TypeScope, Map<String, Type>> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withPatternPropertiesResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withStringMinLengthResolver(ConfigFunction<TypeScope, Integer> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withStringMinLengthResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withStringMaxLengthResolver(ConfigFunction<TypeScope, Integer> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withStringMaxLengthResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withStringFormatResolver(ConfigFunction<TypeScope, String> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withStringFormatResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withStringPatternResolver(ConfigFunction<TypeScope, String> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withStringPatternResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withNumberInclusiveMinimumResolver(ConfigFunction<TypeScope, BigDecimal> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withNumberInclusiveMinimumResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withNumberExclusiveMinimumResolver(ConfigFunction<TypeScope, BigDecimal> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withNumberExclusiveMinimumResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withNumberInclusiveMaximumResolver(ConfigFunction<TypeScope, BigDecimal> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withNumberInclusiveMaximumResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withNumberExclusiveMaximumResolver(ConfigFunction<TypeScope, BigDecimal> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withNumberExclusiveMaximumResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withNumberMultipleOfResolver(ConfigFunction<TypeScope, BigDecimal> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withNumberMultipleOfResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withArrayMinItemsResolver(ConfigFunction<TypeScope, Integer> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withArrayMinItemsResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withArrayMaxItemsResolver(ConfigFunction<TypeScope, Integer> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withArrayMaxItemsResolver(resolver);
    }

    @Override
    public SchemaGeneratorGeneralConfigPart withArrayUniqueItemsResolver(ConfigFunction<TypeScope, Boolean> resolver) {
        return (SchemaGeneratorGeneralConfigPart) super.withArrayUniqueItemsResolver(resolver);
    }
}
