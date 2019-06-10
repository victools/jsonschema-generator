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

import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import java.util.function.Predicate;

/**
 * Default module for excluding methods.
 */
public class MethodExclusionModule implements Module {

    /**
     * Factory method: creating a {@link MethodExclusionModule} instance that excludes all methods with a {@code void} return type.
     *
     * @return created module instance
     */
    public static MethodExclusionModule forVoidMethods() {
        return new MethodExclusionModule(MethodScope::isVoid);
    }

    /**
     * Factory method: creating a {@link MethodExclusionModule} instance that excludes all getter methods.
     *
     * @return created module instance
     */
    public static MethodExclusionModule forGetterMethods() {
        return new MethodExclusionModule(MethodScope::isGetter);
    }

    /**
     * Factory method: creating a {@link MethodExclusionModule} instance that excludes all methods that don't fall in any of the other categories.
     *
     * @return created module instance
     * @see MethodExclusionModule#forVoidMethods()
     * @see MethodExclusionModule#forGetterMethods()
     */
    public static MethodExclusionModule forNonStaticNonVoidNonGetterMethods() {
        return new MethodExclusionModule(
                method -> !method.isStatic() && !method.isVoid() && !method.isGetter());
    }

    private final Predicate<MethodScope> shouldExcludeMethodsMatching;

    /**
     * Constructor setting the underlying check to be set via {@link SchemaGeneratorConfigPart#withIgnoreCheck(Predicate)}.
     *
     * @param shouldExcludeMethodsMatching check to identify methods to be excluded
     * @see SchemaGeneratorConfigBuilder#forMethods()
     * @see SchemaGeneratorConfigPart#withIgnoreCheck(Predicate)
     */
    public MethodExclusionModule(Predicate<MethodScope> shouldExcludeMethodsMatching) {
        this.shouldExcludeMethodsMatching = shouldExcludeMethodsMatching;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forMethods()
                .withIgnoreCheck(this.shouldExcludeMethodsMatching);
    }
}
