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

import com.github.victools.jsonschema.generator.JavaType;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.impl.ReflectionTypeUtils;

/**
 * Default module being included if {@code Option.FLATTENED_OPTIONALS} is enabled.
 */
public class FlattenedOptionalModule implements Module {

    /**
     * Determine the type of the item/component wrapped in the given {@link Optional} type.
     *
     * @param javaType (possible) optional type
     * @return the wrapped component type (or null if it is not an {@link Optional} (sub) type
     */
    private JavaType resolveOptionalComponentType(JavaType javaType) {
        if (ReflectionTypeUtils.isOptionalType(javaType)) {
            return ReflectionTypeUtils.getOptionalComponentType(javaType);
        }
        return null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withNullableCheck((field, javaType) -> ReflectionTypeUtils.isOptionalType(javaType) ? Boolean.TRUE : null)
                .withTargetTypeOverrideResolver((field, javaType) -> this.resolveOptionalComponentType(javaType));
        builder.forMethods()
                .withNullableCheck((method, javaType) -> ReflectionTypeUtils.isOptionalType(javaType) ? Boolean.TRUE : null)
                .withTargetTypeOverrideResolver((method, javaType) -> this.resolveOptionalComponentType(javaType));
    }
}
