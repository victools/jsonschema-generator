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
import com.github.victools.jsonschema.generator.impl.ReflectionToStringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
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
            {Object.class, SchemaConstants.TAG_TYPE_OBJECT},
            {String.class, SchemaConstants.TAG_TYPE_STRING},
            {Character.class, SchemaConstants.TAG_TYPE_STRING},
            {char.class, SchemaConstants.TAG_TYPE_STRING},
            {CharSequence.class, SchemaConstants.TAG_TYPE_STRING},
            {Boolean.class, SchemaConstants.TAG_TYPE_BOOLEAN},
            {boolean.class, SchemaConstants.TAG_TYPE_BOOLEAN},
            {Integer.class, SchemaConstants.TAG_TYPE_INTEGER},
            {int.class, SchemaConstants.TAG_TYPE_INTEGER},
            {Long.class, SchemaConstants.TAG_TYPE_INTEGER},
            {long.class, SchemaConstants.TAG_TYPE_INTEGER},
            {Short.class, SchemaConstants.TAG_TYPE_INTEGER},
            {short.class, SchemaConstants.TAG_TYPE_INTEGER},
            {Byte.class, SchemaConstants.TAG_TYPE_INTEGER},
            {byte.class, SchemaConstants.TAG_TYPE_INTEGER},
            {Double.class, SchemaConstants.TAG_TYPE_NUMBER},
            {double.class, SchemaConstants.TAG_TYPE_NUMBER},
            {Float.class, SchemaConstants.TAG_TYPE_NUMBER},
            {float.class, SchemaConstants.TAG_TYPE_NUMBER}
        };
    }

    @Test
    @Parameters
    public void testGenerateSchema_SimpleType(Class<?> targetType, String expectedJsonSchemaType) throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper()).build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(expectedJsonSchemaType, result.get(SchemaConstants.TAG_TYPE).asText());
    }

    private static void populateConfigPart(SchemaGeneratorConfigPart<?> configPart, String descriptionPrefix) {
        configPart
                .withArrayMinItemsResolver((f, type, p) -> type.isArray() || type.isInstanceOf(Collection.class) ? 0 : null)
                .withArrayMaxItemsResolver((f, type, p) -> type.isArray() || type.isInstanceOf(Collection.class) ? Integer.MAX_VALUE : null)
                .withArrayUniqueItemsResolver((f, type, p) -> type.isArray() || type.isInstanceOf(Collection.class) ? false : null)
                .withDescriptionResolver((f, type, p) -> descriptionPrefix + ReflectionToStringUtils.createStringRepresentation(type))
                .withEnumResolver((f, type, p) -> type.isInstanceOf(Number.class) ? Arrays.asList(1, 2, 3, 4, 5) : null)
                .withEnumResolver((f, type, p) -> type.isInstanceOf(String.class) ? Arrays.asList("constant string value") : null)
                .withNullableCheck((f, type, p) -> Boolean.TRUE)
                .withNumberExclusiveMaximumResolver((f, type, p) -> type.isInstanceOf(Number.class) ? BigDecimal.TEN.add(BigDecimal.ONE) : null)
                .withNumberExclusiveMinimumResolver((f, type, p) -> type.isInstanceOf(Number.class) ? BigDecimal.ZERO : null)
                .withNumberInclusiveMaximumResolver((f, type, p) -> type.isInstanceOf(Number.class) ? BigDecimal.TEN : null)
                .withNumberInclusiveMinimumResolver((f, type, p) -> type.isInstanceOf(Number.class) ? BigDecimal.ONE : null)
                .withNumberMultipleOfResolver((f, type, p) -> type.isInstanceOf(Number.class) ? BigDecimal.ONE : null)
                .withStringFormatResolver((f, type, p) -> type.isInstanceOf(String.class) ? "date" : null)
                .withStringMaxLengthResolver((f, type, p) -> type.isInstanceOf(String.class) ? 256 : null)
                .withStringMinLengthResolver((f, type, p) -> type.isInstanceOf(String.class) ? 1 : null)
                .withTitleResolver((f, type, p) -> ReflectionToStringUtils.createStringRepresentation(type));
    }

    Object parametersForTestGenerateSchema() {
        Module neutralModule = configBuilder -> {
        };
        Module methodModule = configBuilder -> populateConfigPart(configBuilder.forMethods(), "looked-up from method: ");
        Module fieldModule = configBuilder -> populateConfigPart(configBuilder.forFields(), "looked-up from field: ");
        return new Object[][]{
            {"testclass1-FULL_DOCUMENTATION", OptionPreset.FULL_DOCUMENTATION, TestClass1.class, neutralModule},
            {"testclass1-JAVA_OBJECT-method-attributes", OptionPreset.JAVA_OBJECT, TestClass1.class, methodModule},
            {"testclass1-PLAIN_JSON-field-attributes", OptionPreset.PLAIN_JSON, TestClass1.class, fieldModule},
            {"testclass2-array", OptionPreset.FULL_DOCUMENTATION, TestClass2[].class, neutralModule},
            {"testclass3-FULL_DOCUMENTATION", OptionPreset.FULL_DOCUMENTATION, TestClass3.class, neutralModule},
            {"testclass3-JAVA_OBJECT-field-attributes", OptionPreset.JAVA_OBJECT, TestClass3.class, fieldModule},
            {"testclass3-PLAIN_JSON-method-attributes", OptionPreset.PLAIN_JSON, TestClass3.class, methodModule}
        };
    }

    @Test
    @Parameters
    @TestCaseName(value = "{method}({0}) [{index}]")
    public void testGenerateSchema(String caseTitle, OptionPreset preset, Class<?> targetType, Module testModule) throws Exception {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(new ObjectMapper(), preset);
        configBuilder.with(testModule);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        JsonNode result = generator.generateSchema(targetType);
        JSONAssert.assertEquals('\n' + result.toString() + '\n', loadResource(caseTitle + ".json"), result.toString(), JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = SchemaGeneratorTest.class
                .getResourceAsStream(resourcePath);
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
        private Integer ignoredInternalValue;

        public int getPrimitiveValue() {
            return this.primitiveValue;
        }

        public <A extends B, B extends Number> void calculateSomething(A param0, B param1) {
            // nothing to do
        }

        public boolean isSimpleTestClass() {
            return true;
        }
    }

    private static class TestClass2<T> {

        private T genericValue;
        public T[] genericArray;

        public T getGenericValue() {
            return this.genericValue;
        }
    }

    private static class TestClass3 {

        private TestClass2<Long> nestedLong;
        private TestClass2<TestClass1[]> nestedClass1Array;
        private List<? extends TestClass2<Long>> nestedLongList;
        private TestClass4<String> class4;

        public TestClass2<Long> getNestedLong() {
            return this.nestedLong;
        }

        public TestClass2<TestClass1[]> getNestedClass1Array() {
            return this.nestedClass1Array;
        }

        public List<? extends TestClass2<Long>> getNestedLongList() {
            return this.nestedLongList;
        }

        public TestClass4<String> getClass4() {
            return this.class4;
        }
    }

    private static class TestClass4<T> {

        private TestClass2<TestClass2<T>> class2OfClass2OfT;
        public Optional<T> optionalT;
        public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

        public TestClass2<TestClass2<T>> getClass2OfClass2OfT() {
            return this.class2OfClass2OfT;
        }
    }
}
