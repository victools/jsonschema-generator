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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProvider;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.TypePlaceholderResolver;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of a schema generator's configuration.
 */
public class SchemaGeneratorConfigImpl implements SchemaGeneratorConfig {

    private final ObjectMapper objectMapper;
    private final Map<Option, Boolean> options;
    private final SchemaGeneratorConfigPart<Field> fieldConfigPart;
    private final SchemaGeneratorConfigPart<Method> methodConfigPart;
    private final List<CustomDefinitionProvider> customDefinitions;

    /**
     * Constructor of a configuration instance.
     *
     * @param objectMapper supplier for object and array nodes for the JSON structure being generated
     * @param options specifically configured settings/options (thereby overriding the default enabled/disabled flag)
     * @param fieldConfigPart configuration part for fields
     * @param methodConfigPart configuration part for methods
     * @param customDefinitions custom suppliers for a type's schema definition
     */
    public SchemaGeneratorConfigImpl(ObjectMapper objectMapper,
            Map<Option, Boolean> options,
            SchemaGeneratorConfigPart<Field> fieldConfigPart,
            SchemaGeneratorConfigPart<Method> methodConfigPart,
            List<CustomDefinitionProvider> customDefinitions) {
        this.objectMapper = objectMapper;
        this.options = options;
        this.fieldConfigPart = fieldConfigPart;
        this.methodConfigPart = methodConfigPart;
        this.customDefinitions = customDefinitions;
    }

    /**
     * Whether a given option is currently enabled (either specifically or by default).
     *
     * @param setting generator option to check
     * @return whether the given generator option is enabled
     */
    private boolean isOptionEnabled(Option setting) {
        return this.options.getOrDefault(setting, setting.isEnabledByDefault());
    }

    @Override
    public boolean shouldCreateDefinitionsForAllObjects() {
        return this.isOptionEnabled(Option.DEFINITIONS_FOR_ALL_OBJECTS);
    }

    @Override
    public ObjectNode createObjectNode() {
        return this.objectMapper.createObjectNode();
    }

    @Override
    public ArrayNode createArrayNode() {
        return this.objectMapper.createArrayNode();
    }

    @Override
    public CustomDefinition getCustomDefinition(Type javaType, TypePlaceholderResolver placeholderResolver) {
        CustomDefinition result = this.customDefinitions.stream()
                .map(provider -> provider.provideCustomSchemaDefinition(javaType, placeholderResolver))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return result;
    }

    @Override
    public boolean isNullable(Field field) {
        return Optional.ofNullable(this.fieldConfigPart.isNullable(field))
                .orElseGet(() -> this.isOptionEnabled(Option.FIELDS_ARE_NULLABLE_BY_DEFAULT));
    }

    @Override
    public boolean isNullable(Method method) {
        return Optional.ofNullable(this.methodConfigPart.isNullable(method))
                .orElseGet(() -> this.isOptionEnabled(Option.METHODS_RETURN_NULLABLE_BY_DEFAULT));
    }

    @Override
    public boolean shouldIgnore(Field field) {
        return this.fieldConfigPart.shouldIgnore(field);
    }

    @Override
    public boolean shouldIgnore(Method method) {
        return this.methodConfigPart.shouldIgnore(method);
    }

    @Override
    public Type resolveTargetTypeOverride(Field field, Type defaultType) {
        return this.fieldConfigPart.resolveTargetTypeOverride(field, defaultType);
    }

    @Override
    public Type resolveTargetTypeOverride(Method method, Type defaultType) {
        return this.methodConfigPart.resolveTargetTypeOverride(method, defaultType);
    }

    @Override
    public String resolvePropertyNameOverride(Field field, String defaultName) {
        return this.fieldConfigPart.resolvePropertyNameOverride(field, defaultName);
    }

    @Override
    public String resolvePropertyNameOverride(Method method, String defaultName) {
        return this.methodConfigPart.resolvePropertyNameOverride(method, defaultName);
    }

    @Override
    public String resolveTitle(Field field) {
        return this.fieldConfigPart.resolveTitle(field);
    }

    @Override
    public String resolveTitle(Method method) {
        return this.methodConfigPart.resolveTitle(method);
    }

    @Override
    public String resolveDescription(Field field) {
        return this.fieldConfigPart.resolveDescription(field);
    }

    @Override
    public String resolveDescription(Method method) {
        return this.methodConfigPart.resolveDescription(method);
    }

    @Override
    public Collection<?> resolveEnum(Field field) {
        return this.fieldConfigPart.resolveEnum(field);
    }

    @Override
    public Collection<?> resolveEnum(Method method) {
        return this.methodConfigPart.resolveEnum(method);
    }

    @Override
    public Integer resolveStringMinLength(Field field) {
        return this.fieldConfigPart.resolveStringMinLength(field);
    }

    @Override
    public Integer resolveStringMinLength(Method method) {
        return this.methodConfigPart.resolveStringMinLength(method);
    }

    @Override
    public Integer resolveStringMaxLength(Field field) {
        return this.fieldConfigPart.resolveStringMaxLength(field);
    }

    @Override
    public Integer resolveStringMaxLength(Method method) {
        return this.methodConfigPart.resolveStringMaxLength(method);
    }

    @Override
    public String resolveStringFormat(Field field) {
        return this.fieldConfigPart.resolveStringFormat(field);
    }

    @Override
    public String resolveStringFormat(Method method) {
        return this.methodConfigPart.resolveStringFormat(method);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMinimum(Field field) {
        return this.fieldConfigPart.resolveNumberInclusiveMinimum(field);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMinimum(Method method) {
        return this.methodConfigPart.resolveNumberInclusiveMinimum(method);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMinimum(Field field) {
        return this.fieldConfigPart.resolveNumberExclusiveMinimum(field);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMinimum(Method method) {
        return this.methodConfigPart.resolveNumberExclusiveMinimum(method);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMaximum(Field field) {
        return this.fieldConfigPart.resolveNumberInclusiveMaximum(field);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMaximum(Method method) {
        return this.methodConfigPart.resolveNumberInclusiveMaximum(method);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMaximum(Field field) {
        return this.fieldConfigPart.resolveNumberExclusiveMaximum(field);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMaximum(Method method) {
        return this.methodConfigPart.resolveNumberExclusiveMaximum(method);
    }

    @Override
    public BigDecimal resolveNumberMultipleOf(Field field) {
        return this.fieldConfigPart.resolveNumberMultipleOf(field);
    }

    @Override
    public BigDecimal resolveNumberMultipleOf(Method method) {
        return this.methodConfigPart.resolveNumberMultipleOf(method);
    }

    @Override
    public Integer resolveArrayMinItems(Field field) {
        return this.fieldConfigPart.resolveArrayMinItems(field);
    }

    @Override
    public Integer resolveArrayMinItems(Method method) {
        return this.methodConfigPart.resolveArrayMinItems(method);
    }

    @Override
    public Integer resolveArrayMaxItems(Field field) {
        return this.fieldConfigPart.resolveArrayMaxItems(field);
    }

    @Override
    public Integer resolveArrayMaxItems(Method method) {
        return this.methodConfigPart.resolveArrayMaxItems(method);
    }

    @Override
    public Boolean resolveArrayUniqueItems(Field field) {
        return this.fieldConfigPart.resolveArrayUniqueItems(field);
    }

    @Override
    public Boolean resolveArrayUniqueItems(Method method) {
        return this.methodConfigPart.resolveArrayUniqueItems(method);
    }
}
