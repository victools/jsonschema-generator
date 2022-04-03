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
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for the {@link JsonSubTypesResolver}.
 */
public class JsonSubTypesResolverLookUpTest extends AbstractTypeAwareTest {

    private final JsonSubTypesResolver instance = new JsonSubTypesResolver();

    public JsonSubTypesResolverLookUpTest() {
        super(TestSuperClassWithNameProperty.class);
    }

    private void assertErasedSubtypesAreEquals(List<Class<?>> erasedSubtypes, List<ResolvedType> subtypes) {
        if (erasedSubtypes == null) {
            Assertions.assertNull(subtypes);
        } else {
            Assertions.assertNotNull(subtypes);
            Assertions.assertEquals(erasedSubtypes.size(), subtypes.size());
            for (int index = 0; index < erasedSubtypes.size(); index++) {
                Assertions.assertSame(erasedSubtypes.get(index), subtypes.get(index).getErasedType());
            }
        }
    }

    static Stream<Arguments> parametersForTestFindSubtypes() {
        return Stream.of(
            Arguments.of(JsonSubTypesResolverLookUpTest.class, null),
            Arguments.of(TestSuperClassWithNameProperty.class, Arrays.asList(TestSubClass1.class, TestSubClass2.class, TestSubClass3.class)),
            Arguments.of(TestInterfaceWithTypeInfo.class, Arrays.asList(TestSubClass4.class, TestSubClass5.class))
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestFindSubtypes")
    public void testFindSubtypes(Class<?> targetType, List<Class<?>> erasedSubtypes) {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
        TypeContext typeContext = this.getContext().getTypeContext();
        List<ResolvedType> subtypes = this.instance.findSubtypes(typeContext.resolve(targetType), this.getContext());
        this.assertErasedSubtypesAreEquals(erasedSubtypes, subtypes);
    }

    static Stream<Arguments> parametersForTestFindTargetTypeOverridesForField() {
        return Stream.of(
            Arguments.of("supertypeNoAnnotation", null),
            Arguments.of("supertypeWithAnnotationOnField", Arrays.asList(TestSubClass1.class, TestSubClass2.class)),
            Arguments.of("supertypeWithAnnotationOnGetter", Arrays.asList(TestSubClass2.class, TestSubClass3.class)),
            Arguments.of("supertypeWithAnnotationOnFieldAndGetter", Arrays.asList(TestSubClass1.class, TestSubClass2.class))
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestFindTargetTypeOverridesForField")
    public void testFindTargetTypeOverridesForField(String fieldName, List<Class<?>> erasedSubtypes) {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
        List<ResolvedType> subtypes = this.instance.findTargetTypeOverrides(this.getTestClassField(fieldName));
        this.assertErasedSubtypesAreEquals(erasedSubtypes, subtypes);
    }

    static Stream<Arguments> parametersForTestFindTargetTypeOverridesForMethod() {
        return Stream.of(
            Arguments.of("getSupertypeNoAnnotation", null),
            Arguments.of("getSupertypeWithAnnotationOnField", Arrays.asList(TestSubClass1.class, TestSubClass2.class)),
            Arguments.of("getSupertypeWithAnnotationOnGetter", Arrays.asList(TestSubClass2.class, TestSubClass3.class)),
            Arguments.of("getSupertypeWithAnnotationOnFieldAndGetter", Arrays.asList(TestSubClass2.class, TestSubClass3.class))
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestFindTargetTypeOverridesForMethod")
    public void testFindTargetTypeOverridesForMethod(String methodName, List<Class<?>> erasedSubtypes) {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
        List<ResolvedType> subtypes = this.instance.findTargetTypeOverrides(this.getTestClassMethod(methodName));
        this.assertErasedSubtypesAreEquals(erasedSubtypes, subtypes);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
        @JsonSubTypes.Type(TestSubClass1.class),
        @JsonSubTypes.Type(TestSubClass2.class),
        @JsonSubTypes.Type(TestSubClass3.class)
    })
    private static class TestSuperClassWithNameProperty {

        public TestSuperClassWithNameProperty supertypeNoAnnotation;
        @JsonSubTypes({
            @JsonSubTypes.Type(TestSubClass1.class),
            @JsonSubTypes.Type(TestSubClass2.class)
        })
        public TestSuperClassWithNameProperty supertypeWithAnnotationOnField;
        public TestSuperClassWithNameProperty supertypeWithAnnotationOnGetter;
        @JsonSubTypes({
            @JsonSubTypes.Type(TestSubClass1.class),
            @JsonSubTypes.Type(TestSubClass2.class)
        })
        public TestSuperClassWithNameProperty supertypeWithAnnotationOnFieldAndGetter;

        public TestSuperClassWithNameProperty getSupertypeNoAnnotation() {
            return this.supertypeNoAnnotation;
        }

        public TestSuperClassWithNameProperty getSupertypeWithAnnotationOnField() {
            return this.supertypeWithAnnotationOnField;
        }

        @JsonSubTypes({
            @JsonSubTypes.Type(TestSubClass2.class),
            @JsonSubTypes.Type(TestSubClass3.class)
        })
        public TestSuperClassWithNameProperty getSupertypeWithAnnotationOnGetter() {
            return this.supertypeWithAnnotationOnGetter;
        }

        @JsonSubTypes({
            @JsonSubTypes.Type(TestSubClass2.class),
            @JsonSubTypes.Type(TestSubClass3.class)
        })
        public TestSuperClassWithNameProperty getSupertypeWithAnnotationOnFieldAndGetter() {
            return this.supertypeWithAnnotationOnFieldAndGetter;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
        @JsonSubTypes.Type(TestSubClass4.class),
        @JsonSubTypes.Type(TestSubClass5.class)
    })
    private interface TestInterfaceWithTypeInfo {
    }

    @JsonTypeName("SUB_CLASS_1")
    private static class TestSubClass1 extends TestSuperClassWithNameProperty {
    }

    @JsonTypeName("SUB_CLASS_2")
    private static class TestSubClass2 extends TestSuperClassWithNameProperty {
    }

    @JsonTypeName("SUB_CLASS_3")
    private static class TestSubClass3 extends TestSuperClassWithNameProperty {
    }

    @JsonTypeName("SUB_CLASS_4")
    private static class TestSubClass4 implements TestInterfaceWithTypeInfo {
    }

    @JsonTypeName("SUB_CLASS_5")
    private static class TestSubClass5 implements TestInterfaceWithTypeInfo {
    }
}
