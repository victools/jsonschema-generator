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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.classmate.members.ResolvedMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Optional;

/**
 * Representation of a single introspected field or method.
 *
 * @param <M> type of member in scope (i.e. {@link ResolvedField} or {@link ResolvedMethod}).
 * @param <T> type of java/reflection member in scope (i.e. {@link java.lang.reflect.Field FIeld} or {@link java.lang.reflect.Method Nethod}
 */
public abstract class MemberScope<M extends ResolvedMember<T>, T extends Member> extends TypeScope {

    private final M member;
    private final ResolvedType overriddenType;
    private final String overriddenName;
    private final ResolvedTypeWithMembers declaringTypeMembers;
    private final TypeContext context;

    /**
     * Constructor.
     *
     * @param member targeted field or method
     * @param overriddenType alternative type for this field or method's return value
     * @param overriddenName alternative name for this field or method
     * @param declaringTypeMembers collection of the declaring type's (other) fields and methods
     * @param context the overall type resolution context
     */
    protected MemberScope(M member, ResolvedType overriddenType, String overriddenName,
            ResolvedTypeWithMembers declaringTypeMembers, TypeContext context) {
        super(Optional.ofNullable(overriddenType).orElseGet(member::getType), context);
        this.member = member;
        this.overriddenType = overriddenType;
        this.overriddenName = overriddenName;
        this.declaringTypeMembers = declaringTypeMembers;
        this.context = context;
    }

    /**
     * Create another instance for this field or method and context, but overriding the declared field/method return type with the given one.
     *
     * @param overriddenType alternative type for this field or method return value (overriding the declared type)
     * @return new instance with the given type override
     * @see #getDeclaredType()
     * @see #getOverriddenType()
     */
    public abstract MemberScope<M, T> withOverriddenType(ResolvedType overriddenType);

    /**
     * Create another instance for this field or method and context, but overriding the declared field/method name with the given one.
     *
     * @param overriddenName alternative name for this field or method
     * @return new instance with the given name override
     * @see #getDeclaredType()
     * @see #getOverriddenType()
     */
    public abstract MemberScope<M, T> withOverriddenName(String overriddenName);

    /**
     * Getter for the represented field or method.
     *
     * @return represented field or method
     */
    public M getMember() {
        return this.member;
    }

    /**
     * Getter for the collection of the member's declaring type's (other) fields and methods.
     *
     * @return declaring type's fields and methods
     */
    public ResolvedTypeWithMembers getDeclaringTypeMembers() {
        return this.declaringTypeMembers;
    }

    /**
     * Returns the type declared as the field's or method return value's type.
     *
     * @return declared type
     * @see #getType()
     */
    public ResolvedType getDeclaredType() {
        return this.member.getType();
    }

    /**
     * Returns the overridden type of the field or method's return value.
     *
     * @return overridden type (or {@code null} if no override applies)
     * @see #getType()
     */
    public ResolvedType getOverriddenType() {
        return this.overriddenType;
    }

    /**
     * Returns the member's name as specified in the declaring class.
     *
     * @return declared method/field name
     * @see #getName()
     */
    public String getDeclaredName() {
        return this.member.getName();
    }

    /**
     * Returns the member's overridden name.
     *
     * @return overridden name (or {@code null} if no override applies)
     * @see #getName()
     */
    public String getOverriddenName() {
        return this.overriddenName;
    }

    /**
     * Returns name of this member.
     *
     * @return method/field name
     * @see #getDeclaredName()
     * @see #getOverriddenName()
     */
    public String getName() {
        return Optional.ofNullable(this.getOverriddenName())
                .orElseGet(this::getDeclaredName);
    }

    /* ===================================== *
     * Delegators to wrapped resolved member *
     * ===================================== */
    /**
     * Returns the member's declaring type.
     *
     * @return declaring type
     */
    public final ResolvedType getDeclaringType() {
        return this.member.getDeclaringType();
    }

    /**
     * Returns the JDK object that represents member.
     *
     * @return raw member
     */
    public T getRawMember() {
        return this.member.getRawMember();
    }

    /**
     * Indicates whether the member has the {@code static} keyword.
     *
     * @return whether member is static
     */
    public boolean isStatic() {
        return this.member.isStatic();
    }

    /**
     * Indicates whether the member has the {@code final} keyword.
     *
     * @return whether member is final
     */
    public boolean isFinal() {
        return this.member.isFinal();
    }

    /**
     * Indicates whether the member is of {@code private} visibility.
     *
     * @return whether member is private
     */
    public boolean isPrivate() {
        return this.member.isPrivate();
    }

    /**
     * Indicates whether the member is of {@code protected} visibility.
     *
     * @return whether member is protected
     */
    public boolean isProtected() {
        return this.member.isProtected();
    }

    /**
     * Indicates whether the member is of {@code public} visibility.
     *
     * @return whether member is public
     */
    public boolean isPublic() {
        return this.member.isPublic();
    }

    /**
     * Return the annotation of the given type on the member, if such an annotation is present.
     *
     * @param <A> type of annotation to look-up
     * @param annotationClass annotation class to look up instance on member for
     * @return annotation instance (or {@code null} if no annotation of the given type is present
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return this.member.get(annotationClass);
    }

    /**
     * Return the annotation of the given type on the member, if such an annotation is present on either the field or its getter.
     *
     * @param <A> type of annotation
     * @param annotationClass type of annotation
     * @return annotation instance (or {@code null} if no annotation of the given type is present)
     */
    public abstract <A extends Annotation> A getAnnotationConsideringFieldAndGetter(Class<A> annotationClass);

    /**
     * Returns the name to be used to reference this member in its parent's "properties".
     *
     * @return member's name in parent "properties"
     */
    public abstract String getSchemaPropertyName();

    @Override
    public String toString() {
        return this.getSimpleTypeDescription() + " " + this.getSchemaPropertyName();
    }
}
