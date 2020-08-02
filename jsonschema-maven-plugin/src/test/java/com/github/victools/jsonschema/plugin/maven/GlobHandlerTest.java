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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for conversion logic from globs to regular expressions. Adapted from: https://stackoverflow.com/a/17369948
 */
@RunWith(JUnitParamsRunner.class)
public class GlobHandlerTest {

    public Object[] parametersForTestBasicPattern() {
        return new Object[][]{
            {"single star becomes all-but-shlash star", "gl*b", "gl[^/]*b"},
            {"double star becomes dot star", "gl**b", "gl.*b"},
            {"escaped star is unchanged", "gl\\*b", "gl\\*b"},
            {"question mark becomes all-but-shlash", "gl?b", "gl[^/]b"},
            {"escaped question mark is unchanged", "gl\\?b", "gl\\?b"},
            {"character classes dont need conversion", "gl[-o]b", "gl[-o]b"},
            {"escaped classes are unchanged", "gl\\[-o\\]b", "gl\\[-o\\]b"},
            {"negation in character classes", "gl[!a-n!p-z]b", "gl[^a-n!p-z]b"},
            {"nested negation in character classes", "gl[[!a-n]!p-z]b", "gl[[^a-n]!p-z]b"},
            {"escape carat if it is the first char in a character class", "gl[^o]b", "gl[\\^o]b"},
            {"metachars are escaped", "gl?*.()+|^$@%b", "gl[^/][^/]*\\.\\(\\)\\+\\|\\^\\$\\@\\%b"},
            {"metachars in character classes dont need escaping", "gl[?*.()+|^$@%]b", "gl[?*.()+|^$@%]b"},
            {"escaped backslash is unchanged", "gl\\\\b", "gl\\\\b"},
            {"slash-Q and slash-E are escaped", "\\Qglob\\E", "\\\\Qglob\\\\E"},
            {"braces are turned into groups", "{glob,regex}", "(glob|regex)"},
            {"escaped braces are unchanged", "\\{glob\\}", "\\{glob\\}"},
            {"commas dont need escaping", "{glob\\,regex},", "(glob,regex),"},
        };
    }

    /**
     * Testing the basic glob to regex conversion (with slash prefix to circumvent the backward compatible handling of absolute paths).
     *
     * @param testCaseName what to identify this test case by (not part of the actual test execution)
     * @param globInput class name glob expression as defined in the {@code pom.xml}
     * @param regexOutput expected regular expression for determining whether a class should be included or excluded in the schema generation
     */
    @Test
    @TestCaseName("testBasicPattern: {0}")
    @Parameters
    public void testBasicPattern(String testCaseName, String globInput, String regexOutput) {
        Assert.assertEquals(regexOutput, GlobHandler.createClassOrPackageNameFilter(globInput, false).pattern());
    }

    /**
     * Additional tests for more realistic patterns for class names.
     *
     * @param inputPattern expression entered in the {@code pom.xml}
     * @param clsOrPkg indicating whether the inputPattern is supposed to refer to a class ("cls") or a package ("pkg")
     * @param matching whether the pattern is supposed to match the following classNamePath
     * @param classNamePath full class name with slash ("/") instead of dots (".") as package separators
     */
    @Test
    @Parameters({
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
        Assert.assertSame(matching, regex.matcher(classNamePath).matches());
    }
}
