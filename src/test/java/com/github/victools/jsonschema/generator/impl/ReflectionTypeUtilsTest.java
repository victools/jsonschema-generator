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

package com.github.victools.jsonschema.generator.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the {@link ReflectionTypeUtils} class.
 */
@RunWith(JUnitParamsRunner.class)
public class ReflectionTypeUtilsTest {

    Object parametersForTestGetRawType() {
        return new Object[][]{
            {"primitiveIntField", int.class},
            {"primitiveLongArrayField", long[].class},
            {"stringField", String.class},
            {"stringArrayField", String[].class},
            {"stringListField", List.class},
            {"optionalIntegerField", Optional.class},
            {"wildcardNumberSetField", Set.class},
            {"unboundedVariableCollectionField", Collection.class},
            {"variableNumberArrayField", null}
        };
    }

    @Test
    @Parameters
    public void testGetRawType(String fieldName, Class<?> expectedResult) throws Exception {
        Type fieldType = TestClass.class.getDeclaredField(fieldName).getGenericType();
        Class<?> result = ReflectionTypeUtils.getRawType(fieldType);

        Assert.assertSame(expectedResult, result);
    }

    @Test(expected = UnsupportedOperationException.class)
    @Parameters({
        "wildcardNumberSetField",
        "unboundedVariableCollectionField"
    })
    public void testGetRawType_Generic(String fieldName) throws Exception {
        Type fieldType = TestClass.class.getDeclaredField(fieldName).getGenericType();
        ReflectionTypeUtils.getRawType(((ParameterizedType) fieldType).getActualTypeArguments()[0]);
    }

    @Test
    @Parameters({
        "primitiveIntField, false",
        "primitiveLongArrayField, true",
        "stringField, false",
        "stringArrayField, true",
        "stringListField, true",
        "optionalIntegerField, false",
        "wildcardNumberSetField, true",
        "unboundedVariableCollectionField, true",
        "variableNumberArrayField, true"
    })
    public void testIsArrayType(String fieldName, boolean expectedResult) throws Exception {
        Type fieldType = TestClass.class.getDeclaredField(fieldName).getGenericType();
        boolean result = ReflectionTypeUtils.isArrayType(fieldType);

        Assert.assertSame(expectedResult, result);
    }

    Object parametersForTestGetArrayComponentType_ClassResult() {
        return new Object[][]{
            {"primitiveLongArrayField", long.class},
            {"stringArrayField", String.class},
            {"stringListField", String.class},
            {"wildcardNumberSetField", Number.class},
            {"unboundedVariableCollectionField", Object.class},
            {"variableNumberArrayField", Number.class}
        };
    }

    @Test
    @Parameters
    public void testGetArrayComponentType_ClassResult(String fieldName, Class<?> expectedResult) throws Exception {
        Type fieldType = TestClass.class.getDeclaredField(fieldName).getGenericType();
        TypeVariableContext typeVariables = TypeVariableContext.forType(TestClass.class, TypeVariableContext.EMPTY_SCOPE);
        Type result = ReflectionTypeUtils.getArrayComponentType(fieldType, typeVariables);

        Assert.assertSame(expectedResult, result);
    }

    @Test(expected = UnsupportedOperationException.class)
    @Parameters({
        "primitiveIntField",
        "stringField",
        "optionalIntegerField"
    })
    public void testGetArrayComponentType_notAnArray(String fieldName) throws Exception {
        Type fieldType = TestClass.class.getDeclaredField(fieldName).getGenericType();
        ReflectionTypeUtils.getArrayComponentType(fieldType, TypeVariableContext.EMPTY_SCOPE);
    }

    private static class TestClass<S, T extends Number> {

        public int primitiveIntField;
        public long[] primitiveLongArrayField;
        public String stringField;
        public String[] stringArrayField;
        public List<String> stringListField;
        public Optional<Integer> optionalIntegerField;
        public Set<? extends Number> wildcardNumberSetField;
        public Collection<S> unboundedVariableCollectionField;
        public T[] variableNumberArrayField;
    }
}
