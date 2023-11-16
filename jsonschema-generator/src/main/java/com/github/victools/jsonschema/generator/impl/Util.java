/*
 * Copyright 2023 VicTools.
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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Utility class offering various helper functions to simplify common checks, e.g., with the goal reduce the complexity of checks and conditions.
 */
public final class Util {

    private Util() {
        // private constructor to avoid instantiation
    }

    /**
     * Check whether the given text value is either {@code null} or empty (i.e., has zero length).
     *
     * @param string the text value to check
     * @return check result
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean isNullOrEmpty(JsonNode node) {
        return node == null || node.isEmpty();
    }

    /**
     * Check whether the given array is either {@code null} or empty (i.e., has zero length).
     *
     * @param array the array to check
     * @return check result
     */
    public static boolean isNullOrEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Check whether the given collection is either {@code null} or empty (i.e., has zero size).
     *
     * @param collection the collection to check
     * @return check result
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Convert the given array into a {@code List} containing its items. If the given array is {@code null}, an empty {@code List} is being returned.
     *
     * @param <T> type of array items
     * @param array the array to convert (may be {@code null}
     * @return list instance
     */
    public static <T> List<T> nullSafe(T[] array) {
        if (isNullOrEmpty(array)) {
            return Collections.emptyList();
        }
        return Arrays.asList(array);
    }

    /**
     * Ensure the given list into a {@code List} containing its items. If the given array is {@code null}, an empty {@code List} is being returned.
     *
     * @param <T> type of list items
     * @param list the list to convert (may be {@code null}
     * @return non-{@code null} list instance
     */
    public static <T> List<T> nullSafe(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }
}
