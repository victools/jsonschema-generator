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

/**
 * Module for setting up schema generation aspects based on {@code jackson-annotations}.
 *
 * @see JacksonSchemaModule
 */
@Deprecated(forRemoval = true, since = "5.0.0")
public class JacksonModule extends JacksonSchemaModule {

    /**
     * Constructor, without any additional options.
     *
     * @see JacksonSchemaModule#JacksonSchemaModule(JacksonOption...)
     */
    public JacksonModule() {
        super();
    }

    /**
     * Constructor.
     *
     * @param options features to enable
     */
    public JacksonModule(JacksonOption... options) {
        super(options);
    }
}
