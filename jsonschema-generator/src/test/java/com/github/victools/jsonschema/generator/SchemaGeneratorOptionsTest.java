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

package com.github.victools.jsonschema.generator;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for {@link SchemaGenerator} class.
 */
@RunWith(JUnitParamsRunner.class)
public class SchemaGeneratorOptionsTest {

    Object parametersForTestGenerateSchema() {
        return new Object[][]{
            {"properties in alphabetic order", null, Arrays.asList(
                "CONSTANT_A", "CONSTANT_B", "fieldInSupertype", "firstField", "lastField", "middleField",
                "firstMethod()", "lastMethod()", "methodInSupertype()", "middleMethod()"
                )},
            {"properties in declaration order", Option.PROPERTIES_IN_DECLARATION_ORDER, Arrays.asList(
                "fieldInSupertype", "firstField", "middleField", "lastField", "CONSTANT_B", "CONSTANT_A",
                "middleMethod()", "firstMethod()", "lastMethod()", "methodInSupertype()"
                )}
        };
    }

    @Test
    @Parameters
    @TestCaseName(value = "{method}({0}) [{index}]")
    public void testGenerateSchema(String testCaseName, Option additionalOption, List<String> sortedPropertyNames) throws Exception {
        final SchemaVersion schemaVersion = SchemaVersion.DRAFT_2019_09;
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion);
        if (additionalOption != null) {
            configBuilder.with(additionalOption);
        }
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        JsonNode result = generator.generateSchema(TestClass1.class);
        // ensure that the generated definition keys are valid URIs without any characters requiring encoding
        JsonNode properties = result.get(SchemaKeyword.TAG_PROPERTIES.forVersion(schemaVersion));
        Iterator<String> propertyNameIterator = properties.fieldNames();
        List<String> actualPropertyNames = new ArrayList<>();
        while (propertyNameIterator.hasNext()) {
            actualPropertyNames.add(propertyNameIterator.next());
        }

        Assert.assertEquals(sortedPropertyNames, actualPropertyNames);
    }

    private static class TestClass1 extends TestClass2 {

        public static final String CONSTANT_B = "B";

        public int firstField;
        public int middleField;
        public int lastField;

        public void firstMethod() {
        }

        public void middleMethod() {
        }

        public void lastMethod() {
        }
    }

    private static class TestClass2 {

        public static final String CONSTANT_A = "A";

        public int fieldInSupertype;

        public void methodInSupertype() {
        }
    }
}
