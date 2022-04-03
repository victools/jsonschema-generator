/*
 * Copyright 2020 VicTools.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.victools.jsonschema.plugin.maven;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for conversion logic from globs to regular expressions. Adapted from: https://stackoverflow.com/a/17369948
 */
public class GlobHandlerTest {

    static Stream<Arguments> parametersForTestBasicPattern() {
        return Stream.of(
            Arguments.of("single star becomes all-but-shlash star", "gl*b", "gl[^/]*b"),
            Arguments.of("double star becomes dot star", "gl**b", "gl.*b"),
            Arguments.of("escaped star is unchanged", "gl\\*b", "gl\\*b"),
            Arguments.of("question mark becomes all-but-shlash", "gl?b", "gl[^/]b"),
            Arguments.of("escaped question mark is unchanged", "gl\\?b", "gl\\?b"),
            Arguments.of("character classes dont need conversion", "gl[-o]b", "gl[-o]b"),
            Arguments.of("escaped classes are unchanged", "gl\\[-o\\]b", "gl\\[-o\\]b"),
            Arguments.of("negation in character classes", "gl[!a-n!p-z]b", "gl[^a-n!p-z]b"),
            Arguments.of("nested negation in character classes", "gl[[!a-n]!p-z]b", "gl[[^a-n]!p-z]b"),
            Arguments.of("escape carat if it is the first char in a character class", "gl[^o]b", "gl[\\^o]b"),
            Arguments.of("metachars are escaped", "gl?*.()+|^$@%b", "gl[^/][^/]*\\.\\(\\)\\+\\|\\^\\$\\@\\%b"),
            Arguments.of("metachars in character classes dont need escaping", "gl[?*.()+|^$@%]b", "gl[?*.()+|^$@%]b"),
            Arguments.of("escaped backslash is unchanged", "gl\\\\b", "gl\\\\b"),
            Arguments.of("slash-Q and slash-E are escaped", "\\Qglob\\E", "\\\\Qglob\\\\E"),
            Arguments.of("braces are turned into groups", "{glob,regex}", "(glob|regex)"),
            Arguments.of("escaped braces are unchanged", "\\{glob\\}", "\\{glob\\}"),
            Arguments.of("commas dont need escaping", "{glob\\,regex},", "(glob,regex),")
        );
    }

    /**
     * Testing the basic glob to regex conversion (with slash prefix to circumvent the backward compatible handling of absolute paths).
     *
     * @param testCaseName what to identify this test case by (not part of the actual test execution)
     * @param globInput class name glob expression as defined in the {@code pom.xml}
     * @param regexOutput expected regular expression for determining whether a class should be included or excluded in the schema generation
     */
    @ParameterizedTest
    @MethodSource("parametersForTestBasicPattern")
    public void testBasicPattern(String testCaseName, String globInput, String regexOutput) {
        Assertions.assertEquals(regexOutput, GlobHandler.createClassOrPackageNameFilter(globInput, false).pattern());
    }

    /**
     * Additional tests for more realistic patterns for class names.
     *
     * @param inputPattern expression entered in the {@code pom.xml}
     * @param clsOrPkg indicating whether the inputPattern is supposed to refer to a class ("cls") or a package ("pkg")
     * @param matching whether the pattern is supposed to match the following classNamePath
     * @param classNamePath full class name with slash ("/") instead of dots (".") as package separators
     */
    @ParameterizedTest
    @CsvSource({
        "com.github.victools.jsonschema, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com.github.victools.jsonschema, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com.github.victools.jsonschema.plugin.maven.testpackage, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com.github.victools.jsonschema.plugin.maven.testpackage, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com.github.victools.jsonschema.plugin.maven.testpackage.TestClassA, pkg, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com.github.victools.jsonschema.plugin.maven.testpackage.TestClassA, cls, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage/*, pkg, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage/*, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/**/testpackage/*, cls, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage/*, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/**/testpackage/**, pkg, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage/**, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/**/testpackage/**, cls, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage/**, cls, true, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/**/testpackage, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/**/testpackage, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "**/subpackage/**, pkg, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "**/subpackage/**, pkg, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "**/subpackage/**, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "**/subpackage/**, cls, true, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "**/subpackage, pkg, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "**/subpackage, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "**/subpackage, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "subpackage/**, pkg, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "subpackage/**, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/*/victools/**, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/*/victools/**, cls, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/*/jsonschema/**, pkg, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/*/jsonschema/**, cls, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/jsonschema/**, pkg, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/jsonschema/**, cls, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
    })
    public void testClassNamePattern(String inputPattern, String clsOrPkg, boolean matching, String classNamePath) {
        Pattern regex = GlobHandler.createClassOrPackageNameFilter(inputPattern, clsOrPkg.equals("pkg"));
        Assertions.assertSame(matching, regex.matcher(classNamePath).matches());
    }
}
