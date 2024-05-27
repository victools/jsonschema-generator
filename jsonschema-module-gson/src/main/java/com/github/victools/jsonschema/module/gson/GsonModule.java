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

package com.github.victools.jsonschema.module.gson;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.Optional;

/**
 * Module for setting up schema generation aspects based on {@code gson-annotations}.
 * <ul>
 * <li>Apply alternative property names defined in {@link SerializedName} annotations.</li>
 * <li>Exclude properties that are deemed to be ignored per the use of {@link Expose}.</li>
 * </ul>
 */
public class GsonModule implements Module {

    /**
     * Constructor, without any additional options.
     *
     * @see GsonModule
     */
    public GsonModule() {
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        //if the field should be included yes/no based on GSON Expose declaration
        builder.forFields().withIgnoreCheck(field -> {
            Expose annotation = field.getAnnotationConsideringFieldAndGetter(Expose.class);
            return annotation != null && !annotation.serialize();
        });

        //Take the property name from the GSON SerializedName declaration
        builder.forFields().withPropertyNameOverrideResolver(
                field -> Optional.ofNullable(field.getAnnotationConsideringFieldAndGetter(SerializedName.class))
                        .map(SerializedName::value).orElse(null));
    }
}
