/*
 * Copyright 2025 Christian Scheer.
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

package com.github.victools.jsonschema.plugin.maven;

import com.github.victools.jsonschema.generator.CustomModule;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

import java.util.List;

public class TestCustomModule implements CustomModule {
    private String skipFieldsStartingWith;

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields().withIgnoreCheck(field -> field.getName().startsWith(skipFieldsStartingWith));
    }

    @Override
    public void setOptions(List<String> options) {
        if (options.size() > 1) {
            throw new IllegalArgumentException("More than one option specified");
        }
        this.skipFieldsStartingWith = options.get(0);
    }
}
