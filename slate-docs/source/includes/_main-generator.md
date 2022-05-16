The [victools:jsonschema-generator](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-generator) aims at allowing the generation of JSON Schema (Draft 6, Draft 7, Draft 2019-09 or Draft 2020-12) to document Java code.
This is expressly not limited to _JSON_ but also allows for a Java API to be documented (i.e. including methods and the associated return values).

# Generator – Options

The schema generation caters for a certain degree of flexibility out-of-the-box.   
Various aspects can be toggled on/off by including or excluding respective `Option`s.

```java
configBuilder.with(
    Option.EXTRA_OPEN_API_FORMAT_VALUES,
    Option.PLAIN_DEFINITION_KEYS);
configBuilder.without(
    Option.Schema_VERSION_INDICATOR,
    Option.ENUM_KEYWORD_FOR_SINGLE_VALUES);
```

<aside class="success">
    Via <code>.with(Option...)</code> and <code>.without(Option...)</code> you can enable or disable <code>Option</code>s respectively.
    <br/>
    For your convenience, there are three <code>OptionPreset</code>s containing a number of these <code>Option</code>s – but you can create <code>OptionPreset</code>s of your own to provide it in the <code>SchemaGeneratorConfigBuilder</code>'s constructor.
</aside>

<table>
  <thead>
    <tr><th>#</th><th style="width: 50%">Behavior if included</th><th style="width: 50%">Behavior if excluded</th></tr>
  </thead>
  <tbody>
    <tr>
      <td rowspan="2" style="text-align: right">1</td>
      <td colspan="2"><code>Option.SCHEMA_VERSION_INDICATOR</code></td>
    </tr>
    <tr>
      <td>Setting appropriate <code>$schema</code> attribute on main schema being generated.</td>
      <td>No <code>$schema</code> attribute is being added.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">2</td>
      <td colspan="2"><code>Option.ADDITIONAL_FIXED_TYPES</code></td>
    </tr>
    <tr>
      <td>
        <ul>
          <li><code>String</code>/<code>Character</code>/<code>char</code>/<code>CharSequence</code> are treated as <code>{ "type": "string" }</code> schema</li>
          <li><code>Boolean</code>/<code>boolean</code> are treated as <code>{ "type": "boolean" }</code> schema</li>
          <li><code>Integer</code>/<code>int</code>/<code>Long</code>/<code>long</code>/<code>Short</code>/<code>short</code>/<code>Byte</code>/<code>byte</code> are treated as <code>{ "type": "integer" }</code> schema</li>
          <li><code>Double</code>/<code>double</code>/<code>Float</code>/<code>float</code> are treated as <code>{ "type": "number" }</code> schema</li>
          <li><code>BigInteger</code> as <code>{ "type": "integer" }</code> schema</li>
          <li><code>BigDecimal</code>/<code>Number</code> as <code>{ "type": "number" }</code> schema</li>
          <li><code>LocalDate</code>/<code>LocalDateTime</code>/<code>LocalTime</code>/<code>ZonedDateTime</code>/<code>OffsetDateTime</code>/<code>OffsetTime</code>/<code>Instant</code>/<code>ZoneId</code>/<code>Date</code>/<code>Calendar</code>/<code>UUID</code> as <code>{ "type": "string" }</code> schema</li>
        </ul>
      </td>
      <td>
        <ul>
          <li><code>String</code>/<code>Character</code>/<code>char</code>/<code>CharSequence</code> are treated as <code>{ "type": "string" }</code> schema</li>
          <li><code>Boolean</code>/<code>boolean</code> are treated as <code>{ "type": "boolean" }</code> schema</li>
          <li><code>Integer</code>/<code>int</code>/<code>Long</code>/<code>long</code>/<code>Short</code>/<code>short</code>/<code>Byte</code>/<code>byte</code> are treated as <code>{ "type": "integer" }</code> schema</li>
          <li><code>Double</code>/<code>double</code>/<code>Float</code>/<code>float</code> are treated as <code>{ "type": "number" }</code> schema</li>
        </ul>
      </td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">3</td>
      <td colspan="2"><code>Option.EXTRA_OPEN_API_FORMAT_VALUES</code></td>
    </tr>
    <tr>
      <td>Include extra <code>"format"</code> values (e.g. <code>"int32"</code>, <code>"int64"</code>, <code>"date"</code>, <code>"date-time"</code>, <code>"uuid"</code>) for fixed types (primitive/basic types, plus some of the <code>Option.ADDITIONAL_FIXED_TYPES</code> if they are enabled as well).</td>
      <td>no automatic <code>"format"</code> values are being included.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">4</td>
      <td colspan="2"><code>Option.SIMPLIFIED_ENUMS</code></td>
    </tr>
    <tr>
      <td>Treating encountered enum types as objects, but including only the <code>name()</code> method and listing the names of the enum constants as its <code>enum</code> values.</td>
      <td>-</td>
    </tr>
    <tr><th>#</th><th>Behavior if included</th><th>Behavior if excluded</th></tr>
    <tr>
      <td rowspan="2" style="text-align: right">5</td>
      <td colspan="2"><code>Option.FLATTENED_ENUMS</code></td>
    </tr>
    <tr>
      <td>Treating enountered enum types as <code>{ "type": "string" }</code> schema with the names of the enum constants being listed as its <code>enum</code> values.</td>
      <td>-</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">6</td>
      <td colspan="2"><code>Option.FLATTENED_ENUMS_FROM_TOSTRING</code></td>
    </tr>
    <tr>
      <td>Treating enountered enum types as <code>{ "type": "string" }</code> schema with the <code>toString()</code> values of the enum constants being listed as its <code>enum</code> values.</td>
      <td>-</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">7</td>
      <td colspan="2"><code>Option.SIMPLIFIED_OPTIONALS</code></td>
    </tr>
    <tr>
      <td>Treating encountered <code>Optional</code> instances as objects, but including only the <code>get()</code>, <code>orElse()</code> and <code>isPresent()</code> methods.</td>
      <td>-</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">8</td>
      <td colspan="2"><code>Option.FLATTENED_OPTIONALS</code></td>
    </tr>
    <tr>
      <td>Replacing encountered <code>Optional</code> instances as null-able forms of their generic parameter type.</td>
      <td>-</td>
    </tr>
    <tr><th>#</th><th>Behavior if included</th><th>Behavior if excluded</th></tr>
    <tr>
      <td rowspan="2" style="text-align: right">9</td>
      <td colspan="2"><code>Option.FLATTENED_SUPPLIERS</code></td>
    </tr>
    <tr>
      <td>Replacing encountered <code>Supplier</code> instances with their generic parameter type.</td>
      <td>-</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">10</td>
      <td colspan="2"><code>Option.VALUES_FROM_CONSTANT_FIELDS</code></td>
    </tr>
    <tr>
      <td>Attempt to load the values of <code>static</code> <code>final</code> fields, serialize them via the <code>ObjectMapper</code> and include them as the respective schema's <code>const</code> value.</td>
      <td>No <code>const</code> values are populated for <code>static</code> <code>final</code> fields.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">11</td>
      <td colspan="2"><code>Option.PUBLIC_STATIC_FIELDS</code></td>
    </tr>
    <tr>
      <td>Include <code>public</code> <code>static</code> fields in an object's <code>properties</code>.</td>
      <td>No <code>public</code> <code>static</code> fields are included in an object's <code>properties</code>.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">12</td>
      <td colspan="2"><code>Option.PUBLIC_NONSTATIC_FIELDS</code></td>
    </tr>
    <tr>
      <td>Include <code>public</code> non-<code>static</code> fields in an object's <code>properties</code>.</td>
      <td>No <code>public</code> non-<code>static</code> fields are included in an object's <code>properties</code>.</td>
    </tr>
    <tr><th>#</th><th>Behavior if included</th><th>Behavior if excluded</th></tr>
    <tr>
      <td rowspan="2" style="text-align: right">13</td>
      <td colspan="2"><code>Option.NONPUBLIC_STATIC_FIELDS</code></td>
    </tr>
    <tr>
      <td>Include <code>protected</code>/package-visible/<code>private</code> <code>static</code> fields in an object's <code>properties</code>.</td>
      <td>No <code>protected</code>/package-visible/<code>private</code> <code>static</code> fields are included in an object's <code>properties</code>.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">14</td>
      <td colspan="2"><code>Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS</code></td>
    </tr>
    <tr>
      <td>Include <code>protected</code>/package-visible/<code>private</code> non-<code>static</code> fields in an object's <code>properties</code> if they have corresponding getter methods.</td>
      <td>No <code>protected</code>/package-visible/<code>private</code> non-<code>static</code> fields with getter methods are included in an object's <code>properties</code>.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">15</td>
      <td colspan="2"><code>Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS</code></td>
    </tr>
    <tr>
      <td>Include <code>protected</code>/package-visible/<code>private</code> non-<code>static</code> fields in an object's <code>properties</code> if they don't have corresponding getter methods.</td>
      <td>No <code>protected</code>/package-visible/<code>private</code> non-<code>static</code> fields without getter methods are included in an object's <code>properties</code>.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">16</td>
      <td colspan="2"><code>Option.TRANSIENT_FIELDS</code></td>
    </tr>
    <tr>
      <td>Include <code>transient</code> fields in an object's <code>properties</code> if they would otherwise be included according to the <code>Option</code>s above.</td>
      <td>No <code>transient</code> fields are included in an object's <code>properties</code> even if they would otherwise be included according to the <code>Option</code>s above.</td>
    </tr>
    <tr><th>#</th><th>Behavior if included</th><th>Behavior if excluded</th></tr>
    <tr>
      <td rowspan="2" style="text-align: right">17</td>
      <td colspan="2"><code>Option.STATIC_METHODS</code></td>
    </tr>
    <tr>
      <td>Include <code>public</code> <code>static</code> methods in an object's <code>properties</code></td>
      <td>No <code>static</code> methods are included in an object's <code>properties</code> even if they would be included according to the <code>Option.VOID_METHODS</code> below.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">18</td>
      <td colspan="2"><code>Option.VOID_METHODS</code></td>
    </tr>
    <tr>
      <td>Include <code>public</code> <code>void</code> methods in an object's <code>properties</code></td>
      <td>No <code>void</code> methods are included in an object's <code>properties</code> even if they would be included according to the <code>Option.STATIC_METHODS</code> above.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">19</td>
      <td colspan="2"><code>Option.GETTER_METHODS</code></td>
    </tr>
    <tr>
      <td>Include <code>public</code> methods in an object's <code>properties</code> if a corresponding field exists that fulfills the usual naming conventions (<code>getX()</code>/<code>x</code> or <code>isValid()</code>/<code>valid</code>).</td>
      <td>No methods are included in an object's <code>properties</code>> for which a field exists that fulfills the usual naming conventions.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">20</td>
      <td colspan="2"><code>Option.NONSTATIC_NONVOID_NONGETTER_METHODS</code></td>
    </tr>
    <tr>
      <td>Include <code>public</code> non-<code>static</code> non-<code>void</code> methods in an object's <code>properties</code> for which no field exists that fulfills the usual getter naming conventions.</td>
      <td>No non-<code>static</code>/non-<code>void</code>/non-getter methods are included in an object's <code>properties</code>.</td>
    </tr>
    <tr><th>#</th><th>Behavior if included</th><th>Behavior if excluded</th></tr>
    <tr>
      <td rowspan="2" style="text-align: right">21</td>
      <td colspan="2"><code>Option.NULLABLE_FIELDS_BY_DEFAULT</code></td>
    </tr>
    <tr>
      <td>The schema <code>type</code> for a field allows <code>null</code> by default unless some configuration specifically says it is not null-able.</td>
      <td>The schema <code>type</code> for a field does not allow for <code>null</code> by default unless some configuration specifically says it is null-able.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">22</td>
      <td colspan="2"><code>Option.NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT</code></td>
    </tr>
    <tr>
      <td>The schema <code>type</code> for a method's return type allows <code>null</code> by default unless some configuration specifically says it is not null-able.</td>
      <td>The schema <code>type</code> for a method's return type does not allow for <code>null</code> by default unless some configuration specifically says it is null-able.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">23</td>
      <td colspan="2"><code>Option.NULLABLE_ARRAY ITEMS_ALLOWED</code></td>
    </tr>
    <tr>
      <td>The schema <code>type</code> for the items in an array (in case of a field's value or method's return value being a container/array) allows <code>null</code>, if the corresponding configuration explicitly says so. Otherwise, they're still deemed not null-able by default.</td>
      <td>The schema <code>type</code> for the items in an array (in case of a field's value or method's return value being a container/array) never allows <code>null</code>.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">24</td>
      <td colspan="2"><code>Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS</code></td>
    </tr>
    <tr>
      <td>Include argument-free methods as fields, e.g. the return type of <code>getName()</code> will be included as <code>name</code> field.</td>
      <td>Argument-free methods will be included with the appended parentheses.</td>
    </tr>
    <tr><th>#</th><th>Behavior if included</th><th>Behavior if excluded</th></tr>
    <tr>
      <td rowspan="2" style="text-align: right">25</td>
      <td colspan="2"><code>Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES</code></td>
    </tr>
    <tr>
      <td>Setting the <code>additionalProperties</code> attribute in each <code>Map<K, V></code> to a schema representing the declared value type <code>V</code>.</td>
      <td>Omitting the <code>additionalProperties</code> attribute in <code>Map<K, V></code> schemas by default (thereby allowing additional properties of any type) unless some configuration specifically says something else.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">26</td>
      <td colspan="2"><code>Option.ENUM_KEYWORD_FOR_SINGLE_VALUES</code></td>
    </tr>
    <tr>
      <td>Using the <code>enum</code> keyword for allowed values, even if there is only one.</td>
      <td>In case of a single allowed value, use the <code>const</code> keyword instead of <code>enum</code>.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">27</td>
      <td colspan="2"><code>Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT</code></td>
    </tr>
    <tr>
      <td>Setting the <code>additionalProperties</code> attribute in all object schemas to <code>false</code> by default unless some configuration specifically says something else.</td>
      <td>Omitting the <code>additionalProperties</code> attribute in all object schemas by default (thereby allowing any additional properties) unless some configuration specifically says something else.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">28</td>
      <td colspan="2"><code>Option.DEFINITIONS_FOR_ALL_OBJECTS</code></td>
    </tr>
    <tr>
      <td>Include an entry in the <code>$defs</code>/<code>definitions</code> for each encountered object type that is not explicitly declared as "inline" via a custom definition.</td>
      <td>Only include those entries in the <code>$defs</code>/<code>definitions</code> for object types that are referenced more than once and which are not explicitly declared as "inline" via a custom definition.</td>
    </tr>
    <tr><th>#</th><th>Behavior if included</th><th>Behavior if excluded</th></tr>
    <tr>
      <td rowspan="2" style="text-align: right">29</td>
      <td colspan="2"><code>Option.DEFINITION_FOR_MAIN_SCHEMA</code></td>
    </tr>
    <tr>
      <td>Include an entry in the <code>$defs</code>/<code>definitions</code> for the main/target type and a corresponding <code>$ref</code> on the top level (which is only valid from Draft 2019-09 onward).</td>
      <td>Define the main/target type "inline".</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">30</td>
      <td colspan="2"><code>Option.INLINE_ALL_SCHEMAS</code></td>
    </tr>
    <tr>
      <td>Do not include any <code>$defs</code>/<code>definitions</code> but rather define all sub-schemas "inline" – however, this results in an exception being thrown if the given type contains any kind of circular reference.</td>
      <td>Depending on whether <code>DEFINITIONS_FOR_ALL_OBJECTS</code> is included or excluded.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">31</td>
      <td colspan="2"><code>Option.PLAIN_DEFINITION_KEYS</code></td>
    </tr>
    <tr>
      <td>Ensure that the keys for any <code>$defs</code>/<code>definitions</code> match the regular expression <code>^[a-zA-Z0-9\.\-_]+$</code> (as expected by the OpenAPI specification 3.0).</td>
      <td>Ensure that the keys for any <code>$defs</code>/<code>definitions</code> are URI compatible (as expected by the JSON Schema specification).</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">32</td>
      <td colspan="2"><code>Option.ALLOF_CLEANUP_AT_THE_END</code></td>
    </tr>
    <tr>
      <td>At the very end of the schema generation reduce <code>allOf</code> wrappers where it is possible without overwriting any attributes – this also affects the results from custom definitions.</td>
      <td>Do not attempt to reduce <code>allOf</code> wrappers but preserve them as they were generated regardless of them being necessary or not.</td>
    </tr>
  </tbody>
