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

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Default module being included if {@code Option.SIMPLIFIED_OPTIONALS} is enabled.
 */
public class SimplifiedOptionalModule implements Module {

    /**
     * The method names to preserve for an {@link Optional} instance's schema representation: "get", "orElse", "isPresent".
     *
     * @see Optional#get()
     * @see Optional#orElse(Object)
     * @see Optional#isPresent()
     */
    public static final String[] DEFAULT_INCLUDED_METHOD_NAMES = {"get", "orElse", "isPresent"};

    private final List<String> includedMethodNames;

    /**
     * Constructor: setting the names of methods declared by the {@link Optional} class to include in its generated object schema.
     *
     * @param includedMethodNames names of the {@link Optional} class' methods to include
     *
     * @see SimplifiedOptionalModule#DEFAULT_INCLUDED_METHOD_NAMES
     */
    public SimplifiedOptionalModule(String... includedMethodNames) {
        if (includedMethodNames == null || includedMethodNames.length == 0) {
            this.includedMethodNames = Arrays.asList(SimplifiedOptionalModule.DEFAULT_INCLUDED_METHOD_NAMES);
        } else {
            this.includedMethodNames = Arrays.asList(includedMethodNames);
        }
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withIgnoreCheck(field -> isOptional(field.getDeclaringType()));
        builder.forMethods()
                .withIgnoreCheck(method -> isOptional(method.getDeclaringType()) && !this.includedMethodNames.contains(method.getName()));
    }

    private static boolean isOptional(ResolvedType type) {
        return type.getErasedType() == Optional.class;
    }
}
