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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for {@link SchemaGenerator} class.
 */
@RunWith(JUnitParamsRunner.class)
public class SchemaGeneratorCustomDefinitionsTest {

    @Test
    @Parameters(source = SchemaVersion.class)
    public void testGenerateSchema_CustomDefinition(SchemaVersion schemaVersion) throws Exception {
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> javaType.getErasedType() == Integer.class
                ? new CustomDefinition(context.createDefinition(context.getTypeContext().resolve(String.class)))
                : null;
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), schemaVersion)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(Integer.class);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(schemaVersion),
                result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
    }

    @Test
    @Parameters(source = SchemaVersion.class)
    public void testGenerateSchema_CustomCollectionDefinition(SchemaVersion schemaVersion) throws Exception {
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
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), schemaVersion)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(ArrayList.class, String.class);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(SchemaKeyword.TAG_TYPE_OBJECT.forVersion(schemaVersion),
                result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        Assert.assertNotNull(result.get(SchemaKeyword.TAG_PROPERTIES.forVersion(schemaVersion)));
        Assert.assertNotNull(result.get(SchemaKeyword.TAG_PROPERTIES.forVersion(schemaVersion)).get(accessProperty));
        JsonNode accessPropertyType = result.get(SchemaKeyword.TAG_PROPERTIES.forVersion(schemaVersion))
                .get(accessProperty).get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion));
        Assert.assertNotNull(accessPropertyType);
        Assert.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(schemaVersion), accessPropertyType.get(0).asText());
        Assert.assertEquals(SchemaKeyword.TAG_TYPE_NULL.forVersion(schemaVersion), accessPropertyType.get(1).asText());
    }

    @Test
    @Parameters(source = SchemaVersion.class)
    public void testGenerateSchema_CustomStandardDefinition(SchemaVersion schemaVersion) throws Exception {
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
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), schemaVersion)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(Integer.class);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(SchemaKeyword.TAG_TYPE_INTEGER.forVersion(schemaVersion),
                result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        Assert.assertEquals("custom override of Integer", result.get("$comment").asText());
    }

    @Test
    @Parameters(source = SchemaVersion.class)
    public void testGenerateSchema_CircularCustomStandardDefinition(SchemaVersion schemaVersion) throws Exception {
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
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(new ObjectMapper(), schemaVersion)
                .with(customDefinitionProvider)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(TestCircularClass1.class);
        JSONAssert.assertEquals('\n' + result.toString() + '\n',
                loadResource("circular-custom-definition-" + schemaVersion.name() + ".json"), result.toString(), JSONCompareMode.STRICT);
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
