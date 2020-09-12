# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### `jsonschema-generator`
#### Added
- New `Option.EXTRA_OPEN_API_FORMAT_VALUES` to support automatic inclusion of `"format"` values for certain simple/fixed types

## [4.14.0] - 2020-08-02
### `jsonschema-generator`
#### Added
- Entries in `SchemaKeyword` enum for `"not"`, `"minProperties"`, `"maxProperties"` (without further handling)

### `jsonschema-module-swagger-2`
#### Changed
- Make use of new `SchemaKeyword` enum entries instead of hard-coded strings (no change in behaviour)

### `jsonschema-maven-plugin`
#### Added
- Support for including classes via glob patterns in `<classNames>` and `<packageNames>` (in addition to absolute paths)
- Support for excluding classes via absolute paths or glob patterns in new `<excludeClassNames>`

#### Fixed
- Explicitly include dependencies of supported generator modules that they expect to be provided
- Avoid generating the same schema multiple times if there are overlaps between entries in `<classNames>` and/or `<packageNames>`

## [4.13.0] - 2020-06-27
### `jsonschema-generator`
#### Added
- Possibility to configure `SchemaDefinitionNamingStrategy` via `configBuilder.forTypesInGeneral().withDefinitionNamingStrategy()`
- Explicit `DefaultSchemaDefinitionNamingStrategy` that is being applied if no other `SchemaDefinitionNamingStrategy` is being configured
- New `Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS` to allow deriving fields from getter methods

#### Changed
- Determine names/keys of subschemas in `definitions`/`$defs` through configurable `SchemaDefinitionNamingStrategy`
- Default property sort order to consider property name when differentiating fields and methods (now that methods may have field-like names)

#### Fixed
- For multiple definitions for the same type, they might be unnecessarily treated as having conflicting definition names requiring resolution

#### Removed
- Obsolete `TypeContext.getSchemaDefinitionName()`

### `jsonschema-module-swagger-2`
#### Added
- Initial implementation of `Swagger2Module` for deriving schema attributes from OpenAPI `@Schema` annotations.

### `jsonschema-maven-plugin`
#### Added
- Support for new `jsonschema-module-swagger-2`

## [4.12.2] - 2020-06-10
### `jsonschema-generator`
#### Fixed
- Performance: Cache looked-up getter in `FieldScope.findGetter()`
- Performance: Cache looked-up field in `MethodScope.findGetterField()`

### `jsonschema-maven-plugin`
#### Fixed
- Collecting all classes from a given `<packageName>` even if it contains classes whose super types are not included

## [4.12.1] - 2020-05-28
### `jsonschema-maven-plugin`
#### Fixed
- Setting correct contextual classpath for class/package look-up via reflections

## [4.12.0] - 2020-05-10
### `jsonschema-generator`
#### Added
- New `SchemaGeneratorGeneralConfigPart.withPropertySorter()` exposing the sorting logic of an object schema's properties

### `jsonschema-module-jackson`
#### Added
- New `JacksonOption.RESPECT_JSONPROPERTY_ORDER` to sort properties in an object's schema based on `@JsonPropertyOrder` annotations
- New `JacksonOption.IGNORE_PROPERTY_NAMING_STRATEGY` to skip the adjustment of property names based on `@JsonNaming` annotations

#### Changed
- Consider `@JsonNaming` annotations to alter the names of contained fields according to the specified `PropertyNamingStrategy`

