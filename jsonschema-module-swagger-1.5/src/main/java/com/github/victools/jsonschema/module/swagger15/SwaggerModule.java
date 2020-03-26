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

package com.github.victools.jsonschema.module.swagger15;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.TypeScope;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON Schema Generator Module – Swagger (1.5).
 */
public class SwaggerModule implements Module {

    private static final String OPENING_BRACKET = Pattern.quote("(") + '|' + Pattern.quote("[");
    private static final String NUMBER_OR_NEGATIVE_INFINITE = "-?[0-9]+\\.?[0-9]*|-infinity";
    private static final String NUMBER_OR_INFINITE = "-?[0-9]+\\.?[0-9]*|infinity";
    private static final String CLOSING_BRACKET = Pattern.quote(")") + '|' + Pattern.quote("]");
    private static final Pattern ALLOWABLE_VALUES_RANGE = Pattern.compile("range(" + OPENING_BRACKET
            + ")(" + NUMBER_OR_NEGATIVE_INFINITE + "), *(" + NUMBER_OR_INFINITE + ")(" + CLOSING_BRACKET + ")");

    private final List<SwaggerOption> options;

    /**
     * Constructor.
     *
     * @param options features to enable
     */
    public SwaggerModule(SwaggerOption... options) {
        this.options = options == null ? Collections.emptyList() : Arrays.asList(options);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<FieldScope> fieldConfigPart = builder.forFields();
        this.applyToConfigPart(fieldConfigPart);
        if (this.options.contains(SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES)) {
            fieldConfigPart
                    .withPropertyNameOverrideResolver(this::resolvePropertyNameOverride);
        }
        this.applyToConfigPart(builder.forMethods());

        if (!this.options.contains(SwaggerOption.NO_APIMODEL_TITLE)) {
            builder.forTypesInGeneral()
                    .withTitleResolver(this::resolveTitleForType);
        }
        if (!this.options.contains(SwaggerOption.NO_APIMODEL_DESCRIPTION)) {
            builder.forTypesInGeneral()
                    .withDescriptionResolver(this::resolveDescriptionForType);
        }
    }

    /**
     * Apply configurations that are part of this module to the given configuration part – expectation being that fields and methods get the same.
     *
     * @param configPart configuration instance to add configurations too
     */
    private void applyToConfigPart(SchemaGeneratorConfigPart<?> configPart) {
        if (this.options.contains(SwaggerOption.IGNORING_HIDDEN_PROPERTIES)) {
            configPart.withIgnoreCheck(this::shouldIgnore);
        }
        configPart.withDescriptionResolver(this::resolveDescription);
        configPart.withNumberExclusiveMinimumResolver(this::resolveNumberExclusiveMinimum);
        configPart.withNumberInclusiveMinimumResolver(this::resolveNumberInclusiveMinimum);
        configPart.withNumberExclusiveMaximumResolver(this::resolveNumberExclusiveMaximum);
        configPart.withNumberInclusiveMaximumResolver(this::resolveNumberInclusiveMaximum);
        configPart.withEnumResolver(this::resolveAllowedValues);
    }

    /**
     * Determine whether a given member should be ignored, i.e. excluded from the generated schema.
     *
     * @param member targeted field/method
     * @return whether to ignore the given field/method
     */
    protected boolean shouldIgnore(MemberScope<?, ?> member) {
        ApiModelProperty annotation = member.getAnnotationConsideringFieldAndGetter(ApiModelProperty.class);
        return annotation != null && annotation.hidden();
    }

    /**
     * Look-up name override for a given field or its associated getter method from the {@link ApiModelProperty} annotation's {@code name}.
     *
     * @param field targeted field
     * @return applicable name override (or {@code null})
     */
    protected String resolvePropertyNameOverride(FieldScope field) {
        return Optional.ofNullable(field.getAnnotationConsideringFieldAndGetter(ApiModelProperty.class))
                .map(ApiModelProperty::name)
                .filter(name -> !name.isEmpty() && !name.equals(field.getName()))
                .orElse(null);
    }

