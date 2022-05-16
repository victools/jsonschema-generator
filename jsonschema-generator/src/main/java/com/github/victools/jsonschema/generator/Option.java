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

import com.github.victools.jsonschema.generator.impl.module.AdditionalPropertiesModule;
import com.github.victools.jsonschema.generator.impl.module.ConstantValueModule;
import com.github.victools.jsonschema.generator.impl.module.EnumModule;
import com.github.victools.jsonschema.generator.impl.module.FieldExclusionModule;
import com.github.victools.jsonschema.generator.impl.module.FlattenedOptionalModule;
import com.github.victools.jsonschema.generator.impl.module.FlattenedWrapperModule;
import com.github.victools.jsonschema.generator.impl.module.InlineSchemaModule;
import com.github.victools.jsonschema.generator.impl.module.MethodExclusionModule;
import com.github.victools.jsonschema.generator.impl.module.SimpleTypeModule;
import com.github.victools.jsonschema.generator.impl.module.SimplifiedOptionalModule;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration options to be set on a {@link SchemaGeneratorConfigBuilder} instance.
 */
public enum Option {
    /**
     * Whether the {@link SchemaKeyword#TAG_SCHEMA} attribute with {@link SchemaKeyword#TAG_SCHEMA_VALUE} should be included.
     */
    SCHEMA_VERSION_INDICATOR(null, null),
    /**
     * Whether additional types (and not just primitives and their associated classes should be included as fixed schema with a
     * {@link SchemaKeyword#TAG_TYPE} attribute of "string"/"boolean"/"integer"/"number".
     */
    ADDITIONAL_FIXED_TYPES(SimpleTypeModule::forPrimitiveAndAdditionalTypes, SimpleTypeModule::forPrimitiveTypes),
    /**
     * Whether enums should be treated as {@link SchemaKeyword#TAG_TYPE_OBJECT}, with all methods but {@link Enum#name() name()} being excluded.
     * <br>
     * This only takes effect if {@link Option#FLATTENED_ENUMS} and {@link Option#FLATTENED_ENUMS_FROM_TOSTRING} are disabled.
     *
     * @see Option#FLATTENED_ENUMS
     * @see Option#FLATTENED_ENUMS_FROM_TOSTRING
     */
    SIMPLIFIED_ENUMS(EnumModule::asObjects, null),
    /**
     * Whether enums should be treated as plain {@link SchemaKeyword#TAG_TYPE_STRING} values – derived from their respective constant name.
     * <br>
     * This only takes effect if {@link Option#FLATTENED_ENUMS_FROM_TOSTRING} is disabled but takes priority over {@link Option#SIMPLIFIED_ENUMS}.
     *
     * @see Option#FLATTENED_ENUMS_FROM_TOSTRING
     * @see Option#SIMPLIFIED_ENUMS
     */
    FLATTENED_ENUMS(EnumModule::asStringsFromName, null, Option.SIMPLIFIED_ENUMS),
    /**
     * Whether enums should be treated as plain {@link SchemaKeyword#TAG_TYPE_STRING} values – derived from their respective {@code toString()}.
     * <br>
     * This takes priority over both {@link Option#FLATTENED_ENUMS} and {@link Option#SIMPLIFIED_ENUMS}.
     *
     * @see Option#FLATTENED_ENUMS
     * @see Option#SIMPLIFIED_ENUMS
     */
    FLATTENED_ENUMS_FROM_TOSTRING(EnumModule::asStringsFromToString, null, Option.FLATTENED_ENUMS, Option.SIMPLIFIED_ENUMS),
    /**
     * Whether any {@link java.util.Optional Optional} instance should be reduced to an object with only three methods.
     * <br>
     * This only takes effect if {@link Option#FLATTENED_OPTIONALS} is disabled.
     *
     * @see SimplifiedOptionalModule#DEFAULT_INCLUDED_METHOD_NAMES
     */
    SIMPLIFIED_OPTIONALS(SimplifiedOptionalModule::new, null),
    /**
     * Whether any {@link java.util.Optional Optional} instance should be treated as nullable value of the wrapped type.
     * <br>
     * This takes priority over {@link Option#SIMPLIFIED_OPTIONALS}.
     *
     * @see Option#SIMPLIFIED_OPTIONALS
     */
    FLATTENED_OPTIONALS(FlattenedOptionalModule::new, null, Option.SIMPLIFIED_OPTIONALS),
    /**
     * Whether a {@link java.util.function.Supplier Supplier&lt;T&gt;} instance should be treated as the wrapped type {@code T}.
     *
     * @since 4.25.0
     */
    FLATTENED_SUPPLIERS(() -> new FlattenedWrapperModule<>(Supplier.class), null),
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
     * Without this option, all fields are deemed not-nullable unless a specific check says otherwise.
     */
    NULLABLE_FIELDS_BY_DEFAULT(null, null),
    /**
     * Whether a method's return value should be deemed to be nullable if no specific check says otherwise.
     * <br>
     * Without this option, all methods' return values are deemed not-nullable unless a specific check says otherwise.
     */
    NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT(null, null),
    /**
     * Whether a container/collection/array item should be subject to the standard nullable checks.
     * <br>
     * Without this option, no null values are allowed in any array.
     * <br>
     * Beware: with this option enabled, all nullable checks may need to consider the field/method's {@code isFakeContainerItemScope()} indication.
     *
     * @since 4.20.0
     */
    NULLABLE_ARRAY_ITEMS_ALLOWED(null, null),
    /**
     * Whether methods that do not expect any arguments, should be treated as fields: i.e. renaming the property.
     */
    FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS(null, null),
    /**
     * Whether a {@link java.util.Map Map&lt;K, V&gt;} schema's "additionalProperties" should be set to its declared value type {@code V}.
     *
     * @since 4.11.0
     */
    MAP_VALUES_AS_ADDITIONAL_PROPERTIES(AdditionalPropertiesModule::forMapValues, null),
    /**
     * Whether allowed values should always be included in an {@link SchemaKeyword#TAG_ENUM "enum"} keyword. If there is exactly one allowed value, it
     * will otherwise be represented by a {@link SchemaKeyword#TAG_CONST "const"} keyword instead.
     *
     * @since 4.16.0
     */
    ENUM_KEYWORD_FOR_SINGLE_VALUES(null, null),
    /**
     * Whether a schema's "additionalProperties" should be set to "false" if no specific configuration says otherwise.
     * <br>
     * Without this option, i.e. by default, the "additionalProperties" keyword will be omitted and thereby allowing any additional properties in an
     * object schema.
     *
     * @since 4.2.0
     */
    FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT(AdditionalPropertiesModule::forbiddenForAllObjectsButContainers, null),
    /**
     * Whether all referenced objects should be listed in the schema's "definitions"/"$defs".
     * <br>
     * Without this option, only those subschemas will be "$ref"-erenced if they occur more than once – in-lining everything else.
     */
    DEFINITIONS_FOR_ALL_OBJECTS(null, null),
    /**
     * Whether the schema for the target/main type should be included in the "definitions"/"$defs" – thereby avoiding an potential {@code "$ref": "#"}
     * and assigning a name to it like for all other defined subschemas.
     * <br>
     * Otherwise, "$ref"-erences to the main/target schema will use the empty fragment ("#") and it will not be listed in the "definitions"/"$defs".
     * <p>
     * Beware: this only results in a valid schema from {@link SchemaVersion#DRAFT_2019_09} onward. Before that, everything besides "$ref" would be
     * ignored.
     * </p>
     *
     * @since 4.11.0
     */
    DEFINITION_FOR_MAIN_SCHEMA(null, null),
    /**
     * Whether all sub-schemas should be defined in-line, i.e. including no "definitions"/"$defs". This takes precedence over
     * {@link #DEFINITIONS_FOR_ALL_OBJECTS} and {@link #DEFINITION_FOR_MAIN_SCHEMA}.
     * <p>
     * Beware: This will result in an exception being thrown if a single circular reference is being encountered!
     * </p>
     *
     * @since 4.10.0
     */
    INLINE_ALL_SCHEMAS(InlineSchemaModule::new, null, Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.DEFINITION_FOR_MAIN_SCHEMA),
    /**
     * Generally, keys in the collected "definitions"/"$defs" are ensured to be URI compatible but may include parentheses and commas for listing type
     * parameters. By enabling this option, these parentheses and commas will be removed to conform with a reduced set of characters, e.g. as expected
     * by OpenAPI: following the regular expression {@code ^[a-zA-Z0-9\.\-_]+$}.
     *
     * @since 4.11.0
     */
    PLAIN_DEFINITION_KEYS(null, null),
    /**
     * For the "format" attribute, JSON Schema defines various supported values. The OpenAPI specification assigns a few more of those in order to
     * differentiate between standard data types (e.g. float vs. double) and even some more fixed data types (e.g. LocalDate, LocalDateTime) if
     * {@link #ADDITIONAL_FIXED_TYPES} is enabled. By enabling this option, these extra "format" values will be set for the respective
     * primitive/standard data types automatically.
     *
     * @since 4.15.0
     */
    EXTRA_OPEN_API_FORMAT_VALUES(null, null),
    /**
     * Whether as the last step of the schema generation, unnecessary "allOf" elements (i.e. where there are no conflicts/overlaps between the
     * contained sub-schemas) should be merged into one, in order to make the generated schema more readable. This also applies to manually added
     * "allOf" elements, e.g. through custom definitions or attribute overrides.
     *
     * @since 4.6.0
     */
    ALLOF_CLEANUP_AT_THE_END(null, null);

