# Generator – Individual Configurations
> E.g. for the given configuration:

```java
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09);
configBuilder.forField()
    .withTitleResolver(field -> field.getName() + " = "
            + (field.isFakeContainerItemScope() ? "(fake) " : "(real) ")
            + field.getSimpleTypeDescription())
    .withDescriptionResolver(field -> "original type = "
            + field.getContext().getSimpleTypeDescription(field.getDeclaredType()));
JsonNode mySchema = new SchemaGenerator(configBuilder.build())
        .generateSchema(MyClass.class);
```

> and target class:

```java
class MyClass {
    public List<String> texts;
}
```

> The following schema will be generated:

```json
{
  "type": "object",
  "properties": {
    "texts": {
      "type": "array",
      "title": "texts = (real) List<String>",
      "description": "original type = List<String>",
      "items": {
        "type": "string",
        "title": "texts = (fake) String",
        "description": "original type = List<String>"
      }
    }
  }
}
```

In order to control various attributes being set during the schema generation, you can define for each (supported) one of them individually how a respective value should be resolved. Overall, you usually have the same configuration options either for:

* an encountered type in general via `SchemaGeneratorConfigBuilder.forTypesInGeneral()` or
* in the context of a specific field via `SchemaGeneratorConfigBuilder.forFields()` or 
* in the context of a specific method's return value via `SchemaGeneratorConfigBuilder.forMethods()`.

<aside class="warning">
    As general rules:
    <ol>
        <li>Returning <code>null</code> in any of those individual configurations means that no special handling applies and the next configuration of the same kind will be consulted.</li>
        <li><strong>Order matters!</strong> The configurations will be consulted in the order they are set on the <code>SchemaGeneratorConfigBuilder</code>. Configurations from <code>Option</code>s always go last.</li>
        <li>You may want to specifically consider or return <code>null</code> for cases where <code>FieldScope.isFakeContainerItemScope()</code>/<code>MethodScope.isFakeContainerItemScope()</code> returns <code>true</code>.<br/>
            Because each individual configuration added via <code>.forField()</code> or <code>.forMethods()</code> is being called upon twice if the encountered type is a "container" (i.e. an array or subtype of <code>Collection</code>).</li>
    </ol>
</aside>

