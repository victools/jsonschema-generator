/*
 * Copyright 2019 VicTools.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for {@link SchemaGenerator} class.
 */
@RunWith(JUnitParamsRunner.class)
public class SchemaGeneratorTest {

    Object parametersForTestGenerateSchema_SimpleType() {
        return new Object[][]{
            {String.class, SchemaConstants.TAG_TYPE_STRING},
            {Character.class, SchemaConstants.TAG_TYPE_STRING},
            {char.class, SchemaConstants.TAG_TYPE_STRING},
            {CharSequence.class, SchemaConstants.TAG_TYPE_STRING},
            {Boolean.class, SchemaConstants.TAG_TYPE_BOOLEAN},
            {boolean.class, SchemaConstants.TAG_TYPE_BOOLEAN},
            {Integer.class, SchemaConstants.TAG_TYPE_INTEGER},
            {int.class, SchemaConstants.TAG_TYPE_INTEGER},
            {Short.class, SchemaConstants.TAG_TYPE_INTEGER},
            {short.class, SchemaConstants.TAG_TYPE_INTEGER},
            {Long.class, SchemaConstants.TAG_TYPE_INTEGER},
            {long.class, SchemaConstants.TAG_TYPE_INTEGER},
            {Double.class, SchemaConstants.TAG_TYPE_NUMBER},
            {double.class, SchemaConstants.TAG_TYPE_NUMBER},
            {Float.class, SchemaConstants.TAG_TYPE_NUMBER},
            {float.class, SchemaConstants.TAG_TYPE_NUMBER},
            {Byte.class, SchemaConstants.TAG_TYPE_NUMBER},
            {byte.class, SchemaConstants.TAG_TYPE_NUMBER},
            {Object.class, SchemaConstants.TAG_TYPE_OBJECT}
        };
    }

    @Test
    @Parameters
    public void testGenerateSchema_SimpleType(Class<?> targetType, String expectedJsonSchemaType) throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper()).build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
        Assert.assertNotNull(result.get("$schema"));
        JSONAssert.assertEquals("{\"type\":\"" + expectedJsonSchemaType + "\"}", new JSONObject(result.toString()), JSONCompareMode.LENIENT);
    }

    Object parametersForTestGenerateSchema_Object() {
        return new Object[][]{
            {"default-options", EnumSet.noneOf(Option.class), EnumSet.noneOf(Option.class)},
            {"no-getters", EnumSet.of(Option.EXCLUDE_GETTER_METHODS), EnumSet.noneOf(Option.class)},
            {"with-void-methods", EnumSet.noneOf(Option.class), EnumSet.of(Option.EXCLUDE_VOID_METHODS)},
            {"not-nullable-fields", EnumSet.noneOf(Option.class), EnumSet.of(Option.FIELDS_ARE_NULLABLE_BY_DEFAULT)}
        };
    }

    @Test
    @Parameters
    @TestCaseName(value = "{method}({0}) [{index}]")
    public void testGenerateSchema_Object(String caseTitle, EnumSet<Option> enabledOptions, EnumSet<Option> disabledOptions) throws Exception {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(new ObjectMapper());
        enabledOptions.forEach(configBuilder::withEnabled);
        disabledOptions.forEach(configBuilder::withDisabled);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        JsonNode result = generator.generateSchema(TestClass1.class);
        System.out.println(caseTitle + "\n" + result.toString());
        JSONAssert.assertEquals(loadResource("testclass1_" + caseTitle + ".json"), result.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    private static final String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = SchemaGeneratorTest.class.getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        String fileAsString = stringBuilder.toString();
        return fileAsString;
    }

    private static class TestClass1 extends TestClass2<String> {

        public static final Long CONSTANT = 5L;

        private int primitiveValue;
        private Integer internalValue;

        public int getPrimitiveValue() {
            return this.primitiveValue;
        }

        public void calculateSomething() {
            // nothing to do
        }

        public boolean isSimpleTestClass() {
            return true;
        }
    }

    private static class TestClass2<T> {

        private T genericValue;

        public T getGenericValue() {
            return this.genericValue;
        }
    }

    private static class TestClass3 {

        private List<BigDecimal> decimalValues;
        private TestClass2<Long> nested;
        private String text;

        public List<BigDecimal> getDecimalValues() {
            return decimalValues;
        }

        public TestClass2<Long> getNested() {
            return nested;
        }

        public String getText() {
            return text;
        }
    }
}
