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

import com.github.victools.jsonschema.generator.CustomDefinitionProvider;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for {@link SimpleTypeModule} class.
 */
public class SimpleTypeModuleTest {

    private SimpleTypeModule instance;
    private SchemaGeneratorConfigBuilder builder;

    @Before
    public void setUp() {
        this.instance = new SimpleTypeModule();
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class, Mockito.RETURNS_DEEP_STUBS);
    }

    @Test
    public void testApplyToConfigBuilder() {
        this.instance.applyToConfigBuilder(this.builder);

        SchemaGeneratorConfigPart<Field> fieldConfigPart = this.builder.forFields();
        Mockito.verify(fieldConfigPart).addNullableCheck(Mockito.any());
        Mockito.verifyNoMoreInteractions(fieldConfigPart);
        Mockito.verify(this.builder, Mockito.times(2)).forFields();

        SchemaGeneratorConfigPart<Method> methodConfigPart = this.builder.forMethods();
        Mockito.verify(methodConfigPart).addNullableCheck(Mockito.any());
        Mockito.verifyNoMoreInteractions(methodConfigPart);
        Mockito.verify(this.builder, Mockito.times(2)).forMethods();

        Mockito.verify(this.builder).getObjectMapper();
        Mockito.verify(this.builder).with(Mockito.any(CustomDefinitionProvider.class));

        Mockito.verifyNoMoreInteractions(this.builder);
    }
}
