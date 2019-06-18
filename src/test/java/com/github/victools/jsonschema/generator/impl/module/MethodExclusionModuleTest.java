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

import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.AbstractTypeAwareTest;
import java.util.function.Predicate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test for the {@link MethodExclusionModule} class.
 */
@RunWith(JUnitParamsRunner.class)
public class MethodExclusionModuleTest extends AbstractTypeAwareTest {

    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    public MethodExclusionModuleTest() {
        super(TestClass.class);
    }

    @Before
    public void setUp() {
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.methodConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(this.builder.forMethods()).thenReturn(this.methodConfigPart);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyToConfigBuilder() {
        Predicate<MethodScope> ignoreCheck = method -> true;
        MethodExclusionModule module = new MethodExclusionModule(ignoreCheck);
        module.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forMethods();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.methodConfigPart).withIgnoreCheck(ignoreCheck);
        Mockito.verifyNoMoreInteractions(this.methodConfigPart);
    }

    Object parametersForTestIgnoreCheck() {
        return new Object[][]{
            {"getIntValue", "forVoidMethods", false},
            {"getIntValue", "forGetterMethods", true},
            {"getIntValue", "forNonStaticNonVoidNonGetterMethods", false},
            {"isGetterFlag", "forVoidMethods", false},
            {"isGetterFlag", "forGetterMethods", true},
            {"isGetterFlag", "forNonStaticNonVoidNonGetterMethods", false},
            {"isNoGetter", "forVoidMethods", false},
            {"isNoGetter", "forGetterMethods", false},
            {"isNoGetter", "forNonStaticNonVoidNonGetterMethods", true},
            {"getCalculatedValue", "forVoidMethods", false},
            {"getCalculatedValue", "forGetterMethods", false},
            {"getCalculatedValue", "forNonStaticNonVoidNonGetterMethods", true},
            {"returningVoid", "forVoidMethods", true},
            {"returningVoid", "forGetterMethods", false},
            {"returningVoid", "forNonStaticNonVoidNonGetterMethods", false},
            {"staticReturningVoid", "forVoidMethods", true},
            {"staticReturningVoid", "forGetterMethods", false},
            {"staticReturningVoid", "forNonStaticNonVoidNonGetterMethods", false},
            {"staticCalculation", "forVoidMethods", false},
            {"staticCalculation", "forGetterMethods", false},
            {"staticCalculation", "forNonStaticNonVoidNonGetterMethods", false}
        };
    }

    @Test
    @Parameters
    @TestCaseName("{method}({1}: {0} => {2}) [{index}]")
    public void testIgnoreCheck(String testMethodName, String supplierMethodName, boolean ignored) throws Exception {
        MethodExclusionModule moduleInstance = (MethodExclusionModule) MethodExclusionModule.class.getMethod(supplierMethodName).invoke(null);
        moduleInstance.applyToConfigBuilder(this.builder);

        MethodScope method = this.getTestClassMethod(testMethodName);
        Assert.assertEquals(ignored, this.methodConfigPart.shouldIgnore(method));
    }

    private static class TestClass {

        int intValue;
        boolean getterFlag;

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

        public void returningVoid() {
            // nothing being returned
        }

        public static void staticReturningVoid() {
            // nothing being returned
        }

        public static int staticCalculation() {
            return 20 + 1;
        }
    }
}
