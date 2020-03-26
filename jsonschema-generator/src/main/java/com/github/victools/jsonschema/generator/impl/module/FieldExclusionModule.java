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

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import java.util.function.Predicate;

/**
 * Default module for excluding fields.
 */
public class FieldExclusionModule implements Module {

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all {@code public} non-{@code static} fields.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forPublicNonStaticFields() {
        return new FieldExclusionModule(
                field -> field.isPublic() && !field.isStatic());
    }

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all non-{@code public} non-{@code static} fields that also have
     * an associated getter method.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forNonPublicNonStaticFieldsWithGetter() {
        return new FieldExclusionModule(
                field -> !field.isPublic() && !field.isStatic() && field.hasGetter());
    }

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all non-{@code public} non-{@code static} fields that do not
     * have an associate getter method.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forNonPublicNonStaticFieldsWithoutGetter() {
        return new FieldExclusionModule(
                field -> !field.isPublic() && !field.isStatic() && !field.hasGetter());
    }

    /**
     * Factory method: creating a {@link FieldExclusionModule} instance that excludes all {@code public} {@code static} fields.
     *
     * @return created module instance
     */
    public static FieldExclusionModule forTransientFields() {
        return new FieldExclusionModule(FieldScope::isTransient);
    }

    private final Predicate<FieldScope> shouldExcludeFieldsMatching;

    /**
     * Constructor setting the underlying check to be set via {@link SchemaGeneratorConfigPart#withIgnoreCheck(Predicate)}.
     *
     * @param shouldExcludeFieldsMatching check to identify fields to be excluded
     * @see SchemaGeneratorConfigBuilder#forFields()
     * @see SchemaGeneratorConfigPart#withIgnoreCheck(Predicate)
     */
    public FieldExclusionModule(Predicate<FieldScope> shouldExcludeFieldsMatching) {
        this.shouldExcludeFieldsMatching = shouldExcludeFieldsMatching;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withIgnoreCheck(this.shouldExcludeFieldsMatching);
    }
}
