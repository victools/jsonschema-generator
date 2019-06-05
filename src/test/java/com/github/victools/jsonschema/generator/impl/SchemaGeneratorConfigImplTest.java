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

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProvider;
import com.github.victools.jsonschema.generator.InstanceAttributeOverride;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.TypeAttributeOverride;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class SchemaGeneratorConfigImplTest extends AbstractTypeAwareTest {

    private SchemaGeneratorConfigImpl instance;
    private ObjectMapper objectMapper;
    private Map<Option, Boolean> options;
    private SchemaGeneratorConfigPart<ResolvedField> fieldConfigPart;
    private SchemaGeneratorConfigPart<ResolvedMethod> methodConfigPart;
    private List<CustomDefinitionProvider> customDefinitions;
    private List<TypeAttributeOverride> typeAttributeOverrides;

    public SchemaGeneratorConfigImplTest() {
        super(TestClass.class);
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        this.objectMapper = Mockito.mock(ObjectMapper.class);
        this.options = new HashMap<>();
        this.fieldConfigPart = Mockito.mock(SchemaGeneratorConfigPart.class);
        this.methodConfigPart = Mockito.mock(SchemaGeneratorConfigPart.class);
        this.customDefinitions = new ArrayList<>();
        this.typeAttributeOverrides = new ArrayList<>();
        this.instance = new SchemaGeneratorConfigImpl(this.objectMapper, this.options,
                this.fieldConfigPart, this.methodConfigPart, this.customDefinitions, this.typeAttributeOverrides);
        TypeResolver typeResolver = new TypeResolver();
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        ResolvedType resolvedTestClass = typeResolver.resolve(TestClass.class);
        this.testClassMembers = memberResolver.resolve(resolvedTestClass, null, null);
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
        Assert.assertNull(this.instance.getCustomDefinition(Mockito.mock(ResolvedType.class)));
    }

    @Test
    public void testGetCustomDefinition_withMappingReturningNull() {
        CustomDefinitionProvider provider = Mockito.mock(CustomDefinitionProvider.class);
        this.customDefinitions.add(provider);
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        Assert.assertNull(this.instance.getCustomDefinition(javaType));

        // ensure that the provider has been called and the given java type and type variable context were forward accordingly
        Mockito.verify(provider).provideCustomSchemaDefinition(javaType);
    }

    @Test
    public void testGetCustomDefinition_withMappingReturningValue() {
        CustomDefinition value = Mockito.mock(CustomDefinition.class);
        this.customDefinitions.add(_input -> value);
        Assert.assertSame(value, this.instance.getCustomDefinition(Mockito.mock(ResolvedType.class)));
    }

    @Test
    public void testGetCustomDefinition_withMultipleMappings() {
        CustomDefinition valueOne = Mockito.mock(CustomDefinition.class);
        CustomDefinition valueTwo = Mockito.mock(CustomDefinition.class);
        this.customDefinitions.add(_input -> null);
        this.customDefinitions.add(_input -> valueOne);
        this.customDefinitions.add(_input -> valueTwo);
        // ignoring definition look-ups that return null, but taking the first non-null definition
        Assert.assertSame(valueOne, this.instance.getCustomDefinition(Mockito.mock(ResolvedType.class)));
    }

    Object parametersForTestIsNullable() {
        return new Object[][]{
            {null, null, false},
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
        ResolvedField field = this.getTestClassField("field");
        Mockito.when(this.fieldConfigPart.isNullable(field, null, this.testClassMembers)).thenReturn(configResult);
        if (optionEnabled != null) {
            this.options.put(Option.NULLABLE_FIELDS_BY_DEFAULT, optionEnabled);
        }
        boolean result = this.instance.isNullable(field, null, this.testClassMembers);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    @Parameters(method = "parametersForTestIsNullable")
    public void testIsNullableMethod(Boolean configResult, Boolean optionEnabled, boolean expectedResult) throws Exception {
        ResolvedMethod method = this.getTestClassMethod("getField");
        Mockito.when(this.methodConfigPart.isNullable(method, null, this.testClassMembers)).thenReturn(configResult);
        if (optionEnabled != null) {
            this.options.put(Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT, optionEnabled);
        }
        boolean result = this.instance.isNullable(method, null, this.testClassMembers);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testGetTypeAttributeOverrides() {
        TypeAttributeOverride typeOverride = (typeNode, javaType, c) -> typeNode.put("$comment", javaType.getTypeName());
        this.typeAttributeOverrides.add(typeOverride);

        List<TypeAttributeOverride> result = this.instance.getTypeAttributeOverrides();
        Assert.assertEquals(1, result.size());
        Assert.assertSame(typeOverride, result.get(0));
    }

    @Test
    public void testGetFieldAttributeOverrides() {
        InstanceAttributeOverride<ResolvedField> instanceOverride = (node, field, t, p, c) -> node.put("$comment", field.getName());
        Mockito.when(this.fieldConfigPart.getInstanceAttributeOverrides()).thenReturn(Collections.singletonList(instanceOverride));

        List<InstanceAttributeOverride<ResolvedField>> result = this.instance.getFieldAttributeOverrides();
        Assert.assertEquals(1, result.size());
        Assert.assertSame(instanceOverride, result.get(0));
    }

    @Test
    public void testGetMethodAttributeOverrides() {
        InstanceAttributeOverride<ResolvedMethod> instanceOverride = (node, method, t, p, c) -> node.put("$comment", method.getName() + "()");
        Mockito.when(this.methodConfigPart.getInstanceAttributeOverrides()).thenReturn(Collections.singletonList(instanceOverride));

        List<InstanceAttributeOverride<ResolvedMethod>> result = this.instance.getMethodAttributeOverrides();
        Assert.assertEquals(1, result.size());
        Assert.assertSame(instanceOverride, result.get(0));
    }

    private static class TestClass {

        private String field;

        public String getField() {
            return this.field;
        }
    }
}
