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

package com.github.victools.jsonschema.generator;

import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test for the {@link SchemaBuilder}.
 */
public class SchemaBuilderTest {

    private SchemaGeneratorConfig config;
    private TypeContext typeContext;

    @BeforeEach
    public void setUp() {
        this.config = Mockito.spy(new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.PLAIN_DEFINITION_KEYS)
                .build());
        this.typeContext = TypeContextFactory.createDefaultTypeContext(this.config);
    }

    @Test
    public void testMultiTypeSchemaGeneration() throws Exception {
        SchemaBuilder instance = SchemaBuilder.forMultipleTypes(this.config, this.typeContext);

        ObjectNode result = this.config.createObjectNode();
        result.put("openapi", "3.0.0");
        result.putObject("info")
                .put("title", "Test API")
                .put("version", "0.1.0");
        ObjectNode testPath = result.putObject("paths")
                .putObject("/test");
        ObjectNode testPathPost = testPath.putObject("post");
        testPathPost.putObject("requestBody")
                .putObject("content").putObject("application/json")
                .set("schema", instance.createSchemaReference(TestClass1.class));
        testPathPost.putObject("responses").putObject("200")
                .put("description", "successful POST")
                .putObject("content").putObject("application/json")
                .set("schema", instance.createSchemaReference(TestClass2.class));
        ObjectNode testPathPut = testPath.putObject("put");
        testPathPut.putObject("requestBody")
                .putObject("content").putObject("application/json")
                .set("schema", instance.createSchemaReference(TestClass3.class));
        testPathPut.putObject("responses").putObject("201")
                .put("description", "successful PUT")
                .putObject("content").putObject("application/json")
                .set("schema", instance.createSchemaReference(TestClass3.class));

        result.putObject("components")
                .set("schemas", instance.collectDefinitions("components/schemas"));

        TestUtils.assertGeneratedSchema(result, this.getClass(), "openapi.json");
        Mockito.verify(this.config, Mockito.times(4)).resetAfterSchemaGenerationFinished();
    }

    private static class TestClass1 {

        public TestClass2 value2;
    }

    private static class TestClass2 {

        public String text;
    }

    private static class TestClass3 {

        public double number;
    }
}
