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

import com.github.victools.jsonschema.generator.TypePlaceholderResolver;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of type variables and their associated types (as far as they can be resolved).
 */
public final class TypeVariableContext implements TypePlaceholderResolver {

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
        if (targetType instanceof Class<?> && ((Class<?>) targetType).getTypeParameters().length > 0) {
            TypeVariable<?>[] genericParams = ((Class<?>) targetType).getTypeParameters();
            result = new TypeVariableContext(genericParams, TypeVariableContext.EMPTY_SCOPE);
        } else if (targetType instanceof ParameterizedType) {
            result = new TypeVariableContext((ParameterizedType) targetType, parentTypeVariables);
        } else if (targetType instanceof GenericArrayType) {
            result = parentTypeVariables;
        } else {
            result = TypeVariableContext.EMPTY_SCOPE;
        }
        return result;
    }

    public static TypeVariableContext forMethod(Method targetMethod, TypeVariableContext parentTypeVariables) {
        TypeVariableContext result;
        TypeVariable<?>[] genericParams = targetMethod.getTypeParameters();
        if (genericParams.length == 0) {
            result = parentTypeVariables;
        } else {
            result = new TypeVariableContext(genericParams, parentTypeVariables);
        }
        return result;
    }

    /**
     * Mapped type variable names to their respective values (as far as they can be resolved).
     */
    private final Map<String, Type> typeVariablesByName;
    private final TypeVariableContext parentContext;

    /**
     * Constructor: only intended to be used by the {@link TypeVariableContext#EMPTY_SCOPE}.
     */
    private TypeVariableContext() {
        this.typeVariablesByName = Collections.emptyMap();
        this.parentContext = null;
    }

    /**
     * Constructor: for a {@link Class}.
     *
     * @param targetType type for which to collect type variables and their associated types
     */
    private TypeVariableContext(TypeVariable<?>[] genericParams, TypeVariableContext baseTypeVariables) {
        this.typeVariablesByName = new HashMap<>(baseTypeVariables.typeVariablesByName);
        for (TypeVariable<?> singleParameter : genericParams) {
            Type[] bounds = singleParameter.getBounds();
            Type typeArgument = Stream.of(bounds).findFirst().orElse(Object.class);
            this.typeVariablesByName.put(singleParameter.getName(), typeArgument);
        }
        this.parentContext = baseTypeVariables.parentContext;
    }

    /**
     * Constructor: for a {@link ParameterizedType}.
     *
     * @param targetType type for which to collect type variables and their associated types
     * @param parentTypeVariables type variables present in the class where the given targetType is declared (e.g. as field)
     */
    private TypeVariableContext(ParameterizedType targetType, TypeVariableContext parentTypeVariables) {
        TypeVariable<?>[] genericParams = ((Class<?>) targetType.getRawType()).getTypeParameters();
        this.typeVariablesByName = new HashMap<>();
        Type[] typeArguments = targetType.getActualTypeArguments();
        for (int index = 0; index < genericParams.length; index++) {
            Type typeArgument = parentTypeVariables.resolveGenericTypePlaceholder(typeArguments[index]);
            this.typeVariablesByName.put(genericParams[index].getName(), typeArgument);
        }
        this.parentContext = parentTypeVariables;
    }

    @Override
    public Type resolveGenericTypePlaceholder(Type targetType) {
        Type actualTargetType = targetType;
        while (actualTargetType instanceof TypeVariable || actualTargetType instanceof WildcardType) {
            logger.debug("attempting to resolve actual type of {}", actualTargetType);
            Type newTargetType;
            if (actualTargetType instanceof WildcardType) {
                newTargetType = Stream.of(((WildcardType) actualTargetType).getUpperBounds()).findFirst().orElse(null);
                logger.debug("looked-up upper bound of wildcard type: {}", newTargetType);
            } else if (actualTargetType instanceof TypeVariable) {
                String variableName = ((TypeVariable) actualTargetType).getName();
                if (this.typeVariablesByName.containsKey(variableName)) {
                    newTargetType = this.typeVariablesByName.get(variableName);
                    logger.debug("resolved type variable \"{}\" as {}", variableName, newTargetType);
                } else if (this.parentContext != null) {
                    newTargetType = this.parentContext.resolveGenericTypePlaceholder(actualTargetType);
                } else {
                    throw new IllegalStateException(variableName + " is an undefined variable in this " + this.toString()
                            + ". Maybe wrong context was applied?");
                }
            } else {
                newTargetType = null;
            }
            if (newTargetType == null || newTargetType == actualTargetType) {
                logger.info("Skipping type variable {} as no specific type can be determined beyond {}.",
                        targetType.getTypeName(), actualTargetType.getTypeName());
                return Object.class;
            }
            actualTargetType = newTargetType;
        }
        return actualTargetType;
    }

    @Override
    public String toString() {
        return this.typeVariablesByName.entrySet().stream()
                .map(entry -> entry.getKey() + " > " + entry.getValue())
                .collect(Collectors.joining(",\n\t", "TypeVariableContext(\n\t", "\n)"));
    }
}
