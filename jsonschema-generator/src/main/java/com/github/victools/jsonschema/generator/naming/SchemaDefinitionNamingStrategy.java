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

package com.github.victools.jsonschema.generator.naming;

import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import java.util.Map;

/**
 * Naming strategy for the keys in the "definitions"/"$defs" containing shared/reused subschemas.
 */
public interface SchemaDefinitionNamingStrategy {

    /**
     * Getter for the name/key in the "definitions"/"$defs" to represent the given {@link DefinitionKey}.
     *
     * @param key definition reference for a type (there may be multiple different keys for the same type if custom definitions are involved)
     * @param generationContext generation context providing access to the applied configuration and type context
     * @return name/key in "definitions"/"$defs" for the indicated subschema
     * @see SchemaGenerationContext#getGeneratorConfig()
     * @see SchemaGenerationContext#getTypeContext()
     */
    String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext);

    /**
     * Adjust the names/keys in the "definitions"/"$defs" for the given definition references that have the same names (according to
     * {@link #getDefinitionNameForKey(DefinitionKey, SchemaGenerationContext)}) to ensure their uniqueness.
     * <br>
     * By default, a numeric counter is appended after a separating dash to each duplicate name.
     *
     * @param subschemasWithDuplicateNames definition references that initially have the same values that should be adjusted
     * @param generationContext generation context providing access to the applied configuration and type context
     * @see #getDefinitionNameForKey(DefinitionKey, SchemaGenerationContext)
     */
    default void adjustDuplicateNames(Map<DefinitionKey, String> subschemasWithDuplicateNames, SchemaGenerationContext generationContext) {
        int index = 1;
        for (Map.Entry<DefinitionKey, String> singleEntry : subschemasWithDuplicateNames.entrySet()) {
            singleEntry.setValue(singleEntry.getValue() + "-" + index);
            index++;
        }
    }

    /**
     * Provide an alternative definition name for the given key and name when it is nullable.
     * <br>
     * By default, a {@code "-nullable"} suffix will be appended.
     *
     * @param key definition reference for a type (there may be multiple different keys for the same type if custom definitions are involved)
     * @param definitionName previous result of {@link #getDefinitionNameForKey(DefinitionKey, SchemaGenerationContext)} to be adjusted
     * @param generationContext generation context providing access to the applied configuration and type context
     * @return adjusted definition name
     */
    default String adjustNullableName(DefinitionKey key, String definitionName, SchemaGenerationContext generationContext) {
        return definitionName + "-nullable";
    }
}
