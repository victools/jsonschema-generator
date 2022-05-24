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

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.victools.jsonschema.generator.impl.SchemaGeneratorConfigImpl;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builder class for creating a configuration object to be passed into the SchemaGenerator's constructor.
 */
public class SchemaGeneratorConfigBuilder {

    /**
     * Instantiate an ObjectMapper to be used in case no specific instance is provided in constructor.
     *
     * @return default ObjectMapper instance
     */
    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig()
                // since version 4.21.0
                .with(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS);
        // since version 4.25.0; as the above doesn't always work
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));

        return mapper;
    }

    private final ObjectMapper objectMapper;
    private final OptionPreset preset;
    private final SchemaVersion schemaVersion;

    private final Map<Option, Boolean> options = new HashMap<>();
    private final SchemaGeneratorGeneralConfigPart typesInGeneralConfigPart = new SchemaGeneratorGeneralConfigPart();
    private final SchemaGeneratorConfigPart<FieldScope> fieldConfigPart = new SchemaGeneratorConfigPart<>();
    private final SchemaGeneratorConfigPart<MethodScope> methodConfigPart = new SchemaGeneratorConfigPart<>();

    /**
     * Constructor of an empty configuration builder for a {@link SchemaVersion#DRAFT_7 Draft 7} schema. This is equivalent to calling:<br>
     * {@code new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_7, OptionPreset.FULL_DOCUMENTATION)}
     *
     * @param objectMapper supplier for object and array nodes for the JSON structure being generated
     * @see #SchemaGeneratorConfigBuilder(ObjectMapper, SchemaVersion, OptionPreset)
     * @deprecated use {@link #SchemaGeneratorConfigBuilder(ObjectMapper, SchemaVersion)} instead
     */
    @Deprecated
    public SchemaGeneratorConfigBuilder(ObjectMapper objectMapper) {
        this(objectMapper, SchemaVersion.DRAFT_7, OptionPreset.FULL_DOCUMENTATION);
    }

    /**
     * Constructor of an empty configuration builder. This is equivalent to calling:<br>
     * {@code new SchemaGeneratorConfigBuilder(objectMapper, schemaVersion, OptionPreset.FULL_DOCUMENTATION)}
     *
     * @param objectMapper supplier for object and array nodes for the JSON structure being generated
     * @param schemaVersion designated JSON Schema version
     * @see #SchemaGeneratorConfigBuilder(ObjectMapper, SchemaVersion, OptionPreset)
     */
    public SchemaGeneratorConfigBuilder(ObjectMapper objectMapper, SchemaVersion schemaVersion) {
        this(objectMapper, schemaVersion, OptionPreset.FULL_DOCUMENTATION);
    }

    /**
     * Constructor of an empty configuration builder with a default {@link ObjectMapper} instance. This is equivalent to calling:<br>
     * {@code new SchemaGeneratorConfigBuilder(schemaVersion, OptionPreset.FULL_DOCUMENTATION)}
     *
     * @param schemaVersion designated JSON Schema version
     * @see #SchemaGeneratorConfigBuilder(ObjectMapper, SchemaVersion, OptionPreset)
     */
    public SchemaGeneratorConfigBuilder(SchemaVersion schemaVersion) {
        this(createDefaultObjectMapper(), schemaVersion, OptionPreset.FULL_DOCUMENTATION);
    }

    /**
     * Constructor of an empty configuration builder for a {@link SchemaVersion#DRAFT_7 Draft 7} schema. This is equivalent to calling:<br>
     * {@code new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_7, preset)}
     *
     * @param objectMapper supplier for object and array nodes for the JSON structure being generated
     * @param preset default settings for standard {@link Option} values
     * @deprecated use {@link #SchemaGeneratorConfigBuilder(ObjectMapper, SchemaVersion, OptionPreset)} instead
     */
    @Deprecated
    public SchemaGeneratorConfigBuilder(ObjectMapper objectMapper, OptionPreset preset) {
        this(objectMapper, SchemaVersion.DRAFT_7, preset);
    }

    /**
     * Constructor of an empty configuration builder with a default {@link ObjectMapper} instance.
     *
     * @param schemaVersion designated JSON Schema version
     * @param preset default settings for standard {@link Option} values
     */
    public SchemaGeneratorConfigBuilder(SchemaVersion schemaVersion, OptionPreset preset) {
        this(createDefaultObjectMapper(), schemaVersion, preset);
    }

    /**
     * Constructor of an empty configuration builder.
     *
     * @param objectMapper supplier for object and array nodes for the JSON structure being generated
     * @param schemaVersion designated JSON Schema version
     * @param preset default settings for standard {@link Option} values
     */
    public SchemaGeneratorConfigBuilder(ObjectMapper objectMapper, SchemaVersion schemaVersion, OptionPreset preset) {
        this.objectMapper = objectMapper;
        this.schemaVersion = schemaVersion;
        this.preset = preset;
    }

    /**
     * Create a schema generator instance from the builder.
     *
     * @return successfully created/initialised generator instance
     */
    public SchemaGeneratorConfig build() {
        // apply the configurations associated with enabled/disabled options
        EnumSet<Option> allOptions = EnumSet.allOf(Option.class);
        Set<Option> enabledOptions = EnumSet.allOf(Option.class).stream()
                .filter(option -> this.options.getOrDefault(option, this.preset.isOptionEnabledByDefault(option)))
                .collect(Collectors.toSet());
        Map<Option, Boolean> validOptions = allOptions.stream()
                .filter((configuredOption) -> enabledOptions.stream().noneMatch(enabledOne -> enabledOne.isOverriding(configuredOption)))
                .collect(Collectors.toMap(option -> option, enabledOptions::contains, (first, second) -> first, LinkedHashMap::new));

        validOptions.entrySet().stream()
                .map(setting -> setting.getKey().getModule(setting.getValue()))
                .filter(Objects::nonNull)
                .forEach(this::with);
        // discard invalid enabled options
        enabledOptions.retainAll(validOptions.keySet());
        // construct the actual configuration instance
        return new SchemaGeneratorConfigImpl(this.objectMapper,
                this.schemaVersion,
                enabledOptions,
                this.typesInGeneralConfigPart,
                this.fieldConfigPart,
                this.methodConfigPart);
    }

    /**
     * Get the part of this configuration builder dedicated to custom attribute look-ups for types in general, independent of the declaration context.
     *
     * @return configuration part responsible for handling types regardless of their declaration context
     */
    public SchemaGeneratorGeneralConfigPart forTypesInGeneral() {
        return this.typesInGeneralConfigPart;
    }

    /**
     * Get the part of this configuration builder dedicated to custom attribute look-ups for fields.
     *
     * @return configuration part responsible for handling of fields
     * @see #forTypesInGeneral() : holding configurations also applying to methods or types that are not declared as member directly
     * @see #forMethods() : holding configuration applying to methods
     */
    public SchemaGeneratorConfigPart<FieldScope> forFields() {
        return this.fieldConfigPart;
    }

    /**
     * Get the part of this configuration builder dedicated to custom attribute look-ups for methods.
     *
     * @return configuration part responsible for handling of methods
     * @see #forTypesInGeneral() : holding configurations also applying to fields or types that are not declared as member directly
     * @see #forFields() : holding configuration applying to fields
     */
    public SchemaGeneratorConfigPart<MethodScope> forMethods() {
        return this.methodConfigPart;
    }

    /**
     * Retrieve the associated object mapper instance.
     *
     * @return supplier for object and array nodes for the JSON structure being generated
     */
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    /**
     * Check whether the given setting/option has been set and if yes, whether it is enabled or disabled.
     *
     * @param setting generator option to check for
     * @return currently configured flag (i.e. true/false if already set), or null if not configured
     */
    public Boolean getSetting(Option setting) {
        return this.options.get(setting);
    }

    /**
     * Applying a module to this configuration builder instance.
     *
     * @param module configuration module to add/apply
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorConfigBuilder with(Module module) {
        module.applyToConfigBuilder(this);
        return this;
    }

    /**
     * Adding a custom schema provider – if it returns null for a given type, the next definition provider will be applied.
     * <br>
     * If all custom schema providers return null (or there is none), then the standard behaviour applies.
     *
     * @param definitionProvider provider of a custom definition to register, which may return null
     * @return this builder instance (for chaining)
     * @deprecated use {@link SchemaGeneratorGeneralConfigPart#withCustomDefinitionProvider(CustomDefinitionProviderV2)} instead
     */
    @Deprecated
    public SchemaGeneratorConfigBuilder with(CustomDefinitionProviderV2 definitionProvider) {
        this.typesInGeneralConfigPart.withCustomDefinitionProvider(definitionProvider);
        return this;
    }

    /**
     * Adding an override for type attributes – all of the registered overrides will be applied in the order of having been added.
     *
     * @param override adding/removing attributes on a JSON Schema node – specifically intended for attributes relating to the type in general.
     * @return this builder instance (for chaining)
     * @deprecated use {@link SchemaGeneratorGeneralConfigPart#withTypeAttributeOverride(TypeAttributeOverrideV2)} instead
     */
    @Deprecated
    public SchemaGeneratorConfigBuilder with(TypeAttributeOverride override) {
        this.typesInGeneralConfigPart.withTypeAttributeOverride(override);
        return this;
    }

    /**
     * Enable an option for the schema generation.
     *
     * @param setting generator option to enable
     * @param moreSettings additional generator options to enable
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorConfigBuilder with(Option setting, Option... moreSettings) {
        return this.setOptionEnabled(setting, moreSettings, true);
    }

    /**
     * Disable an option for the schema generation.
     *
     * @param setting generator option to disable
     * @param moreSettings additional generator options to disable
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorConfigBuilder without(Option setting, Option... moreSettings) {
        return this.setOptionEnabled(setting, moreSettings, false);
    }

    /**
     * Enabled/disable an option for the schema generation.
     *
     * @param setting generator option to enabled/disable
     * @param moreSettings additional generator options to enable/disable
     * @param enabled whether to enable or disable the given generator options
     * @return this builder instance (for chaining)
     */
    private SchemaGeneratorConfigBuilder setOptionEnabled(Option setting, Option[] moreSettings, boolean enabled) {
        this.options.put(setting, enabled);
        if (moreSettings != null) {
            for (Option additionalSetting : moreSettings) {
                this.options.put(additionalSetting, enabled);
            }
        }
        return this;
    }
}
