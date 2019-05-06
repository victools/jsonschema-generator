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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test for the {@link SchemaGeneratorConfigImpl} class.
 */
@RunWith(JUnitParamsRunner.class)
public class SchemaGeneratorConfigImplTest {

    private SchemaGeneratorConfigImpl instance;
    private ObjectMapper objectMapper;
    private Map<Option, Boolean> options;
    private SchemaGeneratorConfigPart<Field> fieldConfigPart;
    private SchemaGeneratorConfigPart<Method> methodConfigPart;
    private Map<Class<?>, List<Function<Type, CustomDefinition>>> customDefinitions;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        this.objectMapper = Mockito.mock(ObjectMapper.class);
        this.options = new HashMap<>();
        this.fieldConfigPart = Mockito.mock(SchemaGeneratorConfigPart.class);
        this.methodConfigPart = Mockito.mock(SchemaGeneratorConfigPart.class);
        this.customDefinitions = new HashMap<>();
        this.instance = new SchemaGeneratorConfigImpl(this.objectMapper, this.options,
                this.fieldConfigPart, this.methodConfigPart, this.customDefinitions);
    }

    @Test
    public void testShouldCreateDefinitionsForAllObjects_default() {
        boolean defaultDisabled = this.instance.shouldCreateDefinitionsForAllObjects();
        Assert.assertFalse(defaultDisabled);
    }

    @Test
    public void testShouldCreateDefinitionsForAllObjects_optionEnabled() {
        this.options.put(Option.DEFINITIONS_FOR_ALL_OBJECTS, Boolean.TRUE);
        boolean specificallyEnabled = this.instance.shouldCreateDefinitionsForAllObjects();
        Assert.assertTrue(specificallyEnabled);
    }

    @Test
    public void testShouldCreateDefinitionsForAllObjects_optionDisabled() {
        this.options.put(Option.DEFINITIONS_FOR_ALL_OBJECTS, Boolean.FALSE);
        boolean specificallyDisabled = this.instance.shouldCreateDefinitionsForAllObjects();
        Assert.assertFalse(specificallyDisabled);
    }

    @Test
    public void testCreateObjectNode() {
        ObjectNode node = Mockito.mock(ObjectNode.class);
        Mockito.when(this.objectMapper.createObjectNode()).thenReturn(node);

        Assert.assertSame(node, this.instance.createObjectNode());
    }

    @Test
    public void testCreateArrayNode() {
        ArrayNode node = Mockito.mock(ArrayNode.class);
        Mockito.when(this.objectMapper.createArrayNode()).thenReturn(node);

        Assert.assertSame(node, this.instance.createArrayNode());
    }

    @Test
    public void testGetCustomDefinition_noMapping() {
        Type javaType = String.class;
        Assert.assertNull(this.instance.getCustomDefinition(javaType));
    }

    @Test
    public void testGetCustomDefinition_differentMapping() {
        Type javaType = long.class;
        Function<Type, CustomDefinition> otherDefinitionLookup = _input -> Mockito.mock(CustomDefinition.class);
        this.customDefinitions.put(int.class, Collections.singletonList(otherDefinitionLookup));
        Assert.assertNull(this.instance.getCustomDefinition(javaType));
    }

    @Test
    public void testGetCustomDefinition_withMappingReturningNull() {
        Class<?> javaType = double.class;
        Function<Type, CustomDefinition> definitionLookupReturningNull = _input -> null;
        this.customDefinitions.put(javaType, Collections.singletonList(definitionLookupReturningNull));
        Assert.assertNull(this.instance.getCustomDefinition(javaType));
    }

    @Test
    public void testGetCustomDefinition_withMappingReturningValue() {
        Class<?> javaType = Boolean.class;
        CustomDefinition value = Mockito.mock(CustomDefinition.class);
        Function<Type, CustomDefinition> definitionLookupReturningValue = _input -> value;
        this.customDefinitions.put(javaType, Collections.singletonList(definitionLookupReturningValue));
        Assert.assertSame(value, this.instance.getCustomDefinition(javaType));
    }

    @Test
    public void testGetCustomDefinition_withMultipleMappings() {
        Class<?> javaType = Float.class;
        Function<Type, CustomDefinition> lookupReturningNull = _input -> null;
        CustomDefinition valueOne = Mockito.mock(CustomDefinition.class);
        Function<Type, CustomDefinition> lookupReturningValue1 = _input -> valueOne;
        CustomDefinition valueTwo = Mockito.mock(CustomDefinition.class);
        Function<Type, CustomDefinition> lookupReturningValue2 = _input -> valueTwo;
        this.customDefinitions.put(javaType, Arrays.asList(lookupReturningNull, lookupReturningValue1, lookupReturningValue2));
        // ignoring definition look-ups that return null, but taking the first non-null definition
        Assert.assertSame(valueOne, this.instance.getCustomDefinition(javaType));
    }

    Object parametersForTestIsNullable() {
        return new Object[][]{
            {null, null, true},
            {null, Boolean.TRUE, true},
            {null, Boolean.FALSE, false},
            {Boolean.TRUE, null, true},
            {Boolean.TRUE, Boolean.TRUE, true},
            {Boolean.TRUE, Boolean.FALSE, true},
            {Boolean.FALSE, null, false},
            {Boolean.FALSE, Boolean.TRUE, false},
            {Boolean.FALSE, Boolean.FALSE, false}
        };
    }

    @Test
    @Parameters(method = "parametersForTestIsNullable")
    public void testIsNullableField(Boolean configResult, Boolean optionEnabled, boolean expectedResult) throws Exception {
        Field field = TestClass.class.getDeclaredField("field");
        Mockito.when(this.fieldConfigPart.isNullable(field)).thenReturn(configResult);
        if (optionEnabled != null) {
            this.options.put(Option.FIELDS_ARE_NULLABLE_BY_DEFAULT, optionEnabled);
        }
        boolean result = this.instance.isNullable(field);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    @Parameters(method = "parametersForTestIsNullable")
    public void testIsNullableMethod(Boolean configResult, Boolean optionEnabled, boolean expectedResult) throws Exception {
        Method method = TestClass.class.getDeclaredMethod("getField");
        Mockito.when(this.methodConfigPart.isNullable(method)).thenReturn(configResult);
        if (optionEnabled != null) {
            this.options.put(Option.METHODS_RETURN_NULLABLE_BY_DEFAULT, optionEnabled);
        }
        boolean result = this.instance.isNullable(method);
        Assert.assertEquals(expectedResult, result);
    }

    private static class TestClass {

        private String field;

        public String getField() {
            return this.field;
        }
    }
}
