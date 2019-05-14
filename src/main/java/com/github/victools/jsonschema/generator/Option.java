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

import com.github.victools.jsonschema.generator.impl.module.ConstantValueModule;
import com.github.victools.jsonschema.generator.impl.module.FieldAttributesForGetterModule;
import com.github.victools.jsonschema.generator.impl.module.FieldWithoutGetterExclusionModule;
import com.github.victools.jsonschema.generator.impl.module.GetterAttributesForFieldModule;
import com.github.victools.jsonschema.generator.impl.module.GetterMethodExclusionModule;
import com.github.victools.jsonschema.generator.impl.module.SimpleTypeModule;
import com.github.victools.jsonschema.generator.impl.module.StaticMethodExclusionModule;
import com.github.victools.jsonschema.generator.impl.module.VoidMethodExclusionModule;
import java.util.function.Supplier;

/**
 * Configuration options to be set on a {@link SchemaGeneratorConfigBuilder} instance.
 */
public enum Option {
    /**
     * Whether additional types (and not just primitives and their associated classes should be included as fixed schema with a "type" attribute of
     * "string"/"boolean"/"integer"/"number".
     * <br>
     * Default: true (enabled)
     */
    ADDITIONAL_FIXED_TYPES(true, SimpleTypeModule::forPrimitiveAndAdditionalTypes, SimpleTypeModule::forPrimitiveTypes),
    /**
     * Whether the constant values of static final fields should be included.
     * <br>
     * Default: false (disabled)
     */
    VALUES_FROM_CONSTANT_FIELDS(false, ConstantValueModule::new, null),
    /**
     * Whether all fields with private/package/protected visibility and no accompanying getter method should be included.
     * <br>
     * Default: false (disabled)
     */
    NONPUBLIC_FIELDS_WITHOUT_GETTERS(false, null, FieldWithoutGetterExclusionModule::new),
    /**
     * Whether methods without return value (e.g. setters) should be included.
     * <br>
     * Default: false (disabled)
     */
    VOID_METHODS(false, null, VoidMethodExclusionModule::new),
    /**
     * Whether static methods should be included.
     * <br>
     * Default: false (disabled)
     */
    STATIC_METHODS(false, null, StaticMethodExclusionModule::new),
    /**
     * Whether getter methods should be included (assuming their fields are not included).
     * <br>
     * Default: false (disabled)
     */
    GETTER_METHODS(true, null, GetterMethodExclusionModule::new),
    /**
     * Whether attributes collected for a field's getter method should be associated with the field directly (assuming the getters are excluded).
     * <br>
     * Beware: this should NOT be enabled at the same time as {@link #FIELD_ATTRIBUTES_FOR_GETTERS}
     * <br>
     * Default: false (disabled)
     */
    GETTER_ATTRIBUTES_FOR_FIELDS(false, GetterAttributesForFieldModule::new, null),
    /**
     * Whether attributes collected for a field should be associated with its getter method directly (assuming the fields are excluded).
     * <br>
     * Beware: this should NOT be enabled at the same time as {@link #GETTER_ATTRIBUTES_FOR_FIELDS}
     * <br>
     * Default: false (disabled)
     */
    FIELD_ATTRIBUTES_FOR_GETTERS(false, FieldAttributesForGetterModule::new, null),
    /**
     * Whether an object's field/property should be deemed to be nullable if no specific check says otherwise.
     * <br>
     * Default: false (disabled)
     */
    NULLABLE_FIELDS_BY_DEFAULT(false, null, null),
    /**
     * Whether a method's return value should be deemed to be nullable if no specific check says otherwise.
     * <br>
     * Default: false (disabled)
     */
    NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT(false, null, null),
    /**
     * Whether all referenced objects should be listed in the schema's "definitions", otherwise single occurrences are defined in-line.
     * <br>
     * Default: false (disabled)
     */
    DEFINITIONS_FOR_ALL_OBJECTS(false, null, null);

    /**
     * Whether the setting is enabled by default.
     */
    private final boolean enabledByDefault;
    /**
     * Optional: the module realising the setting/option if it is enabled.
     */
    private final Supplier<Module> enabledModuleProvider;
    /**
     * Optional: the module realising the setting/option if it is disabled.
     */
    private final Supplier<Module> disabledModuleProvider;

    /**
     * Constructor.
     *
     * @param defaultValue whether the setting is enabled by default
     * @param enabledModuleProvider type of the module realising this setting/option if it is enabled
     * @param disabledModuleProvider type of the module realising this setting/option if it is disabled
     */
    private Option(boolean defaultValue, Supplier<Module> enabledModuleProvider, Supplier<Module> disabledModuleProvider) {
        this.enabledByDefault = defaultValue;
        this.enabledModuleProvider = enabledModuleProvider;
        this.disabledModuleProvider = disabledModuleProvider;
    }

    /**
     * Whether this setting should be enabled if no specific check says otherwise.
     *
     * @return default flag
     */
    public boolean isEnabledByDefault() {
        return this.enabledByDefault;
    }

    /**
     * Retrieve the associated configuration changes as a module instance if possible (depending on particular setting/option: may return null).
     *
     * @param isEnabled whether the option is currently enabled
     * @return a module instance representing this setting/option's associated configurations (may be null)
     */
    public Module getModule(boolean isEnabled) {
        Supplier<Module> targetModuleProvider = isEnabled ? this.enabledModuleProvider : this.disabledModuleProvider;
        return targetModuleProvider == null ? null : targetModuleProvider.get();
    }
}
