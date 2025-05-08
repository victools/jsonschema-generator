/*
 * Copyright 2019-2025 VicTools.
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
import com.fasterxml.classmate.members.HierarchicType;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.victools.jsonschema.generator.AnnotationHelper;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Module for setting up schema generation aspects based on {@code jackson-annotations}.
 * <ul>
 * <li>Populate the "description" attributes as per {@link JsonPropertyDescription} and {@link JsonClassDescription} annotations.</li>
 * <li>Apply alternative property names defined in {@link JsonProperty} annotations.</li>
 * <li>Exclude properties that are deemed to be ignored per the various annotations for that purpose.</li>
 * <li>Optionally: treat enum types as plain strings as per {@link com.fasterxml.jackson.annotation.JsonValue JsonValue} annotations.</li>
 * </ul>
 */
public class JacksonModule implements Module {

    static final Predicate<Annotation> NESTED_ANNOTATION_CHECK = annotation ->
            annotation.annotationType().isAnnotationPresent(JacksonAnnotationsInside.class);

    private final Set<JacksonOption> options;
    private ObjectMapper objectMapper;
    private final Map<Class<?>, BeanDescription> beanDescriptions = Collections.synchronizedMap(new HashMap<>());
    private final Map<Class<?>, PropertyNamingStrategy> namingStrategies = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructor, without any additional options.
     *
     * @see JacksonModule#JacksonModule(JacksonOption...)
     */
    public JacksonModule() {
        this.options = Collections.emptySet();
    }

    /**
     * Constructor.
     *
     * @param options features to enable
     */
    public JacksonModule(JacksonOption... options) {
        this.options = options == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(options));
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        this.objectMapper = builder.getObjectMapper();
        SchemaGeneratorConfigPart<FieldScope> fieldConfigPart = builder.forFields();
        SchemaGeneratorConfigPart<MethodScope> methodConfigPart = builder.forMethods();

        this.applyToConfigBuilderPart(fieldConfigPart);
        this.applyToConfigBuilderPart(methodConfigPart);

        fieldConfigPart.withIgnoreCheck(this::shouldIgnoreField);
        methodConfigPart.withIgnoreCheck(this::shouldIgnoreMethod);
        if (!this.options.contains(JacksonOption.IGNORE_PROPERTY_NAMING_STRATEGY)) {
            // only consider @JsonNaming as fall-back
            fieldConfigPart.withPropertyNameOverrideResolver(this::getPropertyNameOverrideBasedOnJsonNamingAnnotation);
        }

        SchemaGeneratorGeneralConfigPart generalConfigPart = builder.forTypesInGeneral();
        generalConfigPart.withDescriptionResolver(this::resolveDescriptionForType);

        boolean considerEnumJsonValue = this.options.contains(JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE);
        boolean considerEnumJsonProperty = this.options.contains(JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY);
        if (considerEnumJsonValue || considerEnumJsonProperty) {
            generalConfigPart.withCustomDefinitionProvider(new CustomEnumDefinitionProvider(considerEnumJsonValue, considerEnumJsonProperty));
        }

        if (this.options.contains(JacksonOption.RESPECT_JSONPROPERTY_ORDER)) {
            generalConfigPart.withPropertySorter(new JsonPropertySorter(true));
        }
        if (this.options.contains(JacksonOption.JSONIDENTITY_REFERENCE_ALWAYS_AS_ID)) {
            JsonIdentityReferenceDefinitionProvider identityReferenceDefinitionProvider = new JsonIdentityReferenceDefinitionProvider();
            generalConfigPart.withCustomDefinitionProvider(identityReferenceDefinitionProvider);
            fieldConfigPart.withCustomDefinitionProvider(identityReferenceDefinitionProvider::provideCustomPropertySchemaDefinition);
            methodConfigPart.withCustomDefinitionProvider(identityReferenceDefinitionProvider::provideCustomPropertySchemaDefinition);
        }

        applySubtypeResolverToConfigBuilder(generalConfigPart, fieldConfigPart, methodConfigPart);

