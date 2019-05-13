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
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for the {@link JsonNodeUtils} class.
 */
public class JsonNodeUtilsTest {

    @Test
    public void testSetIfValueNotNull_isNull() {
        ObjectNode node = Mockito.mock(ObjectNode.class);
        JsonNodeUtils.setIfValueNotNull(node, "field", null);

        Mockito.verifyZeroInteractions(node);
    }

    @Test
    public void testSetIfValueNotNull_isNotNull() {
        ObjectNode node = Mockito.mock(ObjectNode.class);
        String fieldName = "field";
        String value = "value";
        JsonNodeUtils.setIfValueNotNull(node, fieldName, value);

        Mockito.verify(node).putPOJO(fieldName, value);
    }
}
