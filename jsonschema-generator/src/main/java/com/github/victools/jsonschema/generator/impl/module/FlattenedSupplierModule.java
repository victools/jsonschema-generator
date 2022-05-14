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
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Default module being included if {@code Option.FLATTENED_SUPPLIERS} is enabled.
 */
public class FlattenedSupplierModule implements Module {

    private static boolean isSupplier(ResolvedType type) {
        return type.getErasedType() == Supplier.class;
    }

    /**
     * Determine the type of the item/component supplied by the given {@link Supplier} type.
     *
     * @param fieldOrMethod reference to the method/field (which is being ignored here)
     * @return the wrapped supplied type (or null if it is not an {@link Supplier} (sub) type
     */
    private List<ResolvedType> resolveSupplierComponentType(MemberScope<?, ?> fieldOrMethod) {
        return FlattenedSupplierModule.isSupplier(fieldOrMethod.getType())
                ? Collections.singletonList(fieldOrMethod.getTypeParameterFor(Supplier.class, 0))
                : null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withTargetTypeOverridesResolver(this::resolveSupplierComponentType)
                // need to getDeclaredType() to avoid the above override
                .withNullableCheck(field -> FlattenedSupplierModule.isSupplier(field.getDeclaredType()) ? Boolean.TRUE : null);
        builder.forMethods()
                .withTargetTypeOverridesResolver(this::resolveSupplierComponentType)
                // need to getDeclaredType() to avoid the above override
                .withNullableCheck(method -> FlattenedSupplierModule.isSupplier(method.getDeclaredType()) ? Boolean.TRUE : null);
    }
}
