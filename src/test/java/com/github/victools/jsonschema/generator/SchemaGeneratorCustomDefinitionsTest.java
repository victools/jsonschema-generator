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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for {@link SchemaGenerator} class.
 */
public class SchemaGeneratorCustomDefinitionsTest {

    @Test
    public void testGenerateSchema_CustomDefinition() throws Exception {
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> javaType.getErasedType() == Integer.class
                ? new CustomDefinition(context.createDefinition(context.getTypeContext().resolve(String.class)))
                : null;
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), SchemaVersion.DRAFT_7)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(Integer.class);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(SchemaVersion.getLatest().get(SchemaKeyword.TAG_TYPE_STRING),
                result.get(SchemaVersion.getLatest().get(SchemaKeyword.TAG_TYPE)).asText());
    }

    @Test
    public void testGenerateSchema_CustomCollectionDefinition() throws Exception {
        String accessProperty = "stream().findFirst().orElse(null)";
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> {
            if (!javaType.isInstanceOf(Collection.class)) {
                return null;
            }
            ResolvedType generic = context.getTypeContext().getContainerItemType(javaType);
            SchemaGeneratorConfig config = context.getGeneratorConfig();
            return new CustomDefinition(context.getGeneratorConfig().createObjectNode()
                    .put(config.getKeyword(SchemaKeyword.TAG_TYPE), config.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                    .set(config.getKeyword(SchemaKeyword.TAG_PROPERTIES), config.createObjectNode()
                            .set(accessProperty, context.makeNullable(context.createDefinition(generic)))));
        };
        final SchemaVersion schemaVersion = SchemaVersion.DRAFT_7;
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), schemaVersion)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(ArrayList.class, String.class);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(schemaVersion.get(SchemaKeyword.TAG_TYPE_OBJECT), result.get(schemaVersion.get(SchemaKeyword.TAG_TYPE)).asText());
        Assert.assertNotNull(result.get(schemaVersion.get(SchemaKeyword.TAG_PROPERTIES)));
        Assert.assertNotNull(result.get(schemaVersion.get(SchemaKeyword.TAG_PROPERTIES)).get(accessProperty));
        JsonNode accessPropertyType = result.get(schemaVersion.get(SchemaKeyword.TAG_PROPERTIES))
                .get(accessProperty).get(schemaVersion.get(SchemaKeyword.TAG_TYPE));
        Assert.assertNotNull(accessPropertyType);
        Assert.assertEquals(schemaVersion.get(SchemaKeyword.TAG_TYPE_STRING), accessPropertyType.get(0).asText());
        Assert.assertEquals(schemaVersion.get(SchemaKeyword.TAG_TYPE_NULL), accessPropertyType.get(1).asText());
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
        final SchemaVersion schemaVersion = SchemaVersion.DRAFT_7;
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), schemaVersion)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(Integer.class);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(schemaVersion.get(SchemaKeyword.TAG_TYPE_INTEGER), result.get(schemaVersion.get(SchemaKeyword.TAG_TYPE)).asText());
        Assert.assertEquals("custom override of Integer", result.get("$comment").asText());
    }

    @Test
    public void testGenerateSchema_CircularCustomStandardDefinition() throws Exception {
        String accessProperty = "get(0)";
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> {
            if (!javaType.isInstanceOf(List.class)) {
                return null;
            }
            ResolvedType generic = context.getTypeContext().getContainerItemType(javaType);
            SchemaGeneratorConfig config = context.getGeneratorConfig();
            return new CustomDefinition(context.getGeneratorConfig().createObjectNode()
                    .put(config.getKeyword(SchemaKeyword.TAG_TYPE), config.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                    .set(config.getKeyword(SchemaKeyword.TAG_PROPERTIES), context.getGeneratorConfig().createObjectNode()
                            .set(accessProperty, context.createDefinitionReference(generic))));
        };
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), SchemaVersion.DRAFT_7)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(TestCircularClass1.class);
        JSONAssert.assertEquals('\n' + result.toString() + '\n',
                loadResource("circular-custom-definition.json"), result.toString(), JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = SchemaGeneratorComplexTypesTest.class
                .getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        String fileAsString = stringBuilder.toString();
        return fileAsString;
    }

    private static class TestCircularClass1 {

        public List<TestCircularClass2> list2;

    }

    private static class TestCircularClass2 {

        public List<TestCircularClass1> list1;

    }
}
