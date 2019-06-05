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

import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import java.util.stream.Stream;

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
     * @param declaringType field's declaring type
     * @return public getter from within the field's declaring class (i.e. ignoring getters in sub classes)
     */
    public static ResolvedMethod findGetterForField(ResolvedField field, ResolvedTypeWithMembers declaringType) {
        String capitalisedFieldName = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        String getterName1 = "get" + capitalisedFieldName;
        String getterName2 = "is" + capitalisedFieldName;
        ResolvedMethod[] methods = declaringType.getMemberMethods();
        ResolvedMethod getter = Stream.of(methods)
                .filter(method -> method.getRawMember().getParameterCount() == 0)
                .filter(method -> method.getName().equals(getterName1) || method.getName().equals(getterName2))
                .filter(ResolvedMethod::isPublic)
                .findFirst()
                .orElse(null);
        return getter;
    }

    /**
     * Determine whether a given field's declaring class contains a matching method starting with "get" or "is".
     *
     * @param field targeted field
     * @param declaringType field's declaring type
     * @return whether a matching getter exists in the field's declaring class (i.e. ignoring getters in sub classes)
     */
    public static boolean hasGetter(ResolvedField field, ResolvedTypeWithMembers declaringType) {
        return ReflectionGetterUtils.findGetterForField(field, declaringType) != null;
    }

    /**
     * Look-up the field associated with a given a method if that is deemed to be a getter by convention.
     *
     * @param method targeted method
     * @param declaringType method's declaring type
     * @return associated field
     */
    public static ResolvedField findFieldForGetter(ResolvedMethod method, ResolvedTypeWithMembers declaringType) {
        if (method.getReturnType() == null || !method.isPublic()) {
            // void and non-public methods are not deemed to be getters
            return null;
        }
        String methodName = method.getName();
        String fieldName;
        if (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
            // ensure that the variable starts with a lower-case letter
            fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(methodName.charAt(2))) {
            // ensure that the variable starts with a lower-case letter
            fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        } else {
            // method name does not fall into getter conventions
            fieldName = null;
        }
        if (fieldName == null) {
            return null;
        }
        // method name matched getter conventions
        // check whether a matching field exists
        return Stream.of(declaringType.getMemberFields())
                .filter(memberField -> memberField.getName().equals(fieldName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Determine whether the given method's name matches the getter naming convention ("getFoo()"/"isFoo()") and a respective field ("foo") exists.
     *
     * @param method targeted method
     * @param declaringType method's declaring type
     * @return whether method name starts with "get"/"is" and rest matches name of field in declaring class (i.e. ignoring fields in super classes)
     */
    public static boolean isGetter(ResolvedMethod method, ResolvedTypeWithMembers declaringType) {
        return ReflectionGetterUtils.findFieldForGetter(method, declaringType) != null;
    }
}