        generalConfigPart.withCustomDefinitionProvider(new JsonUnwrappedDefinitionProvider());
    }

    private void applySubtypeResolverToConfigBuilder(SchemaGeneratorGeneralConfigPart generalConfigPart,
            SchemaGeneratorConfigPart<FieldScope> fieldConfigPart, SchemaGeneratorConfigPart<MethodScope> methodConfigPart) {
        boolean skipLookUpSubtypes = this.options.contains(JacksonOption.SKIP_SUBTYPE_LOOKUP);
        boolean skipTypeInfoTransform = this.options.contains(JacksonOption.IGNORE_TYPE_INFO_TRANSFORM);
        if (skipLookUpSubtypes && skipTypeInfoTransform) {
            return;
        }
        JsonSubTypesResolver subtypeResolver = new JsonSubTypesResolver(this.options);
        if (!skipLookUpSubtypes) {
            generalConfigPart.withSubtypeResolver(subtypeResolver);
            fieldConfigPart.withTargetTypeOverridesResolver(subtypeResolver::findTargetTypeOverrides);
            methodConfigPart.withTargetTypeOverridesResolver(subtypeResolver::findTargetTypeOverrides);
        }
        if (!skipTypeInfoTransform) {
            generalConfigPart.withCustomDefinitionProvider(subtypeResolver);
            fieldConfigPart.withCustomDefinitionProvider(subtypeResolver::provideCustomPropertySchemaDefinition);
            methodConfigPart.withCustomDefinitionProvider(subtypeResolver::provideCustomPropertySchemaDefinition);
        }
    }

    /**
     * Apply common member configurations.
     *
     * @param configPart config builder part for either fields or methods
     */
    private void applyToConfigBuilderPart(SchemaGeneratorConfigPart<?> configPart) {
        configPart.withDescriptionResolver(this::resolveDescription);
        configPart.withPropertyNameOverrideResolver(this::getPropertyNameOverrideBasedOnJsonPropertyAnnotation);
        configPart.withReadOnlyCheck(this::getReadOnlyCheck);
        configPart.withWriteOnlyCheck(this::getWriteOnlyCheck);

        if (this.options.contains(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED)) {
            configPart.withRequiredCheck(this::getRequiredCheckBasedOnJsonPropertyAnnotation);
        }
    }

    /**
     * Determine the given member's associated "description" in the following order of priority.
     * <ol>
     * <li>{@link JsonPropertyDescription} annotation on the field/method itself</li>
     * <li>{@link JsonPropertyDescription} annotation on the field's getter method or the getter method's associated field</li>
     * </ol>
     *
     * @param member field/method for which to collect an available description
     * @return successfully looked-up description (or {@code null})
     */
    protected String resolveDescription(MemberScope<?, ?> member) {
        // look for property specific description
        JsonPropertyDescription propertyAnnotation = member.getAnnotationConsideringFieldAndGetterIfSupported(JsonPropertyDescription.class,
                NESTED_ANNOTATION_CHECK);
        if (propertyAnnotation != null) {
            return propertyAnnotation.value();
        }
        return null;
    }

    /**
     * Determine the given type's associated "description" via the following annotation.
     * <ul>
     * <li>{@link JsonClassDescription} annotation on the targeted type's class</li>
     * </ul>
     *
     * @param scope scope for which to collect an available description
     * @return successfully looked-up description (or {@code null})
     */
    protected String resolveDescriptionForType(TypeScope scope) {
        Class<?> rawType = scope.getType().getErasedType();
        return AnnotationHelper.resolveAnnotation(rawType, JsonClassDescription.class, NESTED_ANNOTATION_CHECK)
                .map(JsonClassDescription::value)
                .orElse(null);
    }

    /**
     * Look-up an alternative name as per the following order of priority.
     * <ol>
     * <li>{@link JsonProperty} annotation on the member itself</li>
     * <li>{@link JsonProperty} annotation on the field's getter method or the getter method's associated field</li>
     * </ol>
     *
     * @param member field/method to look-up alternative property name for
     * @return alternative property name (or {@code null})
     */
    protected String getPropertyNameOverrideBasedOnJsonPropertyAnnotation(MemberScope<?, ?> member) {
        JsonProperty annotation = member.getAnnotationConsideringFieldAndGetter(JsonProperty.class, NESTED_ANNOTATION_CHECK);
        if (annotation != null) {
            String nameOverride = annotation.value();
            // check for invalid overrides
            if (nameOverride != null && !nameOverride.isEmpty() && !nameOverride.equals(member.getDeclaredName())) {
                return nameOverride;
            }
        }
        return null;
    }

    /**
     * Alter the declaring name of the given field as per the declaring type's {@link JsonNaming} annotation.
     *
     * @param field field to look-up naming strategy for
     * @return altered property name (or {@code null})
     */
    protected String getPropertyNameOverrideBasedOnJsonNamingAnnotation(FieldScope field) {
        final PropertyNamingStrategy strategy;
        synchronized (this.namingStrategies) {
            strategy = this.namingStrategies.computeIfAbsent(field.getDeclaringType().getErasedType(),
                    this::getAnnotatedNamingStrategy);
        }
        if (strategy == null) {
            return null;
        }
        return strategy.nameForField(null, null, field.getName());
    }

    /**
     * Look-up the given type's {@link JsonNaming} annotation and instantiate the declared {@link PropertyNamingStrategy}.
     *
     * @param declaringType type declaring fields for which the applicable naming strategy should be looked-up
     * @return annotated naming strategy instance (or {@code null})
     */
    private PropertyNamingStrategy getAnnotatedNamingStrategy(Class<?> declaringType) {
        return AnnotationHelper.resolveAnnotation(declaringType, JsonNaming.class, NESTED_ANNOTATION_CHECK)
                .map(JsonNaming::value)
                .map(strategyType -> {
                    try {
                        return strategyType.getConstructor().newInstance();
                    } catch (ReflectiveOperationException | SecurityException ex) {
                        return null;
                    }
                })
                .orElse(null);
    }

    /**
     * Create a jackson {@link BeanDescription} for the given type's erased class in order to avoid having to re-create the complexity therein.
     * <br>
     * This is assumed to have a negative performance impact (as one type is being introspected twice), that should be fine for schema generation.
     *
     * @param targetType type for whose erased class the {@link BeanDescription} should be created
     * @return introspection result of given type's erased class
     */
    protected final BeanDescription getBeanDescriptionForClass(ResolvedType targetType) {
        // use a map to cater for some caching (and thereby performance improvement)
        synchronized (this.beanDescriptions) {
            return this.beanDescriptions.computeIfAbsent(targetType.getErasedType(),
                    type -> this.objectMapper.getSerializationConfig().introspect(this.objectMapper.getTypeFactory().constructType(type)));
        }
    }

    /**
     * Determine whether a given field should be ignored, according to various jackson annotations for that purpose,
     * <br>
     * e.g. {@code JsonBackReference}, {@code JsonIgnore}, {@code JsonIgnoreType}, {@code JsonIgnoreProperties}
     *
     * @param field field to check
     * @return whether field should be excluded
     */
    protected boolean shouldIgnoreField(FieldScope field) {
        if (field.getAnnotationConsideringFieldAndGetterIfSupported(JsonBackReference.class, NESTED_ANNOTATION_CHECK) != null) {
            return true;
        }
        // @since 4.32.0
        JsonUnwrapped unwrappedAnnotation = field.getAnnotationConsideringFieldAndGetterIfSupported(JsonUnwrapped.class, NESTED_ANNOTATION_CHECK);
        if (unwrappedAnnotation != null && unwrappedAnnotation.enabled()) {
            // unwrapped properties should be ignored here, as they are included in their unwrapped form
            return true;
        }
        // instead of re-creating the various ways a property may be included/excluded in jackson: just use its built-in introspection
        HierarchicType topMostHierarchyType = field.getDeclaringTypeMembers().allTypesAndOverrides().get(0);
        BeanDescription beanDescription = this.getBeanDescriptionForClass(topMostHierarchyType.getType());
        // some kinds of field ignorals are only available via an annotation introspector
        Set<String> ignoredProperties = this.objectMapper.getSerializationConfig().getAnnotationIntrospector()
                .findPropertyIgnoralByName(null, beanDescription.getClassInfo()).getIgnored();
        String declaredName = field.getDeclaredName();
        if (ignoredProperties.contains(declaredName)) {
            return true;
        }
        // @since 4.37.0 also consider overridden property name as it may match the getter method
        String fieldName = field.getName();
        // other kinds of field ignorals are handled implicitly, i.e. are only available by way of being absent
        return beanDescription.findProperties().stream()
                .noneMatch(propertyDefinition -> declaredName.equals(propertyDefinition.getInternalName())
                        || fieldName.equals(propertyDefinition.getInternalName()));
    }

    /**
     * Determine whether a given method should be ignored, according to various jackson annotations for that purpose,
     * <br>
     * e.g. {@code JsonBackReference}, {@code JsonIgnore}, {@code JsonIgnoreType}, {@code JsonIgnoreProperties}
     *
     * @param method method to check
     * @return whether method should be excluded
     */
    protected boolean shouldIgnoreMethod(MethodScope method) {
        FieldScope getterField = method.findGetterField();
        if (getterField == null) {
            if (method.getAnnotationConsideringFieldAndGetterIfSupported(JsonBackReference.class, NESTED_ANNOTATION_CHECK) != null) {
                return true;
            }
            // @since 4.32.0
            JsonUnwrapped unwrapped = method.getAnnotationConsideringFieldAndGetterIfSupported(JsonUnwrapped.class, NESTED_ANNOTATION_CHECK);
            if (unwrapped != null && unwrapped.enabled()) {
                // unwrapped properties should be ignored here, as they are included in their unwrapped form
                return true;
            }
        } else if (this.shouldIgnoreField(getterField)) {
            return true;
        }
        return this.options.contains(JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS)
                && method.getAnnotationConsideringFieldAndGetter(JsonProperty.class, NESTED_ANNOTATION_CHECK) == null;
    }

    /**
     * Look-up the given field's/method's {@link JsonProperty} annotation and consider its "required" attribute.
     *
     * @param member field/method to look-up required strategy for
     * @return whether the field should be in the "required" list or not
     */
    protected boolean getRequiredCheckBasedOnJsonPropertyAnnotation(MemberScope<?, ?> member) {
        JsonProperty jsonProperty = member.getAnnotationConsideringFieldAndGetterIfSupported(JsonProperty.class, NESTED_ANNOTATION_CHECK);
        return jsonProperty != null && jsonProperty.required();
    }

    /**
     * Determine whether the given field's/method's {@link JsonProperty} annotation marks it as read-only.
     *
     * @param member field/method to check read-only status for
     * @return whether the field should be marked as read-only
     */
    protected boolean getReadOnlyCheck(MemberScope<?, ?> member) {
        JsonProperty jsonProperty = member.getAnnotationConsideringFieldAndGetter(JsonProperty.class, NESTED_ANNOTATION_CHECK);
        return jsonProperty != null && jsonProperty.access() == JsonProperty.Access.READ_ONLY;
    }

    /**
     * Determine whether the given field's/method's {@link JsonProperty} annotation marks it as write-only.
     *
     * @param member field/method to check write-only status for
     * @return whether the field should be marked as write-only
     */
    protected boolean getWriteOnlyCheck(MemberScope<?, ?> member) {
        JsonProperty jsonProperty = member.getAnnotationConsideringFieldAndGetter(JsonProperty.class, NESTED_ANNOTATION_CHECK);
        return jsonProperty != null && jsonProperty.access() == JsonProperty.Access.WRITE_ONLY;
    }
}
