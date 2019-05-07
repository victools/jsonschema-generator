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

import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test for the {@link FieldWithoutGetterExclusionModule} class.
 */
@RunWith(JUnitParamsRunner.class)
public class FieldWithoutGetterExclusionModuleTest {

    private FieldWithoutGetterExclusionModule instance;
    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<Field> fieldConfigPart;

    @Before
    public void setUp() {
        this.instance = new FieldWithoutGetterExclusionModule();
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(builder.forFields()).thenReturn(this.fieldConfigPart);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyToConfigBuilder() {
        this.instance.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forFields();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.fieldConfigPart).addIgnoreCheck(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.fieldConfigPart);
    }

    Object parametersForTestIgnoreCheck() {
        return new Object[][]{
            {"intValue", false},
            {"getterFlag", false},
            {"unaccessibleField", true},
            {"publicFieldWithoutGetter", false}
        };
    }

    @Test
    @Parameters
    public void testIgnoreCheck(String fieldName, boolean expectedResult) throws Exception {
        this.instance.applyToConfigBuilder(this.builder);

        Field field = TestClass.class.getDeclaredField(fieldName);
        boolean result = this.fieldConfigPart.shouldIgnore(field);
        Assert.assertEquals(expectedResult, result);
    }

    private static class TestClass {

        int intValue;
        boolean getterFlag;
        long unaccessibleField;
        public String publicFieldWithoutGetter;

        public int getIntValue() {
            return this.intValue;
        }

        public boolean isGetterFlag() {
            return this.getterFlag;
        }

        public boolean isNoGetter() {
            return true;
        }

        public long getCalculatedValue() {
            return 42;
        }
    }
}
