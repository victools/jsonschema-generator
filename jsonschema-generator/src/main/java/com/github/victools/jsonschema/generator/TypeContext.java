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

import com.fasterxml.classmate.AnnotationConfiguration;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Context in which types can be resolved (as well as their declared fields and methods).
 */
public class TypeContext {

    private final TypeResolver typeResolver;
    private final MemberResolver memberResolver;
    private final AnnotationConfiguration annotationConfig;
    private final boolean derivingFieldsFromArgumentFreeMethods;

    /**
     * Constructor.
     *
     * @param annotationConfig annotation configuration to apply when collecting resolved fields and methods
     */
    public TypeContext(AnnotationConfiguration annotationConfig) {
        this(annotationConfig, false);
    }

    /**
     * Constructor.
     *
     * @param annotationConfig annotation configuration to apply when collecting resolved fields and methods
     * @param generatorConfig generator configuration indicating whether argument free methods should be represented as fields
     */
    public TypeContext(AnnotationConfiguration annotationConfig, SchemaGeneratorConfig generatorConfig) {
        this(annotationConfig, generatorConfig.shouldDeriveFieldsFromArgumentFreeMethods());
    }

    /**
     * Constructor.
     *
     * @param annotationConfig annotation configuration to apply when collecting resolved fields and methods
     * @param derivingFieldsFromArgumentFreeMethods whether argument free methods should be represented as fields
     */
    private TypeContext(AnnotationConfiguration annotationConfig, boolean derivingFieldsFromArgumentFreeMethods) {
        this.typeResolver = new TypeResolver();
        this.memberResolver = new MemberResolver(this.typeResolver);
        this.annotationConfig = annotationConfig;
        this.derivingFieldsFromArgumentFreeMethods = derivingFieldsFromArgumentFreeMethods;
    }

    /**
     * Getter for the flag indicating whether to derive fields from argument-free methods.
     *
     * @return whether argument-free methods should be represented as fields
     */
    public boolean isDerivingFieldsFromArgumentFreeMethods() {
        return this.derivingFieldsFromArgumentFreeMethods;
    }

    /**
     * Resolve actual type (mostly relevant for parameterised types, type variables and such.
     *
     * @param type java type to resolve
     * @param typeParameters (optional) type parameters to pass on
     * @return resolved type
     * @see TypeResolver#resolve(Type, Type...)
     */
    public final ResolvedType resolve(Type type, Type... typeParameters) {
        return this.typeResolver.resolve(type, typeParameters);
    }

    /**
     * Resolve subtype considering the given super-types (potentially) known type parameters.
     *
     * @param supertype already resolved super type
     * @param subtype erased java subtype to resolve
     * @return resolved subtype
     * @see TypeResolver#resolveSubtype(ResolvedType, Class)
     */
    public final ResolvedType resolveSubtype(ResolvedType supertype, Class<?> subtype) {
        return this.typeResolver.resolveSubtype(supertype, subtype);
    }

    /**
     * Collect a given type's declared fields and methods.
     *
     * @param resolvedType type for which to collect declared fields and methods
     * @return collection of (resolved) fields and methods
     */
    public final ResolvedTypeWithMembers resolveWithMembers(ResolvedType resolvedType) {
        return this.memberResolver.resolve(resolvedType, this.annotationConfig, null);
    }

    /**
     * Construct a {@link FieldScope} instance for the given field.
     *
     * @param field targeted field
     * @param declaringTypeMembers collection of the declaring type's (other) fields and methods
     * @return created {@link FieldScope} instance
     */
    public FieldScope createFieldScope(ResolvedField field, ResolvedTypeWithMembers declaringTypeMembers) {
        return new FieldScope(field, declaringTypeMembers, this);
    }

    /**
     * Construct a {@link MethodScope} instance for the given method.
     *
     * @param method targeted method
     * @param declaringTypeMembers collection of the declaring type's fields and (other) methods
     * @return created {@link MethodScope} instance
     */
    public MethodScope createMethodScope(ResolvedMethod method, ResolvedTypeWithMembers declaringTypeMembers) {
        return new MethodScope(method, declaringTypeMembers, this);
    }

