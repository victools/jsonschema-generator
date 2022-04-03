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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Integration test of this module being used in a real SchemaGenerator instance.
 */
public class IntegrationTest {

    /**
     * Test
     *
     * @throws Exception
     */
    @Test
    public void testIntegration() throws Exception {
        JacksonModule module = new JacksonModule(JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE, JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY,
                JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS);
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                .with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                .with(Option.NONSTATIC_NONVOID_NONGETTER_METHODS, Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS)
                .with(module)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(TestClass.class);

        String rawJsonSchema = result.toString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource("integration-test-result.json"), rawJsonSchema, JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = IntegrationTest.class
                .getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        String fileAsString = stringBuilder.toString();
        return fileAsString;

    }

    @JsonClassDescription("test description")
    static class TestClass {

        @JsonPropertyDescription("field description")
        public String fieldWithDescription;

        @JsonProperty(value = "fieldWithOverriddenName", access = JsonProperty.Access.WRITE_ONLY)
        public boolean originalFieldName;

        public TestEnum enumValueHandledByStandardOption;

        public TestEnumWithJsonValueAnnotation enumValueWithJsonValueAnnotation;

        public TestEnumWithJsonPropertyAnnotations enumValueWithJsonPropertyAnnotations;

        public String ignoredUnannotatedMethod() {
            return "nothing";
        }

        @JsonProperty(value = "calculated", access = JsonProperty.Access.READ_ONLY)
        @JsonPropertyDescription("calculated value from non-getter method")
        public double getCalculatedValue() {
            return 19.75;
        }
    }

    static enum TestEnum {
        A, B, C;
    }

    static enum TestEnumWithJsonValueAnnotation {
        ENTRY1, ENTRY2, ENTRY3;

        @JsonValue
        public String getJsonValue() {
            return this.name().toLowerCase();
        }
    }

    static enum TestEnumWithJsonPropertyAnnotations {
        @JsonProperty("x_property") X,
        @JsonProperty Y;
    }
}