    /**
     * Look-up a "description" for the given member or its associated getter/field from the {@link ApiModelProperty} annotation's {@code value}.
     *
     * @param member targeted field/method
     * @return description (or {@code null})
     */
    protected String resolveDescription(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetter(ApiModelProperty.class))
                .map(ApiModelProperty::value)
                .filter(value -> !value.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up a "description" from the given type's {@link ApiModel} annotation's {@code description}.
     *
     * @param scope targeted type
     * @return description (or {@code null})
     */
    protected String resolveDescriptionForType(TypeScope scope) {
        return Optional.ofNullable(scope.getType())
                .map(type -> type.getErasedType().getAnnotation(ApiModel.class))
                .map(ApiModel::description)
                .filter(description -> !description.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up a "title" for the given member or its associated getter/field from the member type's {@link ApiModel} annotation's {@code value}.
     *
     * @param scope targeted type
     * @return title (or {@code null})
     */
    protected String resolveTitleForType(TypeScope scope) {
        return Optional.ofNullable(scope.getType())
                .map(type -> type.getErasedType().getAnnotation(ApiModel.class))
                .map(ApiModel::value)
                .filter(title -> !title.isEmpty())
                .orElse(null);
    }

    /**
     * Retrieve the given member's (or its associated getter/field's) {@link ApiModelProperty} annotation and extract its {@code allowableValues}.
     *
     * @param member targeted field/method
     * @return {@link ApiModelProperty} annotation's non-empty {@code allowableValues} (or {@code null})
     */
    private Optional<String> findModelPropertyAllowableValues(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetter(ApiModelProperty.class))
                .map(ApiModelProperty::allowableValues)
                .filter(allowableValues -> !allowableValues.isEmpty());
    }

    /**
     * Look-up a "const"/"enum" for the given member or its associated getter/field from the {@link ApiModelProperty} annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return list of allowed values (or {@code null})
     */
    protected List<String> resolveAllowedValues(MemberScope<?, ?> member) {
        return this.findModelPropertyAllowableValues(member)
                .filter(allowableValues -> !ALLOWABLE_VALUES_RANGE.matcher(allowableValues).matches())
                .map(allowableValues -> Arrays.asList(allowableValues.split(", *")))
                .orElse(null);
    }

    /**
     * Determine (inclusive) numeric minimum for the given member or its associated getter/field from the {@link ApiModelProperty} annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return inclusive numeric minimum (or {@code null})
     */
    protected BigDecimal resolveNumberInclusiveMinimum(MemberScope<?, ?> member) {
        return this.resolveNumberMinimum(member, "[");
    }

    /**
     * Determine (exclusive) numeric minimum for the given member or its associated getter/field from the {@link ApiModelProperty} annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return exclusive numeric minimum (or {@code null})
     */
    protected BigDecimal resolveNumberExclusiveMinimum(MemberScope<?, ?> member) {
        return this.resolveNumberMinimum(member, "(");
    }

    /**
     * Determine numeric minimum for the given member or its associated getter/field from the {@link ApiModelProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @param inclusiveIndicator the opening parenthesis/bracket indicating the desired kind of minimum (either inclusive or exclusive)
     * @return numeric minimum (or {@code null})
     */
    private BigDecimal resolveNumberMinimum(MemberScope<?, ?> member, String inclusiveIndicator) {
        String allowableValues = this.findModelPropertyAllowableValues(member).orElse(null);
        if (allowableValues != null) {
            Matcher matcher = ALLOWABLE_VALUES_RANGE.matcher(allowableValues);
            if (matcher.matches() && inclusiveIndicator.equals(matcher.group(1)) && !"-infinity".equals(matcher.group(2))) {
                return new BigDecimal(matcher.group(2));
            }
        }
        return null;
    }

    /**
     * Determine (inclusive) numeric maximum for the given member or its associated getter/field from the {@link ApiModelProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return inclusive numeric maximum (or {@code null})
     */
    protected BigDecimal resolveNumberInclusiveMaximum(MemberScope<?, ?> member) {
        return this.resolveNumberMaximum(member, "]");
    }

    /**
     * Determine (exclusive) numeric maximum for the given member or its associated getter/field from the {@link ApiModelProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return exclusive numeric maximum (or {@code null})
     */
    protected BigDecimal resolveNumberExclusiveMaximum(MemberScope<?, ?> member) {
        return this.resolveNumberMaximum(member, ")");
    }

    /**
     * Determine numeric maximum for the given member or its associated getter/field from the {@link ApiModelProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @param inclusiveIndicator the closing parenthesis/bracket indicating the desired kind of minimum (either inclusive or exclusive)
     * @return numeric maximum (or {@code null})
     */
    private BigDecimal resolveNumberMaximum(MemberScope<?, ?> member, String inclusiveIndicator) {
        String allowableValues = this.findModelPropertyAllowableValues(member).orElse(null);
        if (allowableValues != null) {
            Matcher matcher = ALLOWABLE_VALUES_RANGE.matcher(allowableValues);
            if (matcher.matches() && inclusiveIndicator.equals(matcher.group(4)) && !"infinity".equals(matcher.group(3))) {
                return new BigDecimal(matcher.group(3));
            }
        }
        return null;
    }
}
