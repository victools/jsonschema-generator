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
 * Utilities for working with JsonNode instances.
 */
public final class JsonNodeUtils {

    /**
     * Hidden constructor to prevent instantiation of utility class.
     */
    private JsonNodeUtils() {
        // prevent instantiation of static helper class
    }

    /**
     * Set the specified attribute value on the given node. If the attribute value is null, do nothing.
     *
     * @param node object node add the attribute to
     * @param attributeName name of the attribute to set
     * @param attributeValue value to set (or to ignore if it is null)
     */
    public static void setAttributeIfNotNull(ObjectNode node, String attributeName, Object attributeValue) {
        if (attributeValue != null) {
            node.putPOJO(attributeName, attributeValue);
        }
    }
}
