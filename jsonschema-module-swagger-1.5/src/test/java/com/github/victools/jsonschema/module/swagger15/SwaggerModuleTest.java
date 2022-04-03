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

package com.github.victools.jsonschema.module.swagger15;

import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.TypeScope;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Test for the {@link SwaggerModule} class.
 */
public class SwaggerModuleTest {

    private SchemaGeneratorConfigBuilder configBuilder;
    private SchemaGeneratorGeneralConfigPart typesInGeneralConfigPart;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    @BeforeEach
    public void setUp() {
        this.configBuilder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.typesInGeneralConfigPart = Mockito.spy(new SchemaGeneratorGeneralConfigPart());
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        this.methodConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(this.configBuilder.forTypesInGeneral()).thenReturn(this.typesInGeneralConfigPart);
        Mockito.when(this.configBuilder.forFields()).thenReturn(this.fieldConfigPart);
        Mockito.when(this.configBuilder.forMethods()).thenReturn(this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithDefaultOptions() {
        new SwaggerModule().applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.configBuilder, Mockito.times(2)).forTypesInGeneral();
        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(Mockito.any());
        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.typesInGeneralConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithAllOptions() {
        new SwaggerModule(SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES, SwaggerOption.IGNORING_HIDDEN_PROPERTIES,
                SwaggerOption.NO_APIMODEL_DESCRIPTION, SwaggerOption.NO_APIMODEL_TITLE)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.fieldConfigPart).withPropertyNameOverrideResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withIgnoreCheck(Mockito.any());

        Mockito.verify(this.methodConfigPart).withIgnoreCheck(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.typesInGeneralConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithPropertyNameOverrides() {
        new SwaggerModule(SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.configBuilder, Mockito.times(2)).forTypesInGeneral();
        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(Mockito.any());
        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(Mockito.any());

        Mockito.verify(this.fieldConfigPart).withPropertyNameOverrideResolver(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.typesInGeneralConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderIgnoringHiddenProperties() {
        new SwaggerModule(SwaggerOption.IGNORING_HIDDEN_PROPERTIES)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.configBuilder, Mockito.times(2)).forTypesInGeneral();
        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(Mockito.any());
        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(Mockito.any());

        Mockito.verify(this.fieldConfigPart).withIgnoreCheck(Mockito.any());

        Mockito.verify(this.methodConfigPart).withIgnoreCheck(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.typesInGeneralConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithoutApiModelTitle() {
        new SwaggerModule(SwaggerOption.NO_APIMODEL_TITLE)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.configBuilder).forTypesInGeneral();
        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.typesInGeneralConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithoutApiModelDescription() {
        new SwaggerModule(SwaggerOption.NO_APIMODEL_DESCRIPTION)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.configBuilder).forTypesInGeneral();
        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.typesInGeneralConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    private void verifyCommonConfigurations() {
        Mockito.verify(this.configBuilder).forFields();
        Mockito.verify(this.configBuilder).forMethods();

        Mockito.verify(this.fieldConfigPart).withDescriptionResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNumberInclusiveMinimumResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNumberExclusiveMinimumResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNumberInclusiveMaximumResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNumberExclusiveMaximumResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withEnumResolver(Mockito.any());

        Mockito.verify(this.methodConfigPart).withDescriptionResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNumberInclusiveMinimumResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNumberExclusiveMinimumResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNumberInclusiveMaximumResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNumberExclusiveMaximumResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withEnumResolver(Mockito.any());
    }

    static Stream<Arguments> parametersForTestIgnoreCheck() {
        return Stream.of(
            Arguments.of("unannotatedField", false),
            Arguments.of("annotatedAsNotHiddenField", false),
            Arguments.of("annotatedAsNotHiddenGetterField", false),
            Arguments.of("annotatedField", true),
            Arguments.of("annotatedGetterField", true)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestIgnoreCheck")
    public void testIgnoreCheck(String fieldName, boolean expectedResult) {
        new SwaggerModule(SwaggerOption.IGNORING_HIDDEN_PROPERTIES).applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForIgnoreCheck.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<Predicate<FieldScope>> captor = ArgumentCaptor.forClass(Predicate.class);
        Mockito.verify(this.fieldConfigPart).withIgnoreCheck(captor.capture());
        boolean result = captor.getValue().test(field);
        Assertions.assertEquals(expectedResult, result);
    }

    static Stream<Arguments> parametersForTestPropertyNameOverrideResolver() {
        return Stream.of(
            Arguments.of("unannotatedField", null),
            Arguments.of("annotatedWithoutValueField", null),
            Arguments.of("annotatedWithoutValueGetterField", null),
            Arguments.of("annotatedWithSameNameField", null),
            Arguments.of("annotatedWithSameNameGetterField", null),
            Arguments.of("annotatedField", "overrideOne"),
            Arguments.of("annotatedGetterField", "overrideTwo")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestPropertyNameOverrideResolver")
    public void testPropertyNameOverrideResolver(String fieldName, String expectedNameOverride) {
        new SwaggerModule(SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES).applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForPropertyNameOverride.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withPropertyNameOverrideResolver(captor.capture());
        String override = captor.getValue().apply(field);
        Assertions.assertEquals(expectedNameOverride, override);
    }

    static Stream<Arguments> parametersForTestTitleResolver() {
        return Stream.of(
            Arguments.of("unannotatedField", false, null),
            Arguments.of("exampleWithEmptyApiModel", false, null),
            Arguments.of("exampleWithApiModelTitle", false, "example title"),
            Arguments.of("listExampleWithApiModelTitle", false, null),
            Arguments.of("listExampleWithApiModelTitle", true, "example title")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestTitleResolver")
    public void testTitleResolver(String fieldName, boolean asContainerItem, String expectedTitle) {
        new SwaggerModule().applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForDescription.class);
        FieldScope field = testType.getMemberField(fieldName);
        if (asContainerItem) {
            field = field.asFakeContainerItemScope();
        }

        ArgumentCaptor<ConfigFunction<TypeScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(captor.capture());
        String title = captor.getValue().apply(field);
        Assertions.assertEquals(expectedTitle, title);
    }

    static Stream<Arguments> parametersForTestDescriptionResolver() {
        return Stream.of(
            Arguments.of("unannotatedField", false, null, null),
            Arguments.of("annotatedWithoutValueField", false, null, null),
            Arguments.of("annotatedWithoutValueGetterField", false, null, null),
            Arguments.of("annotatedField", false, "annotation value 1", null),
            Arguments.of("annotatedGetterField", false, "annotation value 2", null),
            Arguments.of("exampleWithEmptyApiModel", false, null, null),
            Arguments.of("exampleWithApiModelDescription", false, null, "type description"),
            Arguments.of("arrayExampleWithApiModelDescription", false, null, null),
            Arguments.of("arrayExampleWithApiModelDescription", true, null, "type description"),
            Arguments.of("exampleWithTwoDescriptions", false, "property description", "type description"),
            Arguments.of("exampleWithApiModelTitle", false, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestDescriptionResolver")
    public void testDescriptionResolver(String fieldName, boolean asContainerItem, String expectedMemberDescription, String expectedTypeDescription) {
        new SwaggerModule().applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForDescription.class);
        FieldScope field = testType.getMemberField(fieldName);
        if (asContainerItem) {
            field = field.asFakeContainerItemScope();
        }

        ArgumentCaptor<ConfigFunction<FieldScope, String>> memberCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withDescriptionResolver(memberCaptor.capture());
        String memberDescription = memberCaptor.getValue().apply(field);
        Assertions.assertEquals(expectedMemberDescription, memberDescription);

        ArgumentCaptor<ConfigFunction<TypeScope, String>> typeCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(typeCaptor.capture());
        TypeScope scope = Mockito.mock(TypeScope.class);
        Mockito.when(scope.getType()).thenReturn(field.getType());
        String typeDescription = typeCaptor.getValue().apply(scope);
        Assertions.assertEquals(expectedTypeDescription, typeDescription);
    }

    static Stream<Arguments> parametersForTestDescriptionResolverWithNoApiModelDescription() {
        return Stream.of(
            Arguments.of("unannotatedField", null),
            Arguments.of("annotatedWithoutValueField", null),
            Arguments.of("annotatedWithoutValueGetterField", null),
            Arguments.of("annotatedField", "annotation value 1"),
            Arguments.of("annotatedGetterField", "annotation value 2"),
            Arguments.of("exampleWithEmptyApiModel", null),
            Arguments.of("exampleWithApiModelDescription", null),
            Arguments.of("exampleWithTwoDescriptions", "property description"),
            Arguments.of("exampleWithApiModelTitle", null)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestDescriptionResolverWithNoApiModelDescription")
    public void testDescriptionResolverWithNoApiModelDescription(String fieldName, String expectedMemberDescription) {
        new SwaggerModule(SwaggerOption.NO_APIMODEL_DESCRIPTION).applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForDescription.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withDescriptionResolver(captor.capture());
        String description = captor.getValue().apply(field);
        Assertions.assertEquals(expectedMemberDescription, description);

        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.typesInGeneralConfigPart);
    }

    static Stream<Arguments> parametersForTestNumberMinMaxResolvers() {
        return Stream.of(
            Arguments.of("unannotatedInt", false, null, null, null, null),
            Arguments.of("unannotatedIntArray", false, null, null, null, null),
            Arguments.of("unannotatedIntArray", true, null, null, null, null),
            Arguments.of("minMinusHundredLong", false, "-100", null, null, null),
            Arguments.of("minMinusHundredOnGetterLong", false, "-100", null, null, null),
            Arguments.of("maxFiftyShort", false, null, null, "50", null),
            Arguments.of("maxFiftyOnGetterShort", false, null, null, "50", null),
            Arguments.of("tenToTwentyInclusiveDouble", false, "10.1", null, "20.2", null),
            Arguments.of("tenToTwentyInclusiveOnGetterDouble", false, "10.1", null, "20.2", null),
            Arguments.of("tenToTwentyExclusiveDecimal", false, null, "10.1", null, "20.2"),
            Arguments.of("tenToTwentyExclusiveOnGetterDecimal", false, null, "10.1", null, "20.2"),
            Arguments.of("positiveByte", false, null, BigDecimal.ZERO, null, null),
            Arguments.of("positiveOnGetterByte", false, null, BigDecimal.ZERO, null, null),
            Arguments.of("positiveOrZeroBigInteger", false, BigDecimal.ZERO, null, null, null),
            Arguments.of("positiveOrZeroOnGetterBigInteger", false, BigDecimal.ZERO, null, null, null),
            Arguments.of("negativeDecimal", false, null, null, null, BigDecimal.ZERO),
            Arguments.of("negativeOnGetterDecimal", false, null, null, null, BigDecimal.ZERO),
            Arguments.of("negativeOrZeroLong", false, null, null, BigDecimal.ZERO, null),
            Arguments.of("negativeOrZeroOnGetterLong", false, null, null, BigDecimal.ZERO, null)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestNumberMinMaxResolvers")
    public void testNumberMinMaxResolvers(String fieldName, boolean asContainerItem, BigDecimal expectedMinInclusive, BigDecimal expectedMinExclusive,
            BigDecimal expectedMaxInclusive, BigDecimal expectedMaxExclusive) throws Exception {
        new SwaggerModule().applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForNumberMinMax.class);
        FieldScope field = testType.getMemberField(fieldName);
        if (asContainerItem) {
            field = field.asFakeContainerItemScope();
        }

        ArgumentCaptor<ConfigFunction<FieldScope, BigDecimal>> minInclusiveCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNumberInclusiveMinimumResolver(minInclusiveCaptor.capture());
        BigDecimal minInclusive = minInclusiveCaptor.getValue().apply(field);
        Assertions.assertEquals(expectedMinInclusive, minInclusive);

        ArgumentCaptor<ConfigFunction<FieldScope, BigDecimal>> minExclusiveCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNumberExclusiveMinimumResolver(minExclusiveCaptor.capture());
        BigDecimal minExclusive = minExclusiveCaptor.getValue().apply(field);
        Assertions.assertEquals(expectedMinExclusive, minExclusive);

        ArgumentCaptor<ConfigFunction<FieldScope, BigDecimal>> maxInclusiveCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNumberInclusiveMaximumResolver(maxInclusiveCaptor.capture());
        BigDecimal maxInclusive = maxInclusiveCaptor.getValue().apply(field);
        Assertions.assertEquals(expectedMaxInclusive, maxInclusive);

        ArgumentCaptor<ConfigFunction<FieldScope, BigDecimal>> maxExclusiveCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNumberExclusiveMaximumResolver(maxExclusiveCaptor.capture());
        BigDecimal maxExclusive = maxExclusiveCaptor.getValue().apply(field);
        Assertions.assertEquals(expectedMaxExclusive, maxExclusive);
    }

    private static class TestClassForPropertyNameOverride {

        String unannotatedField;
        @ApiModelProperty
        int annotatedWithoutValueField;
        double annotatedWithoutValueGetterField;
        @ApiModelProperty(name = "annotatedWithSameNameField")
        Object annotatedWithSameNameField;
        boolean annotatedWithSameNameGetterField;
        @ApiModelProperty(name = "overrideOne")
        Object annotatedField;
        boolean annotatedGetterField;

        public String getUnannotatedField() {
            return this.unannotatedField;
        }

        public int getAnnotatedWithoutValueField() {
            return this.annotatedWithoutValueField;
        }

        @ApiModelProperty
        public double getAnnotatedWithoutValueGetterField() {
            return this.annotatedWithoutValueGetterField;
        }

        public Object getAnnotatedWithSameNameField() {
            return this.annotatedWithSameNameField;
        }

        @ApiModelProperty(name = "annotatedWithSameNameGetterField")
        public boolean isAnnotatedWithSameNameGetterField() {
            return this.annotatedWithSameNameGetterField;
        }

        public Object getAnnotatedField() {
            return this.annotatedField;
        }

        @ApiModelProperty(name = "overrideTwo")
        public boolean isAnnotatedGetterField() {
            return this.annotatedGetterField;
        }
    }

    private static class TestClassForDescription {

        String unannotatedField;
        @ApiModelProperty
        int annotatedWithoutValueField;
        double annotatedWithoutValueGetterField;
        @ApiModelProperty(value = "annotation value 1")
        List<Object> annotatedField;
        boolean annotatedGetterField;

        ExampleWithEmptyApiModel exampleWithEmptyApiModel;
        ExampleWithApiModelDescription exampleWithApiModelDescription;
        ExampleWithApiModelDescription[] arrayExampleWithApiModelDescription;
        @ApiModelProperty(value = "property description")
        ExampleWithApiModelDescription exampleWithTwoDescriptions;
        ExampleWithApiModelTitle exampleWithApiModelTitle;
        List<ExampleWithApiModelTitle> listExampleWithApiModelTitle;

        public String getUnannotatedField() {
            return this.unannotatedField;
        }

        public int getAnnotatedWithoutValueField() {
            return this.annotatedWithoutValueField;
        }

        @ApiModelProperty
        public double getAnnotatedWithoutValueGetterField() {
            return this.annotatedWithoutValueGetterField;
        }

        public List<Object> getAnnotatedField() {
            return this.annotatedField;
        }

        @ApiModelProperty(value = "annotation value 2")
        public boolean isAnnotatedGetterField() {
            return this.annotatedGetterField;
        }

        @ApiModel
        private class ExampleWithEmptyApiModel {
        }

        @ApiModel(value = "example title")
        private class ExampleWithApiModelTitle {
        }

        @ApiModel(description = "type description")
        private class ExampleWithApiModelDescription {
        }
    }

    private static class TestClassForIgnoreCheck {

        String unannotatedField;
        @ApiModelProperty(hidden = false)
        int annotatedAsNotHiddenField;
        double annotatedAsNotHiddenGetterField;
        @ApiModelProperty(hidden = true)
        Object annotatedField;
        boolean annotatedGetterField;

        public String getUnannotatedField() {
            return this.unannotatedField;
        }

        public int getAnnotatedWithoutValueField() {
            return this.annotatedAsNotHiddenField;
        }

        @ApiModelProperty
        public double getAnnotatedWithoutValueGetterField() {
            return this.annotatedAsNotHiddenGetterField;
        }

        public Object getAnnotatedField() {
            return this.annotatedField;
        }

        @ApiModelProperty(hidden = true)
        public boolean isAnnotatedGetterField() {
            return this.annotatedGetterField;
        }
    }

    private static class TestClassForNumberMinMax {

        int unannotatedInt;
        int[] unannotatedIntArray;
        @ApiModelProperty(allowableValues = "range[-100, infinity)")
        long minMinusHundredLong;
        long minMinusHundredOnGetterLong;
        @ApiModelProperty(allowableValues = "range(-infinity, 50]")
        short maxFiftyShort;
        short maxFiftyOnGetterShort;
        @ApiModelProperty(allowableValues = "range[10.1, 20.2]")
        Double tenToTwentyInclusiveDouble;
        Double tenToTwentyInclusiveOnGetterDouble;
        @ApiModelProperty(allowableValues = "range(10.1, 20.2)")
        BigDecimal tenToTwentyExclusiveDecimal;
        BigDecimal tenToTwentyExclusiveOnGetterDecimal;
        @ApiModelProperty(allowableValues = "range(0, infinity]")
        byte positiveByte;
        byte positiveOnGetterByte;
        @ApiModelProperty(allowableValues = "range[0, infinity]")
        BigInteger positiveOrZeroBigInteger;
        BigInteger positiveOrZeroOnGetterBigInteger;
        @ApiModelProperty(allowableValues = "range(-infinity, 0)")
        BigDecimal negativeDecimal;
        BigDecimal negativeOnGetterDecimal;
        @ApiModelProperty(allowableValues = "range[-infinity, 0]")
        Long negativeOrZeroLong;
        Long negativeOrZeroOnGetterLong;

        @ApiModelProperty(allowableValues = "range[-100, infinity)")
        public long getMinMinusHundredOnGetterLong() {
            return this.minMinusHundredOnGetterLong;
        }

        @ApiModelProperty(allowableValues = "range(-infinity, 50]")
        public short getMaxFiftyOnGetterShort() {
            return this.maxFiftyOnGetterShort;
        }

        @ApiModelProperty(allowableValues = "range[10.1, 20.2]")
        public Double getTenToTwentyInclusiveOnGetterDouble() {
            return this.tenToTwentyInclusiveOnGetterDouble;
        }

        @ApiModelProperty(allowableValues = "range(10.1, 20.2)")
        public BigDecimal getTenToTwentyExclusiveOnGetterDecimal() {
            return this.tenToTwentyExclusiveOnGetterDecimal;
        }

        @ApiModelProperty(allowableValues = "range(0, infinity]")
        public byte getPositiveOnGetterByte() {
            return this.positiveOnGetterByte;
        }

        @ApiModelProperty(allowableValues = "range[0, infinity]")
        public BigInteger getPositiveOrZeroOnGetterBigInteger() {
            return this.positiveOrZeroOnGetterBigInteger;
        }

        @ApiModelProperty(allowableValues = "range(-infinity, 0)")
        public BigDecimal getNegativeOnGetterDecimal() {
            return this.negativeOnGetterDecimal;
        }

        @ApiModelProperty(allowableValues = "range[-infinity, 0]")
        public Long getNegativeOrZeroOnGetterLong() {
            return this.negativeOrZeroOnGetterLong;
        }
    }
}
