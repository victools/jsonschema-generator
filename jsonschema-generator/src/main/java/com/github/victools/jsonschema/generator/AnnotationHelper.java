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

import com.fasterxml.classmate.members.ResolvedMember;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helper class providing with standard mechanism to resolve annotations on annotated entities.
 * 
 * @since 4.37.0
 */
public final class AnnotationHelper {
    
    private AnnotationHelper() {
        super();
    }

    /**
     * Resolves the specified annotation on the given resolved member and resolve nested annotations.
     *
     * @param <A> the generic type of the annotation
     * @param member where to look for the specified annotation
     * @param annotationClass the class of the annotation to look for
     * @param metaAnnotationPredicate the predicate indicating nested annotations
     * @return an empty entry if not found
     */
    public static <A extends Annotation> Optional<A> resolveAnnotation(
            ResolvedMember<?> member,
            Class<A> annotationClass,
            Predicate<Annotation> metaAnnotationPredicate
    ) {
        final A annotation = member.getAnnotations().get(annotationClass);
        if (annotation != null) {
            return Optional.of(annotation);
        }
        return resolveNestedAnnotations(StreamSupport.stream(member.getAnnotations().spliterator(), false), annotationClass, metaAnnotationPredicate);
    }

    /**
     * Select the instance of the specified annotation type from the given list. 
     * 
     * <p>Also considering meta annotations (i.e., annotations on annotations) if a meta annotation is 
     * deemed eligible according to the given {@code Predicate}.</p>
     *
     * @param <A> the generic type of the annotation
     * @param annotationList a list of annotations to look into
     * @param annotationClass the class of the annotation to look for
     * @param metaAnnotationPredicate the predicate indicating nested annotations
     * @return an empty entry if not found
     */
    public static <A extends Annotation> Optional<A> resolveAnnotation(
            List<Annotation> annotationList,
            Class<A> annotationClass,
            Predicate<Annotation> metaAnnotationPredicate
    ) {
        final Optional<Annotation> annotation = annotationList.stream().filter(annotationClass::isInstance).findFirst();
        if (annotation.isPresent()) {
            return annotation.map(annotationClass::cast);
        }
        return resolveNestedAnnotations(annotationList.stream(), annotationClass, metaAnnotationPredicate);
    }

    /**
     * Select the instance of the specified annotation type from the given annotatedElement's annotations. 
     *
     * <p>Also considering meta annotations (i.e., annotations on annotations) if a meta annotation is 
     * deemed eligible according to the given <code>metaAnnotationPredicate</code>.</p>
     *
     * @param <A> the generic type of the annotation
     * @param annotatedElement where to look for the specified annotation
     * @param annotationClass the class of the annotation to look for
     * @param metaAnnotationPredicate the predicate indicating meta annotations
     * @return an empty entry if not found
     */
    public static <A extends Annotation> Optional<A> resolveAnnotation(
            AnnotatedElement annotatedElement,
            Class<A> annotationClass,
            Predicate<Annotation> metaAnnotationPredicate
    ) {
        final A annotation = annotatedElement.getAnnotation(annotationClass);
        if (annotation != null) {
            return Optional.of(annotation);
        }
        return resolveNestedAnnotations(Arrays.stream(annotatedElement.getAnnotations()), annotationClass, metaAnnotationPredicate);
    }

    private static <A extends Annotation> Optional<A> resolveNestedAnnotations(
            Stream<Annotation> initialAnnotations,
            Class<A> annotationClass,
            Predicate<Annotation> metaAnnotationPredicate
    ) {
        List<Annotation> annotations = extractAnnotationsFromMetaAnnotations(initialAnnotations, metaAnnotationPredicate);
        while (!annotations.isEmpty()) {
            final Optional<Annotation> directAnnotation = annotations.stream().filter(annotationClass::isInstance).findFirst();
            if (directAnnotation.isPresent()) {
                return directAnnotation.map(annotationClass::cast);
            }
            annotations = extractAnnotationsFromMetaAnnotations(annotations.stream(), metaAnnotationPredicate);
        }
        return Optional.empty();
    }

    private static List<Annotation> extractAnnotationsFromMetaAnnotations(
            Stream<Annotation> annotations,
            Predicate<Annotation> metaAnnotationPredicate
    ) {
        return annotations.filter(metaAnnotationPredicate)
                .flatMap(a -> Arrays.stream(a.annotationType().getAnnotations()))
                .collect(Collectors.toList());
    }
}
