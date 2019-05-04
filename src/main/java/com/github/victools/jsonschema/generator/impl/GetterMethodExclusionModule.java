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

import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.ReflectionUtils;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

/**
 * Default module being included if {@code Option.EXCLUDE_GETTER_METHODS} is enabled.
 */
public class GetterMethodExclusionModule implements Module {

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forMethods()
                .addIgnoreCheck(ReflectionUtils::isGetter);
    }
}
