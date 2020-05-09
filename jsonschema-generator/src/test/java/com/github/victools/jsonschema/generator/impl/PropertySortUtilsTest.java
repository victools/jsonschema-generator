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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for the {@link PropertySortUtils} class.
 */
public class PropertySortUtilsTest {

    private FieldScope instanceFieldA;
    private FieldScope instanceFieldC;
    private MethodScope instanceMethodB;
    private FieldScope staticFieldE;
    private MethodScope staticMethodD;
    private MethodScope staticMethodF;
    private List<MemberScope<?, ?>> memberList;

    @Before
    public void setUp() {
        this.instanceFieldA = this.createMemberMock(FieldScope.class, false, "a");
        this.instanceFieldC = this.createMemberMock(FieldScope.class, false, "c");
        this.staticFieldE = this.createMemberMock(FieldScope.class, true, "e");
        this.instanceMethodB = this.createMemberMock(MethodScope.class, false, "b()");
        this.staticMethodD = this.createMemberMock(MethodScope.class, true, "d()");
        this.staticMethodF = this.createMemberMock(MethodScope.class, true, "f()");

        this.memberList = Arrays.asList(
                this.instanceFieldC, this.staticMethodF, this.staticFieldE, this.instanceFieldA, this.instanceMethodB, this.staticMethodD);
    }

    private <S extends MemberScope<?, ?>> S createMemberMock(Class<S> scopeType, boolean isStatic, String name) {
        S mock = Mockito.mock(scopeType);
        Mockito.when(mock.isStatic()).thenReturn(isStatic);
        Mockito.when(mock.getSchemaPropertyName()).thenReturn(name);
        return mock;
    }

    /**
     * Test the correct sorting based on the {@link PropertySortUtils#SORT_PROPERTIES_FIELDS_BEFORE_METHODS} {@code Comparator}.
     */
    @Test
    public void testSortPropertiesFieldsBeforeMethods() {
        String sortingResult = this.memberList.stream()
                .sorted(PropertySortUtils.SORT_PROPERTIES_FIELDS_BEFORE_METHODS)
                .map(MemberScope::getSchemaPropertyName)
                .collect(Collectors.joining(" "));
        Assert.assertEquals("c e a f() b() d()", sortingResult);
    }

    /**
     * Test the correct sorting based on the {@link PropertySortUtils#SORT_PROPERTIES_BY_NAME_ALPHABETICALLY} {@code Comparator}.
     */
    @Test
    public void testSortPropertiesByNameAlphabetically() {
        String sortingResult = this.memberList.stream()
                .sorted(PropertySortUtils.SORT_PROPERTIES_BY_NAME_ALPHABETICALLY)
                .map(MemberScope::getSchemaPropertyName)
                .collect(Collectors.joining(" "));
        Assert.assertEquals("a b() c d() e f()", sortingResult);
    }

    /**
     * Test the correct sorting based on the {@link PropertySortUtils#DEFAULT_PROPERTY_ORDER} {@code Comparator}.
     */
    @Test
    public void testDefaultPropertyOrder() {
        String sortingResult = this.memberList.stream()
                .sorted(PropertySortUtils.DEFAULT_PROPERTY_ORDER)
                .map(MemberScope::getSchemaPropertyName)
                .collect(Collectors.joining(" "));
        Assert.assertEquals("a c e b() d() f()", sortingResult);
    }
}
