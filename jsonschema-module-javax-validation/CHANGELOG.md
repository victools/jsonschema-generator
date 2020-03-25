# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [4.7.0] – 2020-03-20
- No feature changes, just bumping of minor version to indicate compatibility with `jsonschema-generator` version 4.7+

## [4.4.0] – 2020-03-03
- No feature changes, just bumping of minor version to indicate compatibility with `jsonschema-generator` version 4.4+

## [4.0.0] – 2020-01-03
- No changes, just bumping of major version to indicate compatibility with `jsonschema-generator` version 4.*

## [3.2.0] – 2019-09-01
### Added
- Option for treating not-nullable fields as "required" in their parent type
- Option for treating not-nullable methods as "required" in their parent type
- Indicate a string's "format" to be "email" if `@Email` is present
- Option for returning "idn-email" instead of "email" as "format" if `@Email` is present
- Indicate a string's "pattern" according to regular expressions on `@Pattern` or `@Email` (ignoring specified flags)
- Option for enabling the inclusion of "pattern" expressions (they are excluded by default)
- Allow filtering applicable annotations by their declared validation `groups` via `JavaxValidationModule.forValidationGroups()`

## [3.0.0] – 2019-06-10
### Added
- Consider the same validation annotations on a getter method also for its field
- Consider the same validation annotations on a field also for its getter method

### Changed
- Internal changes to comply with latest `jsonschema-generator` release `v3.x`

## v1.0.0 – 2019-05-22
### Added
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

[4.7.0]: https://github.com/victools/jsonschema-module-javax-validation/compare/v4.4.0...v4.7.0
[4.4.0]: https://github.com/victools/jsonschema-module-javax-validation/compare/v4.0.0...v4.4.0
[4.0.0]: https://github.com/victools/jsonschema-module-javax-validation/compare/v3.2.0...v4.0.0
[3.2.0]: https://github.com/victools/jsonschema-module-javax-validation/compare/v3.0.0...v3.2.0
[3.0.0]: https://github.com/victools/jsonschema-module-javax-validation/compare/v1.0.0...v3.0.0
[1.0.0]: https://github.com/victools/jsonschema-module-javax-validation/releases/tag/v1.0.0
