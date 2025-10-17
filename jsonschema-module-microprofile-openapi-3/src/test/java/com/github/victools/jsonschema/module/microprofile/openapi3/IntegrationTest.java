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

package com.github.victools.jsonschema.module.microprofile.openapi3;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * Integration test of this module being used in a real SchemaGenerator instance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

    private SchemaGenerator generator;

    @BeforeAll
    public void setUp() {
        MicroProfileOpenApi3Module module = new MicroProfileOpenApi3Module();
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                .with(Option.NONSTATIC_NONVOID_NONGETTER_METHODS, Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS)
                .with(module);
        this.generator = new SchemaGenerator(configBuilder.build());
    }

    @ParameterizedTest
    @ValueSource(classes = {TestClass.class, Foo.class})
    public void testIntegration(Class<?> rawTargetType) throws Exception {
        JsonNode result = this.generator.generateSchema(rawTargetType);

        String rawJsonSchema = result.toString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource("integration-test-result-" + rawTargetType.getSimpleName() + ".json"), rawJsonSchema,
                JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = IntegrationTest.class.getResourceAsStream(resourcePath)) {
            assert inputStream != null;
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                while (scanner.hasNext()) {
                    stringBuilder.append(scanner.nextLine()).append('\n');
                }
            }
        }
        return stringBuilder.toString();

    }

    @Schema(minProperties = 2, maxProperties = 5, requiredProperties = {"fieldWithInclusiveNumericRange"}, ref = "./TestClass-schema.json")
    static class TestClass {

        @Schema(hidden = true)
        public Object hiddenField;

        @Schema(type = SchemaType.ARRAY, name = "fieldWithOverriddenName", defaultValue = "true", nullable = true,
                minItems = 1, maxItems = 20, required = true)
        public List<Boolean> originalFieldName;

        @Schema(description = "field description", nullable = true, enumeration = {"A", "B", "C", "D"}, minLength = 1, maxLength = 1)
        public String fieldWithDescriptionAndAllowableValues;

        @Schema(minimum = "15", maximum = "20", multipleOf = 0.0123456789, required = false)
        public BigDecimal fieldWithInclusiveNumericRange;

        @Schema(minimum = "14", maximum = "21", exclusiveMinimum = true, exclusiveMaximum = true, multipleOf = 0.1,
                required = true)
        public int fieldWithExclusiveNumericRange;
    }

    @Schema(anyOf = {Reference.class, PersonReference.class})
    interface IReference {

        String getName();
    }

    static class Reference<T> implements IReference {

        private String name;

        @Override
        public String getName() {
            return this.name;
        }
    }

    static class Person {

    }

    @Schema(description = "the foo's person", title = "reference title", name = "referenceToPerson")
    static class PersonReference extends Reference<Person> {

        @Override
        @Schema(description = "the person's name")
        public String getName() {
            return super.getName();
        }
    }

    @Schema(additionalProperties = Schema.False.class)
    static class Foo {

        @Schema(implementation = PersonReference.class, writeOnly = true)
        private Reference<Person> person;

        @Schema(ref = "http://example.com/bar", readOnly = true,
                additionalProperties = Schema.True.class)
        private Object bar;

        @Schema(anyOf = {Double.class, Integer.class})
        private Object anyOfDoubleOrInt;

        @Schema(oneOf = {Boolean.class, String.class})
        private Object oneOfBooleanOrString;

        // on a member, the "additionalProperties" attribute is ignored
        @Schema(additionalProperties = Schema.False.class)
        private TestClass test;
    }
}
