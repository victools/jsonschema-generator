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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchemaCleanUpUtilsTest {

    Stream<Arguments> parametersForTestSetStrictTypeInfo() {
        return Stream.of(
                Arguments.of("{ \"title\": \"test\" }", true,
                        "{ \"title\": \"test\" }"),
                Arguments.of("{ \"description\": \"test\" }", true,
                        "{ \"description\": \"test\" }"),
                Arguments.of("{ \"$id\": \"test\" }", true,
                        "{ \"$id\": \"test\" }"),
                Arguments.of("{ \"properties\": { \"test\": {} } }", true,
                        "{ \"properties\": { \"test\": {} }, \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{ \"type\": \"object\", \"properties\": { \"test\": {} } }", true,
                        "{ \"type\": \"object\", \"properties\": { \"test\": {} } }"),
                Arguments.of("{ \"required\": [] }", true,
                        "{ \"required\": [], \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{ \"properties\": { \"test\": {} }, \"required\": [] }", true,
                        "{ \"properties\": { \"test\": {} }, \"required\": [], \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{ \"items\": {} }", true,
                        "{ \"items\": {}, \"type\": [\"array\", \"null\"] }"),
                Arguments.of("{ \"properties\": { \"test\": {} }, \"items\": {} }", true,
                        "{ \"properties\": { \"test\": {} }, \"items\": {}, \"type\": [\"array\", \"object\", \"null\"] }"),
                Arguments.of("{ \"unevaluatedItems\": {} }", true,
                        "{ \"unevaluatedItems\": {}, \"type\": [\"array\", \"null\"] }"),
                Arguments.of("{ \"prefixItems\": [] }", true,
                        "{ \"prefixItems\": [], \"type\": [\"array\", \"null\"] }"),
                Arguments.of("{ \"dependentSchemas\": { \"test\": {} } }", true,
                        "{ \"dependentSchemas\": { \"test\": {} }, \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{ \"dependentRequired\": { \"test\": [] } }", true,
                        "{ \"dependentRequired\": { \"test\": [] }, \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{ \"additionalProperties\": {} }", true,
                        "{ \"additionalProperties\": {}, \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{ \"patternProperties\": { \"a[bc]\": {} } }", true,
                        "{ \"patternProperties\": { \"a[bc]\": {} }, \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{ \"minProperties\": 1 }", true,
                        "{ \"minProperties\": 1, \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{ \"maxProperties\": 3 }", true,
                        "{ \"maxProperties\": 3, \"type\": [\"object\", \"null\"] }"),
                Arguments.of("{\"allOf\":[{\"anyOf\":[{\"oneOf\":[{\"not\":{\"required\":[\"test\"],\"type\":[\"object\",\"null\"]}}]}]}]}", true,
                        "{\"allOf\":[{\"anyOf\":[{\"oneOf\":[{\"not\":{\"required\":[\"test\"],\"type\":[\"object\",\"null\"]}}]}]}]}"),
                Arguments.of("{ \"minLength\": 1 }", true,
                        "{ \"minLength\": 1, \"type\": [\"string\", \"null\"] }"),
                Arguments.of("{ \"maxLength\": 1 }", true,
                        "{ \"maxLength\": 1, \"type\": [\"string\", \"null\"] }"),
                Arguments.of("{ \"format\": \"email\" }", true,
                        "{ \"format\": \"email\", \"type\": [\"string\", \"null\"] }"),
                Arguments.of("{ \"pattern\": \"a[bc]\" }", true,
                        "{ \"pattern\": \"a[bc]\", \"type\": [\"string\", \"null\"] }"),
                Arguments.of("{ \"minimum\": 10 }", true,
                        "{ \"minimum\": 10, \"type\": [\"integer\", \"number\", \"null\"] }"),
                Arguments.of("{ \"maximum\": 100 }", true,
                        "{ \"maximum\": 100, \"type\": [\"integer\", \"number\", \"null\"] }"),
                Arguments.of("{ \"exclusiveMinimum\": 9 }", true,
                        "{ \"exclusiveMinimum\": 9, \"type\": [\"integer\", \"number\", \"null\"] }"),
                Arguments.of("{ \"exclusiveMaximum\": 101 }", true,
                        "{ \"exclusiveMaximum\": 101, \"type\": [\"integer\", \"number\", \"null\"] }"),
                Arguments.of("{ \"minimum\": 10, \"maximum\": 100 }", true,
                        "{ \"minimum\": 10, \"maximum\": 100, \"type\": [\"integer\", \"number\", \"null\"] }"),
                Arguments.of("{ \"multipleOf\": 2 }", true,
                        "{ \"multipleOf\": 2, \"type\": [\"integer\", \"number\", \"null\"] }"),
                Arguments.of("{ \"if\": {}, \"then\": {}, \"else\": {} }", true,
                        "{ \"if\": {}, \"then\": {}, \"else\": {} }")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestSetStrictTypeInfo")
    public void testSetStrictTypeInfo(String schemaInput, boolean considerNullType, String expectedOutput) throws Exception {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
        SchemaCleanUpUtils utilsInstance = new SchemaCleanUpUtils(configBuilder.build());

        JsonNode schema = configBuilder.getObjectMapper().readTree(schemaInput);
        utilsInstance.setStrictTypeInfo(Collections.singletonList((ObjectNode) schema), considerNullType);

        String schemaAsString = schema.toString();
        JSONAssert.assertEquals('\n' + schemaAsString +'\n', expectedOutput, schemaAsString, JSONCompareMode.STRICT);
    }

    Stream<Arguments> parametersForTestReduceAllOfNodes() {
        return Stream.of(
                Arguments.of(
                        "{\"allOf\":[{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}," +
                                "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"},\"b\":{\"type\":\"string\"}}}]}",
                        "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"},\"b\":{\"type\":\"string\"}}}"),
                Arguments.of(
                        "{\"allOf\":[{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"required\":[\"a\"]}," +
                                "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"required\":[\"a\"]}]}",
                        "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"required\":[\"a\"]}"),
                Arguments.of(
                        "{\"allOf\":[{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"required\":[\"a\"]}," +
                                "{\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"}},\"required\":[\"a\",\"b\"]}]}",
                        "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"},\"b\":{\"type\":\"string\"}},\"required\":[\"a\",\"b\"]}")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestReduceAllOfNodes")
    public void testReduceAllOfNodes(String schemaInput, String expectedOutput) throws Exception {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
        SchemaCleanUpUtils utilsInstance = new SchemaCleanUpUtils(configBuilder.build());

        JsonNode schema = configBuilder.getObjectMapper().readTree(schemaInput);
        utilsInstance.reduceAllOfNodes(Collections.singletonList((ObjectNode) schema));

        String schemaAsString = schema.toString();
        JSONAssert.assertEquals('\n' + schemaAsString + '\n', expectedOutput, schemaAsString, JSONCompareMode.STRICT);
    }
}
