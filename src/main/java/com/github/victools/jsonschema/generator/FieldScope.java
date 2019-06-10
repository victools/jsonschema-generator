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
import com.fasterxml.classmate.members.ResolvedMethod;
import java.lang.reflect.Field;
import java.util.stream.Stream;

/**
 * Representation of a single introspected field.
 */
public class FieldScope extends MemberScope<ResolvedField, Field> {

    /**
     * Constructor.
     *
     * @param field targeted field
     * @param declaringTypeMembers collection of the declaring type's (other) fields and methods
     * @param context the overall type resolution context
     */
    protected FieldScope(ResolvedField field, ResolvedTypeWithMembers declaringTypeMembers, TypeContext context) {
        this(field, null, null, declaringTypeMembers, context);
    }

    /**
     * Constructor.
     *
     * @param field targeted field
     * @param overriddenType alternative type for this field
     * @param overriddenName alternative name for this field
     * @param declaringTypeMembers collection of the declaring type's (other) fields and methods
     * @param context the overall type resolution context
     */
    protected FieldScope(ResolvedField field, ResolvedType overriddenType, String overriddenName,
            ResolvedTypeWithMembers declaringTypeMembers, TypeContext context) {
        super(field, overriddenType, overriddenName, declaringTypeMembers, context);
    }

    @Override
    public FieldScope withOverriddenType(ResolvedType overriddenType) {
        return new FieldScope(this.getMember(), overriddenType, this.getOverriddenName(), this.getDeclaringTypeMembers(), this.getContext());
    }

    @Override
    public FieldScope withOverriddenName(String overriddenName) {
        return new FieldScope(this.getMember(), this.getOverriddenType(), overriddenName, this.getDeclaringTypeMembers(), this.getContext());
    }

    /**
     * Returns the name to be used to reference this field in its parent's "properties".
     *
     * @return the (potentially overridden) name of this field
     * @see #getName()
     */
    @Override
    public String getSchemaPropertyName() {
        return this.getName();
    }

    /**
     * Indicates whether the field has the {@code transient} keyword.
     *
     * @return whether field is transient
     */
    public boolean isTransient() {
        return this.getMember().isTransient();
    }

    /**
     * Return the conventional getter method (if one exists). E.g. for a field named "foo", look-up either "getFoo()" or "isFoo()".
     *
     * @return public getter from within the field's declaring class
     */
    public MethodScope findGetter() {
        String capitalisedFieldName = this.getName().substring(0, 1).toUpperCase() + this.getName().substring(1);
        String getterName1 = "get" + capitalisedFieldName;
        String getterName2 = "is" + capitalisedFieldName;
        ResolvedMethod[] methods = this.getDeclaringTypeMembers().getMemberMethods();
        MethodScope getter = Stream.of(methods)
                .filter(method -> method.getRawMember().getParameterCount() == 0)
                .filter(ResolvedMethod::isPublic)
                .filter(method -> method.getName().equals(getterName1) || method.getName().equals(getterName2))
                .findFirst()
                .map(method -> this.getContext().createMethodScope(method, this.getDeclaringTypeMembers()))
                .orElse(null);
        return getter;
    }

    /**
     * Determine whether the field's declaring class contains a matching method starting with "get" or "is".
     *
     * @return whether a matching getter exists in the field's declaring class
     * @see #findGetter()
     */
    public boolean hasGetter() {
        return this.findGetter() != null;
    }
}
