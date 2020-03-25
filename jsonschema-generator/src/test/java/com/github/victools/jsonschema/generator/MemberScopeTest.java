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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the {@link MemberScope} class.
 */
@RunWith(JUnitParamsRunner.class)
public class MemberScopeTest extends AbstractTypeAwareTest {

    public MemberScopeTest() {
        super(TestClass.class);
    }

    @Before
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
    }

    @Test
    @Parameters({
        "privateVisibleField, true, false, false",
        "packageVisibleField, false, false, false",
        "protectedVisibleField, false, true, false",
        "publicVisibleField, false, false, true"
    })
    public void testGetVisibility(String fieldName, boolean isPrivate, boolean isProtected, boolean isPublic) {
        FieldScope field = this.getTestClassField(fieldName);

        Assert.assertEquals(isPrivate, field.isPrivate());
        Assert.assertEquals(isProtected, field.isProtected());
        Assert.assertEquals(isPublic, field.isPublic());
    }

    Object parametersForTestContainerType() {
        return new Object[][]{
            {"getStringArray", true, String.class},
            {"getRawCollection", true, Object.class},
            {"getListOfIntArrays", true, int[].class},
            {"getMapWithNestedGenerics", false, null},
            {"getBooleanField", false, null},
            {"executeVoidMethod", false, null}};
    }

    @Test
    @Parameters
    public void testContainerType(String methodName, boolean isContainerType, Class<?> expectedItemType) throws Exception {
        MethodScope method = this.getTestClassMethod(methodName);

        Assert.assertEquals(isContainerType, method.isContainerType());
        ResolvedType itemType = method.getContainerItemType();
        if (expectedItemType == null) {
            Assert.assertNull(itemType);
        } else {
            Assert.assertNotNull(itemType);
            Assert.assertSame(expectedItemType, itemType.getErasedType());
        }
    }

    Object parametersForTestTypeDescription() {
        return new Object[][]{
            {"getStringArray", "String[]", "java.lang.String[]"},
            {"getRawCollection", "Collection", "java.util.Collection"},
            {"getListOfIntArrays", "List<int[]>", "java.util.List<int[]>"},
            {"getMapWithNestedGenerics", "Map<String, Map<String, List<Set<Class<Object>>>>>",
                "java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.util.List<java.util.Set<java.lang.Class<java.lang.Object>>>>>"},
            {"getBooleanField", "Boolean", "java.lang.Boolean"},
            {"executeVoidMethod", "void", "void"}};
    }

    @Test
    @Parameters
    public void testTypeDescription(String methodName, String simpleTypeDescription, String fullTypeDescription) throws Exception {
        MethodScope method = this.getTestClassMethod(methodName);

        Assert.assertEquals(simpleTypeDescription, method.getSimpleTypeDescription());
        Assert.assertEquals(fullTypeDescription, method.getFullTypeDescription());
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
