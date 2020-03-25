# Java JSON Schema Generator – Module Swagger (1.5)
[![Build Status](https://github.com/victools/jsonschema-module-swagger-1.5/workflows/Java%20CI%20(Maven)/badge.svg)](https://github.com/victools/jsonschema-module-swagger-1.5/actions?query=workflow%3A%22Java+CI+%28Maven%29%22)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-swagger-1.5/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-swagger-1.5)

Module for the `jsonschema-generator` – deriving JSON Schema attributes from `swagger` (1.5.x) annotations

## Features
1. Optionally override a field's property name with `@ApiModelProperty(name = ...)`
2. Optionally ignore a field/method if `@ApiModelProperty(hidden = true)`
3. Optionally provide a type's "title" as per `@ApiModel(value = ...)`
4. Optionally provide a type's "description" as per `@ApiModel(description = ...)`
5. Provide a field/method's "description" as per `@ApiModelProperty(value = ...)`
6. Indicate a number's (field/method) "minimum" (inclusive) according to `@ApiModelProperty(allowableValues = "range[...")`
7. Indicate a number's (field/method) "exclusiveMinimum" according to `@ApiModelProperty(allowableValues = "range(...")`
8. Indicate a number's (field/method) "maximum" (inclusive) according to `@ApiModelProperty(allowableValues = "range...]")`
9. Indicate a number's (field/method) "exclusiveMaximum" according to `@ApiModelProperty(allowableValues = "range...)")`
10. Indicate a field/method's "const"/"enum" as `@ApiModelProperty(allowableValues = ...)` (if it is not a numeric range declaration)

Schema attributes derived from `@ApiModelProperty` on fields are also applied to their respective getter methods.
Schema attributes derived from `@ApiModelProperty` on getter methods are also applied to their associated fields.

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-swagger-1.5</artifactId>
    <version>4.7.0</version>
</dependency>
```

### Compatibility
Please note that while the minor versions may differ, it is recommended to use matching major versions of the `jsonschema-generator` and this module.
However, versions 3.* of this module are also compatible with `jsonschema-generator` 4.* (as no relevant features were removed/broken).

### Code
#### Passing into SchemaGeneratorConfigBuilder.with(Module)
```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
```
```java
SwaggerModule module = new SwaggerModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper)
    .with(module);
```

#### Enabling optional processing options
```java
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger15.SwaggerOption;
```
```java
SwaggerModule module = new SwaggerModule(SwaggerOption.IGNORING_HIDDEN_PROPERTIES, SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES);
```

#### Disabling optional processing options
```java
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger15.SwaggerOption;
```
```java
SwaggerModule module = new SwaggerModule(SwaggerOption.NO_APIMODEL_DESCRIPTION, SwaggerOption.NO_APIMODEL_TITLE);
```

#### Complete Example
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
```
```java
ObjectMapper objectMapper = new ObjectMapper();
SwaggerModule module = new SwaggerModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
