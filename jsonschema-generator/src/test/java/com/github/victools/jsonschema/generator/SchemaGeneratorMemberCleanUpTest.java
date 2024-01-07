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

package com.github.victools.jsonschema.generator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link SchemaGenerator} class.
 */
public class SchemaGeneratorMemberCleanUpTest {

    @Test
    public void testMemberCleanUp() {
        SchemaGeneratorConfig generatorConfig = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .build();
        ObjectNode schema = new SchemaGenerator(generatorConfig).generateSchema(TestClass.class);
        System.out.println(schema.toPrettyString());
    }

    private static class TestClass {
        @JsonProperty("map_value")
        public Map<String, ValueClass> mapValue;
    }

    private static class ValueClass {
        @JsonProperty("int_value")
        public Integer intValue;
        @JsonProperty("string_value")
        public String stringValue;
    }
}
