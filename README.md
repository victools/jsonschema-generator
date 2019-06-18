# Java JSON Schema Generator
[![Build Status](https://travis-ci.org/victools/jsonschema-generator.svg?branch=master)](https://travis-ci.org/victools/jsonschema-generator)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator)

Creating JSON Schema (Draft 7) from your Java classes utilising Jackson (inspired by JJSchema).

## Usage
### Dependency (Maven)

```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-generator</artifactId>
    <version>3.1.0</version>
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
```
```java
ObjectMapper objectMapper = new ObjectMapper();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper, OptionPreset.PLAIN_JSON);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```

#### Toggling Standard Options (via OptionPresets)
There are three predefined `OptionPreset` alternatives:
- `OptionPreset.FULL_DOCUMENTATION` – show-casing the inclusion of all fields (`private`/`package`/`protected`/`public` and `static`/non-`static`) and all `public` methods (`static`/non-`static`)
- `OptionPreset.PLAIN_JSON` – catering for a representation of a JSON data structure, including all non-`static`fields (`private`/`package`/`protected`/`public`) and no methods
- `OptionPreset.JAVA_OBJECT` – catering for a representation of a Java object, including all `public` fields (`static` and non-`static`) and all `public` methods (`static`/non-`static`)

The `OptionPreset` needs to be provided in the `SchemaGeneratorConfigBuilder`'s constructor:
```java
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper, OptionPreset.PLAIN_JSON);
```

#### Toggling Standard Options (individually)
As alternative to (or on top of) the predefined `OptionPreset`s, you can set the individual standard `Option`s directly on the `SchemaGeneratorConfigBuilder`:
```java
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper)
    .with(Option.FLATTENED_ENUMS)
    .without(Option.NULLABLE_FIELDS_BY_DEFAULT, Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT);
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

Some available modules are:
- [victools/jsonschema-module-jackson](https://github.com/victools/jsonschema-module-jackson) – deriving JSON Schema attributes from `jackson` annotations (e.g. "description", property name overrides, what properties to ignore).
- [victools/jsonschema-module-javax-validation](https://github.com/victools/jsonschema-module-javax-validation) – deriving JSON Schema attributes from `javax.validation` annotations (e.g. which properties are nullable or not, their "minimum"/"maximum", "minItems"/"maxItems", "minLength"/"maxLength").
- [victools/jsonschema-module-swagger-1.5](https://github.com/victools/jsonschema-module-swagger-1.5) – deriving JSON Schema attributes from `swagger` (1.5.x) annotations (e.g. "description", property name overrides, what properties to ignore, their "minimum"/"maximum", "const"/"enum").

#### Defining Desired Behaviour via individual configurations
```java
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper);
configBuilder.forFields()
    // populate the "title" of all fields with a description of the field's type
    .withTitleResolver(FieldScope::getSimpleTypeDescription);
```
