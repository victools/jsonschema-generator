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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.AbstractTypeAwareTest;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test for the {@link SchemaGenerationContextImpl} class.
 */
public class SchemaGenerationContextImplTest extends AbstractTypeAwareTest {

    private SchemaGenerationContextImpl contextImpl;

    public SchemaGenerationContextImplTest() {
        super(TestClass.class);
    }

    @BeforeEach
    public void setUp() {
        SchemaVersion schemaVersion = SchemaVersion.DRAFT_2019_09;
        this.prepareContextForVersion(schemaVersion);

        SchemaGeneratorConfig config = Mockito.mock(SchemaGeneratorConfig.class);
        Mockito.when(config.getSchemaVersion()).thenReturn(schemaVersion);
        Mockito.when(config.getKeyword(Mockito.any()))
                .thenAnswer(invocation -> ((SchemaKeyword) invocation.getArgument(0)).forVersion(schemaVersion));
        Mockito.when(config.resolveTitle(Mockito.any(FieldScope.class))).thenReturn("Field Title");
        Mockito.when(config.resolveTitle(Mockito.any(MethodScope.class))).thenReturn("Method Title");
        Mockito.when(config.resolveDescriptionForType(Mockito.any())).thenReturn("Type Description");
        Mockito.when(config.resolveStringMinLength(Mockito.any(FieldScope.class))).thenReturn(null);
        Mockito.when(config.resolveStringMaxLength(Mockito.any(FieldScope.class))).thenReturn(null);
        Mockito.when(config.resolveArrayMinItems(Mockito.any(FieldScope.class))).thenReturn(null);
        Mockito.when(config.resolveArrayMaxItems(Mockito.any(FieldScope.class))).thenReturn(null);
        Mockito.when(config.resolveArrayUniqueItems(Mockito.any(FieldScope.class))).thenReturn(null);
        Mockito.when(config.resolveStringMinLength(Mockito.any(MethodScope.class))).thenReturn(null);
        Mockito.when(config.resolveStringMaxLength(Mockito.any(MethodScope.class))).thenReturn(null);
        Mockito.when(config.resolveArrayMinItems(Mockito.any(MethodScope.class))).thenReturn(null);
        Mockito.when(config.resolveArrayMaxItems(Mockito.any(MethodScope.class))).thenReturn(null);
        Mockito.when(config.resolveArrayUniqueItems(Mockito.any(MethodScope.class))).thenReturn(null);
        Mockito.when(config.resolveStringMinLengthForType(Mockito.any())).thenReturn(null);
        Mockito.when(config.resolveStringMaxLengthForType(Mockito.any())).thenReturn(null);
        Mockito.when(config.resolveArrayMinItemsForType(Mockito.any())).thenReturn(null);
        Mockito.when(config.resolveArrayMaxItemsForType(Mockito.any())).thenReturn(null);
        Mockito.when(config.resolveArrayUniqueItemsForType(Mockito.any())).thenReturn(null);

        ObjectMapper objectMapper = new ObjectMapper();
        Mockito.when(config.createObjectNode()).then(_invocation -> objectMapper.createObjectNode());
        Mockito.when(config.createArrayNode()).then(_invocation -> objectMapper.createArrayNode());

        this.contextImpl = new SchemaGenerationContextImpl(config, this.getContext().getTypeContext());
    }

    @Test
    public void testHandlingDefinition_ExistentNode() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        ObjectNode definitionInput = Mockito.mock(ObjectNode.class);
        SchemaGenerationContextImpl returnValue = this.contextImpl.putDefinition(javaType, definitionInput, null);

