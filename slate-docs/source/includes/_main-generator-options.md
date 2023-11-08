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
          <li><code>LocalDate</code>/<code>LocalDateTime</code>/<code>LocalTime</code>/<code>ZonedDateTime</code>/<code>OffsetDateTime</code>/<code>OffsetTime</code>/<code>Instant</code>/<code>Period</code>/<code>ZoneId</code>/<code>Date</code>/<code>Calendar</code>/<code>UUID</code> as <code>{ "type": "string" }</code> schema</li>
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
      <td>Treating encountered enum types as <code>{ "type": "string" }</code> schema with the names of the enum constants being listed as its <code>enum</code> values.</td>
      <td>-</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">6</td>
      <td colspan="2"><code>Option.FLATTENED_ENUMS_FROM_TOSTRING</code></td>
    </tr>
    <tr>
      <td>Treating encountered enum types as <code>{ "type": "string" }</code> schema with the <code>toString()</code> values of the enum constants being listed as its <code>enum</code> values.</td>
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
      <td>
        Attempt to load the values of <code>static</code> <code>final</code> fields, serialize them via the <code>ObjectMapper</code> and include them as the respective schema's <code>const</code> value.
        <div>For this option to take effect, those <code>static</code> <code>final</code> fields need to be included via <code>Option.PUBLIC_STATIC_FIELDS</code> and/or <code>Option.NONPUBLIC_STATIC_FIELDS</code>.</div>
      </td>
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
      <td colspan="2"><code>Option.NULLABLE_ARRAY_ITEMS_ALLOWED</code></td>
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
      <td>Setting the <code>additionalProperties</code> attribute in each <code>Map&lt;K, V&gt;</code> to a schema representing the declared value type <code>V</code>.</td>
      <td>Omitting the <code>additionalProperties</code> attribute in <code>Map&lt;K, V&gt;</code> schemas by default (thereby allowing additional properties of any type) unless some configuration specifically says something else.</td>
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
      <td colspan="2"><code>Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES</code></td>
    </tr>
    <tr>
      <td>For a member (field/method), having a declared type for which subtypes are being detected, include a single definition with any collected member attributes assigned directly. Any subtypes are only being handled as generic types, i.e., outside of the member context. That means, certain relevant annotations may be ignored (e.g. a jackson <code>@JsonTypeInfo</code> override on a single member would not be correctly reflected in the produced schema).</td>
      <td>For a member (field/method), having a declared type for which subtypes are being detected, include a list of definittions: one for each subtype in the given member's context. This allows independently interpreting contextual information (e.g., member annotations) for each subtype.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">31</td>
      <td colspan="2"><code>Option.INLINE_ALL_SCHEMAS</code></td>
    </tr>
    <tr>
      <td>Do not include any <code>$defs</code>/<code>definitions</code> but rather define all sub-schemas "inline" – however, this results in an exception being thrown if the given type contains any kind of circular reference.</td>
      <td>Depending on whether <code>DEFINITIONS_FOR_ALL_OBJECTS</code> is included or excluded.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">32</td>
      <td colspan="2"><code>Option.PLAIN_DEFINITION_KEYS</code></td>
    </tr>
    <tr>
      <td>Ensure that the keys for any <code>$defs</code>/<code>definitions</code> match the regular expression <code>^[a-zA-Z0-9\.\-_]+$</code> (as expected by the OpenAPI specification 3.0).</td>
      <td>Ensure that the keys for any <code>$defs</code>/<code>definitions</code> are URI compatible (as expected by the JSON Schema specification).</td>
    </tr>
    <tr><th>#</th><th>Behavior if included</th><th>Behavior if excluded</th></tr>
    <tr>
      <td rowspan="2" style="text-align: right">33</td>
      <td colspan="2"><code>Option.ALLOF_CLEANUP_AT_THE_END</code></td>
    </tr>
    <tr>
      <td>At the very end of the schema generation reduce <code>allOf</code> wrappers where it is possible without overwriting any attributes – this also affects the results from custom definitions.</td>
      <td>Do not attempt to reduce <code>allOf</code> wrappers but preserve them as they were generated regardless of them being necessary or not.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">34</td>
      <td colspan="2"><code>Option.STRICT_TYPE_INFO</code></td>
    </tr>
    <tr>
      <td>As final step in the schema generation process, ensure all sub schemas containing keywords implying a particular "type" (e.g., "properties" implying an "object") have this "type" declared explicitly – this also affects the results from custom definitions.</td>
      <td>No additional "type" indication will be added for each sub schema, e.g. on the collected attributes where the "allOf" clean-up could not be applied or was disabled.</td>
    </tr>
    <tr>
      <td rowspan="2" style="text-align: right">35</td>
      <td colspan="2"><code>Option.STANDARD_FORMATS</code></td>
    </tr>
    <tr>
      <td>Same as <code>Option.EXTRA_OPEN_API_FORMAT_VALUES</code> but only for built-in supported <code>"format"</code> values (<code>"date"</code>, <code>"time"</code>, <code>"date-time"</code>, <code>"duration"</code>, <code>"uuid"</code>, <code>"uri"</code>).
      Only works if <code>Option.ADDITIONAL_FIXED_TYPES</code> is set and it is overriden by <code>Option.EXTRA_OPEN_API_FORMAT_VALUES</code></td>
      <td>no automatic <code>"format"</code> values are being included.</td>
    </tr>
  </tbody>
