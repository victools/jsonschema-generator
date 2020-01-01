/*
 * Copyright 2020 VicTools.
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

import com.fasterxml.classmate.ResolvedType;

/**
 * Provider of non-standard JSON schema definitions.
 */
public interface CustomDefinitionProviderV2 {

    /**
     * Look-up the non-standard JSON schema definition for a given type. If it returns null, the next definition provider is expected to be applied.
     *
     * @param javaType generic type to provide custom definition for
     * @param context generation context allowing to let the standard generation take over nested parts of the custom definition
     * @return non-standard JSON schema definition (may be null)
     */
    CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context);
}
