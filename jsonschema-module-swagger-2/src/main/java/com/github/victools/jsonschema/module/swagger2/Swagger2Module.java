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

package com.github.victools.jsonschema.module.swagger2;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeScope;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * JSON Schema Generator Module – Swagger (2.x).
 *
 * @since 4.13.0
 */
public class Swagger2Module implements Module {

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        this.applyToConfigBuilder(builder.forTypesInGeneral());
        this.applyToConfigBuilder(builder.forFields());
        this.applyToConfigBuilder(builder.forMethods());
    }

    private void applyToConfigBuilder(SchemaGeneratorGeneralConfigPart configPart) {
        configPart.withDescriptionResolver(this.createTypePropertyResolver(Schema::description,
                description -> !description.isEmpty()));
        configPart.withTitleResolver(this.createTypePropertyResolver(Schema::title,
                description -> !description.isEmpty()));
    }

    private void applyToConfigBuilder(SchemaGeneratorConfigPart<?> configPart) {
        configPart.withTargetTypeOverridesResolver(this::resolveTargetTypeOverrides);
        configPart.withIgnoreCheck(this::shouldBeIgnored);
        configPart.withPropertyNameOverrideResolver(this::resolvePropertyNameOverride);

        configPart.withDescriptionResolver(this::resolveDescription);
        configPart.withTitleResolver(this::resolveTitle);
        configPart.withRequiredCheck(this::checkRequired);
        configPart.withNullableCheck(this::checkNullable);
        configPart.withEnumResolver(this::resolveEnum);
        configPart.withDefaultResolver(this::resolveDefault);

        configPart.withStringMinLengthResolver(this::resolveMinLength);
        configPart.withStringMaxLengthResolver(this::resolveMaxLength);
        configPart.withStringFormatResolver(this::resolveFormat);
        configPart.withStringPatternResolver(this::resolvePattern);

        configPart.withNumberMultipleOfResolver(this::resolveMultipleOf);
        configPart.withNumberExclusiveMaximumResolver(this::resolveExclusiveMaximum);
        configPart.withNumberInclusiveMaximumResolver(this::resolveInclusiveMaximum);
        configPart.withNumberExclusiveMinimumResolver(this::resolveExclusiveMinimum);
        configPart.withNumberInclusiveMinimumResolver(this::resolveInclusiveMinimum);

        configPart.withArrayMinItemsResolver(this::resolveArrayMinItems);
        configPart.withArrayMaxItemsResolver(this::resolveArrayMaxItems);
        configPart.withArrayUniqueItemsResolver(this::resolveArrayUniqueItems);

        configPart.withInstanceAttributeOverride(this::overrideInstanceAttributes);
    }

    protected List<ResolvedType> resolveTargetTypeOverrides(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::implementation, annotatedImplementation -> annotatedImplementation != Void.class)
                .map(annotatedType -> member.getContext().resolve(annotatedType))
                .map(Collections::singletonList)
                .orElse(null);
    }

    protected boolean shouldBeIgnored(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::hidden, Boolean.TRUE::equals)
                .isPresent();
    }

    protected String resolvePropertyNameOverride(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::name, name -> !name.isEmpty())
                .orElse(null);
    }

    protected String resolveDescription(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::description, description -> !description.isEmpty())
                .orElse(null);
    }

    protected String resolveTitle(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::title, title -> !title.isEmpty())
                .orElse(null);
    }

    protected boolean checkRequired(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::required, Boolean.TRUE::equals)
                .isPresent();
    }

    protected boolean checkNullable(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::nullable, Boolean.TRUE::equals)
                .isPresent();
    }

    protected List<String> resolveEnum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::allowableValues, values -> values.length > 0)
                .map(Arrays::asList)
                .orElse(null);
    }

    protected String resolveDefault(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::defaultValue, defaultValue -> !defaultValue.isEmpty())
                .orElse(null);
    }

    protected Integer resolveMinLength(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::minLength, minLength -> minLength > 0)
                .orElse(null);
    }

    protected Integer resolveMaxLength(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::maxLength, maxLength -> maxLength < Integer.MAX_VALUE && maxLength > -1)
                .orElse(null);
    }

    protected String resolveFormat(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::format, format -> !format.isEmpty())
                .orElse(null);
    }

    protected String resolvePattern(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::pattern, pattern -> !pattern.isEmpty())
                .orElse(null);
    }

    protected BigDecimal resolveMultipleOf(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::multipleOf, multipleOf -> multipleOf != 0)
                .map(BigDecimal::new)
                .orElse(null);
    }

    protected BigDecimal resolveExclusiveMaximum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::maximum, maximum -> !maximum.isEmpty())
                .filter(_maximum -> this.getSchemaAnnotationValue(member, Schema::exclusiveMaximum, Boolean.TRUE::equals).isPresent())
                .map(BigDecimal::new)
                .orElse(null);
    }

    protected BigDecimal resolveInclusiveMaximum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::maximum, maximum -> !maximum.isEmpty())
                .filter(_maximum -> this.getSchemaAnnotationValue(member, Schema::exclusiveMaximum, Boolean.FALSE::equals).isPresent())
                .map(BigDecimal::new)
                .orElse(null);
    }

    protected BigDecimal resolveExclusiveMinimum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::minimum, minimum -> !minimum.isEmpty())
                .filter(_minimum -> this.getSchemaAnnotationValue(member, Schema::exclusiveMinimum, Boolean.TRUE::equals).isPresent())
                .map(BigDecimal::new)
                .orElse(null);
    }

    protected BigDecimal resolveInclusiveMinimum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::minimum, minimum -> !minimum.isEmpty())
                .filter(_minimum -> this.getSchemaAnnotationValue(member, Schema::exclusiveMinimum, Boolean.FALSE::equals).isPresent())
                .map(BigDecimal::new)
                .orElse(null);
    }

    /**
     * Determine the given field/method's {@link ArraySchema} annotation is present and contains a specific {@code minItems}.
     *
     * @param member potentially annotated field/method
     * @return the {@code @ArraySchema(minItems)} value, otherwise {@code null}
     */
    protected Integer resolveArrayMinItems(MemberScope<?, ?> member) {
        if (member.isFakeContainerItemScope()) {
            return null;
        }
        return this.getArraySchemaAnnotation(member).map(ArraySchema::minItems)
                .filter(minItems -> minItems != Integer.MAX_VALUE).orElse(null);
    }

    /**
     * Determine the given field/method's {@link ArraySchema} annotation is present and contains a specific {@code maxItems}.
     *
     * @param member potentially annotated field/method
     * @return the {@code @ArraySchema(maxItems)} value, otherwise {@code null}
     */
    protected Integer resolveArrayMaxItems(MemberScope<?, ?> member) {
        if (member.isFakeContainerItemScope()) {
            return null;
        }
        return this.getArraySchemaAnnotation(member).map(ArraySchema::maxItems)
                .filter(maxItems -> maxItems != Integer.MIN_VALUE).orElse(null);
    }

    /**
     * Determine the given field/method's {@link ArraySchema} annotation is present and is marked as {@code uniqueItems = true}.
     *
     * @param member potentially annotated field/method
     * @return whether {@code @ArraySchema(uniqueItems = true)} is present
     */
    protected Boolean resolveArrayUniqueItems(MemberScope<?, ?> member) {
        if (member.isFakeContainerItemScope()) {
            return null;
        }
        return this.getArraySchemaAnnotation(member).map(ArraySchema::uniqueItems)
                .filter(uniqueItemsFlag -> uniqueItemsFlag).orElse(null);
    }

    protected void overrideInstanceAttributes(ObjectNode memberAttributes, MemberScope<?, ?> member, SchemaGenerationContext context) {
        Schema annotation = this.getSchemaAnnotationValue(member, Function.identity(), x -> true)
                .orElse(null);
        if (annotation == null) {
            return;
        }
        if (annotation.not() != Void.class) {
            memberAttributes.set("not", context.createDefinitionReference(context.getTypeContext().resolve(annotation.not())));
        }
        if (annotation.allOf().length > 0) {
            Stream.of(annotation.allOf())
                    .map(rawType -> context.getTypeContext().resolve(rawType))
                    .map(context::createDefinitionReference).forEach(memberAttributes
                    .withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF))::add);
        }
        if (annotation.minProperties() > 0) {
            memberAttributes.put("minProperties", annotation.minProperties());
        }
        if (annotation.maxProperties() > 0) {
            memberAttributes.put("maxProperties", annotation.maxProperties());
        }
        if (annotation.requiredProperties().length > 0) {
            Set<String> alreadyMentionedRequiredFields = new HashSet<>();
            ArrayNode requiredFieldNames = memberAttributes
                    .withArray(context.getKeyword(SchemaKeyword.TAG_REQUIRED));
            requiredFieldNames
                    .forEach(arrayItem -> alreadyMentionedRequiredFields.add(arrayItem.asText()));
            Stream.of(annotation.requiredProperties())
                    .filter(field -> !alreadyMentionedRequiredFields.contains(field))
                    .forEach(requiredFieldNames::add);
        }
    }

    /**
     * Look up a value from a {@link Schema} annotation on the given property or its associated field/getter or an external class referenced by
     * {@link Schema#implementation()}.
     *
     * @param member field/method for which to look-up any present {@link Schema} annotation
     * @param valueExtractor the getter for the value from the annotation
     * @param valueFilter filter that determines whether the value from a given annotation matches our criteria
     * @param <T> the type of the returned value
     *
     * @return the value from one of the matching {@link Schema} annotations or {@code Optional.empty()}
     */
    private <T> Optional<T> getSchemaAnnotationValue(MemberScope<?, ?> member, Function<Schema, T> valueExtractor, Predicate<T> valueFilter) {
        if (member.isFakeContainerItemScope()) {
            return this.getArraySchemaAnnotation(member)
                    .map(ArraySchema::schema)
                    .map(valueExtractor)
                    .filter(valueFilter);
        }
        Schema annotation = member.getAnnotationConsideringFieldAndGetter(Schema.class);
        if (annotation != null) {
            return Optional.of(annotation)
                    .map(valueExtractor)
                    .filter(valueFilter);
        }
        return this.getArraySchemaAnnotation(member)
                .map(ArraySchema::arraySchema)
                .map(valueExtractor)
                .filter(valueFilter);
    }

    /**
     * Create a {@link ConfigFunction} that extracts a value from the
     * {@link Schema} annotation of a {@link TypeScope}.
     * 
     * @param valueExtractor
     *            the getter for the value from the annotation
     * @param valueFilter
     *            filter that determines whether the value from a given
     *            annotation matches our criteria
     * @param <T>
     *            the type of the returned value
     * @return the value from the matching type's {@link Schema} annotation or
     *         {@code Optional.empty()}
     */
    private <T> ConfigFunction<TypeScope, T> createTypePropertyResolver(
            Function<Schema, T> valueExtractor, Predicate<T> valueFilter) {
        return typeScope -> Optional
                .ofNullable(typeScope.getType().getErasedType().getAnnotation(Schema.class))
                .map(valueExtractor).filter(valueFilter).orElse(null);
     }

    /**
     * Look-up the {@link ArraySchema} annotation on the given property or its associated field/getter.
     *
     * @param member field/method for which to look-up any present {@link ArraySchema} annotation
     * @return present {@link ArraySchema} annotation or {@code Optional.empty()}
     */
    private Optional<ArraySchema> getArraySchemaAnnotation(MemberScope<?, ?> member) {
        return Optional.ofNullable(member.getAnnotationConsideringFieldAndGetter(ArraySchema.class));
    }
}
