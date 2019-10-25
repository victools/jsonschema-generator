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
    <version>3.3.0</version>
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

You can also define your own `OptionPreset` or provide an empty one and then build your desired configuration from there:
```java
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper, new OptionPreset())
    .with(Option.ADDITIONAL_FIXED_TYPES, Option.PUBLIC_NONSTATIC_FIELDS);
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
- [imIfOu/jsonschema-module-addon](https://github.com/imIfOu/jsonschema-module-addon) – deriving JSON Schema attributes from a custom annotation with various parameters, which is part of the module.

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

## Supported JSON Schema attributes
|    # | Attribute | Description |
| ---: | :--- | :--- |
|    1 | `$schema` | Fixed to "http://json-schema.org/draft-07/schema#" – can be toggled on/off via `Option.SCHEMA_VERSION_INDICATOR`. |
|    2 | `definitions` | Filled with sub-schemas to support circular references – via `Option.DEFINITIONS_FOR_ALL_OBJECTS` it can be configured whether only sub-schemas appearing more than once are included or all. |
|    3 | `$ref` | Used with relative references to sub-schemas in `definitions`. |
|    4 | `type` | Differentiating between `boolean`/`string`/`integer`/`number` for primitive/known types. `null` is added if a property is deemed nullable according to configuration (`SchemaGeneratorConfigPart.withNullableCheck()`). Arrays and sub-types of `Collection<?>` are treated as `array`, everything else as `object`. A declared type may be interpreted as another type according to configuration (`SchemaGeneratorConfigPart.withTargetTypeOverrideResolver()`). |
|    5 | `properties` | Listing all detected fields and/or methods in an `object`. Which ones are being included can be steered by various `Option`s or via one of the provided `OptionPreset`s as well as by ignoring individual ones via configuration (`SchemaGeneratorConfigPart.withIgnoreCheck()`). Names can be altered via configuration (`SchemaGeneratorConfigPart.withPropertyNameOverrideResolver()`). |
|    6 | `items` | Indicating the type of `array`/`Collection` elements. |
|    7 | `required` | Listing the names of fields/methods that are deemed mandatory according to configuration (`SchemaGeneratorConfigPart.withRequiredCheck()`). |
|    8 | `allOf` | Used to combine general attributes derived from the type itself with attributes collected in the respective context of the associated field/method. |
|    9 | `oneOf` | Used to indicate when a particular field/method can be of `type` `null`. |
|   10 | `title` | Collected value according to configuration (`SchemaGeneratorConfigPart.withTitleResolver()`). |
|   11 | `description` | Collected value according to configuration (`SchemaGeneratorConfigPart.withDescriptionResolver()`). |
|   12 | `const` | Collected value according to configuration (`SchemaGeneratorConfigPart.withEnumResolver()`) if only a single value was found. |
|   13 | `enum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withEnumResolver()`) if multiple values were found. |
|   14 | `default` | Collected value according to configuration (`SchemaGeneratorConfigPart.withDefaultResolver()`). |
|   15 | `minLength` | Collected value according to configuration (`SchemaGeneratorConfigPart.withStringMinLengthResolver()`). |
|   16 | `maxLength` | Collected value according to configuration (`SchemaGeneratorConfigPart.withStringMaxLengthResolver()`). |
|   17 | `format` | Collected value according to configuration (`SchemaGeneratorConfigPart.withStringFormatResolver()`). |
|   18 | `pattern` | Collected value according to configuration (`SchemaGeneratorConfigPart.withStringPatternResolver()`). |
|   19 | `minimum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberInclusiveMinimumResolver()`). |
|   20 | `exclusiveMinimum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberExclusiveMinimumResolver()`). |
|   21 | `maximum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberInclusiveMaximumResolver()`). |
|   22 | `exclusiveMaximum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberExclusiveMaximumResolver()`). |
|   23 | `multipleOf` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberMultipleOfResolver()`). |
|   24 | `minItems` | Collected value according to configuration (`SchemaGeneratorConfigPart.withArrayMinItemsResolver()`). |
|   25 | `maxItems` | Collected value according to configuration (`SchemaGeneratorConfigPart.withArrayMaxItemsResolver()`). |
|   26 | `uniqueItems` | Collected value according to configuration (`SchemaGeneratorConfigPart.withArrayUniqueItemsResolver()`). |
|   27 | any other | You can directly manipulate the generated `ObjectNode` of a sub-schema – e.g. setting additional attributes – via configuration based on a given type in general (`SchemaGeneratorConfigBuilder.with(TypeAttributeOverride)`) and/or in the context of a particular field/method (`SchemaGeneratorConfigPart.withInstanceAttributeOverride()`). |
