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

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Entry point for customising the JSON Schema attributes being collected for a type in general, i.e. the part that may be referenced multiple times.
 */
public interface TypeAttributeOverrideV2 {

    /**
     * Add/remove attributes on the given JSON Schema node – this is specifically intended for attributes relating to the type in general.
     * <br>
     * E.g. {@link SchemaKeyword#TAG_FORMAT}, {@link SchemaKeyword#TAG_PATTERN}, {@link SchemaKeyword#TAG_REQUIRED}
     *
     * @param collectedTypeAttributes node to modify (the part that may be referenced multiple times)
     * @param scope the type representation associated with the JSON Schema node
     * @param context generation context
     */
    void overrideTypeAttributes(ObjectNode collectedTypeAttributes, TypeScope scope, SchemaGenerationContext context);
}
