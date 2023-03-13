/*
 * Copyright 2023 VicTools.
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
 * Type of configuration (or an aspect of it), that may change during a schema generation and7or remember some kind of state.
 */
public interface StatefulConfig {

    /**
     * Method being invoked after the generation of a single "main" type's schema has been completed. This enables the same {@code SchemaGenerator}
     * instance to be re-used for multiple subsequent executions, even if some aspect of the configuration remembers the original "main" type.
     */
    default void resetAfterSchemaGenerationFinished() {
        // nothing to reset by default
    }
}
