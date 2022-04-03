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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.NamingBase;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.impl.SchemaCleanUpUtils;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * Test/examples of possible {@link SchemaDefinitionNamingStrategy} implementations.
 */
public class SchemaDefinitionNamingStrategyTest {

    private static TypeContext typeContext = TypeContextFactory.createDefaultTypeContext();
    private DefinitionKey key;
    private SchemaGenerationContext generationContext;

    @AfterAll
    public static void discardTypeContext() {
        SchemaDefinitionNamingStrategyTest.typeContext = null;
    }

    @BeforeEach
    public void setUp() {
        this.key = Mockito.mock(DefinitionKey.class);
        this.generationContext = Mockito.mock(SchemaGenerationContext.class);
        Mockito.when(this.generationContext.getTypeContext()).thenReturn(SchemaDefinitionNamingStrategyTest.typeContext);
    }

    static Stream<Arguments> parametersForTestExampleStrategy() {
        NamingBase jacksonSnakeCase = (NamingBase) PropertyNamingStrategies.SNAKE_CASE;
        SchemaDefinitionNamingStrategy snakeCase = new DefaultSchemaDefinitionNamingStrategy() {
            @Override
            public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
                return jacksonSnakeCase.translate(super.getDefinitionNameForKey(key, generationContext))
                        .replaceAll("<_", "<")
                        .replaceAll(", _", ",");
            }
        };
        NamingBase jacksonDotCase = (NamingBase) PropertyNamingStrategies.LOWER_DOT_CASE;
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

        return Stream.of(
            Arguments.of("Snake Case", snakeCase, typeContext.resolve(BigDecimal.class), "big_decimal", "big_decimal"),
            Arguments.of("Snake Case", snakeCase, typeContext.resolve(Map.class, String.class, BigDecimal.class),
                "map(string,big_decimal)", "map_string.big_decimal_"),
            Arguments.of("Dot Case", dotCase, typeContext.resolve(BigDecimal.class), "big.decimal", "big.decimal"),
            Arguments.of("Dot Case", dotCase, typeContext.resolve(Map.class, String.class, BigDecimal.class),
                "map(string-big.decimal)", "map_string-big.decimal_"),
            Arguments.of("Incl. Package", inclPackage, typeContext.resolve(BigDecimal.class), "java.math.BigDecimal", "java.math.BigDecimal"),
            Arguments.of("Incl. Package", inclPackage, typeContext.resolve(Map.class, String.class, BigDecimal.class),
                "java.util.Map(java.lang.String,java.math.BigDecimal)", "java.util.Map_java.lang.String.java.math.BigDecimal_")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestExampleStrategy")
    public void testExampleStrategy(String caseTitle, SchemaDefinitionNamingStrategy strategy, ResolvedType type,
            String expectedUriCompatibleName, String expectedPlainName) {
        Mockito.when(this.key.getType()).thenReturn(type);
        String result = strategy.getDefinitionNameForKey(this.key, this.generationContext);
        // before the produced name is used in an actual schema, the SchemaCleanUpUtils come into play one way or another
        SchemaCleanUpUtils cleanUpUtils = new SchemaCleanUpUtils(null);
        Assertions.assertEquals(expectedUriCompatibleName, cleanUpUtils.ensureDefinitionKeyIsUriCompatible(result));
        Assertions.assertEquals(expectedPlainName, cleanUpUtils.ensureDefinitionKeyIsPlain(result));
    }
}
