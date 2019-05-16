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
import com.github.victools.jsonschema.generator.InstanceAttributeOverride;
import com.github.victools.jsonschema.generator.JavaType;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.TypeAttributeOverride;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
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
    private final List<TypeAttributeOverride> typeAttributeOverrides;

    /**
     * Constructor of a configuration instance.
     *
     * @param objectMapper supplier for object and array nodes for the JSON structure being generated
     * @param options specifically configured settings/options (thereby overriding the default enabled/disabled flag)
     * @param fieldConfigPart configuration part for fields
     * @param methodConfigPart configuration part for methods
     * @param customDefinitions custom suppliers for a type's schema definition
     * @param typeAttributeOverrides applicable type attribute overrides
     */
    public SchemaGeneratorConfigImpl(ObjectMapper objectMapper,
            Map<Option, Boolean> options,
            SchemaGeneratorConfigPart<Field> fieldConfigPart,
            SchemaGeneratorConfigPart<Method> methodConfigPart,
            List<CustomDefinitionProvider> customDefinitions,
            List<TypeAttributeOverride> typeAttributeOverrides) {
        this.objectMapper = objectMapper;
        this.options = options;
        this.fieldConfigPart = fieldConfigPart;
        this.methodConfigPart = methodConfigPart;
        this.customDefinitions = customDefinitions;
        this.typeAttributeOverrides = typeAttributeOverrides;
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
    public boolean shouldIncludeSchemaVersionIndicator() {
        return this.isOptionEnabled(Option.SCHEMA_VERSION_INDICATOR);
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    @Override
    public ObjectNode createObjectNode() {
        return this.getObjectMapper().createObjectNode();
    }

    @Override
    public ArrayNode createArrayNode() {
        return this.getObjectMapper().createArrayNode();
    }

    @Override
    public CustomDefinition getCustomDefinition(JavaType javaType) {
        CustomDefinition result = this.customDefinitions.stream()
                .map(provider -> provider.provideCustomSchemaDefinition(javaType))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return result;
    }

    @Override
    public List<TypeAttributeOverride> getTypeAttributeOverrides() {
        return Collections.unmodifiableList(this.typeAttributeOverrides);
    }

    @Override
    public List<InstanceAttributeOverride<Field>> getFieldAttributeOverrides() {
        return this.fieldConfigPart.getInstanceAttributeOverrides();
    }

    @Override
    public List<InstanceAttributeOverride<Method>> getMethodAttributeOverrides() {
        return this.methodConfigPart.getInstanceAttributeOverrides();
    }

    @Override
    public boolean isNullable(Field field, JavaType fieldType) {
        return Optional.ofNullable(this.fieldConfigPart.isNullable(field, fieldType))
                .orElseGet(() -> this.isOptionEnabled(Option.NULLABLE_FIELDS_BY_DEFAULT));
    }

    @Override
    public boolean isNullable(Method method, JavaType returnValueType) {
        return Optional.ofNullable(this.methodConfigPart.isNullable(method, returnValueType))
                .orElseGet(() -> this.isOptionEnabled(Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT));
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
    public JavaType resolveTargetTypeOverride(Field field, JavaType defaultType) {
        return this.fieldConfigPart.resolveTargetTypeOverride(field, defaultType);
    }

    @Override
    public JavaType resolveTargetTypeOverride(Method method, JavaType defaultType) {
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
    public String resolveTitle(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveTitle(field, fieldType);
    }

    @Override
    public String resolveTitle(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveTitle(method, returnValueType);
    }

    @Override
    public String resolveDescription(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveDescription(field, fieldType);
    }

    @Override
    public String resolveDescription(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveDescription(method, returnValueType);
    }

    @Override
    public Collection<?> resolveEnum(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveEnum(field, fieldType);
    }

    @Override
    public Collection<?> resolveEnum(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveEnum(method, returnValueType);
    }

    @Override
    public Integer resolveStringMinLength(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveStringMinLength(field, fieldType);
    }

    @Override
    public Integer resolveStringMinLength(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveStringMinLength(method, returnValueType);
    }

    @Override
    public Integer resolveStringMaxLength(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveStringMaxLength(field, fieldType);
    }

    @Override
    public Integer resolveStringMaxLength(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveStringMaxLength(method, returnValueType);
    }

    @Override
    public String resolveStringFormat(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveStringFormat(field, fieldType);
    }

    @Override
    public String resolveStringFormat(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveStringFormat(method, returnValueType);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMinimum(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveNumberInclusiveMinimum(field, fieldType);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMinimum(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveNumberInclusiveMinimum(method, returnValueType);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMinimum(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveNumberExclusiveMinimum(field, fieldType);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMinimum(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveNumberExclusiveMinimum(method, returnValueType);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMaximum(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveNumberInclusiveMaximum(field, fieldType);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMaximum(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveNumberInclusiveMaximum(method, returnValueType);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMaximum(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveNumberExclusiveMaximum(field, fieldType);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMaximum(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveNumberExclusiveMaximum(method, returnValueType);
    }

    @Override
    public BigDecimal resolveNumberMultipleOf(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveNumberMultipleOf(field, fieldType);
    }

    @Override
    public BigDecimal resolveNumberMultipleOf(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveNumberMultipleOf(method, returnValueType);
    }

    @Override
    public Integer resolveArrayMinItems(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveArrayMinItems(field, fieldType);
    }

    @Override
    public Integer resolveArrayMinItems(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveArrayMinItems(method, returnValueType);
    }

    @Override
    public Integer resolveArrayMaxItems(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveArrayMaxItems(field, fieldType);
    }

    @Override
    public Integer resolveArrayMaxItems(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveArrayMaxItems(method, returnValueType);
    }

    @Override
    public Boolean resolveArrayUniqueItems(Field field, JavaType fieldType) {
        return this.fieldConfigPart.resolveArrayUniqueItems(field, fieldType);
    }

    @Override
    public Boolean resolveArrayUniqueItems(Method method, JavaType returnValueType) {
        return this.methodConfigPart.resolveArrayUniqueItems(method, returnValueType);
    }
}
