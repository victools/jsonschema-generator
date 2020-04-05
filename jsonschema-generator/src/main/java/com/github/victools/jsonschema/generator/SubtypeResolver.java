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

package com.github.victools.jsonschema.generator;

import com.fasterxml.classmate.ResolvedType;
import java.util.List;

/**
 * Resolver for looking up a declared type's subtypes in order to list those specifically (in an {@link SchemaKeyword#TAG_ANYOF}).
 * <br>
 * Assumption being that {@link SchemaKeyword#TAG_ONEOF} would require a schema validator to unnecessarily check against all listed sub-schemas to
 * ensure that only a single one is matching a given JSON instance. By making the sub-schemas mutually exclusive, the same semantics can be achieved,
 * but allowing the schema validator to ignore any sub-schemas after the first match was found.
 */
@FunctionalInterface
public interface SubtypeResolver {

    /**
     * Look-up the subtypes for a given type, that should be listed independently.
     * <br>
     * If it returns null, the next subtype resolver is expected to be applied. An empty list will result only in the originally declared type to be
     * considered.
     * <br>
     * Returning a list with a single entry will treat the declared type as one-to-one alias for the returned type. Alternatively, you may want to
     * only replace it in the context of a particular field/method through target type overrides.
     *
     * @param declaredType declared type (i.e. without type parameter information)
     * @param context generation context (including a reference to the {@code TypeContext} for deriving a {@link ResolvedType} from a {@link Class})
     * @return list of subtypes to represent as separate schemas; returning
     * @see SchemaGeneratorConfigPart#withTargetTypeOverrideResolver(ConfigFunction)
     */
    List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context);
}
