# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### `jsonschema-module-jackson`
#### Fixed
- support `@JacksonAnnotationsInside` annotated combo annotations also when looking for `@JsonPropertyDescription`  

## [4.38.0] - 2025-03-24
### `jsonschema-generator`
#### Changed
- avoid duplicate entries in `required` array when performing final clean-ups

### `jsonschema-module-swagger2`
#### Fixed
- respect `Option.NULLABLE_FIELDS_BY_DEFAULT`/`Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT` for fields/methods without `@Schema` annotation

## [4.37.0] - 2024-11-11
### `jsonschema-generator`
#### Added
- new `Option.NULLABLE_ALWAYS_AS_ANYOF` that avoids the `"null"` type being included with other type values, e.g. `"type": ["object", "null"]`

#### Changed
- apply property name overrides before triggering the ignore check (i.e., provide both the declared and overridden property names if there is one)
- update various (runtime/test/build-time) dependencies

#### Fixed
- avoid exception when trying to collect supported enum values from raw `Enum` type (i.e., missing type parameter)
- avoid exception when trying to find type with annotation when given type is `null`

### `jsonschema-module-jackson`
#### Added
- support `@JacksonAnnotationsInside` annotated combo annotations

#### Fixed
- avoid exception in subtype resolution, when targeting void method
- check for ignored properties excluded fields when a property name override makes it conflict with a non-conventional getter method

### `jsonschema-maven-plugin`
#### Added
- support `<skipAbstractTypes>` flag to exclude abstract types (not interfaces)
- support `<skipInterfaces>` flag to exclude interface types

### `jsonschema-module-microprofile-openapi-3`
#### Added
***NOTE: `org.eclipse.microprofile.openapi:microprofile-openapi-api` minimum version is `3.1.1`!***
- Initial implementation of `MicroProfileOpenApi3Module` for deriving schema attributes from MicroProfile OpenAPI 3 `@Schema` annotations.

## [4.36.0] - 2024-07-20
### `jsonschema-generator`
#### Added
- new `Option.ACCEPT_SINGLE_VALUE_AS_ARRAY` to support Jackson `DeserializationFeature` of the same name, i.e., when an array type is declared, an instance of a single item should also be accepted by the schema

#### Changed
- consider `Boolean` values as valid in `const`/`enum` (i.e., no longer ignore them)

### `jsonschema-module-jakarta-validation`
#### Added
- populate `const`/`enum` based on `@AssertTrue`/`@AssertFalse`

## [4.35.0] - 2024-03-29
### `jsonschema-generator`
#### Added
- check for custom definitions for `void` methods (this may result in exceptions inside custom configuration if a `null` return type is not considered)

#### Changed
- if present, apply custom definition for `void` methods

## [4.34.0] - 2024-03-14
### `jsonschema-generator`
#### Added
- new `Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END` discard duplicate elements from member sub-schemas

#### Changed
- new `Option.DUPLICATE_MEMBER_ATTRIBUTE_CLEANUP_AT_THE_END` by default included in standard `OptionPreset`s

### `jsonschema-module-jackson`
#### Fixed
- `@JsonUnwrapped` annotation on inherited properties resulted in those properties being ignored instead of being unwrapped

## [4.33.1] - 2023-12-19
### `jsonschema-module-jackson`
#### Fixed
- Respect `@JsonPropertyOrder` also for properties derived from non-getter methods

## [4.33.0] - 2023-11-23
### `jsonschema-generator`
#### Added
- new `Option.STANDARD_FORMATS` includes standard `"format"` values to some types considered by `Option.ADDITIONAL_FIXED_TYPES`
- new `Option.INLINE_NULLABLE_SCHEMAS` avoids `"<type>-nullable"` entries in the `"definitions"`/`"$defs"`

#### Changed
- include new `Option.STANDARD_FORMATS` in `OptionPreset.PLAIN_JSON` by default
- extended parameters for creation of `FieldScope`/`MethodScope` through the `TypeContext` to include type for which a schema is being generated

