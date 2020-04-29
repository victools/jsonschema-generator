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

import com.github.victools.jsonschema.generator.OptionPreset;

/**
 * Enum of supported standard {@link OptionPreset}s that can be configured.
 */
public enum StandardOptionPreset {
    FULL_DOCUMENTATION(OptionPreset.FULL_DOCUMENTATION),
    PLAIN_JSON(OptionPreset.PLAIN_JSON),
    JAVA_OBJECT(OptionPreset.JAVA_OBJECT),
    NONE(new OptionPreset());

    private final OptionPreset preset;

    private StandardOptionPreset(OptionPreset preset) {
        this.preset = preset;
    }

    /**
     * Getter for the corresponding {@link OptionPreset} instance.
     *
     * @return {@link OptionPreset} instance
     */
    public OptionPreset getPreset() {
        return this.preset;
    }
}
