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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for the {@link SchemaGenerationContext} class.
 */
public class SchemaGenerationContextTest {

    @Test
    public void testHandlingDefinition_ExistentNode() {
        SchemaGenerationContext context = new SchemaGenerationContext();
        JavaType javaType = Mockito.mock(JavaType.class);
        ObjectNode definitionInput = Mockito.mock(ObjectNode.class);
        SchemaGenerationContext returnValue = context.putDefinition(javaType, definitionInput);

        Assert.assertSame(context, returnValue);
        Assert.assertTrue(context.containsDefinition(javaType));
        Assert.assertSame(definitionInput, context.getDefinition(javaType));
        Assert.assertEquals(Collections.singleton(javaType), context.getDefinedTypes());
    }

    @Test
    public void testHandlingDefinition_EmptyContext() {
        SchemaGenerationContext context = new SchemaGenerationContext();
        JavaType javaType = Mockito.mock(JavaType.class);

        Assert.assertFalse(context.containsDefinition(javaType));
        Assert.assertNull(context.getDefinition(javaType));
        Assert.assertEquals(Collections.<JavaType>emptySet(), context.getDefinedTypes());
    }

    @Test
    public void testReference_SameType() {
        SchemaGenerationContext context = new SchemaGenerationContext();
        JavaType javaType = Mockito.mock(JavaType.class);

        // initially, all lists are empty
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), context.getReferences(javaType));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), context.getNullableReferences(javaType));

        // adding a not-nullable entry creates the "references" list
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        SchemaGenerationContext returnValue = context.addReference(javaType, referenceInputOne, false);
        Assert.assertSame(context, returnValue);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), context.getReferences(javaType));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), context.getNullableReferences(javaType));

        // adding another not-nullable entry adds it to the existing "references" list
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        returnValue = context.addReference(javaType, referenceInputTwo, false);
        Assert.assertSame(context, returnValue);
        Assert.assertEquals(Arrays.asList(referenceInputOne, referenceInputTwo), context.getReferences(javaType));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), context.getNullableReferences(javaType));

        // adding a nullable entry creates the "nullableReferences" list
        ObjectNode referenceInputThree = Mockito.mock(ObjectNode.class);
        returnValue = context.addReference(javaType, referenceInputThree, true);
        Assert.assertSame(context, returnValue);
        Assert.assertEquals(Arrays.asList(referenceInputOne, referenceInputTwo), context.getReferences(javaType));
        Assert.assertEquals(Collections.singletonList(referenceInputThree), context.getNullableReferences(javaType));
    }

    @Test
    public void testReference_DifferentTypes() {
        SchemaGenerationContext context = new SchemaGenerationContext();
        JavaType javaTypeOne = Mockito.mock(JavaType.class);
        JavaType javaTypeTwo = Mockito.mock(JavaType.class);

        // adding an entry creates the "references" list for that type
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        context.addReference(javaTypeOne, referenceInputOne, false);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), context.getReferences(javaTypeOne));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), context.getReferences(javaTypeTwo));

        // adding an entry for another type creates a separate "references" list for this other type
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        context.addReference(javaTypeTwo, referenceInputTwo, false);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), context.getReferences(javaTypeOne));
        Assert.assertEquals(Collections.singletonList(referenceInputTwo), context.getReferences(javaTypeTwo));

    }
}
