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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for the {@link JsonSubTypesResolver}.
 */
public class JsonSubTypesResolverCustomDefinitionsTest extends AbstractTypeAwareTest {

    private final JsonSubTypesResolver instance = new JsonSubTypesResolver();

    public JsonSubTypesResolverCustomDefinitionsTest() {
        super(TestClassWithSuperTypeReferences.class);
    }

    @BeforeEach
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
        Answer<String> memberTitleLookUp = invocation -> ((MemberScope<?, ?>) invocation.getArgument(0))
                .getType().getErasedType() == TestSubClass1.class ? "property attribute" : null;
        Mockito.when(this.getContext().getGeneratorConfig().resolveTitle(Mockito.any(FieldScope.class))).thenAnswer(memberTitleLookUp);
        Mockito.when(this.getContext().getGeneratorConfig().resolveTitle(Mockito.any(MethodScope.class))).thenAnswer(memberTitleLookUp);
    }

    private void assertCustomDefinitionsAreEqual(String customDefinition, CustomDefinition result, CustomDefinition.DefinitionType definitionType)
            throws Exception {
        if (customDefinition == null) {
            Assertions.assertNull(result);
        } else {
            Assertions.assertNotNull(result);
            Assertions.assertNotNull(result.getValue());
            JSONAssert.assertEquals('\n' + result.getValue().toString() + '\n',
                    customDefinition, result.getValue().toString(), JSONCompareMode.STRICT);
            Assertions.assertEquals(definitionType, result.getDefinitionType());
            Assertions.assertEquals(CustomDefinition.AttributeInclusion.NO, result.getAttributeInclusion());
        }
    }

    static Stream<Arguments> parametersForTestProvideCustomSchemaDefinition() {
        return Stream.of(
            Arguments.of(TestClassWithSuperTypeReferences.class, null),
            Arguments.of(TestSuperClassWithNameProperty.class, null),
            Arguments.of(TestSubClass1.class, "{\"allOf\":[{},"
                + "{\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_1\"}},\"required\":[\"@type\"]}]}")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestProvideCustomSchemaDefinition")
    public void testProvideCustomSchemaDefinition(Class<?> erasedTargetType, String customDefinition) throws Exception {
        ResolvedType javaType = this.getContext().getTypeContext().resolve(erasedTargetType);
        CustomDefinition result = this.instance.provideCustomSchemaDefinition(javaType, this.getContext());
        this.assertCustomDefinitionsAreEqual(customDefinition, result, CustomDefinition.DefinitionType.STANDARD);
    }

    static Stream<Arguments> parametersForTestProvideCustomPropertySchemaDefinitionForField() {
        return Stream.of(
            Arguments.of("superTypeNoAnnotation", null, null),
            Arguments.of("superTypeNoAnnotation", TestSubClass1.class, null),
            Arguments.of("superTypeNoAnnotation", TestSubClass2.class, null),
            Arguments.of("superTypeWithAnnotationOnField", null, null),
            Arguments.of("superTypeWithAnnotationOnField", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"}},"
                + "\"required\":[\"fullClass\"]}]}"),
            Arguments.of("superTypeWithAnnotationOnField", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"}},"
                + "\"required\":[\"fullClass\"]}]}"),
            Arguments.of("superTypeWithAnnotationOnGetter", null, null),
            Arguments.of("superTypeWithAnnotationOnGetter", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\","
                + "\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_1\"}},\"required\":[\"@type\"]}]}"),
            Arguments.of("superTypeWithAnnotationOnGetter", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_2\"}},\"required\":[\"@type\"]}]}"),
            Arguments.of("superTypeWithAnnotationOnFieldAndGetter", null, null),
            Arguments.of("superTypeWithAnnotationOnFieldAndGetter", TestSubClass1.class,
                "{\"type\":\"array\",\"items\":[{\"type\":\"string\",\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"},"
                + "{\"allOf\":[{},{\"title\":\"property attribute\"}]}]}"),
            Arguments.of("superTypeWithAnnotationOnFieldAndGetter", TestSubClass2.class,
                "{\"type\":\"array\",\"items\":[{\"type\":\"string\",\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"},{}]}"),
            Arguments.of("superInterfaceWithAnnotationOnField", null, null),
            Arguments.of("superInterfaceWithAnnotationOnField", TestSubClass3.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass3\"}},"
                + "\"required\":[\"fullClass\"]}]}"),
            Arguments.of("superInterfaceWithAnnotationOnField", TestSubClass4.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass4\"}},"
                + "\"required\":[\"fullClass\"]}]}")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestProvideCustomPropertySchemaDefinitionForField")
    public void testProvideCustomPropertySchemaDefinitionForField(String fieldName, Class<?> typeOverride, String customDefinition)
            throws Exception {
        FieldScope field = this.getTestClassField(fieldName);
        if (typeOverride != null) {
            field = field.withOverriddenType(this.getContext().getTypeContext().resolve(typeOverride));
        }
        CustomDefinition result = this.instance.provideCustomPropertySchemaDefinition(field, this.getContext());
        this.assertCustomDefinitionsAreEqual(customDefinition, result, CustomDefinition.DefinitionType.INLINE);
    }

    static Stream<Arguments> parametersForTestProvideCustomPropertySchemaDefinitionForMethod() {
        return Stream.of(
            Arguments.of("getSuperTypeNoAnnotation", null, null),
            Arguments.of("getSuperTypeNoAnnotation", TestSubClass1.class, null),
            Arguments.of("getSuperTypeNoAnnotation", TestSubClass2.class, null),
            Arguments.of("getSuperTypeWithAnnotationOnField", null, null),
            Arguments.of("getSuperTypeWithAnnotationOnField", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"}},"
                + "\"required\":[\"fullClass\"]}]}"),
            Arguments.of("getSuperTypeWithAnnotationOnField", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"}},"
                + "\"required\":[\"fullClass\"]}]}"),
            Arguments.of("getSuperTypeWithAnnotationOnGetter", null, null),
            Arguments.of("getSuperTypeWithAnnotationOnGetter", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\","
                + "\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_1\"}},\"required\":[\"@type\"]}]}"),
            Arguments.of("getSuperTypeWithAnnotationOnGetter", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_2\"}},\"required\":[\"@type\"]}]}"),
            Arguments.of("getSuperTypeWithAnnotationOnFieldAndGetter", null, null),
            Arguments.of("getSuperTypeWithAnnotationOnFieldAndGetter", TestSubClass1.class,
                "{\"type\":\"object\",\"properties\":{\"SUB_CLASS_1\":{\"allOf\":[{},{\"title\":\"property attribute\"}]}},"
                + "\"required\":[\"SUB_CLASS_1\"]}"),
            Arguments.of("getSuperTypeWithAnnotationOnFieldAndGetter", TestSubClass2.class,
                "{\"type\":\"object\",\"properties\":{\"SUB_CLASS_2\":{}},\"required\":[\"SUB_CLASS_2\"]}")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestProvideCustomPropertySchemaDefinitionForMethod")
    public void testProvideCustomPropertySchemaDefinitionForMethod(String methodName, Class<?> typeOverride, String customDefinition)
            throws Exception {
        MethodScope method = this.getTestClassMethod(methodName);
        if (typeOverride != null) {
            method = method.withOverriddenType(this.getContext().getTypeContext().resolve(typeOverride));
        }
        CustomDefinition result = this.instance.provideCustomPropertySchemaDefinition(method, this.getContext());
        this.assertCustomDefinitionsAreEqual(customDefinition, result, CustomDefinition.DefinitionType.INLINE);
    }

    private static class TestClassWithSuperTypeReferences {

        public TestSuperClassWithNameProperty superTypeNoAnnotation;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "fullClass", include = JsonTypeInfo.As.EXISTING_PROPERTY)
        public TestSuperClassWithNameProperty superTypeWithAnnotationOnField;
        public TestSuperClassWithNameProperty superTypeWithAnnotationOnGetter;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public TestSuperClassWithNameProperty superTypeWithAnnotationOnFieldAndGetter;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "fullClass", include = JsonTypeInfo.As.PROPERTY)
        public TestSuperInterface superInterfaceWithAnnotationOnField;

        public TestSuperClassWithNameProperty getSuperTypeNoAnnotation() {
            return this.superTypeNoAnnotation;
        }

        public TestSuperClassWithNameProperty getSuperTypeWithAnnotationOnField() {
            return this.superTypeWithAnnotationOnField;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
        public TestSuperClassWithNameProperty getSuperTypeWithAnnotationOnGetter() {
            return this.superTypeWithAnnotationOnGetter;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
        public TestSuperClassWithNameProperty getSuperTypeWithAnnotationOnFieldAndGetter() {
            return this.superTypeWithAnnotationOnFieldAndGetter;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(TestSubClass1.class),
        @JsonSubTypes.Type(TestSubClass2.class)
    })
    private static class TestSuperClassWithNameProperty {

        public String fullClass;
    }

    @JsonTypeName("SUB_CLASS_1")
    private static class TestSubClass1 extends TestSuperClassWithNameProperty {
    }

    @JsonTypeName("SUB_CLASS_2")
    private static class TestSubClass2 extends TestSuperClassWithNameProperty {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(TestSubClass3.class),
        @JsonSubTypes.Type(TestSubClass4.class)
    })
    private interface TestSuperInterface {
    }

    @JsonTypeName("SUB_CLASS_3")
    private static class TestSubClass3 implements TestSuperInterface {
    }

    @JsonTypeName("SUB_CLASS_4")
    private static class TestSubClass4 implements TestSuperInterface {
    }
}
