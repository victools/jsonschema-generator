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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import java.util.List;
import tools.jackson.databind.node.ObjectNode;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/discussions/337">#337</a>.
 * <br/>
 * Utilising Jackson subtype resolution in combination with various options to steer what is included in the "$defs".
 */
public class JacksonSubtypeDefinitionExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        JacksonModule jacksonModule = new JacksonModule(JacksonOption.ALWAYS_REF_SUBTYPES, JacksonOption.INLINE_TRANSFORMED_SUBTYPES);
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(jacksonModule)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES, Option.DEFINITION_FOR_MAIN_SCHEMA);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(BaseType.class);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SubType1.class, name = "subtype1"),
            @JsonSubTypes.Type(value = SubType2.class, name = "subtype2"),
    })
    interface BaseType {
    }

    static class SubType1 implements BaseType {
        public NestedObject nestedObject;
    }

    static class SubType2 implements BaseType {
        public List<BaseType> conditions;
    }

    static class NestedObject {
        public Object field;
    }
}