#### Fixed
- when using `Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS` on a method where the second character of the derived field name is in uppercase, don't capitalise the first character

## [4.32.0] - 2023-10-27
### `jsonschema-generator`
#### Added
- offer `SchemaGeneratorConfigBuilder.withObjectMapper()`; mainly for use in custom modules in combination with the Maven plugin, where the constructor parameter cannot be used instead

#### Changed
- consider JavaBeans API specification in getter naming convention for field names with the second character being uppercase (e.g., a field `xIndex` has the getter `getxIndex()` according to the specification)
- allow for field names starting with `is` to have a getter of the same name (e.g., a field `isBool` may have the getter `isBool()`)
- the default `ObjectMapper` instance now includes the enabled `SerializationFeature.INDENT_OUTPUT`

### `jsonschema-module-jackson`
#### Added
- elevate nested properties to the parent type where members are annotated with `@JsonUnwrapped`

### `jsonschema-module-swagger-2`
***NOTE: `io.swagger.core.v3:swagger-annotations` minimum version is now `2.2.5`!***
#### Added
- consider `@Schema(additionalProperties = ...)` attribute (only values `TRUE` and `FALSE`), when it is annotated on a type (not on a member)
- consider `@Schema(requiredMode = REQUIRED)` in addition to deprecated `@Schema(required = true)`

#### Fixed
- avoid rounding error when taking over the value from `@Schema(multipleOf)`

### `jsonschema-maven-plugin`
### Added
- support custom configuration `Module` being loaded from test classpath elements 

### Changed
- a generated schema is now serialized through the configuration's `ObjectMapper` instance (e.g., granting control over pretty printing or even generating YAML instead of JSON files)

## [4.31.1] - 2023-04-28
### `jsonschema-generator`
#### Fixed
- avoid error being thrown in `allOf` clean-up for invalid payload

## [4.31.0] - 2023-04-22
### `jsonschema-generator`
#### Added
- extend `TypeContext` creation to support configuration of differing annotation inheritance by annotation type

### `jsonschema-module-jakarta-validation`
#### Fixed
- consider inheritance of validation constraint annotations
- when limiting scope to validation groups, also consider a specified group's supertypes 

### `jsonschema-module-javax-validation`
#### Fixed
- consider inheritance of validation constraint annotations
- when limiting scope to validation groups, also consider a specified group's supertypes

## [4.30.0] - 2023-04-16
### `jsonschema-generator`
#### Added
- introduce configuration option for `dependentRequired` keyword
- introduce new `Option.STRICT_TYPE_INFO` for implying the `type` of sub-schemas based on their contained attributes (note: implied "type" array always contains "null")
- extend convenience methods for annotation lookup, to consider meta annotations (annotations on annotations)

#### Changed
- enable `allOf` clean-up when any of the following keywords are contained: `dependentRequired`/`dependentSchemas`/`prefixItems`/`unevaluatedItems`/`unevaluatedProperties`
- extend consideration of sub-schemas for `allOf` clean-up to more recognized keywords

### `jsonschema-module-jackson`
#### Added
- introduce new `JacksonOption.INLINE_TRANSFORMED_SUBTYPES` in order to avoid definitions with `-1`/`-2` suffixes being generated in case of subtypes involving transformation (e.g., additional property, wrapping array, wrapping object)  
  To be used with care, as a recursive reference can cause a `StackOverflowError`. In some scenarios, such an error can be avoided by also enabling the `Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES`.

#### Fixed
- use `prefixItems` instead of `items` keyword (from Draft 2019-09 onward) for tuples in `WRAPPING_ARRAY` subtype definitions 

### `jsonschema-module-jakarta-validation`
#### Added
- support meta annotations (validation annotations on other annotations marked as `@Constraint`)

### `jsonschema-module-javax-validation`
#### Added
- support meta annotations (validation annotations on other annotations marked as `@Constraint`)

