/*
 * Copyright 2024 VicTools.
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
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.util.Arrays;
import java.util.List;

/**
 * Default module being included if {@code Option.ACCEPT_SINGLE_VALUE_AS_ARRAY} is enabled.
 *
 * @since 4.36.0
 */
public class SingleValueAsArrayModule implements Module {

    /**
     * Allow a container's item type as single instance alternative to an array, or return null for non-containers.
     *
     * @param scope targeted field/method
     * @return collection containing both the container's item type and the container type as such, or null
     */
    private static List<ResolvedType> acceptSingleValueAsArray(MemberScope<?, ?> scope) {
        if (scope.isContainerType() && !scope.isFakeContainerItemScope()) {
            return Arrays.asList(scope.getContainerItemType(), scope.getType());
        }
        return null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withTargetTypeOverridesResolver(SingleValueAsArrayModule::acceptSingleValueAsArray);
        builder.forMethods()
                .withTargetTypeOverridesResolver(SingleValueAsArrayModule::acceptSingleValueAsArray);
    }
}
