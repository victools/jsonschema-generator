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

import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Default module being included if {@code Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT} is enabled.
 */
public class AdditionalPropertiesModule implements Module {

    /**
     * Create module instance that specifically allows additional properties on {@link Map} instances.
     *
     * @return module instance
     */
    public static AdditionalPropertiesModule forMapValues() {
        return new AdditionalPropertiesModule(scope -> {
            if (scope.getType().isInstanceOf(Map.class)) {
                // within a Map<Key, Value> allow additionalProperties of the Value type
                // if no type parameters are defined, this will result in additionalProperties to be omitted (by way of returning Object.class)
                return scope.getTypeParameterFor(Map.class, 1);
            }
            return null;
        });
    }

    /**
     * Create module instance that forbids additional properties everywhere but on container types.
     * <br>
     * This assumes that the respective {@link SimpleTypeModule} instance is being applied first and already enforces the "additionalProperties"
     * keyword to be omitted on other non-object schemas.
     *
     * @return module instance
     */
    public static AdditionalPropertiesModule forbiddenForAllObjectsButContainers() {
        return new AdditionalPropertiesModule(scope -> scope.isContainerType() ? null : Void.class);
    }

    private final ConfigFunction<TypeScope, Type> additionalPropertiesResolver;

    /**
     * Constructor.
     *
     * @param additionalPropertiesResolver resolver for additionalProperties
     */
    public AdditionalPropertiesModule(ConfigFunction<TypeScope, Type> additionalPropertiesResolver) {
        this.additionalPropertiesResolver = additionalPropertiesResolver;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forTypesInGeneral()
                .withAdditionalPropertiesResolver(this.additionalPropertiesResolver);
    }
}