### `jsonschema-examples`
#### Added
- new collection of examples (and implicit integration test) holding various examples (e.g., ones created in response to issues/discussions on GitHub)

## [4.29.0] - 2023-03-13
### `jsonschema-generator`
#### Added
- include basic Java module descriptor (also for standard modules and maven plugin)
- add possibility to reset various types of configuration aspects after a schema was generated, to enable re-using a generator instance even if it is stateful (i.e., behaves differently on subsequent invocations)

#### Changed
- treat `java.time.Period` as `{ "type": "string" }` when `Option.ADDITIONAL_FIXED_TYPES` is enabled
- treat `java.time.LocalTime` and `java.time.OffsetTime` as `{ "format": "time" }` when `Option.ADDITIONAL_FIXED_TYPES` is enabled (instead of "date-time")
- update jackson dependency from version `2.13.4.20221013` to `2.14.2` and replace usage of now deprecated methods

### `jsonschema-module-swagger-2`
#### Added
- consider `@Schema(ref = "...")` attribute, when it is annotated on a type (and not just a member) except for the main type being targeted

### `jsonschema-maven-plugin`
#### Fixed
- regression: filtering of considered classes for schema generation stopped working (after migration to `classgraph` in 4.28.0)

## [4.28.0] - 2022-10-31
### `jsonschema-generator`
#### Added
- enable look-up of annotations on a member's type parameter (e.g., a `Map`'s value type)
- enable providing full custom schema definition to be included in `additionalProperties` or `patternProperties`
- new function `TypeContext.getTypeWithAnnotation()` for finding also super type of interface with certain type annotation
- new function `TypeContext.getTypeAnnotationConsideringHierarchy()` for searching type annotations also on super types and interfaces

#### Changed
- consider annotations on `Map` value types when using `Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES`
- enhanced schema clean-up at the end: consolidating `allOf` with distinct `properties` (mostly relevant in jackson subtype resolution)
- enhanced schema clean-up at the end: consolidating `allOf` even with some keywords being present with differing values
- bump `slf4j-api` dependency version from `1.7.35` to `2.0.3`
- bump `jackson-core` dependency version from `2.13.2` to `2.13.4`
- bump `jackson-databind` dependency version from `2.13.2.2` to `2.13.4.2`

#### Fixed
- custom property definition containing only a definition reference/placeholder is being ignored

### `jsonschema-module-jackson`
#### Added
- new `JacksonOption.JSONIDENTITY_REFERENCE_ALWAYS_AS_ID` to respect `@JsonIdentityReference(alwaysAsId=true)` annotations (with `@JsonIdentityInfo`)

### `jsonschema-module-jakarta-validation`
#### Added
- set `minProperties`/`maxProperties` for `Map` types with `@NotEmpty` or `@Size` annotation

#### Changed
- bump `jakarta-validation-api` compile dependency version from `3.0.0` to `3.0.2`

### `jsonschema-module-swagger-1.5`
#### Changed
- bump `swagger-annotations` compile dependency version from `1.5.22` to `1.6.7`

### `jsonschema-module-swagger-2`
#### Changed
- bump `swagger-annotations` compile dependency version from `2.1.2` to `2.2.3`
- bump `swagger-core` compile dependency version from `2.1.2` to `2.2.3`

### `jsonschema-maven-plugin`
#### Changed
- use `classgraph` dependency for classpath scanning determining entry points for schema generation

#### Removed
- allow non-public classes as entry points for schema generation
- `reflections` dependency

## [4.27.0] - 2022-09-29
### `jsonschema-generator`
#### Added
- new `Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES` to disable the transparent member subtype resolution, i.e., enable inclusion of a supertype schema
- new `DefinitionType.ALWAYS_REF` for custom definitions, to produce centralised definition even if just referenced once

#### Fixed
- under some circumstances, even after the general schema clean-up procedure there were unnecessary `allOf` wrappers containing just a single entry

