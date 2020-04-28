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

package com.github.victools.jsonschema.plugin.maven;

/**
 * Configuration class to hold the configuration of generator modules and their options.
 */
public class GeneratorModule {

    /**
     * Name of the module.
     */
    public String name;

    /**
     * The fully qualified java class name of the module.
     */
    public String className;

    /**
     * The options to be configured on the module.
     */
    public String[] options;
}
