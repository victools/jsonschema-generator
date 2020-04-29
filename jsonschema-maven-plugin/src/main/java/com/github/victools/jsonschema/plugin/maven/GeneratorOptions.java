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

import com.github.victools.jsonschema.generator.Option;

/**
 * Class to hold the configuration options of the generator.
 */
public class GeneratorOptions {

    // The preset options
    public StandardOptionPreset preset;

    // The options of the generator that should be enabled
    public Option[] enabled;

    // The options of the generator that should be disabled
    public Option[] disabled;
}