The [jsonschema-generator README](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-generator#supported-json-schema-attributes) contains a list of the supported JSON Schema attributes.  
The following list of individual configuration options on the `SchemaGeneratorConfigBuilder` is to a large extent the inverse of that list.

## `"$id"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withIdResolver(scope -> scope.getType().getErasedType() == MyClass.class ? "main-schema-id" : null);
```

`withIdResolver()` is expecting the `"$id"` attribute's value to be returned based on a given `TypeScope` – in case of multiple configurations, the first non-`null` value will be applied.

<aside class="notice">
    While an <code>"$id"</code> value may be included in a (sub)schema, it is <strong>not</strong> considered in the standard <code>"$ref"</code> values within the generated overall schema.
</aside>

## `"$anchor"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withAnchorResolver(scope -> scope.getType().getErasedType() == AnchorClass.class ? "anchor-value" : null);
```

`withAnchorResolver()` is expecting the `"$anchor"` attribute's value to be returned based on a given `TypeScope` – in case of multiple configurations, the first non-`null` value will be applied.

<aside class="notice">
    While an <code>"$anchor"</code> value may be included in a (sub)schema, it is <strong>not</strong> considered in the standard <code>"$ref"</code> values within the generated overall schema.
</aside>

## Order of entries in `"properties"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withPropertySorter(PropertySortUtils.SORT_PROPERTIES_FIELDS_BEFORE_METHODS
        .thenComparing((memberOne, memberTwo) ->
            // sort fields/methods alphabetically, while ignoring upper/lower case
            memberOne.getSchemaPropertyName().toLowerCase()
                .compareTo(memberTwo.getSchemaPropertyName().toLowerCase()));
```

`withPropertySorter()` is expecting a `Comparator` for sorting an object's fields and methods in the produced `"properties"` – this replaces any previously given sorting algorithm, i.e. only one `Comparator` can be set – by default, fields are listed before methods with each group in alphabetical order.

<aside class="notice">
    The JSON Schema specification does not offer a keyword to indicate that the order of <code>properties</code> within a schema must be a met by a JSON instance being validated.
    <br/>
    However, some libraries that produce a web form based on a given JSON Schema may present the respective input fields in the order of appearance in the schema. If you intend to use the generated JSON Schema for such a use-case, the <code>withPropertySorter()</code> configuration is the way to go.
</aside>

## Names in global `"$defs"`/`"definitions"`
```java
configBuilder.forTypesInGeneral()
    .withDefinitionNamingStrategy(new DefaultSchemaDefinitionNamingStrategy() {
        @Override
        public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext context) {
            return super.getDefinitionNameForKey(key, generationContext).toLowerCase();
        }
        @Override
        public void adjustDuplicateNames(Map<DefinitionKey, String> duplicateNames, SchemaGenerationContext context) {
            char suffix = 'a';
            duplicateNames.entrySet().forEach(entry -> entry.setValue(entry.getValue() + "-" + suffix++));
        }
        @Override
        public String adjustNullableName(DefinitionKey key, String definitionName, SchemaGenerationContext context) {
            return definitionName + "-nullable";
        }
    });
```

`withDefinitionNamingStrategy()` is expecting a `SchemaDefinitionNamingStrategy` that defines what keys to assign to subschemas in the `"definitions"`/`"$defs"`.   
Optionally, you can override the logic how to adjust them in case of multiple types having the same name and for a subschema's nullable alternative.

There is a `DefaultSchemaDefinitionNamingStrategy`, which is being applied if you don't set a specific naming strategy yourself:

* It uses a given type's simple class name (i.e. without package prefix) as the definition name, potentially prepending type arguments in case of it being a parameterized type.
* Duplicate names may occur if the same simple class name (with identical type parameters) appears multiple times in your schema, i.e. from different packages. As the definition names need to be unique, those are then prepended with a running number. E.g. `java.time.DateTime` and `your.pkg.DateTime` would be represented by `DateTime-1` and `DateTime-2`.
* When a given type appears in its `null`able and non-`null`able form, two separate definitions may be included to reduce duplication. The "normal" named one and the `null`able one getting a `"-nullable"` suffix to its definition name.

<aside class="warning">
    To avoid illegal characters being used in <code>"$ref"</code> values referencing a definition, the definition names are automatically cleaned-up. I.e. the actual definition names in the generated schema may differ from what is being returned by your <code>SchemaDefinitionNamingStrategy</code> implementation.
    <br/>
    When the <code>Option.PLAIN_DEFINITION_KEYS</code> is being enabled, that automatic clean-up gets even stricter, in order to comply with even more limited set of allowed characters according to the OpenAPI specification.
</aside>

## Names of fields/methods in an object's `properties`
```java
configBuilder.forFields()
    .withPropertyNameOverrideResolver(field -> Optional
            .ofNullable(field.getAnnotationConsideringFieldAndGetter(JsonProperty.class))
            .map(JsonProperty::value).orElse(null));
configBuilder.forMethods()
    .withPropertyNameOverrideResolver(method -> method.getName().startsWith("is") && method.getArgumentCount() == 0
            ? method.getName().substring(2, method.getName().length() - 2) : null);
```

`withPropertyNameOverrideResolver()` is expecting an alternative name to be returned for a given `FieldScope`/`MethodScope` to be used as key in the containing object's `"properties"` – the first non-`null` value will be applied.

<aside class="notice">
    This configuration is useful only if the naming of fields/methods in your code don't match the corresponding structure being described in the generated schema.
    <br/>
    Most likely, there will be a convention (e.g. <code>@JsonNamingStrategy</code>) or specific setting (e.g. <code>@JsonProperty("x")</code>) indicating what alternative name to use. That same logic would need to be replicated here then.
</aside>

## Omitting/ignoring certain fields/methods
```java
configBuilder.forFields()
    .withIgnoreCheck(field -> field.getName().startsWith("_"));
configBuilder.forMethods()
    .withIgnoreCheck(method -> !method.isVoid() && method.getType().getErasedType() == Object.class);
```

`withIgnoreCheck()` is expecting the indication to be returned whether a given `FieldScope`/`MethodScope` should be excluded from the generated schema. If any check returns `true`, the field/method will be ignored.

## Decide whether a field's/method's value may be `null`
```java
configBuilder.forFields()
    .withNullableCheck(field -> field.getAnnotationConsideringFieldAndGetter(Nullable.class) != null);
configBuilder.forMethods()
    .withNullableCheck(method -> method.getAnnotationConsideringFieldAndGetter(NotNull.class) == null);
```

`withNullableCheck()` is expecting the indication to be returned whether a given `FieldScope`/`MethodScope` may return `null` and should therefore include `"null"` in the generated schema's `"type"`.

* If there is no check or all of them return `null`, the default will be applied (depending on whether `Option.NULLABLE_FIELDS_BY_DEFAULT`/`Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT` were enabled).
* If any check returns `true`, the field/method will be deemed nullable.
* Otherwise, the field/method will be deemed not-nullable.

## `"required"` Keyword
```java
configBuilder.forFields()
    .withRequiredCheck(field -> field.getAnnotationConsideringFieldAndGetter(Nullable.class) == null);
configBuilder.forMethods()
    .withRequiredCheck(method -> method.getAnnotationConsideringFieldAndGetter(NotNull.class) != null);
```

`withRequiredCheck()` is expecting the indication to be returned whether a given `FieldScope`/`MethodScope` should be included in the `"required"` attribute – if any check returns `true`, the field/method will be deemed `"required"`.

## `"dependentRequired"` Keyword
```java
configBuilder.forFields()
    .withDependentRequiresResolver(field -> Optional
        .ofNullable(field.getAnnotationConsideringFieldAndGetter(IfPresentAlsoRequire.class))
        .map(IfPresentAlsoRequire::value)
        .map(Arrays::asList)
        .orElse(null));
configBuilder.forMethods()
    .withDependentRequiresResolver(method -> Optional.ofNullable(method.findGetterField())
        .map(FieldScope::getSchemaPropertyName)
        .map(Collections::singletonList)
        .orElse(null));
```

`withDependentRequiresResolver()` is expecting the names of other properties to be returned, which should be deemed "required", if the property represented by the given field/method is present.
The results of all registered resolvers are being combined.

## `"readOnly"` Keyword
```java
configBuilder.forFields()
    .withReadOnlyCheck(field -> field.getAnnotationConsideringFieldAndGetter(ReadOnly.class) != null);
configBuilder.forMethods()
    .withReadOnlyCheck(method -> method.getAnnotationConsideringFieldAndGetter(ReadOnly.class) != null);
```

`withReadOnlyCheck()` is expecting the indication to be returned whether a given `FieldScope`/`MethodScope` should be included in the `"readOnly"` attribute – if any check returns `true`, the field/method will be deemed `"readOnly"`.

## `"writeOnly"` Keyword
```java
configBuilder.forFields()
    .withWriteOnlyCheck(field -> field.getAnnotationConsideringFieldAndGetter(WriteOnly.class) != null);
configBuilder.forMethods()
    .withWriteOnlyCheck(method -> method.getAnnotationConsideringFieldAndGetter(WriteOnly.class) != null);
```

`withWriteOnlyCheck()` is expecting the indication to be returned whether a given `FieldScope`/`MethodScope` should be included in the `"writeOnly"` attribute – if any check returns `true`, the field/method will be deemed `"writeOnly"`.

## `"title"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withTitleResolver(scope -> scope.getType().getErasedType() == YourClass.class ? "main schema title" : null);
configBuilder.forFields()
    .withTitleResolver(field -> field.getType().getErasedType() == String.class ? "text field" : null);
configBuilder.forMethods()
    .withTitleResolver(method -> method.getName().startsWith("get") ? "getter" : null);
```

`withTitleResolver()` is expecting the `"title"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"description"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withDescriptionResolver(scope -> scope.getType().getErasedType() == YourClass.class ? "main schema description" : null);
configBuilder.forFields()
    .withDescriptionResolver(field -> field.getType().getErasedType() == String.class ? "text field" : null);
configBuilder.forMethods()
    .withDescriptionResolver(method -> method.getName().startsWith("get") ? "getter" : null);
```

`withDescriptionResolver()` is expecting the `"description"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"default"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withDefaultResolver(scope -> scope.getType().getErasedType() == boolean.class ? Boolean.FALSE : null);
configBuilder.forFields()
    .withDefaultResolver(field -> field.getType().getErasedType() == String.class ? "" : null);
configBuilder.forMethods()
    .withDefaultResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetter(Default.class))
            .map(Default::value).orElse(null));
