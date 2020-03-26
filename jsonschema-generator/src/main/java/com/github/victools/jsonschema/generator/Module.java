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

/**
 * Collection of configurations for the schema generation.
 */
public interface Module {

    /**
     * Apply this module to the given configuration builder instance.
     *
     * @param builder configuration builder instance to which to apply this module
     */
    void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder);
}
