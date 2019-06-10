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

    /**
     * Constructor.
     *
     * @param annotationConfig annotation configuration to apply when collecting resolved fields and methods
     */
    public TypeContext(AnnotationConfiguration annotationConfig) {
        this.typeResolver = new TypeResolver();
        this.memberResolver = new MemberResolver(this.typeResolver);
        this.annotationConfig = annotationConfig;
    }

    /**
     * Resolve actual type (mostly relevant for parameterised types, type variables and such.
     *
     * @param type java type to resolve
     * @return resolved type
     */
    public final ResolvedType resolve(Type type) {
        return this.typeResolver.resolve(type);
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
     * Determine whether a given type should be treated as "{@value SchemaConstants#TAG_TYPE_ARRAY}" in the generated schema.
     *
     * @param type type to check
     * @return whether the given type is an array or sub type of {@link Collection}
     */
    public boolean isContainerType(ResolvedType type) {
        return type.isArray() || type.isInstanceOf(Collection.class);
    }

    /**
     * Identify the element/item type of the given "{@value SchemaConstants#TAG_TYPE_ARRAY}".
     *
     * @param containerType type to extract type of element/item from
     * @return type of elements/items
     * @see #isContainerType(ResolvedType)
     */
    public ResolvedType getContainerItemType(ResolvedType containerType) {
        ResolvedType itemType = containerType.getArrayElementType();
        if (itemType == null && this.isContainerType(containerType)) {
            itemType = containerType.typeParametersFor(Collection.class).get(0);
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
        String result = simpleClassNames ? erasedType.getSimpleName() : erasedType.getName();
        List<ResolvedType> typeParameters = type.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            result += typeParameters.stream()
                    .map(parameterType -> this.getTypeDescription(parameterType, simpleClassNames))
                    .collect(Collectors.joining(", ", "<", ">"));
        }
        return result;
    }

    /**
     * Returns the name to be associated with an entry in the generated schema's list of "definitions".
     * <br>
     * Beware: if multiple types have the same name, only one of them will be included in the schema's "definitions"
     *
     * @param type the type to be represented in the generated schema's "definitions"
     * @return name in "definitions"
     */
    public String getSchemaDefinitionName(ResolvedType type) {
        // known limitation: if two types have the same name and the only difference is their package, the resulting schema will only contain one
        // using getFullTypeDescription() instead would avoid that issue, but the resulting schema does not look as nice
        return this.getSimpleTypeDescription(type);
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
