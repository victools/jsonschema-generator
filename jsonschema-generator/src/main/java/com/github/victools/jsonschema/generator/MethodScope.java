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
import com.fasterxml.classmate.members.ResolvedMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Representation of a single introspected method.
 */
public class MethodScope extends MemberScope<ResolvedMethod, Method> {

    /**
     * Constructor.
     *
     * @param method targeted method
     * @param declaringTypeMembers collection of the declaring type's fields and (other) methods
     * @param context the overall type resolution context
     */
    protected MethodScope(ResolvedMethod method, ResolvedTypeWithMembers declaringTypeMembers, TypeContext context) {
        this(method, null, null, declaringTypeMembers, context);
    }

    /**
     * Constructor.
     *
     * @param method targeted method
     * @param overriddenType alternative type for this method's return value
     * @param overriddenName alternative name for this method
     * @param declaringTypeMembers collection of the declaring type's fields and (other) methods
     * @param context the overall type resolution context
     */
    protected MethodScope(ResolvedMethod method, ResolvedType overriddenType, String overriddenName,
            ResolvedTypeWithMembers declaringTypeMembers, TypeContext context) {
        super(method, overriddenType, overriddenName, declaringTypeMembers, context);
    }

    @Override
    public MethodScope withOverriddenType(ResolvedType overriddenType) {
        return new MethodScope(this.getMember(), overriddenType, this.getOverriddenName(), this.getDeclaringTypeMembers(), this.getContext());
    }

    @Override
    public MethodScope withOverriddenName(String overriddenName) {
        return new MethodScope(this.getMember(), this.getOverriddenType(), overriddenName, this.getDeclaringTypeMembers(), this.getContext());
    }

    /**
     * Indicating whether the method is declared as {@code void}, i.e. has no return value.
     *
     * @return whether method has no return value
     */
    public boolean isVoid() {
        return this.getType() == null;
    }

    /**
     * Returns the number of arguments this method has.
     *
     * @return number of arguments
     */
    public int getArgumentCount() {
        return this.getMember().getArgumentCount();
    }

    /**
     * Returns the list of types of this method's arguments.
     *
     * @return argument types
     */
    public List<ResolvedType> getArgumentTypes() {
        return IntStream.range(0, this.getArgumentCount())
                .mapToObj(this.getMember()::getArgumentType)
                .collect(Collectors.toList());
    }

    /**
     * Look-up the field associated with this method if it is deemed to be a getter by convention.
     *
     * @return associated field
     */
    public FieldScope findGetterField() {
        if (this.getType() == null || !this.isPublic() || this.getArgumentCount() > 0) {
            // void and non-public methods or those with arguments are not deemed to be getters
            return null;
        }
        String methodName = this.getName();
        String fieldName;
        if (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
            // ensure that the variable starts with a lower-case letter
            fieldName = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is") && methodName.length() > 2 && Character.isUpperCase(methodName.charAt(2))) {
            // ensure that the variable starts with a lower-case letter
            fieldName = methodName.substring(2, 3).toLowerCase() + methodName.substring(3);
        } else {
            // method name does not fall into getter conventions
            fieldName = null;
        }
        if (fieldName == null) {
            return null;
        }
        // method name matched getter conventions
        // check whether a matching field exists
        return Stream.of(this.getDeclaringTypeMembers().getMemberFields())
                .filter(memberField -> memberField.getName().equals(fieldName))
                .findFirst()
                .map(field -> this.getContext().createFieldScope(field, this.getDeclaringTypeMembers()))
                .orElse(null);
    }

    /**
     * Determine whether the method's name matches the getter naming convention ("getFoo()"/"isFoo()") and a respective field ("foo") exists.
     *
     * @return whether method name starts with "get"/"is" and rest matches name of field in declaring class
     * @see #findGetterField()
     */
    public boolean isGetter() {
        return this.findGetterField() != null;
    }

    @Override
    public <A extends Annotation> A getAnnotationConsideringFieldAndGetter(Class<A> annotationClass) {
        A annotation = this.getAnnotation(annotationClass);
        if (annotation == null) {
            MemberScope<?, ?> associatedField = this.findGetterField();
            annotation = associatedField == null ? null : associatedField.getAnnotation(annotationClass);
        }
        return annotation;
    }

    /**
     * Returns the name to be used to reference this method in its parent's "properties".
     *
     * @return the (potentially overridden) name of the method followed by its argument types in parentheses
     * @see #getName()
     * @see TypeContext#getMethodPropertyArgumentTypeDescription(ResolvedType)
     */
    @Override
    public String getSchemaPropertyName() {
        String result = this.getName();
        result += this.getArgumentTypes().stream()
                .map(this.getContext()::getMethodPropertyArgumentTypeDescription)
                .collect(Collectors.joining(", ", "(", ")"));
        return result;
    }
}
