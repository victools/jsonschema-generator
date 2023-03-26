/*
 * Copyright 2023 VicTools.
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

package com.github.victools.jsonschema.examples;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.function.Function;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/discussions/335">#335</a>.
 * <br/>
 * Collecting attributes from annotations grouped via another custom annotation.
 */
public class GroupedAnnotationExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(new GroupedAnnotationAwareJakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS))
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(Example.class);
    }

    /**
     * Extend standard Jakarta validation module (exactly the same can be done for the Javax validation module), in order to utilise existing logic.
     */
    static class GroupedAnnotationAwareJakartaValidationModule extends JakartaValidationModule {

        GroupedAnnotationAwareJakartaValidationModule(JakartaValidationOption... options) {
            super(options);
        }

        @Override
        protected <A extends Annotation> A getAnnotationFromFieldOrGetter(MemberScope<?, ?> member, Class<A> annotationClass,
                Function<A, Class<?>[]> validationGroupsLookup) {
            A annotation = super.getAnnotationFromFieldOrGetter(member, annotationClass, validationGroupsLookup);
            if (annotation != null) {
                return annotation;
            }
            if (!member.isFakeContainerItemScope()) {
                Optional<A> groupedAnnotationFromMember = this.getGroupedAnnotationFromMember(member, annotationClass);
                if (!groupedAnnotationFromMember.isPresent()) {
                    MemberScope<?, ?> alternativeMember;
                    if (member instanceof FieldScope) {
                        alternativeMember = ((FieldScope) member).findGetter();
                    } else if (member instanceof MethodScope) {
                        alternativeMember = ((MethodScope) member).findGetterField();
                    } else {
                        return null;
                    }
                    if (alternativeMember != null) {
                        groupedAnnotationFromMember = this.getGroupedAnnotationFromMember(alternativeMember, annotationClass);
                    }
                }
                return groupedAnnotationFromMember.orElse(null);
            }
            return null;
        }

        private <A extends Annotation> Optional<A> getGroupedAnnotationFromMember(MemberScope<?, ?> member, Class<A> annotationClass) {
            for (Annotation otherAnnotation : member.getMember().getAnnotations()) {
                A annotation = otherAnnotation.annotationType().getDeclaredAnnotation(annotationClass);
                if (annotation != null) {
                    return Optional.of(annotation);
                }
            }
            return Optional.empty();
        }
    }

    static class Example {
        @CustomIdentifier
        private String identifier;
    }

    @Pattern(regexp = "^[a-zA-Z#@$][a-zA-Z#@$0-9]*$")
    @Size(min = 1, max = 8)
    @Constraint(validatedBy = {})
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomIdentifier {

        String message() default "";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }
}
