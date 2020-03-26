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
import java.util.Collection;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test for the {@link ConstantValueModule} class.
 */
@RunWith(JUnitParamsRunner.class)
public class ConstantValueModuleTest extends AbstractTypeAwareTest {

    private ConstantValueModule instance;
    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;

    public ConstantValueModuleTest() {
        super(TestClass.class);
    }

    @Before
    public void setUp() {
        this.instance = new ConstantValueModule();
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(builder.forFields()).thenReturn(this.fieldConfigPart);
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyToConfigBuilder() {
        this.instance.applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forFields();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.fieldConfigPart).withEnumResolver(Mockito.any());
        Mockito.verify(this.fieldConfigPart).withNullableCheck(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.fieldConfigPart);
    }

    Object parametersForTestEnumResolver() {
        return new Object[][]{
            {"staticFinalValueField", Collections.singletonList(42)},
            {"staticFinalNullField", Collections.singletonList(null)},
            {"staticField", null},
            {"finalField", null},
            {"normalField", null}
        };
    }

    @Test
    @Parameters
    public void testEnumResolver(String fieldName, Collection<?> expectedResult) throws Exception {
        this.instance.applyToConfigBuilder(this.builder);

        FieldScope field = this.getTestClassField(fieldName);
        Collection<?> result = this.fieldConfigPart.resolveEnum(field);
        Assert.assertEquals(expectedResult, result);
    }

    Object parametersForTestNullableCheck() {
        return new Object[][]{
            {"staticFinalValueField", Boolean.FALSE},
            {"staticFinalNullField", Boolean.TRUE},
            {"staticField", null},
            {"finalField", null},
            {"normalField", null}
        };
    }

    @Test
    @Parameters
    public void testNullableCheck(String fieldName, Boolean expectedResult) throws Exception {
        this.instance.applyToConfigBuilder(this.builder);

        FieldScope field = this.getTestClassField(fieldName);
        Boolean result = this.fieldConfigPart.isNullable(field);
        Assert.assertEquals(expectedResult, result);
    }

    private static class TestClass {

        static final int staticFinalValueField = 42;
        static final Object staticFinalNullField = null;
        static String staticField = "value";
        final long finalField = 21L;
        double normalField = 10.5;
    }
}
