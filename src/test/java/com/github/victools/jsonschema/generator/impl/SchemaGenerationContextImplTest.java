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
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for the {@link SchemaGenerationContextImpl} class.
 */
public class SchemaGenerationContextImplTest {

    private SchemaGenerationContextImpl context;

    @Before
    public void setUp() {
        SchemaGeneratorConfig config = Mockito.mock(SchemaGeneratorConfig.class);
        this.context = new SchemaGenerationContextImpl(config, TypeContextFactory.createDefaultTypeContext());
    }

    @Test
    public void testHandlingDefinition_ExistentNode() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        ObjectNode definitionInput = Mockito.mock(ObjectNode.class);
        SchemaGenerationContextImpl returnValue = this.context.putDefinition(javaType, definitionInput, null);

        Assert.assertSame(this.context, returnValue);
        Assert.assertTrue(this.context.containsDefinition(javaType, null));
        DefinitionKey key = new DefinitionKey(javaType, null);
        Assert.assertSame(definitionInput, this.context.getDefinition(key));
        Assert.assertEquals(Collections.singleton(key), this.context.getDefinedTypes());
    }

    @Test
    public void testHandlingDefinition_EmptyContext() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        DefinitionKey key = new DefinitionKey(javaType, null);

        Assert.assertFalse(this.context.containsDefinition(javaType, null));
        Assert.assertNull(this.context.getDefinition(key));
        Assert.assertEquals(Collections.<DefinitionKey>emptySet(), this.context.getDefinedTypes());
    }

    @Test
    public void testReference_SameType() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        DefinitionKey key = new DefinitionKey(javaType, null);

        // initially, all lists are empty
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getReferences(key));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getNullableReferences(key));

        // adding a not-nullable entry creates the "references" list
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        SchemaGenerationContextImpl returnValue = this.context.addReference(javaType, referenceInputOne, null, false);
        Assert.assertSame(this.context, returnValue);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), this.context.getReferences(key));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getNullableReferences(key));

        // adding another not-nullable entry adds it to the existing "references" list
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        returnValue = this.context.addReference(javaType, referenceInputTwo, null, false);
        Assert.assertSame(this.context, returnValue);
        Assert.assertEquals(Arrays.asList(referenceInputOne, referenceInputTwo), this.context.getReferences(key));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getNullableReferences(key));

        // adding a nullable entry creates the "nullableReferences" list
        ObjectNode referenceInputThree = Mockito.mock(ObjectNode.class);
        returnValue = this.context.addReference(javaType, referenceInputThree, null, true);
        Assert.assertSame(this.context, returnValue);
        Assert.assertEquals(Arrays.asList(referenceInputOne, referenceInputTwo), this.context.getReferences(key));
        Assert.assertEquals(Collections.singletonList(referenceInputThree), this.context.getNullableReferences(key));
    }

    @Test
    public void testReference_DifferentTypes() {
        ResolvedType javaTypeOne = Mockito.mock(ResolvedType.class);
        DefinitionKey keyOne = new DefinitionKey(javaTypeOne, null);
        ResolvedType javaTypeTwo = Mockito.mock(ResolvedType.class);
        DefinitionKey keyTwo = new DefinitionKey(javaTypeTwo, null);

        // adding an entry creates the "references" list for that type
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        this.context.addReference(javaTypeOne, referenceInputOne, null, false);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), this.context.getReferences(keyOne));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getReferences(keyTwo));

        // adding an entry for another type creates a separate "references" list for this other type
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        this.context.addReference(javaTypeTwo, referenceInputTwo, null, false);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), this.context.getReferences(keyOne));
        Assert.assertEquals(Collections.singletonList(referenceInputTwo), this.context.getReferences(keyTwo));
    }

    @Test
    public void testReference_DifferentIgnoredDefinitionProvider() {
        ResolvedType javaType = Mockito.mock(ResolvedType.class);
        CustomDefinitionProviderV2 providerOne = null;
        CustomDefinitionProviderV2 providerTwo = Mockito.mock(CustomDefinitionProviderV2.class);
        DefinitionKey keyOne = new DefinitionKey(javaType, providerOne);
        DefinitionKey keyTwo = new DefinitionKey(javaType, providerTwo);

        // adding an entry creates the "references" list for that type
        ObjectNode referenceInputOne = Mockito.mock(ObjectNode.class);
        this.context.addReference(javaType, referenceInputOne, providerOne, false);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), this.context.getReferences(keyOne));
        Assert.assertEquals(Collections.<ObjectNode>emptyList(), this.context.getReferences(keyTwo));

        // adding an entry for the same type but other ignored definition provider creates a separate "references" list for this other type
        ObjectNode referenceInputTwo = Mockito.mock(ObjectNode.class);
        this.context.addReference(javaType, referenceInputTwo, providerTwo, false);
        Assert.assertEquals(Collections.singletonList(referenceInputOne), this.context.getReferences(keyOne));
        Assert.assertEquals(Collections.singletonList(referenceInputTwo), this.context.getReferences(keyTwo));
    }
}
