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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Conversion logic from globs to regular expressions.
 */
public class GlobHandler {

    private static final char ESCAPE_CHAR = '\\';
    private static final char ASTERISK_CHAR = '*';
    private static final char QUESTION_MARK_CHAR = '?';
    private static final char EXCLAMATION_SIGN_CHAR = '!';
    private static final char COMMA_CHAR = ',';

    private static final int[] GLOB_IDENTIFIERS = {
            ESCAPE_CHAR, ASTERISK_CHAR, QUESTION_MARK_CHAR, '/', '+', '[', '{'
    };
    private static final int[] INPUT_CHARS_REQUIRING_ESCAPE = {
            '.', '(', ')', '+', '|', '^', '$', '@', '%'
    };

    /**
     * Generate predicate to check the given input for filtering classes on the classpath.
     *
     * @param input either absolute value (with "." as package separator) or glob pattern (with "/" as package separator)
     * @param forPackage whether the given input identifies a package
     * @return predicate to filter classes on classpath by
     */
    public static Predicate<String> createClassOrPackageNameFilter(String input, boolean forPackage) {
        return (className) -> GlobHandler.createClassOrPackageNamePattern(input, forPackage).matcher(className).matches();
    }

    /**
     * Generate regular expression from the given input for filtering classes on the classpath.
     *
     * @param input either absolute value (with "." as package separator) or glob pattern (with "/" as package separator)
     * @param forPackage whether the given input identifies a package
     * @return regular expression to filter classes on classpath by
     */
    public static Pattern createClassOrPackageNamePattern(String input, boolean forPackage) {
        String inputRegex = convertInputToRegex(input);
        if (forPackage) {
            // cater for any classname and any subpackages in between
            inputRegex += inputRegex.charAt(inputRegex.length() - 1) == '/' ? ".+" : "/.+";
        }
        return Pattern.compile(inputRegex);
    }

    private static String convertInputToRegex(String input) {
        if (IntStream.of(GLOB_IDENTIFIERS).anyMatch(identifier -> input.chars().anyMatch(inputChar -> inputChar == identifier))) {
            // convert glob pattern into regular expression
            return GlobHandler.convertGlobToRegex(input);
        }
        // backward compatible support for absolute paths with "." as separator
        return input.replace('.', '/');
    }

    /**
     * Converts a standard POSIX Shell globbing pattern into a regular expression pattern. The result can be used with the standard
     * {@link java.util.regex} API to recognize strings which match the glob pattern.
     *
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
        AtomicInteger inGroup = new AtomicInteger(0);
        AtomicInteger inClass = new AtomicInteger(0);
        AtomicInteger firstIndexInClass = new AtomicInteger(-1);
        char[] arr = pattern.toCharArray();
        for (AtomicInteger index = new AtomicInteger(0); index.get() < arr.length; index.incrementAndGet()) {
            char ch = arr[index.get()];
            switch (ch) {
            case ESCAPE_CHAR:
                handleEscapeChar(sb, arr, index.incrementAndGet());
                break;
            case ASTERISK_CHAR:
                handleAsteriskChar(sb, inClass, arr, index);
                break;
            case QUESTION_MARK_CHAR:
                handleQuestionMarkChar(sb, inClass);
                break;
            case '[':
                handleOpeningBracketChar(sb, inClass, firstIndexInClass, index);
                break;
            case ']':
                handleClosingBracketChar(sb, inClass);
                break;
            case EXCLAMATION_SIGN_CHAR:
                handleExclamationSignChar(sb, firstIndexInClass, index);
                break;
            case '{':
                handleOpeningBraceChar(sb, inGroup);
                break;
            case '}':
                handleClosingBraceChar(sb, inGroup);
                break;
            case COMMA_CHAR:
                handleCommaChar(sb, inGroup);
                break;
            default:
                boolean shouldBeEscaped = IntStream.of(INPUT_CHARS_REQUIRING_ESCAPE).anyMatch(specialChar -> specialChar == ch)
                        && (inClass.get() == 0 || (ch == '^' && firstIndexInClass.get() == index.get()));
                if (shouldBeEscaped) {
                    sb.append(ESCAPE_CHAR);
                }
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void handleEscapeChar(StringBuilder sb, char[] arr, int nextCharIndex) {
        if (nextCharIndex >= arr.length) {
            sb.append(ESCAPE_CHAR);
        } else {
            char next = arr[nextCharIndex];
            switch (next) {
            case COMMA_CHAR:
                // escape not needed
                break;
            case 'Q':
            case 'E':
                // extra escape needed
                sb.append(ESCAPE_CHAR).append(ESCAPE_CHAR);
                break;
            default:
                sb.append(ESCAPE_CHAR);
            }
            sb.append(next);
        }
    }

    private static void handleAsteriskChar(StringBuilder sb, AtomicInteger inClass, char[] arr, AtomicInteger index) {
        if (inClass.get() != 0) {
            sb.append(ASTERISK_CHAR);
        } else if ((index.get() + 1) < arr.length && arr[index.get() + 1] == ASTERISK_CHAR) {
            index.incrementAndGet();
            sb.append(".*");
        } else {
            sb.append("[^/]*");
        }
    }

    private static void handleQuestionMarkChar(StringBuilder sb, AtomicInteger inClass) {
        if (inClass.get() == 0) {
            sb.append("[^/]");
        } else {
            sb.append(QUESTION_MARK_CHAR);
        }
    }

    private static void handleExclamationSignChar(StringBuilder sb, AtomicInteger firstIndexInClass, AtomicInteger index) {
        if (firstIndexInClass.get() == index.get()) {
            sb.append('^');
        } else {
            sb.append(EXCLAMATION_SIGN_CHAR);
        }
    }

    private static void handleOpeningBracketChar(StringBuilder sb, AtomicInteger inClass, AtomicInteger firstIndexInClass, AtomicInteger index) {
        inClass.incrementAndGet();
        firstIndexInClass.set(index.get() + 1);
        sb.append('[');
    }

    private static void handleClosingBracketChar(StringBuilder sb, AtomicInteger inClass) {
        inClass.decrementAndGet();
        sb.append(']');
    }

    private static void handleOpeningBraceChar(StringBuilder sb, AtomicInteger inGroup) {
        inGroup.incrementAndGet();
        sb.append('(');
    }

    private static void handleClosingBraceChar(StringBuilder sb, AtomicInteger inGroup) {
        inGroup.decrementAndGet();
        sb.append(')');
    }

    private static void handleCommaChar(StringBuilder sb, AtomicInteger inGroup) {
        if (inGroup.get() > 0) {
            sb.append('|');
        } else {
            sb.append(COMMA_CHAR);
        }
    }
}
