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
import com.github.victools.jsonschema.generator.MethodScope;
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
 * Test for the {@link MethodExclusionModule} class.
 */
public class MethodExclusionModuleTest extends AbstractTypeAwareTest {

    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    public MethodExclusionModuleTest() {
        super(TestClass.class);
    }

    @BeforeEach
    public void setUp() {
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.methodConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(this.builder.forMethods()).thenReturn(this.methodConfigPart);
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
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

    static Stream<Arguments> parametersForTestIgnoreCheck() {
        return Stream.of(
            Arguments.of("getIntValue", "forVoidMethods", false),
            Arguments.of("getIntValue", "forGetterMethods", true),
            Arguments.of("getIntValue", "forNonStaticNonVoidNonGetterMethods", false),
            Arguments.of("isGetterFlag", "forVoidMethods", false),
            Arguments.of("isGetterFlag", "forGetterMethods", true),
            Arguments.of("isGetterFlag", "forNonStaticNonVoidNonGetterMethods", false),
            Arguments.of("isNoGetter", "forVoidMethods", false),
            Arguments.of("isNoGetter", "forGetterMethods", false),
            Arguments.of("isNoGetter", "forNonStaticNonVoidNonGetterMethods", true),
            Arguments.of("getCalculatedValue", "forVoidMethods", false),
            Arguments.of("getCalculatedValue", "forGetterMethods", false),
            Arguments.of("getCalculatedValue", "forNonStaticNonVoidNonGetterMethods", true),
            Arguments.of("returningVoid", "forVoidMethods", true),
            Arguments.of("returningVoid", "forGetterMethods", false),
            Arguments.of("returningVoid", "forNonStaticNonVoidNonGetterMethods", false),
            Arguments.of("staticReturningVoid", "forVoidMethods", true),
            Arguments.of("staticReturningVoid", "forGetterMethods", false),
            Arguments.of("staticReturningVoid", "forNonStaticNonVoidNonGetterMethods", false),
            Arguments.of("staticCalculation", "forVoidMethods", false),
            Arguments.of("staticCalculation", "forGetterMethods", false),
            Arguments.of("staticCalculation", "forNonStaticNonVoidNonGetterMethods", false)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestIgnoreCheck")
    public void testIgnoreCheck(String testMethodName, String supplierMethodName, boolean ignored) throws Exception {
        MethodExclusionModule moduleInstance = (MethodExclusionModule) MethodExclusionModule.class.getMethod(supplierMethodName).invoke(null);
        moduleInstance.applyToConfigBuilder(this.builder);

        MethodScope method = this.getTestClassMethod(testMethodName);
        Assertions.assertEquals(ignored, this.methodConfigPart.shouldIgnore(method));
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
