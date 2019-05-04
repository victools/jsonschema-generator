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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Builder class for creating a configuration object to be passed into the SchemaGenerator's constructor.
 */
public class SchemaGeneratorConfigBuilder {

    private final ObjectMapper objectMapper;
    private final Map<Option, Boolean> options = new HashMap<>();
    private final SchemaGeneratorConfigPart<Field> fieldConfigPart = new SchemaGeneratorConfigPart<>();
    private final SchemaGeneratorConfigPart<Method> methodConfigPart = new SchemaGeneratorConfigPart<>();
    private final Map<Class<?>, List<Function<Type, CustomDefinition>>> customDefinitions = new HashMap<>();

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
                .forEach(this::withModule);
        // construct the actual configuration instance
        return new SchemaGeneratorConfigImpl(this.objectMapper,
                this.options,
                this.fieldConfigPart,
                this.methodConfigPart,
                this.customDefinitions);
    }

    /**
     * Applying a module to this configuration builder instance.
     *
     * @param module configuration module to add/apply
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorConfigBuilder withModule(Module module) {
        module.applyToConfigBuilder(this);
        return this;
    }

    /**
     * Enable an option for the schema generation.
     *
     * @param setting generator option to enable
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorConfigBuilder withEnabled(Option setting) {
        this.options.put(setting, true);
        return this;
    }

    /**
     * Disable an option for the schema generation.
     *
     * @param setting generator option to disable
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorConfigBuilder withDisabled(Option setting) {
        this.options.put(setting, false);
        return this;
    }

    /**
     * Map certain java types to a schema containing only a "type" property of the specified value.
     *
     * @param jsonSchemaTypeValue textual representation of the resulting JSON schema's "type"
     * @param javaTypes java types to map to such a simple schema representation
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorConfigBuilder withFixedTypeMapping(String jsonSchemaTypeValue, Class<?>... javaTypes) {
        Function<Type, CustomDefinition> customDefinition = _genericType -> new CustomDefinition(
                this.objectMapper.createObjectNode().put(SchemaConstants.TAG_TYPE, jsonSchemaTypeValue), true);
        for (Class<?> javaType : javaTypes) {
            this.withCustomDefinition(javaType, customDefinition);
        }
        return this;
    }

    /**
     * Adding an optional custom schema definition for a certain (raw) java type. Falls-back to the next custom schema definition for the same tyoe if
     * the given one returns null. If there is no further custom schema provider, then the standard behaviour is applied.
     *
     * @param javaType (raw) java type for which to register a custom definition provider
     * @param definitionProvider provider of a custom definition to register, which may return null
     * @return this builder instance (for chaining)
     */
    public SchemaGeneratorConfigBuilder withCustomDefinition(Class<?> javaType, Function<Type, CustomDefinition> definitionProvider) {
        List<Function<Type, CustomDefinition>> valueList = this.customDefinitions.get(javaType);
        if (valueList == null) {
            valueList = new ArrayList<>();
            this.customDefinitions.put(javaType, valueList);
        }
        valueList.add(definitionProvider);
        return this;
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
}
