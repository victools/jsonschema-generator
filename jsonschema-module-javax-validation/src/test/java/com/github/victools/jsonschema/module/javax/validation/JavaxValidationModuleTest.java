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

package com.github.victools.jsonschema.module.javax.validation;

import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Test for the {@link JavaxValidationModule}.
 */
@RunWith(JUnitParamsRunner.class)
public class JavaxValidationModuleTest {

    private SchemaGeneratorConfigBuilder configBuilder;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    @Before
    public void setUp() {
        this.configBuilder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        this.methodConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(this.configBuilder.forFields()).thenReturn(this.fieldConfigPart);
        Mockito.when(this.configBuilder.forMethods()).thenReturn(this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithDefaultOptions() {
        new JavaxValidationModule().applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithRequiredFieldsOption() {
        new JavaxValidationModule(JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.fieldConfigPart).withRequiredCheck(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithRequiredMethodsOption() {
        new JavaxValidationModule(JavaxValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.methodConfigPart).withRequiredCheck(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithPatternOption() {
        new JavaxValidationModule(JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.fieldConfigPart).withStringPatternResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withStringPatternResolver(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithAllOptions() {
        new JavaxValidationModule(JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED, JavaxValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED,
                JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations();

        Mockito.verify(this.fieldConfigPart).withRequiredCheck(Mockito.any());
        Mockito.verify(this.methodConfigPart).withRequiredCheck(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withStringPatternResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withStringPatternResolver(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart);
    }

    private void verifyCommonConfigurations() {
        Mockito.verify(this.configBuilder).forFields();
        Mockito.verify(this.configBuilder).forMethods();

        Mockito.verify(this.fieldConfigPart).withNullableCheck(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withArrayMinItemsResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withArrayMaxItemsResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withStringMinLengthResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withStringMaxLengthResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withStringFormatResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNumberInclusiveMinimumResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNumberExclusiveMinimumResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNumberInclusiveMaximumResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNumberExclusiveMaximumResolver(Mockito.any());

        Mockito.verify(this.methodConfigPart).withNullableCheck(Mockito.any());
        Mockito.verify(this.methodConfigPart).withArrayMinItemsResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withArrayMaxItemsResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withStringMinLengthResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withStringMaxLengthResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withStringFormatResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNumberInclusiveMinimumResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNumberExclusiveMinimumResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNumberInclusiveMaximumResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withNumberExclusiveMaximumResolver(Mockito.any());
    }

    Object parametersForTestNullableCheck() {
        return new Object[][]{
            {"unannotatedField", null},
            {"notNullNumber", Boolean.FALSE},
            {"notNullOnGetterNumber", Boolean.FALSE},
            {"notEmptyList", Boolean.FALSE},
            {"notEmptyOnGetterList", Boolean.FALSE},
            {"notBlankString", Boolean.FALSE},
            {"notBlankOnGetterString", Boolean.FALSE},
            {"nullField", Boolean.TRUE},
            {"nullGetter", Boolean.TRUE}
        };
    }

    @Test
    @Parameters(method = "parametersForTestNullableCheck")
    public void testNullableCheckOnFieldNoValidationGroup(String fieldName, Boolean expectedResult) throws Exception {
        new JavaxValidationModule().applyToConfigBuilder(this.configBuilder);

        this.testNullableCheckOnField(fieldName, expectedResult);
    }

    @Test
    @Parameters(method = "parametersForTestNullableCheck")
    public void testNullableCheckOnFieldMatchingValidationGroup(String fieldName, Boolean expectedResult) throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Test.class)
                .applyToConfigBuilder(this.configBuilder);

        this.testNullableCheckOnField(fieldName, expectedResult);
    }

    @Test
    @Parameters(method = "parametersForTestNullableCheck")
    @TestCaseName("{method}({0}) [{index}]")
    public void testNullableCheckOnFieldDifferentValidationGroup(String fieldName, Boolean ignoredResult) throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Object.class)
                .applyToConfigBuilder(this.configBuilder);

        // none of the annotated values are actually expected to be returned
        this.testNullableCheckOnField(fieldName, null);
    }

    private void testNullableCheckOnField(String fieldName, Boolean expectedResult) throws Exception {
        ArgumentCaptor<ConfigFunction<FieldScope, Boolean>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNullableCheck(captor.capture());
        TestType testType = new TestType(TestClassForNullableCheck.class);
        FieldScope field = testType.getMemberField(fieldName);

        Boolean result = captor.getValue().apply(field);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    @Parameters(method = "parametersForTestNullableCheck")
    public void testNullableCheckOnMethodNoValidationGroup(String fieldName, Boolean expectedResult) throws Exception {
        new JavaxValidationModule().applyToConfigBuilder(this.configBuilder);

        this.testNullableCheckOnMethod(fieldName, expectedResult);
    }

    @Test
    @Parameters(method = "parametersForTestNullableCheck")
    public void testNullableCheckOnMethodMatchingValidationGroup(String fieldName, Boolean expectedResult) throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Test.class)
                .applyToConfigBuilder(this.configBuilder);

        this.testNullableCheckOnMethod(fieldName, expectedResult);
    }

    @Test
    @Parameters(method = "parametersForTestNullableCheck")
    @TestCaseName("{method}({0}) [{index}]")
    public void testNullableCheckOnMethodDifferentValidationGroup(String fieldName, Boolean ignoredResult) throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Object.class)
                .applyToConfigBuilder(this.configBuilder);

        // none of the annotated values are actually expected to be returned
        this.testNullableCheckOnMethod(fieldName, null);
    }

    private void testNullableCheckOnMethod(String fieldName, Boolean expectedResult) throws Exception {
        ArgumentCaptor<ConfigFunction<MethodScope, Boolean>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.methodConfigPart).withNullableCheck(captor.capture());
        TestType testType = new TestType(TestClassForNullableCheck.class);
        String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        MethodScope method = testType.getMemberMethod(methodName);

        Boolean result = captor.getValue().apply(method);
        Assert.assertEquals(expectedResult, result);
    }

    Object parametersForTestArrayItemCountResolvers() {
        return new Object[][]{
            {"unannotatedArray", null, null},
            {"sizeTenToTwentyString", null, null},
            {"sizeTenToTwentyOnGetterString", null, null},
            {"minSizeFiveArray", 5, null},
            {"minSizeFiveOnGetterArray", 5, null},
            {"maxSizeFiftyArray", null, 50},
            {"maxSizeFiftyOnGetterArray", null, 50},
            {"sizeTenToTwentySet", 10, 20},
            {"sizeTenToTwentyOnGetterSet", 10, 20},
            {"nonEmptyMaxSizeHundredList", 1, 100},
            {"nonEmptyMaxSizeHundredOnGetterList", 1, 100}
        };
    }

    @Test
    @Parameters(method = "parametersForTestArrayItemCountResolvers")
    public void testArrayItemCountResolversNoValidationGroup(String fieldName, Integer expectedMinItems, Integer expectedMaxItems) throws Exception {
        new JavaxValidationModule().applyToConfigBuilder(this.configBuilder);

        this.testArrayItemCountResolvers(fieldName, expectedMinItems, expectedMaxItems);
    }

    @Test
    @Parameters(method = "parametersForTestArrayItemCountResolvers")
    public void testArrayItemCountResolversMatchingValidationGroup(String fieldName, Integer expectedMinItems, Integer expectedMaxItems)
            throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Test.class)
                .applyToConfigBuilder(this.configBuilder);

        this.testArrayItemCountResolvers(fieldName, expectedMinItems, expectedMaxItems);
    }

    @Test
    @Parameters(method = "parametersForTestArrayItemCountResolvers")
    @TestCaseName("{method}({0}) [{index}]")
    public void testArrayItemCountResolversDifferentValidationGroup(String fieldName, Integer ignoredMinItems, Integer ignoredMaxItems)
            throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Object.class)
                .applyToConfigBuilder(this.configBuilder);

        // none of the annotated values are actually expected to be returned
        this.testArrayItemCountResolvers(fieldName, null, null);
    }

