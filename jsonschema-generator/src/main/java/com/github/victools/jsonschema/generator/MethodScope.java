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
import com.github.victools.jsonschema.generator.impl.LazyValue;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Representation of a single introspected method.
 */
public class MethodScope extends MemberScope<ResolvedMethod, Method> {

    private final LazyValue<FieldScope> getterField = new LazyValue<>(this::doFindGetterField);

    /**
     * Constructor.
     *
     * @param method targeted method
     * @param declaringTypeMembers collection of the declaring type's fields and (other) methods
     * @param context the overall type resolution context
     */
    protected MethodScope(ResolvedMethod method, ResolvedTypeWithMembers declaringTypeMembers, TypeContext context) {
        this(method, null, null, declaringTypeMembers, null, context);
    }

    /**
     * Constructor.
     *
     * @param method targeted method
     * @param overriddenType alternative type for this method's return value
     * @param overriddenName alternative name for this method
     * @param declaringTypeMembers collection of the declaring type's fields and (other) methods
     * @param fakeContainerItemIndex index of the container item on the generic field/method scope's declared type (e.g., in case of a List, it is 0)
     * @param context the overall type resolution context
     */
    protected MethodScope(ResolvedMethod method, ResolvedType overriddenType, String overriddenName,
            ResolvedTypeWithMembers declaringTypeMembers, Integer fakeContainerItemIndex, TypeContext context) {
        super(method, overriddenType, overriddenName, declaringTypeMembers, fakeContainerItemIndex, context);
    }

    @Override
    public MethodScope withOverriddenType(ResolvedType overriddenType) {
        return new MethodScope(this.getMember(), overriddenType, this.getOverriddenName(), this.getDeclaringTypeMembers(),
                this.getFakeContainerItemIndex(), this.getContext());
    }

    @Override
    public MethodScope withOverriddenName(String overriddenName) {
        return new MethodScope(this.getMember(), this.getOverriddenType(), overriddenName, this.getDeclaringTypeMembers(),
                this.getFakeContainerItemIndex(), this.getContext());
    }

    @Override
    public MethodScope asFakeContainerItemScope() {
        return (MethodScope) super.asFakeContainerItemScope();
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
        return this.getterField.get();
    }

    /**
     * Look-up the field associated with this method if it is deemed to be a getter by convention.
     *
     * @return associated field
     */
    private FieldScope doFindGetterField() {
        if (this.getType() == null || this.getArgumentCount() > 0) {
            // void methods or those with arguments are not deemed to be getters
            return null;
        }
        if (!this.isPublic()) {
            // only public methods are deemed eligible to be getters
            return null;
        }
        String methodName = this.getDeclaredName();
        Set<String> possibleFieldNames = Stream.of("get", "is")
                .flatMap(prefix -> getPossibleFieldNames(prefix, methodName))
                .collect(Collectors.toSet());
        if (possibleFieldNames.isEmpty()) {
            // method name does not fall into getter conventions
            return null;
        }
        // method name matched getter conventions
        // check whether a matching field exists
        return Stream.of(this.getDeclaringTypeMembers().getMemberFields())
                .filter(memberField -> possibleFieldNames.contains(memberField.getName()))
                .findFirst()
                .map(field -> this.getContext().createFieldScope(field, this.getDeclaringTypeMembers()))
                .orElse(null);
    }

