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
 * Test for the {@link FieldScope} class.
 */
public class FieldScopeTest extends AbstractTypeAwareTest {

    public FieldScopeTest() {
        super(TestClass.class);
    }

    @BeforeEach
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    static Stream<Arguments> parametersForTestFindGetter() {
        return Stream.of(
            Arguments.of("fieldWithoutGetter", null, null),
            Arguments.of("fieldWithoutGetter", "fieldWithPublicGetter", null),
            Arguments.of("fieldWithPrivateGetter", null, null),
            Arguments.of("fieldWithPublicGetter", null, "getFieldWithPublicGetter"),
            Arguments.of("fieldWithPublicGetter", "fieldWithoutGetter", "getFieldWithPublicGetter"),
            Arguments.of("fieldWithPublicBooleanGetter", null, "isFieldWithPublicBooleanGetter"),
            Arguments.of("isFieldWithMatchingGetter", null, "isFieldWithMatchingGetter"),
            Arguments.of("isFieldWithMatchingGetter", "fieldWithPublicGetter", "isFieldWithMatchingGetter"),
            Arguments.of("aFieldWithJavaBeansConformingGetter", null, "getaFieldWithJavaBeansConformingGetter"),
            Arguments.of("aFieldWithJavaBeansNonConformingGetter", null, "getAFieldWithJavaBeansNonConformingGetter"),
            Arguments.of("aFieldWithJavaBeansConformingBooleanGetter", null, "isaFieldWithJavaBeansConformingBooleanGetter"),
            Arguments.of("aFieldWithJavaBeansNonConformingBooleanGetter", null, "isAFieldWithJavaBeansNonConformingBooleanGetter")
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestFindGetter")
    public void testFindGetter(String fieldName, String fieldNameOverride, String methodName) throws Exception {
        FieldScope field = this.getTestClassField(fieldName)
                .withOverriddenName(fieldNameOverride);
        MethodScope getter = field.findGetter();

        if (methodName == null) {
            Assertions.assertNull(getter);
        } else {
            Assertions.assertNotNull(getter);
            Assertions.assertEquals(methodName, getter.getName());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "fieldWithoutGetter, false",
        "fieldWithPrivateGetter, false",
        "fieldWithPublicGetter, true",
        "fieldWithPublicBooleanGetter, true"
    })
    public void testHasGetter(String fieldName, boolean expectedResult) throws Exception {
        FieldScope field = this.getTestClassField(fieldName);
        boolean result = field.hasGetter();

        Assertions.assertEquals(expectedResult, result);
    }

    @ParameterizedTest
    @CsvSource({
        "fieldWithoutGetter, false",
        "fieldWithPrivateGetter, true",
        "fieldWithPublicGetter, false",
        "fieldWithPublicBooleanGetter, true"
    })
    public void testGetAnnotationConsideringFieldAndGetter(String fieldName, boolean annotationExpectedToBeFound) {
        FieldScope field = this.getTestClassField(fieldName);
        TestAnnotation annotation = field.getAnnotationConsideringFieldAndGetter(TestAnnotation.class);

        if (annotationExpectedToBeFound) {
            Assertions.assertNotNull(annotation);
        } else {
            Assertions.assertNull(annotation);
        }
    }

    private static class TestClass {

        private String fieldWithoutGetter;
        @TestAnnotation
        private int fieldWithPrivateGetter;
        private long fieldWithPublicGetter;
        private boolean fieldWithPublicBooleanGetter;
        private boolean isFieldWithMatchingGetter;
        private int aFieldWithJavaBeansConformingGetter;
        private long aFieldWithJavaBeansNonConformingGetter;
        private boolean aFieldWithJavaBeansConformingBooleanGetter;
        private boolean aFieldWithJavaBeansNonConformingBooleanGetter;

        private int getFieldWithPrivateGetter() {
            return this.fieldWithPrivateGetter;
        }

        public long getFieldWithPublicGetter() {
            return this.fieldWithPublicGetter;
        }

        @TestAnnotation
        public boolean isFieldWithPublicBooleanGetter() {
            return this.fieldWithPublicBooleanGetter;
        }

        public boolean isFieldWithMatchingGetter() {
            return this.isFieldWithMatchingGetter;
        }

        public int getaFieldWithJavaBeansConformingGetter() {
            return this.aFieldWithJavaBeansConformingGetter;
        }

        public long getAFieldWithJavaBeansNonConformingGetter() {
            return this.aFieldWithJavaBeansNonConformingGetter;
        }

        public boolean isaFieldWithJavaBeansConformingBooleanGetter() {
            return this.aFieldWithJavaBeansConformingBooleanGetter;
        }

        public boolean isAFieldWithJavaBeansNonConformingBooleanGetter() {
            return this.aFieldWithJavaBeansNonConformingBooleanGetter;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    private @interface TestAnnotation {
    }
}
