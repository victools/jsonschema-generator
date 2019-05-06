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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of type variables and their associated types (as far as they can be resolved).
 */
public final class TypeVariableContext {

    private static final Logger logger = LoggerFactory.getLogger(TypeVariableContext.class);

    /**
     * An empty context, that applies to any type not defining any generic type variables.
     */
    public static final TypeVariableContext EMPTY_SCOPE = new TypeVariableContext();

    /**
     * Factory method: collecting the type variables present in the given targetType.
     *
     * @param targetType type for which to collect type variables and their associated types
     * @param parentTypeVariables type variables present in the class where the given targetType is declared (e.g. as field)
     * @return collected type variables (may be the {@link TypeVariableContext#EMPTY_SCOPE})
     */
    public static TypeVariableContext forType(Type targetType, TypeVariableContext parentTypeVariables) {
        TypeVariableContext result;
        if (targetType instanceof ParameterizedType) {
            result = new TypeVariableContext((ParameterizedType) targetType, parentTypeVariables);
        } else if (targetType instanceof Class<?>) {
            result = new TypeVariableContext((Class<?>) targetType, parentTypeVariables);
        } else if (targetType instanceof GenericArrayType) {
            result = parentTypeVariables;
        } else {
            result = TypeVariableContext.EMPTY_SCOPE;
        }
        return result;
    }

    /**
     * Mapped type variable names to their respective values (as far as they can be resolved).
     */
    private final Map<String, Type> typeVariablesByName;

    /**
     * Constructor: only intended to be used by the {@link TypeVariableContext#EMPTY_SCOPE}.
     */
    private TypeVariableContext() {
        this.typeVariablesByName = Collections.emptyMap();
    }

    /**
     * Constructor: for a {@link ParameterizedType}.
     *
     * @param targetType type for which to collect type variables and their associated types
     * @param parentTypeVariables type variables present in the class where the given targetType is declared (e.g. as field)
     */
    private TypeVariableContext(ParameterizedType targetType, TypeVariableContext parentTypeVariables) {
        TypeVariable<?>[] genericParams = ((Class<?>) targetType.getRawType()).getTypeParameters();
        if (genericParams.length == 0) {
            this.typeVariablesByName = Collections.emptyMap();
        } else {
            this.typeVariablesByName = new HashMap<>();
            Type[] typeArguments = targetType.getActualTypeArguments();
            for (int index = 0; index < genericParams.length; index++) {
                Type typeArgument = parentTypeVariables.resolveGenericTypePlaceholder(typeArguments[index]);
                this.typeVariablesByName.put(genericParams[index].getName(), typeArgument);
            }
        }
    }

    /**
     * Constructor: for a {@link Class}.
     *
     * @param targetType type for which to collect type variables and their associated types
     * @param parentTypeVariables type variables present in the class where the given targetType is declared (e.g. as field)
     */
    private TypeVariableContext(Class<?> targetType, TypeVariableContext parentTypeVariables) {
        TypeVariable<?>[] genericParams = targetType.getTypeParameters();
        if (genericParams.length == 0) {
            this.typeVariablesByName = Collections.emptyMap();
        } else {
            this.typeVariablesByName = new HashMap<>();
            for (TypeVariable<?> singleParameter : genericParams) {
                Type[] bounds = singleParameter.getBounds();
                Type typeArgument = bounds.length == 0 ? Object.class : parentTypeVariables.resolveGenericTypePlaceholder(bounds[0]);
                this.typeVariablesByName.put(singleParameter.getName(), typeArgument);
            }
        }
    }

    /**
     * Determine the actual type behind the given {@link TypeVariable} or {@link WildcardType} (as far as possible).
     * <br>
     * If the given type is not a {@link TypeVariable} or {@link WildcardType}, it will be returned as-is.
     *
     * @param targetType (possible) TypeVariable or WildcardType to resolve
     * @return actual type behind the given target type
     */
    public Type resolveGenericTypePlaceholder(Type targetType) {
        Type actualTargetType = targetType;
        while (actualTargetType instanceof TypeVariable || actualTargetType instanceof WildcardType) {
            logger.debug("attempting to resolve actual type of {}", actualTargetType);
            Type newTargetType = actualTargetType;
            if (newTargetType instanceof WildcardType) {
                newTargetType = Stream.of(((WildcardType) newTargetType).getUpperBounds()).findFirst().orElse(Object.class);
                logger.debug("looked-up upper bound of wildcard type: {}", newTargetType);
            } else if (newTargetType instanceof TypeVariable) {
                String variableName = ((TypeVariable) newTargetType).getName();
                if (this.typeVariablesByName.containsKey(variableName)) {
                    newTargetType = this.typeVariablesByName.get(variableName);
                    logger.debug("resolved type variable \"{}\" as {}", variableName, newTargetType);
                } else {
                    throw new IllegalStateException(variableName + " is an undefined variable in this context. Maybe wrong context was applied?");
                }
            }
            if (newTargetType == actualTargetType
                    || newTargetType == Object.class
                    || newTargetType == null) {
                logger.info("Skipping type variable {} as no specific type can be determined beyond {}.",
                        targetType.getTypeName(), actualTargetType.getTypeName());
                return Object.class;
            }
            actualTargetType = newTargetType;
        }
        return actualTargetType;
    }
}
