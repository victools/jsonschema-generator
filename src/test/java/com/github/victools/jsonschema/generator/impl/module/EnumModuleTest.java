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
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Test for the {@link EnumModule} class.
 */
@RunWith(JUnitParamsRunner.class)
public class EnumModuleTest extends AbstractTypeAwareTest {

    private EnumModule instanceAsStringsFromName;
    private EnumModule instanceAsStringsFromToString;
    private EnumModule instanceAsObjects;
    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    public EnumModuleTest() {
        super(TestEnum.class);
    }

    @Before
    public void setUp() {
        this.instanceAsStringsFromName = EnumModule.asStringsFromName();
        this.instanceAsStringsFromToString = EnumModule.asStringsFromToString();
        this.instanceAsObjects = EnumModule.asObjects();
    }

    /**
     * Initialise configuration builder instance for the given JSON Schema version.
     *
     * @param schemaVersion designated JSON Schema version
     */
    private void initConfigBuilder(SchemaVersion schemaVersion) {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(new ObjectMapper(), schemaVersion, new OptionPreset());
        this.fieldConfigPart = Mockito.spy(configBuilder.forFields());
        this.methodConfigPart = Mockito.spy(configBuilder.forMethods());
        this.builder = Mockito.spy(configBuilder);
        Mockito.when(this.builder.forFields()).thenReturn(this.fieldConfigPart);
        Mockito.when(this.builder.forMethods()).thenReturn(this.methodConfigPart);
    }

    @Test
    @Parameters(source = SchemaVersion.class)
    public void testApplyToConfigBuilder_asStringsFromName(SchemaVersion schemaVersion) {
        this.initConfigBuilder(schemaVersion);
        this.instanceAsStringsFromName.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).getObjectMapper();
        Mockito.verify(this.builder).with(Mockito.any(CustomDefinitionProviderV2.class));
        Mockito.verifyNoMoreInteractions(this.builder);
    }

    @Test
    @Parameters(source = SchemaVersion.class)
    public void testApplyToConfigBuilder_asStringsFromToString(SchemaVersion schemaVersion) {
        this.initConfigBuilder(schemaVersion);
        this.instanceAsStringsFromToString.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).getObjectMapper();
        Mockito.verify(this.builder).with(Mockito.any(CustomDefinitionProviderV2.class));
        Mockito.verifyNoMoreInteractions(this.builder);
    }

    @Test
    @Parameters(source = SchemaVersion.class)
    public void testApplyToConfigBuilder_asObjects(SchemaVersion schemaVersion) {
        this.initConfigBuilder(schemaVersion);
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
    @Parameters(source = SchemaVersion.class)
    public void testCustomSchemaDefinition_asStringsFromName(SchemaVersion schemaVersion) {
        this.initConfigBuilder(schemaVersion);
        this.instanceAsStringsFromName.applyToConfigBuilder(this.builder);
        ArgumentCaptor<CustomDefinitionProviderV2> captor = ArgumentCaptor.forClass(CustomDefinitionProviderV2.class);
        Mockito.verify(this.builder).with(captor.capture());

        ResolvedType testEnumType = this.getContext().getTypeContext().resolve(TestEnum.class);
        CustomDefinition schemaDefinition = captor.getValue().provideCustomSchemaDefinition(testEnumType, this.getContext());
        Assert.assertFalse(schemaDefinition.isMeantToBeInline());
        ObjectNode node = schemaDefinition.getValue();
        Assert.assertEquals(2, node.size());

        JsonNode typeNode = node.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion));
        Assert.assertEquals(JsonNodeType.STRING, typeNode.getNodeType());
        Assert.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(schemaVersion), typeNode.textValue());

        JsonNode enumNode = node.get(SchemaKeyword.TAG_ENUM.forVersion(schemaVersion));
        Assert.assertEquals(JsonNodeType.ARRAY, enumNode.getNodeType());
        Assert.assertEquals(3, ((ArrayNode) enumNode).size());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(0).getNodeType());
        Assert.assertEquals("VALUE1", enumNode.get(0).textValue());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(1).getNodeType());
        Assert.assertEquals("VALUE2", enumNode.get(1).textValue());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(2).getNodeType());
        Assert.assertEquals("VALUE3", enumNode.get(2).textValue());
    }

    @Test
    @Parameters(source = SchemaVersion.class)
    public void testCustomSchemaDefinition_asStringsFromToString(SchemaVersion schemaVersion) {
        this.initConfigBuilder(schemaVersion);
        this.instanceAsStringsFromToString.applyToConfigBuilder(this.builder);
        ArgumentCaptor<CustomDefinitionProviderV2> captor = ArgumentCaptor.forClass(CustomDefinitionProviderV2.class);
        Mockito.verify(this.builder).with(captor.capture());

        ResolvedType testEnumType = this.getContext().getTypeContext().resolve(TestEnum.class);
        CustomDefinition schemaDefinition = captor.getValue().provideCustomSchemaDefinition(testEnumType, this.getContext());
        Assert.assertFalse(schemaDefinition.isMeantToBeInline());
        ObjectNode node = schemaDefinition.getValue();
        Assert.assertEquals(2, node.size());

        JsonNode typeNode = node.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion));
        Assert.assertEquals(JsonNodeType.STRING, typeNode.getNodeType());
        Assert.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(schemaVersion), typeNode.textValue());

        JsonNode enumNode = node.get(SchemaKeyword.TAG_ENUM.forVersion(schemaVersion));
        Assert.assertEquals(JsonNodeType.ARRAY, enumNode.getNodeType());
        Assert.assertEquals(3, ((ArrayNode) enumNode).size());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(0).getNodeType());
        Assert.assertEquals("value1_toString", enumNode.get(0).textValue());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(1).getNodeType());
        Assert.assertEquals("value2_toString", enumNode.get(1).textValue());
        Assert.assertEquals(JsonNodeType.STRING, enumNode.get(2).getNodeType());
        Assert.assertEquals("value3_toString", enumNode.get(2).textValue());
    }

    private static enum TestEnum {
        VALUE1, VALUE2, VALUE3;

        @Override
        public String toString() {
            return this.name().toLowerCase() + "_toString";
        }
    }
}
