/*
 * Copyright 2019 VicTools.
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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import java.util.Optional;

/**
 * Helper functions related to reflections, for identifying getters and their associated fields.
 */
public final class ReflectionGetterUtils {

    /**
     * Hidden constructor to prevent instantiation of utility class.
     */
    private ReflectionGetterUtils() {
        // prevent instantiation of static helper class
    }

    /**
     * Given a field, return the conventional getter method (if one exists).E.g. for field named "foo", look-up either "getFoo()" or "isFoo()".
     *
     * @param field targeted field
     * @param declaringContext the declaring type's description
     * @return public getter from within the field's declaring class (i.e. ignoring getters in sub classes)
     */
    public static AnnotatedMethod findGetterForField(AnnotatedField field, BeanDescription declaringContext) {
        Optional<BeanPropertyDefinition> property = declaringContext.findProperties()
                .stream()
                .filter(BeanPropertyDefinition::hasField)
                .filter(propertyDefinition -> propertyDefinition.getField().getAnnotated().equals(field.getAnnotated()))
                .findFirst();
        AnnotatedMethod getter = property.map(BeanPropertyDefinition::getGetter)
                .orElse(null);
        return getter;
    }

    /**
     * Determine whether a given field's declaring class contains a matching method starting with "get" or "is".
     *
     * @param field targeted field
     * @param declaringContext the declaring type's description
     * @return whether a matching getter exists in the field's declaring class (i.e. ignoring getters in sub classes)
     */
    public static boolean hasGetter(AnnotatedField field, BeanDescription declaringContext) {
        return ReflectionGetterUtils.findGetterForField(field, declaringContext) != null;
    }

    /**
     * Look-up the field associated with a given a method if that is deemed to be a getter by convention.
     *
     * @param method targeted method
     * @param declaringContext the declaring type's description
     * @return associated field
     */
    public static AnnotatedField findFieldForGetter(AnnotatedMethod method, BeanDescription declaringContext) {
        Optional<BeanPropertyDefinition> property = declaringContext.findProperties()
                .stream()
                .filter(BeanPropertyDefinition::hasGetter)
                .filter(propertyDefinition -> propertyDefinition.getGetter().getAnnotated().equals(method.getAnnotated()))
                .findFirst();
        AnnotatedField field = property.map(BeanPropertyDefinition::getField)
                .orElse(null);
        return field;
    }

    /**
     * Determine whether the given method's name matches the getter naming convention ("getFoo()"/"isFoo()") and a respective field ("foo") exists.
     *
     * @param method targeted method
     * @param declaringContext the declaring type's description
     * @return whether method name starts with "get"/"is" and rest matches name of field in declaring class (i.e. ignoring fields in super classes)
     */
    public static boolean isGetter(AnnotatedMethod method, BeanDescription declaringContext) {
        return ReflectionGetterUtils.findFieldForGetter(method, declaringContext) != null;
    }
}
