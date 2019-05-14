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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reference to a (possible generic) java type and the type variables in the declaring type's context.
 */
public class JavaType {

    private final Type declaredType;
    private volatile Type resolvedType;
    private final TypeVariableContext parentTypeVariables;

    /**
     * Constructor including the actual (possibly generic) java type and the type variables available in its parent's context.
     *
     * @param declaredType (possibly generic) java type being represented
     * @param typeVariables resolver for type variables in the specific (i.e. declaring type's) context
     */
    public JavaType(Type declaredType, TypeVariableContext typeVariables) {
        this.declaredType = declaredType;
        this.resolvedType = null;
        this.parentTypeVariables = typeVariables;
    }

    /**
     * Getter for the represented java type (as declared).
     *
     * @return (possibly generic) java type being represented
     */
    Type getDeclaredType() {
        return this.declaredType;
    }

    /**
     * Getter for the resolved java type.
     *
     * @return actual java type being represented
     */
    public Type getResolvedType() {
        if (this.resolvedType == null) {
            this.resolvedType = this.parentTypeVariables.resolveGenericTypePlaceholder(this.declaredType).declaredType;
        }
        return this.resolvedType;
    }

    /**
     * Getter for the available type variables.
     *
     * @return resolver for type variables in the specific (i.e. declaring type's) context
     */
    public TypeVariableContext getParentTypeVariables() {
        return this.parentTypeVariables;
    }

    /**
     * Constructing a string that fully represents this type (including possible type parameters and the actual types).
     *
     * @param fullClassNames whether to include full package names in front of the respective class names
     * @return resulting string
     */
    private String convertTypeToString(boolean fullClassNames) {
        String result;
        Type targetType = this.getResolvedType();
        if (targetType instanceof Class<?>) {
            result = fullClassNames ? targetType.getTypeName() : ((Class<?>) targetType).getSimpleName();
        } else if (targetType instanceof ParameterizedType) {
            Class<?> rawType = (Class<?>) ((ParameterizedType) targetType).getRawType();
            result = fullClassNames ? rawType.getTypeName() : rawType.getSimpleName();
            result += Stream.of(((ParameterizedType) targetType).getActualTypeArguments())
                    .map(this.parentTypeVariables::resolveGenericTypePlaceholder)
                    .map(typeArgument -> typeArgument.convertTypeToString(fullClassNames))
                    .collect(Collectors.joining(", ", "<", ">"));
        } else {
            result = this.declaredType.getTypeName();
        }
        return result;
    }

    @Override
    public String toString() {
        return this.convertTypeToString(false);
    }

    @Override
    public int hashCode() {
        return this.getResolvedType().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JavaType)) {
            return false;
        }
        JavaType otherJavaType = (JavaType) other;
        return this.convertTypeToString(true).equals(otherJavaType.convertTypeToString(true));
    }
}
