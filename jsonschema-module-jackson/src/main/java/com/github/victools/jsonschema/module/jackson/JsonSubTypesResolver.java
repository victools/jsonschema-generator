/*
 * Copyright 2020-2024 VicTools.
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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.victools.jsonschema.generator.AnnotationHelper;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SubtypeResolver;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.TypeScope;
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Look-up of subtypes from a {@link JsonSubTypes} annotation.
 */
public class JsonSubTypesResolver implements SubtypeResolver, CustomDefinitionProviderV2 {

    private final CustomDefinition.DefinitionType wrappingSubtypeDefinitionType;
    private final boolean shouldInlineNestedSubtypes;
    private final Optional<JsonIdentityReferenceDefinitionProvider> identityReferenceProvider;

    /**
     * Default constructor equivalent to calling {@code new JsonSubTypesResolver(Collections.emptyList())}.
     */
    public JsonSubTypesResolver() {
        this(Collections.emptyList());
        // default constructor for backward compatibility, in case this was extended by someone
    }

    /**
     * Constructor expecting list of enabled module options.
     * <br>
     * Currently, only the {@link JacksonOption#ALWAYS_REF_SUBTYPES} is considered here. Other relevant options are handled by the module class.
     *
     * @param options module options to derive differing behavior from
     */
    public JsonSubTypesResolver(Collection<JacksonOption> options) {
        this.wrappingSubtypeDefinitionType = options.contains(JacksonOption.ALWAYS_REF_SUBTYPES)
                ? CustomDefinition.DefinitionType.ALWAYS_REF
                : CustomDefinition.DefinitionType.STANDARD;
        this.shouldInlineNestedSubtypes = options.contains(JacksonOption.INLINE_TRANSFORMED_SUBTYPES);
        if (options.contains(JacksonOption.JSONIDENTITY_REFERENCE_ALWAYS_AS_ID)) {
            this.identityReferenceProvider = Optional.of(new JsonIdentityReferenceDefinitionProvider());
        } else {
            this.identityReferenceProvider = Optional.empty();
        }
    }

    /**
     * Check whether to skip the subtype handling for a particular field/method (e.g. when {@code JacksonOption.JSONIDENTITY_REFERENCE_ALWAYS_AS_ID}
     * applies instead).
     *
     * @param member field/method for which a potential subtype should be resolved (or not)
     * @return whether to skip the subtype resolution for the given field/method
     */
    private boolean skipSubtypeResolution(MemberScope<?, ?> member) {
        return member.getType() == null
                || this.identityReferenceProvider.flatMap(idRefProvider -> idRefProvider.getIdentityReferenceType(member)).isPresent();
    }

    /**
     * Check whether to skip the subtype handling for a particular type (e.g. when {@code JacksonOption.JSONIDENTITY_REFERENCE_ALWAYS_AS_ID} applies
     * instead).
     *
     * @param declaredType type for which a potential subtype should be resolved (or not)
     * @param context applicable type context, that offers convenience methods, e.g., for the annotation look-up
     * @return whether to skip the subtype resolution for the given type
     */
    private boolean skipSubtypeResolution(ResolvedType declaredType, TypeContext context) {
        return this.identityReferenceProvider.flatMap(idRefProvider -> idRefProvider.getIdentityReferenceType(declaredType, context)).isPresent();
    }

