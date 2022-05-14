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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Selection of {@link Option} entries to be enabled by default, which can be overridden via
 * {@link SchemaGeneratorConfigBuilder#with(Option, Option...)}/{@link SchemaGeneratorConfigBuilder#without(Option, Option...)}.
 */
public class OptionPreset {

    /**
     * Preset: including private/package/protected/public fields and all public methods.
     */
    public static final OptionPreset FULL_DOCUMENTATION = new OptionPreset(
            Option.VALUES_FROM_CONSTANT_FIELDS,
            Option.PUBLIC_STATIC_FIELDS,
            Option.PUBLIC_NONSTATIC_FIELDS,
            Option.NONPUBLIC_STATIC_FIELDS,
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
            Option.TRANSIENT_FIELDS,
            Option.STATIC_METHODS,
            Option.VOID_METHODS,
            Option.GETTER_METHODS,
            Option.NONSTATIC_NONVOID_NONGETTER_METHODS,
            Option.SIMPLIFIED_ENUMS,
            Option.SIMPLIFIED_OPTIONALS,
            Option.DEFINITIONS_FOR_ALL_OBJECTS,
            Option.NULLABLE_FIELDS_BY_DEFAULT,
            Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT,
            Option.ALLOF_CLEANUP_AT_THE_END
    );

    /**
     * Preset: including private/package/protected/public fields and no methods.
     */
    public static final OptionPreset PLAIN_JSON = new OptionPreset(
            Option.SCHEMA_VERSION_INDICATOR,
            Option.ADDITIONAL_FIXED_TYPES,
            Option.FLATTENED_ENUMS,
            Option.FLATTENED_OPTIONALS,
            Option.FLATTENED_SUPPLIERS,
            Option.VALUES_FROM_CONSTANT_FIELDS,
            Option.PUBLIC_NONSTATIC_FIELDS,
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS,
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS,
            Option.ALLOF_CLEANUP_AT_THE_END
    );

    /**
     * Preset: including public fields and all public methods.
     */
    public static final OptionPreset JAVA_OBJECT = new OptionPreset(
            Option.VALUES_FROM_CONSTANT_FIELDS,
            Option.PUBLIC_STATIC_FIELDS,
            Option.PUBLIC_NONSTATIC_FIELDS,
            Option.STATIC_METHODS,
            Option.VOID_METHODS,
            Option.GETTER_METHODS,
            Option.NONSTATIC_NONVOID_NONGETTER_METHODS,
            Option.SIMPLIFIED_ENUMS,
            Option.SIMPLIFIED_OPTIONALS,
            Option.ALLOF_CLEANUP_AT_THE_END
    );

    private final Set<Option> defaultEnabledOptions;

    /**
     * Constructor: defining the fixed list of options to be enabled by default.
     *
     * @param enabledByDefault options in this preset
     */
    public OptionPreset(Option... enabledByDefault) {
        this.defaultEnabledOptions = new HashSet<>(Arrays.asList(enabledByDefault));
    }

    /**
     * Indicate whether the given option should be enabled, if it was not specifically included/excluded in the {@link SchemaGeneratorConfigBuilder}.
     *
     * @param setting option to check
     * @return whether the given option is enabled by defaul
     * @see SchemaGeneratorConfigBuilder#with(Option, Option...)
     * @see SchemaGeneratorConfigBuilder#without(Option, Option...)
     */
    public boolean isOptionEnabledByDefault(Option setting) {
        return this.defaultEnabledOptions.contains(setting);
    }
}
