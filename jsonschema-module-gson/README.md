# Java JSON Schema Generation – Module Gson
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-jackson/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-jackson)

Module for the [jsonschema-generator](../jsonschema-generator) – deriving JSON Schema attributes from `gson` annotations.

## Features
1. Override a field's/method's property name as per `@SerializedName` annotations.
2. Ignore fields/methods that are marked with a `@Expose` annotation.

----

## Documentation
JavaDoc is being used throughout the codebase, offering contextual information in your respective IDE or being available online through services like [javadoc.io](https://www.javadoc.io/doc/com.github.victools/jsonschema-module-jackson).

Additional documentation can be found in the [Project Wiki](https://github.com/victools/jsonschema-generator/wiki).

----

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-gson</artifactId>
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
import com.github.victools.jsonschema.module.gson.GsonModule;
```
```java
GsonModule module = new GsonModule();
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
import com.github.victools.jsonschema.module.gson.GsonModule;
```
```java
GsonModule module = new GsonModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