```

`withDefaultResolver()` is expecting the `"default"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied, which will be serialised through the `ObjectMapper` instance provided in the `SchemaGeneratorConfigBuilder`'s constructor.

## `"const"`/`"enum"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withEnumResolver(scope -> scope.getType().getErasedType().isEnum()
            ? Stream.of(scope.getType().getErasedType().getEnumConstants())
                    .map(v -> ((Enum) v).name()).collect(Collectors.toList())
            : null);
configBuilder.forFields()
    .withEnumResolver(field -> Optional
            .ofNullable(field.getAnnotationConsideringFieldAndGetter(AllowedValues.class))
            .map(AllowedValues::valueList).orElse(null));
configBuilder.forMethods()
    .withEnumResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetter(SupportedValues.class))
            .map(SupportedValues::values).map(Arrays::asList).orElse(null));
```

`withEnumResolver()` is expecting the `"const"`/`"enum"` attribute's value(s) to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied, which will be serialised through the `ObjectMapper` instance provided in the `SchemaGeneratorConfigBuilder`'s constructor.

## `"additionalProperties"` Keyword
> Option 1: derive plain type from given scope

One version of the `withAdditionalPropertiesResolver()` is expecting the `"additionalProperties"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

```java
configBuilder.forTypesInGeneral()
    .withAdditionalPropertiesResolver(scope -> Object.class);
configBuilder.forFields()
    .withAdditionalPropertiesResolver(field -> field.getType().getErasedType() == Object.class
            ? null : Void.class);
configBuilder.forMethods()
    .withAdditionalPropertiesResolver(method -> method.getType().getErasedType() == Map.class
            ? method.getTypeParameterFor(Map.class, 1) : Void.class);
```

