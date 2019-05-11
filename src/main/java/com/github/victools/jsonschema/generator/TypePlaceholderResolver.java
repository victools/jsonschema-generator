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

package com.github.victools.jsonschema.generator;

import java.lang.reflect.Type;

/**
 * Contextual resolver for a parent type's declared type variables.
 */
public interface TypePlaceholderResolver {

    /**
     * Determine the actual type behind the given {@link TypeVariable} or {@link WildcardType} (as far as possible).
     * <br>
     * If the given type is not a {@link TypeVariable} or {@link WildcardType}, it will be returned as-is.
     *
     * @param targetType (possible) TypeVariable or WildcardType to resolve
     * @return actual type behind the given target type
     */
    Type resolveGenericTypePlaceholder(Type targetType);

}
