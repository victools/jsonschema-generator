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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
import java.util.Arrays;
import java.util.List;
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
public class SubtypeResolutionIntegrationTest {

    @Test
    public void testIntegration() throws Exception {
        JacksonModule module = new JacksonModule(JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE);
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.NULLABLE_FIELDS_BY_DEFAULT)
                .with(module)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(TestClassForSubtypeResolution.class);

        String rawJsonSchema = result.toString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource("subtype-integration-test-result.json"), rawJsonSchema, JSONCompareMode.STRICT);

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
        try (InputStream inputStream = SubtypeResolutionIntegrationTest.class
                .getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        String fileAsString = stringBuilder.toString();
        return fileAsString;
    }

    private static class TestClassForSubtypeResolution {

        public TestSuperClass supertypeWithoutAnnotation;
        @JsonSubTypes({
            @JsonSubTypes.Type(value = TestSubClassWithTypeNameAnnotation.class, name = "SpecificSubClass1"),
            @JsonSubTypes.Type(value = TestSubClass2.class, name = "SpecificSubClass2")
        })
        public TestSuperClass supertypeWithJsonSubTypesAnnotation;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public TestSuperClass supertypeAsWrapperArray;

        TestClassForSubtypeResolution() {
            this.supertypeWithoutAnnotation = new TestSubClassWithTypeNameAnnotation(
                    new TestSubClass2(new TestSubClass3(null))
            );
            this.supertypeWithJsonSubTypesAnnotation = new TestSubClass2(
                    new TestSubClassWithTypeNameAnnotation(
                            new TestSubClass2(new TestSubClassWithTypeNameAnnotation()),
                            new TestSubClass2()
                    )
            );
            this.supertypeAsWrapperArray = new TestSubClass3(
                    new TestSubClass3(null)
            );
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TestSubClassWithTypeNameAnnotation.class, name = "SubClass1"),
        @JsonSubTypes.Type(value = TestSubClass2.class, name = "SubClass2"),
        @JsonSubTypes.Type(value = TestSubClass3.class, name = "SubClass3")
    })
    private static class TestSuperClass {

    }

    @JsonTypeName("AnnotatedSubTypeName")
    private static class TestSubClassWithTypeNameAnnotation extends TestSuperClass {

        public List<TestSubClass2> directSubClass2;
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        public TestSubClass2 unwrappedSubClass2;

        TestSubClassWithTypeNameAnnotation(TestSubClass2... directSubClass2) {
            this.directSubClass2 = Arrays.asList(directSubClass2);
            if (this.directSubClass2.isEmpty()) {
                this.unwrappedSubClass2 = null;
            } else {
                this.unwrappedSubClass2 = directSubClass2[0];
            }
        }
    }

    private static class TestSubClass2 extends TestSuperClass {

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "typeString")
        @JsonSubTypes({
            @JsonSubTypes.Type(value = TestSubClassWithTypeNameAnnotation.class, name = "Sub1"),
            @JsonSubTypes.Type(value = TestSubClass3.class, name = "Sub3")
        })
        public TestSuperClass superClassViaProperty;

        public TestSubClass2() {
            this.superClassViaProperty = null;
        }

        public TestSubClass2(TestSubClassWithTypeNameAnnotation superClassViaProperty) {
            this.superClassViaProperty = superClassViaProperty;
        }

        public TestSubClass2(TestSubClass3 superClassViaProperty) {
            this.superClassViaProperty = superClassViaProperty;
        }
    }

    private static class TestSubClass3 extends TestSuperClass {

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        public TestSubClass3 recursiveSubClass3;

        TestSubClass3(TestSubClass3 recursiveSubClass3) {
            this.recursiveSubClass3 = recursiveSubClass3;
        }
    }
}