    /*
     * Looking-up declared subtypes for encountered supertype in general.
     */
    @Override
    public List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
        if (this.skipSubtypeResolution(declaredType, context.getTypeContext())) {
            return null;
        }
        JsonSubTypes subtypesAnnotation = AnnotationHelper.resolveAnnotation(declaredType.getErasedType(), JsonSubTypes.class,
                JacksonSchemaModule.NESTED_ANNOTATION_CHECK).orElse(null);
        return this.lookUpSubtypesFromAnnotation(declaredType, subtypesAnnotation, context.getTypeContext());
    }

    /**
     * Look-up applicable subtypes for the given field/method if there is a {@link JsonSubTypes} annotation.
     *
     * @param property targeted field/method
     * @return list of annotated subtypes (or {@code null} if there is no {@link JsonSubTypes} annotation)
     */
    public List<ResolvedType> findTargetTypeOverrides(MemberScope<?, ?> property) {
        if (this.skipSubtypeResolution(property)) {
            return null;
        }
        JsonSubTypes subtypesAnnotation = property.getAnnotationConsideringFieldAndGetter(JsonSubTypes.class,
                JacksonSchemaModule.NESTED_ANNOTATION_CHECK);
        return this.lookUpSubtypesFromAnnotation(property.getType(), subtypesAnnotation, property.getContext());
    }

    /**
     * Mapping the declared erased types from the annotation to resolved types. Returns {@code null} if annotation is {@code null}/not present.
     *
     * @param declaredType supertype encountered while generating a schema
     * @param subtypesAnnotation annotation to derive applicable subtypes from.
     * @param context type context for proper type resolution
     * @return resolved annotated subtypes (or {@code null} if given annotation is {@code null})
     */
    public List<ResolvedType> lookUpSubtypesFromAnnotation(ResolvedType declaredType, JsonSubTypes subtypesAnnotation, TypeContext context) {
        if (subtypesAnnotation == null) {
            return null;
        }
        return Stream.of(subtypesAnnotation.value())
                .map(entry -> this.resolveSubtype(declaredType, entry, context))
                .collect(Collectors.toList());
    }

    /**
     * Safe way of resolving an erased subtype from its supertype. If the subtype introduces generic parameters not present on the supertype, the
     * subtype will be resolved without any type parameters - for simplicity's sake not even the ones declared alongside the supertype then.
     *
     * @param declaredType supertype encountered while generating a schema
     * @param annotatedSubtype single subtype declared via {@link JsonSubTypes} on the super class
     * @param context type context for proper type resolution
     * @return resolved annotated subtype
     */
    private ResolvedType resolveSubtype(ResolvedType declaredType, JsonSubTypes.Type annotatedSubtype, TypeContext context) {
        try {
            return context.resolveSubtype(declaredType, annotatedSubtype.value());
        } catch (IllegalArgumentException ex) {
            return context.resolve(annotatedSubtype.value());
        }
    }

    /*
     * Providing custom schema definition for subtype.
     */
    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
        if (javaType == null) {
            // since 4.37.0: not for void methods
            return null;
        }
        final TypeContext typeContext = context.getTypeContext();
        ResolvedType typeWithTypeInfo = typeContext.getTypeWithAnnotation(javaType, JsonTypeInfo.class, JacksonSchemaModule.NESTED_ANNOTATION_CHECK);
        if (typeWithTypeInfo == null
                || AnnotationHelper.resolveAnnotation(javaType.getErasedType(), JsonSubTypes.class,
                    JacksonSchemaModule.NESTED_ANNOTATION_CHECK).isPresent()
                || this.skipSubtypeResolution(javaType, typeContext)) {
            // no @JsonTypeInfo annotation found or the given javaType is the super type, that should be replaced
            return null;
        }
        Class<?> erasedTypeWithTypeInfo = typeWithTypeInfo.getErasedType();
        final List<Annotation> annotationsList = Arrays.asList(erasedTypeWithTypeInfo.getAnnotations());
        JsonTypeInfo typeInfoAnnotation = typeContext.getAnnotationFromList(JsonTypeInfo.class, annotationsList,
                JacksonSchemaModule.NESTED_ANNOTATION_CHECK);
        JsonSubTypes subTypesAnnotation = typeContext.getAnnotationFromList(JsonSubTypes.class, annotationsList,
                JacksonSchemaModule.NESTED_ANNOTATION_CHECK);
        TypeScope scope = typeContext.createTypeScope(javaType);
        ObjectNode definition = this.createSubtypeDefinition(scope, typeInfoAnnotation, subTypesAnnotation, context);
        if (definition == null) {
            return null;
        }
        return new CustomDefinition(definition, this.wrappingSubtypeDefinitionType, CustomDefinition.AttributeInclusion.NO);
    }

    /**
     * Providing custom schema definition for field/method in case of a per-property override of the applicable subtypes or how they are serialized.
     *
     * @param scope targeted field or method
     * @param context generation context
     * @return applicable custom per-property override schema definition (may be {@code null})
     */
    public CustomPropertyDefinition provideCustomPropertySchemaDefinition(MemberScope<?, ?> scope, SchemaGenerationContext context) {
        if (this.skipSubtypeResolution(scope) || AnnotationHelper.resolveAnnotation(scope.getType().getErasedType(), JsonSubTypes.class,
                JacksonSchemaModule.NESTED_ANNOTATION_CHECK).isPresent()) {
            return null;
        }
        JsonTypeInfo typeInfoAnnotation = scope.getAnnotationConsideringFieldAndGetter(JsonTypeInfo.class,
                JacksonSchemaModule.NESTED_ANNOTATION_CHECK);
        if (typeInfoAnnotation == null) {
            // the normal per-type behaviour is not being overridden, i.e., no need for an inline custom property schema
            return null;
        }
        if (typeInfoAnnotation.use() == JsonTypeInfo.Id.NONE) {
            ObjectNode definition = context.getGeneratorConfig().createObjectNode();
            // wrap reference into "allOf" in order to force correct handling, in case the field/method being nullable
            definition.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF))
                    .add(context.createStandardDefinitionReference(scope.getType(), this));
            return new CustomPropertyDefinition(definition, CustomDefinition.AttributeInclusion.YES);
        }
        JsonSubTypes subTypesAnnotation = scope.getAnnotationConsideringFieldAndGetter(JsonSubTypes.class,
                JacksonSchemaModule.NESTED_ANNOTATION_CHECK);
        ObjectNode definition = this.createSubtypeDefinition(scope, typeInfoAnnotation, subTypesAnnotation, context);
        if (definition == null) {
            return null;
        }
        return new CustomPropertyDefinition(definition, CustomDefinition.AttributeInclusion.NO);
    }

    /**
     * Determine the appropriate type identifier according to {@link JsonTypeInfo#use()}.
     *
     * @param javaType specific subtype to identify
     * @param typeInfoAnnotation annotation for determining what kind of identifier to use
     * @param subTypesAnnotation annotation to consider for certain kinds of discriminators
     * @return type identifier (or {@code null} if no supported value could be found)
     */
    private String getTypeIdentifier(ResolvedType javaType, JsonTypeInfo typeInfoAnnotation, JsonSubTypes subTypesAnnotation) {
        Class<?> erasedTargetType = javaType.getErasedType();
        final String typeIdentifier;
        switch (typeInfoAnnotation.use()) {
        case NAME:
            typeIdentifier = getNameFromSubTypeAnnotation(erasedTargetType, subTypesAnnotation)
                    .orElseGet(() -> getNameFromTypeNameAnnotation(erasedTargetType)
                    .orElseGet(() -> getUnqualifiedClassName(erasedTargetType)));
            break;
        case CLASS:
            typeIdentifier = erasedTargetType.getName();
            break;
        default:
            typeIdentifier = null;
        }
        return typeIdentifier;
    }

    /**
     * In case of a {@code JsonTypeInfo.Id.NAME}, try to look up the reference via {@link JsonSubTypes} annotation.
     *
     * @param erasedTargetType class to look-up the "name" for
     * @param subTypesAnnotation {@link JsonSubTypes} annotation instance of consider
     * @return successfully looked-up "name" (otherwise an empty {@code Optional})
     */
    private static Optional<String> getNameFromSubTypeAnnotation(Class<?> erasedTargetType, JsonSubTypes subTypesAnnotation) {
        if (subTypesAnnotation == null) {
            return Optional.empty();
        }
        return Stream.of(subTypesAnnotation.value())
                .filter(subTypeAnnotation -> erasedTargetType.equals(subTypeAnnotation.value()))
                .findFirst()
                .map(JsonSubTypes.Type::name)
                .filter(name -> !name.isEmpty());
    }

    /**
     * Determine the unqualified name of the given class, e.g., as fall-back value for subtype reference with {@code JsonTypeInfo.Id.NAME}.
     *
     * @param erasedTargetType class to produce unqualified class name for
     * @return simple class name, with declaring class's unqualified name as prefix for member classes
     */
    private static Optional<String> getNameFromTypeNameAnnotation(Class<?> erasedTargetType) {
        return AnnotationHelper.resolveAnnotation(erasedTargetType, JsonTypeName.class, JacksonSchemaModule.NESTED_ANNOTATION_CHECK)
                .map(JsonTypeName::value)
                .filter(name -> !name.isEmpty());
    }

    /**
     * Determine the unqualified name of the given class, e.g., as fall-back value for subtype reference with {@code JsonTypeInfo.Id.NAME}.
     *
     * @param erasedTargetType class to produce unqualified class name for
     * @return simple class name, with declaring class's unqualified name as prefix for member classes
     */
    private static String getUnqualifiedClassName(Class<?> erasedTargetType) {
        Class<?> declaringClass = erasedTargetType.getDeclaringClass();
        if (declaringClass == null) {
            return erasedTargetType.getSimpleName();
        }
        return getUnqualifiedClassName(declaringClass) + '$' + erasedTargetType.getSimpleName();
    }

    /**
     * Create the custom schema definition for the given subtype, considering the {@link JsonTypeInfo#include()} setting.
     *
     * @param scope targeted subtype
     * @param typeInfoAnnotation annotation for looking up the type identifier and determining the kind of inclusion/serialization
     * @param subTypesAnnotation annotation specifying the mapping from super to subtypes (potentially including the discriminator values)
     * @param context generation context
     * @return created custom definition (or {@code null} if no supported subtype resolution scenario could be detected
     */
    private ObjectNode createSubtypeDefinition(TypeScope scope, JsonTypeInfo typeInfoAnnotation, JsonSubTypes subTypesAnnotation,
            SchemaGenerationContext context) {
        ResolvedType javaType = scope.getType();
        final String typeIdentifier = this.getTypeIdentifier(javaType, typeInfoAnnotation, subTypesAnnotation);
        if (typeIdentifier == null) {
            return null;
        }
        ObjectNode attributesToInclude = this.getAttributesToInclude(scope, context);
        final ObjectNode definition = context.getGeneratorConfig().createObjectNode();
        SubtypeDefinitionDetails subtypeDetails = new SubtypeDefinitionDetails(javaType, attributesToInclude, context, typeIdentifier, definition);
        switch (typeInfoAnnotation.include()) {
        case WRAPPER_ARRAY:
            createSubtypeDefinitionForWrapperArrayTypeInfo(subtypeDetails);
            break;
        case WRAPPER_OBJECT:
            this.createSubtypeDefinitionForWrapperObjectTypeInfo(subtypeDetails);
            break;
        case PROPERTY:
        case EXISTING_PROPERTY:
            this.createSubtypeDefinitionForPropertyTypeInfo(subtypeDetails, typeInfoAnnotation);
            break;
        default:
            return null;
        }
        return definition;
    }

    private void createSubtypeDefinitionForWrapperArrayTypeInfo(SubtypeDefinitionDetails details) {
        details.getDefinition().put(details.getKeyword(SchemaKeyword.TAG_TYPE), details.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY));
        ArrayNode itemsArray = details.getDefinition().withArray(details.getKeyword(SchemaKeyword.TAG_PREFIX_ITEMS));
        itemsArray.addObject()
                .put(details.getKeyword(SchemaKeyword.TAG_TYPE), details.getKeyword(SchemaKeyword.TAG_TYPE_STRING))
                .put(details.getKeyword(SchemaKeyword.TAG_CONST), details.getTypeIdentifier());
        if (details.getAttributesToInclude() == null || details.getAttributesToInclude().isEmpty()) {
            itemsArray.add(this.createNestedSubtypeSchema(details.getJavaType(), details.getContext()));
        } else {
            itemsArray.addObject()
                    .withArray(details.getKeyword(SchemaKeyword.TAG_ALLOF))
                    .add(this.createNestedSubtypeSchema(details.getJavaType(), details.getContext()))
                    .add(details.getAttributesToInclude());
        }
    }

    private void createSubtypeDefinitionForWrapperObjectTypeInfo(SubtypeDefinitionDetails details) {
        details.getDefinition().put(details.getKeyword(SchemaKeyword.TAG_TYPE), details.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));
        ObjectNode propertiesNode = details.getDefinition()
                .putObject(details.getKeyword(SchemaKeyword.TAG_PROPERTIES));
        ObjectNode nestedSubtypeSchema = this.createNestedSubtypeSchema(details.getJavaType(), details.getContext());
        if (details.getAttributesToInclude() == null || details.getAttributesToInclude().isEmpty()) {
            propertiesNode.set(details.getTypeIdentifier(), nestedSubtypeSchema);
        } else {
            propertiesNode.putObject(details.getTypeIdentifier())
                    .withArray(details.getKeyword(SchemaKeyword.TAG_ALLOF))
                    .add(nestedSubtypeSchema)
                    .add(details.getAttributesToInclude());
        }
        details.getDefinition().withArray(details.getKeyword(SchemaKeyword.TAG_REQUIRED)).add(details.getTypeIdentifier());
    }

    private void createSubtypeDefinitionForPropertyTypeInfo(SubtypeDefinitionDetails details, JsonTypeInfo typeInfoAnnotation) {
        final String propertyName = Optional.ofNullable(typeInfoAnnotation.property())
                .filter(name -> !name.isEmpty())
                .orElseGet(() -> typeInfoAnnotation.use().getDefaultPropertyName());
        ObjectNode additionalPart = details.getDefinition().withArray(details.getKeyword(SchemaKeyword.TAG_ALLOF))
                .add(this.createNestedSubtypeSchema(details.getJavaType(), details.getContext()))
                .addObject();
        if (details.getAttributesToInclude() != null && !details.getAttributesToInclude().isEmpty()) {
            additionalPart.setAll(details.getAttributesToInclude());
        }
        additionalPart.put(details.getKeyword(SchemaKeyword.TAG_TYPE), details.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                .putObject(details.getKeyword(SchemaKeyword.TAG_PROPERTIES))
                .putObject(propertyName)
                .put(details.getKeyword(SchemaKeyword.TAG_CONST), details.getTypeIdentifier());
        if (!details.getJavaType().getErasedType().equals(typeInfoAnnotation.defaultImpl())) {
            additionalPart.withArray(details.getKeyword(SchemaKeyword.TAG_REQUIRED))
                    .add(propertyName);
        }
    }

    private ObjectNode createNestedSubtypeSchema(ResolvedType javaType, SchemaGenerationContext context) {
        if (this.shouldInlineNestedSubtypes) {
            return context.createStandardDefinition(javaType, this);
        }
        return context.createStandardDefinitionReference(javaType, this);
    }

    private ObjectNode getAttributesToInclude(TypeScope scope, SchemaGenerationContext context) {
        ObjectNode attributesToInclude;
        if (scope instanceof FieldScope fieldScope) {
            attributesToInclude = AttributeCollector.collectFieldAttributes(fieldScope, context);
        } else if (scope instanceof MethodScope methodScope) {
            attributesToInclude = AttributeCollector.collectMethodAttributes(methodScope, context);
        } else {
            attributesToInclude = null;
        }
        return attributesToInclude;
    }

    private static class SubtypeDefinitionDetails {
        private final ResolvedType javaType;
        private final ObjectNode attributesToInclude;
        private final SchemaGenerationContext context;
        private final String typeIdentifier;
        private final ObjectNode definition;

        SubtypeDefinitionDetails(ResolvedType javaType, ObjectNode attributesToInclude, SchemaGenerationContext context,
                String typeIdentifier, ObjectNode definition) {
            this.javaType = javaType;
            this.attributesToInclude = attributesToInclude;
            this.context = context;
            this.typeIdentifier = typeIdentifier;
            this.definition = definition;
        }

        ResolvedType getJavaType() {
            return this.javaType;
        }

        ObjectNode getAttributesToInclude() {
            return this.attributesToInclude;
        }

        SchemaGenerationContext getContext() {
            return this.context;
        }

        String getTypeIdentifier() {
            return this.typeIdentifier;
        }

        ObjectNode getDefinition() {
            return this.definition;
        }

        String getKeyword(SchemaKeyword keyword) {
            return this.context.getKeyword(keyword);
        }
    }
}
