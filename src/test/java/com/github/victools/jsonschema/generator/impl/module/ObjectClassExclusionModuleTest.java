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
import java.lang.reflect.Method;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test for the {@link ObjectClassExclusionModule} class.
 */
@RunWith(JUnitParamsRunner.class)
public class ObjectClassExclusionModuleTest {

    private ObjectClassExclusionModule instance;
    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<Method> methodConfigPart;

    @Before
    public void setUp() {
        this.instance = new ObjectClassExclusionModule();
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.methodConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(builder.forMethods()).thenReturn(this.methodConfigPart);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyToConfigBuilder() {
        this.instance.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forMethods();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.methodConfigPart).addIgnoreCheck(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.methodConfigPart);
    }

    Object parametersForTestIgnoreCheck() {
        return new Object[][]{
            {"declaredHere", false},
            {"hashCode", true}
        };
    }

    @Test
    @Parameters
    public void testIgnoreCheck(String methodName, boolean expectedResult) throws Exception {
        this.instance.applyToConfigBuilder(this.builder);

        Method method = TestClass.class.getMethod(methodName);
        boolean result = this.methodConfigPart.shouldIgnore(method);
        Assert.assertEquals(expectedResult, result);
    }

    private static class TestClass {

        public void declaredHere() {
            // nothing to do
        }
    }
}
