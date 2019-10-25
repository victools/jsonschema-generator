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

package com.github.victools.jsonschema.generator.impl.module;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.AbstractTypeAwareTest;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Test for the {@link EnumModule} class.
 */
public class EnumModuleTest extends AbstractTypeAwareTest {

    private EnumModule instanceAsStrings;
    private EnumModule instanceAsObjects;
    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    public EnumModuleTest() {
        super(TestEnum.class);
    }

    @Before
    public void setUp() {
        this.instanceAsStrings = EnumModule.asStrings();
        this.instanceAsObjects = EnumModule.asObjects();
        this.builder = Mockito.spy(new SchemaGeneratorConfigBuilder(new ObjectMapper(), new OptionPreset()));
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        this.methodConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(this.builder.forFields()).thenReturn(this.fieldConfigPart);
        Mockito.when(this.builder.forMethods()).thenReturn(this.methodConfigPart);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyToConfigBuilder_asStrings() {
        this.instanceAsStrings.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).getObjectMapper();
        Mockito.verify(this.builder).with(Mockito.any(CustomDefinitionProvider.class));
        Mockito.verifyNoMoreInteractions(this.builder);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyToConfigBuilder_asObjects() {
        this.instanceAsObjects.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forFields();
        Mockito.verify(this.builder).forMethods();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.fieldConfigPart).withIgnoreCheck(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.fieldConfigPart);
        Mockito.verify(this.methodConfigPart).withIgnoreCheck(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNullableCheck(Mockito.any());
        Mockito.verify(this.methodConfigPart).withEnumResolver(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.methodConfigPart);
    }

    @Test
    public void testCustomSchemaDefinition_asStrings() {
        this.instanceAsStrings.applyToConfigBuilder(this.builder);
        ArgumentCaptor<CustomDefinitionProvider> captor = ArgumentCaptor.forClass(CustomDefinitionProvider.class);
        Mockito.verify(this.builder).with(captor.capture());

        ResolvedType testEnumType = this.getContext().resolve(TestEnum.class);
        CustomDefinition schemaDefinition = captor.getValue().provideCustomSchemaDefinition(testEnumType, this.getContext());
        Assert.assertFalse(schemaDefinition.isMeantToBeInline());
        ObjectNode node = schemaDefinition.getValue();
        Assert.assertEquals(2, node.size());

        JsonNode typeNode = node.get(SchemaConstants.TAG_TYPE);
        Assert.assertEquals(JsonNodeType.STRING, typeNode.getNodeType());
        Assert.assertEquals(SchemaConstants.TAG_TYPE_STRING, typeNode.textValue());

        JsonNode enumNode = node.get(SchemaConstants.TAG_ENUM);
        Assert.assertEquals(JsonNodeType.ARRAY, enumNode.getNodeType());
        Assert.assertEquals(3, ((ArrayNode) enumNode).size());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(0).getNodeType());
        Assert.assertEquals("VALUE1", enumNode.get(0).textValue());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(1).getNodeType());
        Assert.assertEquals("VALUE2", enumNode.get(1).textValue());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(2).getNodeType());
        Assert.assertEquals("VALUE3", enumNode.get(2).textValue());
    }

    private static enum TestEnum {
        VALUE1, VALUE2, VALUE3;
    }
}
