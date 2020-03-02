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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Integration tests generating schemas for various use-cases with differing configurations.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    SchemaGeneratorSimpleTypesTest.class,
    SchemaGeneratorComplexTypesTest.class,
    SchemaGeneratorSubtypesTest.class,
    SchemaGeneratorCustomDefinitionsTest.class
})
public class SchemaGeneratorTest {
    // suite as collection of separate test classes to avoid one huge test class
}
