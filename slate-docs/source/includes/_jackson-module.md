# Jackson Module
The [victools:jsonschema-module-jackson](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-module-jackson) provides a number of standard configurations for deriving JSON Schema attributes from `jackson` annotations as well as looking up appropriate (annotated) subtypes.

```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;


JacksonModule module = new JacksonModule(
        JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE
);
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09)
    .with(module);
```

1. Set a field/method's "description" as per `@JsonPropertyDescription`
2. Set a type's "description" as per `@JsonClassDescription`.
3. Override a field's/method's property name as per `@JsonProperty` annotations.
4. Ignore fields/methods that are marked with a `@JsonBackReference` annotation.
5. Ignore fields (and their associated getter methods) that are deemed to be ignored according to various other `jackson-annotations` (e.g. `@JsonIgnore`, `@JsonIgnoreType`, `@JsonIgnoreProperties`) or are otherwise supposed to be excluded.
6. Optionally: set a field/method as "required" as per `@JsonProperty` annotations, if the `JacksonOption.RESPECT_JSONPROPERTY_REQUIRED` was provided (i.e. this is an "opt-in").
7. Optionally: treat enum types as plain strings as per the `@JsonValue` annotated method, if there is one and the `JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE` was provided (i.e. this is an "opt-in").
8. Optionally: treat enum types as plain strings, as per each enum constant's `@JsonProperty` annotation, if all values of an enum have such annotations and the `JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY` was provided (i.e. this is an "opt-in").
9. Optionally: sort an object's properties according to its `@JsonPropertyOrder` annotation, if the `JacksonOption.RESPECT_JSONPROPERTY_ORDER` was provided (i.e. this is an "opt-in").
10. Subtype resolution according to `@JsonSubTypes` on a supertype in general or directly on specific fields/methods as an override of the per-type behavior unless `JacksonOption.SKIP_SUBTYPE_LOOKUP` was provided (i.e. this is an "opt-out").
11. Apply structural changes for subtypes according to `@JsonTypeInfo` on a supertype in general or directly on specific fields/methods as an override of the per-type behavior unless `JacksonOption.IGNORE_TYPE_INFO_TRANSFORM` was provided (i.e. this is an "opt-out").
    * Considering `@JsonTypeInfo.include` with values `As.PROPERTY`, `As.EXISTING_PROPERTY`, `As.WRAPPER_ARRAY`, `As.WRAPPER_OBJECT`
    * Considering `@JsonTypeInfo.use` with values `Id.CLASS`, `Id.NAME`
12. Consider `@JsonProperty.access` for marking a field/method as `readOnly` or `writeOnly`
13. Optionally: ignore all methods but those with a `@JsonProperty` annotation, if the `JacksonOption.INCLUDE_ONLY_JSONPROPERTY_ANNOTATED_METHODS` was provided (i.e. this is an "opt-in").

Schema attributes derived from annotations on getter methods are also applied to their associated fields.

To use it, just pass a module instance into `SchemaGeneratorConfigBuilder.with(Module)`, optionally providing `JacksonOption` values in the module's constructor.