#### Changed
- enable moving subtype schema into `$defs` and thereby reduce number of unnecessary `anyOf` wrappers

### `jsonschema-module-jackson`
#### Added
- new `JacksonOption.ALWAYS_REF_SUBTYPES`, to produce centralised definition for each resolved subtype (wrapper) even if just referenced once

### `jsonschema-maven-plugin`
#### Added
- support `<classpath>` parameter in order to also consider compile dependencies for the schema generation (and/or ignoring runtime dependencies)
- support `<annotations>` parameter in order to allow selecting classes to generate a schema for by the presence of certain type annotations

## [4.26.0] - 2022-08-22
### `jsonschema-module-jackson`
#### Changed
- support `@JsonTypeInfo.defaultImpl` in combination with `As.PROPERTY`, to no longer require the type property for the specified default subtype

### `jsonschema-module-swagger-2`
#### Added
- support `@Schema.anyOf` and `@Schema.oneOf` on fields/methods

### `jsonschema-maven-plugin`
#### Added
- support `<failIfNoClassesMatch>false</failIfNoClassesMatch>` parameter, in order to continue build even if no class matches the defined pattern(s)

## [4.25.0] - 2022-06-24
### `jsonschema-generator`
#### Added
- new `Option.FLATTENED_SUPPLIERS` to unwrap the supplied type; `Supplier<T>` would thus be a type `T`

#### Fixed
- when resolving subtypes with a single other type, under some circumstances the type definition gets lost
- set default `ObjectMapper` node factory to `JsonNodeFactory.withExactBigDecimals(true)` to avoid scientific notation for numbers

#### Changed
- new `Option.FLATTENED_SUPPLIERS` is enabled by default in the `OptionPreset.PLAIN_JSON`
- existing `Option.FLATTENED_OPTIONALS` now also considers `Optional` container items (e.g., `List<Optional<T>>`).

## [4.24.3] - 2022-05-03
### `jsonschema-generator`
#### Fixed
- ensure thread-safety when `Option.INLINE_ALL_SCHEMAS` is enabled

### `jsonschema-module-jackson`
#### Fixed
-  `@JsonPropertyOrder` is only considered on the targeted type, i.e., no attempt is made to respect a super type's property order
- ensure thread-safety when loading bean descriptions

## [4.24.2] - 2022-04-04
### `jsonschema-generator-bom`
#### Fixed
- Actually publish BOM during release

### `jsonschema-generator-parent`
#### Changed
- Use BOM as parent and introduce separate reactor build definition

## [4.24.1] - 2022-04-01
### `jsonschema-generator`
#### Dependency Update
- Bump `jackson-databind` dependency to `2.13.2.2` to avoid security vulnerability
- Bump other `jackson` dependencies to `2.13.2`

## [4.24.0] - 2022-04-01
### `jsonschema-generator`
#### Fixed
- When looking-up a matching field or getter, only consider the declared property names and not any overrides

### `jsonschema-module-jackson`
#### Added
- Support for subtype resolution on properties with `JsonTypeInfo.Id.NONE` override avoiding the default wrapping/modification

#### Fixed
- Correctly consider `@JsonIgnoreProperties` targeting fields in super type

## [4.23.0] - 2022-03-13
### `jsonschema-generator-bom`
#### Added
- Introduce BOM (Bill of Materials) artifact, as option for importing Generator + Modules with matching versions

### `jsonschema-maven-plugin`
#### Added
- Declare thread-safety, to avoid warnings at runtime

### `jsonschema-module-jackson`
#### Fixed
- Subtype resolution utilising `JsonTypeInfo.Id.NAME` now considers `@JsonSubTypes.value[].name` instead of relying on `@JsonTypeName` being present

#### Changed
- Subtype resolution utilising `JsonTypeInfo.As.PROPERTY`/`JsonTypeInfo.As.EXISTING_PROPERTY` now marks discriminator property as `"required"`
- Subtype resolution utilising `JsonTypeInfo.As.WRAPPER_OBJECT` now marks discriminator value as `"required"` in wrapper object

