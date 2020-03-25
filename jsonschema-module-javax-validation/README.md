# Java JSON Schema Generator – Module javax.validation
[![Build Status](https://github.com/victools/jsonschema-module-javax-validation/workflows/Java%20CI%20(Maven)/badge.svg)](https://github.com/victools/jsonschema-module-javax-validation/actions?query=workflow%3A%22Java+CI+%28Maven%29%22)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-javax-validation/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-javax-validation)

Module for the `jsonschema-generator` – deriving JSON Schema attributes from `javax.validation.constraints` annotations.

## Features
1. Determine whether a member is not nullable, base assumption being that all fields and method return values are nullable if not annotated. Based on `@NotNull`/`@Null`/`@NotEmpty`/`@NotBlank`
2. Populate list of "required" fields/methods for objects if `JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED` and/or `JavaxValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED` is/are being provided in constructor.
3. Populate "minItems" and "maxItems" for containers (i.e. arrays and collections). Based on `@Size`/`@NotEmpty`
4. Populate "minLength" and "maxLength" for strings. Based on `@Size`/`@NotEmpty`/`@NotBlank`
5. Populate "format" for strings. Based on `@Email`, can be "email" or "idn-email" depending on whether `JavaxValidationOption.PREFER_IDN_EMAIL_FORMAT` is being provided in constructor.
6. Populate "pattern" for strings. Based on `@Pattern`/`@Email`, when corresponding `JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS` is being provided in constructor.
7. Populate "minimum"/"exclusiveMinimum" for numbers. Based on `@Min`/`@DecimalMin`/`@Positive`/`@PositiveOrZero`
8. Populate "maximum"/"exclusiveMaximum" for numbers. Based on `@Max`/`@DecimalMax`/`@Negative`/`@NegativeOrZero`

Schema attributes derived from validation annotations on fields are also applied to their respective getter methods.
Schema attributes derived from validation annotations on getter methods are also applied to their associated fields.

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-javax-validation</artifactId>
    <version>4.7.0</version>
</dependency>
```

### Compatibility
Please note that while the minor versions may differ, it is recommended to use matching major versions of the `jsonschema-generator` and this module.
However, version 3.* of this module is also compatible with `jsonschema-generator` version 4.* (as no relevant features were removed/broken).

### Code
#### Passing into SchemaGeneratorConfigBuilder.with(Module)
```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
```
```java
JavaxValidationModule module = new JavaxValidationModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper)
    .with(module);
```

#### Complete Example
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
```
```java
ObjectMapper objectMapper = new ObjectMapper();
JavaxValidationModule module = new JavaxValidationModule(JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS);
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
