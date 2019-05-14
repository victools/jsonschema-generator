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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link TypeVariableContext} class.
 */
public class TypeVariableContextTest {

    @Test
    public void testForType_plainClass() throws Exception {
        TypeVariableContext context = TypeVariableContext.forType(StringListExtension.class, TypeVariableContext.EMPTY_SCOPE);
        Assert.assertSame(TypeVariableContext.EMPTY_SCOPE, context);
    }

    @Test
    public void testForType_parameterizedTypeWithoutGenerics() throws Exception {
        Type targetType = TestClass1.class.getDeclaredField("genericStringList").getGenericType();
        TypeVariableContext context = TypeVariableContext.forType(targetType, TypeVariableContext.EMPTY_SCOPE);
        Assert.assertSame(TypeVariableContext.EMPTY_SCOPE, context);
    }

    @Test
    public void testResolveGenericTypePlaceholder_class_class() throws Exception {
        TypeVariableContext context = TypeVariableContext.forType(TestClass1.class, TypeVariableContext.EMPTY_SCOPE);
        Class<String> typePlaceholder = String.class;
        JavaType result = context.resolveGenericTypePlaceholder(typePlaceholder);
        Assert.assertEquals(typePlaceholder, result.getResolvedType());
    }

    @Test(expected = IllegalStateException.class)
    public void testResolveGenericTypePlaceholder_invalidContext_typeVariable() throws Exception {
        Type typePlaceholder = TestClass1.class.getDeclaredField("numberField").getGenericType();
        TypeVariableContext.EMPTY_SCOPE.resolveGenericTypePlaceholder(typePlaceholder);
    }

    @Test
    public void testResolveGenericTypePlaceholder_class_typeVariableWildcard() throws Exception {
        TypeVariableContext context = TypeVariableContext.forType(TestClass1.class, TypeVariableContext.EMPTY_SCOPE);
        Type typePlaceholder = TestClass1.class.getDeclaredField("numberField").getGenericType();
        JavaType result = context.resolveGenericTypePlaceholder(typePlaceholder);
        Assert.assertEquals(Number.class, result.getResolvedType());
    }

    @Test
    public void testResolveGenericTypePlaceholder_parameterizedType_typeVariable() throws Exception {
        Type stringListBigDecimalType = StringListExtension.class.getDeclaredField("testClass1Field").getGenericType();
        TypeVariableContext context = TypeVariableContext.forType(stringListBigDecimalType, TypeVariableContext.EMPTY_SCOPE);
        Type typePlaceholder = TestClass1.class.getDeclaredField("numberField").getGenericType();
        JavaType result = context.resolveGenericTypePlaceholder(typePlaceholder);
        Assert.assertEquals(BigDecimal.class, result.getResolvedType());
    }

    @Test
    public void testResolveGenericTypePlaceholder_genericArray_typeVariableWildcard() throws Exception {
        TypeVariableContext parentContext = TypeVariableContext.forType(TestClass1.class, TypeVariableContext.EMPTY_SCOPE);
        GenericArrayType genericArray = (GenericArrayType) TestClass1.class.getDeclaredField("numberArray").getGenericType();
        TypeVariableContext context = TypeVariableContext.forType(genericArray, parentContext);
        Type genericComponentType = genericArray.getGenericComponentType();
        JavaType result = context.resolveGenericTypePlaceholder(genericComponentType);
        Assert.assertEquals(Number.class, result.getResolvedType());
    }

    private class TestClass1<S extends Number> {

        private S numberField;
        private S[] numberArray;
        private List<? extends S> numberList;
        private StringListExtension genericStringList;
    }

    private class StringListExtension extends ArrayList<String> {

        private TestClass1<BigDecimal> testClass1Field;
    }
}
