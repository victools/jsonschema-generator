/*
 * Copyright 2020 VicTools.
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

import com.fasterxml.classmate.ResolvedType;

/**
 * Representation of a single type to represent as (sub) schema.
 */
public class TypeScope {

    private final ResolvedType type;
    private final TypeContext context;

    /**
     * Constructor.
     *
     * @param type targeted type
     * @param context the overall type resolution context
     */
    protected TypeScope(ResolvedType type, TypeContext context) {
        this.type = type;
        this.context = context;
    }

    /**
     * Getter for the overall type resolution context.
     *
     * @return type resolution context
     */
    public TypeContext getContext() {
        return this.context;
    }

    /**
     * Returns represented type; if there is one, for methods this is the return type and for fields their field type.
     *
     * @return represented type
     */
    public ResolvedType getType() {
        return this.type;
    }

    /* ===================================== *
     * Convenience methods for targeted type *
     * ===================================== */
    /**
     * Find type parameterization for the specified (super) type at return the type parameter at the given index.
     *
     * @param erasedSuperType (super) type to find declared type parameter for
     * @param parameterIndex index of the single type parameter's declared type to return
     * @return declared parameter type; or Object.class if no parameters are defined; or null if the given type or index are invalid
     * @see TypeContext#getTypeParameterFor(ResolvedType, Class, int)
     */
    public ResolvedType getTypeParameterFor(Class<?> erasedSuperType, int parameterIndex) {
        return this.context.getTypeParameterFor(this.type, erasedSuperType, parameterIndex);
    }

    /**
     * Determine whether this targeted type should be treated as {@link SchemaKeyword#TAG_TYPE_ARRAY} in the generated schema.
     * <br>
     * This is equivalent to calling: {@code scope.getContext().isContainerType(scope.getType())}
     *
     * @return whether the targeted type is an array or sub type of {@link java.util.Collection Collection}
     */
    public boolean isContainerType() {
        ResolvedType targetType = this.getType();
        return targetType != null && this.getContext().isContainerType(targetType);
    }

    /**
     * Identify the element/item type of the given {@link SchemaKeyword#TAG_TYPE_ARRAY}.
     * <br>
     * This is equivalent to calling: {@code scope.getContext().getContainerItemType(scope.getType())}
     *
     * @return type of elements/items
     */
    public ResolvedType getContainerItemType() {
        ResolvedType targetType = this.getType();
        return targetType == null ? null : this.getContext().getContainerItemType(targetType);
    }

    /**
     * Constructing a string that represents this member's type (including possible type parameters and their actual types) – excluding package names.
     * <br>
     * This is equivalent to calling: {@code scope.getContext().getSimpleTypeDescription(scope.getType())}
     *
     * @return resulting string
     * @see #getType()
     * @see TypeContext#getSimpleTypeDescription(ResolvedType)
     */
    public String getSimpleTypeDescription() {
        ResolvedType targetType = this.getType();
        return targetType == null ? "void" : this.getContext().getSimpleTypeDescription(targetType);
    }

    /**
     * Constructing a string that represents this member's type (including possible type parameters and their actual types) – including package names.
     * <br>
     * This is equivalent to calling: {@code scope.getContext().getFullTypeDescription(scope.getType())}
     *
     * @return resulting string
     * @see #getType()
     * @see TypeContext#getFullTypeDescription(ResolvedType)
     */
    public String getFullTypeDescription() {
        ResolvedType targetType = this.getType();
        return targetType == null ? "void" : this.getContext().getFullTypeDescription(targetType);
    }

    @Override
    public String toString() {
        return this.getSimpleTypeDescription();
    }
}
