/*
 * Copyright 2022 VicTools.
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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.TypeContext;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of the {@link CustomDefinitionProviderV2} interface for handling types with the {@code @JsonIdentityReference(alwaysAsid = true)}
 identityReferenceAnnotation.
 */
public class JsonIdentityReferenceDefinitionProvider implements CustomDefinitionProviderV2 {

    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
        if (javaType == null) {
            // since 4.37.0: not for void methods
            return null;
        }
        return this.getIdentityReferenceType(javaType, context.getTypeContext())
                .map(context::createDefinitionReference)
                .map(CustomDefinition::new)
                .orElse(null);
    }

    /**
     * Implementation of the {@code CustomPropertyDefinitionProvider} interface that can be used for both fields and methods.
     *
     * @param scope field/method on which to check for the {@code @JsonIdentityReference} annotation
     * @param context generation context enabling the standard schema generation for the identity property's value type
     * @return created custom definition (may be {@code null})
     */
    public CustomPropertyDefinition provideCustomPropertySchemaDefinition(MemberScope<?, ?> scope, SchemaGenerationContext context) {
        return this.getIdentityReferenceType(scope)
                .map(context::createDefinitionReference)
                .map(CustomPropertyDefinition::new)
                .orElse(null);
    }

    /**
     * If applicable, determine the type of the identity reference that should replace the given actual type, if the
     * {@code @JsonIdentityReference(alwaysAsId = true)} annotation is present as well as a corresponding {@code @JsonIdentityInfo} annotation on the
     * type itself.
     *
     * @param javaType reference type that may be replaced by a reference to its identity property
     * @param typeContext type context providing convenience methods, e.g., for the annotation or member look-up
     * @return designated type of the applicable identity reference (may be empty)
     */
    public Optional<ResolvedType> getIdentityReferenceType(ResolvedType javaType, TypeContext typeContext) {
        JsonIdentityReference referenceAnnotation = typeContext.getAnnotationFromList(
                JsonIdentityReference.class,
                Arrays.asList(javaType.getErasedType().getAnnotations()),
                JacksonHelper.JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER
        );
        return this.getIdentityReferenceType(referenceAnnotation, javaType, typeContext);
    }

    /**
     * If applicable, determine the type of the identity reference that should replace the given field/method's type, if the
     * {@code @JsonIdentityReference(alwaysAsId = true)} annotation is present as well as a corresponding {@code @JsonIdentityInfo} annotation on the
     * type itself.
     *
     * @param scope field/method that may be replaced by a reference to its identity property
     * @return designated type of the applicable identity reference (may be empty)
     */
    public Optional<ResolvedType> getIdentityReferenceType(MemberScope<?, ?> scope) {
        JsonIdentityReference referenceAnnotation = scope.getContainerItemAnnotationConsideringFieldAndGetterIfSupported(JsonIdentityReference.class, JacksonHelper.JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER);
        if (referenceAnnotation == null) {
            referenceAnnotation = scope.getAnnotationConsideringFieldAndGetter(JsonIdentityReference.class, JacksonHelper.JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER);
        }
        return this.getIdentityReferenceType(referenceAnnotation, scope.getType(), scope.getContext());
    }
    
    /**
     * If applicable, determine the type of the identity reference that should replace the given actual type, if the
     * {@code @JsonIdentityReference(alwaysAsId = true)} annotation is present as well as a corresponding {@code @JsonIdentityInfo} annotation on the
     * type itself.
     *
     * @param referenceAnnotation the applicable {@code @JsonidentityReference} annotation in the given context (may be {@code null})
     * @param javaType reference type that may be replaced by a reference to its identity property
     * @param typeContext type context providing convenience methods, e.g., for the annotation or member look-up
     * @return designated type of the applicable identity reference (may be empty)
     */
    private Optional<ResolvedType> getIdentityReferenceType(JsonIdentityReference referenceAnnotation, ResolvedType javaType,
            TypeContext typeContext) {
        if (referenceAnnotation == null || !referenceAnnotation.alwaysAsId()) {
            // no replacement if no @JsonIdentityReference annotation is present or it has "alwaysAsId=false"
            return Optional.empty();
        }
        // additionally, the type itself must have a @JsonIdentityInfo annotation
        ResolvedType typeWithIdentityInfoAnnotation = typeContext.getTypeWithAnnotation(javaType, JsonIdentityInfo.class, JacksonHelper.JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER);
        if (typeWithIdentityInfoAnnotation == null) {
            // otherwise, the @JsonIdentityReference annotation is simply ignored
            return Optional.empty();
        }
        JsonIdentityInfo identityInfoAnnotation = typeContext.getAnnotationFromList(JsonIdentityInfo.class, Arrays.asList(typeWithIdentityInfoAnnotation.getErasedType().getAnnotations()), JacksonHelper.JACKSON_ANNOTATIONS_INSIDE_ANNOTATED_FILTER);
        // @JsonIdentityInfo annotation declares generator with specific identity type
        ResolvedType identityTypeFromGenerator = typeContext.getTypeParameterFor(typeContext.resolve(identityInfoAnnotation.generator()),
                ObjectIdGenerator.class, 0);
        if (identityTypeFromGenerator == null || identityTypeFromGenerator.getErasedType() == Object.class) {
            // the identity may be derived from a property
            String idPropertyName = identityInfoAnnotation.property();
            ResolvedField[] eligibleFields = typeContext.resolveWithMembers(typeWithIdentityInfoAnnotation).getMemberFields();
            Optional<ResolvedType> identityTypeFromProperty = Stream.of(eligibleFields)
                    .filter(field -> field.getName().equals(idPropertyName))
                    .map(ResolvedMember::getType)
                    .findFirst();
            if (identityTypeFromProperty.isPresent()) {
                return identityTypeFromProperty;
            }
        }
        return Optional.ofNullable(identityTypeFromGenerator);
    }
}
