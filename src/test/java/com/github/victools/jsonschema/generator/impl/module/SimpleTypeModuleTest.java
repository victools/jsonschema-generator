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

import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorTypeConfigPart;
import com.github.victools.jsonschema.generator.TypeScope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link SimpleTypeModule} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleTypeModuleTest {

    private SimpleTypeModule instance;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SchemaGeneratorConfigBuilder builder;
    @Mock(answer = Answers.RETURNS_SELF)
    private SchemaGeneratorTypeConfigPart<TypeScope> typeConfigPart;

    @Before
    public void setUp() {
        this.instance = new SimpleTypeModule();
        Mockito.when(this.builder.forTypesInGeneral()).thenReturn(this.typeConfigPart);
    }

    @Test
    public void testApplyToConfigBuilder() {
        this.instance.applyToConfigBuilder(this.builder);

        SchemaGeneratorConfigPart<FieldScope> fieldConfigPart = this.builder.forFields();
        Mockito.verify(fieldConfigPart).withNullableCheck(Mockito.any());
        Mockito.verifyNoMoreInteractions(fieldConfigPart);
        Mockito.verify(this.builder, Mockito.times(2)).forFields();

        SchemaGeneratorConfigPart<MethodScope> methodConfigPart = this.builder.forMethods();
        Mockito.verify(methodConfigPart).withNullableCheck(Mockito.any());
        Mockito.verifyNoMoreInteractions(methodConfigPart);
        Mockito.verify(this.builder, Mockito.times(2)).forMethods();

        Mockito.verify(this.typeConfigPart).withAdditionalPropertiesResolver(Mockito.any());
        Mockito.verify(this.typeConfigPart).withPatternPropertiesResolver(Mockito.any());
        Mockito.verifyNoMoreInteractions(this.typeConfigPart);
        Mockito.verify(this.builder).forTypesInGeneral();

        Mockito.verify(this.builder).getObjectMapper();
        Mockito.verify(this.builder).with(Mockito.any(CustomDefinitionProviderV2.class));

        Mockito.verifyNoMoreInteractions(this.builder);
    }
}
