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
        <annotations>
            <annotation>com.myOrg.MySchemaAnnotation</annotation>
        </annotations>
        <skipAbstractTypes>true</skipAbstractTypes>
        <skipInterfaces>true</skipInterfaces>
        <classpath>PROJECT_ONLY</classpath>
        <failIfNoClassesMatch>false</failIfNoClassesMatch>
    </configuration>
</plugin>
```

The designated types can be mentioned separately (with dots as package separator) or in the form of glob patterns (with `/` as package separator) in `<classNames>` and/or included as part of their packages through `<packageNames>`. Through `<excludeClassNames>` you can further narrow down the type selection.   
When specifying expected `<annotations>`, at least one of them need to be present on a given type to be considered – in addition to matching the aforementioned criteria, if those are present.   
The considered `<classpath>` may be further specified as one of four values:
- `PROJECT_ONLY` : only source files of the current project
- `WITH_COMPILE_DEPENDENCIES` : `PROJECT_ONLY` and compile dependencies
- `WITH_RUNTIME_DEPENDENCIES` : `PROJECT_ONLY` and runtime dependencies (default, if unspecified)
- `WITH_ALL_DEPENDENCIES` : all of the above
- `WITH_ALL_DEPENDENCIES_AND_TESTS` : all of the above, with the addition of the current project's test files  
  Note that this requires a different `<phase>` (e.g., `test-compile`) being specified on the `<execution>`.
----

By default, the plugin aborts if the glob pattern does not match any class. If this is not desired, the `<failIfNoClassesMatch>` property can be set to `false`.

## Basic Configuration Options
There are some additional parameters available in the plugin `<configuration>`:

| #   | Tag                | Default              | Description                                                                                                                                                                                                                                                                                                                         |
|-----|--------------------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | `<schemaFilePath>` | `src/main/resources` | Directory to generate all schemas in                                                                                                                                                                                                                                                                                                |
| 2   | `<schemaFileName>` | `{0}-schema.json`    | Relative path from the `<schemaFilePath>` including the file name pattern. Two placeholders are supported: `{0}` will be replaced with the respective simple class name (e.g. `TypeA`) `{1}` will be replaced with the respective package path (e.g. `com/myOrg/myApp`) in case you want to preserve the original package structure |
| 3   | `<schemaVersion>`  | `DRAFT_7`            | JSON Schema version to apply (`DRAFT_6`, `DRAFT_7`, `DRAFT_2019_09` or `DRAFT_2020_12`)                                                                                                                                                                                                                                             |


### Configuring generated file names and locations
```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <schemaFilePath>src/main/resources/schemas</schemaFilePath>
</configuration>
```
The location where the files will be generated can be specified with the `<schemaFilePath>` element.
The default path is `src/main/resources`

```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <schemaFileName>{0}.schema</schemaFileName>
</configuration>
```
The name of the generated schema files can be configured with the `<schemaFileName>` element.
This is a substitution pattern that is used for all generated files. It is following the `MessageFormat` syntax,
where the following variables can be used:
 - `{0}` : This is the name of the class
 - `{1}` : This is the package path of the class

For example, the given configuration will create a `MyClass.schema` file.

```xml
<configuration>
    <packageNames>com.myOrg.myApp.utils</packageNames>
    <schemaFileName>{1}/{0}.schema</schemaFileName>
</configuration>
```
To store the generated schema files in the same directory structure as the originating classes, the following can be used `<schemaFileName>{1}/{0}-schema.json</schemaFileName>`.   
The default `<schemaFileName>` is `{0}-schema.json`.

Additionally, you can omit the generation for abstract classes and/or interfaces by setting the respective `<skipAbstractTypes>` or `<skipInterfaces>` flags to `true` (by default, they are `false`).
```xml
<configuration>
    <packageNames>com/myOrg/myApp/package/**</packageNames>
    <skipAbstractTypes>true</skipAbstractTypes>
    <skipInterfaces>true</skipInterfaces>
</configuration>
```

### Selecting Options

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
Make sure your custom module is on the classpath (considering the project itself as well as all compile and runtime dependencies) and has a default constructor.    
It is not possible to configure options for custom modules.

### Altering the format of generated schema files

```java
public class MavenPluginYamlModule implements Module {
    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        // Maven plugin should produce YAML files instead of JSON
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // set additional serialization options
        mapper.getSerializationConfig()
                .with(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS);
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        builder.withObjectMapper(mapper);
    }
}
```

One possibility within such a custom Module (as mentioned above) is to configure the format of the generated schema files.
The file contents are being produced by the schema generator's associated `ObjectMapper`.
That default `ObjectMapper` can be replaced, e.g., to opt-out of the default pretty-printing or changing the file format to YAML.
The given example requires the inclusion of the extra `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` dependency.

### Loading custom Module from test classes

```xml
<executions>
    <execution>
        <phase>test-compile</phase>
        <goals>
            <goal>generate</goal>
        </goals>
    </execution>
</executions>
```

When you're using a custom Module (as mentioned above) for additional configuration options, but don't want to include it among your application code,
you can either package it as separate artifact and include that as dependency of the plugin (not going into further detail here)
or the custom Module class can be included in your test packages.  
When you do the latter, the Maven plugin will by default not be able to load that class, since it won't be compiled yet in the Maven phase during which the schema generation is being executed.  
The Maven `compile` phase is when the schema generation gets triggered by default.
If you want the test classes (including the custom Module) to be available, a later phase (most likely: `test-compile`) needs to be specified.
