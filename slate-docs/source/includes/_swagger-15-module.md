# Swagger 1.5 Module
The [victools:jsonschema-module-swagger-1.5](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-module-swagger-1.5) provides a number of standard configurations for deriving JSON Schema attributes from `swagger` (1.5.x) annotations.

```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger15.SwaggerOption;


SwaggerModule module = new SwaggerModule(
        SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES,
        SwaggerOption.IGNORING_HIDDEN_PROPERTIES
);
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09)
    .with(module);
```

1. Set a field/method's "description" as per `@ApiModelProperty(value = ...)`
2. Set a type's "title" as per `@ApiModel(value = ...)` unless `SwaggerOption.NO_APIMODEL_TITLE` was provided (i.e. this is an "opt-out")
3. Set a type's "description" as per `@ApiModel(description = ...)` unless `SwaggerOption.NO_APIMODEL_DESCRIPTION` was provided (i.e. this is an "opt-out")
4. Ignore a field/method if `@ApiModelProperty(hidden = true)` and `SwaggerOption.IGNORING_HIDDEN_PROPERTIES` was provided (i.e. this is an "opt-in")
5. Override a field's property name as per `@ApiModelProperty(name = ...)` if `SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES` was provided (i.e. this is an "opt-in")
6. Indicate a number's (field/method) "minimum" (inclusive) according to `@ApiModelProperty(allowableValues = "range[...")`
7. Indicate a number's (field/method) "exclusiveMinimum" according to `@ApiModelProperty(allowableValues = "range(...")`
8. Indicate a number's (field/method) "maximum" (inclusive) according to `@ApiModelProperty(allowableValues = "range...]")`
9. Indicate a number's (field/method) "exclusiveMaximum" according to `@ApiModelProperty(allowableValues = "range...)")`
10. Indicate a field/method's "const"/"enum" as per `@ApiModelProperty(allowableValues = ...)` if it is not a numeric range declaration

Schema attributes derived from `@ApiModelProperty` on fields are also applied to their respective getter methods.  
Schema attributes derived from `@ApiModelProperty` on getter methods are also applied to their associated fields.

To use it, just pass a module instance into `SchemaGeneratorConfigBuilder.with(Module)`, optionally providing `SwaggerOption` values in the module's constructor.
