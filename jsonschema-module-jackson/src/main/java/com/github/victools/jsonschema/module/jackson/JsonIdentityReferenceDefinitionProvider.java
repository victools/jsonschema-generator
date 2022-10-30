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
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.TypeContext;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of the {@link CustomDefinitionProviderV2} interface for handling types with the {@code @JsonIdentityReference(alwaysAsid = true)}
 identityReferenceAnnotation.
 */
public class JsonIdentityReferenceDefinitionProvider implements CustomDefinitionProviderV2 {

    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
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
                .map(idType -> this.createWrappedDefinitionReference(idType, context))
                .map(CustomPropertyDefinition::new)
                .orElse(null);
    }

    /**
     * Create a schema node containing an "allOf" property that wraps a single schema reference/placeholder to ensure its correct inclusion.
     *
     * @param targetType type for which to generate a schema reference/placeholder
     * @param context generation context enabling the standard schema generation for the identity property's value type
     * @return wrapper schema with the "allOf" array holding the single reference/placeholder
     */
    private ObjectNode createWrappedDefinitionReference(ResolvedType targetType, SchemaGenerationContext context) {
        ObjectNode wrapperNode = context.getGeneratorConfig().createObjectNode();
        wrapperNode.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF))
                .add(context.createDefinitionReference(targetType));
        return wrapperNode;
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
        JsonIdentityReference referenceAnnotation = javaType.getErasedType().getAnnotation(JsonIdentityReference.class);
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
        JsonIdentityReference referenceAnnotation = scope.getContainerItemAnnotationConsideringFieldAndGetterIfSupported(JsonIdentityReference.class);
        if (referenceAnnotation == null) {
            referenceAnnotation = scope.getAnnotationConsideringFieldAndGetter(JsonIdentityReference.class);
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
        ResolvedType typeWithIdentityInfoAnnotation = typeContext.getTypeWithAnnotation(javaType, JsonIdentityInfo.class);
        if (typeWithIdentityInfoAnnotation == null) {
            // otherwise, the @JsonIdentityReference annotation is simply ignored
            return Optional.empty();
        }
        JsonIdentityInfo identityInfoAnnotation = typeWithIdentityInfoAnnotation.getErasedType().getAnnotation(JsonIdentityInfo.class);
        // @JsonIdentityInfo annotation declares generator with specific identity type
        ResolvedType identityTypeFromGenerator = typeContext.getTypeParameterFor(typeContext.resolve(identityInfoAnnotation.generator()),
                ObjectIdGenerator.class, 0);
        if (identityTypeFromGenerator.getErasedType() == Object.class) {
            // the identity may be derived from a property
            String idPropertyName = identityInfoAnnotation.property();
            ResolvedField[] eligibleFields = typeContext.resolveWithMembers(typeWithIdentityInfoAnnotation).getMemberFields();
            Optional<ResolvedType> identityTypeFromProperty = Stream.of(eligibleFields)
                    .filter(field -> field.getName().equals(idPropertyName))
                    .map(field -> field.getType())
                    .findFirst();
            if (identityTypeFromProperty.isPresent()) {
                return identityTypeFromProperty;
            }
        }
        return Optional.of(identityTypeFromGenerator);
    }
}