    private void testArrayItemCountResolvers(String fieldName, Integer expectedMinItems, Integer expectedMaxItems) throws Exception {
        TestType testType = new TestType(TestClassForArrayItemCount.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, Integer>> minItemCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withArrayMinItemsResolver(minItemCaptor.capture());
        Integer minItemCount = minItemCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMinItems, minItemCount);

        ArgumentCaptor<ConfigFunction<FieldScope, Integer>> maxItemCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withArrayMaxItemsResolver(maxItemCaptor.capture());
        Integer maxItemCount = maxItemCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMaxItems, maxItemCount);
    }

    Object parametersForTestStringLengthResolvers() {
        return new Object[][]{
            {"unannotatedString", null, null},
            {"sizeTenToTwentyArray", null, null},
            {"sizeTenToTwentyOnGetterArray", null, null},
            {"minSizeFiveSequence", 5, null},
            {"minSizeFiveOnGetterSequence", 5, null},
            {"maxSizeFiftyString", null, 50},
            {"maxSizeFiftyOnGetterString", null, 50},
            {"sizeTenToTwentyString", 10, 20},
            {"sizeTenToTwentyOnGetterString", 10, 20},
            {"nonEmptyMaxSizeHundredString", 1, 100},
            {"nonEmptyMaxSizeHundredOnGetterString", 1, 100},
            {"nonBlankString", 1, null},
            {"nonBlankOnGetterString", 1, null}
        };
    }

    @Test
    @Parameters(method = "parametersForTestStringLengthResolvers")
    public void testStringLengthResolversNoValidationGroup(String fieldName, Integer expectedMinLength, Integer expectedMaxLength) throws Exception {
        new JavaxValidationModule().applyToConfigBuilder(this.configBuilder);

        this.testStringLengthResolvers(fieldName, expectedMinLength, expectedMaxLength);
    }

    @Test
    @Parameters(method = "parametersForTestStringLengthResolvers")
    public void testStringLengthResolversMatchingValidationGroup(String fieldName, Integer expectedMinLength, Integer expectedMaxLength)
            throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Test.class)
                .applyToConfigBuilder(this.configBuilder);

        this.testStringLengthResolvers(fieldName, expectedMinLength, expectedMaxLength);
    }

    @Test
    @Parameters(method = "parametersForTestStringLengthResolvers")
    @TestCaseName("{method}({0}) [{index}]")
    public void testStringLengthResolversDifferentValidationGroup(String fieldName, Integer ignoredMinLength, Integer ignoredMaxLength)
            throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Object.class)
                .applyToConfigBuilder(this.configBuilder);

        // none of the annotated values are actually expected to be returned
        this.testStringLengthResolvers(fieldName, null, null);
    }

    private void testStringLengthResolvers(String fieldName, Integer expectedMinLength, Integer expectedMaxLength) throws Exception {
        TestType testType = new TestType(TestClassForStringProperties.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, Integer>> minLengthCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withStringMinLengthResolver(minLengthCaptor.capture());
        Integer minLength = minLengthCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMinLength, minLength);

        ArgumentCaptor<ConfigFunction<FieldScope, Integer>> maxLengthCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withStringMaxLengthResolver(maxLengthCaptor.capture());
        Integer maxLength = maxLengthCaptor.getValue().apply(field);
        Assert.assertEquals(expectedMaxLength, maxLength);
    }

    Object parametersForTestStringFormatAndPatternResolvers() {
        JavaxValidationOption[] onlyPatternOption = new JavaxValidationOption[]{
            JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS
        };
        JavaxValidationOption[] patternAndIdnEmailOptions = new JavaxValidationOption[]{
            JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS, JavaxValidationOption.PREFER_IDN_EMAIL_FORMAT
        };
        return new Object[][]{
            {"unannotatedString", onlyPatternOption, null, null},
            {"sizeTenToTwentyArray", onlyPatternOption, null, null},
            {"sizeTenToTwentyOnGetterArray", onlyPatternOption, null, null},
            {"minSizeFiveSequence", onlyPatternOption, null, "^\\d+$"},
            {"minSizeFiveOnGetterSequence", onlyPatternOption, null, "^\\d+$"},
            {"nonEmptyMaxSizeHundredString", onlyPatternOption, "email", null},
            {"nonEmptyMaxSizeHundredString", patternAndIdnEmailOptions, "idn-email", null},
            {"nonEmptyMaxSizeHundredOnGetterString", onlyPatternOption, "email", null},
            {"nonEmptyMaxSizeHundredOnGetterString", patternAndIdnEmailOptions, "idn-email", null},
            {"nonBlankString", onlyPatternOption, "email", "^.+your-company\\.com$"},
            {"nonBlankOnGetterString", onlyPatternOption, "email", "^.+your-company\\.com$"}
        };
    }

    @Test
    @Parameters(method = "parametersForTestStringFormatAndPatternResolvers")
    public void testStringFormatAndPatternResolversNoValidationGroup(String fieldName, JavaxValidationOption[] options,
            String expectedFormat, String expectedPattern) throws Exception {
        new JavaxValidationModule(options).applyToConfigBuilder(this.configBuilder);

        this.testStringFormatAndPatternResolvers(fieldName, expectedFormat, expectedPattern);
    }

    @Test
    @Parameters(method = "parametersForTestStringFormatAndPatternResolvers")
    public void testStringFormatAndPatternResolversMatchingValidationGroup(String fieldName, JavaxValidationOption[] options,
            String expectedFormat, String expectedPattern) throws Exception {
        new JavaxValidationModule(options)
                .forValidationGroups(Test.class)
                .applyToConfigBuilder(this.configBuilder);

        this.testStringFormatAndPatternResolvers(fieldName, expectedFormat, expectedPattern);
    }

    @Test
    @Parameters(method = "parametersForTestStringFormatAndPatternResolvers")
    @TestCaseName("{method}({0}, {1}) [{index}]")
    public void testStringFormatAndPatternResolversDifferentValidationGroup(String fieldName, JavaxValidationOption[] options,
            String ignoredFormat, String ignoredPattern) throws Exception {
        new JavaxValidationModule(options)
                .forValidationGroups(Object.class)
                .applyToConfigBuilder(this.configBuilder);

        // none of the annotated values are actually expected to be returned
        this.testStringFormatAndPatternResolvers(fieldName, null, null);
    }

    private void testStringFormatAndPatternResolvers(String fieldName, String expectedFormat, String expectedPattern)
            throws Exception {
        TestType testType = new TestType(TestClassForStringProperties.class);
        FieldScope field = testType.getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> formatCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withStringFormatResolver(formatCaptor.capture());
        String formatValue = formatCaptor.getValue().apply(field);
        Assert.assertEquals(expectedFormat, formatValue);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> patternCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withStringPatternResolver(patternCaptor.capture());
        String patternValue = patternCaptor.getValue().apply(field);
        Assert.assertEquals(expectedPattern, patternValue);
    }

    Object parametersForTestNumberMinMaxResolvers() {
        return new Object[][]{
            {"unannotatedInt", null, null, null, null},
            {"minMinusHundredLong", "-100", null, null, null},
            {"minMinusHundredOnGetterLong", "-100", null, null, null},
            {"maxFiftyShort", null, null, "50", null},
            {"maxFiftyOnGetterShort", null, null, "50", null},
            {"tenToTwentyInclusiveInteger", "10.1", null, "20.2", null},
            {"tenToTwentyInclusiveOnGetterInteger", "10.1", null, "20.2", null},
            {"tenToTwentyExclusiveInteger", null, "10.1", null, "20.2"},
            {"tenToTwentyExclusiveOnGetterInteger", null, "10.1", null, "20.2"},
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
    @Parameters(method = "parametersForTestNumberMinMaxResolvers")
    public void testNumberMinMaxResolversNoValidationGroup(String fieldName, BigDecimal expectedMinInclusive, BigDecimal expectedMinExclusive,
            BigDecimal expectedMaxInclusive, BigDecimal expectedMaxExclusive) throws Exception {
        new JavaxValidationModule().applyToConfigBuilder(this.configBuilder);

        this.testNumberMinMaxResolvers(fieldName, expectedMinInclusive, expectedMinExclusive, expectedMaxInclusive, expectedMaxExclusive);
    }

    @Test
    @Parameters(method = "parametersForTestNumberMinMaxResolvers")
    public void testNumberMinMaxResolversMatchingValidationGroup(String fieldName, BigDecimal expectedMinInclusive, BigDecimal expectedMinExclusive,
            BigDecimal expectedMaxInclusive, BigDecimal expectedMaxExclusive) throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Test.class)
                .applyToConfigBuilder(this.configBuilder);

        this.testNumberMinMaxResolvers(fieldName, expectedMinInclusive, expectedMinExclusive, expectedMaxInclusive, expectedMaxExclusive);
    }

    @Test
    @Parameters(method = "parametersForTestNumberMinMaxResolvers")
    @TestCaseName("{method}({0}) [{index}]")
    public void testNumberMinMaxResolversDifferentValidationGroup(String fieldName, BigDecimal ignoredMinInclusive, BigDecimal ignoredMinExclusive,
            BigDecimal ignoredMaxInclusive, BigDecimal ignoredMaxExclusive) throws Exception {
        new JavaxValidationModule()
                .forValidationGroups(Object.class)
                .applyToConfigBuilder(this.configBuilder);

        // none of the annotated values are actually expected to be returned
        this.testNumberMinMaxResolvers(fieldName, null, null, null, null);
    }

    private void testNumberMinMaxResolvers(String fieldName, BigDecimal expectedMinInclusive, BigDecimal expectedMinExclusive,
            BigDecimal expectedMaxInclusive, BigDecimal expectedMaxExclusive) {
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

    public Object[] parametersForTestValidationGroupSetting() {
        return new Object[][]{
            {"skippedConfiguringGroups", "fieldWithoutValidationGroup", Boolean.TRUE, null},
            {"skippedConfiguringGroups", "fieldWithSingleValidationGroup", Boolean.TRUE, null},
            {"skippedConfiguringGroups", "fieldWithMultipleValidationGroups", Boolean.TRUE, null},
            {"noConfiguredGroups", "fieldWithoutValidationGroup", Boolean.TRUE, new Class<?>[0]},
            {"noConfiguredGroups", "fieldWithSingleValidationGroup", null, new Class<?>[0]},
            {"noConfiguredGroups", "fieldWithMultipleValidationGroups", null, new Class<?>[0]},
            {"singleConfiguredGroup", "fieldWithoutValidationGroup", Boolean.TRUE, new Class<?>[]{Test.class}},
            {"singleConfiguredMatchingGroup", "fieldWithSingleValidationGroup", Boolean.TRUE, new Class<?>[]{Test.class}},
            {"singleConfiguredMatchingGroup", "fieldWithMultipleValidationGroups", Boolean.TRUE, new Class<?>[]{Test.class}},
            {"singleConfiguredDifferentGroup", "fieldWithSingleValidationGroup", null, new Class<?>[]{Object.class}},
            {"singleConfiguredDifferentGroup", "fieldWithMultipleValidationGroups", null, new Class<?>[]{Object.class}},
            {"multipleConfiguredGroups", "fieldWithoutValidationGroup", Boolean.TRUE, new Class<?>[]{Test.class, Object.class}},
            {"multipleConfiguredGroupsSingleMatch", "fieldWithSingleValidationGroup", Boolean.TRUE, new Class<?>[]{Test.class, Object.class}},
            {"multipleConfiguredGroupsSingleMatch", "fieldWithMultipleValidationGroups", Boolean.TRUE, new Class<?>[]{Test.class, Object.class}},
            {"multipleConfiguredGroupsMultipleMatches", "fieldWithMultipleValidationGroups", Boolean.TRUE, new Class<?>[]{Test.class, Assert.class}},
            {"multipleConfiguredGroupsNoMatch", "fieldWithSingleValidationGroup", null, new Class<?>[]{Integer.class, Double.class}},
            {"multipleConfiguredGroupsNoMatch", "fieldWithMultipleValidationGroups", null, new Class<?>[]{Integer.class, Double.class}}
        };
    }

    @Test
    @Parameters
    @TestCaseName("{method}({0}, {1}, {2}) [{index}]")
    public void testValidationGroupSetting(String testCase, String fieldName, Boolean expectedResult, Class<?>[] validationGroups) {
        JavaxValidationModule module = new JavaxValidationModule();
        if (validationGroups != null) {
            module.forValidationGroups(validationGroups);
        }
        module.applyToConfigBuilder(this.configBuilder);

        ArgumentCaptor<ConfigFunction<FieldScope, Boolean>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withNullableCheck(captor.capture());
        TestType testType = new TestType(TestClassForValidationGroups.class);
        FieldScope field = testType.getMemberField(fieldName);

        Boolean result = captor.getValue().apply(field);
        Assert.assertEquals(expectedResult, result);
    }

    private static class TestClassForNullableCheck {

        Integer unannotatedField;
        @NotNull(groups = Test.class)
        Double notNullNumber;
        Double notNullOnGetterNumber;
        @NotEmpty(groups = Test.class)
        List<Object> notEmptyList;
        List<Object> notEmptyOnGetterList;
        @NotBlank(groups = Test.class)
        String notBlankString;
        String notBlankOnGetterString;
        @Null(groups = Test.class)
        Object nullField;
        Object nullGetter;

        public Integer getUnannotatedField() {
            return this.unannotatedField;
        }

        public Double getNotNullNumber() {
            return this.notNullNumber;
        }

        @NotNull(groups = Test.class)
        public Double getNotNullOnGetterNumber() {
            return this.notNullOnGetterNumber;
        }

        public List<Object> getNotEmptyList() {
            return this.notEmptyList;
        }

        @NotEmpty(groups = Test.class)
        public List<Object> getNotEmptyOnGetterList() {
            return this.notEmptyOnGetterList;
        }

        public String getNotBlankString() {
            return this.notBlankString;
        }

        @NotBlank(groups = Test.class)
        public String getNotBlankOnGetterString() {
            return this.notBlankOnGetterString;
        }

        public Object getNullField() {
            return this.nullField;
        }

        @Null(groups = Test.class)
        public Object getNullGetter() {
            return this.nullGetter;
        }
    }

    private static class TestClassForArrayItemCount {

        String[] unannotatedArray;
        @Size(min = 10, max = 20, groups = Test.class)
        String sizeTenToTwentyString;
        String sizeTenToTwentyOnGetterString;
        @Size(min = 5, groups = Test.class)
        int[] minSizeFiveArray;
        int[] minSizeFiveOnGetterArray;
        @Size(max = 50, groups = Test.class)
        long[] maxSizeFiftyArray;
        long[] maxSizeFiftyOnGetterArray;
        @Size(min = 10, max = 20, groups = Test.class)
        Set<Boolean> sizeTenToTwentySet;
        Set<Boolean> sizeTenToTwentyOnGetterSet;
        @NotEmpty(groups = Test.class)
        @Size(max = 100, groups = Test.class)
        List<Double> nonEmptyMaxSizeHundredList;
        List<Double> nonEmptyMaxSizeHundredOnGetterList;

        @Size(min = 10, max = 20, groups = Test.class)
        public String getSizeTenToTwentyString() {
            return this.sizeTenToTwentyString;
        }

        @Size(min = 5, groups = Test.class)
        public int[] getMinSizeFiveOnGetterArray() {
            return this.minSizeFiveOnGetterArray;
        }

        @Size(max = 50, groups = Test.class)
        public long[] getMaxSizeFiftyOnGetterArray() {
            return this.maxSizeFiftyOnGetterArray;
        }

        @Size(min = 10, max = 20, groups = Test.class)
        public Set<Boolean> getSizeTenToTwentyOnGetterSet() {
            return this.sizeTenToTwentyOnGetterSet;
        }

        @NotEmpty(groups = Test.class)
        @Size(max = 100, groups = Test.class)
        public List<Double> getNonEmptyMaxSizeHundredOnGetterList() {
            return this.nonEmptyMaxSizeHundredOnGetterList;
        }
    }

    private static class TestClassForStringProperties {

        String unannotatedString;
        @Size(min = 10, max = 20, groups = Test.class)
        @Email(groups = Test.class)
        @Pattern(regexp = ".*", groups = Test.class)
        int[] sizeTenToTwentyArray;
        int[] sizeTenToTwentyOnGetterArray;
        @Size(min = 5, groups = Test.class)
        @Pattern(regexp = "^\\d+$", groups = Test.class)
        CharSequence minSizeFiveSequence;
        CharSequence minSizeFiveOnGetterSequence;
        @Size(max = 50, groups = Test.class)
        String maxSizeFiftyString;
        String maxSizeFiftyOnGetterString;
        @Size(min = 10, max = 20, groups = Test.class)
        String sizeTenToTwentyString;
        String sizeTenToTwentyOnGetterString;
        @NotEmpty(groups = Test.class)
        @Size(max = 100, groups = Test.class)
        @Email(groups = Test.class)
        String nonEmptyMaxSizeHundredString;
        String nonEmptyMaxSizeHundredOnGetterString;
        @NotBlank(groups = Test.class)
        @Email(regexp = "^.+your-company\\.com$", groups = Test.class)
        String nonBlankString;
        String nonBlankOnGetterString;

        @Size(min = 10, max = 20, groups = Test.class)
        @Email(groups = Test.class)
        @Pattern(regexp = ".*", groups = Test.class)
        public int[] getSizeTenToTwentyOnGetterArray() {
            return this.sizeTenToTwentyOnGetterArray;
        }

        @Size(min = 5, groups = Test.class)
        @Pattern(regexp = "^\\d+$", groups = Test.class)
        public CharSequence getMinSizeFiveOnGetterSequence() {
            return this.minSizeFiveOnGetterSequence;
        }

        @Size(max = 50, groups = Test.class)
        public String getMaxSizeFiftyOnGetterString() {
            return this.maxSizeFiftyOnGetterString;
        }

        @Size(min = 10, max = 20, groups = Test.class)
        public String getSizeTenToTwentyOnGetterString() {
            return this.sizeTenToTwentyOnGetterString;
        }

        @NotEmpty(groups = Test.class)
        @Size(max = 100, groups = Test.class)
        @Email(groups = Test.class)
        public String getNonEmptyMaxSizeHundredOnGetterString() {
            return this.nonEmptyMaxSizeHundredOnGetterString;
        }

        @NotBlank(groups = Test.class)
        @Email(regexp = "^.+your-company\\.com$", groups = Test.class)
        public String getNonBlankOnGetterString() {
            return this.nonBlankOnGetterString;
        }
    }

    private static class TestClassForNumberMinMax {

        int unannotatedInt;
        @Min(value = -100L, groups = Test.class)
        long minMinusHundredLong;
        long minMinusHundredOnGetterLong;
        @Max(value = 50, groups = Test.class)
        short maxFiftyShort;
        short maxFiftyOnGetterShort;
        @DecimalMin(value = "10.1", groups = Test.class)
        @DecimalMax(value = "20.2", groups = Test.class)
        Integer tenToTwentyInclusiveInteger;
        Integer tenToTwentyInclusiveOnGetterInteger;
        @DecimalMin(value = "10.1", inclusive = false, groups = Test.class)
        @DecimalMax(value = "20.2", inclusive = false, groups = Test.class)
        Integer tenToTwentyExclusiveInteger;
        Integer tenToTwentyExclusiveOnGetterInteger;
        @Positive(groups = Test.class)
        byte positiveByte;
        byte positiveOnGetterByte;
        @PositiveOrZero(groups = Test.class)
        BigInteger positiveOrZeroBigInteger;
        BigInteger positiveOrZeroOnGetterBigInteger;
        @Negative(groups = Test.class)
        BigDecimal negativeDecimal;
        BigDecimal negativeOnGetterDecimal;
        @NegativeOrZero(groups = Test.class)
        Long negativeOrZeroLong;
        Long negativeOrZeroOnGetterLong;

        @Min(value = -100L, groups = Test.class)
        public long getMinMinusHundredOnGetterLong() {
            return minMinusHundredOnGetterLong;
        }

        @Max(value = 50, groups = Test.class)
        public short getMaxFiftyOnGetterShort() {
            return maxFiftyOnGetterShort;
        }

        @DecimalMin(value = "10.1", groups = Test.class)
        @DecimalMax(value = "20.2", groups = Test.class)
        public Integer getTenToTwentyInclusiveOnGetterInteger() {
            return tenToTwentyInclusiveOnGetterInteger;
        }

        @DecimalMin(value = "10.1", inclusive = false, groups = Test.class)
        @DecimalMax(value = "20.2", inclusive = false, groups = Test.class)
        public Integer getTenToTwentyExclusiveOnGetterInteger() {
            return tenToTwentyExclusiveOnGetterInteger;
        }

        @Positive(groups = Test.class)
        public byte getPositiveOnGetterByte() {
            return positiveOnGetterByte;
        }

        @PositiveOrZero(groups = Test.class)
        public BigInteger getPositiveOrZeroOnGetterBigInteger() {
            return positiveOrZeroOnGetterBigInteger;
        }

        @Negative(groups = Test.class)
        public BigDecimal getNegativeOnGetterDecimal() {
            return negativeOnGetterDecimal;
        }

        @NegativeOrZero(groups = Test.class)
        public Long getNegativeOrZeroOnGetterLong() {
            return negativeOrZeroOnGetterLong;
        }
    }

    private static class TestClassForValidationGroups {

        @Null
        String fieldWithoutValidationGroup;
        @Null(groups = Test.class)
        String fieldWithSingleValidationGroup;
        @Null(groups = {Test.class, Assert.class})
        String fieldWithMultipleValidationGroups;
    }
}