    private static Stream<String> getPossibleFieldNames(String prefix, String methodName) {
        if (!methodName.startsWith(prefix) || prefix.length() == methodName.length()) {
            return Stream.of();
        }
        Stream.Builder<String> possibleFieldNames = Stream.builder();
        String methodNameWithoutPrefix = methodName.substring(prefix.length());
        if (Character.isUpperCase(methodNameWithoutPrefix.charAt(0))) {
            // ensure that the variable starts with a lower-case letter
            possibleFieldNames.add(methodNameWithoutPrefix.substring(0, 1).toLowerCase() + methodNameWithoutPrefix.substring(1));
            if ("is".equals(prefix)) {
                // since 4.32.0: a method "isBool()" is considered a possible getter for a field "isBool" as well as for "bool"
                possibleFieldNames.add(methodName);
            }
        }
        // @since 4.32.0 - conforming with JavaBeans API specification edge case when second character in field name is in uppercase
        if (methodNameWithoutPrefix.length() > 1 && Character.isUpperCase(methodNameWithoutPrefix.charAt(1))) {
            possibleFieldNames.add(methodNameWithoutPrefix);
        }
        return possibleFieldNames.build();
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

    /**
     * Return the annotation of the given type on the method or its return type, if such an annotation is present.
     *
     * @param <A> type of annotation to look-up
     * @param annotationClass annotation class to look up instance on member for
     * @return annotation instance (or {@code null} if no annotation of the given type is present
     */
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass, Predicate<Annotation> considerOtherAnnotation) {
        A annotation = super.getAnnotation(annotationClass, considerOtherAnnotation);
        if (annotation == null) {
            List<Annotation> annotationList = Arrays.asList(this.getRawMember().getAnnotatedReturnType().getAnnotations());
            annotation = this.getContext().getAnnotationFromList(annotationClass, annotationList, considerOtherAnnotation);
        }
        return annotation;
    }

    @Override
    public <A extends Annotation> A getContainerItemAnnotation(Class<A> annotationClass, Predicate<Annotation> considerOtherAnnotation) {
        AnnotatedType annotatedReturnType = this.getRawMember().getAnnotatedReturnType();
        return this.getContext()
                .getTypeParameterAnnotation(annotationClass, considerOtherAnnotation, annotatedReturnType, this.getFakeContainerItemIndex());
    }

    @Override
    public <A extends Annotation> A getAnnotationConsideringFieldAndGetter(Class<A> annotationClass, Predicate<Annotation> considerOtherAnnotation) {
        A annotation = this.getAnnotation(annotationClass, considerOtherAnnotation);
        if (annotation == null) {
            MemberScope<?, ?> associatedField = this.findGetterField();
            if (associatedField != null) {
                annotation = associatedField.getAnnotation(annotationClass, considerOtherAnnotation);
            }
        }
        return annotation;
    }

    @Override
    public <A extends Annotation> A getContainerItemAnnotationConsideringFieldAndGetter(Class<A> annotationClass,
            Predicate<Annotation> considerOtherAnnotation) {
        A annotation = this.getContainerItemAnnotation(annotationClass, considerOtherAnnotation);
        if (annotation == null) {
            MemberScope<?, ?> associatedField = this.findGetterField();
            if (associatedField != null) {
                annotation = associatedField.getContainerItemAnnotation(annotationClass, considerOtherAnnotation);
            }
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
    protected String doGetSchemaPropertyName() {
        String result;
        if (!this.getContext().isDerivingFieldsFromArgumentFreeMethods() || this.getArgumentCount() > 0) {
            result = this.getName() + this.getArgumentTypes().stream()
                    .map(this.getContext()::getMethodPropertyArgumentTypeDescription)
                    .collect(Collectors.joining(", ", "(", ")"));
        } else if (this.getOverriddenName() == null) {
            result = this.deriveFieldName();
        } else {
            result = this.getName();
        }
        return result;
    }

    private String deriveFieldName() {
        String methodName = this.getDeclaredName();
        // remove the "get"/"is" prefix from non-overridden method names
        Optional<String> possibleFieldName = Stream.of("get", "is")
                .filter(methodName::startsWith)
                .map(prefix -> methodName.substring(prefix.length()))
                .filter(name -> !name.isEmpty())
                .findFirst();
        if (!possibleFieldName.isPresent()) {
            return methodName + "()";
        }
        String methodNameWithoutPrefix = possibleFieldName.get();
        if (Character.isUpperCase(methodNameWithoutPrefix.charAt(1))) {
            // since 4.33.0 according to the Java Beans specification, if the second character is uppercase the first should remain as-is
            // e.g., getxIndex() corresponds to "xIndex" and isURL() to "URL"
            return methodNameWithoutPrefix;
        }
        return Character.toLowerCase(methodNameWithoutPrefix.charAt(0)) + methodNameWithoutPrefix.substring(1);
    }
}
