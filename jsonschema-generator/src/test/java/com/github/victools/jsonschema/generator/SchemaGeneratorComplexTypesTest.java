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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for {@link SchemaGenerator} class.
 */
public class SchemaGeneratorComplexTypesTest {

    private static void populateTypeConfigPart(SchemaGeneratorTypeConfigPart<?> configPart, String descriptionPrefix) {
        configPart
                .withArrayMinItemsResolver(scope -> scope.isContainerType() ? 2 : null)
                .withArrayMaxItemsResolver(scope -> scope.isContainerType() ? 100 : null)
                .withArrayUniqueItemsResolver(scope -> scope.isContainerType() ? false : null)
                .withDefaultResolver(scope -> scope.getType().isInstanceOf(Number.class) ? 1 : null)
                .withDescriptionResolver(scope -> descriptionPrefix + scope.getSimpleTypeDescription())
                .withEnumResolver(scope -> scope.getType().isInstanceOf(Number.class) ? Arrays.asList(1, 2, 3, 4, 5) : null)
                .withEnumResolver(scope -> scope.getType().isInstanceOf(String.class) ? Arrays.asList("constant string value") : null)
                .withAdditionalPropertiesResolver(SchemaGeneratorComplexTypesTest::resolveAdditionalProperties)
                .withPatternPropertiesResolver(SchemaGeneratorComplexTypesTest::resolvePatternProperties)
                .withNumberExclusiveMaximumResolver(scope -> scope.getType().isInstanceOf(Number.class) ? BigDecimal.TEN.add(BigDecimal.ONE) : null)
                .withNumberExclusiveMinimumResolver(scope -> scope.getType().isInstanceOf(Number.class) ? BigDecimal.ZERO : null)
                .withNumberInclusiveMaximumResolver(scope -> scope.getType().isInstanceOf(Number.class) ? BigDecimal.TEN : null)
                .withNumberInclusiveMinimumResolver(scope -> scope.getType().isInstanceOf(Number.class) ? BigDecimal.ONE : null)
                .withNumberMultipleOfResolver(scope -> scope.getType().isInstanceOf(Number.class) ? BigDecimal.ONE : null)
                .withStringFormatResolver(scope -> scope.getType().isInstanceOf(String.class) ? "date" : null)
                .withStringMaxLengthResolver(scope -> scope.getType().isInstanceOf(String.class) ? 256 : null)
                .withStringMinLengthResolver(scope -> scope.getType().isInstanceOf(String.class) ? 1 : null)
                .withStringPatternResolver(scope -> scope.getType().isInstanceOf(String.class) ? "^.{1,256}$" : null)
                .withTitleResolver(scope -> scope.getSimpleTypeDescription());
    }

    private static Type resolveAdditionalProperties(TypeScope scope) {
        if (scope.getType().isInstanceOf(TestClass2.class)) {
            return Void.class;
        }
        if (scope.getType().isInstanceOf(TestClass4.class)) {
            return scope.getTypeParameterFor(TestClass4.class, 1);
        }
        return null;
    }

    private static Map<String, Type> resolvePatternProperties(TypeScope scope) {
        if (scope.getType().isInstanceOf(TestClass2.class)) {
            Map<String, Type> patternProperties = new HashMap<>();
            patternProperties.put("^generic.+$", scope.getTypeParameterFor(TestClass2.class, 0));
            return patternProperties;
        }
        if (scope.getType().isInstanceOf(TestClass4.class)) {
            return Collections.emptyMap();
        }
        return null;
    }

    private static void populateConfigPart(SchemaGeneratorConfigPart<? extends MemberScope<?, ?>> configPart, String descriptionPrefix) {
        populateTypeConfigPart(configPart, descriptionPrefix);
        configPart
                .withNullableCheck(member -> !member.getName().startsWith("nested"))
                .withRequiredCheck(member -> member.getName().startsWith("nested"))
                .withReadOnlyCheck(member -> member.isFinal())
                .withWriteOnlyCheck(member -> member.getType() != null && TestClass4.class.isAssignableFrom(member.getType().getErasedType()));
    }

