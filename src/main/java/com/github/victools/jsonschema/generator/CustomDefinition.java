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

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The result of a custom definition look-up.
 */
public class CustomDefinition {

    private final ObjectNode value;
    private final boolean meantToBeInline;

    /**
     * Constructor for a custom definition that should follow the standard behaviour in regards to be inlined or mentioned in the "definitions".
     *
     * @param value generated custom definition
     */
    public CustomDefinition(ObjectNode value) {
        this(value, false);
    }

    /**
     * Constructor for a custom definition.
     *
     * @param value generated custom definition
     * @param meantToBeInline whether the definition should be inlined even if it occurs multiple times; otherwise applying standard behaviour
     */
    public CustomDefinition(ObjectNode value, boolean meantToBeInline) {
        this.value = value;
        this.meantToBeInline = meantToBeInline;
    }

    /**
     * Getter for the actual custom definition.
     *
     * @return node containing the custom definition
     */
    public ObjectNode getValue() {
        return this.value;
    }

    /**
     * Getter for the flag indicating whether this custom definition should be inlined even if it occurs multiple times.
     *
     * @return whether this custom definition should be inlined even if it occurs multiple times
     */
    public boolean isMeantToBeInline() {
        return this.meantToBeInline;
    }
}
