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
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for {@link SchemaGenerator} class.
 */
public class SchemaGeneratorSimpleTypesTest {

    static Stream<Arguments> getSimpleTypeCombinations() {
        return Stream.of(
            Arguments.of(Object.class, null, null),
            Arguments.of(String.class, SchemaKeyword.TAG_TYPE_STRING, null),
            Arguments.of(Character.class, SchemaKeyword.TAG_TYPE_STRING, null),
            Arguments.of(char.class, SchemaKeyword.TAG_TYPE_STRING, null),
            Arguments.of(CharSequence.class, SchemaKeyword.TAG_TYPE_STRING, null),
            Arguments.of(Byte.class, SchemaKeyword.TAG_TYPE_STRING, null),
            Arguments.of(byte.class, SchemaKeyword.TAG_TYPE_STRING, null),
            Arguments.of(Boolean.class, SchemaKeyword.TAG_TYPE_BOOLEAN, null),
            Arguments.of(boolean.class, SchemaKeyword.TAG_TYPE_BOOLEAN, null),
            Arguments.of(Integer.class, SchemaKeyword.TAG_TYPE_INTEGER, "int32"),
            Arguments.of(int.class, SchemaKeyword.TAG_TYPE_INTEGER, "int32"),
            Arguments.of(Long.class, SchemaKeyword.TAG_TYPE_INTEGER, "int64"),
            Arguments.of(long.class, SchemaKeyword.TAG_TYPE_INTEGER, "int64"),
            Arguments.of(Short.class, SchemaKeyword.TAG_TYPE_INTEGER, null),
            Arguments.of(short.class, SchemaKeyword.TAG_TYPE_INTEGER, null),
            Arguments.of(Double.class, SchemaKeyword.TAG_TYPE_NUMBER, "double"),
            Arguments.of(double.class, SchemaKeyword.TAG_TYPE_NUMBER, "double"),
            Arguments.of(Float.class, SchemaKeyword.TAG_TYPE_NUMBER, "float"),
            Arguments.of(float.class, SchemaKeyword.TAG_TYPE_NUMBER, "float"),
            Arguments.of(java.time.LocalDate.class, SchemaKeyword.TAG_TYPE_STRING, "date"),
            Arguments.of(java.time.LocalDateTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"),
            Arguments.of(java.time.LocalTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"),
            Arguments.of(java.time.ZonedDateTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"),
            Arguments.of(java.time.OffsetDateTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"),
            Arguments.of(java.time.OffsetTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"),
            Arguments.of(java.time.Instant.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"),
            Arguments.of(java.util.Date.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"),
            Arguments.of(java.util.Calendar.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"),
            Arguments.of(java.util.UUID.class, SchemaKeyword.TAG_TYPE_STRING, "uuid"),
            Arguments.of(java.time.ZoneId.class, SchemaKeyword.TAG_TYPE_STRING, null),
            Arguments.of(java.math.BigInteger.class, SchemaKeyword.TAG_TYPE_INTEGER, null),
            Arguments.of(java.math.BigDecimal.class, SchemaKeyword.TAG_TYPE_NUMBER, null),
            Arguments.of(Number.class, SchemaKeyword.TAG_TYPE_NUMBER, null)
        );
    }

    static Stream<Arguments> parametersForTestGenerateSchema_SimpleTypeWithoutFormat() {
        List<Arguments> typeCombinations = getSimpleTypeCombinations().collect(Collectors.toList());
        return EnumSet.allOf(SchemaVersion.class).stream()
                .flatMap(schemaVersion -> typeCombinations.stream()
                        .map(entry -> Arguments.of(entry.get()[0], entry.get()[1], schemaVersion)));
    }

    static Stream<Arguments> parametersForTestGenerateSchema_SimpleTypeWithFormat() {
        List<Arguments> typeCombinations = getSimpleTypeCombinations().collect(Collectors.toList());
        return EnumSet.allOf(SchemaVersion.class).stream()
                .flatMap(schemaVersion -> typeCombinations.stream()
                        .map(entry -> Arguments.of(entry.get()[0], entry.get()[1], entry.get()[2], schemaVersion)));
    }

    @ParameterizedTest
    @MethodSource("parametersForTestGenerateSchema_SimpleTypeWithoutFormat")
    public void testGenerateSchema_SimpleTypeWithoutFormat(Class<?> targetType, SchemaKeyword expectedJsonSchemaType, SchemaVersion schemaVersion)
            throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(schemaVersion)
                .with(Option.ADDITIONAL_FIXED_TYPES)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
        if (expectedJsonSchemaType == null) {
            Assertions.assertTrue(result.isEmpty());
        } else {
            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals(expectedJsonSchemaType.forVersion(schemaVersion),
                    result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        }
    }

    @ParameterizedTest
    @MethodSource("parametersForTestGenerateSchema_SimpleTypeWithFormat")
    public void testGenerateSchema_SimpleTypeWithFormat(Class<?> targetType, SchemaKeyword expectedJsonSchemaType, String expectedFormat,
            SchemaVersion schemaVersion) throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(schemaVersion)
                .with(Option.ADDITIONAL_FIXED_TYPES, Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
        if (expectedJsonSchemaType == null) {
            Assertions.assertTrue(result.isEmpty());
        } else if (expectedFormat == null) {
            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals(expectedJsonSchemaType.forVersion(schemaVersion),
                    result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        } else {
            Assertions.assertEquals(2, result.size());
            Assertions.assertEquals(expectedJsonSchemaType.forVersion(schemaVersion),
                    result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
            Assertions.assertEquals(expectedFormat, result.get(SchemaKeyword.TAG_FORMAT.forVersion(schemaVersion)).asText());
        }
    }

    @ParameterizedTest
    @MethodSource("parametersForTestGenerateSchema_SimpleTypeWithoutFormat")
    public void testGenerateSchema_SimpleType_withAdditionalPropertiesOption(Class<?> targetType, SchemaKeyword expectedJsonSchemaType,
            SchemaVersion schemaVersion) throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(schemaVersion)
                .with(Option.ADDITIONAL_FIXED_TYPES, Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
        if (expectedJsonSchemaType == null) {
            Assertions.assertTrue(result.isEmpty());
        } else {
            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals(expectedJsonSchemaType.forVersion(schemaVersion),
                    result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        }
    }
}
