# Java JSON Schema Generator – Maven Plugin
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-javax-validation/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-maven-plugin)

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
    <version>4.8.0</version>
    <executions>
        <execution>
            <goals>
                <goal>schema-generator</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <className>com.myOrg.myApp.myclass</className>
    </configuration>
</plugin>
```
This will use the default configuration of the generator. This will enable all of the modules that are part of the generator. 


### Configuring file locations
```xml
<configuration>
    <className>com.myOrg.myApp.myclass</className>
    <schemaFileName>mySchema.json</schemaFileName>
    <schemaFilePath>src/main/resources/schemas</schemaFilePath>
</configuration>
```
- The default name of the file is the name of the class extended with ".schema.json"
- The default path is "src/main/resources"
### Configuring schema version
The version of JSON Schema that is to be used can be configed with the schemaVersion element.
```xml
<configuration>
    <className>com.github.victools.jsonschema.plugin.maven.TestClass</className>
    <schemaVersion>DRAFT_2019_09</schemaVersion>
</configuration>
```
Allowed values are:
- DRAFT_2019_09
- DRAFT_7

The last one is the default
### Configuring modules
When you want to have tighter control over the modules that are to be used during generation, the <module> element can be used to define them 
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
This configuration will only enable the Jackson module.
### Defining the options for a module
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
            <name>MySchemaModule</name>
            <className>com.myOrg.myApp.MyAddonModule</className>
        </module>
    </modules>
</configuration>  
```
Make sure your custom module is on the classpath.
Setting options on custom modules is not yet supported.
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
                    <option>OTHER_OPTION</option>
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