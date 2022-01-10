# Maven Plugin
The [victools:jsonschema-maven-plugin](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-maven-plugin) allows you to incorporate the generation of JSON Schemas from your code into your build process.
There are a number of basic configuration options as well as the possibility to define any kind of configurations through a `Module` of your own.

## Target Types to generate Schemas for
```xml
<plugin>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <classNames>com/myOrg/myApp/My*</classNames>
        <packageNames>com/myOrg/myApp/package?</packageNames>
        <excludeClassNames>com/myOrg/myApp/**Hidden*</excludeClassNames>
    </configuration>
</plugin>
```

The designated types can be mentioned separately (with dots as package separator) or in the form of glob patterns (with `/` as package separator) in `<classNames>` and/or included as part of their packages through `<packageNames>`. Through `<excludeClassNames>` you can further narrow down the type selection.

## Basic Configuration Options
There are some additional parameters available in the plugin `<configuration>`:

| # | Tag | Default | Description |
| --- | --- | --- | --- |
|  1 | `<schemaFilePath>` | `src/main/resources` | Directory to generate all schemas in |
|  2 | `<schemaFileName>` | `{0}-schema.json` | Relative path from the `<schemaFilePath>` including the file name pattern. Two placeholders are supported: `{0}` will be replaced with the respective simple class name (e.g. `TypeA`) `{1}` will be replaced with the respective package path (e.g. `com/myOrg/myApp`) in case you want to preserve the original package structure |
|  3 | `<schemaVersion>` | `DRAFT_7` | JSON Schema version to apply (`DRAFT_6`, `DRAFT_7`, `DRAFT_2019_09` or `DRAFT_2020_12`) |

```xml
<options>
    <preset>FULL_DOCUMENTATION</preset>
    <enabled>
        <option>DEFINITIONS_FOR_ALL_OBJECTS</option>
        <option>FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT</option>
    </enabled>
    <disabled>SCHEMA_VERSION_INDICATOR</disabled>
</options>
```

The standard generator [`Option`s](#generator-options) can be included via the `<options>` tag.

## Further configurations through Modules
```xml
<modules>
    <module>
        <name>Jackson</name>
        <options>
            <option>FLATTENED_ENUMS_FROM_JSONVALUE</option>
        </options>
    </module>
</modules>
```

Through the `<modules>` tag you can include the standard modules – potentially with their `<options>` if there are any.

```xml
<modules>
    <module>
        <className>com.myOrg.myApp.CustomModule</className>
    </module>
</modules>
```

You can also group any kind of configurations into a Module of your own and include it via its full class name.
