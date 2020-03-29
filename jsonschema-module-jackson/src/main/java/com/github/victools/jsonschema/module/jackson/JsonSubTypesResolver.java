/*
 * Copyright 2020 VicTools.
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Look-up of subtypes from a {@link JsonSubTypes} annotation.
 */
public class JsonSubTypesResolver implements SubtypeResolver, CustomDefinitionProviderV2 {

    /*
     * Looking-up declared subtypes for encountered supertype in general.
     */
    @Override
    public List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
        JsonSubTypes subtypesAnnotation = declaredType.getErasedType().getAnnotation(JsonSubTypes.class);
        return this.lookUpSubtypesFromAnnotation(declaredType, subtypesAnnotation, context.getTypeContext());
    }

    /**
     * Look-up applicable subtypes for the given field if there is a {@link JsonSubTypes} annotation.
     *
     * @param field targeted field
     * @return list of annotated subtypes (or {@code null} if there is no {@link JsonSubTypes} annotation)
     */
    public List<ResolvedType> findTargetTypeOverrides(FieldScope field) {
        List<ResolvedType> subtypesFromFieldOverride = this.lookUpSubtypesFromAnnotation(field.getType(),
                field.getAnnotationConsideringFieldAndGetter(JsonSubTypes.class), field.getContext());
        if (subtypesFromFieldOverride != null) {
            return subtypesFromFieldOverride;
        }
        if (field.getAnnotationConsideringFieldAndGetter(JsonTypeInfo.class) == null) {
            return null;
        }
        JsonSubTypes subtypesAnnotation = field.getType().getErasedType().getAnnotation(JsonSubTypes.class);
        return this.lookUpSubtypesFromAnnotation(field.getType(), subtypesAnnotation, field.getContext());
    }

    /**
     * Look-up applicable subtypes for the given method if there is a {@link JsonSubTypes} annotation.
     *
     * @param method targeted method
     * @return list of annotated subtypes (or {@code null} if there is no {@link JsonSubTypes} annotation)
     */
    public List<ResolvedType> findTargetTypeOverrides(MethodScope method) {
        if (method.isVoid()) {
            return null;
        }
        return this.lookUpSubtypesFromAnnotation(method.getType(), method.getAnnotationConsideringFieldAndGetter(JsonSubTypes.class),
                method.getContext());
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
     * subtype will be resolved without any type parameters – for simplicity's sake not even the ones declared alongside the supertype then.
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
        Class<?> targetSuperType = javaType.getErasedType();
        JsonTypeInfo typeInfoAnnotation;
        do {
            typeInfoAnnotation = targetSuperType.getAnnotation(JsonTypeInfo.class);
            targetSuperType = targetSuperType.getSuperclass();
        } while (typeInfoAnnotation == null && targetSuperType != null);

        if (typeInfoAnnotation == null || javaType.getErasedType().getDeclaredAnnotation(JsonSubTypes.class) != null) {
            return null;
        }
        ObjectNode definition = this.createSubtypeDefinition(javaType, typeInfoAnnotation, null, context);
        if (definition == null) {
            return null;
        }
        return new CustomDefinition(definition, CustomDefinition.DefinitionType.STANDARD, CustomDefinition.AttributeInclusion.NO);
    }

    /**
     * Providing custom schema definition for field/method in case of a per-property override of the applicable subtypes or how they are serialized.
     *
     * @param scope targeted field or method
     * @param context generation context
     * @return applicable custom per-property override schema definition (may be {@code null})
     */
    public CustomPropertyDefinition provideCustomPropertySchemaDefinition(MemberScope<?, ?> scope, SchemaGenerationContext context) {
        JsonTypeInfo typeInfoAnnotation = scope.getAnnotationConsideringFieldAndGetter(JsonTypeInfo.class);
        if (typeInfoAnnotation == null || scope.getType().getErasedType().getDeclaredAnnotation(JsonSubTypes.class) != null) {
            return null;
        }
        ObjectNode attributes;
        if (scope instanceof FieldScope) {
            attributes = AttributeCollector.collectFieldAttributes((FieldScope) scope, context);
        } else if (scope instanceof MethodScope) {
            attributes = AttributeCollector.collectMethodAttributes((MethodScope) scope, context);
        } else {
            attributes = null;
        }
        ObjectNode definition = this.createSubtypeDefinition(scope.getType(), typeInfoAnnotation, attributes, context);
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
     * @return type identifier (or {@code null} if no supported value could be found)
     */
    private String getTypeIdentifier(ResolvedType javaType, JsonTypeInfo typeInfoAnnotation) {
        Class<?> erasedTargetType = javaType.getErasedType();
        final String typeIdentifier;
        switch (typeInfoAnnotation.use()) {
        case NAME:
            typeIdentifier = Optional.ofNullable(erasedTargetType.getAnnotation(JsonTypeName.class))
                    .map(JsonTypeName::value)
                    .filter(name -> !name.isEmpty())
                    .orElseGet(erasedTargetType::getSimpleName);
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
     * Create the custom schema definition for the given subtype, considering the {@link JsonTypeInfo#include()} setting.
     *
     * @param javaType targeted subtype
     * @param typeInfoAnnotation annotation for looking up the type identifier and determining the kind of inclusion/serialization
     * @param attributesToInclude optional: additional attributes to include on the actual/contained schema definition
     * @param context generation context
     * @return created custom definition (or {@code null} if no supported subtype resolution scenario could be detected
     */
    private ObjectNode createSubtypeDefinition(ResolvedType javaType, JsonTypeInfo typeInfoAnnotation, ObjectNode attributesToInclude,
            SchemaGenerationContext context) {
        final String typeIdentifier = this.getTypeIdentifier(javaType, typeInfoAnnotation);
        if (typeIdentifier == null) {
            return null;
        }
        final ObjectNode definition = context.getGeneratorConfig().createObjectNode();
        switch (typeInfoAnnotation.include()) {
        case WRAPPER_ARRAY:
            definition.put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY));
            ArrayNode itemsArray = definition.withArray(context.getKeyword(SchemaKeyword.TAG_ITEMS));
            itemsArray.addObject()
                    .put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_STRING))
                    .put(context.getKeyword(SchemaKeyword.TAG_CONST), typeIdentifier);
            if (attributesToInclude == null || attributesToInclude.isEmpty()) {
                itemsArray.add(context.createStandardDefinitionReference(javaType, this));
            } else {
                itemsArray.addObject()
                        .withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF))
                        .add(context.createStandardDefinitionReference(javaType, this))
                        .add(attributesToInclude);
            }
            break;
        case WRAPPER_OBJECT:
            definition.put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT));
            ObjectNode propertiesNode = definition.with(context.getKeyword(SchemaKeyword.TAG_PROPERTIES));
            if (attributesToInclude == null || attributesToInclude.isEmpty()) {
                propertiesNode.set(typeIdentifier, context.createStandardDefinitionReference(javaType, this));
            } else {
                propertiesNode.with(typeIdentifier)
                        .withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF))
                        .add(context.createStandardDefinitionReference(javaType, this))
                        .add(attributesToInclude);
            }
            break;
        case PROPERTY:
        case EXISTING_PROPERTY:
            final String propertyName = Optional.ofNullable(typeInfoAnnotation.property())
                    .filter(name -> !name.isEmpty())
                    .orElseGet(() -> typeInfoAnnotation.use().getDefaultPropertyName());
            ObjectNode additionalPart = definition.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF))
                    .add(context.createStandardDefinitionReference(javaType, this))
                    .addObject();
            if (attributesToInclude != null && !attributesToInclude.isEmpty()) {
                additionalPart.setAll(attributesToInclude);
            }
            additionalPart.put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                    .with(context.getKeyword(SchemaKeyword.TAG_PROPERTIES))
                    .with(propertyName)
                    .put(context.getKeyword(SchemaKeyword.TAG_CONST), typeIdentifier);
            break;
        default:
            return null;
        }
        return definition;
    }
}
