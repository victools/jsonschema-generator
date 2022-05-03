# Java JSON Schema Generation – Bill of Materials (BOM)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator-bom/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-generator-bom)

BOM for the [jsonschema-generator](../jsonschema-generator) – ensuring a compatible combination of the main generator and its standard modules.

----

## Usage
### Dependency (Maven)
1. Include BOM in `<dependencyManagement>` section of your `pom.xml`, e.g.
```xml
<dependencyManagement>
    <dependency>
        <groupId>com.github.victools</groupId>
        <artifactId>jsonschema-generator-bom</artifactId>
        <version>[4.24.3,5.0.0)</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencyManagement>
```
2. Reference main generator and required modules in `<dependencies>` section without explicit `<version>`, e.g.
```xml
<dependencies>
    <dependency>
        <groupId>com.github.victools</groupId>
        <artifactId>jsonschema-generator</artifactId>
    </dependency>
    <dependency>
        <groupId>com.github.victools</groupId>
        <artifactId>jsonschema-module-jackson</artifactId>
    </dependency>
</dependencies>
```
