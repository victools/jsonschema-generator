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
import java.util.Arrays;
import java.util.EnumSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link SchemaGenerator} class.
 */
@RunWith(JUnitParamsRunner.class)
public class SchemaGeneratorSimpleTypesTest {

    private Object[][] getSimpleTypeCombinations() {
        return new Object[][]{
            {Object.class, null, null},
            {String.class, SchemaKeyword.TAG_TYPE_STRING, null},
            {Character.class, SchemaKeyword.TAG_TYPE_STRING, null},
            {char.class, SchemaKeyword.TAG_TYPE_STRING, null},
            {CharSequence.class, SchemaKeyword.TAG_TYPE_STRING, null},
            {Byte.class, SchemaKeyword.TAG_TYPE_STRING, null},
            {byte.class, SchemaKeyword.TAG_TYPE_STRING, null},
            {Boolean.class, SchemaKeyword.TAG_TYPE_BOOLEAN, null},
            {boolean.class, SchemaKeyword.TAG_TYPE_BOOLEAN, null},
            {Integer.class, SchemaKeyword.TAG_TYPE_INTEGER, "int32"},
            {int.class, SchemaKeyword.TAG_TYPE_INTEGER, "int32"},
            {Long.class, SchemaKeyword.TAG_TYPE_INTEGER, "int64"},
            {long.class, SchemaKeyword.TAG_TYPE_INTEGER, "int64"},
            {Short.class, SchemaKeyword.TAG_TYPE_INTEGER, null},
            {short.class, SchemaKeyword.TAG_TYPE_INTEGER, null},
            {Double.class, SchemaKeyword.TAG_TYPE_NUMBER, "double"},
            {double.class, SchemaKeyword.TAG_TYPE_NUMBER, "double"},
            {Float.class, SchemaKeyword.TAG_TYPE_NUMBER, "float"},
            {float.class, SchemaKeyword.TAG_TYPE_NUMBER, "float"},
            {java.time.LocalDate.class, SchemaKeyword.TAG_TYPE_STRING, "date"},
            {java.time.LocalDateTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"},
            {java.time.LocalTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"},
            {java.time.ZonedDateTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"},
            {java.time.OffsetDateTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"},
            {java.time.OffsetTime.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"},
            {java.time.Instant.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"},
            {java.util.Date.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"},
            {java.util.Calendar.class, SchemaKeyword.TAG_TYPE_STRING, "date-time"},
            {java.util.UUID.class, SchemaKeyword.TAG_TYPE_STRING, "uuid"},
            {java.time.ZoneId.class, SchemaKeyword.TAG_TYPE_STRING, null},
            {java.math.BigInteger.class, SchemaKeyword.TAG_TYPE_INTEGER, null},
            {java.math.BigDecimal.class, SchemaKeyword.TAG_TYPE_NUMBER, null},
            {Number.class, SchemaKeyword.TAG_TYPE_NUMBER, null},
        };
    }

    Object[] parametersForTestGenerateSchema_SimpleTypeWithoutFormat() {
        Object[][] typeCombinations = this.getSimpleTypeCombinations();
        return EnumSet.allOf(SchemaVersion.class).stream()
                .flatMap(schemaVersion -> Arrays.stream(typeCombinations).map(entry -> new Object[]{entry[0], entry[1], schemaVersion}))
                .toArray();
    }

    Object[] parametersForTestGenerateSchema_SimpleTypeWithFormat() {
        Object[][] typeCombinations = this.getSimpleTypeCombinations();
        return EnumSet.allOf(SchemaVersion.class).stream()
                .flatMap(schemaVersion -> Arrays.stream(typeCombinations).map(entry -> new Object[]{entry[0], entry[1], entry[2], schemaVersion}))
                .toArray();
    }

    @Test
    @Parameters
    public void testGenerateSchema_SimpleTypeWithoutFormat(Class<?> targetType, SchemaKeyword expectedJsonSchemaType, SchemaVersion schemaVersion)
            throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(schemaVersion)
                .with(Option.ADDITIONAL_FIXED_TYPES)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
        if (expectedJsonSchemaType == null) {
            Assert.assertTrue(result.isEmpty());
        } else {
            Assert.assertEquals(1, result.size());
            Assert.assertEquals(expectedJsonSchemaType.forVersion(schemaVersion),
                    result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        }
    }

    @Test
    @Parameters
    public void testGenerateSchema_SimpleTypeWithFormat(Class<?> targetType, SchemaKeyword expectedJsonSchemaType, String expectedFormat,
            SchemaVersion schemaVersion) throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(schemaVersion)
                .with(Option.ADDITIONAL_FIXED_TYPES, Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
        if (expectedJsonSchemaType == null) {
            Assert.assertTrue(result.isEmpty());
        } else if (expectedFormat == null) {
            Assert.assertEquals(1, result.size());
            Assert.assertEquals(expectedJsonSchemaType.forVersion(schemaVersion),
                    result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        } else {
            Assert.assertEquals(2, result.size());
            Assert.assertEquals(expectedJsonSchemaType.forVersion(schemaVersion),
                    result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
            Assert.assertEquals(expectedFormat, result.get(SchemaKeyword.TAG_FORMAT.forVersion(schemaVersion)).asText());
        }
    }

    @Test
    @Parameters(method = "parametersForTestGenerateSchema_SimpleTypeWithoutFormat")
    public void testGenerateSchema_SimpleType_withAdditionalPropertiesOption(Class<?> targetType, SchemaKeyword expectedJsonSchemaType,
            SchemaVersion schemaVersion) throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(schemaVersion)
                .with(Option.ADDITIONAL_FIXED_TYPES, Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
        if (expectedJsonSchemaType == null) {
            Assert.assertTrue(result.isEmpty());
        } else {
            Assert.assertEquals(1, result.size());
            Assert.assertEquals(expectedJsonSchemaType.forVersion(schemaVersion),
                    result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        }
    }
}
