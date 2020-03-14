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

    Object parametersForTestGenerateSchema_SimpleType() {
        Object[][] typeCombinations = new Object[][]{
            {Object.class, null},
            {String.class, SchemaKeyword.TAG_TYPE_STRING},
            {Character.class, SchemaKeyword.TAG_TYPE_STRING},
            {char.class, SchemaKeyword.TAG_TYPE_STRING},
            {CharSequence.class, SchemaKeyword.TAG_TYPE_STRING},
            {Boolean.class, SchemaKeyword.TAG_TYPE_BOOLEAN},
            {boolean.class, SchemaKeyword.TAG_TYPE_BOOLEAN},
            {Integer.class, SchemaKeyword.TAG_TYPE_INTEGER},
            {int.class, SchemaKeyword.TAG_TYPE_INTEGER},
            {Long.class, SchemaKeyword.TAG_TYPE_INTEGER},
            {long.class, SchemaKeyword.TAG_TYPE_INTEGER},
            {Short.class, SchemaKeyword.TAG_TYPE_INTEGER},
            {short.class, SchemaKeyword.TAG_TYPE_INTEGER},
            {Byte.class, SchemaKeyword.TAG_TYPE_INTEGER},
            {byte.class, SchemaKeyword.TAG_TYPE_INTEGER},
            {Double.class, SchemaKeyword.TAG_TYPE_NUMBER},
            {double.class, SchemaKeyword.TAG_TYPE_NUMBER},
            {Float.class, SchemaKeyword.TAG_TYPE_NUMBER},
            {float.class, SchemaKeyword.TAG_TYPE_NUMBER}
        };
        return EnumSet.allOf(SchemaVersion.class).stream()
                .flatMap(schemaVersion -> Arrays.stream(typeCombinations).map(entry -> new Object[]{entry[0], entry[1], schemaVersion}))
                .toArray();
    }

    @Test
    @Parameters
    public void testGenerateSchema_SimpleType(Class<?> targetType, SchemaKeyword expectedJsonSchemaType, SchemaVersion schemaVersion)
            throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper()).build();
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
    @Parameters(method = "parametersForTestGenerateSchema_SimpleType")
    public void testGenerateSchema_SimpleType_withAdditionalPropertiesOption(Class<?> targetType, SchemaKeyword expectedJsonSchemaType,
            SchemaVersion schemaVersion) throws Exception {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper())
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
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
