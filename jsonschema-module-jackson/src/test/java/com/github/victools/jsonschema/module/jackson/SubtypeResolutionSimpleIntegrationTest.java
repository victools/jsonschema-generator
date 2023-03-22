/*
 * Copyright 2022 VicTools.
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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Integration test of this module being used in a real SchemaGenerator instance, focusing on the subtype resolution.
 */
public class SubtypeResolutionSimpleIntegrationTest {

    @Test
    public void testIntegration() throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .with(new JacksonModule(JacksonOption.ALWAYS_REF_SUBTYPES))
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(TestClassForSubtypeResolution.class);

        String rawJsonSchema = result.toString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource("subtype-simple-integration-test-result.json"), rawJsonSchema, JSONCompareMode.STRICT);

        JsonSchema schemaForValidation = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema(result);
        String jsonInstance = config.getObjectMapper().writeValueAsString(new TestClassForSubtypeResolution());

        Set<ValidationMessage> validationResult = schemaForValidation.validate(config.getObjectMapper().readTree(jsonInstance));
        if (!validationResult.isEmpty()) {
            Assertions.fail("\n" + jsonInstance + "\n  " + validationResult.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("\n  ")));
        }
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = SubtypeResolutionSimpleIntegrationTest.class
                .getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        return stringBuilder.toString();
    }

    private static class TestClassForSubtypeResolution {

        @JsonPropertyDescription("A member description")
        public TestSuperClass supertypeA;
        public TestSuperClass supertypeB;

        TestClassForSubtypeResolution() {
            this.supertypeA = new TestSubClassA();
            this.supertypeB = new TestSubClassB();
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TestSubClassA.class, name = "SubClassA"),
        @JsonSubTypes.Type(value = TestSubClassB.class, name = "SubClassB")
    })
    private static class TestSuperClass {

    }

    private static class TestSubClassA extends TestSuperClass {

        public String aProperty;

        public TestSubClassA() {
            this.aProperty = "a";
        }
    }

    private static class TestSubClassB extends TestSuperClass {

        public int bProperty;

        TestSubClassB() {
            this.bProperty = 'b';
        }
    }
}
