# Java JSON Schema Generator – Module Microprofile OpenAPI (3.x)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-microprofile-openapi-3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-microprofile-openapi-3)
Module for the [jsonschema-generator](../jsonschema-generator) – deriving JSON Schema attributes from `microprofile-openapi-api` (3.x) annotations

## Features
 1. From `@Schema(description = …)` on types in general, derive `"description"`.
 2. From `@Schema(title = …)` on types in general, derive `"title"`.
 3. From `@Schema(ref = …)` on types in general, replace subschema with `"$ref"` to external/separate schema (except for the main type being targeted).
 4. From `@Schema(subTypes = …)` on types in general, derive `"anyOf"` alternatives.
 5. From `@Schema(anyOf = …)` on types in general (as alternative to `subTypes`), derive `"anyOf"` alternatives.
 6. From `@Schema(name = …)` on types in general, derive the keys/names in `"definitions"`/`"$defs"`.
 7. From `@Schema(description = …)` on fields/methods, derive `"description"`.
 8. From `@Schema(title = …)` on fields/methods, derive `"title"`.
 9. From `@Schema(implementation = …)` on fields/methods, override represented type.
10. From `@Schema(hidden = true)` on fields/methods, skip certain properties.
11. From `@Schema(name = …)` on fields/methods, override property names.
12. From `@Schema(ref = …)` on fields/methods, replace subschema with `"$ref"` to external/separate schema.
13. From `@Schema(allOf = …)` on fields/methods, include `"allOf"` parts.
14. From `@Schema(anyOf = …)` on fields/methods, include `"anyOf"` parts.
15. From `@Schema(oneOf = …)` on fields/methods, include `"oneOf"` parts.
16. From `@Schema(not = …)` on fields/methods, include the indicated `"not"` subschema.
17. From `@Schema(required = true)` on fields/methods, mark property as `"required"` in the schema containing the property.
18. From `@Schema(requiredProperties = …)` on fields/methods, derive its `"required"` properties.
19. From `@Schema(minProperties = …)` on fields/methods, derive its `"minProperties"`.
20. From `@Schema(maxProperties = …)` on fields/methods, derive its `"maxProperties"`.
21. From `@Schema(nullable = true)` on fields/methods, include `null` in its `"type"` - when schema type is ARRAY, applied on array item only.
22. From `@Schema(allowableValues = …)` on fields/methods, derive its `"const"`/`"enum"`.
23. From `@Schema(defaultValue = …)` on fields/methods, derive its `"default"` - when schema type is ARRAY, applied on array item only.
24. From `@Schema(readOnly = …)` on fields/methods, to mark them as `"readOnly"`.
25. From `@Schema(writeOnly = …)` on fields/methods, to mark them as `"writeOnly"`.
26. From `@Schema(minLength = …)` on fields/methods, derive its `"minLength"`.
27. From `@Schema(maxLength = …)` on fields/methods, derive its `"maxLength"`.
28. From `@Schema(format = …)` on fields/methods, derive its `"format"`.
29. From `@Schema(pattern = …)` on fields/methods, derive its `"pattern"`.
30. From `@Schema(multipleOf = …)` on fields/methods, derive its `"multipleOf"`.
31. From `@Schema(minimum = …, exclusiveMinimum = …)` on fields/methods, derive its `"minimum"`/`"exclusiveMinimum"`.
32. From `@Schema(maximum = …, exclusiveMaximum = …)` on fields/methods, derive its `"maximum"`/`"exclusiveMaximum"`.
33. From `@Schema(minItems = …)` on fields/methods when schema type is ARRAY, derive its `"minItems"`.
34. From `@Schema(maxItems = …)` on fields/methods when schema type is ARRAY, derive its `"maxItems"`.
35. From `@Schema(uniqueItems = …)` on fields/methods when schema type is ARRAY, derive its `"uniqueItems"`.

Schema attributes derived from `@Schema` on fields are also applied to their respective getter methods.
Schema attributes derived from `@Schema` on getter methods are also applied to their associated fields.

----

## Documentation
JavaDoc is being used throughout the codebase, offering contextual information in your respective IDE or being available online through services like [javadoc.io](https://www.javadoc.io/doc/com.github.victools/jsonschema-module-microprofile-openapi-3).

Additional documentation can be found in the [Project Wiki](https://github.com/victools/jsonschema-generator/wiki).

----

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-microprofile-openapi-3</artifactId>
    <version>[4.37.0,)</version>
</dependency>
```

The release versions of the main generator library and this module are aligned.
It is recommended to use identical versions for both dependencies to ensure compatibility.

### Code
#### Passing into SchemaGeneratorConfigBuilder.with(Module)

```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.microprofile.openapi3.MicroProfileOpenApi3Module;
```
```java
MicroProfileOpenApi3Module module = new MicroProfileOpenApi3Module();
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
import com.github.victools.jsonschema.module.microprofile.openapi3.MicroProfileOpenApi3Module;
```
```java
MicroProfileOpenApi3Module module = new MicroProfileOpenApi3Module();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
