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

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import java.util.stream.Stream;
import org.junit.Before;

/**
 *
 */
public class AbstractTypeAwareTest {

    private final Class<?> testClass;

    protected ResolvedTypeWithMembers testClassMembers;

    protected AbstractTypeAwareTest(Class<?> testClass) {
        this.testClass = testClass;
    }

    @Before
    public final void abstractSetUp() {
        TypeResolver typeResolver = new TypeResolver();
        MemberResolver memberResolver = new MemberResolver(typeResolver);
        ResolvedType resolvedTestClass = typeResolver.resolve(this.testClass);
        this.testClassMembers = memberResolver.resolve(resolvedTestClass, null, null);
    }

    protected ResolvedField getTestClassField(String fieldName) {
        return Stream.of(this.testClassMembers.getMemberFields())
                .filter(field -> fieldName.equals(field.getName()))
                .findAny()
                .orElseGet(
                        () -> Stream.of(this.testClassMembers.getStaticFields())
                                .filter(field -> fieldName.equals(field.getName()))
                                .findAny()
                                .get());
    }

    protected ResolvedMethod getTestClassMethod(String methodName) {
        return Stream.of(this.testClassMembers.getMemberMethods())
                .filter(method -> methodName.equals(method.getName()))
                .findAny()
                .orElseGet(
                        () -> Stream.of(this.testClassMembers.getStaticMethods())
                                .filter(method -> methodName.equals(method.getName()))
                                .findAny()
                                .get());
    }
}
