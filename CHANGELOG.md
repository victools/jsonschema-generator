# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.7.0] - 2020-03-25
### Changed
- Ignore/exclude properties marked with `@JsonBackReference`

## [4.5.1] - 2020-03-09
### Fixed
- Re-introduce argument-free constructor (since Java 11 is too strict)

## [4.5.0] - 2020-03-08
### Added
- New `JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE` for considering `@JsonValue` annotations on enums, similar to `Option.FLATTENED_ENUMS`

## [4.4.0] - 2020-03-04
- No feature changes, just bumping of minor version to indicate compatibility with `jsonschema-generator` version `4.4.*`

## [4.0.0] - 2020-01-03
### Changed
- Look-up descriptions from `@JsonClassDescription` via new `forTypesInGeneral()` API

BEWARE: The `forTypesInGeneral()` API was only added to the main generator library in version 4.0.0.

## [3.0.0] – 2019-06-10
### Added
- Populate "description" as per `@JsonPropertyDescription` (falling-back on `@JsonClassDescription`).
- Apply alternative field names defined in `@JsonProperty` annotations.
- Ignore fields that are deemed to be ignored according to various `jackson-annotations` (e.g. `@JsonIgnore`, `@JsonIgnoreType`, `@JsonIgnoreProperties`) or are otherwise supposed to be excluded.

[4.7.0]: https://github.com/victools/jsonschema-module-jackson/compare/v4.5.1...v4.7.0
[4.5.1]: https://github.com/victools/jsonschema-module-jackson/compare/v4.5.0...v4.5.1
[4.5.0]: https://github.com/victools/jsonschema-module-jackson/compare/v4.4.0...v4.5.0
[4.4.0]: https://github.com/victools/jsonschema-module-jackson/compare/v4.0.0...v4.4.0
[4.0.0]: https://github.com/victools/jsonschema-module-jackson/compare/v3.0.0...v4.0.0
[3.0.0]: https://github.com/victools/jsonschema-module-jackson/releases/tag/v3.0.0
