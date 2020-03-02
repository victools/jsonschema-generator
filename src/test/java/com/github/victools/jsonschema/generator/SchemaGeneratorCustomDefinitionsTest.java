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

package com.github.victools.jsonschema.generator;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link SchemaGenerator} class.
 */
public class SchemaGeneratorCustomDefinitionsTest {

    @Test
    public void testGenerateSchema_CustomDefinition() throws Exception {
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> javaType.getErasedType() == Integer.class
                ? new CustomDefinition(context.createDefinition(context.getTypeContext().resolve(String.class)))
                : null;
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper())
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(Integer.class);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(SchemaConstants.TAG_TYPE_STRING, result.get(SchemaConstants.TAG_TYPE).asText());
    }

    @Test
    public void testGenerateSchema_CustomCollectionDefinition() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        String accessProperty = "stream().findFirst().orElse(null)";
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> {
            if (!javaType.isInstanceOf(Collection.class)) {
                return null;
            }
            ResolvedType generic = context.getTypeContext().getContainerItemType(javaType);
            return new CustomDefinition(objectMapper.createObjectNode()
                    .put(SchemaConstants.TAG_TYPE, SchemaConstants.TAG_TYPE_OBJECT)
                    .set(SchemaConstants.TAG_PROPERTIES, objectMapper.createObjectNode()
                            .set(accessProperty, context.makeNullable(context.createDefinition(generic)))));
        };
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(objectMapper)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(ArrayList.class, String.class);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(SchemaConstants.TAG_TYPE_OBJECT, result.get(SchemaConstants.TAG_TYPE).asText());
        Assert.assertNotNull(result.get(SchemaConstants.TAG_PROPERTIES));
        Assert.assertNotNull(result.get(SchemaConstants.TAG_PROPERTIES).get(accessProperty));
        JsonNode accessPropertyType = result.get(SchemaConstants.TAG_PROPERTIES).get(accessProperty).get(SchemaConstants.TAG_TYPE);
        Assert.assertNotNull(accessPropertyType);
        Assert.assertEquals(SchemaConstants.TAG_TYPE_STRING, accessPropertyType.get(0).asText());
        Assert.assertEquals(SchemaConstants.TAG_TYPE_NULL, accessPropertyType.get(1).asText());
    }

    @Test
    public void testGenerateSchema_CustomStandardDefinition() throws Exception {
        CustomDefinitionProviderV2 customDefinitionProvider = new CustomDefinitionProviderV2() {
            @Override
            public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
                if (javaType.getErasedType() == Integer.class) {
                    // using SchemaGenerationContext.createStandardDefinition() to avoid endless loop with this custom definition
                    ObjectNode standardDefinition = context.createStandardDefinition(context.getTypeContext().resolve(Integer.class), this);
                    standardDefinition.put("$comment", "custom override of Integer");
                    return new CustomDefinition(standardDefinition);
                }
                return null;
            }
        };
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper())
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(Integer.class);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(SchemaConstants.TAG_TYPE_INTEGER, result.get(SchemaConstants.TAG_TYPE).asText());
        Assert.assertEquals("custom override of Integer", result.get("$comment").asText());
    }
}