* If `null` is being returned, the next registered `AdditionalPropertiesResolver` will be checked. If all return `null`, the attribute will be omitted.
* If `Object.class` is being returned, the `"additionalProperties"` attribute will be omitted.
* if `Void.class` is being returned, the `"additionalProperties"` will be set to `false`.
* If any other type is being returned (e.g. other `Class` or a `ResolvedType`) a corresponding schema will be included in `"additionalProperties"`.

> Option 2: specify explicit subschema

Another version of the `withAdditionalPropertiesResolver()` is expecting the `"additionalProperties"` attribute's value to be provided directly as a `JsonNode` (e.g., `ObjectNode`) representing the desired subschema.
In this case, both the `TypeScope`/`FieldScope`/`MethodScope` and the overall generation context are being provided as input parameters.

```java
configBuilder.forTypesInGeneral()
    .withAdditionalPropertiesResolver((scope, context) -> BooleanNode.TRUE);
configBuilder.forFields()
    .withAdditionalPropertiesResolver((field, context) -> field.getType().getErasedType() == Object.class
            ? null : BooleanNode.FALSE);
configBuilder.forMethods()
    .withAdditionalPropertiesResolver((method, context) -> {
        if (!method.getType().isInstanceOf(Map.class)) {
            return null;
        }
        ResolvedType valueType = method.getTypeParameterFor(Map.class, 1);
        if (valueType == null || valueType.getErasedType() == Object.class) {
            return null;
        }
        return context.createStandardDefinitionReference(method.asFakeContainerItemScope(Map.class, 1), null);
    });
```

* If `null` is being returned, the next registered `AdditionalPropertiesResolver` will be checked. If all return `null`, the attribute will be omitted.
* If `BooleanNode.TRUE` is being returned, the `"additionalProperties"` attribute will be omitted.
* if `BooleanNode.FALSE` is being returned, the `"additionalProperties"` will be set to `false`.
* If any other subschema is being returned, that will be included as `"additionalProperties"` attribute directly.

