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
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.github.victools.jsonschema.generator.impl.LazyValue;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Representation of a single introspected field.
 */
public class FieldScope extends MemberScope<ResolvedField, Field> {

    private final LazyValue<MethodScope> getter = new LazyValue<>(this::doFindGetter);

    /**
     * Constructor.
     *
     * @param field targeted field
     * @param declarationDetails basic details regarding the declaration context
     * @param overrideDetails augmenting details (e.g., overridden type, name, or container item index)
     * @param context the overall type resolution context
     */
    protected FieldScope(ResolvedField field, DeclarationDetails declarationDetails, OverrideDetails overrideDetails, TypeContext context) {
        super(field, declarationDetails, overrideDetails, context);
    }

    @Override
    public FieldScope withOverriddenType(ResolvedType overriddenType) {
        OverrideDetails overrideDetails = new OverrideDetails(overriddenType, this.getOverriddenName(), this.getFakeContainerItemIndex());
        return new FieldScope(this.getMember(), this.getDeclarationDetails(), overrideDetails, this.getContext());
    }

    @Override
    public FieldScope withOverriddenName(String overriddenName) {
        OverrideDetails overrideDetails = new OverrideDetails(this.getOverriddenType(), overriddenName, this.getFakeContainerItemIndex());
        return new FieldScope(this.getMember(), this.getDeclarationDetails(), overrideDetails, this.getContext());
    }

    @Override
    public FieldScope asFakeContainerItemScope() {
        return (FieldScope) super.asFakeContainerItemScope();
    }

    /**
     * Returns the name to be used to reference this field in its parent's "properties".
     *
     * @return the (potentially overridden) name of this field
     * @see #getName()
     */
    @Override
    protected String doGetSchemaPropertyName() {
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
        return this.getter.get();
    }

    /**
     * Return the conventional getter method (if one exists). E.g. for a field named "foo", look-up either "getFoo()" or "isFoo()".
     *
     * @return public getter from within the field's declaring class
     */
    private MethodScope doFindGetter() {
        String declaredName = this.getDeclaredName();
        Set<String> possibleGetterNames = new HashSet<>(5);
        // @since 4.32.0 - for a field like "xIndex" also consider "getxIndex()" as getter method (according to JavaBeans specification)
        if (declaredName.length() > 1 && Character.isUpperCase(declaredName.charAt(1))) {
            possibleGetterNames.add("get" + declaredName);
            possibleGetterNames.add("is" + declaredName);
        }
        // common naming convention: capitalise first character and leave the rest as-is
        String capitalisedFieldName = declaredName.substring(0, 1).toUpperCase() + declaredName.substring(1);
        possibleGetterNames.add("get" + capitalisedFieldName);
        possibleGetterNames.add("is" + capitalisedFieldName);
        // @since 4.32.0 - for a field like "isBool" also consider "isBool()" as potential getter method
        boolean fieldNameStartsWithIs = declaredName.startsWith("is") && declaredName.length() > 2 && Character.isUpperCase(declaredName.charAt(2));
        if (fieldNameStartsWithIs) {
            possibleGetterNames.add(declaredName);
        }
        ResolvedMethod[] methods = this.getDeclaringTypeMembers().getMemberMethods();
        return Stream.of(methods)
                .filter(method -> method.isPublic() && method.getRawMember().getParameterCount() == 0)
                .filter(method -> possibleGetterNames.contains(method.getName()))
                .findFirst()
                .map(method -> this.getContext().createMethodScope(method, this.getDeclarationDetails()))
                .orElse(null);
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

    @Override
    public <A extends Annotation> A getContainerItemAnnotation(Class<A> annotationClass, Predicate<Annotation> considerOtherAnnotation) {
        AnnotatedType annotatedType = this.getRawMember().getAnnotatedType();
        return this.getContext()
                .getTypeParameterAnnotation(annotationClass, considerOtherAnnotation, annotatedType, this.getFakeContainerItemIndex());
    }

    @Override
    public <A extends Annotation> A getAnnotationConsideringFieldAndGetter(Class<A> annotationClass, Predicate<Annotation> considerOtherAnnotation) {
        A annotation = this.getAnnotation(annotationClass, considerOtherAnnotation);
        if (annotation == null) {
            MemberScope<?, ?> associatedGetter = this.findGetter();
            annotation = associatedGetter == null ? null : associatedGetter.getAnnotation(annotationClass, considerOtherAnnotation);
        }
        return annotation;
    }

    @Override
    public <A extends Annotation> A getContainerItemAnnotationConsideringFieldAndGetter(Class<A> annotationClass,
            Predicate<Annotation> considerOtherAnnotation) {
        A annotation = this.getContainerItemAnnotation(annotationClass, considerOtherAnnotation);
        if (annotation == null) {
            MemberScope<?, ?> associatedGetter = this.findGetter();
            annotation = associatedGetter == null ? null : associatedGetter.getContainerItemAnnotation(annotationClass, considerOtherAnnotation);
        }
        return annotation;
    }
}
