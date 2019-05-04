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

package com.github.victools.jsonschema.generator;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper functions related to reflections.
 */
public final class ReflectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    /**
     * Given a field, return the conventional getter method (if one exists). E.g. for field named "foo", look-up either "getFoo()" or "isFoo()".
     *
     * @param field targeted field
     * @return getter from within the field's declaring class (i.e. ignoring getters in sub classes)
     */
    public static Method findGetterForField(Field field) {
        String capitalisedFieldName = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        try {
            return field.getDeclaringClass().getDeclaredMethod("get" + capitalisedFieldName);
        } catch (NoSuchMethodException ex1) {
            try {
                return field.getDeclaringClass().getDeclaredMethod("is" + capitalisedFieldName);
            } catch (NoSuchMethodException ex2) {
                return null;
            }
        }
    }

    /**
     * Determine whether a given field's declaring class contains a matching method starting with "get" or "is".
     *
     * @param field targeted field
     * @return whether a matching getter exists in the field's declaring class (i.e. ignoring getters in sub classes)
     */
    public static boolean hasGetter(Field field) {
        return findGetterForField(field) != null;
    }

    /**
     * Look-up the field associated with a given a method if that is deemed to be a getter by convention.
     *
     * @param method targeted method
     * @return associated field
     */
    public static Field findFieldForGetter(Method method) {
        String methodName = method.getName();
        String fieldName;
        if (methodName.startsWith("get") && Character.isUpperCase(methodName.charAt(3)) && methodName.length() > 3) {
            // ensure that the variable starts with a lower-case letter
            fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is") && Character.isUpperCase(methodName.charAt(2)) && methodName.length() > 2) {
            // ensure that the variable starts with a lower-case letter
            fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        } else {
            // method name does not fall into getter conventions
            fieldName = null;
        }
        if (fieldName != null) {
            // method name matched getter conventions
            try {
                // check whether a matching field exists
                return method.getDeclaringClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                // that means "no" then
            }
        }
        return null;
    }

    /**
     * Determine whether the given method's name matches the getter naming convention ("getFoo()"/"isFoo()") and a respective field ("foo") exists.
     *
     * @param method targeted method
     * @return whether method name starts with "get"/"is" and rest matches name of field in declaring class (i.e. ignoring fields in super classes)
     */
    public static boolean isGetter(Method method) {
        return findFieldForGetter(method) != null;
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
            throw new UnsupportedOperationException("Unsupported type: " + javaType.getClass());
        }
        return rawType;
    }

    /**
     * Determine whether the given (generic) type represents an array or collection, which should both be of "type" "array" in a JSON schema.
     *
     * @param javaType (generic) type to check for being an array
     * @return whether the given type represents an array or collection
     */
    public static boolean isArrayType(Type javaType) {
        if (javaType instanceof GenericArrayType) {
            // ReflectionUtils.getRawType() returns null for a GenericArrayType
            return true;
        }
        Class<?> rawType = getRawType(javaType);
        return rawType.isArray() || Collection.class.isAssignableFrom(rawType);
    }

    /**
     * Determine the type of the item/component within the given array type.
     *
     * @param javaArrayType array type for which to determine the type of contained items/components
     * @return item/component type
     * @see #isArrayType(Type)
     */
    public static Type getArrayComponentType(Type javaArrayType) {
        Type componentType;
        if (javaArrayType instanceof GenericArrayType) {
            componentType = ((GenericArrayType) javaArrayType).getGenericComponentType();
        } else if (javaArrayType instanceof Class<?>) {
            componentType = ((Class<?>) javaArrayType).getComponentType();
        } else if (javaArrayType instanceof ParameterizedType) {
            componentType = ((ParameterizedType) javaArrayType).getActualTypeArguments()[0];
        } else {
            throw new UnsupportedOperationException("Cannot determine array component type for target: " + javaArrayType);
        }
        return componentType;
    }

    /**
     * Determine the actual type behind the given {@link TypeVariable} or {@link WildcardType} (as far as possible).
     * <br>
     * If the given type is not a {@link TypeVariable} or {@link WildcardType}, it will be returned as-is.
     *
     * @param targetType (possible) TypeVariable or WildcardType to resolve
     * @param typeVariables mapping of type variable names and their corresponding actual type argument
     * @return actual type behind the given target type
     */
    public static Type resolveGenericTypePlaceholder(Type targetType, Map<String, Type> typeVariables) {
        Type actualTargetType = targetType;
        while (actualTargetType instanceof TypeVariable || actualTargetType instanceof WildcardType) {
            logger.debug("attempting to resolve actual type of {}", actualTargetType);
            Type newTargetType = actualTargetType;
            if (newTargetType instanceof WildcardType) {
                newTargetType = Stream.of(((WildcardType) newTargetType).getUpperBounds()).findFirst().orElse(Object.class);
                logger.debug("looked-up upper bound of wildcard type: {}", newTargetType);
            }
            if (newTargetType instanceof TypeVariable) {
                newTargetType = typeVariables.getOrDefault(((TypeVariable) newTargetType).getName(), newTargetType);
                logger.debug("looked-up type variable by name: {}", newTargetType);
            }
            if (newTargetType == Object.class || newTargetType == actualTargetType) {
                logger.info("Skipping type variable {} as no specific type can be determined beyond {}.",
                        targetType.getTypeName(), actualTargetType.getTypeName());
                return Object.class;
            }
            actualTargetType = newTargetType;
        }
        return actualTargetType;
    }
}
