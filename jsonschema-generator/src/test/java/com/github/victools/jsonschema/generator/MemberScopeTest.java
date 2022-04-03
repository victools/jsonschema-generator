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

package com.github.victools.jsonschema.generator;

import com.fasterxml.classmate.ResolvedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for the {@link MemberScope} class.
 */
public class MemberScopeTest extends AbstractTypeAwareTest {

    public MemberScopeTest() {
        super(TestClass.class);
    }

    @BeforeEach
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    @ParameterizedTest
    @CsvSource({
        "privateVisibleField, true, false, false",
        "packageVisibleField, false, false, false",
        "protectedVisibleField, false, true, false",
        "publicVisibleField, false, false, true"
    })
    public void testGetVisibility(String fieldName, boolean isPrivate, boolean isProtected, boolean isPublic) {
        FieldScope field = this.getTestClassField(fieldName);

        Assertions.assertEquals(isPrivate, field.isPrivate());
        Assertions.assertEquals(isProtected, field.isProtected());
        Assertions.assertEquals(isPublic, field.isPublic());
    }

    static Stream<Arguments> parametersForTestContainerType() {
        return Stream.of(
            Arguments.of("getStringArray", true, String.class),
            Arguments.of("getRawCollection", true, Object.class),
            Arguments.of("getListOfIntArrays", true, int[].class),
            Arguments.of("getMapWithNestedGenerics", false, null),
            Arguments.of("getBooleanField", false, null),
            Arguments.of("executeVoidMethod", false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestContainerType")
    public void testContainerType(String methodName, boolean isContainerType, Class<?> expectedItemType) throws Exception {
        MethodScope method = this.getTestClassMethod(methodName);

        Assertions.assertEquals(isContainerType, method.isContainerType());
        ResolvedType itemType = method.getContainerItemType();
        if (expectedItemType == null) {
            Assertions.assertNull(itemType);
        } else {
            Assertions.assertNotNull(itemType);
            Assertions.assertSame(expectedItemType, itemType.getErasedType());
        }
    }

    static Stream<Arguments> parametersForTestTypeDescription() {
        return Stream.of(
            Arguments.of("getStringArray", "String[]", "java.lang.String[]"),
            Arguments.of("getRawCollection", "Collection", "java.util.Collection"),
            Arguments.of("getListOfIntArrays", "List<int[]>", "java.util.List<int[]>"),
            Arguments.of("getMapWithNestedGenerics", "Map<String, Map<String, List<Set<Class<Object>>>>>",
                "java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<java.util.Set<java.lang.Class<java.lang.Object>>>>>"),
            Arguments.of("getBooleanField", "Boolean", "java.lang.Boolean"),
            Arguments.of("executeVoidMethod", "void", "void")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestTypeDescription")
    public void testTypeDescription(String methodName, String simpleTypeDescription, String fullTypeDescription) throws Exception {
        MethodScope method = this.getTestClassMethod(methodName);

        Assertions.assertEquals(simpleTypeDescription, method.getSimpleTypeDescription());
        Assertions.assertEquals(fullTypeDescription, method.getFullTypeDescription());
    }

    private static class TestClass {

        private Object privateVisibleField;
        Object packageVisibleField;
        protected Object protectedVisibleField;
        public Object publicVisibleField;

        public String[] getStringArray() {
            return null;
        }

        public Collection getRawCollection() {
            return null;
        }

        public List<int[]> getListOfIntArrays() {
            return null;
        }

        public Map<String, Map<String, List<Set<Class<?>>>>> getMapWithNestedGenerics() {
            return null;
        }

        public Boolean getBooleanField() {
            return null;
        }

        public void executeVoidMethod() {
            // nothing to do
        }
    }
}
