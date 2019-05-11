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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.impl.SchemaGeneratorConfigImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Builder class for creating a configuration object to be passed into the SchemaGenerator's constructor.
 */
public class SchemaGeneratorConfigBuilder {

    private final ObjectMapper objectMapper;
    private final Map<Option, Boolean> options = new HashMap<>();
    private final SchemaGeneratorConfigPart<Field> fieldConfigPart = new SchemaGeneratorConfigPart<>();
    private final SchemaGeneratorConfigPart<Method> methodConfigPart = new SchemaGeneratorConfigPart<>();
    private final List<CustomDefinitionProvider> customDefinitions = new ArrayList<>();

    /**
     * Constructor of an empty configuration builder.
     *
     * @param objectMapper supplier for object and array nodes for the JSON structure being generated
     */
    public SchemaGeneratorConfigBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Create a schema generator instance from the builder.
     *
     * @return successfully created/initialised generator instance
     */
    public SchemaGeneratorConfig build() {
        // apply the configurations associated with enabled settings/options
        Stream.of(Option.values())
                .filter(setting -> this.options.getOrDefault(setting, setting.isEnabledByDefault()))
                .map(Option::getModule)
                .filter(Objects::nonNull)
                .forEach(this::with);
        // construct the actual configuration instance
        return new SchemaGeneratorConfigImpl(this.objectMapper,
                this.options,
                this.fieldConfigPart,
                this.methodConfigPart,
                this.customDefinitions);
    }

    /**
     * Get the part of this configuration builder dedicated to custom attribute look-ups for fields.
     *
     * @return configuration part responsible for handling of fields
     */
    public SchemaGeneratorConfigPart<Field> forFields() {
        return this.fieldConfigPart;
    }

    /**
     * Get the part of this configuration builder dedicated to custom attribute look-ups for methods.
     *
     * @return configuration part responsible for handling of methods
     */
    public SchemaGeneratorConfigPart<Method> forMethods() {
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
     */
    public SchemaGeneratorConfigBuilder with(CustomDefinitionProvider definitionProvider) {
        this.customDefinitions.add(definitionProvider);
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
