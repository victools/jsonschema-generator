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

package com.github.victools.jsonschema.module.swagger2;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Integration test of this module being used in a real SchemaGenerator instance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

    private SchemaGenerator generator;

    @BeforeAll
    public void setUp() {
        Swagger2Module module = new Swagger2Module();
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.NULLABLE_ARRAY_ITEMS_ALLOWED, Option.NULLABLE_FIELDS_BY_DEFAULT)
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
        try (InputStream inputStream = IntegrationTest.class.getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        return stringBuilder.toString();

    }

    @Schema(minProperties = 2, maxProperties = 5, requiredProperties = {"fieldWithInclusiveNumericRange"}, ref = "./TestClass-schema.json")
    static class TestClass {

        @Schema(hidden = true)
        public Object hiddenField;

        public String unannotatedField;

        @ArraySchema(arraySchema = @Schema(name = "fieldWithOverriddenName", required = true),
                schema = @Schema(defaultValue = "true", nullable = true), minItems = 1, maxItems = 20)
        public List<Boolean> originalFieldName;

        @Schema(description = "field description", nullable = true, allowableValues = {"A", "B", "C", "D"}, minLength = 1, maxLength = 1)
        public String fieldWithDescriptionAndAllowableValues;

        @Schema(minimum = "15", maximum = "20", multipleOf = 0.0123456789, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        public BigDecimal fieldWithInclusiveNumericRange;

        @Schema(minimum = "14", maximum = "21", exclusiveMinimum = true, exclusiveMaximum = true, multipleOf = 0.1,
                requiredMode = Schema.RequiredMode.REQUIRED)
        public int fieldWithExclusiveNumericRange;
    }

    @Schema(subTypes = {Reference.class, PersonReference.class})
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

    @Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    static class Foo {

        @Schema(implementation = PersonReference.class, accessMode = Schema.AccessMode.WRITE_ONLY)
        private Reference<Person> person;

        @Schema(ref = "http://example.com/bar", accessMode = Schema.AccessMode.READ_ONLY,
                additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        private Object bar;

        @Schema(anyOf = {Double.class, Integer.class})
        private Object anyOfDoubleOrInt;

        @Schema(oneOf = {Boolean.class, String.class})
        private Object oneOfBooleanOrString;

        // on a member, the "additionalProperties" attribute is ignored
        @Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
        private TestClass test;
    }
}
