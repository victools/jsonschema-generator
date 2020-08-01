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
            {"single star becomes all but shlash star", "gl*b", "gl[^/]*b"},
            {"double star becomes dot star", "gl**b", "gl.*b"},
            {"escaped star is unchanged", "gl\\*b", "gl\\*b"},
            {"question mark becomes all but shlash", "gl?b", "gl[^/]b"},
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

    @Test
    @TestCaseName("testBasicPattern: {0}")
    @Parameters
    public void testBasicPattern(String testCaseName, String globInput, String regexOutput) {
        Assert.assertEquals(regexOutput, GlobHandler.convertGlobToRegex(globInput));
    }

    /*
     * Additional tests for more realistic patterns for classnames.
     */
    @Test
    @Parameters({
        "com/**/testpackage/*, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage/*, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/**/testpackage/**, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage/**, true, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/**/testpackage, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/testpackage, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "**/subpackage/**, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "**/subpackage/**, true, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "**/subpackage, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "subpackage/**, false, com/github/victools/jsonschema/plugin/maven/testpackage/subpackage/TestClassC",
        "com/*/victools/**, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/*/jsonschema/**, false, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
        "com/**/jsonschema/**, true, com/github/victools/jsonschema/plugin/maven/testpackage/TestClassA",
    })
    public void testClassNamePattern(String inputPattern, boolean matching, String classNamePath) {
        String regex = GlobHandler.convertGlobToRegex(inputPattern);
        Assert.assertSame(matching, classNamePath.matches(regex));
    }
}
