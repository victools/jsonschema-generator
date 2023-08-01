# Swagger 2 Module
The [victools:jsonschema-module-swagger-2](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-module-swagger-2) provides a number of standard configurations for deriving JSON Schema attributes from OpenAPI/`swagger` (2.x) `@Schema` annotations.

```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;


Swagger2Module module = new Swagger2Module();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09)
    .with(module);
```

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
17. From `@Schema(requiredMode = REQUIRED)` or `@Schema(required = true)` on fields/methods, mark property as `"required"` in the schema containing the property.
18. From `@Schema(requiredProperties = …)` on fields/methods, derive its `"required"` properties.
19. From `@Schema(minProperties = …)` on fields/methods, derive its `"minProperties"`.
20. From `@Schema(maxProperties = …)` on fields/methods, derive its `"maxProperties"`.
21. From `@Schema(nullable = true)` on fields/methods, include `null` in its `"type"`.
22. From `@Schema(allowableValues = …)` on fields/methods, derive its `"const"`/`"enum"`.
23. From `@Schema(defaultValue = …)` on fields/methods, derive its `"default"`.
24. From `@Schema(accessMode = AccessMode.READ_ONLY)` on fields/methods, to mark them as `"readOnly"`.
25. From `@Schema(accessMode = AccessMode.WRITE_ONLY)` on fields/methods, to mark them as `"writeOnly"`.
26. From `@Schema(minLength = …)` on fields/methods, derive its `"minLength"`.
27. From `@Schema(maxLength = …)` on fields/methods, derive its `"maxLength"`.
28. From `@Schema(format = …)` on fields/methods, derive its `"format"`.
29. From `@Schema(pattern = …)` on fields/methods, derive its `"pattern"`.
30. From `@Schema(multipleOf = …)` on fields/methods, derive its `"multipleOf"`.
31. From `@Schema(minimum = …, exclusiveMinimum = …)` on fields/methods, derive its `"minimum"`/`"exclusiveMinimum"`.
32. From `@Schema(maximum = …, exclusiveMaximum = …)` on fields/methods, derive its `"maximum"`/`"exclusiveMaximum"`.
33. From `@ArraySchema(minItems = …)` on fields/methods, derive its `"minItems"`.
34. From `@ArraySchema(maxItems = …)` on fields/methods, derive its `"maxItems"`.
35. From `@ArraySchema(uniqueItems = …)` on fields/methods, derive its `"uniqueItems"`.

Schema attributes derived from `@Schema`/`@ArraySchema` on fields are also applied to their respective getter methods.
Schema attributes derived from `@Schema`/`@ArraySchema` on getter methods are also applied to their associated fields.

To use it, just pass a module instance into `SchemaGeneratorConfigBuilder.with(Module)`.
