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
import java.util.function.Predicate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Test for the {@link SwaggerModule} class.
 */
@RunWith(JUnitParamsRunner.class)
public class SwaggerModuleTest {

    private SchemaGeneratorConfigBuilder configBuilder;
    private SchemaGeneratorGeneralConfigPart typesInGeneralConfigPart;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    @Before
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

    Object parametersForTestIgnoreCheck() {
        return new Object[][]{
            {"unannotatedField", false},
            {"annotatedAsNotHiddenField", false},
            {"annotatedAsNotHiddenGetterField", false},
            {"annotatedField", true},
            {"annotatedGetterField", true}
        };
    }

    @Test
    @Parameters
    public void testIgnoreCheck(String fieldName, boolean expectedResult) {
        new SwaggerModule(SwaggerOption.IGNORING_HIDDEN_PROPERTIES).applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForIgnoreCheck.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<Predicate<FieldScope>> captor = ArgumentCaptor.forClass(Predicate.class);
        Mockito.verify(this.fieldConfigPart).withIgnoreCheck(captor.capture());
        boolean result = captor.getValue().test(field);
        Assert.assertEquals(expectedResult, result);
    }

    Object parametersForTestPropertyNameOverrideResolver() {
        return new Object[][]{
            {"unannotatedField", null},
            {"annotatedWithoutValueField", null},
            {"annotatedWithoutValueGetterField", null},
            {"annotatedWithSameNameField", null},
            {"annotatedWithSameNameGetterField", null},
            {"annotatedField", "overrideOne"},
            {"annotatedGetterField", "overrideTwo"}
        };
    }

    @Test
    @Parameters
    public void testPropertyNameOverrideResolver(String fieldName, String expectedNameOverride) {
        new SwaggerModule(SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES).applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForPropertyNameOverride.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withPropertyNameOverrideResolver(captor.capture());
        String override = captor.getValue().apply(field);
        Assert.assertEquals(expectedNameOverride, override);
    }

    Object parametersForTestTitleResolver() {
        return new Object[][]{
            {"unannotatedField", null},
            {"exampleWithEmptyApiModel", null},
            {"exampleWithApiModelTitle", "example title"}
        };
    }

    @Test
    @Parameters
    public void testTitleResolver(String fieldName, String expectedTitle) {
        new SwaggerModule().applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForDescription.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<TypeScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(captor.capture());
        String title = captor.getValue().apply(field);
        Assert.assertEquals(expectedTitle, title);
    }

    Object parametersForTestDescriptionResolver() {
        return new Object[][]{
            {"unannotatedField", null, null},
            {"annotatedWithoutValueField", null, null},
            {"annotatedWithoutValueGetterField", null, null},
            {"annotatedField", "annotation value 1", null},
            {"annotatedGetterField", "annotation value 2", null},
            {"exampleWithEmptyApiModel", null, null},
            {"exampleWithApiModelDescription", null, "type description"},
            {"exampleWithTwoDescriptions", "property description", "type description"},
            {"exampleWithApiModelTitle", null, null}
        };
    }

    @Test
    @Parameters
    public void testDescriptionResolver(String fieldName, String expectedMemberDescription, String expectedTypeDescription) {
        new SwaggerModule().applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForDescription.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> memberCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withDescriptionResolver(memberCaptor.capture());
        String memberDescription = memberCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMemberDescription, memberDescription);

