/*
 * Copyright 2026 VicTools.
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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration test of this module being used in a real SchemaGenerator instance, focusing on the subtype resolution with multiple names per
 * subtype via {@code @JsonSubTypes.Type#names}.
 */
public class SubtypeResolutionMultipleNamesIntegrationTest {

    @Test
    public void testIntegration() throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .with(new JacksonSchemaModule(JacksonOption.ALWAYS_REF_SUBTYPES))
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(TestClassForSubtypeResolution.class);

        String rawJsonSchema = result.toString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource("subtype-multiple-names-integration-test-result.json"), rawJsonSchema, JSONCompareMode.STRICT);

        Schema schemaForValidation = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2019_09).getSchema(result);
        // Jackson serializes with the primary name; the alternative name must be accepted by the schema as well
        String serializedInstance = JsonMapper.shared().writeValueAsString(new TestClassForSubtypeResolution());
        String instanceWithAlternativeName = serializedInstance.replace("\"type\":\"A\"", "\"type\":\"B\"");
        Assertions.assertNotEquals(serializedInstance, instanceWithAlternativeName);
        for (String jsonInstance : List.of(serializedInstance, instanceWithAlternativeName)) {
            List<Error> validationResult = schemaForValidation.validate(JsonMapper.shared().readTree(jsonInstance));
            if (!validationResult.isEmpty()) {
                Assertions.fail("\n" + jsonInstance + "\n  " + validationResult.stream()
                        .map(Error::getMessage)
                        .collect(Collectors.joining("\n  ")));
            }
        }
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = SubtypeResolutionMultipleNamesIntegrationTest.class
                .getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        return stringBuilder.toString();
    }

    private static class TestClassForSubtypeResolution {

        public TestSuperClass multiNameSubtype;
        public TestSuperClass singleNameSubtype;

        TestClassForSubtypeResolution() {
            this.multiNameSubtype = new TestSubClassAB();
            this.singleNameSubtype = new TestSubClassC();
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TestSubClassAB.class, names = {"A", "B"}),
        @JsonSubTypes.Type(value = TestSubClassC.class, name = "C")
    })
    private static class TestSuperClass {

    }

    private static class TestSubClassAB extends TestSuperClass {

        public String abProperty;

        public TestSubClassAB() {
            this.abProperty = "ab";
        }
    }

    private static class TestSubClassC extends TestSuperClass {

        public int cProperty;

        TestSubClassC() {
            this.cProperty = 'c';
        }
    }
}
