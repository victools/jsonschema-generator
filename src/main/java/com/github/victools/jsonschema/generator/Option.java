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
import com.github.victools.jsonschema.generator.impl.module.ObjectClassExclusionModule;
import com.github.victools.jsonschema.generator.impl.module.SimpleTypeModule;
import com.github.victools.jsonschema.generator.impl.module.VoidMethodExclusionModule;

/**
 * Configuration options to be set on a {@link SchemaGeneratorConfigBuilder} instance.
 */
public enum Option implements Module {
    /**
     * Whether field and methods declaring directly in the Object class should be excluded.
     * <br>
     * Default: true
     */
    IGNORE_OBJECT_CLASS(true, ObjectClassExclusionModule.class),
    /**
     * Whether some fixed types for "string"/"boolean"/"integer"/"number" should be included.
     * <br>
     * Default: true
     */
    INCLUDE_FIXED_SIMPLE_TYPES(true, SimpleTypeModule.class),
    /**
     * Whether the constant values of static final fields should be included.
     * <br>
     * Default: false
     */
    INCLUDE_CONSTANT_FIELD_VALUES(false, ConstantValueModule.class),
    /**
     * Whether all fields with private/package/protected visibility and not accompanying getter method should be excluded.
     * <br>
     * Default: true
     */
    EXCLUDE_NONPUBLIC_FIELDS_WITHOUT_GETTERS(true, FieldWithoutGetterExclusionModule.class),
    /**
     * Whether methods without return value (e.g. setters) should be excluded.
     * <br>
     * Default: true
     */
    EXCLUDE_VOID_METHODS(true, VoidMethodExclusionModule.class),
    /**
     * Whether getter methods should be excluded (assuming their fields are included instead).
     * <br>
     * Default: false
     */
    EXCLUDE_GETTER_METHODS(false, GetterMethodExclusionModule.class),
    /**
     * Whether attributes collected for a field's getter method should be associated with the field directly (assuming the getters are excluded).
     * <br>
     * Beware: this should NOT be enabled at the same time as {@link #INCLUDE_FIELD_ATTRIBUTES_FOR_GETTERS}
     * <br>
     * Default: false
     */
    INCLUDE_GETTER_ATTRIBUTES_FOR_FIELDS(false, GetterAttributesForFieldModule.class),
    /**
     * Whether attributes collected for a field should be associated with its getter method directly (assuming the fields are excluded).
     * <br>
     * Beware: this should NOT be enabled at the same time as {@link #INCLUDE_GETTER_ATTRIBUTES_FOR_FIELDS}
     * <br>
     * Default: false
     */
    INCLUDE_FIELD_ATTRIBUTES_FOR_GETTERS(false, FieldAttributesForGetterModule.class),
    /**
     * Whether an object's field/property should be deemed to be nullable if no specific check says otherwise.
     * <br>
     * Default: true
     */
    FIELDS_ARE_NULLABLE_BY_DEFAULT(true, null),
    /**
     * Whether a method's return value should be deemed to be nullable if no specific check says otherwise.
     * <br>
     * Default: true
     */
    METHODS_RETURN_NULLABLE_BY_DEFAULT(true, null),
    /**
     * Whether all referenced objects should be listed in the schema's "definitions", otherwise single occurrences are defined in-line.
     * <br>
     * Default: false
     */
    DEFINITIONS_FOR_ALL_OBJECTS(false, null);

    /**
     * Whether the setting is enabled by default.
     */
    private final boolean enabledByDefault;
    /**
     * Optional: the module realising the setting/option.
     */
    private final Class<?> defaultModuleClass;

    /**
     * Constructor.
     *
     * @param defaultValue whether the setting is enabled by default
     * @param defaultModuleClass type of the module realising this setting/option if it is enabled
     */
    private Option(boolean defaultValue, Class<? extends Module> defaultModuleClass) {
        this.enabledByDefault = defaultValue;
        this.defaultModuleClass = defaultModuleClass;
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
     * Apply the associated configuration changes (may not do anything, depending on this particular setting/option).
     *
     * @param builder configuration builder instance to which to apply this setting/option's associated configurations
     */
    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        if (this.defaultModuleClass != null) {
            try {
                Module defaultModule = (Module) this.defaultModuleClass.getConstructor().newInstance();
                defaultModule.applyToConfigBuilder(builder);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Default Module without public no-args constructor is not supported", ex);
            }
        }
    }
}
