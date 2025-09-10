package com.github.victools.jsonschema.module.custom.annotations;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface JsonSchema {
    String[] versions() default {};

    String name() default "";

    String title() default "";

    String description() default "";

    Schema.AdditionalPropertiesValue additionalProperties() default Schema.AdditionalPropertiesValue.USE_ADDITIONAL_PROPERTIES_ANNOTATION;

    String ref() default "";

    Class<?> implementation() default Void.class;

    boolean hidden() default false;

    boolean required() default false;

    boolean nullable() default false;

    String[] allowableValues() default {};

    String defaultValue() default "";

    int minLength() default 0;

    int maxLength() default Integer.MAX_VALUE;

    String format() default "";

    String pattern() default "";

    double multipleOf() default (double) 0.0F;

    String maximum() default "";

    boolean exclusiveMaximum() default false;

    String minimum() default "";

    boolean exclusiveMinimum() default false;

    Class<?> not() default Void.class;

    Class<?>[] allOf() default {};

    Class<?>[] oneOf() default {};

    Class<?>[] anyOf() default {};

    int minProperties() default 0;

    int maxProperties() default 0;

    String[] requiredProperties() default {};
}
