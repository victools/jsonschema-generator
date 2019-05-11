# Java JSON Schema Generator
Creating JSON Schema (Draft 7) from your Java classes utilising Jackson (inspired by JJSchema).

## Usage
### Dependency (Maven)

```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-generator</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Code
#### Complete/Minimal Example
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
```
```java
ObjectMapper objectMapper = new ObjectMapper();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);
```

#### Toggling Standard Options
```java
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper)
    .with(Option.DEFINITIONS_FOR_ALL_OBJECTS), // default: disabled
    .without(Option.FIELDS_ARE_NULLABLE_BY_DEFAULT, Option.METHODS_RETURN_NULLABLE_BY_DEFAULT); // default: enabled
```

#### Adding Separate Modules (e.g. from another library)
```java
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
```
```java
Module separateModule = new YourSeparateModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper)
    .with(separateModule);
```
