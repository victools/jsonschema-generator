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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Helper functions, for representing a type or method as string.
 */
public class ReflectionToStringUtils {

    /**
     * Constructing a string that fully represents the given type (including possible type parameters and their actual types).
     *
     * @param type the type to represent
     * @return resulting string
     */
    public static String createStringRepresentation(ResolvedType type) {
        String result = type.getErasedType().getSimpleName();
        List<ResolvedType> typeParameters = type.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            result += typeParameters.stream()
                    .map(ReflectionToStringUtils::createStringRepresentation)
                    .collect(Collectors.joining(", ", "<", ">"));
        }
        return result;
    }

    /**
     * Constructing a string that fully represents the given method (including arguments and possible type parameters and their actual types).
     *
     * @param method the method to represent
     * @return resulting string
     */
    public static String createStringRepresentation(ResolvedMethod method) {
        String result = method.getName();
        result += IntStream.range(0, method.getArgumentCount())
                .mapToObj(method::getArgumentType)
                .map(ReflectionToStringUtils::createStringRepresentation)
                .collect(Collectors.joining(", ", "(", ")"));
        return result;
    }
}
