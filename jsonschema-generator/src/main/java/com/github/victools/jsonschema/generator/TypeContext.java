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

import com.fasterxml.classmate.AnnotationConfiguration;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Context in which types can be resolved (as well as their declared fields and methods).
 */
public class TypeContext {

    protected static final Predicate<Annotation> IGNORE_ANNOTATIONS_ON_ANNOTATIONS = _a -> false;

    private final TypeResolver typeResolver;
    private final MemberResolver memberResolver;
    private final WeakHashMap<ResolvedType, ResolvedTypeWithMembers> typesWithMembersCache;
    private final AnnotationConfiguration annotationConfig;
    private final boolean derivingFieldsFromArgumentFreeMethods;

    /**
     * Constructor.
     *
     * @param annotationConfig annotation configuration to apply when collecting resolved fields and methods
     *
     * @deprecated use {@link #TypeContext(AnnotationConfiguration, SchemaGeneratorConfig)} instead
     */
    @Deprecated
    public TypeContext(AnnotationConfiguration annotationConfig) {
        this(annotationConfig, false);
    }

    /**
     * Constructor.
     * <br>
     * Note: when providing an instance of {@link AnnotationConfiguration.StdConfiguration} as first parameter, any configured annotation inclusion
     * overrides are applied automatically.
     *
     * @param annotationConfig annotation configuration to apply when collecting resolved fields and methods
     * @param generatorConfig generator configuration indicating whether argument free methods should be represented as fields
     */
    public TypeContext(AnnotationConfiguration annotationConfig, SchemaGeneratorConfig generatorConfig) {
        this(annotationConfig, generatorConfig.shouldDeriveFieldsFromArgumentFreeMethods());
        if (annotationConfig instanceof AnnotationConfiguration.StdConfiguration) {
            generatorConfig.getAnnotationInclusionOverrides()
                    .forEach(((AnnotationConfiguration.StdConfiguration) annotationConfig)::setInclusion);
        }
    }

    /**
     * Constructor.
     *
     * @param annotationConfig annotation configuration to apply when collecting resolved fields and methods
     * @param derivingFieldsFromArgumentFreeMethods whether argument free methods should be represented as fields
     */
    private TypeContext(AnnotationConfiguration annotationConfig, boolean derivingFieldsFromArgumentFreeMethods) {
        this.typeResolver = new TypeResolver();
        this.memberResolver = new MemberResolver(this.typeResolver);
        this.annotationConfig = annotationConfig;
        this.derivingFieldsFromArgumentFreeMethods = derivingFieldsFromArgumentFreeMethods;
        this.typesWithMembersCache = new WeakHashMap<>();
    }

    /**
     * Getter for the flag indicating whether to derive fields from argument-free methods.
     *
     * @return whether argument-free methods should be represented as fields
     */
    public boolean isDerivingFieldsFromArgumentFreeMethods() {
        return this.derivingFieldsFromArgumentFreeMethods;
    }

    /**
     * Resolve actual type (mostly relevant for parameterised types, type variables and such).
     *
     * @param type java type to resolve
     * @param typeParameters (optional) type parameters to pass on
     * @return resolved type
     * @see TypeResolver#resolve(Type, Type...)
     */
    public final ResolvedType resolve(Type type, Type... typeParameters) {
        return this.typeResolver.resolve(type, typeParameters);
    }

    /**
     * Resolve subtype considering the given super-types (potentially) known type parameters.
     *
     * @param supertype already resolved super type
     * @param subtype erased java subtype to resolve
     * @return resolved subtype
     * @see TypeResolver#resolveSubtype(ResolvedType, Class)
     */
    public final ResolvedType resolveSubtype(ResolvedType supertype, Class<?> subtype) {
        return this.typeResolver.resolveSubtype(supertype, subtype);
    }

