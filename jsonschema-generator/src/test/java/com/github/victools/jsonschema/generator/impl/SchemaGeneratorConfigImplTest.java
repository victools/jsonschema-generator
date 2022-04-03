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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.AbstractTypeAwareTest;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.InstanceAttributeOverrideV2;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Test for the {@link SchemaGeneratorConfigImpl} class.
 */
public class SchemaGeneratorConfigImplTest extends AbstractTypeAwareTest {

    private SchemaGeneratorConfigImpl instance;
    @Mock
    private ObjectMapper objectMapper;
    private Set<Option> enabledOptions;
    @Mock
    private SchemaGeneratorGeneralConfigPart typesInGeneralConfigPart;
    @Mock
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    @Mock
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    private AutoCloseable mockProvider;

    public SchemaGeneratorConfigImplTest() {
        super(TestClass.class);
    }

    @BeforeEach
    public void setUp() {
        this.mockProvider = MockitoAnnotations.openMocks(this);
        this.enabledOptions = new HashSet<>();

        this.instance = new SchemaGeneratorConfigImpl(this.objectMapper, SchemaVersion.DRAFT_2019_09, this.enabledOptions,
                this.typesInGeneralConfigPart, this.fieldConfigPart, this.methodConfigPart);
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.mockProvider.close();
    }

    @Test
    public void testShouldCreateDefinitionsForAllObjects_default() {
        boolean defaultDisabled = this.instance.shouldCreateDefinitionsForAllObjects();
        Assertions.assertFalse(defaultDisabled);
    }

    @Test
    public void testShouldCreateDefinitionsForAllObjects_optionEnabled() {
        this.enabledOptions.add(Option.DEFINITIONS_FOR_ALL_OBJECTS);
        boolean specificallyEnabled = this.instance.shouldCreateDefinitionsForAllObjects();
        Assertions.assertTrue(specificallyEnabled);
    }

    @Test
    public void testCreateObjectNode() {
        ObjectNode node = Mockito.mock(ObjectNode.class);
        Mockito.when(this.objectMapper.createObjectNode()).thenReturn(node);

        Assertions.assertSame(node, this.instance.createObjectNode());
    }

    @Test
    public void testCreateArrayNode() {
        ArrayNode node = Mockito.mock(ArrayNode.class);
        Mockito.when(this.objectMapper.createArrayNode()).thenReturn(node);

        Assertions.assertSame(node, this.instance.createArrayNode());
    }

    @Test
    public void testGetCustomDefinition_noMapping() {
        Assertions.assertNull(this.instance.getCustomDefinition(Mockito.mock(ResolvedType.class), this.getContext(), null));
    }

    @Test
    public void testGetCustomDefinition_withMappingReturningNull() {
        CustomDefinitionProviderV2 provider = Mockito.mock(CustomDefinitionProviderV2.class);
        Mockito.when(this.typesInGeneralConfigPart.getCustomDefinitionProviders()).thenReturn(Collections.singletonList(provider));
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        Assertions.assertNull(this.instance.getCustomDefinition(javaType, this.getContext(), null));

        // ensure that the provider has been called and the given java type and type variable context were forward accordingly
        Mockito.verify(provider).provideCustomSchemaDefinition(javaType, this.getContext());
    }

    @Test
    public void testGetCustomDefinition_withMappingReturningValue() {
        CustomDefinition value = Mockito.mock(CustomDefinition.class);
        Mockito.when(this.typesInGeneralConfigPart.getCustomDefinitionProviders())
                .thenReturn(Collections.singletonList((type, context) -> value));
        Assertions.assertSame(value, this.instance.getCustomDefinition(Mockito.mock(ResolvedType.class), this.getContext(), null));
    }

