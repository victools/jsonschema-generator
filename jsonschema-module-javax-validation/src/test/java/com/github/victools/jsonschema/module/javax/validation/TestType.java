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

package com.github.victools.jsonschema.module.javax.validation;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.TypeContext;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.util.stream.Stream;

/**
 * Helper class for constructing {@link FieldScope} and {@link MethodScope} instances in tests.
 */
public class TestType {

    private final TypeContext context;
    private final ResolvedType resolvedTestClass;
    private final ResolvedTypeWithMembers testClassMembers;

    public TestType(Class<?> testClass) {
        this.context = TypeContextFactory.createDefaultTypeContext();
        this.resolvedTestClass = this.context.resolve(testClass);
        this.testClassMembers = this.context.resolveWithMembers(this.resolvedTestClass);
    }

    public FieldScope getMemberField(String fieldName) {
        return this.getField(this.testClassMembers.getMemberFields(), fieldName);
    }

    public FieldScope getStaticField(String fieldName) {
        return this.getField(this.testClassMembers.getStaticFields(), fieldName);
    }

    private FieldScope getField(ResolvedField[] fields, String fieldName) {
        return Stream.of(fields)
                .filter(field -> fieldName.equals(field.getName()))
                .findAny()
                .map(field -> this.context.createFieldScope(field, this.testClassMembers))
                .get();
    }

    public MethodScope getMemberMethod(String methodName) {
        return this.getMethod(this.testClassMembers.getMemberMethods(), methodName);
    }

    public MethodScope getStaticMethod(String methodName) {
        return this.getMethod(this.testClassMembers.getStaticMethods(), methodName);
    }

    private MethodScope getMethod(ResolvedMethod[] methods, String methodName) {
        return Stream.of(methods)
                .filter(method -> methodName.equals(method.getName()))
                .findAny()
                .map(method -> this.context.createMethodScope(method, this.testClassMembers))
                .get();
    }
}
