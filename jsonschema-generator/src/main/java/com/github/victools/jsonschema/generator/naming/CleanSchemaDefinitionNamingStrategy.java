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

import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import java.util.Map;
import java.util.function.Function;

/**
 * Wrapper for a {@link SchemaDefinitionNamingStrategy} that performs a specific clean-up task on all returned values.
 */
public class CleanSchemaDefinitionNamingStrategy implements SchemaDefinitionNamingStrategy {

    private final SchemaDefinitionNamingStrategy strategy;
    private final Function<String, String> cleanUpTask;

    /**
     * Constructor expecting a naming strategy to be wrapped and the applicable clean-up task.
     *
     * @param strategy definition naming strategy to be wrapped
     * @param cleanUpTask task discarding/replacing illegal characters
     */
    public CleanSchemaDefinitionNamingStrategy(SchemaDefinitionNamingStrategy strategy, Function<String, String> cleanUpTask) {
        this.strategy = strategy;
        this.cleanUpTask = cleanUpTask;
    }

    @Override
    public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
        String output = this.strategy.getDefinitionNameForKey(key, generationContext);
        return this.cleanUpTask.apply(output);
    }

    @Override
    public void adjustDuplicateNames(Map<DefinitionKey, String> subschemasWithDuplicateNames, SchemaGenerationContext generationContext) {
        this.strategy.adjustDuplicateNames(subschemasWithDuplicateNames, generationContext);
        subschemasWithDuplicateNames.entrySet().forEach(entry -> entry.setValue(this.cleanUpTask.apply(entry.getValue())));
    }

    @Override
    public String adjustNullableName(DefinitionKey key, String definitionName, SchemaGenerationContext generationContext) {
        String output = this.strategy.adjustNullableName(key, definitionName, generationContext);
        return this.cleanUpTask.apply(output);
    }
}
