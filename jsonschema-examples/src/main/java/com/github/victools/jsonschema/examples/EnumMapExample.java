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

package com.github.victools.jsonschema.examples;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/discussions/376">#376</a>.
 * <br/>
 * Representing a Map containing Enum values as keys.
 */
public class EnumMapExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.forTypesInGeneral()
                .withCustomDefinitionProvider(new EnumMapDefinitionProvider(enumValue -> enumValue.name().toLowerCase()));
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(Map.class, Job.class, JobDetails.class);
    }

    class EnumMapDefinitionProvider implements CustomDefinitionProviderV2 {

        private Function<Enum<?>, String> propertyNameSupplier;

        EnumMapDefinitionProvider(Function<Enum<?>, String> propertyNameSupplier) {
            this.propertyNameSupplier = propertyNameSupplier;
        }

        @Override
        public CustomDefinition provideCustomSchemaDefinition(ResolvedType targetType, SchemaGenerationContext context) {
            ResolvedType keyType = context.getTypeContext().getTypeParameterFor(targetType, Map.class, 0);
            if (keyType == null || !keyType.isInstanceOf(Enum.class)) {
                // only consider Maps with an Enum as key
                return null;
            }
            ResolvedType valueType = Optional.ofNullable(context.getTypeContext().getTypeParameterFor(targetType, Map.class, 1))
                    .orElseGet(()  -> context.getTypeContext().resolve(Object.class));
            ObjectNode customSchema = context.getGeneratorConfig().createObjectNode();
            ObjectNode propertiesNode = context.getGeneratorConfig().createObjectNode();
            customSchema.set(context.getKeyword(SchemaKeyword.TAG_PROPERTIES), propertiesNode);
            Stream.of(((Class<? extends Enum<?>>) keyType.getErasedType()).getEnumConstants())
                    .map(this.propertyNameSupplier)
                    .forEach(propertyName -> propertiesNode.set(propertyName,
                            context.createStandardDefinitionReference(valueType, this)));
            return new CustomDefinition(customSchema);
        }
    }

    enum Job {
        BLACKSMITH, FARMER;
    }

    static class JobDetails {
        public Map<TrainingResource, Integer> trainingCosts;
    }

    enum TrainingResource {
        WOOD, FOOD, STONE;
    }
}
