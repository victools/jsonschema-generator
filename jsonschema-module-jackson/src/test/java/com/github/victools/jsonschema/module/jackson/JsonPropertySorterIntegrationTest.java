/*
 * Copyright 2023 VicTools.
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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class JsonPropertySorterIntegrationTest {

    @ParameterizedTest
    @CsvSource({
            "TestObject, one two three",
            "TestContainer, one two three"
    })
    public void testJsonPropertyOrderWithChildAnnotations(String targetTypeName, String expectedFieldOrder) throws Exception {
        JacksonModule module = new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_ORDER,
                JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS);
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.NONSTATIC_NONVOID_NONGETTER_METHODS, Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS)
                .with(module)
                .build();
        Class<?> targetType = Stream.of(JsonPropertySorterIntegrationTest.class.getDeclaredClasses())
                .filter(testType -> testType.getSimpleName().equals(targetTypeName))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);
    
        ObjectNode properties = (ObjectNode) result.get(config.getKeyword(SchemaKeyword.TAG_PROPERTIES));
        List<String> resultPropertyNames = new ArrayList<>();
        properties.fieldNames().forEachRemaining(resultPropertyNames::add);
        Assertions.assertEquals(Arrays.asList(expectedFieldOrder.split(" ")), resultPropertyNames);
    }

    @JsonPropertyOrder({"one", "two", "three"})
    public static class TestContainer {

        @JsonProperty("three")
        private Integer thirdInteger;
        @JsonProperty("one")
        private String firstString;

        @JsonProperty("two")
        public boolean getSecondValue() {
            return true;
        }
    }

    @JsonPropertyOrder({"one", "two", "three"})
    public static class TestObject {

        private TestContainer container;
        private String secondString;

        @JsonIgnore
        public TestContainer getContainer() {
            return this.container;
        }

        @JsonProperty("three")
        public Integer getInteger() {
            return this.container.thirdInteger;
        }

        @JsonProperty("two")
        public String getSecondString() {
            return this.secondString;
        }

        @JsonProperty("one")
        public String getString() {
            return this.container.firstString;
        }
    }

}
