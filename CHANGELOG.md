# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.4.1] - 2019-12-30
### Fixed
- Collected attributes should also be applied to container types (Issue #15)

## [3.4.0] - 2019-11-29
### Added
- Introduce convenience function `MemberScope.getAnnotationConsideringFieldAndGetter(Class)`

## [3.3.0] - 2019-10-25
### Fixed
- Increase dependency version for jackson-databind (and jackson-core) to resolve security alerts.
- Avoid unnecessary quotes when representing constant string values (due to changed behaviour in jackson >=2.10.0)

## [3.2.0] – 2019-09-01
### Added
- In `SchemaGenerator.generateSchema(Type)` also allow passing type parameters via `SchemaGenerator.generateSchema(Type, Type...)`.
- Support for "required" property via `SchemaGeneratorConfigPart.withRequiredCheck()` (PR #5).
- Support for "default" property via `SchemaGeneratorConfigPart.withDefaultResolver()` (PR #5).
- Support for "pattern" property via `SchemaGeneratorConfigPart.withStringPatternResolver()` (Issue #9).

## [3.1.0] – 2019-06-18
### Changed
- Use `Class.getTypeName()` instead of `Class.getName()` in `TypeContext.getFullTypeDescription()`.

### Added
- In `TypeContext.resolve(Type)` also allow passing type parameters via `TypeContext.resolve(Type, Type...)`.

### Fixed
- `NullPointerException` on `MemberScope` representing `void` methods.
- `IndexOutOfBoundsException` when determining container item type of raw `Collection`.

## [3.0.0] – 2019-06-10
### Changed
- Simplify configuration API to be based on `FieldScope`/`MethodScope` respectively.
- Consolidate some utility functions into `FieldScope`/`MethodScope`.
- Consolidate logic for determining whether a type is a container into `TypeContext`.
- Consolidate naming logic for schema "definitions" into `TypeContext`.
- Add `TypeContext` argument to `CustomDefinitionProvider` interface.
- Remove `SchemaGeneratorConfig` argument from `InstanceAttributeOverride` interface.

### Added
- Allow for sub-typing of `FieldScope`/`MethodScope` and `TypeContext` (in case offered configuration options are insufficient).

### Removed
- Remove support for `Option.GETTER_ATTRIBUTES_FOR_FIELDS` and `Option.FIELD_ATTRIBUTES_FOR_GETTERS`, rather let configurations/modules decide individually and avoid potential endless loop.

## [2.0.0] – 2019-06-07
### Changed
- Removed type resolution and replaced it with `org.fasterxml:classmate` dependency.
- Adjusting configuration API to use `classmate` references for types/fields/methods.

### Fixed
- Ignore complex constant values that may not be properly representable as JSON.

## [1.0.2] - 2019-05-30
### Fixed
- Increase dependency version for jackson-databind to resolve security alert.

## [1.0.1] - 2019-05-19
### Fixed
- Specified "test" scope for dependency on jsonassert.

## [1.0.0] - 2019-05-18
### Added
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
