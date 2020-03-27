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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Integration test of this module being used in a real SchemaGenerator instance, focusing on the subtype resolution.
 */
public class SubtypeResolutionIntegrationTest {

    @Test
    public void testIntegration() throws Exception {
        JacksonModule module = new JacksonModule(JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE);
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(module)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(TestClassForSubtypeResolution.class);

        String rawJsonSchema = result.toString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource("subtype-integration-test-result.json"), rawJsonSchema, JSONCompareMode.STRICT);
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
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = TestSubClassWithTypeNameAnnotation.class, name = "SubClass1"),
        @JsonSubTypes.Type(value = TestSubClass2.class, name = "SubClass2"),
        @JsonSubTypes.Type(value = TestSubClass3.class, name = "SubClass3")
    })
    private static class TestSuperClass {

        public String typeString;
    }

    @JsonTypeName("AnnotatedSubTypeName")
    private static class TestSubClassWithTypeNameAnnotation extends TestSuperClass {

        public TestSubClass2 directSubClass2;
    }

    private static class TestSubClass2 extends TestSuperClass {

        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "typeString")
        @JsonSubTypes({
            @JsonSubTypes.Type(value = TestSubClassWithTypeNameAnnotation.class, name = "Sub1"),
            @JsonSubTypes.Type(value = TestSubClass3.class, name = "Sub3")
        })
        public TestSuperClass superClassViaExternalProperty;
    }

    private static class TestSubClass3 extends TestSuperClass {

        public TestSubClass3 recursiveSubClass3;
    }
}
