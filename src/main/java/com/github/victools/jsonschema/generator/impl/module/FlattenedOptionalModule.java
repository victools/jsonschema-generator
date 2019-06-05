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
import com.fasterxml.classmate.ResolvedTypeWithMembers;
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
     * @param <F> either method or field
     * @param fieldOrMethod reference to the method/field (which is being ignored here)
     * @param javaType (possible) optional type of the field or being returned by the method
     * @param declaringType method/field's declaring type (ignored here)
     * @return the wrapped component type (or null if it is not an {@link Optional} (sub) type
     */
    private <F> ResolvedType resolveOptionalComponentType(F fieldOrMethod, ResolvedType javaType, ResolvedTypeWithMembers declaringType) {
        return isOptional(javaType) ? javaType.typeParametersFor(Optional.class).get(0) : null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withNullableCheck((field, javaType, declaringType) -> isOptional(javaType) ? Boolean.TRUE : null)
                .withTargetTypeOverrideResolver(this::resolveOptionalComponentType);
        builder.forMethods()
                .withNullableCheck((method, javaType, declaringType) -> isOptional(javaType) ? Boolean.TRUE : null)
                .withTargetTypeOverrideResolver(this::resolveOptionalComponentType);
    }
}