This usage of the `FieldScope`/`MethodScope` potentially via `asFakeContainerItemScope()` has the advantage of allowing the consideration of annotations on generic parameters, such as the one on `Map<String, @Min(10) Integer>` when that is the declared type of a field/method.
<aside class="success">
    The third example shown for the deriving <code>"additionalProperties"</code> from a <code>Map</code> value is the default behaviour offered via the <code>Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES</code>.
</aside>

## `"patternProperties"` Keyword
> Option 1: derive plain types from given scope

One version of the `withPatternPropertiesResolver()` is expecting a `Map` of regular expressions to their corresponding allowed types to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

```java
configBuilder.forTypesInGeneral()
    .withPatternPropertiesResolver(scope -> scope.getType().isInstanceOf(Map.class)
            ? Collections.singletonMap("^[a-zA-Z]+$", scope.getTypeParameterFor(Map.class, 1)) : null);
configBuilder.forFields()
    .withPatternPropertiesResolver(field -> field.getType().isInstanceOf(TypedMap.class)
            ? Collections.singletonMap("_int$", int.class) : null);
configBuilder.forMethods()
    .withPatternPropertiesResolver(method -> method.getType().isInstanceOf(StringMap.class)
            ? Collections.singletonMap("^txt_", String.class) : null);
```
Each regular expression will be included as key in the `"patternProperties"` attribute with a schema representing the mapped type as the corresponding value.

> Option 2: specify explicit subschema

Another version of the `withPatternPropertiesResolver()` is expecting a `Map` with each value being a `JsonNode` (e.g., `ObjectNode`) representing the respective desired subschema.
In this case, both the `TypeScope`/`FieldScope`/`MethodScope` and the overall generation context are being provided as input parameters.

> The generation of the subschema could look similar to the example given for the `"additionalProperties"` attribute above.

The usage of the `FieldScope`/`MethodScope` potentially via `asFakeContainerItemScope()` has the advantage of allowing the consideration of annotations on generic parameters, such as the one on `Map<String, @Min(10) Integer>` when that is the declared type of a field/method.

## `"minLength"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withStringMinLengthResolver(scope -> scope.getType().getErasedType() == UUID.class ? 36 : null);
configBuilder.forFields()
    .withStringMinLengthResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(NotEmpty.class) == null ? null : 1);
configBuilder.forMethods()
    .withStringMinLengthResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Size.class))
            .map(Size::min).orElse(null));
```

`withStringMinLengthResolver()` is expecting the `"minLength"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"maxLength"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withStringMaxLengthResolver(scope -> scope.getType().getErasedType() == UUID.class ? 36 : null);
configBuilder.forFields()
    .withStringMaxLengthResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(DbKey.class) == null ? null : 450);
configBuilder.forMethods()
    .withStringMaxLengthResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Size.class))
            .map(Size::max).orElse(null));
```

`withStringMaxLengthResolver()` is expecting the `"maxLength"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"format"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withStringFormatResolver(scope -> scope.getType().getErasedType() == UUID.class ? "uuid" : null);
configBuilder.forFields()
    .withStringFormatResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(Email.class) == null ? null : "email");
configBuilder.forMethods()
    .withStringFormatResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Schema.class))
            .map(Schema::format).orElse(null));
```

`withStringFormatResolver()` is expecting the `"format"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"pattern"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withStringPatternResolver(scope -> scope.getType().getErasedType() == UUID.class
            ? "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$" : null);
configBuilder.forFields()
    .withStringPatternResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(Email.class) == null ? null : "^.+@.+\\..+$");
configBuilder.forMethods()
    .withStringPatternResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Pattern.class))
            .map(Pattern::value).orElse(null));
```

`withStringPatternResolver()` is expecting the `"pattern"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"minimum"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withNumberInclusiveMinimumResolver(scope -> scope.getType().getErasedType() == PositiveInt.class
            ? BigDecimal.ONE : null);
configBuilder.forFields()
    .withNumberInclusiveMinimumResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(NonNegative.class) == null ? null : BigDecimal.ZERO);
