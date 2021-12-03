# Java JSON Schema Generator
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator)

Creating JSON Schema (Draft 6, Draft 7 or Draft 2019-09) from your Java classes utilising Jackson (inspired by JJSchema).

Topics covered in this document are:
- [Usage](#usage)
  - [Dependency (Maven)](#dependency-maven)
  - [Code](#code)
    - [Complete/Minimal Example](#completeminimal-example)
    - [Toggling Standard Options (via OptionPresets)](#toggling-standard-options-via-optionpresets)
    - [Toggling Standard Options (individually)](#toggling-standard-options-individually)
    - [Adding Separate Modules (e.g. from another library)](#adding-separate-modules-eg-from-another-library)
    - [Defining Desired Behaviour via individual Configurations](#defining-desired-behaviour-via-individual-configurations)
      - [Example: Dynamically setting the `additionalProperties` attribute](#example-dynamically-setting-the-additionalproperties-attribute)
- [Supported JSON Schema attributes](#supported-json-schema-attributes)

----

## Documentation
JavaDoc is being used throughout the codebase, offering contextual information in your respective IDE or being available online through services like [javadoc.io](https://www.javadoc.io/doc/com.github.victools/jsonschema-generator).

Additional documentation can be found in the [Project Wiki](https://github.com/victools/jsonschema-generator/wiki).

----

## Usage
### Dependency (Maven)

```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-generator</artifactId>
    <version>[4.21.0,5.0.0)</version>
</dependency>
```

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
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
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
import com.github.victools.jsonschema.generator.SchemaVersion;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
```

#### Toggling Standard Options (individually)
As alternative to (or on top of) the predefined `OptionPreset`s, you can set the individual standard `Option`s directly on the `SchemaGeneratorConfigBuilder`:
```java
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09)
    .with(Option.FLATTENED_ENUMS)
    .without(Option.NULLABLE_FIELDS_BY_DEFAULT, Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT);
```

You can also define your own `OptionPreset` or provide an empty one and then build your desired configuration from there:
```java
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, new OptionPreset())
    .with(Option.ADDITIONAL_FIXED_TYPES, Option.PUBLIC_NONSTATIC_FIELDS);
```

#### Adding Separate Modules (e.g. from another library)
```java
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
```
```java
Module separateModule = new YourSeparateModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09)
    .with(separateModule);
```

For a list of available modules refer to the main [README](../README.md).

#### Defining Desired Behaviour via individual Configurations
```java
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeScope;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09);
configBuilder.forTypesInGeneral()
    // populate the "title" of all schemas with a description of the java type
    .withTitleResolver(TypeScope::getSimpleTypeDescription);
configBuilder.forFields()
    // show the original field name as the "description" (may differ from the overridden property name in the schema)
    .withDescriptionResolver(FieldScope::getDeclaredName);
```

##### Example: Dynamically setting the `additionalProperties` attribute
According to the [JSON Schema Specification](https://json-schema.org/understanding-json-schema/reference/object.html) (as of February 2020):
> The `additionalProperties` keyword is used to control the handling of extra stuff, that is, properties whose names are not listed in the `properties` keyword. By default any additional properties are allowed.
> The `additionalProperties` keyword may be either a boolean or an object. If `additionalProperties` is a boolean and set to false, no additional properties will be allowed.
> If `additionalProperties` is an object, that object is a schema that will be used to validate any additional properties not listed in `properties`.

While there are various ways to consider `additionalProperties`, the standard way could look like this (but may be simpler):
```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.util.Map;
```
```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09)
    .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);
configBuilder.forTypesInGeneral()
    .withAdditionalPropertiesResolver((scope) -> {
        if (scope.getType().isInstanceOf(Map.class)) {
            // within a Map<Key, Value> allow additionalProperties of the Value type
            // if no type parameters are defined, this will result in additionalProperties to be omitted (by way of return Object.class)
            return scope.getTypeParameterFor(Map.class, 1);
        }
        return null;
    });
```

## Supported JSON Schema attributes
|    # | Attribute | Description |
| ---: | :--- | :--- |
|    1 | `$schema` | Set based on the `SchemaVersion` specified in the `SchemaGeneratorConfigBuilder`'s constructor – can be toggled on/off via `Option.SCHEMA_VERSION_INDICATOR`. |
|    2 | `$id` | Collected value according to configuration (`SchemaGeneratorGeneralConfigPart.withIdResolver()`). |
|    3 | `$anchor` | Collected value according to configuration (`SchemaGeneratorGeneralConfigPart.withAnchorResolver()`). |
|    4 | `definitions` | Only for `SchemaVersion.DRAFT_7`: Filled with sub-schemas to support circular references – via `Option.DEFINITIONS_FOR_ALL_OBJECTS` it can be configured whether only sub-schemas appearing more than once are included or all. |
|    5 | `$defs` | From `SchemaVersion.DRAFT_2019_09`: Filled with sub-schemas to support circular references – via `Option.DEFINITIONS_FOR_ALL_OBJECTS` it can be configured whether only sub-schemas appearing more than once are included or all. |
|    6 | `$ref` | Used with relative references to sub-schemas in `definitions`. |
|    7 | `type` | Differentiating between `boolean`/`string`/`integer`/`number` for primitive/known types. `null` is added if a property is deemed nullable according to configuration (`SchemaGeneratorConfigPart.withNullableCheck()`). Arrays and subtypes of `Collection<?>` are treated as `array`, everything else as `object`. A declared type may be interpreted as one or multiple other types according to configuration (`SchemaGeneratorConfigPart.withTargetTypeOverridesResolver()`). |
|    8 | `properties` | Listing all detected fields and/or methods in an `object`. Which ones are being included can be steered by various `Option`s or via one of the provided `OptionPreset`s as well as by ignoring individual ones via configuration (`SchemaGeneratorConfigPart.withIgnoreCheck()`). Names can be altered via configuration (`SchemaGeneratorConfigPart.withPropertyNameOverrideResolver()`). |
|    9 | `items` | Indicating the type of `array`/`Collection` elements. |
|   10 | `required` | Listing the names of fields/methods that are deemed mandatory according to configuration (`SchemaGeneratorConfigPart.withRequiredCheck()`). |
|   11 | `allOf` | Used to combine general attributes derived from the type itself with attributes collected in the respective context of the associated field/method. |
|   12 | `anyOf` | Used to list alternatives according to configuration (`SchemaGeneratorGeneralConfigPart.withSubtypeResolver()`) and to indicate when a particular field/method can be of `type` `null` according to configuration (`SchemaGeneratorConfigPart.withNullableCheck()`). |
|   13 | `oneOf` | Is being considered when coming from a custom definition, but not added by default – the less strict `anyOf` is used instead. |
|   14 | `title` | Collected value according to configuration (`SchemaGeneratorConfigPart.withTitleResolver()`). |
|   15 | `description` | Collected value according to configuration (`SchemaGeneratorConfigPart.withDescriptionResolver()`). |
|   16 | `const` | Collected value according to configuration (`SchemaGeneratorConfigPart.withEnumResolver()`) if only a single value was found. |
|   17 | `enum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withEnumResolver()`) if multiple values were found. |
|   18 | `default` | Collected value according to configuration (`SchemaGeneratorConfigPart.withDefaultResolver()`). |
|   19 | `additionalProperties` | Collected value according to configuration (`SchemaGeneratorConfigPart.withAdditionalPropertiesResolver()`). |
|   20 | `patternProperties` | Collected value(s) according to configuration (`SchemaGeneratorConfigPart.withPatternPropertiesResolver()`). |
|   21 | `minLength` | Collected value according to configuration (`SchemaGeneratorConfigPart.withStringMinLengthResolver()`). |
|   22 | `maxLength` | Collected value according to configuration (`SchemaGeneratorConfigPart.withStringMaxLengthResolver()`). |
|   23 | `format` | Collected value according to configuration (`SchemaGeneratorConfigPart.withStringFormatResolver()`). |
|   24 | `pattern` | Collected value according to configuration (`SchemaGeneratorConfigPart.withStringPatternResolver()`). |
|   25 | `minimum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberInclusiveMinimumResolver()`). |
|   26 | `exclusiveMinimum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberExclusiveMinimumResolver()`). |
|   27 | `maximum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberInclusiveMaximumResolver()`). |
|   28 | `exclusiveMaximum` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberExclusiveMaximumResolver()`). |
|   29 | `multipleOf` | Collected value according to configuration (`SchemaGeneratorConfigPart.withNumberMultipleOfResolver()`). |
|   30 | `minItems` | Collected value according to configuration (`SchemaGeneratorConfigPart.withArrayMinItemsResolver()`). |
|   31 | `maxItems` | Collected value according to configuration (`SchemaGeneratorConfigPart.withArrayMaxItemsResolver()`). |
|   32 | `uniqueItems` | Collected value according to configuration (`SchemaGeneratorConfigPart.withArrayUniqueItemsResolver()`). |
|   33 | any other | You can directly manipulate the generated `ObjectNode` of a sub-schema – e.g. setting additional attributes – via configuration based on a given type in general (`SchemaGeneratorConfigBuilder.with(TypeAttributeOverride)`) and/or in the context of a particular field/method (`SchemaGeneratorConfigPart.withInstanceAttributeOverride()`). |

If all that is not flexible enough, you can freely define any schema definition for (parts of) your data structure through `SchemaGeneratorGeneralConfigPart.withCustomDefinitionProvider()`/`SchemaGeneratorConfigPart.withCustomDefinitionProvider()` respectively.
