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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
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
     * @return collected type variables (may be the {@link TypeVariableContext#EMPTY_SCOPE})
     */
    public static TypeVariableContext forType(JavaType targetType) {
        return TypeVariableContext.forType(targetType.getResolvedType(), targetType.getParentTypeVariables());
    }

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
            ParameterizedType parameterizedTargetType = (ParameterizedType) targetType;
            TypeVariable<?>[] genericParams = ((Class<?>) parameterizedTargetType.getRawType()).getTypeParameters();
            Map<String, JavaType> typeVariables = new HashMap<>();
            Type[] typeArguments = parameterizedTargetType.getActualTypeArguments();
            for (int index = 0; index < genericParams.length; index++) {
                typeVariables.put(genericParams[index].getName(),
                        new JavaType(typeArguments[index], parentTypeVariables));
            }
            result = new TypeVariableContext(typeVariables, parentTypeVariables);
        } else if (targetType instanceof GenericArrayType) {
            result = parentTypeVariables;
        } else {
            result = TypeVariableContext.EMPTY_SCOPE;
        }
        return result;
    }

    /**
     * Factory method: collecting any additional type variables present on a given method and augmenting the provided context accordingly.
     *
     * @param targetMethod method for which to collect type variables and their associated types
     * @param parentTypeVariables type variables present in the class where the given method is declared
     * @return collected type variables (may be the {@link TypeVariableContext#EMPTY_SCOPE})
     */
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
    private final Map<String, JavaType> typeVariablesByName;
    private final TypeVariableContext parentContext;

    /**
     * Constructor: only intended to be used by the {@link TypeVariableContext#EMPTY_SCOPE}.
     */
    private TypeVariableContext() {
        this(Collections.emptyMap(), null);
    }

    /**
     * Constructor simply setting the given parameters to the corresponding private fields.
     *
     * @param typeVariablesByName mapping of type variables to their declared types
     * @param parentContext type variables declared in the next higher level (e.g. sub-class)
     */
    private TypeVariableContext(Map<String, JavaType> typeVariablesByName, TypeVariableContext parentContext) {
        this.typeVariablesByName = typeVariablesByName;
        this.parentContext = parentContext;
    }

    /**
     * Constructor: for a {@link Class} or {@link Method}.
     *
     * @param genericParams additional type parameters to add to given context
     * @param baseTypeVariables declaring type's context to augment with additional type parameters
     */
    private TypeVariableContext(TypeVariable<?>[] genericParams, TypeVariableContext baseTypeVariables) {
        this.typeVariablesByName = new HashMap<>(baseTypeVariables.typeVariablesByName);
        for (TypeVariable<?> singleParameter : genericParams) {
            Type typeArgument = Stream.of(singleParameter.getBounds())
                    .findFirst().orElse(Object.class);
            this.typeVariablesByName.put(singleParameter.getName(), new JavaType(typeArgument, this));
        }
        this.parentContext = baseTypeVariables.parentContext;
    }

    /**
     * Determine the actual type behind the given {@link TypeVariable} or {@link WildcardType} (as far as possible).
     * <br>
     * If the given type is not a {@link TypeVariable} or {@link WildcardType}, it will be returned as-is.
     *
     * @param targetType (possible) TypeVariable or WildcardType to resolve
     * @return actual type behind the given target type
     */
    public JavaType resolveGenericTypePlaceholder(Type targetType) {
        Type actualTargetType = targetType;
        while (actualTargetType instanceof WildcardType) {
            logger.debug("resolving wildcard type's upper bound ({})", actualTargetType);
            actualTargetType = Stream.of(((WildcardType) actualTargetType).getUpperBounds())
                    .findFirst().orElse(Object.class);
        }
        JavaType result;
        if (actualTargetType instanceof TypeVariable) {
            result = this.resolveTypeVariable((TypeVariable<?>) actualTargetType);
        } else {
            // nothing to (further) look-up
            result = new JavaType(actualTargetType, this);
        }
        return result;
    }

    /**
     * Determine the actual type behind the given type variable.
     *
     * @param typeVariable type variable to resolve
     * @return actual type behind the given type variable
     */
    private JavaType resolveTypeVariable(TypeVariable<?> typeVariable) {
        String variableName = typeVariable.getName();
        logger.debug("attempting to resolve type variable {}", variableName);
        if (this.typeVariablesByName.containsKey(variableName)) {
            JavaType resolvedType = this.typeVariablesByName.get(variableName);
            // exclude the variable having been looked up
            TypeVariableContext resolvedContext = resolvedType.getParentTypeVariables();
            TypeVariableContext nextContext;
            if (resolvedContext == this) {
                // resolved type is in the same context, since self-references are not allowed: ignore the visited type variable going forward
                Map<String, JavaType> typeVariables = new HashMap<>(resolvedContext.typeVariablesByName);
                typeVariables.remove(variableName);
                nextContext = new TypeVariableContext(typeVariables, resolvedContext.parentContext);
            } else {
                nextContext = resolvedContext;
            }
            return nextContext.resolveGenericTypePlaceholder(resolvedType.getDeclaredType());
        }
        if (this.parentContext != null) {
            logger.debug("looking-up type variable {} in parent context", variableName);
            return this.parentContext.resolveGenericTypePlaceholder(typeVariable);
        }
        throw new IllegalStateException(variableName + "is an undefined variable in this context. Maybe wrong context was applied?");
    }
}
