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
3. From `@Schema(subTypes = …)` on types in general, derive `"anyOf"` alternatives.
4. From `@Schema(anyOf = …)` on types in general (as alternative to `subTypes`), derive `"anyOf"` alternatives.
5. From `@Schema(name = …)` on types in general, derive the keys/names in `"definitions"`/`"$defs"`.
6. From `@Schema(description = …)` on fields/methods, derive `"description"`.
7. From `@Schema(title = …)` on fields/methods, derive `"title"`.
8. From `@Schema(implementation = …)` on fields/methods, override represented type.
9. From `@Schema(hidden = true)` on fields/methods, skip certain properties.
10. From `@Schema(name = …)` on fields/methods, override property names.
11. From `@Schema(ref = …)` on fields/methods, replace subschema with `"$ref"` to external/separate schema.
12. From `@Schema(allOf = …)` on fields/methods, include `"allOf"` parts.
13. From `@Schema(not = …)` on fields/methods, include the indicated `"not"` subschema.
14. From `@Schema(required = true)` on fields/methods, mark property as `"required"` in the schema containing the property.
15. From `@Schema(requiredProperties = …)` on fields/methods, derive its `"required"` properties.
16. From `@Schema(minProperties = …)` on fields/methods, derive its `"minProperties"`.
17. From `@Schema(maxProperties = …)` on fields/methods, derive its `"maxProperties"`.
18. From `@Schema(nullable = true)` on fields/methods and potentially array items (if `Option.NULLABLE_ARRAY_ITEMS_ALLOWED` is enabled), include `null` in its `"type"`.
19. From `@Schema(allowableValues = …)` on fields/methods, derive its `"const"`/`"enum"`.
20. From `@Schema(defaultValue = …)` on fields/methods, derive its `"default"`.
21. From `@Schema(accessMode = AccessMode.READ_ONLY)` on fields/methods, to mark them as `"readOnly"`.
22. From `@Schema(accessMode = AccessMode.WRITE_ONLY)` on fields/methods, to mark them as `"writeOnly"`.
23. From `@Schema(minLength = …)` on fields/methods, derive its `"minLength"`.
24. From `@Schema(maxLength = …)` on fields/methods, derive its `"maxLength"`.
25. From `@Schema(format = …)` on fields/methods, derive its `"format"`.
26. From `@Schema(pattern = …)` on fields/methods, derive its `"pattern"`.
27. From `@Schema(multipleOf = …)` on fields/methods, derive its `"multipleOf"`.
28. From `@Schema(minimum = …, exclusiveMinimum = …)` on fields/methods, derive its `"minimum"`/`"exclusiveMinimum"`.
29. From `@Schema(maximum = …, exclusiveMaximum = …)` on fields/methods, derive its `"maximum"`/`"exclusiveMaximum"`.
30. From `@ArraySchema(minItems = …)` on fields/methods, derive its `"minItems"`.
31. From `@ArraySchema(maxItems = …)` on fields/methods, derive its `"maxItems"`.
32. From `@ArraySchema(uniqueItems = …)` on fields/methods, derive its `"uniqueItems"`.

Schema attributes derived from `@Schema`/`@ArraySchema` on fields are also applied to their respective getter methods.
Schema attributes derived from `@Schema`/`@ArraySchema` on getter methods are also applied to their associated fields.

To use it, just pass a module instance into `SchemaGeneratorConfigBuilder.with(Module)`.
