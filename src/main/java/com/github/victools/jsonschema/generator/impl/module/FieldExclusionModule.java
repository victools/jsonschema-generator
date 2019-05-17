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

package com.github.victools.jsonschema.generator.impl.module;

import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.impl.ReflectionGetterUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 * Default module for excluding fields.
 */
public class FieldExclusionModule implements Module {

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all {@code public} {@code static} fields.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forPublicStaticFields() {
        return new FieldExclusionModule(field -> isFieldPublic(field) && isFieldStatic(field));
    }

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all {@code public} non-{@code static} fields.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forPublicNonStaticFields() {
        return new FieldExclusionModule(field -> isFieldPublic(field) && !isFieldStatic(field));
    }

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all non-{@code public} {@code static} fields.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forNonPublicStaticFields() {
        return new FieldExclusionModule(field -> !isFieldPublic(field) && isFieldStatic(field));
    }

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all non-{@code public} non-{@code static} fields that also have
     * an associated getter method.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forNonPublicNonStaticFieldsWithGetter() {
        return new FieldExclusionModule(field -> !isFieldPublic(field) && !isFieldStatic(field) && ReflectionGetterUtils.hasGetter(field));
    }

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all non-{@code public} non-{@code static} fields that do not
     * have an associate getter method.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forNonPublicNonStaticFieldsWithoutGetter() {
        return new FieldExclusionModule(field -> !isFieldPublic(field) && !isFieldStatic(field) && !ReflectionGetterUtils.hasGetter(field));
    }

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all {@code public} {@code static} fields.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forTransientFields() {
        return new FieldExclusionModule(FieldExclusionModule::isFieldTransient);
    }

    /**
     * Check whether a given field is of {@code public} visibility.
     *
     * @param field field to check
     * @return whether the visibility is {@code public}
     */
    private static boolean isFieldPublic(Field field) {
        return (field.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC;
    }

    /**
     * Check whether a given fiald has the {@code static} modifier.
     *
     * @param field field to check
     * @return whether the {@code static} modifier is present
     */
    private static boolean isFieldStatic(Field field) {
        return (field.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
    }

    /**
     * Check whether a given fiald has the {@code transient} modifier.
     *
     * @param field field to check
     * @return whether the {@code transient} modifier is present
     */
    private static boolean isFieldTransient(Field field) {
        return (field.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT;
    }

    private final Predicate<Field> shouldExcludeFieldsMatching;

    /**
     * Constructor setting the underlying check to be set via {@link SchemaGeneratorConfigPart#withIgnoreCheck(Predicate)}.
     *
     * @param shouldExcludeFieldsMatching check to identify fields to be excluded
     * @see SchemaGeneratorConfigBuilder#forFields()
     * @see SchemaGeneratorConfigPart#withIgnoreCheck(Predicate)
     */
    public FieldExclusionModule(Predicate<Field> shouldExcludeFieldsMatching) {
        this.shouldExcludeFieldsMatching = shouldExcludeFieldsMatching;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withIgnoreCheck(this.shouldExcludeFieldsMatching);
    }
}
