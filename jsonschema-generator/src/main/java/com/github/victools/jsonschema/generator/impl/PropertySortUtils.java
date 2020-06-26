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

package com.github.victools.jsonschema.generator.impl;

import com.github.victools.jsonschema.generator.MemberScope;
import java.util.Comparator;

/**
 * Utility class containing the declaration of the default property sort algorithm and its components.
 */
public class PropertySortUtils {

    /**
     * {@link Comparator} sorting properties: with fields before methods.
     */
    public static final Comparator<MemberScope<?, ?>> SORT_PROPERTIES_FIELDS_BEFORE_METHODS
            = (first, second) -> Boolean.compare(first.getSchemaPropertyName().endsWith(")"), second.getSchemaPropertyName().endsWith(")"));

    /**
     * {@link Comparator} sorting properties: alphabetically by their name.
     *
     * @see MemberScope#getSchemaPropertyName()
     */
    public static final Comparator<MemberScope<?, ?>> SORT_PROPERTIES_BY_NAME_ALPHABETICALLY
            = (first, second) -> first.getSchemaPropertyName().compareTo(second.getSchemaPropertyName());

    /**
     * {@link Comparator} sorting properties into the following groups and within each group alphabetically by their name.
     * <ol>
     * <li>instance fields</li>
     * <li>instance methods</li>
     * <li>static fields</li>
     * <li>static methods</li>
     * </ol>
     *
     * @see MemberScope#getSchemaPropertyName()
     */
    public static final Comparator<MemberScope<?, ?>> DEFAULT_PROPERTY_ORDER
            = SORT_PROPERTIES_FIELDS_BEFORE_METHODS.thenComparing(SORT_PROPERTIES_BY_NAME_ALPHABETICALLY);
}
