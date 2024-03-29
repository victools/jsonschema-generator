---
title: API Reference

toc_footers:
  - <a href='https://github.com/victools/jsonschema-generator'>Have a look at the GitHub repo</a>
  - <a href='https://github.com/slatedocs/slate'>Documentation Powered by Slate</a>

includes:
  - main-generator-options
  - main-generator-modules
  - main-generator-individual
  - main-generator-advanced
  - jackson-module
  - jakarta-validation-module
  - javax-validation-module
  - swagger-15-module
  - swagger-2-module
  - maven-plugin
  - faq
  - motivation

search: true

code_clipboard: true
---

# Introduction

The [victools/jsonschema-generator](https://github.com/victools/jsonschema-generator) repository is home to multiple artifacts that are published independently to ["The Central Repository" (Sonatype)](https://central.sonatype.org/) and from there to others like "Maven Central".
Besides the main generator library itself, there are a few modules providing some standard configurations for your convenience.

<aside class="notice">
    There may be other available modules outside of this repository. Refer to the main <a href="https://github.com/victools/jsonschema-generator/blob/master/README.md">README</a> for the list of known modules.
</aside>

This documentation aims at always covering the latest released version of the `jsonschema-generator` and its (standard) modules. There is no documentation for previous versions at this stage.
Please refer to the [CHANGELOG](https://github.com/victools/jsonschema-generator/blob/master/CHANGELOG.md) for a list of the incremental changes.

***

The [victools:jsonschema-generator](https://github.com/victools/jsonschema-generator/tree/master/jsonschema-generator) aims at allowing the generation of JSON Schema (Draft 6, Draft 7, Draft 2019-09 or Draft 2020-12) to document Java code.
This is expressly not limited to _JSON_ but also allows for a Java API to be documented (i.e. including methods and the associated return values).