    /**
     * Collect a given type's declared fields and methods.
     *
     * @param resolvedType type for which to collect declared fields and methods
     * @return collection of (resolved) fields and methods
     */
    public final ResolvedTypeWithMembers resolveWithMembers(ResolvedType resolvedType) {
        return this.typesWithMembersCache.computeIfAbsent(resolvedType, this::resolveWithMembersForCache);
    }

    /**
     * Collect a given type's declared fields and methods for the inclusion in the internal cache.
     *
     * @param resolvedType type for which to collect declared fields and methods
     * @return collection of (resolved) fields and methods
     */
    private ResolvedTypeWithMembers resolveWithMembersForCache(ResolvedType resolvedType) {
        return this.memberResolver.resolve(resolvedType, this.annotationConfig, null);
    }

    /**
     * Construct a {@link FieldScope} instance for the given field.
     *
     * @param field targeted field
     * @param declaringTypeMembers collection of the declaring type's (other) fields and methods
     * @return created {@link FieldScope} instance
     * @deprecated use {@link #createFieldScope(ResolvedField, MemberScope.DeclarationDetails)} instead
     */
    @Deprecated
    public FieldScope createFieldScope(ResolvedField field, ResolvedTypeWithMembers declaringTypeMembers) {
        return this.createFieldScope(field, new MemberScope.DeclarationDetails(field.getDeclaringType(), declaringTypeMembers));
    }

    /**
     * Construct a {@link FieldScope} instance for the given field.
     *
     * @param field targeted field
     * @param declarationDetails context of declaration
     * @return created {@link FieldScope} instance
     *
     * @since 4.33.0
     */
    public FieldScope createFieldScope(ResolvedField field, MemberScope.DeclarationDetails declarationDetails) {
        return new FieldScope(field, declarationDetails, null, this);
    }

    /**
     * Construct a {@link MethodScope} instance for the given method.
     *
     * @param method targeted method
     * @param declaringTypeMembers collection of the declaring type's fields and (other) methods
     * @return created {@link MethodScope} instance
     * @deprecated use {@link #createMethodScope(ResolvedMethod, MemberScope.DeclarationDetails)} instead
     */
    @Deprecated
    public MethodScope createMethodScope(ResolvedMethod method, ResolvedTypeWithMembers declaringTypeMembers) {
        return this.createMethodScope(method, new MemberScope.DeclarationDetails(method.getDeclaringType(), declaringTypeMembers));
    }

    /**
     * Construct a {@link MethodScope} instance for the given method.
     *
     * @param method targeted method
     * @param declarationDetails context of declaration
     * @return created {@link MethodScope} instance
     *
     * @since 4.33.0
     */
    public MethodScope createMethodScope(ResolvedMethod method, MemberScope.DeclarationDetails declarationDetails) {
        return new MethodScope(method, declarationDetails, null, this);
    }

    /**
     * Construct a {@link TypeScope} instance for the type.
     *
     * @param type targeted type
     * @return created {@link TypeScope} instance
     */
    public TypeScope createTypeScope(ResolvedType type) {
        return new TypeScope(type, this);
    }

    /**
     * Find type parameterization for the specified (super) type at return the type parameter at the given index.
     *
     * @param type type to find type parameter for
     * @param erasedSuperType (super) type to find declared type parameter for
     * @param parameterIndex index of the single type parameter's declared type to return
     * @return declared parameter type; or {@code Object.class} if no parameters are defined; or null if the given type or index are invalid
     * @see ResolvedType#typeParametersFor(Class)
     */
    public ResolvedType getTypeParameterFor(ResolvedType type, Class<?> erasedSuperType, int parameterIndex) {
        List<ResolvedType> typeParameters = type.typeParametersFor(erasedSuperType);
        if (typeParameters == null) {
            // given type is not a super type of the type in scope
            return null;
        }
        if (!typeParameters.isEmpty() && typeParameters.size() <= parameterIndex) {
            // given index is out of bounds for the specific type
        }
        if (typeParameters.isEmpty() && erasedSuperType.getTypeParameters().length <= parameterIndex) {
            // given index is out of bounds for the designated super type
            return null;
        }
        if (typeParameters.isEmpty()) {
            // no type parameters are defined, for simplicity's sake not trying to resolve declared boundaries
            return this.resolve(Object.class);
        }
        return typeParameters.get(parameterIndex);
    }

