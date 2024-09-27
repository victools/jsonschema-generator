# Java JSON Schema Generator – Maven Plugin
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-maven-plugin)

Maven plugin for the [jsonschema-generator](../jsonschema-generator) – Integrating JSON Schema generation in your builds

## Features
1. Generate JSON schema(s) for one or multiple Java classes.
2. Generate JSON schemas for all classes in one or multiple packages.
3. Configure the designated JSON Schema Draft version.
4. Configure the `OptionPreset` and individual `Option`s to be considered during schema generation.
5. Configure the standard modules and their respective options to be applied.
6. Configure any custom modules to be applied.

## Usage
### Plugin definition
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
        <classNames>com.myOrg.myApp.MyClass</classNames>
    </configuration>
</plugin>
```
This will use the default configuration of the generator.

### Selecting the classes for generation
#### Based on name (`<classNames>` and `<packageNames>`)

The classes for which a JSON schema should be generated are configured using the `<classNames>` and/or `<packageNames>` elements.
Via `<excludeClassNames>` you can filter-out some classes again.
These can be either a single element, or multiple by using nested elements.

```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <packageNames>
        <packageName>com.myOrg.myApp.package1</packageName>
        <packageName>com.myOrg.myApp.package2</packageName>
    </packageNames>
    <excludeClassNames>com.myOrg.myApp.package2.HiddenClass</excludeClassNames>
