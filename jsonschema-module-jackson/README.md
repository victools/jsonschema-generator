# Java JSON Schema Generation – Module jackson
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-jackson/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-jackson)

Module for the [jsonschema-generator](../jsonschema-generator) – deriving JSON Schema attributes from `jackson` annotations.

## Features
1. Populate a field/method's "description" as per `@JsonPropertyDescription`
2. Populate a type's "description" as per `@JsonClassDescription`.
3. Apply alternative field names defined in `@JsonProperty` annotations or as per `@JsonNaming` annotations.
4. Ignore fields that are marked with a `@JsonBackReference` annotation.
5. Ignore fields that are deemed to be ignored according to various other `jackson-annotations` (e.g. `@JsonIgnore`, `@JsonIgnoreType`, `@JsonIgnoreProperties`) or are otherwise supposed to be excluded.
6. Optionally: treat enum types as plain strings, serialized by `@JsonValue` annotated method
7. Optionally: treat enum types as plain strings, as per each enum constant's `@JsonProperty` annotation
8. Optionally: sort an object's properties according to its `@JsonPropertyOrder` annotation
9. Optionally: resolve subtypes according to `@JsonSubTypes` on a supertype in general or directly on specific fields/methods as an override of the per-type behavior.
10. Optionally: apply structural changes for subtypes according to `@JsonTypeInfo` on a supertype in general or directly on specific fields/methods as an override of the per-type behavior.
    - Considering `@JsonTypeInfo.include` with values `As.PROPERTY`, `As.EXISTING_PROPERTY`, `As.WRAPPER_ARRAY`, `As.WRAPPER_OBJECT`
    - Considering `@JsonTypeInfo.use` with values `Id.CLASS`, `Id.NAME`

Schema attributes derived from validation annotations on getter methods are also applied to their associated fields.

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-jackson</artifactId>
    <version>[4.10.0,5.0.0)</version>
</dependency>
```

Since version `4.7`, the release versions of the main generator library and this module are aligned.
It is recommended to use identical versions for both dependencies to ensure compatibility.

### Code
#### Passing into SchemaGeneratorConfigBuilder.with(Module)
```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
```
```java
JacksonModule module = new JacksonModule();
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
import com.github.victools.jsonschema.module.jackson.JacksonModule;
```
```java
JacksonModule module = new JacksonModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