</table>

Below, you can find the lists of <code>Option</code>s included/excluded in the respective standard <code>OptionPreset</code>s:

* "F_D" = <code>FULL_DOCUMENTATION</code>
* "J_O" = <code>JAVA_OBJECT</code>
* "P_J" = <code>PLAIN_JSON</code>

|  # | Standard `Option`                            | F_D | J_O | P_J |
| -- | -------------------------------------------- | --- | --- | --- |
|  1 | `SCHEMA_VERSION_INDICATOR`                   | ⬜️ | ⬜️ | ✅ |
|  2 | `ADDITIONAL_FIXED_TYPES`                     | ⬜️ | ⬜️ | ✅ |
|  3 | `EXTRA_OPEN_API_FORMAT_VALUES`               | ⬜️ | ⬜️ | ⬜️ |
|  4 | `SIMPLIFIED_ENUMS`                           | ✅ | ✅ | ⬜️ |
|  5 | `FLATTENED_ENUMS`                            | ⬜️ | ⬜️ | ✅ |
|  6 | `FLATTENED_ENUMS_FROM_TOSTRING`              | ⬜️ | ⬜️ | ⬜️ |
|  7 | `SIMPLIFIED_OPTIONALS`                       | ✅ | ✅ | ⬜️ |
|  8 | `FLATTENED_OPTIONALS`                        | ⬜️ | ⬜️ | ✅ |
|  8 | `FLATTENED_SUPPLIERS`                        | ⬜️ | ⬜️ | ✅ |
| 10 | `VALUES_FROM_CONSTANT_FIELDS`                | ✅ | ✅ | ✅ |
| 11 | `PUBLIC_STATIC_FIELDS`                       | ✅ | ✅ | ⬜️ |
| 12 | `PUBLIC_NONSTATIC_FIELDS`                    | ✅ | ✅ | ✅ |
| 13 | `NONPUBLIC_STATIC_FIELDS`                    | ✅ | ⬜️ | ⬜️ |
| 14 | `NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS`    | ✅ | ⬜️ | ✅ |
| 15 | `NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS` | ✅ | ⬜️ | ✅ |
| 16 | `TRANSIENT_FIELDS`                           | ✅ | ⬜️ | ⬜️ |
| 17 | `STATIC_METHODS`                             | ✅ | ✅ | ⬜️ |
| 18 | `VOID_METHODS`                               | ✅ | ✅ | ⬜️ |
| 19 | `GETTER_METHODS`                             | ✅ | ✅ | ⬜️ |
| 20 | `NONSTATIC_NONVOID_NONGETTER_METHODS`        | ✅ | ✅ | ⬜️ |
| 21 | `NULLABLE_FIELDS_BY_DEFAULT`                 | ✅ | ⬜️ | ⬜️ |
| 22 | `NULLABLE_METHOD_RETURN_VALUES_BY_DEFAULT`   | ✅ | ⬜️ | ⬜️ |
| 23 | `NULLABLE_ARRAY_ITEMS_ALLOWED`               | ⬜️ | ⬜️ | ⬜️ |
| 24 | `FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS`   | ⬜️ | ⬜️ | ⬜️ |
| 25 | `MAP_VALUES_AS_ADDITIONAL_PROPERTIES`        | ⬜️ | ⬜️ | ⬜️ |
| 26 | `ENUM_KEYWORD_FOR_SINGLE_VALUES`             | ⬜️ | ⬜️ | ⬜️ |
| 27 | `FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT` | ⬜️ | ⬜️ | ⬜️ |
| 28 | `DEFINITIONS_FOR_ALL_OBJECTS`                | ⬜️ | ⬜️ | ⬜️ |
| 29 | `DEFINITION_FOR_MAIN_SCHEMA`                 | ⬜️ | ⬜️ | ⬜️ |
| 30 | `DEFINITIONS_FOR_MEMBER_SUPERTYPES`          | ⬜️ | ⬜️ | ⬜️ |
| 31 | `INLINE_ALL_SCHEMAS`                         | ⬜️ | ⬜️ | ⬜️ |
| 32 | `PLAIN_DEFINITION_KEYS`                      | ⬜️ | ⬜️ | ⬜️ |
| 33 | `ALLOF_CLEANUP_AT_THE_END`                   | ✅ | ✅ | ✅ |
| 34 | `STRICT_TYPE_INFO`                           | ⬜️ | ⬜️ | ⬜️ |
| 35 | `STANDARD_FORMATS`                           | ⬜️ | ⬜️ | ⬜️ |
