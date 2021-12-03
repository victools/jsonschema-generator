# Java JSON Schema Generator – Module jakarta.validation
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-jakarta-validation/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-jakarta-validation)

Module for the [jsonschema-generator](../jsonschema-generator) – deriving JSON Schema attributes from `jakarta.validation.constraints` annotations.

## Features
1. Determine whether a member is not nullable, base assumption being that all fields and method return values are nullable if not annotated. Based on `@NotNull`/`@Null`/`@NotEmpty`/`@NotBlank`
2. Populate list of "required" fields/methods for objects if `JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED` and/or `JakartaValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED` is/are being provided in constructor.
3. Populate "minItems" and "maxItems" for containers (i.e. arrays and collections). Based on `@Size`/`@NotEmpty`
4. Populate "minLength" and "maxLength" for strings. Based on `@Size`/`@NotEmpty`/`@NotBlank`
5. Populate "format" for strings. Based on `@Email`, can be "email" or "idn-email" depending on whether `JakartaValidationOption.PREFER_IDN_EMAIL_FORMAT` is being provided in constructor.
6. Populate "pattern" for strings. Based on `@Pattern`/`@Email`, when corresponding `JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS` is being provided in constructor.
7. Populate "minimum"/"exclusiveMinimum" for numbers. Based on `@Min`/`@DecimalMin`/`@Positive`/`@PositiveOrZero`
8. Populate "maximum"/"exclusiveMaximum" for numbers. Based on `@Max`/`@DecimalMax`/`@Negative`/`@NegativeOrZero`

Schema attributes derived from validation annotations on fields are also applied to their respective getter methods.  
Schema attributes derived from validation annotations on getter methods are also applied to their associated fields.

----

## Documentation
JavaDoc is being used throughout the codebase, offering contextual information in your respective IDE or being available online through services like [javadoc.io](https://www.javadoc.io/doc/com.github.victools/jsonschema-module-jakarta-validation).

Additional documentation can be found in the [Project Wiki](https://github.com/victools/jsonschema-generator/wiki).

----

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-jakarta-validation</artifactId>
    <version>[4.21.0,5.0.0)</version>
</dependency>
```

Since version `4.7`, the release versions of the main generator library and this module are aligned.
It is recommended to use identical versions for both dependencies to ensure compatibility.

### Code
#### Passing into SchemaGeneratorConfigBuilder.with(Module)
```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
```
```java
JakartaValidationModule module = new JakartaValidationModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09)
    .with(module);
```

#### Complete Example
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
```
```java
JakartaValidationModule module = new JakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS);
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
