# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
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

[Unreleased]: https://github.com/victools/jsonschema-generator/compare/v1.0.2...HEAD
[1.0.2]: https://github.com/victools/jsonschema-generator/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/victools/jsonschema-generator/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/victools/jsonschema-generator/releases/tag/v1.0.0