</configuration>
```

The content of each of these elements can be either:
- an absolute path with dots (`.`) as package separators or
- a glob pattern with slashes (`/`) as package separators and various types of placeholders (e.g. `?`, `*`, `**`).

```xml
<configuration>
    <classNames>com/myOrg/myApp/My*</classNames>
    <packageNames>com/myOrg/myApp/package?</packageNames>
    <excludeClassNames>com/myOrg/myApp/**Hidden*</excludeClassNames>
</configuration>
```

Additionally, you can omit the generation for abstract classes and/or interfaces by setting the respective `<skipAbstractTypes>` or `<skipInterfaces>`
flags to `true` (by default, they are `false`).
```xml
<configuration>
    <packageNames>com/myOrg/myApp/package/**</packageNames>
    <skipAbstractTypes>true</skipAbstractTypes>
    <skipInterfaces>true</skipInterfaces>
</configuration>
```

#### Based on Annotations (`<annotations>`)

Alternatively classes can be selected based on annotations using the `<annotations>` element. Then
all classes annotated with at least one of the specified annotations are considered for schema generation.

```xml
<configuration>
    <annotations>
      <annotation>com.myOrg.myApp.MyAnnotation</annotation>
      <annotation>com.myOrg.myApp.MyOtherAnnotation</annotation>
    </annotations>
</configuration>
```

If used together with the `<classNames>` and `<packageNames>` elements, both the class/package name
and at least one of the annotations have to match.

#### Restricting the classpath (`<classpath>`)

By default, the plugin considers all classes of the current project and all runtime dependencies of
the project. This can be changed by setting `<classpath>` to one of the following values:
- `PROJECT_ONLY` : only source files of the current project
- `WITH_COMPILE_DEPENDENCIES` : `PROJECT_ONLY` and compile dependencies
- `WITH_RUNTIME_DEPENDENCIES` : `PROJECT_ONLY` and runtime dependencies (default)
- `WITH_ALL_DEPENDENCIES` : all of the above

----

By default, the plugin aborts if no matching classes are found by the rules above.
If this is not desired, the `<failIfNoClassesMatch>` property can be set to `false`.

### Configuring generated file names and locations
The location where the files will be generated can be specified with the `<schemaFilePath>` element.
```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <schemaFilePath>src/main/resources/schemas</schemaFilePath>
</configuration>
```
The default path is `src/main/resources`

The name of the generated schema files can be configured with the `<schemaFileName>` element.
This is a substitution pattern that is used for all generated files. It following the `MessageFormat` syntax,
where the following variables can be used:
 - `{0}` : This is the name of the class
 - `{1}` : This is the package path of the class

For example, the following configuration will create a `MyClass.schema` file. 
```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <schemaFileName>{0}.schema</schemaFileName>
</configuration>
```

To store the generated schema files in the same directory structure as the originating classes, the following can be used:
```xml
<configuration>
    <packageNames>com.myOrg.myApp.utils</packageNames>
    <schemaFileName>{1}/{0}.schema</schemaFileName>
</configuration>
```
- The default `schemaFileName` is `{0}-schema.json`.

### Configuring schema version
The version of JSON Schema that is to be used can be configured with the `<schemaVersion>` element.
```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <schemaVersion>DRAFT_2019_09</schemaVersion>
</configuration>
```
Allowed values are (as per `com.github.victools.jsonschema.generator.SchemaVersion`):
- `DRAFT_2020_12`
- `DRAFT_2019_09`
- `DRAFT_7`
- `DRAFT_6`

`DRAFT_7` is the default, if not specified.

### Configuring the generator
The JSON schema generator can be configured with individual options as they are defined in `com.github.victools.jsonschema.generator.Option`.
The following example shows the possible elements:
```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <options>
        <preset>FULL_DOCUMENTATION</preset>
        <enabled>
            <option>DEFINITIONS_FOR_ALL_OBJECTS</option>
            <option>FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT</option>
        </enabled>
        <disabled>SCHEMA_VERSION_INDICATOR</disabled>
    </options>
</configuration>
```
The `preset` element can be either `NONE` or one of the constant values from `com.github.victools.jsonschema.generator.OptionPreset`.
In case no `preset` is specified, `PLAIN_JSON` is taken as default value.

Additional to the `preset` a number of individual `Option`s can be either enabled and/or disabled.
The syntax supports a single value as well as multiple values as nested elements.

### Configuring modules
When you want to have more control over the modules that are to be used during generation, the `<modules>` element can be used to define them.
```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <modules>
        <module>
            <name>Jackson</name>
        </module>
    </modules>
</configuration>  
```
This configuration will generate the schema using the Jackson module.

There are five standard modules that can be used:
- `Jackson`
- `JakartaValidation`
- `JavaxValidation`
- `Swagger15` 
- `Swagger2`

### Defining options for a module
```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <modules>
        <module>
            <name>Jackson</name>
            <options>
                <option>FLATTENED_ENUMS_FROM_JSONVALUE</option>
            </options>
        </module>
    </modules>
</configuration>  
```
in this way the options as supported by the module can be specified.

### Defining the use of custom modules
To enable a custom module in the generation the following construct can be used:
```xml
<configuration>
    <classNames>com.myOrg.myApp.MyClass</classNames>
    <modules>
        <module>
            <className>com.myOrg.myApp.CustomModule</className>
        </module>
    </modules>
</configuration>
```
Make sure your custom module is on the classpath and has a default constructor.
It is not possible to configure options for custom modules.

### Complete Example
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.victools</groupId>
            <artifactId>jsonschema-maven-plugin</artifactId>
            <version>[4.21.0,5.0.0)</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <classNames>
                    <className>com.myOrg.myApp.MyClass</className>
                    <className>com.myOrg.myApp.MyOtherClass</className>
                </classNames>
                <packageNames>com.myOrg.myApp.utilities</packageNames>
                <schemaVersion>DRAFT_2019_09</schemaVersion>
                <schemaFileName>{1}/{0}.schema</schemaFileName>
                <schemaFilePath>result/schemas</schemaFilePath>
                <options>
                    <enabled>
                        <option>DEFINITIONS_FOR_ALL_OBJECTS</option>
                        <option>NULLABLE_FIELDS_BY_DEFAULT</option>
                    </enabled>
                </options>
                <modules>
                    <module>
                        <name>Jackson</name>
                        <options>
                            <option>FLATTENED_ENUMS_FROM_JSONVALUE</option>
                        </options>
                    </module>
                    <module>
                        <className>com.github.imifou.jsonschema.module.addon.AddonModule</className>
                    </module>
                </modules>
            </configuration>
        </plugin>
    </plugins>
</build>
```
