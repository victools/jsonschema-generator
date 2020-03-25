# Java JSON Schema Generator
[![Build Status](https://github.com/victools/jsonschema-generator/workflows/Java%20CI%20(Maven)/badge.svg)](https://github.com/victools/jsonschema-generator/actions?query=workflow%3A%22Java+CI+%28Maven%29%22)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator)

Creating JSON Schema (Draft 7 or Draft 2019-09) from your Java classes utilising Jackson.

----

This project consists mainly of:
- the `jsonschema-generator` (the only thing you need to get started) – [find its README here](jsonschema-generator/README.md)
- some "modules" bundling some standard configurations for your convenience:
    - `jsonschema-module-jackson` – deriving JSON Schema attributes from `jackson` annotations (e.g. "description", property name overrides, what properties to ignore) – refer to its [README](jsonschema-module-jackson/README.md) for more details
    - `jsonschema-module-javax-validation` – deriving JSON Schema attributes from `javax.validation` annotations (e.g. which properties are nullable or not, their "minimum"/"maximum", "minItems"/"maxItems", "minLength"/"maxLength") – refer to its [README](jsonschema-module-javax-validation/README.md) for more details
    - `jsonschema-module-swagger-1.5` – deriving JSON Schema attributes from `swagger` (1.5.x) annotations (e.g. "description", property name overrides, what properties to ignore, their "minimum"/"maximum", "const"/"enum") – refer to its [README](jsonschema-module-swagger-1.5/README.md) for more details

Another example for such a module is:
- [imIfOu/jsonschema-module-addon](https://github.com/imIfOu/jsonschema-module-addon) – deriving JSON Schema attributes from a custom annotation with various parameters, which is part of the module.

----

## Usage
### Dependency (Maven)

```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-generator</artifactId>
    <version>4.7.0</version>
</dependency>
```

### Code
#### Complete/Minimal Example
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
```
```java
ObjectMapper objectMapper = new ObjectMapper();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
