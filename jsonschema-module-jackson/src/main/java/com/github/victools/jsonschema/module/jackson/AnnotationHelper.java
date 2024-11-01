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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

final class AnnotationHelper {

    static final Predicate<Annotation> JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER = (annotation) ->
            annotation.annotationType().isAnnotationPresent(JacksonAnnotationsInside.class);

    private AnnotationHelper() {
        super();
    }

    /**
     * Resolves the specified annotation on the given resolved member and resolve indirect jackson annotations.
     *
     * @param <A> the generic type of the annotation
     * @param member where to look for the specified annotation
     * @param annotationClass the class of the annotation to look for
     * @return an empty entry if not found
     */
    static <A extends Annotation> Optional<A> resolveAnnotation(ResolvedMember<?> member, Class<A> annotationClass) {
        return com.github.victools.jsonschema.generator.AnnotationHelper.resolveAnnotation(
                member,
                annotationClass,
                JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER
        );
    }

    /**
     * Resolves the specified annotation on the given type and resolve indirect jackson annotations.
     *
     * @param <A> the generic type of the annotation
     * @param declaringType where to look for the specified annotation
     * @param annotationClass the class of the annotation to look for
     * @return an empty entry if not found
     */
    static <A extends Annotation> Optional<A> resolveAnnotation(AnnotatedElement declaringType, Class<A> annotationClass) {
        return com.github.victools.jsonschema.generator.AnnotationHelper.resolveAnnotation(
                declaringType,
                annotationClass,
                JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER
        );
    }

}