    /**
     * Determine whether a given type should be treated as {@link SchemaKeyword#TAG_TYPE_ARRAY} in the generated schema.
     *
     * @param type type to check
     * @return whether the given type is an array or subtype of {@link Collection}
     */
    public boolean isContainerType(ResolvedType type) {
        return type.isArray() || type.isInstanceOf(Collection.class);
    }

    /**
     * Identify the element/item type of the given {@link SchemaKeyword#TAG_TYPE_ARRAY}.
     *
     * @param containerType type to extract type of element/item from
     * @return type of elements/items
     * @see #isContainerType(ResolvedType)
     */
    public ResolvedType getContainerItemType(ResolvedType containerType) {
        ResolvedType itemType = containerType.getArrayElementType();
        if (itemType == null && this.isContainerType(containerType)) {
            itemType = this.getTypeParameterFor(containerType, Iterable.class, 0);
        }
        return itemType;
    }

    /**
     * Select the instance of the specified annotation type from the given list. Also considering meta annotations (i.e., annotations on annotations)
     * if a meta annotation is deemed eligible according to the given {@code Predicate}.
     *
     * @param <A> type of annotation
     * @param annotationClass type of annotation
     * @param annotationList initial list of annotations to search in (potentially containing meta annotations)
     * @param considerOtherAnnotation check whether some other annotation should also be checked for holding an instance of the target annotation
     * @return annotation instance (or {@code null} if no annotation of the given type is present)
     *
     * @since 4.30.0
     */
    public <A extends Annotation> A getAnnotationFromList(Class<A> annotationClass, List<Annotation> annotationList,
            Predicate<Annotation> considerOtherAnnotation) {
        List<Annotation> annotations = annotationList;
        while (!annotations.isEmpty()) {
            Optional<Annotation> nestedAnnotation = annotations.stream()
                    .filter(annotationClass::isInstance)
                    .findFirst();
            if (nestedAnnotation.isPresent()) {
                return nestedAnnotation.map(annotationClass::cast).get();
            }
            annotations = annotations.stream()
                    .filter(considerOtherAnnotation)
                    .flatMap(otherAnnotation -> Stream.of(otherAnnotation.annotationType().getAnnotations()))
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * Return the annotation of the given type from the annotated container's item, if such an annotation is present.
     *
     * @param <A> type of annotation
     * @param annotationClass type of annotation
     * @param annotatedContainerType annotated container type that is considered if it is an {@link AnnotatedParameterizedType}
     * @param containerItemIndex parameter index of the desired item on the container type
     * @return annotation instance (or {@code null} if no annotation of the given type is present)
     *
     * @deprecated use {@link #getTypeParameterAnnotation(Class, Predicate, AnnotatedType, Integer)} instead
     */
    @Deprecated
    public <A extends Annotation> A getTypeParameterAnnotation(Class<A> annotationClass, AnnotatedType annotatedContainerType,
            Integer containerItemIndex) {
        return this.getTypeParameterAnnotation(annotationClass, IGNORE_ANNOTATIONS_ON_ANNOTATIONS, annotatedContainerType, containerItemIndex);
    }

    /**
     * Return the annotation of the given type from the annotated container's item, if such an annotation is present.
     * <br>
     * Additionally, also consider annotations on annotations, if the given predicate indicates another annotation as eligible for holding the target.
     *
     * @param <A> type of annotation
     * @param annotationClass type of annotation
     * @param considerOtherAnnotation check whether some other annotation should also be checked for holding an instance of the target annotation
     * @param annotatedContainerType annotated container type that is considered if it is an {@link AnnotatedParameterizedType}
     * @param containerItemIndex parameter index of the desired item on the container type
     * @return annotation instance (or {@code null} if no annotation of the given type is present)
     *
     * @since 4.30.0
     */
    public <A extends Annotation> A getTypeParameterAnnotation(Class<A> annotationClass, Predicate<Annotation> considerOtherAnnotation,
                AnnotatedType annotatedContainerType, Integer containerItemIndex) {
        List<Annotation> annotationList = this.getTypeParameterAnnotations(annotatedContainerType, containerItemIndex)
                .collect(Collectors.toList());
        return this.getAnnotationFromList(annotationClass, annotationList, considerOtherAnnotation);
    }

    /**
     * Return the annotations from the annotated container's item.
     *
     * @param annotatedContainerType annotated container type that is considered if it is an {@link AnnotatedParameterizedType}
     * @param containerItemIndex parameter index of the desired item on the container type
     * @return annotation instances (possibly empty if no annotation is present)
     *
     * @since 4.30.0
     */
    public Stream<Annotation> getTypeParameterAnnotations(AnnotatedType annotatedContainerType, Integer containerItemIndex) {
        if (annotatedContainerType instanceof AnnotatedParameterizedType) {
            AnnotatedType[] typeArguments = ((AnnotatedParameterizedType) annotatedContainerType).getAnnotatedActualTypeArguments();
            int itemIndex = containerItemIndex == null ? 0 : containerItemIndex;
            if (typeArguments.length > itemIndex) {
                return Stream.of(typeArguments[itemIndex].getAnnotations());
            }
        }
        return Stream.empty();
    }

    /**
     * Find the (super) type, including interfaces, that has the designated type annotation.
     *
     * @param targetType type to check for the annotation, potentially iterating over its declared interfaces and super types
     * @param annotationType type of annotation to look for
     * @return (super) type with the targeted annotation (or {@code null} if the annotation wasn't found on any of its interfaces or super types)
     *
     * @since 4.28.0
     */
    public ResolvedType getTypeWithAnnotation(ResolvedType targetType, Class<? extends Annotation> annotationType) {
        return this.getTypeWithAnnotation(targetType, annotationType, TypeContext.IGNORE_ANNOTATIONS_ON_ANNOTATIONS);
    }

    /**
     * Find the (super) type, including interfaces, that has the designated type annotation.
     * <br>
     * Additionally, also consider annotations on annotations, if the given predicate indicates another annotation as eligible for holding the target.
     *
     * @param targetType type to check for the annotation, potentially iterating over its declared interfaces and super types
     * @param annotationType type of annotation to look for
     * @param considerOtherAnnotation check whether some other annotation should also be checked for holding an instance of the target annotation
     * @return (super) type with the targeted annotation (or {@code null} if the annotation wasn't found on any of its interfaces or super types)
     *
     * @since 4.30.0
     */
    public ResolvedType getTypeWithAnnotation(ResolvedType targetType, Class<? extends Annotation> annotationType,
            Predicate<Annotation> considerOtherAnnotation) {
        return this.getTypeConsideringHierarchyMatching(targetType, type -> this.getAnnotationFromList(annotationType,
                Arrays.asList(type.getErasedType().getAnnotations()), considerOtherAnnotation) != null);
    }

    /**
     * Find the (super) type, including interfaces, that fulfils the given condition/check..
     *
     * @param targetType type to check for the annotation, potentially iterating over its declared interfaces and super types
     * @param check condition to be fulfilled
     * @return (super) type fulfilling the given condition (or {@code null} if the condition wasn't fulfilled by any of its interfaces or super types)
     *
     * @since 4.30.0
     */
    public ResolvedType getTypeConsideringHierarchyMatching(ResolvedType targetType, Predicate<ResolvedType> check) {
        ResolvedType targetSuperType = targetType;
        do {
            if (check.test(targetSuperType)) {
                return targetSuperType;
            }
            Optional<ResolvedType> interfaceWithAnnotation = targetSuperType.getImplementedInterfaces().stream()
                    .filter(check)
                    .findFirst();
            if (interfaceWithAnnotation.isPresent()) {
                return interfaceWithAnnotation.get();
            }
            targetSuperType = targetSuperType.getParentClass();
        } while (targetSuperType != null);
        return null;
    }

    /**
     * Look-up the given annotation on the targeted type or one of its declared interfaces or super types.
     *
     * @param <A> type of annotation to look for
     * @param targetType type to find annotation on (or on any of its declared interfaces or super types)
     * @param annotationType type of annotation to look for
     * @return annotation instance (or {@code null} if not found)
     *
     * @since 4.28.0
     */
    public <A extends Annotation> A getTypeAnnotationConsideringHierarchy(ResolvedType targetType, Class<A> annotationType) {
        ResolvedType typeWithAnnotation = this.getTypeWithAnnotation(targetType, annotationType);
        if (typeWithAnnotation == null) {
            return null;
        }
        return typeWithAnnotation.getErasedType().getAnnotation(annotationType);
    }

    /**
     * Constructing a string that represents the given type (including possible type parameters and their actual types).
     * <br>
     * This calls {@link Class#getSimpleName()} for a single erased type - i.e. excluding package names.
     *
     * @param type the type to represent
     * @return resulting string
     */
    public final String getSimpleTypeDescription(ResolvedType type) {
        return this.getTypeDescription(type, true);
    }

    /**
     * Constructing a string that fully represents the given type (including possible type parameters and their actual types).
     * <br>
     * This calls {@link Class#getName()} for a single erased type.
     *
     * @param type the type to represent
     * @return resulting string
     */
    public final String getFullTypeDescription(ResolvedType type) {
        return this.getTypeDescription(type, false);
    }

    /**
     * Constructing a string that fully represents the given type (including possible type parameters and their actual types).
     *
     * @param type the type to represent
     * @param simpleClassNames whether simple class names should be used (otherwise: full package names are included)
     * @return resulting string
     */
    private String getTypeDescription(ResolvedType type, boolean simpleClassNames) {
        Class<?> erasedType = type.getErasedType();
        String result = simpleClassNames ? erasedType.getSimpleName() : erasedType.getTypeName();
        List<ResolvedType> typeParameters = type.getTypeParameters();
        if (!typeParameters.isEmpty()) {
            result += typeParameters.stream()
                    .map(parameterType -> this.getTypeDescription(parameterType, simpleClassNames))
                    .collect(Collectors.joining(", ", "<", ">"));
        }
        return result;
    }

    /**
     * Returns the type description for an argument in a method's property name.
     *
     * @param type method argument's type to represent
     * @return argument type description
     */
    public String getMethodPropertyArgumentTypeDescription(ResolvedType type) {
        return this.getSimpleTypeDescription(type);
    }

    /**
     * Helper function to write generic code that targets either a {@link FieldScope} or {@link MethodScope}.
     *
     * @param <R> type of expected return value
     * @param member field or method being targeted
     * @param fieldAction action to perform in case the given member is a {@link FieldScope}
     * @param methodAction action to perform in case the given member is a {@link MethodScope}
     * @return value returned by the performed action
     * @throws IllegalStateException if given member is neither a {@link FieldScope} or {@link MethodScope}
     */
    public <R> R performActionOnMember(MemberScope<?, ?> member, Function<FieldScope, R> fieldAction,
            Function<MethodScope, R> methodAction) {
        R result;
        if (member instanceof FieldScope) {
            result = fieldAction.apply((FieldScope) member);
        } else if (member instanceof MethodScope) {
            result = methodAction.apply((MethodScope) member);
        } else {
            throw new IllegalStateException("Unsupported member scope of type: " + member.getClass());
        }
        return result;
    }
}
