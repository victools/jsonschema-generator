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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mockito;

/**
 * Test for the {@link CustomEnumDefinitionProvider}.
 */
public class CustomEnumDefinitionProviderTest {

    private final TypeContext typeContext = TypeContextFactory.createDefaultTypeContext();
    private SchemaGenerationContext generationContext;

    @BeforeEach
    public void setUp() {
        this.generationContext = Mockito.mock(SchemaGenerationContext.class, Answers.RETURNS_DEEP_STUBS);
        ObjectMapper objectMapper = new ObjectMapper();
        Mockito.when(this.generationContext.getTypeContext()).thenReturn(this.typeContext);
        Mockito.when(this.generationContext.getGeneratorConfig().getObjectMapper()).thenReturn(objectMapper);
        Mockito.when(this.generationContext.getGeneratorConfig().createObjectNode())
                .thenAnswer((_invocation) -> objectMapper.createObjectNode());
        Mockito.when(this.generationContext.getKeyword(Mockito.any()))
                .thenAnswer(invocation -> ((SchemaKeyword) invocation.getArgument(0)).forVersion(SchemaVersion.DRAFT_2019_09));
    }

    static Stream<Arguments> parametersForTestProvideCustomSchemaDefinition() {
        return Stream.of(
            Arguments.of(EnumWithInactiveJsonValueAndJsonProperty.class, true, true, Arrays.asList("entry")),
            Arguments.of(EnumWithInactiveJsonValueAndJsonProperty.class, false, true, Arrays.asList("entry")),
            Arguments.of(EnumWithJsonValueAndJsonProperty.class, true, false, Arrays.asList("json-value-ENTRY1", "json-value-ENTRY2")),
            Arguments.of(EnumWithJsonValueAndJsonProperty.class, true, true, Arrays.asList("json-value-ENTRY1", "json-value-ENTRY2")),
            Arguments.of(EnumWithJsonValueAndJsonProperty.class, false, true, Arrays.asList("entry1", "ENTRY2"))
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestProvideCustomSchemaDefinition")
    public void testProvideCustomSchemaDefinition_withConst(Class<?> erasedType, boolean considerJsonValue, boolean considerJsonProperty,
            List<String> values) {
        Mockito.when(this.generationContext.getGeneratorConfig().shouldRepresentSingleAllowedValueAsConst()).thenReturn(true);
        ResolvedType type = this.typeContext.resolve(erasedType);
        CustomDefinition result = new CustomEnumDefinitionProvider(considerJsonValue, considerJsonProperty)
                .provideCustomSchemaDefinition(type, this.generationContext);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isMeantToBeInline());
        ObjectNode customDefinitionNode = result.getValue();
        Assertions.assertEquals(2, customDefinitionNode.size());
        Assertions.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(SchemaVersion.DRAFT_2019_09),
                customDefinitionNode.get(SchemaKeyword.TAG_TYPE.forVersion(SchemaVersion.DRAFT_2019_09)).asText());
        int expectedValueCount = values.size();
        if (expectedValueCount == 1) {
            JsonNode constNode = customDefinitionNode.get(SchemaKeyword.TAG_CONST.forVersion(SchemaVersion.DRAFT_2019_09));
            Assertions.assertTrue(constNode.isTextual());
            Assertions.assertEquals(values.get(0), constNode.asText());
        } else {
            JsonNode enumNode = customDefinitionNode.get(SchemaKeyword.TAG_ENUM.forVersion(SchemaVersion.DRAFT_2019_09));
            Assertions.assertTrue(enumNode.isArray());
            ArrayNode arrayNode = (ArrayNode) enumNode;
            Assertions.assertEquals(expectedValueCount, arrayNode.size());
            for (int index = 0; index < expectedValueCount; index++) {
                Assertions.assertEquals(values.get(index), arrayNode.get(index).asText());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("parametersForTestProvideCustomSchemaDefinition")
    public void testProvideCustomSchemaDefinition_withoutConst(Class<?> erasedType, boolean considerJsonValue, boolean considerJsonProperty,
            List<String> values) {
        Mockito.when(this.generationContext.getGeneratorConfig().shouldRepresentSingleAllowedValueAsConst()).thenReturn(false);
        ResolvedType type = this.typeContext.resolve(erasedType);
        CustomDefinition result = new CustomEnumDefinitionProvider(considerJsonValue, considerJsonProperty)
                .provideCustomSchemaDefinition(type, this.generationContext);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isMeantToBeInline());
        ObjectNode customDefinitionNode = result.getValue();
        Assertions.assertEquals(2, customDefinitionNode.size());
        Assertions.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(SchemaVersion.DRAFT_2019_09),
                customDefinitionNode.get(SchemaKeyword.TAG_TYPE.forVersion(SchemaVersion.DRAFT_2019_09)).asText());
        JsonNode enumNode = customDefinitionNode.get(SchemaKeyword.TAG_ENUM.forVersion(SchemaVersion.DRAFT_2019_09));
        Assertions.assertTrue(enumNode.isArray());
        int expectedValueCount = values.size();
        ArrayNode arrayNode = (ArrayNode) enumNode;
        Assertions.assertEquals(expectedValueCount, arrayNode.size());
        for (int index = 0; index < expectedValueCount; index++) {
            Assertions.assertEquals(values.get(index), arrayNode.get(index).asText());
        }
    }

    static Stream<Arguments> parametersForTestProvideForInvalidTargetType() {
        return Stream.of(
            Arguments.of(Enum.class, true, false),
            Arguments.of(Enum.class, false, true),
            Arguments.of(Enum.class, true, true),
            Arguments.of(ClassWithJsonValue.class, true, false),
            Arguments.of(ClassWithJsonValue.class, false, true),
            Arguments.of(ClassWithJsonValue.class, true, true),
            Arguments.of(EmptyEnumWithJsonValue.class, true, false),
            Arguments.of(EmptyEnumWithJsonValue.class, false, true),
            Arguments.of(EmptyEnumWithJsonValue.class, true, true),
            Arguments.of(EnumWithInvalidJsonValue.class, true, false),
            Arguments.of(EnumWithInvalidJsonValue.class, false, true),
            Arguments.of(EnumWithInvalidJsonValue.class, true, true),
            Arguments.of(EnumWithInactiveJsonValueAndJsonProperty.class, true, false),
            Arguments.of(EnumWithTwoJsonValuesAndIncompleteJsonProperty.class, true, true),
            Arguments.of(EnumWithTwoJsonValuesAndIncompleteJsonProperty.class, false, true),
            Arguments.of(EnumWithTwoJsonValuesAndIncompleteJsonProperty.class, true, false)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestProvideForInvalidTargetType")
    public void testProvideForInvalidTargetType(Class<?> erasedType, boolean considerJsonValue, boolean considerJsonProperty) {
        CustomDefinition result = new CustomEnumDefinitionProvider(considerJsonValue, considerJsonProperty)
                .provideCustomSchemaDefinition(this.typeContext.resolve(erasedType), this.generationContext);
        Assertions.assertNull(result);
    }

    static Stream<Arguments> parametersForTestGetJsonValueAnnotatedMethod() {
        return Stream.of(
            Arguments.of(Enum.class, null),
            Arguments.of(ClassWithJsonValue.class, "serialise"),
            Arguments.of(EmptyEnumWithJsonValue.class, "asText"),
            Arguments.of(EnumWithInvalidJsonValue.class, null),
            Arguments.of(EnumWithInactiveJsonValueAndJsonProperty.class, null),
            Arguments.of(EnumWithTwoJsonValuesAndIncompleteJsonProperty.class, null),
            Arguments.of(EnumWithJsonValueAndJsonProperty.class, "getJsonValue")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestGetJsonValueAnnotatedMethod")
    public void testGetJsonValueAnnotatedMethod(Class<?> erasedType, String methodName) {
        ResolvedMethod result = new CustomEnumDefinitionProvider(true, false)
                .getJsonValueAnnotatedMethod(this.typeContext.resolve(erasedType), this.generationContext);
        if (methodName == null) {
            Assertions.assertNull(result);
        } else {
            Assertions.assertEquals(methodName, result.getRawMember().getName());
        }
    }

    private static final class ClassWithJsonValue {

        @JsonValue
        public String serialise() {
            return "ClassWithJsonValue";
        }
    }

    private static enum EmptyEnumWithJsonValue {
        ;

        @JsonValue
        public String asText() {
            return "ClassWithJsonValue";
        }
    }

    private static enum EnumWithInvalidJsonValue {
        ENTRY;

        @JsonValue
        public String serialize(String context) {
            return context + ":" + this.name();
        }
    }

    private static enum EnumWithInactiveJsonValueAndJsonProperty {
        @JsonProperty("entry") ENTRY;

        @JsonValue(false)
        public String serialize() {
            return this.name();
        }
    }

    private static enum EnumWithTwoJsonValuesAndIncompleteJsonProperty {
        ENTRY1,
        @JsonProperty("only-entry-2") ENTRY2;

        @JsonValue
        public String getValue1() {
            return "X";
        }

        @JsonValue
        public String getValue2() {
            return "Y";
        }
    }

    private static enum EnumWithJsonValueAndJsonProperty {
        @JsonProperty("entry1") ENTRY1,
        @JsonProperty ENTRY2;

        @JsonValue
        public String getJsonValue() {
            return "json-value-" + this.name();
        }
    }
}
