/*
 * Copyright 2022 VicTools.
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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for the ignored properties being exclude as expected.
 */
public class IgnorePropertyTest {

    static Stream<Arguments> parametersForTestJsonIgnoreProperties() {
        return Stream.of(
            Arguments.of(SubType.class, "[includedChildField, includedParentField1, includedParentField2]"),
            Arguments.of(SuperType.class, "[ignoredParentField2, includedParentField1]")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestJsonIgnoreProperties")
    public void testJsonIgnoreProperties(Class<?> targetType, String expectedIncludedPropertyNames) throws JsonProcessingException {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT)
                .with(new JacksonModule())
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);
        JsonNode result = generator.generateSchema(targetType);

        JsonNode propertiesNode = result.get(config.getKeyword(SchemaKeyword.TAG_PROPERTIES));
        Set<String> propertyNames = new TreeSet<>();
        propertiesNode.fieldNames().forEachRemaining(propertyNames::add);
        Assertions.assertEquals(expectedIncludedPropertyNames, propertyNames.toString());
    }

    @JsonIgnoreProperties({"ignoredChildField2","ignoredParentField2"})
    private static class SubType extends SuperType {
        public int includedChildField;
        @JsonIgnore
        public int ignoredChildField1;
        public int ignoredChildField2;
        @JsonBackReference
        public String ignoredChildField3;
    }

    @JsonIgnoreProperties({"includedParentField2"})
    private static class SuperType {
        public String includedParentField1;
        public String includedParentField2;
        @JsonIgnore
        public String ignoredParentField1;
        public String ignoredParentField2;
        @JsonBackReference
        public String ignoredParentField3;
    }
}
