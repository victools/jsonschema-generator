# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.7.0] – 2020-03-20
- No feature changes, just bumping of minor version to indicate compatibility with `jsonschema-generator` version 4.7+

## [4.4.0] – 2020-03-03
- No feature changes, just bumping of minor version to indicate compatibility with `jsonschema-generator` version 4.4+

## [4.0.0] - 2020-01-03
### Changed
- Look-up titles and descriptions from `@ApiModel` via new `forTypesInGeneral()` API

BEWARE: The `forTypesInGeneral()` API was only added to the main generator library in version 4.0.0.

## [3.1.0] - 2019-11-30
### Added
- Optionally provide a field/method's "title" as per `@ApiModel(value = ...)`
- Allow to ignore the general `@ApiModel(description = ...)` when populating a field/method's "description"

## [3.0.0] – 2019-06-15
### Added
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

[4.7.0]: https://github.com/victools/jsonschema-module-swagger-1.5/compare/v4.4.0...v4.7.0
[4.4.0]: https://github.com/victools/jsonschema-module-swagger-1.5/compare/v4.0.0...v4.4.0
[4.0.0]: https://github.com/victools/jsonschema-module-swagger-1.5/compare/v3.1.0...v4.0.0
[3.1.0]: https://github.com/victools/jsonschema-module-swagger-1.5/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/victools/jsonschema-module-swagger-1.5/releases/tag/v3.0.0
