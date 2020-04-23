# Java JSON Schema Generator – Maven Plugin
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-maven-plugin)

Maven plugin for the [jsonschema-generator](../jsonschema-generator) – Integrating JSON Schema generation in your builds

## Features
1. Generate the JSON schema for a Java class.
2. Configure the generator by selecting the modules to be used.
3. Configure the options of the modules used in the generation.
4. Configure the use of custom modules.

## Usage
### Plugin definition
```xml
<plugin>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>generate-schema</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <className>com.myOrg.myApp.myclass</className>
    </configuration>
</plugin>
```
This will use the default configuration of the generator. 

### Configuring file locations
```xml
<configuration>
    <className>com.myOrg.myApp.Myclass</className>
    <schemaFileName>mySchema.json</schemaFileName>
    <schemaFilePath>src/main/resources/schemas</schemaFilePath>
</configuration>
```
- The default name of the file is the name of the class extended with `.schema.json`
- The default path is `src/main/resources`

### Configuring schema version
The version of JSON Schema that is to be used can be configured with the `<schemaVersion>` element.
```xml
<configuration>
    <className>com.myOrg.myApp.MyClass</className>
    <schemaVersion>DRAFT_2019_09</schemaVersion>
</configuration>
```
Allowed values are:
- `DRAFT_2019_09`
- `DRAFT_7`
- `DRAFT_6`

`DRAFT_7` is the default, if not specified.

### Configuring the generator
The JSON schema generator can be configured with the options as they are defined in `com.github.victools.jsonschema.generator.Option` 
The following example shows the possible elements:
```xml
<configuration>
    <className>com.myOrg.myApp.MyClass</className>
    <options>
        <preset>FULL_DOCUMENTATION</preset>
        <enabled>
            <option>DEFINITIONS_FOR_ALL_OBJECTS</option>
            <option>FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT</option>
        </enabled>
        <disabled>SIMPLIFIED_ENUMS</disabled>
    </options>
</configuration>
```
The `preset` element can select one of the values from `com.github.victools.jsonschema.generator.Option`.
In case no `preset` is specified, `PLAIN_JSON` is taken as default value.

Additional to the `preset` a number of options can be either enabled and/or disabled.
The syntax supports a single value as well as multiple values as nested elements.

### Configuring modules
When you want to have more control over the modules that are to be used during generation, the `<module>` element can be used to define them. 
```xml
<configuration>
    <className>com.myOrg.myApp.myclass</className>
    <modules>
        <module>
            <name>Jackson</name>
        </module>
    </modules>
</configuration>  
```
This configuration will generate the schema using the Jackson module.

There are three standard modules that can be used:
- `Jackson`
- `JavaxValidation`
- `Swagger15` 

### Defining options for a module
```xml
<configuration>
    <className>com.myOrg.myApp.myclass</className>
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
    <className>com.myOrg.myApp.myclass</className>
    <modules>
        <module>
            <className>com.myOrg.myApp.MyAddonModule</className>
        </module>
    </modules>
</configuration>  
```
Make sure your custom module is on the classpath and has a default constructor.
It is not possible to configure options for custom modules.

### Full example
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.victools</groupId>
            <artifactId>jsonschema-maven-plugin</artifactId>
            <version>4.8.0</version>
            <configuration>
                <className>com.github.victools.jsonschema.plugin.maven.TestClass</className>
                <schemaVersion>DRAFT_2019_09</schemaVersion>
                <schemaFileName>testclass.json</schemaFileName>
                <schemaFilePath>result/schemas</schemaFilePath>
                <options>
                    <option>DEFINITIONS_FOR_ALL_OBJECTS</option>
                    <option>NULLABLE_FIELDS_BY_DEFAULT</option>
                </options>
                <modules>
                    <module>
                        <name>Jackson</name>
                        <options>
                            <option>FLATTENED_ENUMS_FROM_JSONVALUE</option>
                        </options>
                    </module>
                    <module>
                        <name>MyModule</name>
                        <className>com.github.imifou.jsonschema.module.addon.AddonModule</className>
                    </module>
                </modules>
            </configuration>
        </plugin>
    </plugins>
</build>
```