## [4.22.0] - 2022-01-10
### `jsonschema-generator`
#### Added
- Introduce support for `SchemaVersion.DRAFT_2020_12`

#### Dependency Update
- Replace `log4j` test dependency with `logback` (still only test dependency)

### `jsonschema-maven-plugin`
#### Fixed
- Enable usage under JDK11 (by adjusting usage of `reflections`, in order to allow finding classes in dependencies/jar)

#### Dependency Update
- Update `reflections` runtime dependency from 0.9.12 to 0.10.2

## [4.21.0] - 2021-12-03
### `jsonschema-generator`
#### Fixed
- prevent mixing of `type: null` with `const`/`enum` in order to avoid validation error when `const`/`enum` does not include `null`

#### Changed
- default `ObjectMapper` when none is given in `SchemaGeneratorConfigBuilder` constructor now enables `JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS`

### `jsonschema-module-jackson`
#### Added
- Consider `@JsonProperty.value` override for methods
- Look-up `"description"` for methods (if included) based on `@JsonPropertyDescription`
- Consider `@JsonProperty(access = Access.READ_ONLY)` when determining whether a field/method should be marked as `readOnly`
- Consider `@JsonProperty(access = Access.WRITE_ONLY)` when determining whether a field/method should be marked as `writeOnly`
- Introduce `JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS` to enable easy inclusion of annotated non-getter methods (typically in combination with the general `Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS` and `Option.NONSTATIC_NONVOID_NONGETTER_METHODS`)

#### Changed
- Ignore getter methods when their associated field is being ignored (according to various Jackson annotations)
- Ignore methods when they or their associated field are marked as `@JsonBackReference`

## [4.20.0] - 2021-09-04
### `jsonschema-generator`
#### Added
- Support for `null` values in collections/containers/arrays
- New opt-in `Option.NULLABLE_ARRAY_ITEMS_ALLOWED` for enabling the respective "NullableCheck" to be considered for items in a field's array value or a method's array return value

### `jsonschema-module-swagger-2`
#### Added
- Consider `@ArraySchema(schema = @Schema(nullable = true))` if the new `Option.NULLABLE_ARRAY_ITEMS_ALLOWED` is enabled

## [4.19.0] - 2021-09-02
### `jsonschema-generator`
#### Added
- Support `readOnly` and `writeOnly` keywords

### `jsonschema-module-jackson`
#### Changed
- subtype resolution now also respects `@JsonTypeInfo` annotation on common interface (and not just common super class)

### `jsonschema-module-swagger-2`
#### Added
- Mark a subschema as `readOnly` or `writeOnly` based on a field or method's `@Schema.accessMode`

## [4.18.0] - 2021-03-21
### `jsonschema-generator`
#### Changed
- Increase of Jackson dependency version to 2.12.1
- Include `java.net.URI` in handling of `Option.ADDITIONAL_FIXED_TYPES`.

### `jsonschema-module-jackson`
#### Added
- New `JacksonOption.RESPECT_JSONPROPERTY_REQUIRED` to set a field as "required" based on `@JsonProperty` annotations

#### Changed
- Replace deprecated Jackson API usage, resulting in MINIMUM Jackson version 2.12.0

## [4.17.0] - 2020-12-24
### `jsonschema-module-jakarta-validation`
#### Added
- Initial implementation (initial features are equivalent to `jsonschema-module-javax-validation`)

### `jsonschema-maven-plugin`
#### Added
- Support for new `jakarta.validation` module

## [4.16.0] - 2020-09-25
### `jsonschema-generator`
#### Added
- New `Option.ENUM_KEYWORD_FOR_SINGLE_VALUES` to produce `"enum": ["A"]` instead of `"const": "A"` if there is only one allowed value.

