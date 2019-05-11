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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the {@link SchemaGeneratorConfigBuilder} class.
 */
public class SchemaGeneratorConfigBuilderTest {

    private SchemaGeneratorConfigBuilder builder;

    @Before
    public void setUp() {
        this.builder = new SchemaGeneratorConfigBuilder(new ObjectMapper());
    }

    @Test
    public void testGetSetting_WithoutSetting() {
        Assert.assertNull(this.builder.getSetting(Option.DEFINITIONS_FOR_ALL_OBJECTS));
    }

    @Test
    public void testGetSetting_WithOption() {
        Assert.assertSame(this.builder, this.builder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS));
        Assert.assertTrue(this.builder.getSetting(Option.DEFINITIONS_FOR_ALL_OBJECTS));
    }

    @Test
    public void testGetSetting_WithoutOption() {
        Assert.assertSame(this.builder, this.builder.without(Option.DEFINITIONS_FOR_ALL_OBJECTS));
        Assert.assertFalse(this.builder.getSetting(Option.DEFINITIONS_FOR_ALL_OBJECTS));
    }

    @Test
    public void testForFields() {
        Assert.assertNotNull(this.builder.forFields());
    }

    @Test
    public void testForMethods() {
        Assert.assertNotNull(this.builder.forMethods());
    }
}