        ArgumentCaptor<ConfigFunction<TypeScope, String>> typeCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(typeCaptor.capture());
        TypeScope scope = Mockito.mock(TypeScope.class);
        Mockito.when(scope.getType()).thenReturn(field.getType());
        String typeDescription = typeCaptor.getValue().apply(scope);
        Assert.assertEquals(expectedTypeDescription, typeDescription);
    }

    Object parametersForTestDescriptionResolverWithNoApiModelDescription() {
        return new Object[][]{
            {"unannotatedField", null},
            {"annotatedWithoutValueField", null},
            {"annotatedWithoutValueGetterField", null},
            {"annotatedField", "annotation value 1"},
            {"annotatedGetterField", "annotation value 2"},
            {"exampleWithEmptyApiModel", null},
            {"exampleWithApiModelDescription", null},
            {"exampleWithTwoDescriptions", "property description"},
            {"exampleWithApiModelTitle", null}
        };
    }

    @Test
    @Parameters
    public void testDescriptionResolverWithNoApiModelDescription(String fieldName, String expectedMemberDescription) {
        new SwaggerModule(SwaggerOption.NO_APIMODEL_DESCRIPTION).applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForDescription.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withDescriptionResolver(captor.capture());
        String description = captor.getValue().apply(field);
        Assert.assertEquals(expectedMemberDescription, description);

        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.typesInGeneralConfigPart);
    }

    Object parametersForTestNumberMinMaxResolvers() {
        return new Object[][]{
            {"unannotatedInt", null, null, null, null},
            {"minMinusHundredLong", "-100", null, null, null},
            {"minMinusHundredOnGetterLong", "-100", null, null, null},
            {"maxFiftyShort", null, null, "50", null},
            {"maxFiftyOnGetterShort", null, null, "50", null},
            {"tenToTwentyInclusiveDouble", "10.1", null, "20.2", null},
            {"tenToTwentyInclusiveOnGetterDouble", "10.1", null, "20.2", null},
            {"tenToTwentyExclusiveDecimal", null, "10.1", null, "20.2"},
            {"tenToTwentyExclusiveOnGetterDecimal", null, "10.1", null, "20.2"},
            {"positiveByte", null, BigDecimal.ZERO, null, null},
            {"positiveOnGetterByte", null, BigDecimal.ZERO, null, null},
            {"positiveOrZeroBigInteger", BigDecimal.ZERO, null, null, null},
            {"positiveOrZeroOnGetterBigInteger", BigDecimal.ZERO, null, null, null},
            {"negativeDecimal", null, null, null, BigDecimal.ZERO},
            {"negativeOnGetterDecimal", null, null, null, BigDecimal.ZERO},
            {"negativeOrZeroLong", null, null, BigDecimal.ZERO, null},
            {"negativeOrZeroOnGetterLong", null, null, BigDecimal.ZERO, null}
        };
    }

    @Test
    @Parameters
    public void testNumberMinMaxResolvers(String fieldName, BigDecimal expectedMinInclusive, BigDecimal expectedMinExclusive,
            BigDecimal expectedMaxInclusive, BigDecimal expectedMaxExclusive) throws Exception {
        new SwaggerModule().applyToConfigBuilder(this.configBuilder);

        TestType testType = new TestType(TestClassForNumberMinMax.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, BigDecimal>> minInclusiveCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNumberInclusiveMinimumResolver(minInclusiveCaptor.capture());
        BigDecimal minInclusive = minInclusiveCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMinInclusive, minInclusive);

        ArgumentCaptor<ConfigFunction<FieldScope, BigDecimal>> minExclusiveCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNumberExclusiveMinimumResolver(minExclusiveCaptor.capture());
        BigDecimal minExclusive = minExclusiveCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMinExclusive, minExclusive);

        ArgumentCaptor<ConfigFunction<FieldScope, BigDecimal>> maxInclusiveCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNumberInclusiveMaximumResolver(maxInclusiveCaptor.capture());
        BigDecimal maxInclusive = maxInclusiveCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMaxInclusive, maxInclusive);

        ArgumentCaptor<ConfigFunction<FieldScope, BigDecimal>> maxExclusiveCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNumberExclusiveMaximumResolver(maxExclusiveCaptor.capture());
        BigDecimal maxExclusive = maxExclusiveCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMaxExclusive, maxExclusive);
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
        Object annotatedField;
        boolean annotatedGetterField;

        ExampleWithEmptyApiModel exampleWithEmptyApiModel;
        ExampleWithApiModelDescription exampleWithApiModelDescription;
        @ApiModelProperty(value = "property description")
        ExampleWithApiModelDescription exampleWithTwoDescriptions;
        ExampleWithApiModelTitle exampleWithApiModelTitle;

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

        public Object getAnnotatedField() {
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
