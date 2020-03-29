# Java JSON Schema Generation – Module jackson
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-jackson/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-jackson)

Module for the [jsonschema-generator](../jsonschema-generator) – deriving JSON Schema attributes from `jackson` annotations.

## Features
1. Populate a field/method's "description" as per `@JsonPropertyDescription`
2. Populate a type's "description" as per `@JsonClassDescription`.
3. Apply alternative field names defined in `@JsonProperty` annotations.
4. Ignore fields that are marked with a `@JsonBackReference` annotation.
5. Ignore fields that are deemed to be ignored according to various other `jackson-annotations` (e.g. `@JsonIgnore`, `@JsonIgnoreType`, `@JsonIgnoreProperties`) or are otherwise supposed to be excluded.
6. Optionally: treat enum types as plain strings, serialized by `@JsonValue` annotated method
7. Subtype resolution through `@JsonTypeInfo` and `@JsonSubTypes` on super type or directly properties as override of the per-type behaviour
    - Considering `@JsonTypeInfo.include` with values `As.PROPERTY`, `As.EXISTING_PROPERTY`, `As.WRAPPER_ARRAY`, `As.WRAPPER_OBJECT`
    - Considering `@JsonTypeInfo.use` with values `Id.CLASS`, `Id.NAME`

Schema attributes derived from validation annotations on getter methods are also applied to their associated fields.

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-jackson</artifactId>
    <version>4.7.0</version>
</dependency>
```

### Code
#### Passing into SchemaGeneratorConfigBuilder.with(Module)
```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
```
```java
JacksonModule module = new JacksonModule();
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
import com.github.victools.jsonschema.module.jackson.JacksonModule;
```
```java
ObjectMapper objectMapper = new ObjectMapper();
JacksonModule module = new JacksonModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
