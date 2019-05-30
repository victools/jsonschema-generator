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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProvider;
import com.github.victools.jsonschema.generator.InstanceAttributeOverride;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.TypeAttributeOverride;
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
    private final SchemaGeneratorConfigPart<AnnotatedField> fieldConfigPart;
    private final SchemaGeneratorConfigPart<AnnotatedMethod> methodConfigPart;
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
            SchemaGeneratorConfigPart<AnnotatedField> fieldConfigPart,
            SchemaGeneratorConfigPart<AnnotatedMethod> methodConfigPart,
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
        return this.options.getOrDefault(setting, false);
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
    public List<InstanceAttributeOverride<AnnotatedField>> getFieldAttributeOverrides() {
        return this.fieldConfigPart.getInstanceAttributeOverrides();
    }

    @Override
    public List<InstanceAttributeOverride<AnnotatedMethod>> getMethodAttributeOverrides() {
        return this.methodConfigPart.getInstanceAttributeOverrides();
    }

    @Override
    public boolean isNullable(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return Optional.ofNullable(this.fieldConfigPart.isNullable(field, fieldType, declaringContext))
                .orElseGet(() -> this.isOptionEnabled(Option.NULLABLE_FIELDS_BY_DEFAULT));
    }

    @Override
    public boolean isNullable(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return Optional.ofNullable(this.methodConfigPart.isNullable(method, returnValueType, declaringContext))
                .orElseGet(() -> this.isOptionEnabled(Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT));
    }

    @Override
    public boolean shouldIgnore(AnnotatedField field, BeanDescription declaringContext) {
        return this.fieldConfigPart.shouldIgnore(field, declaringContext);
    }

    @Override
    public boolean shouldIgnore(AnnotatedMethod method, BeanDescription declaringContext) {
        return this.methodConfigPart.shouldIgnore(method, declaringContext);
    }

    @Override
    public JavaType resolveTargetTypeOverride(AnnotatedField field, JavaType defaultType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveTargetTypeOverride(field, defaultType, declaringContext);
    }

    @Override
    public JavaType resolveTargetTypeOverride(AnnotatedMethod method, JavaType defaultType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveTargetTypeOverride(method, defaultType, declaringContext);
    }

    @Override
    public String resolvePropertyNameOverride(AnnotatedField field, String defaultName, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolvePropertyNameOverride(field, defaultName, declaringContext);
    }

    @Override
    public String resolvePropertyNameOverride(AnnotatedMethod method, String defaultName, BeanDescription declaringContext) {
        return this.methodConfigPart.resolvePropertyNameOverride(method, defaultName, declaringContext);
    }

    @Override
    public String resolveTitle(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveTitle(field, fieldType, declaringContext);
    }

    @Override
    public String resolveTitle(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveTitle(method, returnValueType, declaringContext);
    }

    @Override
    public String resolveDescription(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveDescription(field, fieldType, declaringContext);
    }

    @Override
    public String resolveDescription(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveDescription(method, returnValueType, declaringContext);
    }

    @Override
    public Collection<?> resolveEnum(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveEnum(field, fieldType, declaringContext);
    }

    @Override
    public Collection<?> resolveEnum(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveEnum(method, returnValueType, declaringContext);
    }

    @Override
    public Integer resolveStringMinLength(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveStringMinLength(field, fieldType, declaringContext);
    }

    @Override
    public Integer resolveStringMinLength(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveStringMinLength(method, returnValueType, declaringContext);
    }

    @Override
    public Integer resolveStringMaxLength(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveStringMaxLength(field, fieldType, declaringContext);
    }

    @Override
    public Integer resolveStringMaxLength(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveStringMaxLength(method, returnValueType, declaringContext);
    }

    @Override
    public String resolveStringFormat(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveStringFormat(field, fieldType, declaringContext);
    }

    @Override
    public String resolveStringFormat(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveStringFormat(method, returnValueType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMinimum(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveNumberInclusiveMinimum(field, fieldType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMinimum(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveNumberInclusiveMinimum(method, returnValueType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMinimum(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveNumberExclusiveMinimum(field, fieldType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMinimum(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveNumberExclusiveMinimum(method, returnValueType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMaximum(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveNumberInclusiveMaximum(field, fieldType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMaximum(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveNumberInclusiveMaximum(method, returnValueType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMaximum(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveNumberExclusiveMaximum(field, fieldType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMaximum(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveNumberExclusiveMaximum(method, returnValueType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberMultipleOf(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveNumberMultipleOf(field, fieldType, declaringContext);
    }

    @Override
    public BigDecimal resolveNumberMultipleOf(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveNumberMultipleOf(method, returnValueType, declaringContext);
    }

    @Override
    public Integer resolveArrayMinItems(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveArrayMinItems(field, fieldType, declaringContext);
    }

    @Override
    public Integer resolveArrayMinItems(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveArrayMinItems(method, returnValueType, declaringContext);
    }

    @Override
    public Integer resolveArrayMaxItems(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveArrayMaxItems(field, fieldType, declaringContext);
    }

    @Override
    public Integer resolveArrayMaxItems(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveArrayMaxItems(method, returnValueType, declaringContext);
    }

    @Override
    public Boolean resolveArrayUniqueItems(AnnotatedField field, JavaType fieldType, BeanDescription declaringContext) {
        return this.fieldConfigPart.resolveArrayUniqueItems(field, fieldType, declaringContext);
    }

    @Override
    public Boolean resolveArrayUniqueItems(AnnotatedMethod method, JavaType returnValueType, BeanDescription declaringContext) {
        return this.methodConfigPart.resolveArrayUniqueItems(method, returnValueType, declaringContext);
    }
}
