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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the {@link FieldScope} class.
 */
@RunWith(JUnitParamsRunner.class)
public class FieldScopeTest extends AbstractTypeAwareTest {

    public FieldScopeTest() {
        super(TestClass.class);
    }

    @Before
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    Object parametersForTestFindGetter() {
        return new String[][]{
            {"fieldWithoutGetter", null},
            {"fieldWithPrivateGetter", null},
            {"fieldWithPublicGetter", "getFieldWithPublicGetter"},
            {"fieldWithPublicBooleanGetter", "isFieldWithPublicBooleanGetter"}};
    }

    @Test
    @Parameters
    public void testFindGetter(String fieldName, String methodName) throws Exception {
        FieldScope field = this.getTestClassField(fieldName);
        MethodScope getter = field.findGetter();

        if (methodName == null) {
            Assert.assertNull(getter);
        } else {
            Assert.assertNotNull(getter);
            Assert.assertEquals(methodName, getter.getName());
        }
    }

    @Test
    @Parameters({
        "fieldWithoutGetter, false",
        "fieldWithPrivateGetter, false",
        "fieldWithPublicGetter, true",
        "fieldWithPublicBooleanGetter, true"
    })
    public void testHasGetter(String fieldName, boolean expectedResult) throws Exception {
        FieldScope field = this.getTestClassField(fieldName);
        boolean result = field.hasGetter();

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    @Parameters({
        "fieldWithoutGetter, false",
        "fieldWithPrivateGetter, true",
        "fieldWithPublicGetter, false",
        "fieldWithPublicBooleanGetter, true"
    })
    public void testGetAnnotationConsideringFieldAndGetter(String fieldName, boolean annotationExpectedToBeFound) {
        FieldScope field = this.getTestClassField(fieldName);
        TestAnnotation annotation = field.getAnnotationConsideringFieldAndGetter(TestAnnotation.class);

        if (annotationExpectedToBeFound) {
            Assert.assertNotNull(annotation);
        } else {
            Assert.assertNull(annotation);
        }
    }

    private static class TestClass {

        private String fieldWithoutGetter;
        @TestAnnotation
        private int fieldWithPrivateGetter;
        private long fieldWithPublicGetter;
        private boolean fieldWithPublicBooleanGetter;

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
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    private static @interface TestAnnotation {
    }
}