    /**
     * Optional: the module realising the setting/option if it is enabled.
     */
    private final Supplier<Module> enabledModuleProvider;
    /**
     * Optional: the module realising the setting/option if it is disabled.
     */
    private final Supplier<Module> disabledModuleProvider;
    /**
     * Other options being ignored if this one is enabled.
     */
    private final Set<Option> overriddenOptions;

    /**
     * Constructor.
     *
     * @param enabledModuleProvider type of the module realising this setting/option if it is enabled
     * @param disabledModuleProvider type of the module realising this setting/option if it is disabled
     * @param overriddenOptions other options being ignored if this one is enabled
     */
    private Option(Supplier<Module> enabledModuleProvider, Supplier<Module> disabledModuleProvider, Option... overriddenOptions) {
        this.enabledModuleProvider = enabledModuleProvider;
        this.disabledModuleProvider = disabledModuleProvider;
        if (overriddenOptions == null || overriddenOptions.length == 0) {
            this.overriddenOptions = Collections.emptySet();
        } else {
            this.overriddenOptions = Stream.of(overriddenOptions).collect(Collectors.toSet());
        }
    }

    /**
     * Check whether the given option is being ignored if this one enabled.
     *
     * @param otherOption option that may be ignored
     * @return whether the given option is being ignored in case of this one being enabled
     */
    public boolean isOverriding(Option otherOption) {
        return this.overriddenOptions.contains(otherOption);
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
