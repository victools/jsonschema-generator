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

package com.github.victools.jsonschema.plugin.maven.testpackage;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.github.victools.jsonschema.plugin.maven.annotations.AnotherTestAnnotation;

@AnotherTestAnnotation
@JsonClassDescription("Jackson annotation class for Test Class A")
public class TestClassA {

    private String aString;

    public TestClassA(String aString) {
        this.aString = aString;
    }
}
