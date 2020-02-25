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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorTypeConfigPart;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the {@link AttributeCollector} class.
 */
public class AttributeCollectorTest {

    private AttributeCollector collector;
    private ObjectNode definitionNode;
    private SchemaGenerationContextImpl generationContext;

    @Before
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        this.collector = new AttributeCollector(objectMapper);
        this.definitionNode = objectMapper.createObjectNode();
        SchemaGeneratorConfigImpl generatorConfig = new SchemaGeneratorConfigImpl(
                objectMapper,
                Collections.emptySet(),
                new SchemaGeneratorTypeConfigPart<>(),
                new SchemaGeneratorConfigPart<>(),
                new SchemaGeneratorConfigPart<>(),
                Collections.emptyList(),
                Collections.emptyList());
        this.generationContext = new SchemaGenerationContextImpl(generatorConfig, TypeContextFactory.createDefaultTypeContext());
    }

    @Test
    public void testSetAdditionalProperties_Null() {
        this.collector.setAdditionalProperties(this.definitionNode, null, this.generationContext);
        Assert.assertTrue(this.definitionNode.isEmpty());
    }

    @Test
    public void testSetAdditionalProperties_ObjectClass() {
        this.collector.setAdditionalProperties(this.definitionNode, Object.class, this.generationContext);
        Assert.assertTrue(this.definitionNode.isEmpty());
    }

    @Test
    public void testSetAdditionalProperties_ResolvedObjectClass() {
        ResolvedType resolvedObjectClass = this.generationContext.getTypeContext().resolve(Object.class);
        this.collector.setAdditionalProperties(this.definitionNode, resolvedObjectClass, this.generationContext);
        Assert.assertTrue(this.definitionNode.isEmpty());
    }

    @Test
    public void testSetAdditionalProperties_VoidClass() {
        this.collector.setAdditionalProperties(this.definitionNode, Void.class, this.generationContext);
        Assert.assertEquals(1, this.definitionNode.size());
        JsonNode additionalPropertiesNode = this.definitionNode.get(SchemaConstants.TAG_ADDITIONAL_PROPERTIES);
        Assert.assertNotNull(additionalPropertiesNode);
        Assert.assertTrue(additionalPropertiesNode.isBoolean());
        Assert.assertFalse(additionalPropertiesNode.asBoolean());
    }

    @Test
    public void testSetAdditionalProperties_VoidType() {
        this.collector.setAdditionalProperties(this.definitionNode, Void.TYPE, this.generationContext);
        Assert.assertEquals(1, this.definitionNode.size());
        JsonNode additionalPropertiesNode = this.definitionNode.get(SchemaConstants.TAG_ADDITIONAL_PROPERTIES);
        Assert.assertNotNull(additionalPropertiesNode);
        Assert.assertTrue(additionalPropertiesNode.isBoolean());
        Assert.assertFalse(additionalPropertiesNode.asBoolean());
    }

    @Test
    public void testSetAdditionalProperties_SpecificClass() {
        this.collector.setAdditionalProperties(this.definitionNode, int.class, this.generationContext);
        Assert.assertEquals(1, this.definitionNode.size());
        JsonNode additionalPropertiesNode = this.definitionNode.get(SchemaConstants.TAG_ADDITIONAL_PROPERTIES);
        Assert.assertNotNull(additionalPropertiesNode);
        Assert.assertTrue(additionalPropertiesNode.isObject());
        ResolvedType resolvedType = this.generationContext.getTypeContext().resolve(int.class);
        Assert.assertTrue(this.generationContext.containsDefinition(resolvedType));
    }

    @Test
    public void testSetPatternProperties_Null() {
        this.collector.setPatternProperties(this.definitionNode, null, this.generationContext);
        Assert.assertTrue(this.definitionNode.isEmpty());
    }

    @Test
    public void testSetPatternProperties_Empty() {
        this.collector.setPatternProperties(this.definitionNode, Collections.emptyMap(), this.generationContext);
        Assert.assertTrue(this.definitionNode.isEmpty());
    }

    @Test
    public void testSetPatternProperties_ObjectClass() {
        LinkedHashMap<String, Type> patterns = new LinkedHashMap<>();
        patterns.put("^objectClass.*$", Object.class);
        this.collector.setPatternProperties(this.definitionNode, patterns, this.generationContext);
        Assert.assertEquals(1, this.definitionNode.size());
        JsonNode patternPropertiesNode = this.definitionNode.get(SchemaConstants.TAG_PATTERN_PROPERTIES);
        Assert.assertNotNull(patternPropertiesNode);
        Assert.assertTrue(patternPropertiesNode.isObject());
        Assert.assertEquals(1, patternPropertiesNode.size());
        Assert.assertTrue(patternPropertiesNode.get("^objectClass.*$").isObject());
        Assert.assertFalse(this.generationContext.containsDefinition(this.generationContext.getTypeContext().resolve(Object.class)));
    }

    @Test
    public void testSetPatternProperties_ResolvedObjectClass() {
        LinkedHashMap<String, Type> patterns = new LinkedHashMap<>();
        patterns.put("^resolvedObjectClass.*$", this.generationContext.getTypeContext().resolve(Object.class));
        this.collector.setPatternProperties(this.definitionNode, patterns, this.generationContext);
        Assert.assertEquals(1, this.definitionNode.size());
        JsonNode patternPropertiesNode = this.definitionNode.get(SchemaConstants.TAG_PATTERN_PROPERTIES);
        Assert.assertNotNull(patternPropertiesNode);
        Assert.assertTrue(patternPropertiesNode.isObject());
        Assert.assertEquals(1, patternPropertiesNode.size());
        Assert.assertTrue(patternPropertiesNode.get("^resolvedObjectClass.*$").isObject());
        Assert.assertFalse(this.generationContext.containsDefinition(this.generationContext.getTypeContext().resolve(Object.class)));
    }

    @Test
    public void testSetPatternProperties_SpecificEntries() {
        LinkedHashMap<String, Type> patterns = new LinkedHashMap<>();
        patterns.put("^intClass.*$", int.class);
        patterns.put("^resolvedStringClass.*$", this.generationContext.getTypeContext().resolve(String.class));
        this.collector.setPatternProperties(this.definitionNode, patterns, this.generationContext);
        Assert.assertEquals(1, this.definitionNode.size());
        JsonNode patternPropertiesNode = this.definitionNode.get(SchemaConstants.TAG_PATTERN_PROPERTIES);
        Assert.assertNotNull(patternPropertiesNode);
        Assert.assertTrue(patternPropertiesNode.isObject());
        Assert.assertEquals(2, patternPropertiesNode.size());
        Assert.assertTrue(patternPropertiesNode.get("^intClass.*$").isObject());
        Assert.assertTrue(patternPropertiesNode.get("^resolvedStringClass.*$").isObject());
        Assert.assertTrue(this.generationContext.containsDefinition(this.generationContext.getTypeContext().resolve(int.class)));
        Assert.assertTrue(this.generationContext.containsDefinition(this.generationContext.getTypeContext().resolve(String.class)));
    }
}