## [4.11.1] - 2020-04-30
### `jsonschema-maven-plugin`
#### Fixed
- Maven plugin is unable to resolve runtime dependencies (#95)

## [4.11.0] - 2020-04-28
### `jsonschema-generator`
#### Added
- New `Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES` to include `additionalProperties` with a schema for each `Map<K, V>`'s value type `V`
- New `Option.DEFINITION_FOR_MAIN_SCHEMA` to allow moving main/target schema into `definitions`/`$defs`
- New `Option.PLAIN_DEFINITION_KEYS` to ensure keys in `definitions`/`$defs` adhere to the reduced set of allowed characters expected by OpenAPI
- New `SchemaGenerator.buildMultipleSchemaDefinitions()` allowing to generate JSON Schema elements (e.g. for building an OpenAPI description)

#### Fixed
- Skip fields for which `isFakeContainerItemScope()` returns `true`, when fulfilling value collection for `Option.VALUES_FROM_CONSTANT_FIELDS`
- Treat `Byte`/`byte` as `"type": "string"` and not as `"type": "integer"` by default

### `jsonschema-maven-plugin`
#### Added
- Initial implementation
- Support schema generation from one or multiple classes
- Support schema generation for all classes in one or multiple packages
- Allow configuration of target schema version
- Allow configuration of `OptionPreset` (also allowing for `NONE`)
- Allow configuration of individual `Option`s to be enabled/disabled
- Allow configuration of standard modules by name, with possible list of module options
- Allow configuration of custom modules by class name

## [4.10.0] - 2020-04-12
### `jsonschema-generator`
#### Added
- Official support for Draft 6 (via new `SchemaVersion.DRAFT_6`)
- New `Option.INLINE_ALL_SCHEMAS` to enforce no `definitions`/`$defs` to be produced (throwing exception if there is at least one circular reference)
- Offering also `SchemaGenerationContext.createStandardDefinition(FieldScope, CustomPropertyDefinitionProvider)`
- Offering also `SchemaGenerationContext.createStandardDefinition(MethodScope, CustomPropertyDefinitionProvider)`
- Alternative `SchemaGenerationConfigPart.withInstanceAttributeOverride(InstanceAttributeOverrideV2)` with access to `SchemaGenerationContext`

#### Changed
- Enhance `Option.ALLOF_CLEANUP_AT_THE_END` to also reduce `allOf` if multiple parts have the same attributes but with equal values (except for `if` tags)
- Providing access to the `SchemaGenerationContext` when invoking `TypeAttributeOverrideV2` (potentially BREAKING change in case of lambda usage)

#### Deprecated
- `SchemaGenerationConfigPart.withInstanceAttributeOverride(InstanceAttributeOverride)` without access to `SchemaGenerationContext`
- `InstanceAttributeOverride` interface without access to `SchemaGenerationContext`
- `TypeAttributeOverride` interface with only access to `SchemaGenerationConfig` and not `SchemaGenerationContext`
- Ambiguous `SchemaGeneratorConfigBuilder.with(CustomDefinitionProviderV2)`
- Ambiguous and outdated `SchemaGeneratorConfigBuilder.with(TypeAttributeOverride)`

### `jsonschema-module-jackson`
#### Added
- New `JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY` to allow enum serialization based on each constant's `@JsonProperty` value

## [4.9.0] - 2020-04-02
### `jsonschema-generator`
#### Added
- Convenience constructors on `SchemaGeneratorConfigBuilder` without explicit `ObjectMapper` instance being expected
- Convenience methods on `FieldScope`/`MethodScope` for accessing (first level) container item annotations: `getContainerItemAnnotation()` and `getContainerItemAnnotationConsideringFieldAndGetter()`

#### Changed
- `MethodScope.getAnnotation()` now also considers annotations directly on return type (when no matching annotation was found on the method itself)

#### Fixed
- Attributes set in custom property definitions should not be overridden by other configurations (except for explicit attribute overrides)

### `jsonschema-module-javax-validation`
#### Changed
- Consider (first level) container item annotations (e.g. `List<@Size(min = 3) String>`)

## [4.8.1] - 2020-03-31
### All
#### Fixed
- Include parent `pom.xml` when publishing release

### `jsonschema-module-swagger-1.5`
#### Fixed
- Error when encountering container/collection property

## [4.8.0] - 2020-03-30
### `jsonschema-generator`
#### Added
- Support for custom definitions in the scope of a particular field/method via `SchemaGeneratorConfigPart.withCustomDefinitionProvider()`
- Ability to opt-out of normal "attribute collection" for custom definitions through new `CustomDefinition` constructor parameters

#### Changed
- Consolidate `anyOf` entries that only contain an `anyOf` themselves into the outer `anyOf` (mostly relevant for nullable entries with subtypes)
- If a field/method is of a container type: apply the per-property configurations on its items; with `MemberScope.isFakeContainerItemScope()` flag

#### Fixed
- Should consider per-type attributes even on inline custom definitions
- Use less strict `anyOf` instead of `oneOf` when indicating that a sub-schema may be of `"type": "null"`

#### Dependency Update
- `com.fasterxml.jackson.core`:`jackson-core`/`jackson-databind` from `2.10.2` to `2.10.3`
- Remove dependencies to `log4j` implementation (only `slf4j-api` remains)

### `jsonschema-module-jackson`
#### Added
- Look-up subtypes according to `@JsonTypeInfo` and `@JsonSubTypes` annotations per-type or overridden per-property:
    - Considering `@JsonTypeInfo.include` with values `As.PROPERTY`, `As.EXISTING_PROPERTY`, `As.WRAPPER_ARRAY`, `As.WRAPPER_OBJECT`
    - Considering `@JsonTypeInfo.use` with values `Id.NAME` (from `@JsonTypeName`) and `Id.CLASS`
- New `JacksonOption.SKIP_SUBTYPE_LOOKUP` for disabling the new look-up of subtypes (i.e. to regain previous behaviour) if required
- New `JacksonOption.IGNORE_TYPE_INFO_TRANSFORM` for disabling addition of extra property or wrapping in an array/object according to `@JsonTypeInfo`

## [4.7.0] - 2020-03-20
### `jsonschema-generator`
#### Added
- Support for multiple target type overrides at once per field/method
- Support for "$id" property via `SchemaGeneratorGeneralConfigPart.withIdResolver()`
- Support for "$anchor" property via `SchemaGeneratorGeneralConfigPart.withAnchorResolver()`

#### Fixed
- Allow for multiple types with the same name (but different package) instead of picking one at random
- Allow for multiple definitions for the same type (e.g. in case of a custom definition wrapping the standard definition)

#### Deprecated
- Configuration option for single target type override
- Look-up of single target type override from configuration

### `jsonschema-module-jackson`
#### Changed
- Ignore/exclude properties marked with `@JsonBackReference`

## [4.6.0] - 2020-03-15
### `jsonschema-generator`
#### Added
- Explicit indication of targeted JSON Schema version (for now: Draft 7 or 2019-09)
- Support for renamed keywords between Draft versions through new `SchemaKeyword` enum (replacing static `SchemaConstants` interface)
- Support for `$ref` alongside other attributes (from Draft 2019-09 onwards) thereby reducing number of `allOf` elements
- Introduce `Option.ALLOF_CLEANUP_AT_THE_END` (also included in all standard `OptionPreset`s) for toggling-off this improvement if not desired

#### Changed
- Reduce number of `allOf` elements in generated schema when contained schema parts are distinct (and in case of Draft 7: don't contain `$ref`)

#### Deprecated
- `SchemaConstants` interface containing static `String` constants for tag names/values (in favour of version-aware `SchemaKeyword` enum)
- Internal `AttributeCollector` API without generation context as parameter
- `SchemaGeneratorConfigBuilder` constructors without explicit JSON Schema version indication

#### Dependency Update
- `com.fasterxml.jackson.core`:`jackson-core`/`jackson-databind` from `2.10.0` to `2.10.2`
- `com.fasterxml`:`classmate` from `1.5.0` to `1.5.2`
- Optional: `org.slf4j`:`slf4j-api` from `1.7.26` to `1.7.30`
- Optional: `org.apache.logging.log4j`:`log4j-api`/`log4j-core`/`log4j-slf4j-impl` from `2.11.2` to `2.13.0`

## [4.5.0] - 2020-03-05
### `jsonschema-generator`
#### Added
- Expose `SchemaGenerationContext.createDefinitionReference()` to custom definitions to enable circular references within
- Expose `SchemaGenerationContext.createStandardDefinitionReference()` to custom definitions to enable circular references within

### `jsonschema-module-jackson`
#### Added
- New `JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE` for considering `@JsonValue` annotations on enums, similar to `Option.FLATTENED_ENUMS`

## [4.4.0] - 2020-03-02
### `jsonschema-generator`
#### Added
- Enable declaration of subtypes through `withSubtypeResolver(SubtypeResolver)` on `forTypesInGeneral()` (#24)

#### Changed
- Move custom definitions and type attribute overrides into `forTypesInGeneral()` (while preserving delegate setters on config builder)

## [4.3.0] - 2020-02-28
### `jsonschema-generator`
#### Changed
- Limit collected type attributes by declared "type" (prior to any `TypeAttributeOverride`!)

#### Fixed
- Not declare any "type" for `Object.class` by default

## [4.2.0] - 2020-02-27
### `jsonschema-generator`
#### Added
- Support for "additionalProperties" property via `SchemaGeneratorTypeConfigPart.withAdditionalPropertiesResolver()`
- Support for "patternProperties" property via `SchemaGeneratorTypeConfigPart.withPatternPropertiesResolver()`
- Introduce new `Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT` for more convenient usage
- Offer `TypeScope.getTypeParameterFor()` and `TypeContext.getTypeParameterFor()` convenience methods

#### Fixed
- Possible exceptions in case of encountered collections without specific type parameters

## [4.1.0] - 2020-02-18
### `jsonschema-generator`
#### Added
- New `Option.FLATTENED_ENUMS_FROM_TOSTRING`, using `toString()` instead of `name()` as per `Option.FLATTENED_ENUMS`

## [4.0.2] - 2020-01-30
### `jsonschema-generator`
#### Fixed
- Avoid further characters in definition keys that are not URI-compatible (#19)

## [4.0.1] - 2020-01-29
### `jsonschema-generator`
#### Fixed
- Avoid white-spaces in definition keys

## [4.0.0] - 2020-01-03
### `jsonschema-generator`
#### Added
- Extended API for defining context independent collection of schema attributes (for array/collection items that are not directly under a member)

#### Changed
- BREAKING CHANGE: `TypeAttributeOverride`'s second parameter is now the new `TypeScope` wrapper instead of just a `ResolvedType`

### `jsonschema-module-jackson`
#### Changed
- Look-up descriptions from `@JsonClassDescription` via new `forTypesInGeneral()` API

### `jsonschema-module-swagger-1.5`
#### Changed
- Look-up titles and descriptions from `@ApiModel` via new `forTypesInGeneral()` API

## [3.5.0] - 2020-01-01
### `jsonschema-generator`
#### Added
- `CustomDefinitionProviderV2` with access to `SchemaGenerationContext`, e.g. to allow continuing normal schema generation for nested properties

#### Deprecated
- `CustomDefinitionProvider` receiving only the `TypeContext` as parameter

#### Fixed
- Possible `IllegalAccess` when loading constant values should just be ignored

## [3.4.1] - 2019-12-30
### `jsonschema-generator`
#### Fixed
- Collected attributes should also be applied to container types (Issue #15)

## [3.4.0] - 2019-11-29
### `jsonschema-generator`
#### Added
- Introduce convenience function `MemberScope.getAnnotationConsideringFieldAndGetter(Class)`

### `jsonschema-module-swagger-1.5`
#### Added
- Optionally provide a field/method's "title" as per `@ApiModel(value = ...)`
- Allow to ignore the general `@ApiModel(description = ...)` when populating a field/method's "description"

## [3.3.0] - 2019-10-25
### `jsonschema-generator`
#### Fixed
- Increase dependency version for jackson-databind (and jackson-core) to resolve security alerts.
- Avoid unnecessary quotes when representing constant string values (due to changed behaviour in jackson >=2.10.0)

## [3.2.0] – 2019-09-01
### `jsonschema-generator`
#### Added
- In `SchemaGenerator.generateSchema(Type)` also allow passing type parameters via `SchemaGenerator.generateSchema(Type, Type...)`.
- Support for "required" property via `SchemaGeneratorConfigPart.withRequiredCheck()` (PR #5).
- Support for "default" property via `SchemaGeneratorConfigPart.withDefaultResolver()` (PR #5).
- Support for "pattern" property via `SchemaGeneratorConfigPart.withStringPatternResolver()` (Issue #9).

### `jsonschema-module-javax-validation`
#### Added
- Option for treating not-nullable fields as "required" in their parent type
- Option for treating not-nullable methods as "required" in their parent type
- Indicate a string's "format" to be "email" if `@Email` is present
- Option for returning "idn-email" instead of "email" as "format" if `@Email` is present
- Indicate a string's "pattern" according to regular expressions on `@Pattern` or `@Email` (ignoring specified flags)
- Option for enabling the inclusion of "pattern" expressions (they are excluded by default)
- Allow filtering applicable annotations by their declared validation `groups` via `JavaxValidationModule.forValidationGroups()`

## [3.1.0] – 2019-06-18
### `jsonschema-generator`
#### Changed
- Use `Class.getTypeName()` instead of `Class.getName()` in `TypeContext.getFullTypeDescription()`.

#### Added
- In `TypeContext.resolve(Type)` also allow passing type parameters via `TypeContext.resolve(Type, Type...)`.

#### Fixed
- `NullPointerException` on `MemberScope` representing `void` methods.
- `IndexOutOfBoundsException` when determining container item type of raw `Collection`.

## [3.0.0] – 2019-06-10
### `jsonschema-generator`
#### Changed
- Simplify configuration API to be based on `FieldScope`/`MethodScope` respectively.
- Consolidate some utility functions into `FieldScope`/`MethodScope`.
- Consolidate logic for determining whether a type is a container into `TypeContext`.
- Consolidate naming logic for schema "definitions" into `TypeContext`.
- Add `TypeContext` argument to `CustomDefinitionProvider` interface.
- Remove `SchemaGeneratorConfig` argument from `InstanceAttributeOverride` interface.

#### Added
- Allow for sub-typing of `FieldScope`/`MethodScope` and `TypeContext` (in case offered configuration options are insufficient).

#### Removed
- Remove support for `Option.GETTER_ATTRIBUTES_FOR_FIELDS` and `Option.FIELD_ATTRIBUTES_FOR_GETTERS`, rather let configurations/modules decide individually and avoid potential endless loop.

### `jsonschema-module-jackson`
#### Added
- Populate "description" as per `@JsonPropertyDescription` (falling-back on `@JsonClassDescription`).
- Apply alternative field names defined in `@JsonProperty` annotations.
- Ignore fields that are deemed to be ignored according to various `jackson-annotations` (e.g. `@JsonIgnore`, `@JsonIgnoreType`, `@JsonIgnoreProperties`) or are otherwise supposed to be excluded.

### `jsonschema-module-javax-validation`
#### Added
- Consider the same validation annotations on a getter method also for its field
- Consider the same validation annotations on a field also for its getter method

### `jsonschema-module-swagger-1.5`
#### Added
- Optionally override a field's property name with `@ApiModelProperty(name = ...)`
- Optionally ignore a field/method if `@ApiModelProperty(hidden = true)`
- Provide a field/method's "description" as per `@ApiModelProperty(value = ...)` or `@ApiModel(description = ...)`
- Indicate a number's (field/method) "minimum" (inclusive) according to `@ApiModelProperty(allowableValues = "range[...")`
- Indicate a number's (field/method) "exclusiveMinimum" according to `@ApiModelProperty(allowableValues = "range(...")`
- Indicate a number's (field/method) "maximum" (inclusive) according to `@ApiModelProperty(allowableValues = "range...]")`
- Indicate a number's (field/method) "exclusiveMaximum" according to `@ApiModelProperty(allowableValues = "range...)")`
- Indicate a field/method's "const"/"enum" as `@ApiModelProperty(allowableValues = ...)` (if it is not a numeric range declaration)
- Consider the `@ApiModelProperty` annotation on a getter method also for its field
- Consider the `@ApiModelProperty` annotation on a field also for its getter method

## [2.0.0] – 2019-06-07
### `jsonschema-generator`
#### Changed
- Removed type resolution and replaced it with `org.fasterxml:classmate` dependency.
- Adjusting configuration API to use `classmate` references for types/fields/methods.

#### Fixed
- Ignore complex constant values that may not be properly representable as JSON.

## [1.0.2] - 2019-05-30
### `jsonschema-generator`
#### Fixed
- Increase dependency version for jackson-databind to resolve security alert.

## [1.0.1] - 2019-05-19
### `jsonschema-generator`
#### Fixed
- Specified "test" scope for dependency on jsonassert.

## [1.0.0] - 2019-05-18
### `jsonschema-generator`
#### Added
- Reflection-based JSON Schema Generator.
- Support of generics and the resolution of their type boundaries in the respective scope.
- Inclusion of any fields and public methods in a generated JSON Schema.
- Ability to define a fixed JSON Schema per type (e.g. for determining how to represent primitive types).
- Ability to customise various aspects of schema generation and assigned attributes.
- Concept of modules (e.g. for sub-libraries) to define a single entry-point for applying individual configurations.
- Standard enum of common configuration options.
- Specific handling of enums (two alternatives via standard options).
- Specific handling of optionals (two alternatives via standard options).
- Pre-defined sets of standard options to cover different use-cases and simplify library usage.

### `jsonschema-module-javax-validation`
#### Added
- Indicate a field/method to be nullable if `@Null` is present
- Indicate a field/method to be not nullable if `@NotNull`, `@NotEmpty` or `@NotBlank` is present
- Indicate an array's "minItems" according to `@Size` or `@NotEmpty`
- Indicate an array's "maxItems" according to `@Size`
- Indicate a string's "minLength" according to `@Size`, `@NotEmpty` or `@NotBlank`
- Indicate a string's "maxLength" according to `@Size`
- Indicate a number's "minimum" (inclusive) according to `@Min`, `@DecimalMin` or `@PositiveOrZero`
- Indicate a number's "exclusiveMinimum" according to `@DecimalMin` or `@Positive`
- Indicate a number's "maximum" (inclusive) according to `@Max`, `@DecimalMax` or `@NegativeOrZero`
- Indicate a number's "exclusiveMaximum" according to `@DecimalMax` or `@Negative`


[Unreleased]: https://github.com/victools/jsonschema-generator/compare/v4.14.0...HEAD
[4.14.0]: https://github.com/victools/jsonschema-generator/compare/v4.13.0...v4.14.0
[4.13.0]: https://github.com/victools/jsonschema-generator/compare/v4.12.2...v4.13.0
[4.12.2]: https://github.com/victools/jsonschema-generator/compare/v4.12.1...v4.12.2
[4.12.1]: https://github.com/victools/jsonschema-generator/compare/v4.12.0...v4.12.1
[4.12.0]: https://github.com/victools/jsonschema-generator/compare/v4.11.1...v4.12.0
[4.11.1]: https://github.com/victools/jsonschema-generator/compare/v4.11.0...v4.11.1
[4.11.0]: https://github.com/victools/jsonschema-generator/compare/v4.10.0...v4.11.0
[4.10.0]: https://github.com/victools/jsonschema-generator/compare/v4.9.0...v4.10.0
[4.9.0]: https://github.com/victools/jsonschema-generator/compare/v4.8.1...v4.9.0
[4.8.1]: https://github.com/victools/jsonschema-generator/compare/v4.8.0...v4.8.1
[4.8.0]: https://github.com/victools/jsonschema-generator/compare/v4.7.0...v4.8.0
[4.7.0]: https://github.com/victools/jsonschema-generator/compare/v4.6.0...v4.7.0
[4.6.0]: https://github.com/victools/jsonschema-generator/compare/v4.5.0...v4.6.0
[4.5.0]: https://github.com/victools/jsonschema-generator/compare/v4.4.0...v4.5.0
[4.4.0]: https://github.com/victools/jsonschema-generator/compare/v4.3.0...v4.4.0
[4.3.0]: https://github.com/victools/jsonschema-generator/compare/v4.2.0...v4.3.0
[4.2.0]: https://github.com/victools/jsonschema-generator/compare/v4.1.0...v4.2.0
[4.1.0]: https://github.com/victools/jsonschema-generator/compare/v4.0.2...v4.1.0
[4.0.2]: https://github.com/victools/jsonschema-generator/compare/v4.0.1...v4.0.2
[4.0.1]: https://github.com/victools/jsonschema-generator/compare/v4.0.0...v4.0.1
[4.0.0]: https://github.com/victools/jsonschema-generator/compare/v3.5.0...v4.0.0
[3.5.0]: https://github.com/victools/jsonschema-generator/compare/v3.4.1...v3.5.0
[3.4.1]: https://github.com/victools/jsonschema-generator/compare/v3.4.0...v3.4.1
[3.4.0]: https://github.com/victools/jsonschema-generator/compare/v3.3.0...v3.4.0
[3.3.0]: https://github.com/victools/jsonschema-generator/compare/v3.2.0...v3.3.0
[3.2.0]: https://github.com/victools/jsonschema-generator/compare/v3.1.0...v3.2.0
[3.1.0]: https://github.com/victools/jsonschema-generator/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/victools/jsonschema-generator/compare/v2.0.0...v3.0.0
[2.0.0]: https://github.com/victools/jsonschema-generator/compare/v1.0.2...v2.0.0
[1.0.2]: https://github.com/victools/jsonschema-generator/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/victools/jsonschema-generator/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/victools/jsonschema-generator/releases/tag/v1.0.0
