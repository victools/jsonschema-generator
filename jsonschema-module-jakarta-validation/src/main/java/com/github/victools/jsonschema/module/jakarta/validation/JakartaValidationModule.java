/*
 * Copyright 2020 VicTools & Sascha Kohlmann
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

package com.github.victools.jsonschema.module.jakarta.validation;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * JSON Schema Generation Module: based on annotations from the {@code jakarta.validation.constraints} package.
 * <ul>
 * <li>Determine whether a member is not nullable, base assumption being that all fields and method return values are nullable if not annotated.</li>
 * <li>Optionally: also indicate all explicitly not nullable fields/methods to be required.</li>
 * <li>Populate "minItems" and "maxItems" for containers (i.e. arrays and collections).</li>
 * <li>Populate "minLength", "maxLength" and "format" for strings.</li>
 * <li>Optionally: populate "pattern" for strings.</li>
 * <li>Populate "minimum"/"exclusiveMinimum" and "maximum"/"exclusiveMaximum" for numbers.</li>
 * </ul>
 */
public class JakartaValidationModule implements Module {

    private final Set<JakartaValidationOption> options;
    private Set<Class<?>> validationGroups;

    /**
     * Constructor.
     *
     * @param options features to enable
     */
    public JakartaValidationModule(JakartaValidationOption... options) {
        this.options = options == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(options));
        // by default: ignore validation groups
        this.validationGroups = null;
    }

    /**
     * Add validation groups to be considered.
     * <ul>
     * <li>Never calling this method will result in all annotations to be picked-up.</li>
     * <li>Calling this without parameters will only consider those annotations where no groups are defined.</li>
     * <li>Calling this with not-null parameters will only consider those annotations without defined groups or where at least one matches.</li>
     * </ul>
     *
     * @param validationGroups validation groups to consider
     * @return this module instance (for chaining)
     */
    public JakartaValidationModule forValidationGroups(Class<?>... validationGroups) {
        if (validationGroups == null) {
            this.validationGroups = null;
        } else {
            this.validationGroups = new HashSet<>(Arrays.asList(validationGroups));
        }
        return this;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<FieldScope> fieldConfigPart = builder.forFields();
        this.applyToConfigPart(fieldConfigPart);
        if (this.options.contains(JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED)) {
            fieldConfigPart.withRequiredCheck(this::isRequired);
        }

        SchemaGeneratorConfigPart<MethodScope> methodConfigPart = builder.forMethods();
        this.applyToConfigPart(methodConfigPart);
        if (this.options.contains(JakartaValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED)) {
            methodConfigPart.withRequiredCheck(this::isRequired);
        }
    }

    /**
     * Apply the various annotation-based resolvers for the given configuration part (this is expected to be executed for both fields and methods).
     *
     * @param configPart config builder part to add configurations to
     */
    private void applyToConfigPart(SchemaGeneratorConfigPart<?> configPart) {
        configPart.withNullableCheck(this::isNullable);
        configPart.withArrayMinItemsResolver(this::resolveArrayMinItems);
        configPart.withArrayMaxItemsResolver(this::resolveArrayMaxItems);
        configPart.withStringMinLengthResolver(this::resolveStringMinLength);
        configPart.withStringMaxLengthResolver(this::resolveStringMaxLength);
        configPart.withStringFormatResolver(this::resolveStringFormat);
        configPart.withNumberInclusiveMinimumResolver(this::resolveNumberInclusiveMinimum);
        configPart.withNumberExclusiveMinimumResolver(this::resolveNumberExclusiveMinimum);
        configPart.withNumberInclusiveMaximumResolver(this::resolveNumberInclusiveMaximum);
        configPart.withNumberExclusiveMaximumResolver(this::resolveNumberExclusiveMaximum);

        if (this.options.contains(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS)) {
            configPart.withStringPatternResolver(this::resolveStringPattern);
        }
    }

    /**
     * Retrieves the annotation instance of the given type, either from the field itself or (if not present) from its getter.
     * <br>
     * If the given field/method represents only a container item of the actual declared type, that container item's annotations are being checked.
     *
     * @param <A> type of annotation
     * @param member field or method to retrieve annotation instance from (or from a field's getter or getter method's field)
     * @param annotationClass type of annotation
     * @param validationGroupsLookup how to look-up the associated validation groups of an annotation instance
     * @return annotation instance (or {@code null})
     * @see MemberScope#getAnnotationConsideringFieldAndGetterIfSupported(Class)
     * @see MemberScope#getContainerItemAnnotationConsideringFieldAndGetterIfSupported(Class)
     */
    protected <A extends Annotation> A getAnnotationFromFieldOrGetter(MemberScope<?, ?> member, Class<A> annotationClass,
            Function<A, Class<?>[]> validationGroupsLookup) {
        A containerItemAnnotation = member.getContainerItemAnnotationConsideringFieldAndGetterIfSupported(annotationClass);
        if (this.shouldConsiderAnnotation(containerItemAnnotation, validationGroupsLookup)) {
            return containerItemAnnotation;
        }
        A annotation = member.getAnnotationConsideringFieldAndGetterIfSupported(annotationClass);
        if (this.shouldConsiderAnnotation(annotation, validationGroupsLookup)) {
            return annotation;
        }
        return null;
    }

    /**
     * Check whether a given annotation is supposed to be considered in the schema generation. I.e. if specific validation groups are defined, it must
     * belong to at least one of them.
     *
     * @param <A> type of annotation
     * @param annotation annotation instance (may be {@code null}, which will result in {@code false} to be returned)
     * @param validationGroupsLookup how to look-up the associated validation groups of an annotation instance
     * @return whether the given annotation should be considered in the current schema generation
     */
    private <A extends Annotation> boolean shouldConsiderAnnotation(A annotation, Function<A, Class<?>[]> validationGroupsLookup) {
        // avoid repeated null checks by doing it here
        if (annotation == null) {
            return false;
        }
        // no specific validation groups means: all annotations are fair game
        if (this.validationGroups == null) {
            return true;
        }
        // check that the annotation's validation groups have at least one common entry to the configured groups on this module
        Class<?>[] associatedGroups = validationGroupsLookup.apply(annotation);
        return associatedGroups.length == 0 || !Collections.disjoint(this.validationGroups, Arrays.asList(associatedGroups));
    }

    /**
     * Determine whether a given field or method is annotated to be not nullable.
     *
     * @param member the field or method to check
     * @return whether member is annotated as nullable or not (returns null if not specified: assumption it is nullable then)
     */
    protected Boolean isNullable(MemberScope<?, ?> member) {
        Boolean result;
        if (this.getAnnotationFromFieldOrGetter(member, NotNull.class, NotNull::groups) != null
                || this.getAnnotationFromFieldOrGetter(member, NotBlank.class, NotBlank::groups) != null
                || this.getAnnotationFromFieldOrGetter(member, NotEmpty.class, NotEmpty::groups) != null) {
            // field is specifically NOT nullable
            result = Boolean.FALSE;
        } else if (this.getAnnotationFromFieldOrGetter(member, Null.class, Null::groups) != null) {
            // field is specifically null (and thereby nullable)
            result = Boolean.TRUE;
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Determine whether a given field or method is deemed to be required in its parent type.
     *
     * @param member the field or method to check
     * @return whether member is deemed to be required or not
     */
    protected boolean isRequired(MemberScope<?, ?> member) {
        Boolean nullableCheckResult = this.isNullable(member);
        return Boolean.FALSE.equals(nullableCheckResult);
    }

    /**
     * Determine a given array type's minimum number of items.
     *
     * @param member the field or method to check
     * @return specified minimum number of array items (or null)
     * @see Size
     */
    protected Integer resolveArrayMinItems(MemberScope<?, ?> member) {
        if (member.isContainerType()) {
            Size sizeAnnotation = this.getAnnotationFromFieldOrGetter(member, Size.class, Size::groups);
            if (sizeAnnotation != null && sizeAnnotation.min() > 0) {
                // minimum length greater than the default 0 was specified
                return sizeAnnotation.min();
            }
            if (this.getAnnotationFromFieldOrGetter(member, NotEmpty.class, NotEmpty::groups) != null) {
                return 1;
            }
        }
        return null;
    }

    /**
     * Determine a given array type's maximum number of items.
     *
     * @param member the field or method to check
     * @return specified maximum number of array items (or null)
     * @see Size
     */
    protected Integer resolveArrayMaxItems(MemberScope<?, ?> member) {
        if (member.isContainerType()) {
            Size sizeAnnotation = this.getAnnotationFromFieldOrGetter(member, Size.class, Size::groups);
            if (sizeAnnotation != null && sizeAnnotation.max() < 2147483647) {
                // maximum length below the default 2147483647 was specified
                return sizeAnnotation.max();
            }
        }
        return null;
    }

    /**
     * Determine a given text type's minimum number of characters.
     *
     * @param member the field or method to check
     * @return specified minimum number of characters (or null)
     * @see Size
     * @see NotEmpty
     * @see NotBlank
     */
    protected Integer resolveStringMinLength(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(CharSequence.class)) {
            Size sizeAnnotation = this.getAnnotationFromFieldOrGetter(member, Size.class, Size::groups);
            if (sizeAnnotation != null && sizeAnnotation.min() > 0) {
                // minimum length greater than the default 0 was specified
                return sizeAnnotation.min();
            }
            if (this.getAnnotationFromFieldOrGetter(member, NotEmpty.class, NotEmpty::groups) != null
                    || this.getAnnotationFromFieldOrGetter(member, NotBlank.class, NotBlank::groups) != null) {
                return 1;
            }
        }
        return null;
    }

    /**
     * Determine a given text type's maximum number of characters.
     *
     * @param member the field or method to check
     * @return specified minimum number of characters (or null)
     * @see Size
     */
    protected Integer resolveStringMaxLength(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(CharSequence.class)) {
            Size sizeAnnotation = this.getAnnotationFromFieldOrGetter(member, Size.class, Size::groups);
            if (sizeAnnotation != null && sizeAnnotation.max() < 2147483647) {
                // maximum length below the default 2147483647 was specified
                return sizeAnnotation.max();
            }
        }
        return null;
    }

    /**
     * Determine a given text type's format.
     *
     * @param member the field or method to check
     * @return specified format (or null)
     * @see Email
     */
    protected String resolveStringFormat(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(CharSequence.class)) {
            Email emailAnnotation = this.getAnnotationFromFieldOrGetter(member, Email.class, Email::groups);
            if (emailAnnotation != null) {
                // @Email annotation was found, indicate the respective format
                if (this.options.contains(JakartaValidationOption.PREFER_IDN_EMAIL_FORMAT)) {
                    // the option was set to rather return the value for the internationalised email format
                    return "idn-email";
                }
                // indicate standard internet email address format
                return "email";
            }
        }
        return null;
    }

    /**
     * Determine a given text type's pattern.
     *
     * @param member the field or method to check
     * @return specified pattern (or null)
     * @see Pattern
     */
    protected String resolveStringPattern(MemberScope<?, ?> member) {
        if (member.getType().isInstanceOf(CharSequence.class)) {
            Pattern patternAnnotation = this.getAnnotationFromFieldOrGetter(member, Pattern.class, Pattern::groups);
            if (patternAnnotation != null) {
                // @Pattern annotation was found, return its (mandatory) regular expression
                return patternAnnotation.regexp();
            }
            Email emailAnnotation = this.getAnnotationFromFieldOrGetter(member, Email.class, Email::groups);
            if (emailAnnotation != null && !".*".equals(emailAnnotation.regexp())) {
                // non-default regular expression on @Email annotation should also be considered
                return emailAnnotation.regexp();
            }
        }
        return null;
    }

    /**
     * Determine a number type's minimum (inclusive) value.
     *
     * @param member the field or method to check
     * @return specified inclusive minimum value (or null)
     * @see Min
     * @see DecimalMin
     * @see PositiveOrZero
     */
    protected BigDecimal resolveNumberInclusiveMinimum(MemberScope<?, ?> member) {
        Min minAnnotation = this.getAnnotationFromFieldOrGetter(member, Min.class, Min::groups);
        if (minAnnotation != null) {
            return new BigDecimal(minAnnotation.value());
        }
        DecimalMin decimalMinAnnotation = this.getAnnotationFromFieldOrGetter(member, DecimalMin.class, DecimalMin::groups);
        if (decimalMinAnnotation != null && decimalMinAnnotation.inclusive()) {
            return new BigDecimal(decimalMinAnnotation.value());
        }
        PositiveOrZero positiveAnnotation = this.getAnnotationFromFieldOrGetter(member, PositiveOrZero.class, PositiveOrZero::groups);
        if (positiveAnnotation != null) {
            return BigDecimal.ZERO;
        }
        return null;
    }

    /**
     * Determine a number type's minimum (exclusive) value.
     *
     * @param member the field or method to check
     * @return specified exclusive minimum value (or null)
     * @see DecimalMin
     * @see Positive
     */
    protected BigDecimal resolveNumberExclusiveMinimum(MemberScope<?, ?> member) {
        DecimalMin decimalMinAnnotation = this.getAnnotationFromFieldOrGetter(member, DecimalMin.class, DecimalMin::groups);
        if (decimalMinAnnotation != null && !decimalMinAnnotation.inclusive()) {
            return new BigDecimal(decimalMinAnnotation.value());
        }
        Positive positiveAnnotation = this.getAnnotationFromFieldOrGetter(member, Positive.class, Positive::groups);
        if (positiveAnnotation != null) {
            return BigDecimal.ZERO;
        }
        return null;
    }

    /**
     * Determine a number type's maximum (inclusive) value.
     *
     * @param member the field or method to check
     * @return specified inclusive maximum value (or null)
     * @see Max
     * @see DecimalMax#inclusive()
     * @see NegativeOrZero
     */
    protected BigDecimal resolveNumberInclusiveMaximum(MemberScope<?, ?> member) {
        Max maxAnnotation = this.getAnnotationFromFieldOrGetter(member, Max.class, Max::groups);
        if (maxAnnotation != null) {
            return new BigDecimal(maxAnnotation.value());
        }
        DecimalMax decimalMaxAnnotation = this.getAnnotationFromFieldOrGetter(member, DecimalMax.class, DecimalMax::groups);
        if (decimalMaxAnnotation != null && decimalMaxAnnotation.inclusive()) {
            return new BigDecimal(decimalMaxAnnotation.value());
        }
        NegativeOrZero negativeAnnotation = this.getAnnotationFromFieldOrGetter(member, NegativeOrZero.class, NegativeOrZero::groups);
        if (negativeAnnotation != null) {
            return BigDecimal.ZERO;
        }
        return null;
    }

    /**
     * Determine a number type's maximum (exclusive) value.
     *
     * @param member the field or method to check
     * @return specified exclusive maximum value (or null)
     * @see DecimalMax#inclusive()
     * @see Negative
     */
    protected BigDecimal resolveNumberExclusiveMaximum(MemberScope<?, ?> member) {
        DecimalMax decimalMaxAnnotation = this.getAnnotationFromFieldOrGetter(member, DecimalMax.class, DecimalMax::groups);
        if (decimalMaxAnnotation != null && !decimalMaxAnnotation.inclusive()) {
            return new BigDecimal(decimalMaxAnnotation.value());
        }
        Negative negativeAnnotation = this.getAnnotationFromFieldOrGetter(member, Negative.class, Negative::groups);
        if (negativeAnnotation != null) {
            return BigDecimal.ZERO;
        }
        return null;
    }
}
