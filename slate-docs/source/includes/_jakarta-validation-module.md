
# Jakarta Validation Module
The [victools:jsonschema-module-jakarta-validation](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-module-jakarta-validation) provides a number of standard configurations for deriving JSON Schema attributes from `jakarta.validation.constraints` annotations.

```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;


JakartaValidationModule module = new JakartaValidationModule(JakartaValidationOption.PREFER_IDN_EMAIL_FORMAT)
    .forValidationGroups(YourGroupFlag.class);
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09)
    .with(module);
```

1. Determine whether a member is not nullable, base assumption being that all fields and method return values are nullable if not annotated. Based on `@NotNull`/`@Null`/`@NotEmpty`/`@NotBlank`.
2. Populate list of "required" fields/methods for objects if `JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED`/`JakartaValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED` is being provided respectively (i.e. this is an "opt-in").
3. Populate "minItems" and "maxItems" for containers (i.e. arrays and collections). Based on `@Size`/`@NotEmpty`.
4. Populate "minLength" and "maxLength" for strings. Based on `@Size`/`@NotEmpty`/`@NotBlank`.
5. Populate "format" for strings. Based on `@Email`, can be "email" or "idn-email" depending on whether `JakartaValidationOption.PREFER_IDN_EMAIL_FORMAT` is being provided.
6. Populate "pattern" for strings. Based on `@Pattern`/`@Email`, if `JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS` is being provided (i.e. this is an "opt-in").
7. Populate "minimum"/"exclusiveMinimum" for numbers. Based on `@Min`/`@DecimalMin`/`@Positive`/`@PositiveOrZero`.
8. Populate "maximum"/"exclusiveMaximum" for numbers. Based on `@Max`/`@DecimalMax`/`@Negative`/`@NegativeOrZero`.

Schema attributes derived from validation annotations on fields are also applied to their respective getter methods.  
Schema attributes derived from validation annotations on getter methods are also applied to their associated fields.

To use it, just pass a module instance into `SchemaGeneratorConfigBuilder.with(Module)`, optionally providing `JakartaValidationOption` values in the module's constructor and/or specifying validation groups to filter by via `.forValidationGroups()`.

```java
module.forValidationGroups(JsonSchemaValidation.class);
```

The `jakarta.validation.constraints` annotations cater for a `groups` parameter to be added, to allow different sets of validations to be applied under different circumstances.
Via `.forValidationGroups()` you're able to indicate which `groups` should be considered during the schema generation.
Without specifying particular groups via `.forValidationGroups()`, no filtering will be applied – i.e. all supported `jakarta.validation.constraints` annotations will be considered regardless of their respective `groups`.
