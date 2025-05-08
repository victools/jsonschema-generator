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
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.AbstractTypeAwareTest;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.stream.Stream;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for the {@link EnumModule} class.
 */
public class EnumModuleTest extends AbstractTypeAwareTest {

    private static EnumModule instanceAsStringsFromName;
    private static EnumModule instanceAsStringsFromToString;
    private static EnumModule instanceAsObjects;

    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorGeneralConfigPart typeConfigPart;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    public EnumModuleTest() {
        super(TestEnum.class);
    }

    @BeforeAll
    public static void setUp() {
        instanceAsStringsFromName = EnumModule.asStringsFromName();
        instanceAsStringsFromToString = EnumModule.asStringsFromToString();
        instanceAsObjects = EnumModule.asObjects();
    }

    /**
     * Initialise configuration builder instance for the given JSON Schema version.
     *
     * @param schemaVersion designated JSON Schema version
     */
    private void initConfigBuilder(SchemaVersion schemaVersion) {
        this.prepareContextForVersion(schemaVersion);
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion, new OptionPreset());
        this.typeConfigPart = Mockito.spy(configBuilder.forTypesInGeneral());
        this.fieldConfigPart = Mockito.spy(configBuilder.forFields());
        this.methodConfigPart = Mockito.spy(configBuilder.forMethods());
        this.builder = Mockito.spy(configBuilder);
        Mockito.when(this.builder.forTypesInGeneral()).thenReturn(this.typeConfigPart);
        Mockito.when(this.builder.forFields()).thenReturn(this.fieldConfigPart);
        Mockito.when(this.builder.forMethods()).thenReturn(this.methodConfigPart);
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testApplyToConfigBuilder_asStringsFromName(SchemaVersion schemaVersion) {
        this.initConfigBuilder(schemaVersion);
        instanceAsStringsFromName.applyToConfigBuilder(this.builder);

        Mockito.verify(this.typeConfigPart).withCustomDefinitionProvider(Mockito.any(CustomDefinitionProviderV2.class));
        Mockito.verify(this.builder).forTypesInGeneral();
        Mockito.verifyNoMoreInteractions(this.typeConfigPart, this.builder);
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testApplyToConfigBuilder_asStringsFromToString(SchemaVersion schemaVersion) {
        this.initConfigBuilder(schemaVersion);
        instanceAsStringsFromToString.applyToConfigBuilder(this.builder);

        Mockito.verify(this.typeConfigPart).withCustomDefinitionProvider(Mockito.any(CustomDefinitionProviderV2.class));
        Mockito.verify(this.builder).forTypesInGeneral();
        Mockito.verifyNoMoreInteractions(this.typeConfigPart, this.builder);
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testApplyToConfigBuilder_asObjects(SchemaVersion schemaVersion) {
        this.initConfigBuilder(schemaVersion);
        instanceAsObjects.applyToConfigBuilder(this.builder);

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

    static Stream<Arguments> parametersForTestCustomSchemaDefinition_asStrings() {
        setUp();
        return Stream.of(SchemaVersion.values())
                .flatMap(schemaVersion -> Stream.of(
                        Arguments.of(schemaVersion, instanceAsStringsFromName, "VALUE1", "VALUE2", "VALUE3"),
                        Arguments.of(schemaVersion, instanceAsStringsFromToString, "value1_toString", "value2_toString", "value3_toString")
                ));
    }

    @ParameterizedTest
    @MethodSource("parametersForTestCustomSchemaDefinition_asStrings")
    public void testCustomSchemaDefinition_asStrings(SchemaVersion schemaVersion, EnumModule instance,
            String value1, String value2, String value3) {
        this.initConfigBuilder(schemaVersion);
        instance.applyToConfigBuilder(this.builder);
        ArgumentCaptor<CustomDefinitionProviderV2> captor = ArgumentCaptor.forClass(CustomDefinitionProviderV2.class);
        Mockito.verify(this.typeConfigPart).withCustomDefinitionProvider(captor.capture());

        ResolvedType testEnumType = this.getContext().getTypeContext().resolve(TestEnum.class);
        CustomDefinition schemaDefinition = captor.getValue().provideCustomSchemaDefinition(testEnumType, this.getContext());
        Assertions.assertFalse(schemaDefinition.isMeantToBeInline());
        ObjectNode node = schemaDefinition.getValue();
        Assertions.assertEquals(2, node.size());

        JsonNode typeNode = node.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion));
        Assertions.assertEquals(JsonNodeType.STRING, typeNode.getNodeType());
        Assertions.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(schemaVersion), typeNode.textValue());

        JsonNode enumNode = node.get(SchemaKeyword.TAG_ENUM.forVersion(schemaVersion));
        Assertions.assertEquals(JsonNodeType.ARRAY, enumNode.getNodeType());
        Assertions.assertEquals(3, enumNode.size());
        Assertions.assertEquals(JsonNodeType.STRING, enumNode.get(0).getNodeType());
        Assertions.assertEquals(value1, enumNode.get(0).textValue());
        Assertions.assertEquals(JsonNodeType.STRING, enumNode.get(1).getNodeType());
        Assertions.assertEquals(value2, enumNode.get(1).textValue());
        Assertions.assertEquals(JsonNodeType.STRING, enumNode.get(2).getNodeType());
        Assertions.assertEquals(value3, enumNode.get(2).textValue());
    }

    @Test
    public void testRawEnumType_asString() throws JSONException {
        this.initConfigBuilder(SchemaVersion.DRAFT_2020_12);
        this.builder.with(Option.PUBLIC_NONSTATIC_FIELDS, Option.NONSTATIC_NONVOID_NONGETTER_METHODS);
        instanceAsStringsFromName.applyToConfigBuilder(this.builder);

        JsonNode enumSchema = new SchemaGenerator(this.builder.build()).generateSchema(TestType.class)
                .get(SchemaKeyword.TAG_PROPERTIES.forVersion(SchemaVersion.DRAFT_2020_12))
                .get("rawEnum");
        JSONAssert.assertEquals("{\"type\":\"string\"}", enumSchema.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testRawEnumType_asObject() throws JSONException {
        this.initConfigBuilder(SchemaVersion.DRAFT_2020_12);
        this.builder.with(Option.PUBLIC_NONSTATIC_FIELDS, Option.NONSTATIC_NONVOID_NONGETTER_METHODS);
        instanceAsObjects.applyToConfigBuilder(this.builder);

        JsonNode enumSchema = new SchemaGenerator(this.builder.build()).generateSchema(TestType.class)
                .get(SchemaKeyword.TAG_PROPERTIES.forVersion(SchemaVersion.DRAFT_2020_12))
                .get("rawEnum");
        JSONAssert.assertEquals("{\"type\":\"object\",\"properties\":{\"compareTo(Enum<Object>)\":{\"type\":\"integer\"},\"name()\":{\"type\":\"string\"}}}",
                enumSchema.toString(), JSONCompareMode.STRICT);
    }

    private enum TestEnum {
        VALUE1, VALUE2, VALUE3;

        @Override
        public String toString() {
            return this.name().toLowerCase() + "_toString";
        }
    }

    private static class TestType {
        public Enum rawEnum;
    }
}
