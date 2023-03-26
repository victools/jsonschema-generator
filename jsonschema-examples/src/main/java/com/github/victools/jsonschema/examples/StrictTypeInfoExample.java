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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/discussions/333">#333</a>.
 * <br/>
 * Ensuring "type" inclusion on "allOf" parts holding collected attributes, e.g., "additionalProperties".
 */
public class StrictTypeInfoExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON);
        configBuilder.with(Option.STRICT_TYPE_INFO, Option.DEFINITIONS_FOR_ALL_OBJECTS);
        configBuilder.forFields()
                .withAdditionalPropertiesResolver(field -> field.getType().getErasedType() == NestedType.class ? Void.class : null);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(Example.class);
    }

    static class Example {

        private NestedType alpha;
    }

    static class NestedType {
        private NestedType beta;
    }
}
