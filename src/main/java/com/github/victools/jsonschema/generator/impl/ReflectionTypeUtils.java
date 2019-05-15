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

import com.github.victools.jsonschema.generator.JavaType;
import com.github.victools.jsonschema.generator.TypeVariableContext;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Helper functions related to reflections for identifying/resolving types.
 */
public final class ReflectionTypeUtils {

    /**
     * Hidden constructor to prevent instantiation of utility class.
     */
    private ReflectionTypeUtils() {
        // prevent instantiation of static helper class
    }

    /**
     * Determine the raw type of a given generic type. Beware: this returns null for arrays.
     *
     * @param javaType (possibly) generic type to determine the underlying raw class for
     * @return successfully determined raw class, or null in case of an array
     * @throws UnsupportedOperationException if given type is anything but a Class, ParameterizedType or GenericArrayType
     */
    public static Class<?> getRawType(Type javaType) {
        Class<?> rawType;
        if (javaType instanceof Class<?>) {
            // generic type is not generic at all, simply return it
            rawType = (Class<?>) javaType;
        } else if (javaType instanceof ParameterizedType) {
            // get rid of generic/parameter information and return underlying class
            rawType = (Class<?>) ((ParameterizedType) javaType).getRawType();
        } else if (javaType instanceof GenericArrayType) {
            // there is no Array class (well there is, but is only an array's representation in the reflection context)
            rawType = null;
        } else {
            // other types like TypeVariables or Wildcards should not be passed into this method
            throw new UnsupportedOperationException("Unsupported type: " + javaType);
        }
        return rawType;
    }

    /**
     * Determine whether the given (generic) type represents an array or collection, which should both be of "type" "array" in a JSON schema.
     *
     * @param javaType (generic) type to check for being an array
     * @return whether the given type represents an array or collection
     */
    public static boolean isArrayType(JavaType javaType) {
        Type genericType = javaType.getResolvedType();
        if (genericType instanceof GenericArrayType) {
            // ReflectionUtils.getRawType() returns null for a GenericArrayType
            return true;
        }
        Class<?> rawType = ReflectionTypeUtils.getRawType(genericType);
        return rawType.isArray() || Collection.class.isAssignableFrom(rawType);
    }

    /**
     * Determine the type of the item/component within the given array type.
     *
     * @param arrayType array type for which to determine the type of contained items/components
     * @return item/component type
     * @see #isArrayType(JavaType)
     */
    public static JavaType getArrayComponentType(JavaType arrayType) {
        TypeVariableContext componentTypeVariables = TypeVariableContext.forType(arrayType);
        Type componentType = null;
        if (ReflectionTypeUtils.isArrayType(arrayType)) {
            Type genericType = arrayType.getResolvedType();
            if (genericType instanceof GenericArrayType) {
                // an array whose component type is either a ParameterizedType or a TypeVariable
                componentType = ((GenericArrayType) genericType).getGenericComponentType();
            } else if (genericType instanceof Class<?>) {
                // an array whose component type is a plain Class
                componentType = ((Class<?>) genericType).getComponentType();
            } else if (genericType instanceof ParameterizedType) {
                // an implementation of the Collection interface
                ParameterizedType parameterizedTargetType = (ParameterizedType) genericType;
                Class<?> rawTargetType = (Class<?>) parameterizedTargetType.getRawType();
                while (rawTargetType != Collection.class) {
                    componentTypeVariables = TypeVariableContext.forType(parameterizedTargetType, componentTypeVariables);
                    Type collectionSuperType = Stream.of(rawTargetType.getGenericInterfaces())
                            .filter(interfaceType -> Collection.class.isAssignableFrom(ReflectionTypeUtils.getRawType(interfaceType)))
                            .findFirst()
                            .orElse(rawTargetType.getGenericSuperclass());
                    parameterizedTargetType = (ParameterizedType) collectionSuperType;
                    rawTargetType = (Class<?>) parameterizedTargetType.getRawType();
                }
                componentType = parameterizedTargetType.getActualTypeArguments()[0];
            }
        }
        if (componentType == null) {
            throw new UnsupportedOperationException("Cannot determine array component type for target: " + arrayType);
        }
        return componentTypeVariables.resolveGenericTypePlaceholder(componentType);
    }
}
