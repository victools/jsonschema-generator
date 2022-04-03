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

package com.github.victools.jsonschema.generator.impl.module;

import com.github.victools.jsonschema.generator.AbstractTypeAwareTest;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * Test for the {@link FieldExclusionModule} class.
 */
public class FieldExclusionModuleTest extends AbstractTypeAwareTest {

    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;

    public FieldExclusionModuleTest() {
        super(TestClass.class);
    }

    @BeforeEach
    public void setUp() {
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(builder.forFields()).thenReturn(this.fieldConfigPart);
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyToConfigBuilder() {
        Predicate<FieldScope> ignoreCheck = field -> true;
        FieldExclusionModule module = new FieldExclusionModule(ignoreCheck);
        module.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forFields();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.fieldConfigPart).withIgnoreCheck(ignoreCheck);
        Mockito.verifyNoMoreInteractions(this.fieldConfigPart);
    }

    static Stream<Arguments> parametersForTestIgnoreCheck() {
        return Stream.of(
            Arguments.of("publicStaticField", "forPublicNonStaticFields", false),
            Arguments.of("nonPublicStaticField", "forPublicNonStaticFields", false),
            Arguments.of("publicNonStaticField", "forPublicNonStaticFields", true),
            Arguments.of("nonPublicNonStaticFieldWithGetter", "forNonPublicNonStaticFieldsWithGetter", true),
            Arguments.of("nonPublicNonStaticFieldWithGetter", "forNonPublicNonStaticFieldsWithoutGetter", false),
            Arguments.of("nonPublicNonStaticFieldWithoutGetter", "forNonPublicNonStaticFieldsWithGetter", false),
            Arguments.of("nonPublicNonStaticFieldWithoutGetter", "forNonPublicNonStaticFieldsWithoutGetter", true),
            Arguments.of("transientField", "forNonPublicNonStaticFieldsWithoutGetter", true),
            Arguments.of("transientField", "forTransientFields", true)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestIgnoreCheck")
    public void testIgnoreCheck(String testFieldName, String supplierMethodName, boolean ignored) throws Exception {
        FieldExclusionModule moduleInstance = (FieldExclusionModule) FieldExclusionModule.class.getMethod(supplierMethodName).invoke(null);
        moduleInstance.applyToConfigBuilder(this.builder);

        FieldScope field = this.getTestClassField(testFieldName);
        Assertions.assertEquals(ignored, this.fieldConfigPart.shouldIgnore(field));
    }

    private static class TestClass {

        public static double publicStaticField;
        protected static long nonPublicStaticField;
        public String publicNonStaticField;
        int nonPublicNonStaticFieldWithoutGetter;
        private boolean nonPublicNonStaticFieldWithGetter;
        transient float transientField;

        public boolean isNonPublicNonStaticFieldWithGetter() {
            return this.nonPublicNonStaticFieldWithGetter;
        }
    }
}
