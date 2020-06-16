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

package com.github.victools.jsonschema.generator.naming;

import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.naming.DefaultSchemaDefinitionNamingStrategy;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.impl.SchemaCleanUpUtils;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.math.BigDecimal;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test/examples of possible {@link SchemaDefinitionNamingStrategy} implementations.
 */
@RunWith(JUnitParamsRunner.class)
public class SchemaDefinitionNamingStrategyTest {

    private static TypeContext typeContext = TypeContextFactory.createDefaultTypeContext();
    private DefinitionKey key;
    private SchemaGenerationContext generationContext;

    @AfterClass
    public static void discardTypeContext() {
        SchemaDefinitionNamingStrategyTest.typeContext = null;
    }

    @Before
    public void setUp() {
        this.key = Mockito.mock(DefinitionKey.class);
        this.generationContext = Mockito.mock(SchemaGenerationContext.class);
        Mockito.when(this.generationContext.getTypeContext()).thenReturn(SchemaDefinitionNamingStrategyTest.typeContext);
    }

    public Object[] parametersForTestExampleStrategy() {
        PropertyNamingStrategyBase jacksonSnakeCase = new PropertyNamingStrategy.SnakeCaseStrategy();
        SchemaDefinitionNamingStrategy snakeCase = new DefaultSchemaDefinitionNamingStrategy() {
            @Override
            public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
                return jacksonSnakeCase.translate(super.getDefinitionNameForKey(key, generationContext))
                        .replaceAll("<_", "<")
                        .replaceAll(", _", ",");
            }
        };
        PropertyNamingStrategyBase jacksonDotCase = new PropertyNamingStrategy.LowerDotCaseStrategy();
        SchemaDefinitionNamingStrategy dotCase = new DefaultSchemaDefinitionNamingStrategy() {
            @Override
            public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
                return jacksonDotCase.translate(super.getDefinitionNameForKey(key, generationContext))
                        .replaceAll("<.", "<")
                        .replaceAll(", .", "-");
            }
        };
        SchemaDefinitionNamingStrategy inclPackage = (definitionKey, context) -> context.getTypeContext()
                .getFullTypeDescription(definitionKey.getType());

        return new Object[][]{
            {"Snake Case", snakeCase, typeContext.resolve(BigDecimal.class), "big_decimal", "big_decimal"},
            {"Snake Case", snakeCase, typeContext.resolve(Map.class, String.class, BigDecimal.class),
                "map(string,big_decimal)", "map_string.big_decimal_"},
            {"Dot Case", dotCase, typeContext.resolve(BigDecimal.class), "big.decimal", "big.decimal"},
            {"Dot Case", dotCase, typeContext.resolve(Map.class, String.class, BigDecimal.class),
                "map(string-big.decimal)", "map_string-big.decimal_"},
            {"Incl. Package", inclPackage, typeContext.resolve(BigDecimal.class), "java.math.BigDecimal", "java.math.BigDecimal"},
            {"Incl. Package", inclPackage, typeContext.resolve(Map.class, String.class, BigDecimal.class),
                "java.util.Map(java.lang.String,java.math.BigDecimal)", "java.util.Map_java.lang.String.java.math.BigDecimal_"}
        };
    }

    @Test
    @Parameters
    @TestCaseName(value = "{method}({0}, {3} | {4}) [{index}]")
    public void testExampleStrategy(String caseTitle, SchemaDefinitionNamingStrategy strategy, ResolvedType type,
            String expectedUriCompatibleName, String expectedPlainName) {
        Mockito.when(this.key.getType()).thenReturn(type);
        String result = strategy.getDefinitionNameForKey(this.key, this.generationContext);
        // before the produced name is used in an actual schema, the SchemaCleanUpUtils come into play one way or another
        SchemaCleanUpUtils cleanUpUtils = new SchemaCleanUpUtils(null);
        Assert.assertEquals(expectedUriCompatibleName, cleanUpUtils.ensureDefinitionKeyIsUriCompatible(result));
        Assert.assertEquals(expectedPlainName, cleanUpUtils.ensureDefinitionKeyIsPlain(result));
    }
}
