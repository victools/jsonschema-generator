# Java JSON Schema Generator
[![Build Status](https://github.com/victools/jsonschema-generator/workflows/Java%20CI%20(Maven)/badge.svg)](https://github.com/victools/jsonschema-generator/actions?query=workflow%3A%22Java+CI+%28Maven%29%22)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator)

Creating JSON Schema (Draft 6, Draft 7, Draft 2019-09 or Draft 2020-12) from your Java classes utilising Jackson.

----

This project consists of:
- the [victools/jsonschema-generator](jsonschema-generator) (the only thing you need to get started)
- a few modules bundling standard configurations for your convenience:
    - [victools/jsonschema-module-jackson](jsonschema-module-jackson) – deriving JSON Schema attributes from `jackson` annotations (e.g. "description", property name overrides, what properties to ignore) as well as looking up appropriate (annotated) subtypes
    - [victools/jsonschema-module-jakarta-validation](jsonschema-module-jakarta-validation) – deriving JSON Schema attributes from `jakarta.validation.constraints` annotations (e.g. which properties are nullable or not, their "minimum"/"maximum", "minItems"/"maxItems", "minLength"/"maxLength")
    - [victools/jsonschema-module-javax-validation](jsonschema-module-javax-validation) – deriving JSON Schema attributes from `javax.validation` annotations (e.g. which properties are nullable or not, their "minimum"/"maximum", "minItems"/"maxItems", "minLength"/"maxLength")
    - [victools/jsonschema-module-swagger-1.5](jsonschema-module-swagger-1.5) – deriving JSON Schema attributes from `swagger` (1.5.x) annotations (e.g. "description", property name overrides, what properties to ignore, their "minimum"/"maximum", "const"/"enum")
    - [victools/jsonschema-module-swagger-2](jsonschema-module-swagger-2) – deriving JSON Schema attributes from `swagger` (2.x) `@Schema` annotations
- the [victools/jsonschema-maven-plugin](jsonschema-maven-plugin) – allowing you to generate JSON Schemas as part of your Maven build

Another example for such a module is:
- [imIfOu/jsonschema-module-addon](https://github.com/imIfOu/jsonschema-module-addon) – deriving JSON Schema attributes from a custom annotation with various parameters, which is part of the module.

----

## Documentation
JavaDoc is being used throughout the codebase, offering contextual information in your respective IDE or being available online through services like [javadoc.io](https://www.javadoc.io/doc/com.github.victools/jsonschema-generator).

Additional documentation and configuration examples can be found here: https://victools.github.io/jsonschema-generator

----

## Usage
### Dependency (Maven)

```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-generator</artifactId>
    <version>4.38.0</version>
</dependency>
```

Since version `4.7`, the release versions of the main generator library and the (standard) `victools` modules listed above are aligned.
It is recommended to use identical versions for all of them to ensure compatibility.

It is discouraged to use an older/lower `jsonschema-generator` version than any of your `jsonschema-module-*` dependencies. If the module uses any feature only added to the `jsonschema-generator` in the newer version, runtime errors are to be expected.

### Code
#### Complete/Minimal Example
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toPrettyString());
```

Additional examples can be found in the [jsonschema-examples](jsonschema-examples) folder or throughout the various tests classes.