    /**
     * Construct a {@link TypeScope} instance for the type.
     *
     * @param type targeted type
     * @return created {@link TypeScope} instance
     */
    public TypeScope createTypeScope(ResolvedType type) {
        return new TypeScope(type, this);
    }

    /**
     * Find type parameterization for the specified (super) type at return the type parameter at the given index.
     *
     * @param type type to find type parameter for
     * @param erasedSuperType (super) type to find declared type parameter for
     * @param parameterIndex index of the single type parameter's declared type to return
     * @return declared parameter type; or Object.class if no parameters are defined; or null if the given type or index are invalid
     * @see ResolvedType#typeParametersFor(Class)
     */
    public ResolvedType getTypeParameterFor(ResolvedType type, Class<?> erasedSuperType, int parameterIndex) {
        List<ResolvedType> typeParameters = type.typeParametersFor(erasedSuperType);
        if (typeParameters == null
                || (!typeParameters.isEmpty() && typeParameters.size() <= parameterIndex)
                || (typeParameters.isEmpty() && erasedSuperType.getTypeParameters().length <= parameterIndex)) {
            // given type is not a super type of the type in scope or given index is out of bounds
            return null;
        }
        if (typeParameters.isEmpty()) {
            // no type parameters are defined, for simplicity's sake not trying to resolve declared boundaries
            return this.resolve(Object.class);
        }
        return typeParameters.get(parameterIndex);
    }

    /**
     * Determine whether a given type should be treated as {@link SchemaKeyword#TAG_TYPE_ARRAY} in the generated schema.
     *
     * @param type type to check
     * @return whether the given type is an array or sub type of {@link Collection}
     */
    public boolean isContainerType(ResolvedType type) {
        return type.isArray() || type.isInstanceOf(Collection.class);
    }

    /**
     * Identify the element/item type of the given {@link SchemaKeyword#TAG_TYPE_ARRAY}.
     *
     * @param containerType type to extract type of element/item from
     * @return type of elements/items
     * @see #isContainerType(ResolvedType)
     */
    public ResolvedType getContainerItemType(ResolvedType containerType) {
        ResolvedType itemType = containerType.getArrayElementType();
        if (itemType == null && this.isContainerType(containerType)) {
            itemType = this.getTypeParameterFor(containerType, Iterable.class, 0);
        }
        return itemType;
    }

    /**
     * Constructing a string that represents the given type (including possible type parameters and their actual types).
     * <br>
     * This calls {@link Class#getSimpleName()} for a single erased type – i.e. excluding package names.
     *
     * @param type the type to represent
     * @return resulting string
     */
    public final String getSimpleTypeDescription(ResolvedType type) {
        return this.getTypeDescription(type, true);
    }

    /**
     * Constructing a string that fully represents the given type (including possible type parameters and their actual types).
     * <br>
     * This calls {@link Class#getName()} for a single erased type.
     *
     * @param type the type to represent
     * @return resulting string
     */
    public final String getFullTypeDescription(ResolvedType type) {
        return this.getTypeDescription(type, false);
    }

    /**
     * Constructing a string that fully represents the given type (including possible type parameters and their actual types).
     *
     * @param type the type to represent
     * @param simpleClassNames whether simple class names should be used (otherwise: full package names are included)
     * @return resulting string
     */
    private String getTypeDescription(ResolvedType type, boolean simpleClassNames) {
        Class<?> erasedType = type.getErasedType();
        String result = simpleClassNames ? erasedType.getSimpleName() : erasedType.getTypeName();
        List<ResolvedType> typeParameters = type.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            result += typeParameters.stream()
                    .map(parameterType -> this.getTypeDescription(parameterType, simpleClassNames))
                    .collect(Collectors.joining(", ", "<", ">"));
        }
        return result;
    }

    /**
     * Returns the type description for an argument in a method's property name.
     *
     * @param type method argument's type to represent
     * @return argument type description
     */
    public String getMethodPropertyArgumentTypeDescription(ResolvedType type) {
        return this.getSimpleTypeDescription(type);
    }
}
