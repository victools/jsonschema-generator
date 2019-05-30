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

import com.fasterxml.jackson.databind.JavaType;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.util.Optional;

/**
 * Default module being included if {@code Option.FLATTENED_OPTIONALS} is enabled.
 */
public class FlattenedOptionalModule implements Module {

    /**
     * Check whether the given type represents an {@link Optional}.
     *
     * @param javaType type to check
     * @return whether the given type's raw class is {@link Optional}
     */
    private boolean isOptionalType(JavaType javaType) {
        return javaType.hasRawClass(Optional.class);
    }

    /**
     * Determine the type of the item/component wrapped in the given {@link Optional} type.
     *
     * @param javaType (possible) optional type
     * @return the wrapped component type (or null if it is not an {@link Optional} (sub) type
     */
    private JavaType resolveOptionalComponentType(JavaType javaType) {
        if (this.isOptionalType(javaType)) {
            return javaType.containedType(0);
        }
        return null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withNullableCheck((field, javaType, context) -> this.isOptionalType(javaType) ? Boolean.TRUE : null)
                .withTargetTypeOverrideResolver((field, javaType, context) -> this.resolveOptionalComponentType(javaType));
        builder.forMethods()
                .withNullableCheck((method, javaType, context) -> this.isOptionalType(javaType) ? Boolean.TRUE : null)
                .withTargetTypeOverrideResolver((method, javaType, context) -> this.resolveOptionalComponentType(javaType));
    }
}
