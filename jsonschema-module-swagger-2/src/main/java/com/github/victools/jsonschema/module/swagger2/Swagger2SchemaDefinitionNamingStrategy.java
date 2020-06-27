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

package com.github.victools.jsonschema.module.swagger2;

import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.DefaultSchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Naming strategy for the keys in the {@code definitions}/{@code $defs} of the produced schema, based on {@code @Schema(name = ...)}.
 */
public class Swagger2SchemaDefinitionNamingStrategy implements SchemaDefinitionNamingStrategy {

    private final SchemaDefinitionNamingStrategy baseStrategy;

    /**
     * Constructor expecting a base strategy to be applied if there is no {@link Schema} annotation with a specific {@code name} being specified.
     *
     * @param baseStrategy fall-back strategy to be applied
     */
    public Swagger2SchemaDefinitionNamingStrategy(SchemaDefinitionNamingStrategy baseStrategy) {
        if (baseStrategy == null) {
            this.baseStrategy = new DefaultSchemaDefinitionNamingStrategy();
        } else {
            this.baseStrategy = baseStrategy;
        }
    }

    @Override
    public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
        Schema annotation = key.getType().getErasedType().getAnnotation(Schema.class);
        if (annotation == null || annotation.name().isEmpty()) {
            return this.baseStrategy.getDefinitionNameForKey(key, generationContext);
        }
        return annotation.name();
    }

    @Override
    public void adjustDuplicateNames(Map<DefinitionKey, String> subschemasWithDuplicateNames, SchemaGenerationContext generationContext) {
        this.baseStrategy.adjustDuplicateNames(subschemasWithDuplicateNames, generationContext);
    }

    @Override
    public String adjustNullableName(DefinitionKey key, String definitionName, SchemaGenerationContext generationContext) {
        return this.baseStrategy.adjustNullableName(key, definitionName, generationContext);
    }
}
