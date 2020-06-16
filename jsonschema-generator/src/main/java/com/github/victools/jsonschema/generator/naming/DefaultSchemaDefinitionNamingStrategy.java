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

package com.github.victools.jsonschema.generator.naming;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;

/**
 * Default implementation of a {@link SchemaDefinitionNamingStrategy} using the output of {@link TypeContext#getSimpleTypeDescription(ResolvedType)}
 * as definition name/key.
 */
public class DefaultSchemaDefinitionNamingStrategy implements SchemaDefinitionNamingStrategy {

    @Override
    public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
        TypeContext typeContext = generationContext.getTypeContext();
        ResolvedType type = key.getType();
        return typeContext.getSimpleTypeDescription(type);
    }
}
