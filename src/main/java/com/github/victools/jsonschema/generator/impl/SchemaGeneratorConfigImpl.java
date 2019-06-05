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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final SchemaGeneratorConfigPart<ResolvedField> fieldConfigPart;
    private final SchemaGeneratorConfigPart<ResolvedMethod> methodConfigPart;
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
            SchemaGeneratorConfigPart<ResolvedField> fieldConfigPart,
            SchemaGeneratorConfigPart<ResolvedMethod> methodConfigPart,
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
    public boolean shouldIncludeStaticFields() {
        return this.isOptionEnabled(Option.PUBLIC_STATIC_FIELDS) || this.isOptionEnabled(Option.NONPUBLIC_STATIC_FIELDS);
    }

    @Override
    public boolean shouldIncludeStaticMethods() {
        return this.isOptionEnabled(Option.STATIC_METHODS);
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
    public CustomDefinition getCustomDefinition(ResolvedType javaType) {
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
    public List<InstanceAttributeOverride<ResolvedField>> getFieldAttributeOverrides() {
        return this.fieldConfigPart.getInstanceAttributeOverrides();
    }

    @Override
    public List<InstanceAttributeOverride<ResolvedMethod>> getMethodAttributeOverrides() {
        return this.methodConfigPart.getInstanceAttributeOverrides();
    }

    @Override
    public boolean isNullable(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return Optional.ofNullable(this.fieldConfigPart.isNullable(field, fieldType, declaringType))
                .orElseGet(() -> this.isOptionEnabled(Option.NULLABLE_FIELDS_BY_DEFAULT));
    }

    @Override
    public boolean isNullable(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return Optional.ofNullable(this.methodConfigPart.isNullable(method, returnValueType, declaringType))
                .orElseGet(() -> this.isOptionEnabled(Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT));
    }

    @Override
    public boolean shouldIgnore(ResolvedField field, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.shouldIgnore(field, declaringType);
    }

    @Override
    public boolean shouldIgnore(ResolvedMethod method, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.shouldIgnore(method, declaringType);
    }

    @Override
    public ResolvedType resolveTargetTypeOverride(ResolvedField field, ResolvedType defaultType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveTargetTypeOverride(field, defaultType, declaringType);
    }

    @Override
    public ResolvedType resolveTargetTypeOverride(ResolvedMethod method, ResolvedType defaultType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveTargetTypeOverride(method, defaultType, declaringType);
    }

    @Override
    public String resolvePropertyNameOverride(ResolvedField field, String defaultName, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolvePropertyNameOverride(field, defaultName, declaringType);
    }

    @Override
    public String resolvePropertyNameOverride(ResolvedMethod method, String defaultName, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolvePropertyNameOverride(method, defaultName, declaringType);
    }

    @Override
    public String resolveTitle(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveTitle(field, fieldType, declaringType);
    }

    @Override
    public String resolveTitle(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveTitle(method, returnValueType, declaringType);
    }

    @Override
    public String resolveDescription(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveDescription(field, fieldType, declaringType);
    }

    @Override
    public String resolveDescription(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveDescription(method, returnValueType, declaringType);
    }

    @Override
    public Collection<?> resolveEnum(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveEnum(field, fieldType, declaringType);
    }

    @Override
    public Collection<?> resolveEnum(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveEnum(method, returnValueType, declaringType);
    }

    @Override
    public Integer resolveStringMinLength(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveStringMinLength(field, fieldType, declaringType);
    }

    @Override
    public Integer resolveStringMinLength(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveStringMinLength(method, returnValueType, declaringType);
    }

    @Override
    public Integer resolveStringMaxLength(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveStringMaxLength(field, fieldType, declaringType);
    }

    @Override
    public Integer resolveStringMaxLength(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveStringMaxLength(method, returnValueType, declaringType);
    }

    @Override
    public String resolveStringFormat(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveStringFormat(field, fieldType, declaringType);
    }

    @Override
    public String resolveStringFormat(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveStringFormat(method, returnValueType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMinimum(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveNumberInclusiveMinimum(field, fieldType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMinimum(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveNumberInclusiveMinimum(method, returnValueType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMinimum(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveNumberExclusiveMinimum(field, fieldType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMinimum(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveNumberExclusiveMinimum(method, returnValueType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMaximum(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveNumberInclusiveMaximum(field, fieldType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberInclusiveMaximum(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveNumberInclusiveMaximum(method, returnValueType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMaximum(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveNumberExclusiveMaximum(field, fieldType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberExclusiveMaximum(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveNumberExclusiveMaximum(method, returnValueType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberMultipleOf(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveNumberMultipleOf(field, fieldType, declaringType);
    }

    @Override
    public BigDecimal resolveNumberMultipleOf(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveNumberMultipleOf(method, returnValueType, declaringType);
    }

    @Override
    public Integer resolveArrayMinItems(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveArrayMinItems(field, fieldType, declaringType);
    }

    @Override
    public Integer resolveArrayMinItems(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveArrayMinItems(method, returnValueType, declaringType);
    }

    @Override
    public Integer resolveArrayMaxItems(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveArrayMaxItems(field, fieldType, declaringType);
    }

    @Override
    public Integer resolveArrayMaxItems(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveArrayMaxItems(method, returnValueType, declaringType);
    }

    @Override
    public Boolean resolveArrayUniqueItems(ResolvedField field, ResolvedType fieldType, ResolvedTypeWithMembers declaringType) {
        return this.fieldConfigPart.resolveArrayUniqueItems(field, fieldType, declaringType);
    }

    @Override
    public Boolean resolveArrayUniqueItems(ResolvedMethod method, ResolvedType returnValueType, ResolvedTypeWithMembers declaringType) {
        return this.methodConfigPart.resolveArrayUniqueItems(method, returnValueType, declaringType);
    }
}
