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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for the {@link AttributeCollector} class.
 */
public class AttributeCollectorTest {

    private AttributeCollector collector;
    private ObjectMapper objectMapper;
    private ObjectNode definitionNode;

    @BeforeEach
    public void setUp() {
        this.objectMapper = new ObjectMapper();
        this.collector = new AttributeCollector(this.objectMapper);
        this.definitionNode = this.objectMapper.createObjectNode();
    }

    /**
     * Helper method for creating the test context for a given JSON Schema version.
     *
     * @param schemaVersion designated JSON Schema version
     * @return schema generation context for the given JSON Schema version
     */
    private SchemaGenerationContextImpl generateContext(SchemaVersion schemaVersion, Option... options) {
        SchemaGeneratorConfig generatorConfig = new SchemaGeneratorConfigImpl(
                this.objectMapper,
                schemaVersion,
                Stream.of(options).collect(Collectors.toSet()),
                new SchemaGeneratorGeneralConfigPart(),
                new SchemaGeneratorConfigPart<>(),
                new SchemaGeneratorConfigPart<>());
        return new SchemaGenerationContextImpl(generatorConfig, TypeContextFactory.createDefaultTypeContext());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetEnum_Null(SchemaVersion schemaVersion) {
        this.collector.setEnum(this.definitionNode, null, this.generateContext(schemaVersion));
        Assertions.assertTrue(this.definitionNode.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetEnum_singleConstValue(SchemaVersion schemaVersion) {
        this.collector.setEnum(this.definitionNode, Collections.singletonList("A"), this.generateContext(schemaVersion));
        JsonNode constNode = this.definitionNode.get(SchemaKeyword.TAG_CONST.forVersion(schemaVersion));
        Assertions.assertNotNull(constNode);
        Assertions.assertTrue(constNode.isTextual());
        Assertions.assertEquals("A", constNode.textValue());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetEnum_singleEnumValue(SchemaVersion schemaVersion) {
        this.collector.setEnum(this.definitionNode, Collections.singletonList("A"), this.generateContext(schemaVersion, Option.ENUM_KEYWORD_FOR_SINGLE_VALUES));
        JsonNode enumNode = this.definitionNode.get(SchemaKeyword.TAG_ENUM.forVersion(schemaVersion));
        Assertions.assertNotNull(enumNode);
        Assertions.assertTrue(enumNode.isArray());
        Assertions.assertEquals(1, enumNode.size());
        JsonNode singleValueItem = enumNode.get(0);
        Assertions.assertNotNull(singleValueItem);
        Assertions.assertTrue(singleValueItem.isTextual());
        Assertions.assertEquals("A", singleValueItem.textValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "ENUM_KEYWORD_FOR_SINGLE_VALUES"})
    public void testSetEnum_twoEnumValues(String optionMode) {
        Option[] options = optionMode.equals("default") ? new Option[0] : new Option[]{Option.ENUM_KEYWORD_FOR_SINGLE_VALUES};
        this.collector.setEnum(this.definitionNode, Arrays.asList("A", "B"), this.generateContext(SchemaVersion.DRAFT_2019_09, options));
        JsonNode enumNode = this.definitionNode.get(SchemaKeyword.TAG_ENUM.forVersion(SchemaVersion.DRAFT_2019_09));
        Assertions.assertNotNull(enumNode);
        Assertions.assertTrue(enumNode.isArray());
        Assertions.assertEquals(2, enumNode.size());
        JsonNode firstValueItem = enumNode.get(0);
        Assertions.assertNotNull(firstValueItem);
        Assertions.assertTrue(firstValueItem.isTextual());
        Assertions.assertEquals("A", firstValueItem.textValue());
        JsonNode secondValueItem = enumNode.get(1);
        Assertions.assertNotNull(secondValueItem);
        Assertions.assertTrue(secondValueItem.isTextual());
        Assertions.assertEquals("B", secondValueItem.textValue());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetAdditionalProperties_Null(SchemaVersion schemaVersion) {
        this.collector.setAdditionalProperties(this.definitionNode, null, this.generateContext(schemaVersion));
        Assertions.assertTrue(this.definitionNode.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetAdditionalProperties_ObjectClass(SchemaVersion schemaVersion) {
        this.collector.setAdditionalProperties(this.definitionNode, BooleanNode.TRUE, this.generateContext(schemaVersion));
        Assertions.assertTrue(this.definitionNode.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetAdditionalProperties_VoidClass(SchemaVersion schemaVersion) {
        this.collector.setAdditionalProperties(this.definitionNode, BooleanNode.FALSE, this.generateContext(schemaVersion));
        Assertions.assertEquals(1, this.definitionNode.size());
        JsonNode additionalPropertiesNode = this.definitionNode.get(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES.forVersion(schemaVersion));
        Assertions.assertNotNull(additionalPropertiesNode);
        Assertions.assertTrue(additionalPropertiesNode.isBoolean());
        Assertions.assertFalse(additionalPropertiesNode.asBoolean());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetAdditionalProperties_SpecificDefinition(SchemaVersion schemaVersion) {
        SchemaGenerationContextImpl generationContext = this.generateContext(schemaVersion);
        ResolvedType resolvedType = generationContext.getTypeContext().resolve(int.class);
        ObjectNode definition = generationContext.createDefinitionReference(resolvedType);
        this.collector.setAdditionalProperties(this.definitionNode, definition, generationContext);
        Assertions.assertEquals(1, this.definitionNode.size());
        JsonNode additionalPropertiesNode = this.definitionNode.get(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES.forVersion(schemaVersion));
        Assertions.assertNotNull(additionalPropertiesNode);
        Assertions.assertTrue(additionalPropertiesNode.isObject());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetPatternProperties_Null(SchemaVersion schemaVersion) {
        this.collector.setPatternProperties(this.definitionNode, null, this.generateContext(schemaVersion));
        Assertions.assertTrue(this.definitionNode.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetPatternProperties_Empty(SchemaVersion schemaVersion) {
        this.collector.setPatternProperties(this.definitionNode, Collections.emptyMap(), this.generateContext(schemaVersion));
        Assertions.assertTrue(this.definitionNode.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testSetPatternProperties_SpecificEntries(SchemaVersion schemaVersion) {
        SchemaGenerationContextImpl generationContext = this.generateContext(schemaVersion);
        LinkedHashMap<String, JsonNode> patterns = new LinkedHashMap<>();
        patterns.put("^intClass.*$", generationContext.createDefinitionReference(generationContext.getTypeContext().resolve(int.class)));
        patterns.put("^stringClass.*$", generationContext.createDefinitionReference(generationContext.getTypeContext().resolve(String.class)));
        this.collector.setPatternProperties(this.definitionNode, patterns, generationContext);
        Assertions.assertEquals(1, this.definitionNode.size());
        JsonNode patternPropertiesNode = this.definitionNode.get(SchemaKeyword.TAG_PATTERN_PROPERTIES.forVersion(schemaVersion));
        Assertions.assertNotNull(patternPropertiesNode);
        Assertions.assertTrue(patternPropertiesNode.isObject());
        Assertions.assertEquals(2, patternPropertiesNode.size());
        Assertions.assertTrue(patternPropertiesNode.get("^intClass.*$").isObject());
        Assertions.assertTrue(patternPropertiesNode.get("^stringClass.*$").isObject());
    }
}