## [4.15.1] - 2020-09-15
### `jsonschema-generator`
#### Fixed
- Missing parentheses on void argument-free methods that don't start with "get" or "is" when enabling `Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS`

## [4.15.0] - 2020-09-15
### `jsonschema-generator`
#### Added
- New `Option.EXTRA_OPEN_API_FORMAT_VALUES` to support automatic inclusion of `"format"` values for certain simple/fixed types

### `jsonschema-module-javax-validation`
#### Added
- Support picking up annotations on a (top-level) generic `Optional` parameter (e.g. `Optional<@Size(min=2) String>`)

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


[Unreleased]: https://github.com/victools/jsonschema-generator/compare/v4.38.0...HEAD
[4.38.0]: https://github.com/victools/jsonschema-generator/compare/v4.37.0...v4.38.0
[4.37.0]: https://github.com/victools/jsonschema-generator/compare/v4.36.0...v4.37.0
[4.36.0]: https://github.com/victools/jsonschema-generator/compare/v4.35.0...v4.36.0
[4.35.0]: https://github.com/victools/jsonschema-generator/compare/v4.34.0...v4.35.0
[4.34.0]: https://github.com/victools/jsonschema-generator/compare/v4.33.1...v4.34.0
[4.33.1]: https://github.com/victools/jsonschema-generator/compare/v4.33.0...v4.33.1
[4.33.0]: https://github.com/victools/jsonschema-generator/compare/v4.32.0...v4.33.0
[4.32.0]: https://github.com/victools/jsonschema-generator/compare/v4.31.1...v4.32.0
[4.31.1]: https://github.com/victools/jsonschema-generator/compare/v4.31.0...v4.31.1
[4.31.0]: https://github.com/victools/jsonschema-generator/compare/v4.30.0...v4.31.0
[4.30.0]: https://github.com/victools/jsonschema-generator/compare/v4.29.0...v4.30.0
[4.29.0]: https://github.com/victools/jsonschema-generator/compare/v4.28.0...v4.29.0
[4.28.0]: https://github.com/victools/jsonschema-generator/compare/v4.27.0...v4.28.0
[4.27.0]: https://github.com/victools/jsonschema-generator/compare/v4.26.0...v4.27.0
[4.26.0]: https://github.com/victools/jsonschema-generator/compare/v4.25.0...v4.26.0
[4.25.0]: https://github.com/victools/jsonschema-generator/compare/v4.24.3...v4.25.0
[4.24.3]: https://github.com/victools/jsonschema-generator/compare/v4.24.2...v4.24.3
[4.24.2]: https://github.com/victools/jsonschema-generator/compare/v4.24.1...v4.24.2
[4.24.1]: https://github.com/victools/jsonschema-generator/compare/v4.24.0...v4.24.1
[4.24.0]: https://github.com/victools/jsonschema-generator/compare/v4.23.0...v4.24.0
[4.23.0]: https://github.com/victools/jsonschema-generator/compare/v4.22.0...v4.23.0
[4.22.0]: https://github.com/victools/jsonschema-generator/compare/v4.21.0...v4.22.0
[4.21.0]: https://github.com/victools/jsonschema-generator/compare/v4.20.0...v4.21.0
[4.20.0]: https://github.com/victools/jsonschema-generator/compare/v4.19.0...v4.20.0
[4.19.0]: https://github.com/victools/jsonschema-generator/compare/v4.18.0...v4.19.0
[4.18.0]: https://github.com/victools/jsonschema-generator/compare/v4.17.0...v4.18.0
[4.17.0]: https://github.com/victools/jsonschema-generator/compare/v4.16.0...v4.17.0
[4.16.0]: https://github.com/victools/jsonschema-generator/compare/v4.15.1...v4.16.0
[4.15.1]: https://github.com/victools/jsonschema-generator/compare/v4.15.0...v4.15.1
[4.15.0]: https://github.com/victools/jsonschema-generator/compare/v4.14.0...v4.15.0
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
