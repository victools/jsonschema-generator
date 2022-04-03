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

package com.github.victools.jsonschema.module.swagger2;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.InstanceAttributeOverrideV2;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.SchemaGeneratorGeneralConfigPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

/**
 * Test for the {@link SwaggerModule} class.
 */
public class Swagger2ModuleTest {

    @Mock
    private SchemaGeneratorConfigBuilder configBuilder;
    @Spy
    private SchemaGeneratorGeneralConfigPart typesInGeneralConfigPart;
    @Spy
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    @Spy
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    private AutoCloseable mockProvider;

    @BeforeEach
    public void setUp() {
        this.mockProvider = MockitoAnnotations.openMocks(this);
        Mockito.when(this.configBuilder.forTypesInGeneral()).thenReturn(this.typesInGeneralConfigPart);
        Mockito.when(this.configBuilder.forFields()).thenReturn(this.fieldConfigPart);
        Mockito.when(this.configBuilder.forMethods()).thenReturn(this.methodConfigPart);
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.mockProvider.close();
    }

    @Test
    public void testApplyToConfigBuilder() {
        new Swagger2Module().applyToConfigBuilder(this.configBuilder);

        Mockito.verify(this.configBuilder).forTypesInGeneral();
        Mockito.verify(this.configBuilder).forFields();
        Mockito.verify(this.configBuilder).forMethods();

        Mockito.verify(this.typesInGeneralConfigPart).withDescriptionResolver(Mockito.any());
        Mockito.verify(this.typesInGeneralConfigPart).withTitleResolver(Mockito.any());
        Mockito.verify(this.typesInGeneralConfigPart).withSubtypeResolver(Mockito.any(Swagger2SubtypeResolver.class));
        Mockito.verify(this.typesInGeneralConfigPart).getDefinitionNamingStrategy();
        Mockito.verify(this.typesInGeneralConfigPart).withDefinitionNamingStrategy(Mockito.any(Swagger2SchemaDefinitionNamingStrategy.class));

        this.verifyCommonMemberConfigurations(this.fieldConfigPart);
        this.verifyCommonMemberConfigurations(this.methodConfigPart);

        Mockito.verifyNoMoreInteractions(this.configBuilder, this.typesInGeneralConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    private void verifyCommonMemberConfigurations(SchemaGeneratorConfigPart<?> configPart) {
        Mockito.verify(configPart).withTargetTypeOverridesResolver(Mockito.any());
        Mockito.verify(configPart).withIgnoreCheck(Mockito.any());
        Mockito.verify(configPart).withPropertyNameOverrideResolver(Mockito.any());
        Mockito.verify(configPart).withCustomDefinitionProvider(Mockito.any());

        Mockito.verify(configPart).withDescriptionResolver(Mockito.any());
        Mockito.verify(configPart).withTitleResolver(Mockito.any());
        Mockito.verify(configPart).withRequiredCheck(Mockito.any());
        Mockito.verify(configPart).withNullableCheck(Mockito.any());
        Mockito.verify(configPart).withReadOnlyCheck(Mockito.any());
        Mockito.verify(configPart).withWriteOnlyCheck(Mockito.any());
        Mockito.verify(configPart).withEnumResolver(Mockito.any());
        Mockito.verify(configPart).withDefaultResolver(Mockito.any());

        Mockito.verify(configPart).withStringMinLengthResolver(Mockito.any());
        Mockito.verify(configPart).withStringMaxLengthResolver(Mockito.any());
        Mockito.verify(configPart).withStringFormatResolver(Mockito.any());
        Mockito.verify(configPart).withStringPatternResolver(Mockito.any());

        Mockito.verify(configPart).withNumberMultipleOfResolver(Mockito.any());
        Mockito.verify(configPart).withNumberInclusiveMinimumResolver(Mockito.any());
        Mockito.verify(configPart).withNumberExclusiveMinimumResolver(Mockito.any());
        Mockito.verify(configPart).withNumberInclusiveMaximumResolver(Mockito.any());
        Mockito.verify(configPart).withNumberExclusiveMaximumResolver(Mockito.any());

        Mockito.verify(configPart).withArrayMinItemsResolver(Mockito.any());
        Mockito.verify(configPart).withArrayMaxItemsResolver(Mockito.any());
        Mockito.verify(configPart).withArrayUniqueItemsResolver(Mockito.any());

        Mockito.verify(configPart).withInstanceAttributeOverride(Mockito.any(InstanceAttributeOverrideV2.class));
    }
}
