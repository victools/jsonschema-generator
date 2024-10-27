package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * Unit test class dedicated to the validation of {@link JacksonHelper}.
 *
 * @author Antoine Malliarakis
 */
class JacksonHelperTest {
    
    @JsonTypeName
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
    @JacksonAnnotationsInside
    private @interface UselessFirstComboAnnotation {
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    private @interface UselessSecondComboAnnotation {
    }
    
    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonTypeName("first combo annotation value")
    private @interface FirstComboAnnotation {
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonTypeName("second combo annotation value")
    private @interface SecondComboAnnotation {
    }

    @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @SecondComboAnnotation
    private @interface ThirdComboAnnotation {
    }
    
    @FirstComboAnnotation
    @SecondComboAnnotation
    private static class IndirectlyAnnotatedClass {
    }

    @JsonTypeName("direct value")
    @FirstComboAnnotation
    @SecondComboAnnotation
    private static class BothDirectAndIndirectlyAnnotatedClass {
    }
    
    @ThirdComboAnnotation
    @FirstComboAnnotation
    private static class BreadthFirstAnnotatedClass {}

    @Test
    void resolveAnnotation_returnsAnEmptyInstanceIfNotAnnotated() {
        final Optional<JsonTypeName> result = JacksonHelper.resolveAnnotation(NonAnnotatedClass.class, JsonTypeName.class);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    void resolveAnnotation_returnsAnEmptyInstanceIfNotAnnotatedEvenIfThereAreComboAnnotations() {
        final Optional<JsonTypeName> result = JacksonHelper.resolveAnnotation(AnnotatedClassWithUselessAnnotations.class, JsonTypeName.class);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    void resolveAnnotation_supportsDirectAnnotations() {
        final Optional<JsonTypeName> result = JacksonHelper.resolveAnnotation(DirectlyAnnotatedClass.class, JsonTypeName.class);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(result.get().value(), "");
    }
    
    @Test
    void resolveAnnotation_directAnnotationTakesPrecedence() {
        final Optional<JsonTypeName> result = JacksonHelper.resolveAnnotation(BothDirectAndIndirectlyAnnotatedClass.class, JsonTypeName.class);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("direct value", result.get().value());
    }
    
    @Test
    void resolveAnnotation_returnsFirstValueFound() {
        final Optional<JsonTypeName> result = JacksonHelper.resolveAnnotation(IndirectlyAnnotatedClass.class, JsonTypeName.class);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("first combo annotation value", result.get().value());
    }
    
    @Test
    void resolveAnnotation_usesABreadthFirstlookup() {
        final Optional<JsonTypeName> result = JacksonHelper.resolveAnnotation(BreadthFirstAnnotatedClass.class, JsonTypeName.class);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("first combo annotation value", result.get().value());
    }
}