    static Stream<Arguments> parametersForTestGenerateSchema() {
        Module neutralModule = configBuilder -> configBuilder.forTypesInGeneral().withCustomDefinitionProvider((javaType, _context) -> {
            if (Integer.class == javaType.getErasedType()) {
                ObjectNode customNode = configBuilder.getObjectMapper()
                        .createObjectNode()
                        .put("$comment", "custom definition for Integer.class");
                return new CustomDefinition(customNode, false);
            }
            return null;
        });
        Module alternativeDefinitionModule = configBuilder -> configBuilder.with(Option.DEFINITION_FOR_MAIN_SCHEMA,
                Option.PLAIN_DEFINITION_KEYS, Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS);
        Module typeInGeneralModule = configBuilder -> populateTypeConfigPart(
                configBuilder.with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT).forTypesInGeneral()
                        .withIdResolver(scope -> scope.getType().getTypeName().contains("$Test") ? "id-" + scope.getSimpleTypeDescription() : null)
                        .withAnchorResolver(scope -> scope.isContainerType() ? null : "#anchor")
                        .withPropertySorter((_prop1, _prop2) -> 0),
                "for type in general: ");
        Module methodModule = configBuilder -> populateConfigPart(configBuilder.with(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS)
                .forMethods(), "looked-up from method: ");
        Module fieldModule = configBuilder -> populateConfigPart(configBuilder.with(Option.INLINE_ALL_SCHEMAS).forFields(), "looked-up from field: ");
        Module enumToStringModule = configBuilder -> configBuilder.with(Option.FLATTENED_ENUMS_FROM_TOSTRING);
        return Stream.of(
            Arguments.of("testclass1-FULL_DOCUMENTATION", OptionPreset.FULL_DOCUMENTATION, TestClass1.class, neutralModule),
            Arguments.of("testclass1-FULL_DOCUMENTATION-typeattributes", OptionPreset.FULL_DOCUMENTATION, TestClass1.class, typeInGeneralModule),
            Arguments.of("testclass1-JAVA_OBJECT-methodattributes", OptionPreset.JAVA_OBJECT, TestClass1.class, methodModule),
            Arguments.of("testclass1-PLAIN_JSON-fieldattributes", OptionPreset.PLAIN_JSON, TestClass1.class, fieldModule),
            Arguments.of("testclass2-array", OptionPreset.FULL_DOCUMENTATION, TestClass2[].class, alternativeDefinitionModule),
            Arguments.of("testclass3-FULL_DOCUMENTATION", OptionPreset.FULL_DOCUMENTATION, TestClass3.class, alternativeDefinitionModule),
            Arguments.of("testclass3-FULL_DOCUMENTATION-typeattributes", OptionPreset.FULL_DOCUMENTATION, TestClass3.class, typeInGeneralModule),
            Arguments.of("testclass3-JAVA_OBJECT-methodattributes", OptionPreset.JAVA_OBJECT, TestClass3.class, methodModule),
            Arguments.of("testclass3-PLAIN_JSON-fieldattributes", OptionPreset.PLAIN_JSON, TestClass3.class, fieldModule),
            Arguments.of("testenum-PLAIN_JSON-default", OptionPreset.PLAIN_JSON, TestEnum.class, neutralModule),
            Arguments.of("testenum-FULL_DOCUMENTATION-default", OptionPreset.FULL_DOCUMENTATION, TestEnum.class, alternativeDefinitionModule),
            Arguments.of("testenum-PLAIN_JSON-viaToString", OptionPreset.PLAIN_JSON, TestEnum.class, enumToStringModule)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestGenerateSchema")
    public void testGenerateSchema(String caseTitle, OptionPreset preset, Class<?> targetType, Module testModule) throws Exception {
        final SchemaVersion schemaVersion = SchemaVersion.DRAFT_7;
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion, preset);
        configBuilder.with(testModule);
        configBuilder.with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        JsonNode result = generator.generateSchema(targetType);
        // ensure that the generated definition keys are valid URIs without any characters requiring encoding
        JsonNode definitions = result.get(SchemaKeyword.TAG_DEFINITIONS.forVersion(schemaVersion));
        if (definitions instanceof ObjectNode) {
            Iterator<String> definitionKeys = ((ObjectNode) definitions).fieldNames();
            while (definitionKeys.hasNext()) {
                String key = definitionKeys.next();
                Assertions.assertEquals(key, new URI(key).toASCIIString());
            }
        }
        JSONAssert.assertEquals('\n' + result.toString() + '\n',
                TestUtils.loadResource(this.getClass(), caseTitle + ".json"), result.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testGenerateInlineSchemaWithCircularReference() {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.INLINE_ALL_SCHEMAS)
                .build();

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SchemaGenerator(config).generateSchema(TestClassCircular.class));
    }

    private static class TestClass1 extends TestClass2<String> {

        public static final Long CONSTANT = 5L;

        private int primitiveValue;
        private Integer ignoredInternalValue;

        public int getPrimitiveValue() {
            return this.primitiveValue;
        }

        public <A extends B, B extends Number> void calculateSomething(A param0, B param1) {
            // nothing to do
        }

        public boolean isSimpleTestClass() {
            return true;
        }
    }

    private static class TestClass2<T> {

        private T genericValue;
        public T[] genericArray;

        public T getGenericValue() {
            return this.genericValue;
        }
    }

    private static class TestClass3 {

        private TestClass2<Long> nestedLong;
        private TestClass2<TestClass1[]> nestedClass1Array;
        private List<? extends TestClass2<Long>> nestedLongList;
        private TestClass4<Integer, String> class4;

        public TestClass2<Long> getNestedLong() {
            return this.nestedLong;
        }

        public TestClass2<TestClass1[]> getNestedClass1Array() {
            return this.nestedClass1Array;
        }

        public List<? extends TestClass2<Long>> getNestedLongList() {
            return this.nestedLongList;
        }

        public TestClass4<Integer, String> getClass4() {
            return this.class4;
        }
    }

    private static class TestClass4<S, T> {

        private TestClass2<TestClass2<T>> class2OfClass2OfT;
        public Optional<S> optionalS;
        public List<Optional<S>> listOfOptionalS;
        public Supplier<S> supplierS;
        public Set<LazyStringSupplier> setOfStringSupplier;
        public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

        public TestClass2<TestClass2<T>> getClass2OfClass2OfT() {
            return this.class2OfClass2OfT;
        }
    }

    private static class TestClassCircular {

        public TestClass2<TestClassCircular> class2OfCircular;
    }

    private static enum TestEnum {
        VALUE1, VALUE2, VALUE3;

        @Override
        public String toString() {
            return "toString_" + this.name();
        }
    }

    private static class LazyStringSupplier implements Supplier<String> {

        @Override
        public String get() {
            return "wait for it...";
        }
    }
}
