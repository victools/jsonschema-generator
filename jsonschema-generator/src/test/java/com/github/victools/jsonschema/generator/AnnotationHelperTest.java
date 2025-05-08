/*
 * Copyright 2024 VicTools.
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

package com.github.victools.jsonschema.generator;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit test class dedicated to the validation of {@link AnnotationHelper}.
 */
public class AnnotationHelperTest {

    static Stream<Arguments> annotationLookupScenarios() {
        return Stream.of(
                Arguments.of(NonAnnotatedClass.class, Optional.empty()),
                Arguments.of(AnnotatedClassWithUselessAnnotations.class, Optional.empty()),
                Arguments.of(DirectlyAnnotatedClass.class, Optional.of("")),
                Arguments.of(BothDirectAndIndirectlyAnnotatedClass.class, Optional.of("direct value")),
                Arguments.of(IndirectlyAnnotatedClass.class, Optional.of("first combo annotation value")),
                Arguments.of(BreadthFirstAnnotatedClass.class, Optional.of("first combo annotation value"))
        );
    }

    @ParameterizedTest
    @MethodSource("annotationLookupScenarios")
    void resolveAnnotation_AnnotatedElement_respects_annotationLookupScenarios(Class<?> annotatedClass, Optional<String> expectedAnnotationValue) {
        Optional<String> value = AnnotationHelper.resolveAnnotation(annotatedClass, TargetAnnotation.class, metaAnnotationPredicate()).map(TargetAnnotation::value);
        Assertions.assertEquals(expectedAnnotationValue, value);
    }

    @ParameterizedTest
    @MethodSource("annotationLookupScenarios")
    void resolveAnnotation_List_respects_annotationLookupScenarios(Class<?> annotatedClass, Optional<String> expectedAnnotationValue) {
        Optional<String> value = AnnotationHelper.resolveAnnotation(Arrays.asList(annotatedClass.getAnnotations()), TargetAnnotation.class, metaAnnotationPredicate()).map(TargetAnnotation::value);
        Assertions.assertEquals(expectedAnnotationValue, value);
    }

    private static Predicate<Annotation> metaAnnotationPredicate() {
        return (annotation) -> annotation.annotationType().isAnnotationPresent(MetaAnnotation.class);
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TargetAnnotation {
        String value() default "";
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MetaAnnotation {}
    
    @TargetAnnotation
    private static class DirectlyAnnotatedClass {
    }
    
    private static class NonAnnotatedClass {
    }
    
    @UselessFirstComboAnnotation
    @UselessSecondComboAnnotation
    private static class AnnotatedClassWithUselessAnnotations {
        
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @MetaAnnotation
    private @interface UselessFirstComboAnnotation {
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @MetaAnnotation
    private @interface UselessSecondComboAnnotation {
    }
    
    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @MetaAnnotation
    @TargetAnnotation("first combo annotation value")
    private @interface FirstComboAnnotation {
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @MetaAnnotation
    @TargetAnnotation("second combo annotation value")
    private @interface SecondComboAnnotation {
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @MetaAnnotation
    @SecondComboAnnotation
    private @interface ThirdComboAnnotation {
    }
    
    @FirstComboAnnotation
    @SecondComboAnnotation
    private static class IndirectlyAnnotatedClass {
    }

    @TargetAnnotation("direct value")
    @FirstComboAnnotation
    @SecondComboAnnotation
    private static class BothDirectAndIndirectlyAnnotatedClass {
    }
    
    @ThirdComboAnnotation
    @FirstComboAnnotation
    private static class BreadthFirstAnnotatedClass {}
    
}