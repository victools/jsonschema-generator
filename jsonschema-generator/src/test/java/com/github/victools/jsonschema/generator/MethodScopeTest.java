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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for the {@link MethodSCope} class.
 */
public class MethodScopeTest extends AbstractTypeAwareTest {

    public MethodScopeTest() {
        super(TestClass.class);
    }

    @BeforeEach
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    static Stream<Arguments> parametersForTestFindGetterField() {
        return Stream.of(
            Arguments.of("getFieldWithPrivateGetter", null, null),
            Arguments.of("getFieldWithPrivateGetter", "getFieldWithPublicGetter", null),
            Arguments.of("getFieldWithPublicGetter", null, "fieldWithPublicGetter"),
            Arguments.of("getFieldWithPublicGetter", "getFieldWithPrivateGetter", "fieldWithPublicGetter"),
            Arguments.of("isFieldWithPublicBooleanGetter", null, "fieldWithPublicBooleanGetter"),
            Arguments.of("isFieldWithPublicBooleanGetter", "isBehavingSomehow", "fieldWithPublicBooleanGetter"),
            Arguments.of("getCalculatedValue", null, null),
            Arguments.of("isBehavingSomehow", null, null),
            Arguments.of("isBehavingSomehow", "isFieldWithPublicBooleanGetter", null),
            Arguments.of("get", null, null),
            Arguments.of("is", null, null),
            Arguments.of("calculateSomething", null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestFindGetterField")
    public void testFindGetterField(String methodName, String methodNameOverride, String fieldName) throws Exception {
        MethodScope method = this.getTestClassMethod(methodName)
                .withOverriddenName(methodNameOverride);
        FieldScope field = method.findGetterField();

        if (fieldName == null) {
            Assertions.assertNull(field);
        } else {
            Assertions.assertNotNull(field);
            Assertions.assertEquals(fieldName, field.getDeclaredName());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "getFieldWithPrivateGetter, false",
        "getFieldWithPublicGetter, true",
        "isFieldWithPublicBooleanGetter, true",
        "getCalculatedValue, false",
        "isBehavingSomehow, false",
        "get, false",
        "is, false",
        "calculateSomething, false"
    })
    public void testIsGetter(String methodName, boolean expectedResult) throws Exception {
        MethodScope method = this.getTestClassMethod(methodName);
        boolean result = method.isGetter();

        Assertions.assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @CsvSource({
        "calculateSomething, false",
        "getFieldWithPrivateGetter, true",
        "getFieldWithPublicGetter, false",
        "isFieldWithPublicBooleanGetter, true"
    })
    public void testGetAnnotationConsideringFieldAndGetter(String methodName, boolean annotationExpectedToBeFound) {
        MethodScope method = this.getTestClassMethod(methodName);
        TestAnnotation annotation = method.getAnnotationConsideringFieldAndGetter(TestAnnotation.class);

        if (annotationExpectedToBeFound) {
            Assertions.assertNotNull(annotation);
        } else {
            Assertions.assertNull(annotation);
        }
    }

    private static class TestClass {

        private int fieldWithPrivateGetter;
        private long fieldWithPublicGetter;
        @TestAnnotation
        private boolean fieldWithPublicBooleanGetter;

        @TestAnnotation
        private int getFieldWithPrivateGetter() {
            return this.fieldWithPrivateGetter;
        }

        public long getFieldWithPublicGetter() {
            return this.fieldWithPublicGetter;
        }

        public boolean isFieldWithPublicBooleanGetter() {
            return this.fieldWithPublicBooleanGetter;
        }

        public double getCalculatedValue() {
            return 42.;
        }

        public boolean isBehavingSomehow() {
            return true;
        }

        public Object get() {
            return this;
        }

        public boolean is() {
            return false;
        }

        public int calculateSomething() {
            return 42;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    private static @interface TestAnnotation {
    }
}
