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
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
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
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * JSON Schema Generator Module - Swagger (2.x).
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
        configPart.withDescriptionResolver(this.createTypePropertyResolver(Schema::description, description -> !description.isEmpty()));
        configPart.withTitleResolver(this.createTypePropertyResolver(Schema::title, title -> !title.isEmpty()));
        configPart.withAdditionalPropertiesResolver(this.createTypePropertyResolver(this::mapAdditionalPropertiesEnumValue, Objects::nonNull));

        configPart.withCustomDefinitionProvider(new ExternalRefCustomDefinitionProvider());
        configPart.withSubtypeResolver(new Swagger2SubtypeResolver());
        configPart.withDefinitionNamingStrategy(new Swagger2SchemaDefinitionNamingStrategy(configPart.getDefinitionNamingStrategy()));
    }

    private void applyToConfigBuilder(SchemaGeneratorConfigPart<?> configPart) {
        configPart.withTargetTypeOverridesResolver(this::resolveTargetTypeOverrides);
        configPart.withIgnoreCheck(this::shouldBeIgnored);
        configPart.withPropertyNameOverrideResolver(this::resolvePropertyNameOverride);
        configPart.withCustomDefinitionProvider(this::provideCustomSchemaDefinition);

        configPart.withDescriptionResolver(this::resolveDescription);
        configPart.withTitleResolver(this::resolveTitle);
        configPart.withRequiredCheck(this::checkRequired);
        configPart.withNullableCheck(this::checkNullable);
        configPart.withReadOnlyCheck(this::checkReadOnly);
        configPart.withWriteOnlyCheck(this::checkWriteOnly);
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

        // take care of various keywords that are not so straightforward to apply
        configPart.withInstanceAttributeOverride(this::overrideInstanceAttributes);
    }

    /**
     * Derive target type override from {@code @Schema(implementation = ...)}.
     *
     * @param member field/method to determine target type override for
     * @return single target type override or null
     */
    protected List<ResolvedType> resolveTargetTypeOverrides(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::implementation, annotatedImplementation -> annotatedImplementation != Void.class)
                .map(annotatedType -> member.getContext().resolve(annotatedType))
                .map(Collections::singletonList)
                .orElse(null);
    }

    /**
     * Determine whether the given field/method should be skipped, based on {@code @Schema(hidden = true)}.
     *
     * @param member field/method to check
     * @return whether to skip the field/method
     */
    protected boolean shouldBeIgnored(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::hidden, Boolean.TRUE::equals)
                .isPresent();
    }

    /**
     * Determine an alternative name for the given field/method, based on {@code @Schema(name = ...)}.
     *
     * @param member field/method to look-up alternative property name for
     * @return alternative property name
     */
    protected String resolvePropertyNameOverride(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::name, name -> !name.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up description from {@code @Schema(description = ...)} for given field/method.
     *
     * @param member field/method to look-up description for
     * @return schema description
     */
    protected String resolveDescription(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::description, description -> !description.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up title from {@code @Schema(title = ...)} for given field/method.
     *
     * @param member field/method to look-up title for
     * @return schema title
     */
    protected String resolveTitle(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::title, title -> !title.isEmpty())
                .orElse(null);
    }

    /**
     * Determine whether the given field/method is deemed required in its containing type based on {@code @Schema(required = true)}.
     *
     * @param member field/method to check
     * @return whether the field/method is required
     */
    protected boolean checkRequired(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member,
                        schemaAnnotation -> schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED || schemaAnnotation.required(),
                        Boolean.TRUE::equals)
                .isPresent();
    }

    /**
     * Determine whether the given field/method may be null based on {@code @Schema(nullable = true)}.
     *
     * @param member field/method to check
     * @return whether the field/method is nullable
     */
    protected Boolean checkNullable(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Function.identity(), x -> true)
                .map(Schema::nullable)
                .orElse(null);
    }

    /**
     * Derive the allowed type of a schema's additional properties from the given annotation.
     *
     * @param annotation annotation to check
     * @return {@code Object.class} (if true or an external "$ref" is specified), {@code Void.class} (if forbidden) or {@code null} (if undefined)
     */
    protected Type mapAdditionalPropertiesEnumValue(Schema annotation) {
        if (!annotation.ref().isEmpty()) {
            // prevent invalid combination of "$ref" with "additionalProperties": false
            return Object.class;
        }
        switch (annotation.additionalProperties()) {
        case TRUE:
            // allow any additional properties
            return Object.class;
        case FALSE:
            // block any additional properties
            return Void.class;
        default:
            // fall-back on other configuration, e.g., as per Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT
            return null;
        }
    }

    /**
     * Determine whether the given field/method is deemed read-only based on {@code @Schema(accessMode = AccessMode.READ_ONLY)}.
     *
     * @param member field/method to check
     * @return whether the field/method is read-only
     */
    protected boolean checkReadOnly(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::accessMode, Schema.AccessMode.READ_ONLY::equals)
                .isPresent();
    }

    /**
     * Determine whether the given field/method is deemed write-only based on {@code @Schema(accessMode = AccessMode.WRITE_ONLY)}.
     *
     * @param member field/method to check
     * @return whether the field/method is write-only
     */
    protected boolean checkWriteOnly(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::accessMode, Schema.AccessMode.WRITE_ONLY::equals)
                .isPresent();
    }

    /**
     * Look-up the finite list of possible values from {@code @Schema(allowableValues = ...)}.
     *
     * @param member field/method to determine allowed values for
     * @return applicable "const"/"enum" values or null
     */
    protected List<String> resolveEnum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::allowableValues, values -> values.length > 0)
                .map(Arrays::asList)
                .orElse(null);
    }

    /**
     * Look-up the default value for the given field/method from {@code @Schema(defaultValue = ...)}.
     *
     * @param member field/method to determine default value for
     * @return default property value or null
     */
    protected String resolveDefault(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::defaultValue, defaultValue -> !defaultValue.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up the value from {@code @Schema(minLength = ...)} for the given field/method.
     *
     * @param member field/method to look-up minimum string length for
     * @return minimum string length or null
     */
    protected Integer resolveMinLength(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::minLength, minLength -> minLength > 0)
                .orElse(null);
    }

    /**
     * Look-up the value from {@code @Schema(maxLength = ...)} for the given field/method.
     *
     * @param member field/method to look-up maximum string length for
     * @return maximum string length or null
     */
    protected Integer resolveMaxLength(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::maxLength, maxLength -> maxLength < Integer.MAX_VALUE && maxLength > -1)
                .orElse(null);
    }

    /**
     * Look-up the value from {@code @Schema(format = ...)} for the given field/method.
     *
     * @param member field/method to look-up format for
     * @return format value or null
     */
    protected String resolveFormat(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::format, format -> !format.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up the value from {@code @Schema(pattern = ...)} for the given field/method.
     *
     * @param member field/method to look-up pattern for
     * @return pattern value or null
     */
    protected String resolvePattern(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::pattern, pattern -> !pattern.isEmpty())
                .orElse(null);
    }

    /**
     * Look-up the value from {@code @Schema(multipleOf = ...)} for the given field/method.
     *
     * @param member field/method to look-up multipleOf for
     * @return multipleOf value or null
     */
    protected BigDecimal resolveMultipleOf(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::multipleOf, multipleOf -> multipleOf != 0)
                .map(BigDecimal::valueOf)
                .orElse(null);
    }

    /**
     * Look-up the exclusive maximum value from {@code @Schema(maximum = ..., exclusiveMaxium = true)} for the given field/method.
     *
     * @param member field/method to look-up exclusiveMaximum for
     * @return exclusiveMaximum value or null
     */
    protected BigDecimal resolveExclusiveMaximum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::maximum, maximum -> !maximum.isEmpty())
                .filter(_maximum -> this.getSchemaAnnotationValue(member, Schema::exclusiveMaximum, Boolean.TRUE::equals).isPresent())
                .map(BigDecimal::new)
                .orElse(null);
    }

    /**
     * Look-up the inclusive maximum value from {@code @Schema(maximum = ..., exclusiveMaxium = false)} for the given field/method.
     *
     * @param member field/method to look-up maximum for
     * @return maximum value or null
     */
    protected BigDecimal resolveInclusiveMaximum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::maximum, maximum -> !maximum.isEmpty())
                .filter(_maximum -> this.getSchemaAnnotationValue(member, Schema::exclusiveMaximum, Boolean.FALSE::equals).isPresent())
                .map(BigDecimal::new)
                .orElse(null);
    }

    /**
     * Look-up the exclusive minimum value from {@code @Schema(minimum = ..., exclusiveMinium = true)} for the given field/method.
     *
     * @param member field/method to look-up exclusiveMinimum for
     * @return exclusiveMinimum value or null
     */
    protected BigDecimal resolveExclusiveMinimum(MemberScope<?, ?> member) {
        return this.getSchemaAnnotationValue(member, Schema::minimum, minimum -> !minimum.isEmpty())
                .filter(_minimum -> this.getSchemaAnnotationValue(member, Schema::exclusiveMinimum, Boolean.TRUE::equals).isPresent())
                .map(BigDecimal::new)
                .orElse(null);
    }

    /**
     * Look-up the inclusive minimum value from {@code @Schema(minimum = ..., exclusiveMinium = false)} for the given field/method.
     *
     * @param member field/method to look-up minimum for
     * @return minimum value or null
     */
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
        return this.getArraySchemaAnnotation(member)
                .map(ArraySchema::minItems)
                .filter(minItems -> minItems != Integer.MAX_VALUE)
                .orElse(null);
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
        return this.getArraySchemaAnnotation(member)
                .map(ArraySchema::maxItems)
                .filter(maxItems -> maxItems != Integer.MIN_VALUE)
                .orElse(null);
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
        return this.getArraySchemaAnnotation(member)
                .map(ArraySchema::uniqueItems)
                .filter(uniqueItemsFlag -> uniqueItemsFlag)
                .orElse(null);
    }

    /**
     * Implementation of the {@code CustomPropertyDefinitionProvider} to consider external references given in {@code @Schema(ref = ...)}.
     *
     * @param scope field/method to determine custom definition for
     * @param context generation context
     * @return custom definition containing the looked-up external reference or null
     */
    protected CustomPropertyDefinition provideCustomSchemaDefinition(MemberScope<?, ?> scope, SchemaGenerationContext context) {
        Optional<String> externalReference = this.getSchemaAnnotationValue(scope, Schema::ref, ref -> !ref.isEmpty());
        if (!externalReference.isPresent()) {
            return null;
        }
        // in Draft 6 and Draft 7, no other keywords are allowed besides a "$ref"
        CustomDefinition.AttributeInclusion attributeInclusion;
        switch (context.getGeneratorConfig().getSchemaVersion()) {
        case DRAFT_6:
        // fall-through (same as Draft 7)
        case DRAFT_7:
            attributeInclusion = CustomDefinition.AttributeInclusion.NO;
            break;
        default:
            attributeInclusion = CustomDefinition.AttributeInclusion.YES;
        }
        ObjectNode reference = context.getGeneratorConfig().createObjectNode()
                .put(context.getKeyword(SchemaKeyword.TAG_REF), externalReference.get());
        return new CustomPropertyDefinition(reference, attributeInclusion);
    }

    /**
     * Consider various remaining aspects.
     * <ul>
     * <li>{@code @Schema(not = ...)}</li>
     * <li>{@code @Schema(allOf = ...)}</li>
     * <li>{@code @Schema(minProperties = ...)}</li>
     * <li>{@code @Schema(maxProperties = ...)}</li>
     * <li>{@code @Schema(requiredProperties = ...)}</li>
     * </ul>
     *
     * @param memberAttributes already collected schema for the field/method
     * @param member targeted field/method
     * @param context generation context
     */
    protected void overrideInstanceAttributes(ObjectNode memberAttributes, MemberScope<?, ?> member, SchemaGenerationContext context) {
        Schema annotation = this.getSchemaAnnotationValue(member, Function.identity(), x -> true)
                .orElse(null);
        if (annotation == null) {
            return;
        }
        if (annotation.not() != Void.class) {
            memberAttributes.set(context.getKeyword(SchemaKeyword.TAG_NOT),
                    context.createDefinitionReference(context.getTypeContext().resolve(annotation.not())));
        }
        if (annotation.allOf().length > 0) {
            ArrayNode allOfArray = memberAttributes.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF));
            Stream.of(annotation.allOf())
                    .map(context.getTypeContext()::resolve)
                    .map(context::createDefinitionReference)
                    .forEach(allOfArray::add);
        }
        if (annotation.anyOf().length > 0) {
            // since 4.26.0
            ArrayNode allOfArray = memberAttributes.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF));
            ArrayNode anyOfArray = allOfArray.addObject().withArray(context.getKeyword(SchemaKeyword.TAG_ANYOF));
            Stream.of(annotation.anyOf())
                    .map(context.getTypeContext()::resolve)
                    .map(context::createDefinitionReference)
                    .forEach(anyOfArray::add);
        }
        if (annotation.oneOf().length > 0) {
            // since 4.26.0
            ArrayNode allOfArray = memberAttributes.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF));
            ArrayNode oneOfArray = allOfArray.addObject().withArray(context.getKeyword(SchemaKeyword.TAG_ONEOF));
            Stream.of(annotation.oneOf())
                    .map(context.getTypeContext()::resolve)
                    .map(context::createDefinitionReference)
                    .forEach(oneOfArray::add);
        }
        if (annotation.minProperties() > 0) {
            memberAttributes.put(context.getKeyword(SchemaKeyword.TAG_PROPERTIES_MIN), annotation.minProperties());
        }
        if (annotation.maxProperties() > 0) {
            memberAttributes.put(context.getKeyword(SchemaKeyword.TAG_PROPERTIES_MAX), annotation.maxProperties());
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
     * Create a {@link ConfigFunction} that extracts a value from the {@link Schema} annotation of a {@link TypeScope}.
     *
     * @param valueExtractor the getter for the value from the annotation
     * @param valueFilter filter that determines whether the value from a given annotation matches our criteria
     * @param <T> the type of the returned value
     * @return the value from the matching type's {@link Schema} annotation or {@code Optional.empty()}
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
