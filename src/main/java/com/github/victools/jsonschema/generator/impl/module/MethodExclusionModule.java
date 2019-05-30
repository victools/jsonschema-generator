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

package com.github.victools.jsonschema.generator.impl.module;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.impl.ReflectionGetterUtils;
import java.lang.reflect.Modifier;
import java.util.function.BiPredicate;

/**
 * Default module for excluding methods.
 */
public class MethodExclusionModule implements Module {

    /**
     * Factory method: creating a {@link MethodExclusionModule} instance that excludes all {@code static} methods.
     *
     * @return created module instance
     */
    public static MethodExclusionModule forStaticMethods() {
        return new MethodExclusionModule((method, context) -> isMethodStatic(method));
    }

    /**
     * Factory method: creating a {@link MethodExclusionModule} instance that excludes all methods with a {@code void} return type.
     *
     * @return created module instance
     */
    public static MethodExclusionModule forVoidMethods() {
        return new MethodExclusionModule((method, context) -> !method.hasReturnType());
    }

    /**
     * Factory method: creating a {@link MethodExclusionModule} instance that excludes all getter methods.
     *
     * @return created module instance
     */
    public static MethodExclusionModule forGetterMethods() {
        return new MethodExclusionModule(ReflectionGetterUtils::isGetter);
    }

    /**
     * Factory method: creating a {@link MethodExclusionModule} instance that excludes all methods that don't fall in any of the other categories.
     *
     * @return created module instance
     * @see MethodExclusionModule#forStaticMethods()
     * @see MethodExclusionModule#forVoidMethods()
     * @see MethodExclusionModule#forGetterMethods()
     */
    public static MethodExclusionModule forNonStaticNonVoidNonGetterMethods() {
        return new MethodExclusionModule((method, context) -> !isMethodStatic(method) && method.hasReturnType()
                && !ReflectionGetterUtils.isGetter(method, context));
    }

    /**
     * Check whether a given method has the {@code static} modifier.
     *
     * @param method method to check
     * @return whether the {@code static} modifier is present
     */
    private static boolean isMethodStatic(AnnotatedMethod method) {
        return Modifier.isStatic(method.getModifiers());
    }

    private final BiPredicate<AnnotatedMethod, BeanDescription> shouldExcludeMethodsMatching;

    /**
     * Constructor setting the underlying check to be set via {@link SchemaGeneratorConfigPart#withIgnoreCheck(BiPredicate)}.
     *
     * @param shouldExcludeMethodsMatching check to identify methods to be excluded
     * @see SchemaGeneratorConfigBuilder#forMethods()
     * @see SchemaGeneratorConfigPart#withIgnoreCheck(BiPredicate)
     */
    public MethodExclusionModule(BiPredicate<AnnotatedMethod, BeanDescription> shouldExcludeMethodsMatching) {
        this.shouldExcludeMethodsMatching = shouldExcludeMethodsMatching;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forMethods()
                .withIgnoreCheck(this.shouldExcludeMethodsMatching);
    }
}
