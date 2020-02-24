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

package com.github.victools.jsonschema.generator.impl.module;

import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.reflect.Type;
import java.util.function.Predicate;

/**
 * Default module being included if {@code Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT} is enabled.
 */
public class AdditionalPropertiesModule implements Module {

    /**
     * Create module instance that forbids additional properties everywhere but on container types.
     * <br>
     * This assumes that the respective {@link SimpleTypeModule} instance is being applied first and already enforces the "additionProperties" keyword
     * to be omitted on other non-object schemas.
     *
     * @return module instance
     */
    public static AdditionalPropertiesModule forbiddenForAllObjectsButContainers() {
        return new AdditionalPropertiesModule(scope -> !scope.isContainerType());
    }

    private final Predicate<TypeScope> exclusionCheck;

    /**
     * Constructor.
     *
     * @param exclusionCheck determining whether additionalProperties should be forbidden on a given scope
     */
    public AdditionalPropertiesModule(Predicate<TypeScope> exclusionCheck) {
        this.exclusionCheck = exclusionCheck;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forTypesInGeneral()
                .withAdditionalPropertiesResolver(this::resolveAdditionalProperties);
    }

    /**
     * Either forbid additionProperties (according to the specified check) or leave it up to other configurations.
     *
     * @param scope type scope for which to determine whether additionalProperties should be forbidden
     * @return either Void.class to indicate forbidden additionalProperties or null
     */
    private Type resolveAdditionalProperties(TypeScope scope) {
        if (this.exclusionCheck.test(scope)) {
            return Void.class;
        }
        return null;
    }
}
