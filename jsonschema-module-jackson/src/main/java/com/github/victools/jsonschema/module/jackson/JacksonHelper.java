package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class JacksonHelper {

    static final Predicate<Annotation> JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER = new JacksonAnnotationsInsideAnnotatedFilter();

    private static final class JacksonAnnotationsInsideAnnotatedFilter implements Predicate<Annotation> {
        JacksonAnnotationsInsideAnnotatedFilter() {
            super();
        }
        @Override
        public boolean test(Annotation annotation) {
            return annotation.annotationType().isAnnotationPresent(JacksonAnnotationsInside.class);
        }
    }

    private JacksonHelper() {
        super();
    }

    /**
     * Resolves the specified annotation on the given resolved member and resolve indirect jackson annotations.
     *
     * <p>It uses the same algorithm as {@link com.github.victools.jsonschema.generator.TypeContext#getAnnotationFromList(Class, List, Predicate)}.</p>
     *
     * @param member where to look for the specified annotation
     * @param annotationClass the class of the annotation to look for
     * @return an empty entry if not found
     * @param <A> the generic type of the annotation
     */
    static <A extends Annotation> Optional<A> resolveAnnotation(ResolvedMember<?> member, Class<A> annotationClass) {
        final A annotation = member.getAnnotations().get(annotationClass);
        if (annotation != null) {
            return Optional.of(annotation);
        }
        return resolveNestedAnnotations(StreamSupport.stream(member.getAnnotations().spliterator(), false), annotationClass);
    }

    /**
     * Resolves the specified annotation on the given type and resolve indirect jackson annotations.
     * 
     * <p>It uses the same algorithm as {@link com.github.victools.jsonschema.generator.TypeContext#getAnnotationFromList(Class, List, Predicate)}.</p>
     * 
     * @param declaringType where to look for the specified annotation
     * @param annotationClass the class of the annotation to look for
     * @return an empty entry if not found
     * @param <A> the generic type of the annotation
     */
    static <A extends Annotation> Optional<A> resolveAnnotation(AnnotatedElement declaringType, Class<A> annotationClass) {
        final A annotation = declaringType.getAnnotation(annotationClass);
        if (annotation != null) {
            return Optional.of(annotation);
        }
        return resolveNestedAnnotations(Arrays.stream(declaringType.getAnnotations()), annotationClass);
    }
    
    private static <A extends Annotation> Optional<A> resolveNestedAnnotations(Stream<Annotation> initialAnnotations, Class<A> annotationClass) {
        List<Annotation> annotations = extractNestedAnnotations(initialAnnotations);
        while (!annotations.isEmpty()) {
            final Optional<Annotation> directAnnotation = annotations.stream().filter(annotationClass::isInstance).findFirst();
            if (directAnnotation.isPresent()) {
                return directAnnotation.map(annotationClass::cast);
            }
            annotations = extractNestedAnnotations(annotations.stream());
        }
        return Optional.empty();
    }
    
    private static List<Annotation> extractNestedAnnotations(Stream<Annotation> annotations) {
        return annotations.filter(JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER)
                .flatMap(a -> Arrays.stream(a.annotationType().getAnnotations()))
                .collect(Collectors.toList());
    }
}
