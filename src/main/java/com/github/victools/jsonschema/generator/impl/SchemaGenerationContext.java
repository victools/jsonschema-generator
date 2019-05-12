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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.JavaType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generation context in which to collect definitions of traversed types and remember where they are being referenced.
 */
public class SchemaGenerationContext {

    private final Map<JavaType, ObjectNode> definitions = new HashMap<>();
    private final Map<JavaType, List<ObjectNode>> references = new HashMap<>();
    private final Map<JavaType, List<ObjectNode>> nullableReferences = new HashMap<>();

    /**
     * Add the given type's definition to this context.
     *
     * @param javaType type to which the definition belongs
     * @param definitionNode definition to remember
     * @return this context (for chaining)
     */
    public SchemaGenerationContext putDefinition(JavaType javaType, ObjectNode definitionNode) {
        this.definitions.put(javaType, definitionNode);
        return this;
    }

    /**
     * Whether this context (already) contains a definition for the specified type.
     *
     * @param javaType type to check for
     * @return whether a definition for the given type is already present
     */
    public boolean containsDefinition(JavaType javaType) {
        return this.definitions.containsKey(javaType);
    }

    /**
     * Retrieve the previously added definition for the specified type.
     *
     * @param javaType type for which to retrieve the stored definition
     * @return JSON schema definition (or null if none is present)
     * @see #putDefinition(Type, ObjectNode)
     */
    public ObjectNode getDefinition(JavaType javaType) {
        return this.definitions.get(javaType);
    }

    /**
     * Retrieve the set of all types for which a definition has been remembered in this context.
     *
     * @return types for which a definition is present
     */
    public Set<JavaType> getDefinedTypes() {
        return Collections.unmodifiableSet(this.definitions.keySet());
    }

    /**
     * Remember for the specified type that the given node is supposed to either include or reference the type's associated schema.
     *
     * @param javaType type for which to remember a reference
     * @param referencingNode node that should (later) include either the type's respective inline definition or a "$ref" to the definition
     * @param isNullable whether the reference may be null
     * @return this context (for chaining)
     */
    public SchemaGenerationContext addReference(JavaType javaType, ObjectNode referencingNode, boolean isNullable) {
        Map<JavaType, List<ObjectNode>> targetMap = isNullable ? this.nullableReferences : this.references;
        List<ObjectNode> valueList = targetMap.get(javaType);
        if (valueList == null) {
            valueList = new ArrayList<>();
            targetMap.put(javaType, valueList);
        }
        valueList.add(referencingNode);
        return this;
    }

    public List<ObjectNode> getReferences(JavaType javaType) {
        return Collections.unmodifiableList(this.references.getOrDefault(javaType, Collections.emptyList()));
    }

    public List<ObjectNode> getNullableReferences(JavaType javaType) {
        return Collections.unmodifiableList(this.nullableReferences.getOrDefault(javaType, Collections.emptyList()));
    }
}
