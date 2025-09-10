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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Test for the {@link AdditionalPropertiesModule} class.
 */
public class AdditionalPropertiesModuleTest extends AbstractTypeAwareTest {

    private SchemaGeneratorConfigBuilder builder;
    private SchemaGeneratorGeneralConfigPart generalConfigPart;
    private SchemaGeneratorConfigPart<FieldScope> fieldConfigPart;
    private SchemaGeneratorConfigPart<MethodScope> methodConfigPart;

    public AdditionalPropertiesModuleTest() {
        super(TestMapSubType.class);
    }

    @BeforeEach
    public void setUp() {
        this.builder = Mockito.mock(SchemaGeneratorConfigBuilder.class);
        this.generalConfigPart = Mockito.spy(new SchemaGeneratorGeneralConfigPart());
        this.fieldConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        this.methodConfigPart = Mockito.spy(new SchemaGeneratorConfigPart<>());
        Mockito.when(this.builder.forTypesInGeneral()).thenReturn(this.generalConfigPart);
        Mockito.when(this.builder.forFields()).thenReturn(this.fieldConfigPart);
        Mockito.when(this.builder.forMethods()).thenReturn(this.methodConfigPart);
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    @Test
    public void testApplyToConfigBuilderForMapValuesModule() {
        AdditionalPropertiesModule.forMapValues()
                .applyToConfigBuilder(this.builder);

        Mockito.verify(this.generalConfigPart).withAdditionalPropertiesResolver(Mockito.any(ConfigFunction.class));
        Mockito.verify(this.generalConfigPart).withAdditionalPropertiesResolver(Mockito.any(BiFunction.class));
        Mockito.verify(this.fieldConfigPart).withAdditionalPropertiesResolver(Mockito.any(BiFunction.class));
        Mockito.verify(this.methodConfigPart).withAdditionalPropertiesResolver(Mockito.any(BiFunction.class));
        Mockito.verifyNoMoreInteractions(this.generalConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    @Test
    public void testApplyToConfigBuilderForForbiddenForAllObjectsModule() {
        AdditionalPropertiesModule.forbiddenForAllObjectsButContainers()
                .applyToConfigBuilder(this.builder);

        Mockito.verify(this.generalConfigPart).withAdditionalPropertiesResolver(Mockito.any(ConfigFunction.class));
        Mockito.verify(this.generalConfigPart).withAdditionalPropertiesResolver(Mockito.any(BiFunction.class));
        Mockito.verifyNoMoreInteractions(this.generalConfigPart, this.fieldConfigPart, this.methodConfigPart);
    }

    static Stream<Arguments> parametersForTestResolveAdditionalProperties() {
        AdditionalPropertiesModule mapValuesInstance = AdditionalPropertiesModule.forMapValues();
        AdditionalPropertiesModule noneButContainersInstance = AdditionalPropertiesModule.forbiddenForAllObjectsButContainers();
        return Stream.of(
            Arguments.of(mapValuesInstance, null, String.class, new Type[0]),
            Arguments.of(noneButContainersInstance, Void.class, String.class, new Type[0]),
            Arguments.of(mapValuesInstance, Integer.class, Map.class, new Type[]{String.class, Integer.class}),
            Arguments.of(noneButContainersInstance, Void.class, Map.class, new Type[]{String.class, Integer.class}),
            Arguments.of(mapValuesInstance, String.class, TestMapSubType.class, new Type[0]),
            Arguments.of(noneButContainersInstance, Void.class, TestMapSubType.class, new Type[0]),
            Arguments.of(mapValuesInstance, null, List.class, new Type[]{String.class}),
            Arguments.of(noneButContainersInstance, null, List.class, new Type[]{String.class})
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestResolveAdditionalProperties")
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
            Assertions.assertEquals(expectedAdditionalProperties, result);
        } else {
            Assertions.assertEquals(typeContext.resolve(expectedAdditionalProperties), result);
        }
    }

    @Test
    public void testAdditionalPropertyWithSubtype() throws JSONException, IOException {
        // arrange
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
        configBuilder.with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES, Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES)
                .without(Option.SCHEMA_VERSION_INDICATOR)
                .forTypesInGeneral()
                .withSubtypeResolver((declaredType, context) -> declaredType.getErasedType() == B.class
                        ? Collections.singletonList(context.getTypeContext().resolve(BImpl.class))
                        : null);
        configBuilder.forFields()
                .withDescriptionResolver(field -> field.isFakeContainerItemScope()
                        ? Optional.ofNullable(field.getContainerItemAnnotation(ParamInfo.class)).map(ParamInfo::value).orElse(null)
                        : null);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        // act
        GeneratedSchema[] result = generator.generateSchema(A.class);

        // assert
        JsonNode schema = result[0].getSchema();
        TestUtils.assertGeneratedSchema(schema, AdditionalPropertiesModuleTest.class, "additional-property-with-subtype.json");
    }

    private static class TestMapSubType extends HashMap<Object, String> {
        // no further fields
    }

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    private @interface ParamInfo {

        String value();
    }

    private static class A {

        private Map<String, @ParamInfo("annotated super value") B> map;

        Map<String, B> getMap() {
            return this.map;
        }
    }

    private interface B {

        String getName();
    }

    private static class BImpl implements B {

        private String name;

        @Override
        public String getName() {
            return this.name;
        }
    }
}