    @Test
    public void testGetCustomDefinition_withMultipleMappings() {
        CustomDefinition valueOne = Mockito.mock(CustomDefinition.class);
        CustomDefinition valueTwo = Mockito.mock(CustomDefinition.class);
        Mockito.when(this.typesInGeneralConfigPart.getCustomDefinitionProviders())
                .thenReturn(Arrays.asList((type, context) -> null, (type, context) -> valueOne, (type, context) -> valueTwo));
        // ignoring definition look-ups that return null, but taking the first non-null definition
        Assertions.assertSame(valueOne, this.instance.getCustomDefinition(Mockito.mock(ResolvedType.class), this.getContext(), null));
    }

    static Stream<Arguments> parametersForTestIsNullable() {
        return Stream.of(
            Arguments.of(null, true, true),
            Arguments.of(null, false, false),
            Arguments.of(Boolean.TRUE, true, true),
            Arguments.of(Boolean.TRUE, false, true),
            Arguments.of(Boolean.FALSE, true, false),
            Arguments.of(Boolean.FALSE, false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestIsNullable")
    public void testIsNullableField(Boolean configResult, boolean optionEnabled, boolean expectedResult) throws Exception {
        FieldScope field = this.getTestClassField("field");
        Mockito.when(this.fieldConfigPart.isNullable(field)).thenReturn(configResult);
        if (optionEnabled) {
            this.enabledOptions.add(Option.NULLABLE_FIELDS_BY_DEFAULT);
        }
        boolean result = this.instance.isNullable(field);
        Assertions.assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @MethodSource("parametersForTestIsNullable")
    public void testIsNullableMethod(Boolean configResult, boolean optionEnabled, boolean expectedResult) throws Exception {
        MethodScope method = this.getTestClassMethod("getField");
        Mockito.when(this.methodConfigPart.isNullable(method)).thenReturn(configResult);
        if (optionEnabled) {
            this.enabledOptions.add(Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT);
        }
        boolean result = this.instance.isNullable(method);
        Assertions.assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testIsRequiredField(boolean configuredAndExpectedResult) throws Exception {
        FieldScope field = this.getTestClassField("field");
        Mockito.when(this.fieldConfigPart.isRequired(field)).thenReturn(configuredAndExpectedResult);
        boolean result = this.instance.isRequired(field);
        Assertions.assertEquals(configuredAndExpectedResult, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testIsRequiredMethod(boolean configuredAndExpectedResult) throws Exception {
        MethodScope method = this.getTestClassMethod("getField");
        Mockito.when(this.methodConfigPart.isRequired(method)).thenReturn(configuredAndExpectedResult);
        boolean result = this.instance.isRequired(method);
        Assertions.assertEquals(configuredAndExpectedResult, result);
    }

    @Test
    public void testGetTypeAttributeOverrides() {
        TypeAttributeOverrideV2 typeOverride = (typeNode, type, c) -> typeNode.put("$comment", type.getSimpleTypeDescription());
        Mockito.when(this.typesInGeneralConfigPart.getTypeAttributeOverrides()).thenReturn(Collections.singletonList(typeOverride));

        List<TypeAttributeOverrideV2> result = this.instance.getTypeAttributeOverrides();
        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(typeOverride, result.get(0));
    }

    @Test
    public void testGetFieldAttributeOverrides() {
        InstanceAttributeOverrideV2<FieldScope> instanceOverride = (node, field, _context) -> node.put("$comment", field.getName());
        Mockito.when(this.fieldConfigPart.getInstanceAttributeOverrides()).thenReturn(Collections.singletonList(instanceOverride));

        List<InstanceAttributeOverrideV2<FieldScope>> result = this.instance.getFieldAttributeOverrides();
        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(instanceOverride, result.get(0));
    }

    @Test
    public void testGetMethodAttributeOverrides() {
        InstanceAttributeOverrideV2<MethodScope> instanceOverride = (node, method, _context) -> node.put("$comment", method.getName() + "()");
        Mockito.when(this.methodConfigPart.getInstanceAttributeOverrides()).thenReturn(Collections.singletonList(instanceOverride));

        List<InstanceAttributeOverrideV2<MethodScope>> result = this.instance.getMethodAttributeOverrides();
        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(instanceOverride, result.get(0));
    }

    private static class TestClass {

        private String field;

        public String getField() {
            return this.field;
        }
    }
}
