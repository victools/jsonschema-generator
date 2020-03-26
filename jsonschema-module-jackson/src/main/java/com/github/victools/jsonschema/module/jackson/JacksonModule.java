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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.TypeScope;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Module for setting up schema generation aspects based on {@code jackson-annotations}:
 * <ul>
 * <li>Populate the "description" attributes as per {@link JsonPropertyDescription} and {@link JsonClassDescription} annotations.</li>
 * <li>Apply alternative property names defined in {@link JsonProperty} annotations.</li>
 * <li>Exclude properties that are deemed to be ignored per the various annotations for that purpose.</li>
 * <li>Optionally: treat enum types as plain strings as per {@link com.fasterxml.jackson.annotation.JsonValue JsonValue} annotations.</li>
 * </ul>
 */
public class JacksonModule implements Module {

    private final Set<JacksonOption> options;
    private ObjectMapper objectMapper;
    private final Map<Class<?>, BeanDescription> beanDescriptions = new HashMap<>();

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
        fieldConfigPart.withDescriptionResolver(this::resolveDescription)
                .withPropertyNameOverrideResolver(this::getPropertyNameOverride)
                .withIgnoreCheck(this::shouldIgnoreField);
        SchemaGeneratorGeneralConfigPart generalConfigPart = builder.forTypesInGeneral();
        generalConfigPart.withDescriptionResolver(this::resolveDescriptionForType);

        if (this.options.contains(JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE)) {
            generalConfigPart.withCustomDefinitionProvider(new CustomEnumJsonValueDefinitionProvider());
        }
    }

    /**
     * Determine the given type's associated "description" in the following order of priority.
     * <ol>
     * <li>{@link JsonPropertyDescription} annotation on the field itself</li>
     * <li>{@link JsonPropertyDescription} annotation on the field's getter method</li>
     * </ol>
     *
     * @param field field for which to collect an available description
     * @return successfully looked-up description (or {@code null})
     */
    protected String resolveDescription(FieldScope field) {
        // look for property specific description
        JsonPropertyDescription propertyAnnotation = field.getAnnotationConsideringFieldAndGetter(JsonPropertyDescription.class);
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
        JsonClassDescription classAnnotation = rawType.getAnnotation(JsonClassDescription.class);
        if (classAnnotation != null) {
            return classAnnotation.value();
        }
        return null;
    }

    /**
     * Look-up an alternative name as per the following order of priority.
     * <ol>
     * <li>{@link JsonProperty} annotation on the field itself</li>
     * <li>{@link JsonProperty} annotation on the field's getter method</li>
     * </ol>
     *
     * @param field field to look-up alternative property name for
     * @return alternative property name (or {@code null})
     */
    protected String getPropertyNameOverride(FieldScope field) {
        JsonProperty annotation = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
        if (annotation != null) {
            String nameOverride = annotation.value();
            // check for invalid overrides
            if (nameOverride != null && !nameOverride.isEmpty() && !nameOverride.equals(field.getDeclaredName())) {
                return nameOverride;
            }
        }
        return null;
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
        return this.beanDescriptions.computeIfAbsent(targetType.getErasedType(),
                type -> this.objectMapper.getSerializationConfig().introspect(this.objectMapper.getTypeFactory().constructType(type)));
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
        if (field.getAnnotationConsideringFieldAndGetter(JsonBackReference.class) != null) {
            return true;
        }
        // instead of re-creating the various ways a property may be included/excluded in jackson: just use its built-in introspection
        BeanDescription beanDescription = this.getBeanDescriptionForClass(field.getDeclaringType());
        // some kinds of field ignorals are only available via an annotation introspector
        Set<String> ignoredProperties = this.objectMapper.getSerializationConfig().getAnnotationIntrospector()
                .findPropertyIgnorals(beanDescription.getClassInfo()).getIgnored();
        String fieldName = field.getName();
        if (ignoredProperties.contains(fieldName)) {
            return true;
        }
        // other kinds of field ignorals are handled implicitly, i.e. are only available by way of being absent
        return beanDescription.findProperties().stream()
                .noneMatch(propertyDefinition -> fieldName.equals(propertyDefinition.getInternalName()));
    }
}
