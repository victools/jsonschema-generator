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
 * The result of a custom definition look-up for a particular field/method.
 */
public final class CustomPropertyDefinition extends CustomDefinition {

    /**
     * Constructor for a custom property definition.
     * <br>
     * This is equivalent to calling {@code new CustomPropertyDefinition(value, CustomDefinition.AttributeInclusion.YES)}
     *
     * @param value generated custom definition
     */
    public CustomPropertyDefinition(ObjectNode value) {
        this(value, CustomDefinition.AttributeInclusion.YES);
    }

    /**
     * Constructor for a custom property definition.
     *
     * @param value generated custom definition
     * @param attributeInclusion whether additional attributes should be applied on top of this custom definition
     */
    public CustomPropertyDefinition(ObjectNode value, CustomDefinition.AttributeInclusion attributeInclusion) {
        super(value, DefinitionType.INLINE, attributeInclusion);
    }
}
