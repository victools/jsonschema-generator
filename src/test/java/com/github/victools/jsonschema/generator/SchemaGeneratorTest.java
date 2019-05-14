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
import com.github.victools.jsonschema.generator.impl.ReflectionTypeUtils;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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
        Assert.assertEquals(2, result.size());
        Assert.assertNotNull(result.get("$schema"));
        Assert.assertEquals(expectedJsonSchemaType, result.get(SchemaConstants.TAG_TYPE).asText());
    }

    private static boolean isTypeOf(JavaType type, Class<?> rawSuperType) {
        Class<?> rawType = ReflectionTypeUtils.getRawType(type.getResolvedType());
        return rawType != null && rawSuperType.isAssignableFrom(rawType);
    }

    private static void populateConfigPart(SchemaGeneratorConfigPart<?> configPart, String descriptionPrefix) {
        configPart
                .addArrayMinItemsResolver((field, type) -> ReflectionTypeUtils.isArrayType(type) ? 0 : null)
                .addArrayMaxItemsResolver((field, type) -> ReflectionTypeUtils.isArrayType(type) ? Integer.MAX_VALUE : null)
                .addArrayUniqueItemsResolver((field, type) -> ReflectionTypeUtils.isArrayType(type) ? false : null)
                .addDescriptionResolver((field, type) -> descriptionPrefix + type.toString())
                .addEnumResolver((field, type) -> isTypeOf(type, Number.class) ? Arrays.asList(1, 2, 3, 4, 5) : null)
                .addEnumResolver((field, type) -> isTypeOf(type, String.class) ? Arrays.asList("constant string value") : null)
                .addNullableCheck((field, type) -> Boolean.TRUE)
                .addNumberExclusiveMaximumResolver((field, type) -> isTypeOf(type, Number.class) ? BigDecimal.TEN.add(BigDecimal.ONE) : null)
                .addNumberExclusiveMinimumResolver((field, type) -> isTypeOf(type, Number.class) ? BigDecimal.ZERO : null)
                .addNumberInclusiveMaximumResolver((field, type) -> isTypeOf(type, Number.class) ? BigDecimal.TEN : null)
                .addNumberInclusiveMinimumResolver((field, type) -> isTypeOf(type, Number.class) ? BigDecimal.ONE : null)
                .addNumberMultipleOfResolver((field, type) -> isTypeOf(type, Number.class) ? BigDecimal.ONE : null)
                .addPropertyNameOverrideResolver((field, defaultName) -> "_" + defaultName)
                .addStringFormatResolver((field, type) -> isTypeOf(type, String.class) ? "date" : null)
                .addStringMaxLengthResolver((field, type) -> isTypeOf(type, String.class) ? 256 : null)
                .addStringMinLengthResolver((field, type) -> isTypeOf(type, String.class) ? 1 : null)
                .addTitleResolver((field, type) -> type.toString());
    }

    Object parametersForTestGenerateSchema() {
        Module neutralModule = configBuilder -> {
        };
        Module excludingGetters = configBuilder -> configBuilder.without(Option.GETTER_METHODS);
        Module nullableFields = configBuilder -> configBuilder.with(Option.NULLABLE_FIELDS_BY_DEFAULT);
        Module includingVoidMethods = configBuilder -> configBuilder.with(Option.VOID_METHODS);
        Module fieldModule = configBuilder -> populateConfigPart(configBuilder.forFields(), "looked-up from field: ");
        Module methodModule = configBuilder -> populateConfigPart(configBuilder.forMethods(), "looked-up from method: ");
        Module fieldsOnlyModule = configBuilder -> configBuilder.with(methodModule)
                .with(Option.GETTER_ATTRIBUTES_FOR_FIELDS, Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .forMethods().addIgnoreCheck(method -> true);
        Module methodsOnlyModule = configBuilder -> configBuilder.with(fieldModule)
                .with(Option.FIELD_ATTRIBUTES_FOR_GETTERS, Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .forFields().addIgnoreCheck(field -> true);
        return new Object[][]{
            {"testclass1_default-options", TestClass1.class, neutralModule},
            {"testclass1_no-getters", TestClass1.class, excludingGetters},
            {"testclass1_nullable-fields", TestClass1.class, nullableFields},
            {"testclass1_with-void-methods", TestClass1.class, includingVoidMethods},
            {"testclass2_array", TestClass2[].class, neutralModule},
            {"testclass3_default-options", TestClass3.class, neutralModule},
            {"testclass3_field-attributes", TestClass3.class, fieldModule},
            {"testclass3_fields-only", TestClass3.class, fieldsOnlyModule},
            {"testclass3_method-attributes", TestClass3.class, methodModule},
            {"testclass3_methods-only", TestClass3.class, methodsOnlyModule}
        };
    }

    @Test
    @Parameters
    @TestCaseName(value = "{method}({0}) [{index}]")
    public void testGenerateSchema(String caseTitle, Class<?> targetType, Module testModule) throws Exception {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(new ObjectMapper());
        configBuilder.with(testModule);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        JsonNode result = generator.generateSchema(targetType);
        // System.out.println(caseTitle + "\n" + result.toString());
        JSONAssert.assertEquals(loadResource(caseTitle + ".json"), result.toString(), JSONCompareMode.STRICT);
    }

    private static final String loadResource(String resourcePath) throws IOException {
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

        public TestClass2<TestClass2<T>> getClass2OfClass2OfT() {
            return this.class2OfClass2OfT;
        }
    }
}
