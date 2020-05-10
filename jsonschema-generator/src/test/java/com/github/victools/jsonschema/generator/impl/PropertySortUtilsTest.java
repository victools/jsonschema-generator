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

package com.github.victools.jsonschema.generator.impl;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Test for the {@link PropertySortUtils} class.
 */
@RunWith(JUnitParamsRunner.class)
public class PropertySortUtilsTest {

    private <S extends MemberScope<?, ?>> S createMemberMock(Class<S> scopeType, boolean isStatic, String name) {
        S mock = Mockito.mock(scopeType);
        Mockito.when(mock.isStatic()).thenReturn(isStatic);
        Mockito.when(mock.getSchemaPropertyName()).thenReturn(name);
        return mock;
    }

    public Object[] parametersForTestSortProperties() {
        Comparator<MemberScope<?, ?>> noSorting = (_first, _second) -> 0;
        return new Object[][]{
            {"unsorted", "c f() e a b() d()", noSorting},
            {"fields-before-methods", "c e a f() b() d()", PropertySortUtils.SORT_PROPERTIES_FIELDS_BEFORE_METHODS},
            {"alphabetically-by-name", "a b() c d() e f()", PropertySortUtils.SORT_PROPERTIES_BY_NAME_ALPHABETICALLY},
            {"default-order", "a c e b() d() f()", PropertySortUtils.DEFAULT_PROPERTY_ORDER}
        };
    }

    @Test
    @Parameters
    @TestCaseName(value = "{method}({0}) [{index}]")
    public void testSortProperties(String _testCaseName, String expectedResult, Comparator<MemberScope<?, ?>> sortingLogic) {
        Stream<MemberScope<?, ?>> properties = Stream.of(
                this.createMemberMock(FieldScope.class, false, "c"),
                this.createMemberMock(MethodScope.class, true, "f()"),
                this.createMemberMock(FieldScope.class, true, "e"),
                this.createMemberMock(FieldScope.class, false, "a"),
                this.createMemberMock(MethodScope.class, false, "b()"),
                this.createMemberMock(MethodScope.class, true, "d()"));
        String sortingResult = properties.sorted(sortingLogic)
                .map(MemberScope::getSchemaPropertyName)
                .collect(Collectors.joining(" "));
        Assert.assertEquals(expectedResult, sortingResult);
    }
}
