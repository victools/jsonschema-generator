/*
 * Copyright 2022 VicTools.
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

/**
 * Default module being included if {@code Option.FLATTENED_SUPPLIER} is enabled, but re-usable for other generic wrappers.
 *
 * @param <W> type of the generic wrapper, which presumably has at least on type parameter from which to derive the actual schema
 *
 * @since 4.25.0
 */
public class FlattenedWrapperModule<W> implements Module {

    private final Class<W> wrapperType;

    /**
     * Constructor setting the type to unwrap.
     *
     * @param wrapperType wrapper type (e.g., <code>Optional</code> or <code>Supplier</code>)
     */
    public FlattenedWrapperModule(Class<W> wrapperType) {
        this.wrapperType = wrapperType;
    }

    /**
     * Check whether the given type is an instance of the predefined wrapper class.
     *
     * @param type type to check for assignability to wrapper class
     * @return whether the given type is deemed to be of the targeted wrapper type in the context of this module
     */
    protected boolean isWrapperType(ResolvedType type) {
        return this.wrapperType.isAssignableFrom(type.getErasedType());
    }

    /**
     * Determine whether the given field/method's declared type is assignable to the targeted wrapper type.
     *
     * @param member field/method to check declared type of (also considering if it's a "fake container item scope")
     * @return whether the declared type is deemed relevant in the context of this module
     */
    protected boolean hasMemberWrapperType(MemberScope<?, ?> member) {
        ResolvedType declaredType = member.getDeclaredType();
        if (member.isFakeContainerItemScope() && member.getContext().isContainerType(declaredType)) {
            declaredType = member.getContext().getContainerItemType(declaredType);
        }
        return this.isWrapperType(declaredType);
    }

    /**
     * Determine the type of the item/component wrapped in the given wrapper type.
     *
     * @param fieldOrMethod reference to the method/field
     * @return the wrapped component type (or null if it is not a targeted wrapper (sub) type)
     */
    private List<ResolvedType> resolveWrapperComponentType(MemberScope<?, ?> fieldOrMethod) {
        return this.isWrapperType(fieldOrMethod.getType())
                ? Collections.singletonList(fieldOrMethod.getTypeParameterFor(this.wrapperType, 0))
                : null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withTargetTypeOverridesResolver(this::resolveWrapperComponentType);
        builder.forMethods()
                .withTargetTypeOverridesResolver(this::resolveWrapperComponentType);
    }
}
