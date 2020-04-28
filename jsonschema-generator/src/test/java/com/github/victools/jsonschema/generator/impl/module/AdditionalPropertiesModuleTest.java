/*
 * Copyright 2020 VicTools.
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
import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Test for the {@link AdditionalPropertiesModule} class.
 */
@RunWith(JUnitParamsRunner.class)
public class AdditionalPropertiesModuleTest extends AbstractTypeAwareTest {

    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorGeneralConfigPart generalConfigPart;

    public AdditionalPropertiesModuleTest() {
        super(TestMapSubType.class);
    }

    @Before
    public void setUp() {
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.generalConfigPart = Mockito.spy(new SchemaGeneratorGeneralConfigPart());
        Mockito.when(this.builder.forTypesInGeneral()).thenReturn(this.generalConfigPart);
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    @Test
    public void testApplyToConfigBuilderForMapValuesModule() {
        AdditionalPropertiesModule.forMapValues()
                .applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forTypesInGeneral();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.generalConfigPart).withAdditionalPropertiesResolver(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.generalConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderForForbiddenForAllObjectsModule() {
        AdditionalPropertiesModule.forbiddenForAllObjectsButContainers()
                .applyToConfigBuilder(this.builder);

        Mockito.verify(this.builder).forTypesInGeneral();
        Mockito.verifyNoMoreInteractions(this.builder);
        Mockito.verify(this.generalConfigPart).withAdditionalPropertiesResolver(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.generalConfigPart);
    }

    public Object[] parametersForTestResolveAdditionalProperties() {
        AdditionalPropertiesModule mapValuesInstance = AdditionalPropertiesModule.forMapValues();
        AdditionalPropertiesModule noneButContainersInstance = AdditionalPropertiesModule.forbiddenForAllObjectsButContainers();
        return new Object[][]{
            {mapValuesInstance, null, String.class, new Type[0]},
            {noneButContainersInstance, Void.class, String.class, new Type[0]},
            {mapValuesInstance, Integer.class, Map.class, new Type[]{String.class, Integer.class}},
            {noneButContainersInstance, Void.class, Map.class, new Type[]{String.class, Integer.class}},
            {mapValuesInstance, String.class, TestMapSubType.class, new Type[0]},
            {noneButContainersInstance, Void.class, TestMapSubType.class, new Type[0]},
            {mapValuesInstance, null, List.class, new Type[]{String.class}},
            {noneButContainersInstance, null, List.class, new Type[]{String.class}}
        };
    }

    @Test
    @Parameters
    @SuppressWarnings("unchecked")
    public void testResolveAdditionalProperties(Module moduleInstance, Type expectedAdditionalProperties, Type type, Type[] typeParameters) {
        moduleInstance.applyToConfigBuilder(this.builder);
        ArgumentCaptor<ConfigFunction<TypeScope, Type>> resolverCaptor = ArgumentCaptor.forClass(ConfigFunction.class);
        Mockito.verify(this.generalConfigPart).withAdditionalPropertiesResolver(resolverCaptor.capture());
        ConfigFunction<TypeScope, Type> additionalPropertiesResolver = resolverCaptor.getValue();

        TypeContext typeContext = this.getContext().getTypeContext();
        TypeScope scope = typeContext.createTypeScope(typeContext.resolve(type, typeParameters));
        Type result = additionalPropertiesResolver.apply(scope);

        if (expectedAdditionalProperties == null || expectedAdditionalProperties == Void.class) {
            Assert.assertEquals(expectedAdditionalProperties, result);
        } else {
            Assert.assertEquals(typeContext.resolve(expectedAdditionalProperties), result);
        }
    }

    private static class TestMapSubType extends HashMap<Object, String> {
        // no further fields
    }
}
