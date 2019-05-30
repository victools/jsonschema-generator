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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.github.victools.jsonschema.generator.AbstractAnnotationAwareTest;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import java.util.Map;
import java.util.function.BiPredicate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test for the {@link FieldExclusionModule} class.
 */
@RunWith(JUnitParamsRunner.class)
public class FieldExclusionModuleTest extends AbstractAnnotationAwareTest {

    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<AnnotatedField> fieldConfigPart;

    @Before
    public void setUp() {
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(builder.forFields()).thenReturn(this.fieldConfigPart);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyToConfigBuilder() {
        BiPredicate<AnnotatedField, BeanDescription> ignoreCheck = (field, context) -> true;
        FieldExclusionModule module = new FieldExclusionModule(ignoreCheck);
        module.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forFields();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.fieldConfigPart).withIgnoreCheck(ignoreCheck);
        Mockito.verifyNoMoreInteractions(this.fieldConfigPart);
    }

    Object parametersForTestIgnoreCheck() {
        return new Object[][]{
            {"publicStaticField", "forPublicStaticFields", true},
            {"publicStaticField", "forNonPublicStaticFields", false},
            {"publicStaticField", "forPublicNonStaticFields", false},
            {"nonPublicStaticField", "forPublicStaticFields", false},
            {"nonPublicStaticField", "forNonPublicStaticFields", true},
            {"nonPublicStaticField", "forPublicNonStaticFields", false},
            {"publicNonStaticField", "forPublicStaticFields", false},
            {"publicNonStaticField", "forNonPublicStaticFields", false},
            {"publicNonStaticField", "forPublicNonStaticFields", true},
            {"nonPublicNonStaticFieldWithGetter", "forNonPublicNonStaticFieldsWithGetter", true},
            {"nonPublicNonStaticFieldWithGetter", "forNonPublicNonStaticFieldsWithoutGetter", false},
            {"nonPublicNonStaticFieldWithoutGetter", "forNonPublicNonStaticFieldsWithGetter", false},
            {"nonPublicNonStaticFieldWithoutGetter", "forNonPublicNonStaticFieldsWithoutGetter", true},
            {"transientField", "forNonPublicNonStaticFieldsWithoutGetter", true},
            {"transientField", "forTransientFields", true}
        };
    }

    @Test
    @Parameters
    @TestCaseName("{method}({1}: {0} => {2}) [{index}]")
    public void testIgnoreCheck(String testFieldName, String supplierMethodName, boolean ignored) throws Exception {
        FieldExclusionModule moduleInstance = (FieldExclusionModule) FieldExclusionModule.class.getMethod(supplierMethodName).invoke(null);
        moduleInstance.applyToConfigBuilder(this.builder);

        Map.Entry<AnnotatedField, BeanDescription> annotatedField = this.wrapField(TestClass.class.getDeclaredField(testFieldName));
        Assert.assertEquals(ignored, this.fieldConfigPart.shouldIgnore(annotatedField.getKey(), annotatedField.getValue()));
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
