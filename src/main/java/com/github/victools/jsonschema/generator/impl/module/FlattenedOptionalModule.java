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
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.util.Optional;

/**
 * Default module being included if {@code Option.FLATTENED_OPTIONALS} is enabled.
 */
public class FlattenedOptionalModule implements Module {

    private static boolean isOptional(ResolvedType type) {
        return type.getErasedType() == Optional.class;
    }

    /**
     * Determine the type of the item/component wrapped in the given {@link Optional} type.
     *
     * @param fieldOrMethod reference to the method/field (which is being ignored here)
     * @return the wrapped component type (or null if it is not an {@link Optional} (sub) type
     */
    private ResolvedType resolveOptionalComponentType(MemberScope<?, ?> fieldOrMethod) {
        return FlattenedOptionalModule.isOptional(fieldOrMethod.getType()) ? fieldOrMethod.getTypeParameterFor(Optional.class, 0) : null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withTargetTypeOverrideResolver(this::resolveOptionalComponentType)
                // need to getDeclaredType() to avoid the above override
                .withNullableCheck(field -> FlattenedOptionalModule.isOptional(field.getDeclaredType()) ? Boolean.TRUE : null);
        builder.forMethods()
                .withTargetTypeOverrideResolver(this::resolveOptionalComponentType)
                // need to getDeclaredType() to avoid the above override
                .withNullableCheck(method -> FlattenedOptionalModule.isOptional(method.getDeclaredType()) ? Boolean.TRUE : null);
    }
}
