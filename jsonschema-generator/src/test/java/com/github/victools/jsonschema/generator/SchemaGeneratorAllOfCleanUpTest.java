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

package com.github.victools.jsonschema.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for {@link SchemaGenerator} class.
 */
public class SchemaGeneratorAllOfCleanUpTest {

    static Stream<Arguments> parametersForTestAllOfCleanUp() {
        String differentValueInMainSchema = "{ \"type\":\"object\", \"title\":\"main schema\", \"allOf\":[{ \"title\":\"different title\" }, {}] }";
        String differentValueInAllOfPart = "{ \"type\":\"object\", \"allOf\":[{ \"title\":\"title X\" }, { \"title\":\"title Y\" }] }";
        String equalIfTagInMainSchema = "{ \"type\":\"object\", \"if\":{ \"const\": 1 }, \"then\":{}, "
                + "\"allOf\":[{ \"if\":{ \"const\": 1 }, \"then\":{}, \"else\": { \"title\": \"otherwise...\" } }, {}] }";
        String equalIfTagInAllOfPart = "{ \"type\":\"object\", \"allOf\":[{ \"if\":{ \"const\": 1 }, \"then\":{} }, "
                + "{ \"if\":{ \"const\": 1 }, \"then\":{}, \"else\": { \"title\": \"otherwise...\" } }] }";
        List<Arguments> testCases = EnumSet.allOf(SchemaVersion.class).stream()
                .flatMap(schemaVersion -> Stream.of(
                    Arguments.of(schemaVersion, differentValueInMainSchema, differentValueInMainSchema),
                    Arguments.of(schemaVersion, differentValueInAllOfPart, differentValueInAllOfPart),
                    Arguments.of(schemaVersion, equalIfTagInMainSchema, equalIfTagInMainSchema),
                    Arguments.of(schemaVersion, equalIfTagInAllOfPart, equalIfTagInAllOfPart),
                    Arguments.of(schemaVersion,
                        "{ \"type\": \"object\", \"title\":\"same in all three\", "
                                + "\"allOf\": [{ \"title\":\"same in all three\" }, { \"title\":\"same in all three\" }] }",
                        "{ \"type\": \"object\", \"title\":\"same in all three\" }"),
                    Arguments.of(schemaVersion,
                        "{ \"type\": \"object\",\"allOf\": [{ \"title\":\"from allOf[0]\" }, { \"description\":\"from allOf[1]\" }] }",
                        "{ \"type\": \"object\", \"title\":\"from allOf[0]\", \"description\":\"from allOf[1]\" }")
                ))
                .collect(Collectors.toList());

        // in Drafts 6/7, alongside $ref all other attributes are being ignored and should therefore not be merged
        EnumSet<SchemaVersion> versionsWithSpecialRefHandling = EnumSet.of(SchemaVersion.DRAFT_6, SchemaVersion.DRAFT_7);
        String refInAllOfPart = "{ \"type\": \"object\", \"allOf\": [{ \"$ref\": \"#\" }, { \"title\": \"value\" }] }";
        versionsWithSpecialRefHandling.stream()
                .forEach(schemaVersion -> testCases.add(Arguments.of(schemaVersion, refInAllOfPart, refInAllOfPart)));
        EnumSet.complementOf(versionsWithSpecialRefHandling).stream()
                .forEach(schemaVersion ->
        testCases.add(Arguments.of(schemaVersion, refInAllOfPart, "{ \"type\": \"object\", \"$ref\": \"#\", \"title\": \"value\" }")));
        return testCases.stream();
    }

    @ParameterizedTest
    @MethodSource("parametersForTestAllOfCleanUp")
    public void testAllOfCleanUp(SchemaVersion schemaVersion, String inputSchema, String outputSchema) throws Exception {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion, OptionPreset.PLAIN_JSON)
                .without(Option.SCHEMA_VERSION_INDICATOR);
        configBuilder.forTypesInGeneral()
                .withCustomDefinitionProvider((_type, context) -> {
                    try {
                        return new CustomDefinition((ObjectNode) context.getGeneratorConfig().getObjectMapper()
                                .readTree(inputSchema));
                    } catch (JsonProcessingException ex) {
                        throw new IllegalStateException("This should never happen", ex);
                    }
                });
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        JsonNode result = generator.generateSchema(String.class);
        JSONAssert.assertEquals(outputSchema, result.toString(), JSONCompareMode.STRICT);
    }
}
