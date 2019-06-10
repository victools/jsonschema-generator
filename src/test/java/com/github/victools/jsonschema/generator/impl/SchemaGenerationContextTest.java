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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for the {@link SchemaGenerationContext} class.
 */
public class SchemaGenerationContextTest {

    private SchemaGenerationContext context;

    @Before
    public void setUp() {
        SchemaGeneratorConfig config = Mockito.mock(SchemaGeneratorConfig.class);
        this.context = new SchemaGenerationContext(config, TypeContextFactory.createDefaultTypeContext());
    }

    @Test
    public void testHandlingDefinition_ExistentNode() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        ObjectNode definitionInput = Mockito.mock(ObjectNode.class);
        SchemaGenerationContext returnValue = this.context.putDefinition(javaType, definitionInput);

        Assert.assertSame(this.context, returnValue);
        Assert.assertTrue(this.context.containsDefinition(javaType));
        Assert.assertSame(definitionInput, this.context.getDefinition(javaType));
        Assert.assertEquals(Collections.singleton(javaType), this.context.getDefinedTypes());
    }

    @Test
    public void testHandlingDefinition_EmptyContext() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);

        Assert.assertFalse(this.context.containsDefinition(javaType));
        Assert.assertNull(this.context.getDefinition(javaType));
        Assert.assertEquals(Collections.<Type>emptySet(), this.context.getDefinedTypes());
    }

    @Test
    public void testReference_SameType() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);

        // initially, all lists are empty
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getReferences(javaType));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getNullableReferences(javaType));

        // adding a not-nullable entry creates the "references" list
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        SchemaGenerationContext returnValue = this.context.addReference(javaType, referenceInputOne, false);
        Assert.assertSame(this.context, returnValue);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), this.context.getReferences(javaType));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getNullableReferences(javaType));

        // adding another not-nullable entry adds it to the existing "references" list
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        returnValue = this.context.addReference(javaType, referenceInputTwo, false);
        Assert.assertSame(this.context, returnValue);
        Assert.assertEquals(Arrays.asList(referenceInputOne, referenceInputTwo), this.context.getReferences(javaType));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getNullableReferences(javaType));

        // adding a nullable entry creates the "nullableReferences" list
        ObjectNode referenceInputThree = Mockito.mock(ObjectNode.class);
        returnValue = this.context.addReference(javaType, referenceInputThree, true);
        Assert.assertSame(this.context, returnValue);
        Assert.assertEquals(Arrays.asList(referenceInputOne, referenceInputTwo), this.context.getReferences(javaType));
        Assert.assertEquals(Collections.singletonList(referenceInputThree), this.context.getNullableReferences(javaType));
    }

    @Test
    public void testReference_DifferentTypes() {
        ResolvedType javaTypeOne = Mockito.mock(ResolvedType.class);
        ResolvedType javaTypeTwo = Mockito.mock(ResolvedType.class);

        // adding an entry creates the "references" list for that type
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        this.context.addReference(javaTypeOne, referenceInputOne, false);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), this.context.getReferences(javaTypeOne));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getReferences(javaTypeTwo));

        // adding an entry for another type creates a separate "references" list for this other type
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        this.context.addReference(javaTypeTwo, referenceInputTwo, false);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), this.context.getReferences(javaTypeOne));
        Assert.assertEquals(Collections.singletonList(referenceInputTwo), this.context.getReferences(javaTypeTwo));

    }
}
