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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for the {@link SchemaGeneratorConfigBuilder} class.
 */
public class SchemaGeneratorConfigBuilderTest {

    private SchemaGeneratorConfigBuilder builder;

    @BeforeEach
    public void setUp() {
        this.builder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09);
    }

    @Test
    public void testGetSetting_WithoutSetting() {
        Assertions.assertNull(this.builder.getSetting(Option.DEFINITIONS_FOR_ALL_OBJECTS));
    }

    @Test
    public void testGetSetting_WithOption() {
        Assertions.assertSame(this.builder, this.builder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS));
        Assertions.assertTrue(this.builder.getSetting(Option.DEFINITIONS_FOR_ALL_OBJECTS));
    }

    @Test
    public void testGetSetting_WithoutOption() {
        Assertions.assertSame(this.builder, this.builder.without(Option.DEFINITIONS_FOR_ALL_OBJECTS));
        Assertions.assertFalse(this.builder.getSetting(Option.DEFINITIONS_FOR_ALL_OBJECTS));
    }

    @Test
    public void testForFields() {
        Assertions.assertNotNull(this.builder.forFields());
    }

    @Test
    public void testForMethods() {
        Assertions.assertNotNull(this.builder.forMethods());
    }
}
