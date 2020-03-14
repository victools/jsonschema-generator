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
 * Test for the {@link MethodSCope} class.
 */
@RunWith(JUnitParamsRunner.class)
public class MethodScopeTest extends AbstractTypeAwareTest {

    public MethodScopeTest() {
        super(TestClass.class);
    }

    @Before
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    Object parametersForTestFindGetterField() {
        return new String[][]{
            {"getFieldWithPrivateGetter", null},
            {"getFieldWithPublicGetter", "fieldWithPublicGetter"},
            {"isFieldWithPublicBooleanGetter", "fieldWithPublicBooleanGetter"},
            {"getCalculatedValue", null},
            {"isBehavingSomehow", null},
            {"get", null},
            {"is", null},
            {"calculateSomething", null}};
    }

    @Test
    @Parameters
    public void testFindGetterField(String methodName, String fieldName) throws Exception {
        MethodScope method = this.getTestClassMethod(methodName);
        FieldScope field = method.findGetterField();

        if (fieldName == null) {
            Assert.assertNull(field);
        } else {
            Assert.assertNotNull(field);
            Assert.assertEquals(fieldName, field.getName());
        }
    }

    @Test
    @Parameters({
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

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    @Parameters({
        "calculateSomething, false",
        "getFieldWithPrivateGetter, true",
        "getFieldWithPublicGetter, false",
        "isFieldWithPublicBooleanGetter, true"
    })
    public void testGetAnnotationConsideringFieldAndGetter(String methodName, boolean annotationExpectedToBeFound) {
        MethodScope method = this.getTestClassMethod(methodName);
        TestAnnotation annotation = method.getAnnotationConsideringFieldAndGetter(TestAnnotation.class);

        if (annotationExpectedToBeFound) {
            Assert.assertNotNull(annotation);
        } else {
            Assert.assertNull(annotation);
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
