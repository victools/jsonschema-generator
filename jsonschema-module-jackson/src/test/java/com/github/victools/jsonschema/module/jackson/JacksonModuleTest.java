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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.stream.Collectors;
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
 * Test for the {@link JacksonModule}.
 */
public class JacksonModuleTest {

    private SchemaGeneratorConfigBuilder configBuilder;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;
    private SchemaGeneratorGeneralConfigPart typesInGeneralConfigPart;

    @BeforeEach
    public void setUp() {
        this.configBuilder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(this.configBuilder.forFields()).thenReturn(this.fieldConfigPart);
        this.methodConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(this.configBuilder.forMethods()).thenReturn(this.methodConfigPart);
        this.typesInGeneralConfigPart = Mockito.spy(new SchemaGeneratorGeneralConfigPart());
        Mockito.when(this.configBuilder.forTypesInGeneral()).thenReturn(this.typesInGeneralConfigPart);
    }

    @Test
    public void testApplyToConfigBuilder() {
        new JacksonModule().applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations(true, 1);

        Mockito.verify(this.typesInGeneralConfigPart).withSubtypeResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withTargetTypeOverridesResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withTargetTypeOverridesResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withCustomDefinitionProvider(Mockito.any());
        Mockito.verify(this.methodConfigPart).withCustomDefinitionProvider(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart, this.typesInGeneralConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithRespectJsonPropertyOrderOption() {
        new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_ORDER, JacksonOption.SKIP_SUBTYPE_LOOKUP, JacksonOption.IGNORE_TYPE_INFO_TRANSFORM)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations(true, 0);

        Mockito.verify(this.typesInGeneralConfigPart).withPropertySorter(Mockito.any(JsonPropertySorter.class));

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart, this.typesInGeneralConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithoutOptionalFeatures() {
        new JacksonModule(JacksonOption.IGNORE_PROPERTY_NAMING_STRATEGY, JacksonOption.SKIP_SUBTYPE_LOOKUP, JacksonOption.IGNORE_TYPE_INFO_TRANSFORM)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations(false, 0);

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart, this.typesInGeneralConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithSkipSubtypeLookupOption() {
        new JacksonModule(JacksonOption.SKIP_SUBTYPE_LOOKUP)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations(true, 1);

        Mockito.verify(this.fieldConfigPart).withCustomDefinitionProvider(Mockito.any());
        Mockito.verify(this.methodConfigPart).withCustomDefinitionProvider(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart, this.typesInGeneralConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithIgnoreTypeInfoTranformOption() {
        new JacksonModule(JacksonOption.IGNORE_TYPE_INFO_TRANSFORM)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations(true, 0);

        Mockito.verify(this.typesInGeneralConfigPart).withSubtypeResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withTargetTypeOverridesResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withTargetTypeOverridesResolver(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart, this.typesInGeneralConfigPart);
    }

    static Stream<Arguments> parametersForTestApplyToConfigBuilderWithEnumOptions() {
        return Stream.of(
            Arguments.of((Object) new JacksonOption[]{JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE}),
            Arguments.of((Object) new JacksonOption[]{JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY}),
            Arguments.of((Object) new JacksonOption[]{JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE, JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY})
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestApplyToConfigBuilderWithEnumOptions")
    public void testApplyToConfigBuilderWithEnumOptions(JacksonOption[] options) {
        new JacksonModule(options)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations(true, 2);

        Mockito.verify(this.typesInGeneralConfigPart).withSubtypeResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withTargetTypeOverridesResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withCustomDefinitionProvider(Mockito.any());
        Mockito.verify(this.methodConfigPart).withTargetTypeOverridesResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withCustomDefinitionProvider(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart, this.typesInGeneralConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderWithIdentityReferenceOption() {
        new JacksonModule(JacksonOption.JSONIDENTITY_REFERENCE_ALWAYS_AS_ID)
                .applyToConfigBuilder(this.configBuilder);

        this.verifyCommonConfigurations(true, 2);

        Mockito.verify(this.typesInGeneralConfigPart).withSubtypeResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withTargetTypeOverridesResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart, Mockito.times(2)).withCustomDefinitionProvider(Mockito.any());
        Mockito.verify(this.methodConfigPart).withTargetTypeOverridesResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart, Mockito.times(2)).withCustomDefinitionProvider(Mockito.any());

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.fieldConfigPart, this.methodConfigPart, this.typesInGeneralConfigPart);
    }

    private void verifyCommonConfigurations(boolean considerNamingStrategy, int additionalCustomTypeDefinitions) {
        Mockito.verify(this.configBuilder).getObjectMapper();
        Mockito.verify(this.configBuilder).forFields();
        Mockito.verify(this.configBuilder).forMethods();
        Mockito.verify(this.configBuilder).forTypesInGeneral();

        Mockito.verify(this.fieldConfigPart).withDescriptionResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withIgnoreCheck(Mockito.any());
        Mockito.verify(this.fieldConfigPart, Mockito.times(considerNamingStrategy ? 2 : 1)).withPropertyNameOverrideResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withReadOnlyCheck(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withWriteOnlyCheck(Mockito.any());

        Mockito.verify(this.methodConfigPart).withDescriptionResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withIgnoreCheck(Mockito.any());
        Mockito.verify(this.methodConfigPart).withPropertyNameOverrideResolver(Mockito.any());
        Mockito.verify(this.methodConfigPart).withReadOnlyCheck(Mockito.any());
        Mockito.verify(this.methodConfigPart).withWriteOnlyCheck(Mockito.any());

        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(Mockito.any());
        Mockito.verify(this.typesInGeneralConfigPart, Mockito.times(1 + additionalCustomTypeDefinitions))
                .withCustomDefinitionProvider(Mockito.any());
    }

    static Stream<Arguments> parametersForTestPropertyNameOverride() {
        return Stream.of(
            Arguments.of("unannotatedField", null, "unannotated-field"),
            Arguments.of("fieldWithEmptyPropertyAnnotation", null, "field-with-empty-property-annotation"),
            Arguments.of("fieldWithSameValuePropertyAnnotation", null, "field-with-same-value-property-annotation"),
            Arguments.of("fieldWithNameOverride", "field override 1", "field-with-name-override"),
            Arguments.of("fieldWithNameOverrideOnGetter", "method override 1", "field-with-name-override-on-getter"),
            Arguments.of("fieldWithNameOverrideAndOnGetter", "field override 2", "field-with-name-override-and-on-getter")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestPropertyNameOverride")
    public void testPropertyNameOverride(String fieldName, String expectedOverrideValue, String kebabCaseName) throws Exception {
        new JacksonModule().applyToConfigBuilder(this.configBuilder);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart, Mockito.times(2)).withPropertyNameOverrideResolver(captor.capture());

        FieldScope field = new TestType(TestClassForPropertyNameOverride.class).getMemberField(fieldName);
        List<String> overrideValues = captor.getAllValues().stream()
                .map(nameOverride -> nameOverride.apply(field))
                .collect(Collectors.toList());
        Assertions.assertEquals(expectedOverrideValue, overrideValues.get(0));
        Assertions.assertEquals(kebabCaseName, overrideValues.get(1));
    }

    static Stream<Arguments> parametersForTestDescriptionResolver() {
        return Stream.of(
            Arguments.of("unannotatedField", null),
            Arguments.of("fieldWithDescription", "field description 1"),
            Arguments.of("fieldWithDescriptionOnGetter", "getter description 1"),
            Arguments.of("fieldWithDescriptionAndOnGetter", "wrapped property description"),
            Arguments.of("fieldWithDescriptionOnType", null)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestDescriptionResolver")
    public void testDescriptionResolver(String fieldName, String expectedDescription) throws Exception {
        new JacksonModule().applyToConfigBuilder(this.configBuilder);

        FieldScope field = new TestType(TestClassForDescription.class).getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<FieldScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.fieldConfigPart).withDescriptionResolver(captor.capture());
        String description = captor.getValue().apply(field);
        Assertions.assertEquals(expectedDescription, description);
    }

    static Stream<Arguments> parametersForTestRequiredProperty() {
        return Stream.of(
            Arguments.of(null, "requiredTrue", false),
            Arguments.of(null, "requiredFalseWriteOnly", false),
            Arguments.of(null, "requiredDefaultReadOnly", false),
            Arguments.of(null, "requiredAbsent", false),
            Arguments.of(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED, "requiredTrue", true),
            Arguments.of(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED, "requiredFalseWriteOnly", false),
            Arguments.of(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED, "requiredDefaultReadOnly", false),
            Arguments.of(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED, "requiredAbsent", false)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestRequiredProperty")
    public void testRequiredProperty(JacksonOption requiredOption, String fieldName, boolean expectedRequired) {
        new JacksonModule(requiredOption).applyToConfigBuilder(this.configBuilder);

        FieldScope field = new TestType(TestClassWithRequiredAnnotatedFields.class).getMemberField(fieldName);

        Assertions.assertEquals(this.fieldConfigPart.isRequired(field), expectedRequired);
    }

    static Stream<Arguments> parametersForTestReadOnlyWriteOnly() {
        return Stream.of(
            Arguments.of("requiredTrue", false, false),
            Arguments.of("requiredFalseWriteOnly", false, true),
            Arguments.of("requiredDefaultReadOnly", true, false),
            Arguments.of("requiredAbsent", false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestReadOnlyWriteOnly")
    public void testReadOnlyWriteOnly(String fieldName, boolean expectedReadOnly, boolean expectedWriteOnly) {
        new JacksonModule().applyToConfigBuilder(this.configBuilder);

        FieldScope field = new TestType(TestClassWithRequiredAnnotatedFields.class).getMemberField(fieldName);

        Assertions.assertEquals(this.fieldConfigPart.isReadOnly(field), expectedReadOnly);
        Assertions.assertEquals(this.fieldConfigPart.isWriteOnly(field), expectedWriteOnly);
    }

    static Stream<Arguments> parametersForTestDescriptionForTypeResolver() {
        return Stream.of(
            Arguments.of("unannotatedField", null),
            Arguments.of("fieldWithDescription", null),
            Arguments.of("fieldWithDescriptionOnGetter", null),
            Arguments.of("fieldWithDescriptionAndOnGetter", null),
            Arguments.of("fieldWithDescriptionOnType", "class description text")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestDescriptionForTypeResolver")
    public void testDescriptionForTypeResolver(String fieldName, String expectedDescription) throws Exception {
        new JacksonModule().applyToConfigBuilder(this.configBuilder);

        FieldScope field = new TestType(TestClassForDescription.class).getMemberField(fieldName);

        ArgumentCaptor<ConfigFunction<TypeScope, String>> captor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(captor.capture());
        String description = captor.getValue().apply(field);
        Assertions.assertEquals(expectedDescription, description);
    }

    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    private static class TestClassForPropertyNameOverride {

        Integer unannotatedField;
        @JsonProperty
        Double fieldWithEmptyPropertyAnnotation;
        @JsonProperty(value = "fieldWithSameValuePropertyAnnotation")
        Float fieldWithSameValuePropertyAnnotation;
        @JsonProperty(value = "field override 1")
        Long fieldWithNameOverride;
        Boolean fieldWithNameOverrideOnGetter;
        @JsonProperty(value = "field override 2")
        String fieldWithNameOverrideAndOnGetter;

        public Integer getUnannotatedField() {
            return this.unannotatedField;
        }

        @JsonProperty(value = "method override 1")
        public boolean isFieldWithNameOverrideOnGetter() {
            return this.fieldWithNameOverrideOnGetter;
        }

        @JsonProperty(value = "method override 2")
        public String getFieldWithNameOverrideAndOnGetter() {
            return this.fieldWithNameOverrideAndOnGetter;
        }
    }

    @JsonClassDescription(value = "class description text")
    private static class TestClassForDescription {

        Integer unannotatedField;
        @JsonPropertyDescription(value = "field description 1")
        Double fieldWithDescription;
        Float fieldWithDescriptionOnGetter;
        @AnnotationWrapper
        Long fieldWithDescriptionAndOnGetter;
        TestClassForDescription fieldWithDescriptionOnType;

        @JsonPropertyDescription(value = "getter description 1")
        public Float getFieldWithDescriptionOnGetter() {
            return this.fieldWithDescriptionOnGetter;
        }

        @JsonPropertyDescription(value = "getter description 2")
        public Long getFieldWithDescriptionAndOnGetter() {
            return fieldWithDescriptionAndOnGetter;
        }
    }

    private static class TestClassWithRequiredAnnotatedFields {
        @JsonProperty(required = true, access = JsonProperty.Access.READ_WRITE)
        private String requiredTrue;

        @JsonProperty(required = false, access = JsonProperty.Access.WRITE_ONLY)
        private String requiredFalseWriteOnly;

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private String requiredDefaultReadOnly;

        private String requiredAbsent;

    }

    @JacksonAnnotationsInside
    @JsonPropertyDescription("wrapped property description")
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnotationWrapper {}

}