</table>

Below, you can find the lists of <code>Option</code>s included/excluded in the respective standard <code>OptionPreset</code>s:

* "F_D" = <code>FULL_DOCUMENTATION</code>
* "J_O" = <code>JAVA_OBJECT</code>
* "P_J" = <code>PLAIN_JSON</code>

| # | Standard `Option` | F_D | J_O | P_J |
| --- | --- | --- | --- | --- |
|  1 | `SCHEMA_VERSION_INDICATOR` | ⬜️ | ⬜️ | ✅ |
|  2 | `ADDITIONAL_FIXED_TYPES` | ⬜️ | ⬜️ | ✅ |
|  3 | `EXTRA_OPEN_API_FORMAT_VALUES` | ⬜️ | ⬜️ | ⬜️ |
|  4 | `SIMPLIFIED_ENUMS` | ✅ | ✅ | ⬜️ |
|  5 | `FLATTENED_ENUMS` | ⬜️ | ⬜️ | ✅ |
|  6 | `FLATTENED_ENUMS_FROM_TOSTRING` | ⬜️ | ⬜️ | ⬜️ |
|  7 | `SIMPLIFIED_OPTIONALS` | ✅ | ✅ | ⬜️ 
|  8 | `FLATTENED_OPTIONALS` | ⬜️ | ⬜️ | ✅ |
|  8 | `FLATTENED_SUPPLIERS` | ⬜️ | ⬜️ | ✅ |
| 10 | `VALUES_FROM_CONSTANT_FIELDS` | ✅ | ✅ | ✅ |
| 11 | `PUBLIC_STATIC_FIELDS` | ✅ | ✅ | ⬜️ |
| 12 | `PUBLIC_NONSTATIC_FIELDS` | ✅ | ✅ | ✅ |
| 13 | `NONPUBLIC_STATIC_FIELDS` | ✅ | ⬜️ | ⬜️ |
| 14 | `NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS` | ✅ | ⬜️ | ✅ |
| 15 | `NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS` | ✅ | ⬜️ | ✅ |
| 16 | `TRANSIENT_FIELDS` | ✅ | ⬜️ | ⬜️ |
| 17 | `STATIC_METHODS` | ✅ | ✅ | ⬜️ |
| 18 | `VOID_METHODS` | ✅ | ✅ | ⬜️ |
| 19 | `GETTER_METHODS` | ✅ | ✅ | ⬜️ |
| 20 | `NONSTATIC_NONVOID_NONGETTER_METHODS` | ✅ | ✅ | ⬜️ |
| 21 | `NULLABLE_FIELDS_BY_DEFAULT` | ✅ | ⬜️ | ⬜️ |
| 22 | `NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT` | ✅ | ⬜️ | ⬜️ |
| 23 | `NULLABLE_ARRAY_ITEMS_ALLOWED` | ⬜️ | ⬜️ | ⬜️ |
| 24 | `FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS` | ⬜️ | ⬜️ | ⬜️ |
| 25 | `MAP_VALUES_AS_ADDITIONAL_PROPERTIES` | ⬜️ | ⬜️ | ⬜️ |
| 26 | `ENUM_KEYWORD_FOR_SINGLE_VALUES` | ⬜️ | ⬜️ | ⬜️ |
| 27 | `FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT` | ⬜️ | ⬜️ | ⬜️ |
| 28 | `DEFINITIONS_FOR_ALL_OBJECTS` | ⬜️ | ⬜️ | ⬜️ |
| 29 | `DEFINITION_FOR_MAIN_SCHEMA` | ⬜️ | ⬜️ | ⬜️ |
| 30 | `INLINE_ALL_SCHEMAS` | ⬜️ | ⬜️ | ⬜️ |
| 31 | `PLAIN_DEFINITION_KEYS` | ⬜️ | ⬜️ | ⬜️ |
| 32 | `ALLOF_CLEANUP_AT_THE_END` | ✅ | ✅ | ✅ |

