# Generator – Advanced Configurations
When all of the above configuration options are insufficient to achieve your requirements, there are some more advanced configurations you can resort to.

## Instance Attribute Overrides
```java
configBuilder.forFields()
    .withInstanceAttributeOverride((node, field, context) -> node
            .put("$comment", "Field name in code: " + field.getDeclaredName()));
configBuilder.forMethods()
    .withInstanceAttributeOverride((node, method, context) -> node
            .put("readOnly", true));
```

If you want to set an attribute that is missing in the supported [Individual Configurations](#generator-individual-configurations) for fields/methods or just want to have the last say in what combination of attribute values is being set for a field/method, you can use the following configurations:

* `SchemaGeneratorConfigBuilder.forFields().withInstanceAttributeOverride()`
* `SchemaGeneratorConfigBuilder.forMethods().withInstanceAttributeOverride()`

All defined overrides will be applied in the order of having been added to the `SchemaGeneratorConfigBuilder`. Each receiving the then-current set of attributes on an `ObjectNode` which can be freely manipulated.

## Type Attribute Overrides
```java
configBuilder.forTypesInGeneral()
    .withTypeAttributeOverride((node, scope, context) -> node
            .put("$comment", "Java type: " + scope.getType().getErasedType().getName()));
```

Similarly to (but not quite the same as) the [Instance Attribute Overrides](#instance-attribute-overrides) for fields/methods you can add missing attributes or manipulate collected ones on a per-type level through the following configuration:

* `SchemaGeneratorConfigBuilder.forTypesInGeneral().withTypeAttributeOverride()`

All defined overrides will be applied in the order of having been added to the `SchemaGeneratorConfigBuilder`.
Each receiving the then-current type definition including the collected set of attributes on an `ObjectNode` which can be freely manipulated.

## Target Type Overrides
> E.g. for the `value` field in the following class you may know that the returned value is either a `String` or a `Number` but there is no common supertype but `Object` that can be declared:

```java
class ExampleForTargetTypeOverrides {
    @ValidOneOfTypes({String.class, Number.class})
    private Object value;

    public void setValue(String textValue) {
        this.value = textValue;
    }
    public void setValue(Number numericValue) {
        this.value = numericValue;
    }
}
```

> This could be solved by the following configuration:

```java
configBuilder.forFields()
    .withTargetTypeOverridesResolver(field -> Optional
            .ofNullable(field.getAnnotationConsideringFieldAndGetterIfSupported(ValidOneOfTypes.class))
            .map(ValidOneOfTypes::value).map(Stream::of)
            .map(stream -> stream.map(specificSubtype -> field.getContext().resolve(specificSubtype)))
            .map(stream -> stream.collect(Collectors.toList()))
            .orElse(null));
```

> The generated schema would look like this then:

```json
{
    "type": "object",
    "properties": {
        "value": {
            "anyOf": [
                { "type": "string" },
                { "type": "number" }
            ]
        }
    }
}
```

Java does not support multiple type alternatives to be declared. This means you may have to declare a rather generic type on a field or as a method's return value even though there is only a finite list of types that you actually expect to be returned.
To improve the generated schema by listing the actual alternatives via `"anyOf"`, you can make use of the following configurations:

* `SchemaGeneratorConfigBuilder.forFields().withTargetTypeOverridesResolver()`
* `SchemaGeneratorConfigBuilder.forMethods().withTargetTypeOverridesResolver()`

## Subtype Resolvers
> E.g. to replace every occurrence of the `Animal` interface with the `Cat` and `Dog` implementations:

```java
configBuilder.forTypesInGeneral()
    .withSubtypeResolver((declaredType, generationContext) -> {
        if (declaredType.getErasedType() == Animal.class) {
            TypeContext typeContext = generationContext.getTypeContext();
            return Arrays.asList(
                    typeContext.resolveSubtype(declaredType, Cat.class),
                    typeContext.resolveSubtype(declaredType, Dog.class)
            );
        }
        return null;
    });
```

When a declared type is not too broad as in the example for [Target Type Overrides](#target-type-overrides) above, but rather an appropriate supertype or interface. You may also want to list the alternative implementations via `"anyOf"` wherever you encounter an `abstract` class or interface.
In order to reflect Java's polymorphism, you can make use of the following configuration:

* `SchemaGeneratorConfigBuilder.forTypesInGeneral().withSubtypeResolver()`

This can of course be more generalised by employing your reflections library of choice for scanning your classpath for all implementations of an encountered type.

## Custom Type Definitions
> E.g. treat `Collection`s as objects and not as `"type": "array"` (which is the default):

```java
configBuilder.forTypesInGeneral()
    .withCustomDefinitionProvider((javaType, context) -> {
        if (!javaType.isInstanceOf(Collection.class)) {
            return null;
        }
        ResolvedType generic = context.getTypeContext().getContainerItemType(javaType);
        SchemaGeneratorConfig config = context.getGeneratorConfig();
        return new CustomDefinition(context.getGeneratorConfig().createObjectNode()
                .put(config.getKeyword(SchemaKeyword.TAG_TYPE),
                        config.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                .set(config.getKeyword(SchemaKeyword.TAG_PROPERTIES),
                        config.createObjectNode().set("stream().findFirst().orElse(null)",
                                context.makeNullable(context.createDefinitionReference(generic)))));
    });
```

When all the generic configurations are not enough to achieve your specific requirements, you can still directly define parts of the schema yourself through the following configuration:

* `SchemaGeneratorConfigBuilder.forTypesInGeneral().withCustomDefinitionProvider()`

> (1) When including an unchanged schema of a different type, use `createDefinitionReference()`:

```java
configBuilder.forTypesInGeneral()
    .withCustomDefinitionProvider((javaType, context) ->
        javaType.isInstanceOf(UUID.class)
            ? new CustomDefinition(context.createDefinitionReference(
                    context.getTypeContext().resolve(String.class)))
            : null);
```

> (2) When including an unchanged schema of the same type, use `createStandardDefinitionReference()`:

```java
CustomDefinitionProviderV2 thisProvider = (javaType, context) -> 
    javaType.isInstanceOf(Collection.class)
        ? new CustomDefinition(
            context.createStandardDefinitionReference(javaType, thisProvider),
            DefinitionType.STANDARD, AttributeInclusion.NO)
        : null;
configBuilder.forTypesInGeneral()
    .withCustomDefinitionProvider(thisProvider);
```

> (3) When adjusting a schema of a different type, use `createDefinition()`:

```java
configBuilder.forTypesInGeneral()
    .withCustomDefinitionProvider((javaType, context) ->
        javaType.isInstanceOf(UUID.class)
            ? new CustomDefinition(context.createDefinition(
                    context.getTypeContext().resolve(String.class))
                        .put("format", "uuid"))
            : null);
```

> (4) When adjusting a schema of the same type, use `createStandardDefinition()`:

```java
CustomDefinitionProviderV2 thisProvider = (javaType, context) -> 
    javaType.isInstanceOf(Collection.class)
        ? new CustomDefinition(
                context.createStandardDefinition(javaType, thisProvider)
                    .put("$comment", "collection without other attributes"),
                DefinitionType.STANDARD, AttributeInclusion.NO)
        : null;
configBuilder.forTypesInGeneral()
    .withCustomDefinitionProvider(thisProvider);
```

<aside class="success">
    In order to avoid duplicating the logic for any nested schema, there are a number of methods to allow the "normal" schema generation to take over again.
</aside>

1. `SchemaGenerationContext.createDefinitionReference()` creates a temporarily empty node which will be populated later with either a `$ref` or the appropriate inline schema, i.e. in order to not produce an inline definition – thereby allowing you to avoid endless loops in case of circular references.
2. `SchemaGenerationContext.createStandardDefinitionReference()` to be used instead of the above when targeting the same type, to skip the current definition provider (and all previous ones) and thereby avoid endless loops.
3. `SchemaGenerationContext.createDefinition()` creates an inline definition of the given scope, allowing you to apply changes on top (similar to attribute overrides); thereby avoiding the need to manually create everything from scratch.
4. `SchemaGenerationContext.createStandardDefinition()` to be used instead of the above when targeting the same type, to skip the current definition provider (and all previous ones) and thereby avoid endless loops.

Other useful methods available in the context of a custom definition provider are:

* `SchemaGenerationContext.getGeneratorConfig().getObjectMapper().readTree()` allowing you to parse a string into a json (schema), in case you prefer to statically provide (parts of) the custom definitions.
* `SchemaGenerationContext.getTypeContext().resolve()` allowing you to produce `ResolvedType` instances which are expected by various other methods.

<aside class="notice">
    On the <code>CustomDefinition</code>'s constructor, you are able to decide whether it should be "inlined" or always result in a referenced/central definition and
    whether or not the attributes collected through the various other <a href="#generator-individual-configurations">Individual Configurations</a> shall be added.
</aside>

## Custom Property Definitions
```java
// read a static schema string from an annotation
CustomPropertyDefinitionProvider provider = (member, context) -> Optional
        .ofNullable(member.getAnnotationConsideringFieldAndGetter(Subschema.class))
        .map(Subschema::value)
        .map(rawSchema -> {
            try {
                return context.getGeneratorConfig().getObjectMapper().readTree(rawSchema);
            } catch (Exception ex) {
                return null;
            }
        })
        .map(CustomPropertyDefinition::new)
        .orElse(null);
// if you don't rely on specific field/method functionality,
// you can reuse the same provider for both of them
configBuilder.forFields().withCustomDefinitionProvider(provider);
configBuilder.forMethods().withCustomDefinitionProvider(provider);
```

When not even the [Custom Type Definitions](#custom-type-definitions) are flexible enough for you and you need to consider the specific field/method context in which a type is being encountered, there is one last path you can take:

* `SchemaGeneratorConfigBuilder.forFields().withCustomDefinitionProvider()`
* `SchemaGeneratorConfigBuilder.forMethods().withCustomDefinitionProvider()`

<aside class="success">
    Apart from the given <code>FieldScope</code>/<code>MethodScope</code> input parameter,
    these are pretty much the same as the <a href="#custom-type-definitions">Custom Type Definitions</a> described above.
</aside>

<aside class="notice">
    By their very nature, a field/method schema is always going to be defined "in-line" since it cannot be re-used.
    However, you can still decide whether the attributes collected through the various other <a href="#generator-individual-configurations">Individual Configurations</a> shall be added,
    through the <code>AttributeInclusion</code> parameter in the <code>CustomPropertyDefinition</code>'s constructor.
</aside>
