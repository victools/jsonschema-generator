# Java JSON Schema Generator – Module Swagger (2.x)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-swagger-2/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-swagger-2)

Module for the [jsonschema-generator](../jsonschema-generator) – deriving JSON Schema attributes from `swagger` (2.x) annotations

## Features
1. …

Schema attributes derived from `@Schema` on fields are also applied to their respective getter methods.
Schema attributes derived from `@Schema` on getter methods are also applied to their associated fields.

----

## Documentation
JavaDoc is being used throughout the codebase, offering contextual information in your respective IDE or being available online through services like [javadoc.io](https://www.javadoc.io/doc/com.github.victools/jsonschema-module-swagger-2).

Additional documentation can be found in the [Project Wiki](https://github.com/victools/jsonschema-generator/wiki).

----

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-swagger-2</artifactId>
    <version>[4.13.0,5.0.0)</version>
</dependency>
```

Since version `4.7`, the release versions of the main generator library and this module are aligned.
It is recommended to use identical versions for both dependencies to ensure compatibility.

### Code
#### Passing into SchemaGeneratorConfigBuilder.with(Module)
```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.swagger2.SwaggerModule;
```
```java
SwaggerModule module = new SwaggerModule();
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
import com.github.victools.jsonschema.module.swagger2.SwaggerModule;
```
```java
ObjectMapper objectMapper = new ObjectMapper();
SwaggerModule module = new SwaggerModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
