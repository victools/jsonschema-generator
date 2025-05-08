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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link SchemaGenerator} class.
 */
public class SchemaGeneratorMemberCleanUpTest {

    private static Stream<Arguments> testMemberCleanup() {
        return Stream.of(
                Arguments.of(true, Arrays.asList("$ref")),
                Arguments.of(false, Arrays.asList("$ref", "additionalProperties"))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testMemberCleanup(boolean enableCleanup, List<String> expectedMemberAttributes) {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES, Option.DEFINITIONS_FOR_ALL_OBJECTS);
        if (enableCleanup) {
            configBuilder.with(Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END);
        } else {
            configBuilder.without(Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END);
        }
        SchemaGeneratorConfig generatorConfig = configBuilder.build();
        ObjectNode schema = new SchemaGenerator(generatorConfig).generateSchema(TestClass.class);
        JsonNode memberSchema = schema.get(generatorConfig.getKeyword(SchemaKeyword.TAG_PROPERTIES)).get("mapValue");
        Assertions.assertEquals(expectedMemberAttributes.size(), memberSchema.size());
        memberSchema.fieldNames()
                .forEachRemaining(fieldName -> Assertions.assertTrue(expectedMemberAttributes.contains(fieldName)));
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