# Generator – Modules
Similar to an `OptionPreset` being a short-cut to including various `Option`s, the concept of `Module`s is a convenient way of including multiple [individual configurations](#generator-individual-configurations) or even [advanced configurations](#generator-advanced-configurations) (as per the following sections) at once.

You can easily group your own set of configurations into a `Module` if you wish.
However, the main intention behind `Module`s is that they are an entry-point for separate external dependencies you can "plug-in" as required via `SchemaGeneratorConfigBuilder.with(Module)`, like the few standard `Module`s documented below.

<aside class="notice">
    There may be other available modules outside of this repository.
    Refer to the main <a href="https://github.com/victools/jsonschema-generator/blob/master/README.md">README</a> for the list of known modules.
</aside>

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

`withAdditionalPropertiesResolver()` is expecting the `"additionalProperties"` attribute's value to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied.

* If `Object.class` is being returned, the `"additionalProperties"` attribute will be omitted.
* if `Void.class` is being returned, the `"additionalProperties"` will be set to `false`.
* If any other type is being returned (e.g. other `Class` or a `ResolvedType`) a corresponding schema will be included in `"additionalProperties"`.

## `"patternProperties"` Keyword
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

`withPatternPropertiesResolver()` is expecting a `Map` of regular expressions to their corresponding allowed types to be returned based on a given `TypeScope`/`FieldScope`/`MethodScope` – the first non-`null` value will be applied; the regular expression will be included as keys in the `"patternProperties"` attribute with a schema representing the mapped type as the corresponding values.

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
                    context.getTypeContext().resolve(String.class)
                        .put("format", "uuid")))
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
    On the <code>CustomDefinition</code>'s constructor, you are able to decide whether it should be "inlined" and
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
        .map(CustomDefinition::new)
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
    However, you can still decide whether or not the attributes collected through the various other <a href="#generator-individual-configurations">Individual Configurations</a> shall be added,
    through the <code>AttributeInclusion</code> parameter in the <code>CustomPropertyDefinition</code>'s constructor.
</aside>