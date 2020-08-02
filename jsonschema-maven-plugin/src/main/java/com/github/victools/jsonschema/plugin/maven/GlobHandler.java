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

/**
 * Conversion logic from globs to regular expressions.
 */
public class GlobHandler {

    /**
     * Generate regular expression from the given input for filtering classes on the classpath.
     *
     * @param input either absolute value (with "." as package separator) or glob pattern (with "/" as package separator)
     * @param forPackage whether the given input identifies a package
     * @return regular expression to filter classes on classpath by
     */
    public static Pattern createClassOrPackageNameFilter(String input, boolean forPackage) {
        String inputRegex;
        if (input.chars().anyMatch(c -> c == '/' || c == '*' || c == '?' || c == '+' || c == '[' || c == '{' || c == '\\')) {
            // convert glob pattern into regular expression
            inputRegex = GlobHandler.convertGlobToRegex(input);
        } else {
            // backward compatible support for absolute paths with "." as separator
            inputRegex = input.replace('.', '/');
        }
        if (forPackage) {
            // cater for any classname and any subpackages in between
            inputRegex += inputRegex.charAt(inputRegex.length() - 1) == '/' ? ".+" : "/.+";
        }
        return Pattern.compile(inputRegex);
    }

    /**
     * Converts a standard POSIX Shell globbing pattern into a regular expression pattern. The result can be used with the standard
     * {@link java.util.regex} API to recognize strings which match the glob pattern.
     * <p>
     * See also, the POSIX Shell language: http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
     * </p>
     * Adapted from https://stackoverflow.com/a/17369948 with the difference that a single * is not interpreted as including slashes ("/").
     *
     * @param pattern A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    private static String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
            case '\\':
                if (++i >= arr.length) {
                    sb.append('\\');
                } else {
                    char next = arr[i];
                    switch (next) {
                    case ',':
                        // escape not needed
                        break;
                    case 'Q':
                    case 'E':
                        // extra escape needed
                        sb.append("\\\\");
                        break;
                    default:
                        sb.append('\\');
                    }
                    sb.append(next);
                }
                break;
            case '*':
                if (inClass != 0) {
                    sb.append('*');
                } else if ((i + 1) < arr.length && arr[i + 1] == '*') {
                    i++;
                    sb.append(".*");
                } else {
                    sb.append("[^/]*");
                }
                break;
            case '?':
                if (inClass == 0) {
                    sb.append("[^/]");
                } else {
                    sb.append('?');
                }
                break;
            case '[':
                inClass++;
                firstIndexInClass = i + 1;
                sb.append('[');
                break;
            case ']':
                inClass--;
                sb.append(']');
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                if (inClass == 0 || (firstIndexInClass == i && ch == '^')) {
                    sb.append('\\');
                }
                sb.append(ch);
                break;
            case '!':
                if (firstIndexInClass == i) {
                    sb.append('^');
                } else {
                    sb.append('!');
                }
                break;
            case '{':
                inGroup++;
                sb.append('(');
                break;
            case '}':
                inGroup--;
                sb.append(')');
                break;
            case ',':
                if (inGroup > 0) {
                    sb.append('|');
                } else {
                    sb.append(',');
                }
                break;
            default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
