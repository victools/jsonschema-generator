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

package com.github.victools.jsonschema.plugin.maven.testpackage.subpackage;

import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("Jackson annotation classC of the subpackage")
public class TestClassC {
    private double aDouble;
    private double anotherDouble;

    public TestClassC(double aDouble, double anotherDouble) {
        this.aDouble = aDouble;
        this.anotherDouble = anotherDouble;
    }
}
