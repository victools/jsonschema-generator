/*
 * Copyright 2024 VicTools.
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

package com.github.victools.jsonschema.examples;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/issues/505">#505</a>.
 * <br/>
 * Demonstrating usage of the {@code SchemaDefinitionNamingStrategy} feature.
 */
public class NamingStrategyExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
        configBuilder.forTypesInGeneral()
                .withDefinitionNamingStrategy(new SuffixRemovalNamingStrategy("Model"));
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(Example.class);
    }

    static class SuffixRemovalNamingStrategy implements SchemaDefinitionNamingStrategy {

        private final String typeSuffixToIgnore;

        SuffixRemovalNamingStrategy(String typeSuffixToIgnore) {
            this.typeSuffixToIgnore = typeSuffixToIgnore;
        }

        @Override
        public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
            return this.getDefinitionNameForType(key.getType());
        }

        private String getDefinitionNameForType(ResolvedType type) {
            Class<?> erasedType = type.getErasedType();
            String definitionName = erasedType.getSimpleName();
            if (definitionName.endsWith(this.typeSuffixToIgnore) && definitionName.length() > this.typeSuffixToIgnore.length()) {
                definitionName = definitionName.substring(0, definitionName.length() - this.typeSuffixToIgnore.length());
            }
            List<ResolvedType> typeParameters = type.getTypeParameters();
            if (!typeParameters.isEmpty()) {
                definitionName += typeParameters.stream()
                        .map(this::getDefinitionNameForType)
                        .collect(Collectors.joining(", ", "<", ">"));
            }
            return definitionName;
        }

        @Override
        public void adjustDuplicateNames(Map<DefinitionKey, String> subschemasWithDuplicateNames, SchemaGenerationContext generationContext) {
            char index = 'A';
            for (Map.Entry<DefinitionKey, String> singleEntry : subschemasWithDuplicateNames.entrySet()) {
                singleEntry.setValue(singleEntry.getValue() + "-" + index);
                index++;
            }
        }
    }

    static class Example {
        public List<FooModel<Model>> fooArray;
        public Bar barWithoutRenaming;
        public BarModel barModelDuplicate;
        public Model modelWithoutRenaming;
        public FooBarModel<BarModel> fooBarGeneric;
    }

    static class FooModel<T> {
        public T nested;
    }

    static class Bar {
        public int value;
    }

    static class BarModel {
        public String value;
    }

    static class FooBarModel<T> {
        public T nested;
    }

    static class Model {
        public String value;
    }
}
