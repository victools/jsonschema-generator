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

package com.github.victools.jsonschema.module.microprofile.openapi3;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SubtypeResolver;
import com.github.victools.jsonschema.generator.TypeContext;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Subtype resolver considering {@code @Schema(anyOf = ...)}.
 */
public class MicroProfileOpenApi3AnyOfResolver implements SubtypeResolver {

    @Override
    public List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
        Schema annotation = declaredType.getErasedType().getAnnotation(Schema.class);
        if (annotation == null || annotation.anyOf().length == 0) {
            return null;
        }
        TypeContext typeContext = context.getTypeContext();
        return Stream.of(annotation.anyOf())
                .map(typeContext::resolve)
                .collect(Collectors.toList());
    }
}
