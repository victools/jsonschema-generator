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
import com.github.victools.jsonschema.generator.impl.module.EnumModule;
import com.github.victools.jsonschema.generator.impl.module.FieldExclusionModule;
import com.github.victools.jsonschema.generator.impl.module.FlattenedOptionalModule;
import com.github.victools.jsonschema.generator.impl.module.MethodExclusionModule;
import com.github.victools.jsonschema.generator.impl.module.SimpleTypeModule;
import com.github.victools.jsonschema.generator.impl.module.SimplifiedOptionalModule;
import java.util.function.Supplier;

/**
 * Configuration options to be set on a {@link SchemaGeneratorConfigBuilder} instance.
 */
public enum Option {
    /**
     * Whether the "{@value SchemaConstants#TAG_SCHEMA}" attribute with value "{@value SchemaConstants#TAG_SCHEMA_DRAFT7}" should be included.
     */
    SCHEMA_VERSION_INDICATOR(null, null),
    /**
     * Whether additional types (and not just primitives and their associated classes should be included as fixed schema with a "type" attribute of
     * "string"/"boolean"/"integer"/"number".
     */
    ADDITIONAL_FIXED_TYPES(SimpleTypeModule::forPrimitiveAndAdditionalTypes, SimpleTypeModule::forPrimitiveTypes),
    /**
     * Whether enums should be treated as plain "{@value SchemaConstants#TAG_TYPE_STRING}" values.
     *
     * @see Option#SIMPLIFIED_ENUMS
     */
    FLATTENED_ENUMS(EnumModule::asStrings, null),
    /**
     * Whether enums should be treated as "{@value SchemaConstants#TAG_TYPE_OBJECT}", with all methods but {@link Enum#name() name()} being excluded.
     * <br>
     * This only takes effect if {@link Option#FLATTENED_ENUMS} is disabled.
     */
    SIMPLIFIED_ENUMS(EnumModule::asObjects, null),
    /**
     * Whether any {@link java.util.Optional Optional} instance should be treated as nullable value of the wrapped type.
     *
     * @see Option#SIMPLIFIED_OPTIONALS
     */
    FLATTENED_OPTIONALS(FlattenedOptionalModule::new, null),
    /**
     * Whether any {@link java.util.Optional Optional} instance should be reduced to an object with only three methods.
     * <br>
     * This only takes effect if {@link Option#FLATTENED_OPTIONALS} is disabled.
     *
     * @see SimplifiedOptionalModule#DEFAULT_INCLUDED_METHOD_NAMES
     */
    SIMPLIFIED_OPTIONALS(SimplifiedOptionalModule::new, null),
    /**
     * Whether the constant values of static final fields should be included.
     */
    VALUES_FROM_CONSTANT_FIELDS(ConstantValueModule::new, null),
    /**
     * Whether {@code static} fields with public visibility should be included.
     *
     * @see Option#PUBLIC_NONSTATIC_FIELDS
     * @see Option#NONPUBLIC_STATIC_FIELDS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS
     * @see Option#TRANSIENT_FIELDS
     */
    PUBLIC_STATIC_FIELDS(null, null),
    /**
     * Whether {@code static} fields with public visibility should be included.
     *
     * @see Option#PUBLIC_STATIC_FIELDS
     * @see Option#NONPUBLIC_STATIC_FIELDS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS
     * @see Option#TRANSIENT_FIELDS
     */
    PUBLIC_NONSTATIC_FIELDS(null, FieldExclusionModule::forPublicNonStaticFields),
    /**
     * Whether {@code static} fields with private/package/protected visibility should be included.
     *
     * @see Option#PUBLIC_STATIC_FIELDS
     * @see Option#PUBLIC_NONSTATIC_FIELDS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS
     * @see Option#TRANSIENT_FIELDS
     */
    NONPUBLIC_STATIC_FIELDS(null, null),
    /**
     * Whether fields with private/package/protected visibility, for which a respective getter method can be found, should be included.
     *
     * @see Option#PUBLIC_STATIC_FIELDS
     * @see Option#PUBLIC_NONSTATIC_FIELDS
     * @see Option#NONPUBLIC_STATIC_FIELDS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS
     * @see Option#TRANSIENT_FIELDS
     */
    NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS(null, FieldExclusionModule::forNonPublicNonStaticFieldsWithGetter),
    /**
     * Whether fields with private/package/protected visibility and no accompanying getter method should be included.
     *
     * @see Option#PUBLIC_STATIC_FIELDS
     * @see Option#PUBLIC_NONSTATIC_FIELDS
     * @see Option#NONPUBLIC_STATIC_FIELDS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS
     * @see Option#TRANSIENT_FIELDS
     */
    NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS(null, FieldExclusionModule::forNonPublicNonStaticFieldsWithoutGetter),
    /**
     * Whether {@code transient} fields should be included.
     *
     * @see Option#PUBLIC_STATIC_FIELDS
     * @see Option#PUBLIC_NONSTATIC_FIELDS
     * @see Option#NONPUBLIC_STATIC_FIELDS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS
     * @see Option#NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS
     */
    TRANSIENT_FIELDS(null, FieldExclusionModule::forTransientFields),
    /**
     * Whether methods that are {@code static} should be included.
     *
     * @see Option#VOID_METHODS
     * @see Option#GETTER_METHODS
     * @see Option#NONSTATIC_NONVOID_NONGETTER_METHODS
     */
    STATIC_METHODS(null, null),
    /**
     * Whether methods without return value (e.g. setters) should be included.
     *
     * @see Option#STATIC_METHODS
     * @see Option#GETTER_METHODS
     * @see Option#NONSTATIC_NONVOID_NONGETTER_METHODS
     */
    VOID_METHODS(null, MethodExclusionModule::forVoidMethods),
    /**
     * Whether getter methods should be included (assuming their fields are not included).
     *
     * @see Option#STATIC_METHODS
     * @see Option#VOID_METHODS
     * @see Option#NONSTATIC_NONVOID_NONGETTER_METHODS
     */
    GETTER_METHODS(null, MethodExclusionModule::forGetterMethods),
    /**
     * Whether methods that are (1) not {@code static}, (2) have a specific return value and (3) are not getters, should be included.
     *
     * @see Option#STATIC_METHODS
     * @see Option#VOID_METHODS
     * @see Option#GETTER_METHODS
     */
    NONSTATIC_NONVOID_NONGETTER_METHODS(null, MethodExclusionModule::forNonStaticNonVoidNonGetterMethods),
    /**
     * Whether an object's field/property should be deemed to be nullable if no specific check says otherwise.
     * <br>
     * Default: false (disabled)
     */
    NULLABLE_FIELDS_BY_DEFAULT(null, null),
    /**
     * Whether a method's return value should be deemed to be nullable if no specific check says otherwise.
     * <br>
     * Default: false (disabled)
     */
    NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT(null, null),
    /**
     * Whether all referenced objects should be listed in the schema's "definitions", otherwise single occurrences are defined in-line.
     * <br>
     * Default: false (disabled)
     */
    DEFINITIONS_FOR_ALL_OBJECTS(null, null);

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
     * @param enabledModuleProvider type of the module realising this setting/option if it is enabled
     * @param disabledModuleProvider type of the module realising this setting/option if it is disabled
     */
    private Option(Supplier<Module> enabledModuleProvider, Supplier<Module> disabledModuleProvider) {
        this.enabledModuleProvider = enabledModuleProvider;
        this.disabledModuleProvider = disabledModuleProvider;
    }

    /**
     * Retrieve the associated configuration changes as a module instance if possible (depending on particular setting/option: may return null).
     *
     * @param isEnabled whether the option is currently enabled
     * @return a module instance representing this setting/option's associated configurations (may be null)
     */
    Module getModule(boolean isEnabled) {
        Supplier<Module> targetModuleProvider = isEnabled ? this.enabledModuleProvider : this.disabledModuleProvider;
        return targetModuleProvider == null ? null : targetModuleProvider.get();
    }
}
