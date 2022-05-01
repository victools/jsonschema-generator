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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Default module being included if {@code Option.INLINE_ALL_SCHEMAS} is enabled.
 */
public class InlineSchemaModule implements Module, CustomDefinitionProviderV2 {

    private final ThreadLocal<Deque<ResolvedType>> declaringTypes = ThreadLocal.withInitial(ArrayDeque::new);

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forTypesInGeneral().withCustomDefinitionProvider(this);
    }

    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
        final Deque<ResolvedType> declaringParents = this.declaringTypes.get();
        if (declaringParents.contains(javaType)) {
            throw new IllegalArgumentException("Option.INLINE_ALL_SCHEMAS cannot be fulfilled due to a circular reference to "
                    + context.getTypeContext().getFullTypeDescription(javaType));
        }
        if (context.getTypeContext().isContainerType(javaType)) {
            // container types are being in-lined by default and only handle container-item-scope if the container itself is not a custom definition
            return null;
        }
        declaringParents.addLast(javaType);
        ObjectNode definition = context.createStandardDefinition(javaType, this);
        declaringParents.removeLast();
        return new CustomDefinition(definition, true);
    }
}