        Assertions.assertSame(this.contextImpl, returnValue);
        Assertions.assertTrue(this.contextImpl.containsDefinition(javaType, null));
        DefinitionKey key = new DefinitionKey(javaType, null);
        Assertions.assertSame(definitionInput, this.contextImpl.getDefinition(key));
        Assertions.assertEquals(Collections.singleton(key), this.contextImpl.getDefinedTypes());
    }

    @Test
    public void testHandlingDefinition_EmptyContext() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        DefinitionKey key = new DefinitionKey(javaType, null);

        Assertions.assertFalse(this.contextImpl.containsDefinition(javaType, null));
        Assertions.assertNull(this.contextImpl.getDefinition(key));
        Assertions.assertEquals(Collections.<DefinitionKey>emptySet(), this.contextImpl.getDefinedTypes());
    }

    @Test
    public void testReference_SameType() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        DefinitionKey key = new DefinitionKey(javaType, null);

        // initially, all lists are empty
        Assertions.assertEquals(Collections.<ObjectNode>emptyList(), this.contextImpl.getReferences(key));
        Assertions.assertEquals(Collections.<ObjectNode>emptyList(), this.contextImpl.getNullableReferences(key));

        // adding a not-nullable entry creates the "references" list
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        SchemaGenerationContextImpl returnValue = this.contextImpl.addReference(javaType, referenceInputOne, null, false);
        Assertions.assertSame(this.contextImpl, returnValue);
        Assertions.assertEquals(Collections.singletonList(referenceInputOne), this.contextImpl.getReferences(key));
        Assertions.assertEquals(Collections.<ObjectNode>emptyList(), this.contextImpl.getNullableReferences(key));

        // adding another not-nullable entry adds it to the existing "references" list
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        returnValue = this.contextImpl.addReference(javaType, referenceInputTwo, null, false);
        Assertions.assertSame(this.contextImpl, returnValue);
        Assertions.assertEquals(Arrays.asList(referenceInputOne, referenceInputTwo), this.contextImpl.getReferences(key));
        Assertions.assertEquals(Collections.<ObjectNode>emptyList(), this.contextImpl.getNullableReferences(key));

        // adding a nullable entry creates the "nullableReferences" list
        ObjectNode referenceInputThree = Mockito.mock(ObjectNode.class);
        returnValue = this.contextImpl.addReference(javaType, referenceInputThree, null, true);
        Assertions.assertSame(this.contextImpl, returnValue);
        Assertions.assertEquals(Arrays.asList(referenceInputOne, referenceInputTwo), this.contextImpl.getReferences(key));
        Assertions.assertEquals(Collections.singletonList(referenceInputThree), this.contextImpl.getNullableReferences(key));
    }

    @Test
    public void testReference_DifferentTypes() {
        ResolvedType javaTypeOne = Mockito.mock(ResolvedType.class);
        DefinitionKey keyOne = new DefinitionKey(javaTypeOne, null);
        ResolvedType javaTypeTwo = Mockito.mock(ResolvedType.class);
        DefinitionKey keyTwo = new DefinitionKey(javaTypeTwo, null);

        // adding an entry creates the "references" list for that type
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        this.contextImpl.addReference(javaTypeOne, referenceInputOne, null, false);
        Assertions.assertEquals(Collections.singletonList(referenceInputOne), this.contextImpl.getReferences(keyOne));
        Assertions.assertEquals(Collections.<ObjectNode>emptyList(), this.contextImpl.getReferences(keyTwo));

        // adding an entry for another type creates a separate "references" list for this other type
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        this.contextImpl.addReference(javaTypeTwo, referenceInputTwo, null, false);
        Assertions.assertEquals(Collections.singletonList(referenceInputOne), this.contextImpl.getReferences(keyOne));
        Assertions.assertEquals(Collections.singletonList(referenceInputTwo), this.contextImpl.getReferences(keyTwo));
    }

    @Test
    public void testReference_DifferentIgnoredDefinitionProvider() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        CustomDefinitionProviderV2 providerOne = null;
        CustomDefinitionProviderV2 providerTwo = Mockito.mock(CustomDefinitionProviderV2.class);
        DefinitionKey keyOne = new DefinitionKey(javaType, providerOne);
        DefinitionKey keyTwo = new DefinitionKey(javaType, providerTwo);

        // adding an entry creates the "references" list for that type
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        this.contextImpl.addReference(javaType, referenceInputOne, providerOne, false);
        Assertions.assertEquals(Collections.singletonList(referenceInputOne), this.contextImpl.getReferences(keyOne));
        Assertions.assertEquals(Collections.<ObjectNode>emptyList(), this.contextImpl.getReferences(keyTwo));

        // adding an entry for the same type but other ignored definition provider creates a separate "references" list for this other type
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        this.contextImpl.addReference(javaType, referenceInputTwo, providerTwo, false);
        Assertions.assertEquals(Collections.singletonList(referenceInputOne), this.contextImpl.getReferences(keyOne));
        Assertions.assertEquals(Collections.singletonList(referenceInputTwo), this.contextImpl.getReferences(keyTwo));
    }

    @Test
    public void testCreateStandardDefinitionReferenceForField_noCustomDefinition() {
        FieldScope targetField = this.getTestClassField("booleanField");
        ObjectNode result = this.contextImpl.createStandardDefinitionReference(targetField, null);
        Assertions.assertEquals("{\"allOf\":[{},{\"title\":\"Field Title\"}]}", result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForField_withCustomTypeDefinition() {
        Mockito.doReturn(new CustomDefinition(this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom type"), true))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(ResolvedType.class), Mockito.any(), Mockito.any());
        FieldScope targetField = this.getTestClassField("booleanField");
        ObjectNode result = this.contextImpl.createStandardDefinitionReference(targetField, null);
        Assertions.assertEquals("{\"allOf\":[{\"$comment\":\"custom type\",\"description\":\"Type Description\"},{\"title\":\"Field Title\"}]}",
                result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForField_withCustomPropertyDefinition() {
        Mockito.doReturn(new CustomPropertyDefinition(this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom property")))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(FieldScope.class), Mockito.any(), Mockito.any());
        FieldScope targetField = this.getTestClassField("booleanField");
        ObjectNode result = this.contextImpl.createStandardDefinitionReference(targetField, null);
        Assertions.assertEquals("{\"$comment\":\"custom property\",\"title\":\"Field Title\",\"description\":\"Type Description\"}", result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForField_withCustomPropertyAndTypeDefinitions() {
        Mockito.doReturn(new CustomDefinition(this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom type"), true))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(ResolvedType.class), Mockito.any(), Mockito.any());
        Mockito.doReturn(new CustomPropertyDefinition(this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom property")))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(FieldScope.class), Mockito.any(), Mockito.any());
        FieldScope targetField = this.getTestClassField("booleanField");
        ObjectNode result = this.contextImpl.createStandardDefinitionReference(targetField, null);
        Assertions.assertEquals("{\"$comment\":\"custom property\",\"title\":\"Field Title\",\"description\":\"Type Description\"}", result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForMethod_noCustomDefinition() {
        MethodScope targetMethod = this.getTestClassMethod("isBooleanField");
        JsonNode result = this.contextImpl.createStandardDefinitionReference(targetMethod, null);
        Assertions.assertEquals("{\"allOf\":[{},{\"title\":\"Method Title\"}]}", result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForMethod_withCustomTypeDefinition() {
        Mockito.doReturn(new CustomDefinition(this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom type"), true))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(ResolvedType.class), Mockito.any(), Mockito.any());
        MethodScope targetMethod = this.getTestClassMethod("isBooleanField");
        JsonNode result = this.contextImpl.createStandardDefinitionReference(targetMethod, null);
        Assertions.assertEquals("{\"allOf\":[{\"$comment\":\"custom type\",\"description\":\"Type Description\"},{\"title\":\"Method Title\"}]}",
                result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForMethod_withCustomPropertyDefinition() {
        Mockito.doReturn(new CustomPropertyDefinition(this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom property")))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(MethodScope.class), Mockito.any(), Mockito.any());
        MethodScope targetMethod = this.getTestClassMethod("isBooleanField");
        JsonNode result = this.contextImpl.createStandardDefinitionReference(targetMethod, null);
        Assertions.assertEquals("{\"$comment\":\"custom property\",\"title\":\"Method Title\",\"description\":\"Type Description\"}", result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForMethod_withCustomPropertyAndTypeDefinitions() {
        Mockito.doReturn(new CustomDefinition(this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom type"), true))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(ResolvedType.class), Mockito.any(), Mockito.any());
        Mockito.doReturn(new CustomPropertyDefinition(this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom property")))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(MethodScope.class), Mockito.any(), Mockito.any());
        MethodScope targetMethod = this.getTestClassMethod("isBooleanField");
        JsonNode result = this.contextImpl.createStandardDefinitionReference(targetMethod, null);
        Assertions.assertEquals("{\"$comment\":\"custom property\",\"title\":\"Method Title\",\"description\":\"Type Description\"}", result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForField_withCustomTypeDefinitionExcludingAttributes() {
        ObjectNode customNode = this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom type");
        Mockito.doReturn(new CustomDefinition(customNode, CustomDefinition.INLINE_DEFINITION, CustomDefinition.EXCLUDING_ATTRIBUTES))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(ResolvedType.class), Mockito.any(), Mockito.any());
        FieldScope targetField = this.getTestClassField("booleanField");
        ObjectNode result = this.contextImpl.createStandardDefinitionReference(targetField, null);
        Assertions.assertEquals("{\"allOf\":[{\"$comment\":\"custom type\"},{\"title\":\"Field Title\"}]}", result.toString());
    }

    @Test
    public void testCreateStandardDefinitionReferenceForField_withCustomPropertyDefinitionExcludingAttributes() {
        ObjectNode customNode = this.contextImpl.getGeneratorConfig().createObjectNode().put("$comment", "custom property");
        Mockito.doReturn(new CustomPropertyDefinition(customNode, CustomDefinition.EXCLUDING_ATTRIBUTES))
                .when(this.contextImpl.getGeneratorConfig())
                .getCustomDefinition(Mockito.any(FieldScope.class), Mockito.any(), Mockito.any());
        FieldScope targetField = this.getTestClassField("booleanField");
        ObjectNode result = this.contextImpl.createStandardDefinitionReference(targetField, null);
        Assertions.assertEquals("{\"$comment\":\"custom property\"}", result.toString());
    }

    private static class TestClass {

        public boolean booleanField;

        public boolean isBooleanField() {
            return this.booleanField;
        }
    }
}
