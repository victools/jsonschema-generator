# FAQ
## Is there a Gradle Plugin?
```groovy
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath group: 'com.github.victools', name: 'jsonschema-generator', version: '4.16.0'
    }
}
plugins {
    id 'java-library'
}

task generate {
    doLast {
        def configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
        // apply your configurations here
        def generator = new SchemaGenerator(configBuilder.build());
        // target the class for which to generate a schema
        def jsonSchema = generator.generateSchema(SchemaVersion.class);
        // handle generated schema, e.g. write it to the console or a file
        def jsonSchemaAsString = jsonSchema.toPrettyString();
        println jsonSchemaAsString
        new File(projectDir, "schema.json").text = jsonSchemaAsString
    }
}
```

There currently is no dedicated Gradle Plugin as such, but Gradle is flexible enough to allow you to use a java library straight from within the `build.gradle` file.
CHeckout https://github.com/victools/jsonschema-gradle-example for the complete example.

## What about enums?
> If you have a custom serialization logic for converting enum values to strings, you can re-use it in order to generate the correct list of allowed values:

```java
ObjectMapper objectMapper = new ObjectMapper();
// make use of your enum handling e.g. through your own serializer
// objectMapper.registerModule(new YourCustomEnumSerializerModule());
configBuilder.with(new EnumModule(possibleEnumValue -> {
    try {
        String valueInQuotes = objectMapper.writeValueAsString(possibleEnumValue);
        return valueInQuotes.substring(1, valueInQuotes.length() - 1);
    } catch (JsonProcessingException ex) {
        throw new IllegalStateException(ex);
    }
}));
```

Enums are a special construct for which there are multiple options:

1. `Option.FLATTENED_ENUMS` (which is part of the `OptionPreset.PLAIN_JSON`)
   * This defines an enum as `{ "type": "string", "enum": ["VALUE1", "VALUE2"] }` with the `name()` method being called on each possible enum value.
   * If there is only one enum value, it will be set as `"const"` instead.
   * Such an enum representation will always be in-lined and not moved into the `"definitions"`.
2. `Option.SIMPLIFIED_ENUMS`(which is part of the `OptionPreset.JAVA_OBJECT` and `OptionPreset.FULL_DOCUMENTATION`)
   * This treats enums like any other class but hiding some methods and listing the possible enum values as `"enum"`/`"const"` on the `name()` method.
3. Using neither of the two `Option`s above will let them be handled like any other class (unless there are further configurations taking care of enums).
4. The `JacksonModule` comes with two more alternatives:
   * `JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE`, behaving like `Option.FLATTENED_ENUMS` but looking-up the respective values via the `@JsonValue` annotated method.
   * `JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY`, behaving like `Option.FLATTENED_ENUMS` but looking-up the respective values via the `@JsonProperty` annotation on each enum value/constant.
5. Write your own custom definition provider or re-use the `EnumModule` class as in the shown example.

## Where can I find some more configuration examples?
Internally, a number of the standard `Option`s are realized via [Individual Configurations](#individual-configurations) and/or [Advanced Configurations](#advanced-configurations) – grouped into `Module`s.   
These make for excellent [examples](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-generator/src/main/java/com/github/victools/jsonschema/generator/impl/module) to get you started into your own setup, if the existing `Option`s do not cover your specific requirements.

## How to represent a `Map<K, V>` in a generated schema?
```java
configBuilder.forTypesInGeneral()
    .withPatternPropertiesResolver((scope) -> {
        if (scope.getType().isInstanceOf(Map.class)) {
            // within a Map<Key, Value> allow additional properties of the Value type, with purely alphabetic keys
            Map<String, Type> patternProperties = new HashMap<>();
            // theoretically, you could derive an appropriate schema from the key type, accessible via the same getTypeParameterFor() method
            // if no type parameters are defined, this will result in `{}` to be set as value schema and thereby allowing any values for matching keys
            patternProperties.put("^[a-zA-Z]+$", scope.getTypeParameterFor(Map.class, 1));
            return patternProperties;
        }
        return null;
    });
```

By default, a `Map` will be treated like any other type – i.e. most likely a simple `{ "type": "object" }` without many further details if you use the `OptionPreset.PLAIN_JSON` or otherwise ignore methods.
The following are the two most common approaches:

 1. Indicate the value type `V` as the expected type for any `"additionalProperties"` by including the `Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES`.   
 You may also want to consider including the `Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT` to forbid `"additionalProperties"` everywhere else.
 2. If you have a clear idea of how the key type `K` will be serialized, you could also describe valid keys via `"patternProperties"` instead – as per the example on the right.

Refer to https://json-schema.org/understanding-json-schema/reference/regular_expressions.html for a description of how to build valid patterns.

## How to populate `default` values?
> Example 1

```java
configBuilder.forFields().withDefaultResolver(field -> {
    JsonProperty annotation = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
    return annotation == null || annotation.defaultValue().isEmpty() ? null : annotation.defaultValue());
});
```

> Example 2

```java
ConcurrentMap<Class<?>, Object> instanceCache = new ConcurrentHashMap<>();
configBuilder.forFields().withDefaultResolver(field -> {
    Class<?> declaringClass = field.getDeclaringType().getErasedType();
    if (!field.isFakeContainerItemScope()
            && declaringClass.getName().startsWith("your.package")) {
        MethodScope getter = field.findGetter();
        if (getter != null) {
            try {
                Object instance = instanceCache.computeIfAbsent(declaringClass, declaringClass::newInstance);
                Object defaultValue = getter.getRawMember().invoke(instance);
                return defaultValue;
            } catch (Exception ex) {
                // most likely missing a no-args constructor
            }
        }
    }
    return null;
});
```

The short answer is: via the `withDefaultResolver()` – one of the [Individual Configurations](#13-individual-configurations).   
The exact details depend on how the `default` value can be determined.

1. If the `default` value is explicitly stated via some kind of annotation, it might be as simple as "Example 1" on the right.
2. If the `default` value is only set in code, and you cannot or don't want to maintain that piece of information twice this can get a bit more advanced. Here assuming your own classes all have a default no-args constructor and conventional getters as in "Example 2" on the right.
