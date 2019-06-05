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
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.RawField;
import com.fasterxml.classmate.members.RawMethod;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.classmate.members.ResolvedMethod;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.stream.Stream;

/**
 *
 */
public abstract class TypeContext {

    private final TypeResolver typeResolver;
    private final MemberResolver memberResolver;
    private final AnnotationConfiguration annotationConfig;
    private final SchemaGeneratorConfig generatorConfig;

    protected TypeContext(TypeResolver typeResolver, AnnotationConfiguration annotationConfig, SchemaGeneratorConfig generatorConfig) {
        this.typeResolver = typeResolver;
        this.memberResolver = new MemberResolver(typeResolver);
        this.annotationConfig = annotationConfig;
        this.generatorConfig = generatorConfig;
    }

    public SchemaGeneratorConfig getGeneratorConfig() {
        return this.generatorConfig;
    }

    public ResolvedType resolve(Type type) {
        return this.typeResolver.resolve(type);
    }

    public ResolvedType resolve(Type jdkType, TypeBindings typeBindings) {
        return this.typeResolver.resolve(typeBindings, jdkType);
    }

    public ResolvedTypeWithMembers resolveWithMembers(ResolvedType mainType) {
        return this.memberResolver.resolve(mainType, this.annotationConfig, null);
    }

    public ResolvedTypeWithMembers resolveWithMembers(Type javaType) {
        ResolvedType resolvedType = this.resolve(javaType);
        return this.resolveWithMembers(resolvedType);
    }

    public ResolvedTypeWithMembers resolveWithMembers(Type javaType, TypeBindings bindings) {
        ResolvedType resolvedType = this.resolve(javaType, bindings);
        return this.resolveWithMembers(resolvedType);
    }

    public ResolvedField resolveMember(RawField rawField) {
        return this.resolveMember(rawField, this.resolveWithMembers(rawField.getDeclaringType()));
    }

    public ResolvedField resolveMember(RawField rawField, ResolvedTypeWithMembers declaringType) {
        ResolvedField[] resolvedFields = rawField.isStatic() ? declaringType.getStaticFields() : declaringType.getMemberFields();
        ResolvedField result = this.findMember(resolvedFields, rawField.getRawMember());
        return result;
    }

    public ResolvedMethod resolveMember(RawMethod rawMethod) {
        return this.resolveMember(rawMethod, this.resolveWithMembers(rawMethod.getDeclaringType()));
    }

    public ResolvedMethod resolveMember(RawMethod rawMethod, ResolvedTypeWithMembers declaringType) {
        ResolvedMethod[] resolvedMethods = rawMethod.isStatic() ? declaringType.getStaticMethods() : declaringType.getMemberMethods();
        ResolvedMethod result = this.findMember(resolvedMethods, rawMethod.getRawMember());
        return result;
    }

    /**
     * Select the single matching resolved member from the given array that matches the specified raw member.
     *
     * @param <R> type of resolved member (i.e. field or method)
     * @param <J> type of raw member
     * @param resolvedMembers array of resolved members from which to select
     * @param javaMember unresolved/raw member to look-up
     * @return matching resolved member
     */
    private <R extends ResolvedMember<? extends J>, J extends Member> R findMember(R[] resolvedMembers, J javaMember) {
        return Stream.of(resolvedMembers)
                .filter(resolvedMethod -> resolvedMethod.getRawMember().equals(javaMember))
                .findAny()
                .orElse(null);
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
        if (itemType == null) {
            itemType = containerType.typeParametersFor(Collection.class).get(0);
        }
        return itemType;
    }
}