configBuilder.forMethods()
    .withNumberInclusiveMinimumResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Minimum.class))
            .filter(a -> !a.exclusive()).map(Minimum::value).orElse(null));
```

`withNumberInclusiveMinimumResolver()` is expecting the `"minimum"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"exclusiveMinimum"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withNumberExclusiveMinimumResolver(scope -> scope.getType().getErasedType() == PositiveDecimal.class
            ? BigDecimal.ZERO : null);
configBuilder.forFields()
    .withNumberExclusiveMinimumResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(Positive.class) == null ? null : BigDecimal.ZERO);
configBuilder.forMethods()
    .withNumberExclusiveMinimumResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Minimum.class))
            .filter(Minimum::exclusive).map(Minimum::value).orElse(null));
```

`withNumberExclusiveMinimumResolver()` is expecting the `"exclusiveMinimum"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"maximum"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withNumberInclusiveMaximumResolver(scope -> scope.getType().getErasedType() == int.class
            ? new BigDecimal(Integer.MAX_VALUE) : null);
configBuilder.forFields()
    .withNumberInclusiveMaximumResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(NonPositive.class) == null ? null : BigDecimal.ZERO);
configBuilder.forMethods()
    .withNumberInclusiveMaximumResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Maximum.class))
            .filter(a -> !a.exclusive()).map(Maximum::value).orElse(null));
```

`withNumberInclusiveMaximumResolver()` is expecting the `"maximum"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"exclusiveMaximum"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withNumberExclusiveMaximumResolver(scope -> scope.getType().getErasedType() == NegativeInt.class
            ? BigDecimal.ZERO : null);
configBuilder.forFields()
    .withNumberExclusiveMaximumResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(Negative.class) == null ? null : BigDecimal.ZERO);
configBuilder.forMethods()
    .withNumberExclusiveMaximumResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Maximum.class))
            .filter(Maximum::exclusive).map(Maximum::value).orElse(null));
```

`withNumberExclusiveMaximumResolver()` is expecting the `"exclusiveMaximum"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"multipleOf"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withNumberMultipleOfResolver(scope -> scope.getType().getErasedType() == int.class
            ? BigDecimal.ONE : null);
configBuilder.forFields()
    .withNumberMultipleOfResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(Currency.class) == null ? null : new BigDecimal("0.01"));
configBuilder.forMethods()
    .withNumberMultipleOfResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(NumericConstraint.class))
            .map(NumericConstraint::multipleOf).orElse(null));
```

`withNumberMultipleOfResolver()` is expecting the `"multipleOf"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"minItems"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withArrayMinItemsResolver(scope -> scope.getType().isInstanceOf(MandatoryList.class) ? 1 : null);
configBuilder.forFields()
    .withArrayMinItemsResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(NotEmpty.class) == null ? null : 1);
configBuilder.forMethods()
    .withArrayMinItemsResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Size.class))
            .map(Size::min).orElse(null));
```

`withArrayMinItemsResolver()` is expecting the `"minItems"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"maxItems"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withArrayMaxItemsResolver(scope -> scope.getType().isInstanceOf(Triple.class) ? 3 : null);
configBuilder.forFields()
    .withArrayMaxItemsResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(NoMoreThanADozen.class) == null ? null : 12);
configBuilder.forMethods()
    .withArrayMaxItemsResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(Size.class))
            .map(Size::max).orElse(null));
```

`withArrayMaxItemsResolver()` is expecting the `"maxItems"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

## `"uniqueItems"` Keyword
```java
configBuilder.forTypesInGeneral()
    .withArrayUniqueItemsResolver(scope -> scope.getType().isInstanceOf(Set.class) ? true : null);
configBuilder.forFields()
    .withArrayUniqueItemsResolver(field -> field
            .getAnnotationConsideringFieldAndGetterIfSupported(Unique.class) == null ? null : true);
configBuilder.forMethods()
    .withArrayUniqueItemsResolver(method -> Optional
            .ofNullable(method.getAnnotationConsideringFieldAndGetterIfSupported(ListConstraints.class))
            .map(ListConstraints::distinct).orElse(null));
```

`withArrayUniqueItemsResolver()` is expecting the `"uniqueItems"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.
