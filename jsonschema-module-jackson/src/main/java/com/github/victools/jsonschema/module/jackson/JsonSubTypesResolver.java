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
     * Look-up applicable subtypes for the given field/method if there is a {@link JsonSubTypes} annotation.
     *
     * @param property targeted field/method
     * @return list of annotated subtypes (or {@code null} if there is no {@link JsonSubTypes} annotation)
     */
    public List<ResolvedType> findTargetTypeOverrides(MemberScope<?, ?> property) {
        if (property.getType() == null) {
            return null;
        }
        JsonSubTypes subtypesAnnotation = property.getAnnotationConsideringFieldAndGetter(JsonSubTypes.class);
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
        Class<?> typeWithTypeInfo = this.getTypeDeclaringJsonTypeInfoAnnotation(javaType.getErasedType());
        if (typeWithTypeInfo == null || javaType.getErasedType().getAnnotation(JsonSubTypes.class) != null) {
            // no @JsonTypeInfo annotation found or the given javaType is the super type, that should be replaced
            return null;
        }
        JsonTypeInfo typeInfoAnnotation = typeWithTypeInfo.getAnnotation(JsonTypeInfo.class);
        JsonSubTypes subTypesAnnotation = typeWithTypeInfo.getAnnotation(JsonSubTypes.class);
        ObjectNode definition = this.createSubtypeDefinition(javaType, typeInfoAnnotation, subTypesAnnotation, null, context);
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
        if (scope.getType() == null || scope.getType().getErasedType().getDeclaredAnnotation(JsonSubTypes.class) != null) {
            return null;
        }
        JsonTypeInfo typeInfoAnnotation = scope.getAnnotationConsideringFieldAndGetter(JsonTypeInfo.class);
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
        ObjectNode attributes;
        if (scope instanceof FieldScope) {
            attributes = AttributeCollector.collectFieldAttributes((FieldScope) scope, context);
        } else if (scope instanceof MethodScope) {
            attributes = AttributeCollector.collectMethodAttributes((MethodScope) scope, context);
        } else {
            attributes = null;
        }
        JsonSubTypes subTypesAnnotation = scope.getAnnotationConsideringFieldAndGetter(JsonSubTypes.class);
        ObjectNode definition = this.createSubtypeDefinition(scope.getType(), typeInfoAnnotation, subTypesAnnotation, attributes, context);
        if (definition == null) {
            return null;
        }
        return new CustomPropertyDefinition(definition, CustomDefinition.AttributeInclusion.NO);
    }

    private Class<?> getTypeDeclaringJsonTypeInfoAnnotation(Class<?> erasedTargetType) {
        Class<?> targetSuperType = erasedTargetType;
        do {
            if (targetSuperType.getAnnotation(JsonTypeInfo.class) != null) {
                return targetSuperType;
            }
            // the @JsonTypeInfo annotation may also be present on a common interface rather than a super class
            // assumption: there are never multiple interfaces with such an annotation on a single class
            Optional<Class<?>> interfaceWithAnnotation = Stream.of(targetSuperType.getInterfaces())
                    .filter(superInterface -> superInterface.getAnnotation(JsonTypeInfo.class) != null)
                    .findFirst();
            if (interfaceWithAnnotation.isPresent()) {
                return interfaceWithAnnotation.get();
            }
            targetSuperType = targetSuperType.getSuperclass();
        } while (targetSuperType != null);
        return null;
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
                .map(subTypeAnnotation -> subTypeAnnotation.name())
                .filter(name -> !name.isEmpty());
    }

    /**
     * Determine the unqualified name of the given class, e.g., as fall-back value for subtype reference with {@code JsonTypeInfo.Id.NAME}.
     *
     * @param erasedTargetType class to produce unqualified class name for
     * @return simple class name, with declaring class's unqualified name as prefix for member classes
     */
    private static Optional<String> getNameFromTypeNameAnnotation(Class<?> erasedTargetType) {
        return Optional.ofNullable(erasedTargetType.getAnnotation(JsonTypeName.class))
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
     * @param javaType targeted subtype
     * @param typeInfoAnnotation annotation for looking up the type identifier and determining the kind of inclusion/serialization
     * @param subTypesAnnotation annotation specifying the mapping from super to subtypes (potentially including the discriminator values)
     * @param attributesToInclude optional: additional attributes to include on the actual/contained schema definition
     * @param context generation context
     * @return created custom definition (or {@code null} if no supported subtype resolution scenario could be detected
     */
    private ObjectNode createSubtypeDefinition(ResolvedType javaType, JsonTypeInfo typeInfoAnnotation, JsonSubTypes subTypesAnnotation,
            ObjectNode attributesToInclude, SchemaGenerationContext context) {
        final String typeIdentifier = this.getTypeIdentifier(javaType, typeInfoAnnotation, subTypesAnnotation);
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
            definition.withArray(context.getKeyword(SchemaKeyword.TAG_REQUIRED)).add(typeIdentifier);
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
            additionalPart.withArray(context.getKeyword(SchemaKeyword.TAG_REQUIRED))
                    .add(propertyName);
            break;
        default:
            return null;
        }
        return definition;
    }
}
