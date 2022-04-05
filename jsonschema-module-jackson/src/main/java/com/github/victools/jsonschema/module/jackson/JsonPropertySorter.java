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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.members.HierarchicType;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.impl.PropertySortUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implementation of the sorting logic for an object's properties based on a {@link JsonPropertyOrder} annotation on the declaring type.
 */
public class JsonPropertySorter implements Comparator<MemberScope<?, ?>> {

    private final boolean sortAlphabeticallyIfNotAnnotated;
    private final Map<Class<?>, List<String>> propertyOrderPerDeclaringType = new HashMap<>();
    private final Map<Class<?>, Boolean> enabledAlphabeticSorting = new HashMap<>();

    /**
     * Constructor.
     *
     * @param sortAlphabeticallyIfNotAnnotated whether properties of a type without {@link JsonPropertyOrder} should be sorted alphabetically
     */
    public JsonPropertySorter(boolean sortAlphabeticallyIfNotAnnotated) {
        this.sortAlphabeticallyIfNotAnnotated = sortAlphabeticallyIfNotAnnotated;
    }

    @Override
    public int compare(MemberScope<?, ?> first, MemberScope<?, ?> second) {
        int result = PropertySortUtils.SORT_PROPERTIES_FIELDS_BEFORE_METHODS.compare(first, second);
        if (result == 0) {
            result = this.getPropertyIndex(first) - this.getPropertyIndex(second);
        }
        if (result == 0 && Stream.of(first, second)
                .map(property -> property.getDeclaringType().getErasedType())
                .anyMatch(parentType -> this.enabledAlphabeticSorting.computeIfAbsent(parentType, this::shouldSortPropertiesAlphabetically))) {
            result = PropertySortUtils.SORT_PROPERTIES_BY_NAME_ALPHABETICALLY.compare(first, second);
        }
        return result;
    }

    /**
     * Determine the given property's position in its declaring type's schema based on a {@link JsonPropertyOrder} annotation. If no such annotation
     * is present, {@link Integer#MAX_VALUE} will be returned to append these at the end of the list of properties.
     *
     * @param property field/method for which the respective index should be determined
     * @return specific property index or {@link Integer#MAX_VALUE}
     */
    protected int getPropertyIndex(MemberScope<?, ?> property) {
        HierarchicType topMostHierarchyType = property.getDeclaringTypeMembers().allTypesAndOverrides().get(0);
        List<String> sortedProperties = this.propertyOrderPerDeclaringType
                .computeIfAbsent(topMostHierarchyType.getErasedType(), this::getAnnotatedPropertyOrder);
        String fieldName;
        if (property instanceof MethodScope) {
            fieldName = Optional.ofNullable(((MethodScope) property).findGetterField()).map(MemberScope::getSchemaPropertyName).orElse(null);
        } else {
            fieldName = property.getSchemaPropertyName();
        }
        int propertyIndex = sortedProperties.indexOf(fieldName);
        if (propertyIndex == -1) {
            propertyIndex = Integer.MAX_VALUE;
        }
        return propertyIndex;
    }

    /**
     * Determine whether the given type's properties that are not specifically mentioned in a {@link JsonPropertyOrder} annotation should be sorted
     * alphabetically, based on {@link JsonPropertyOrder#alphabetic()}. If no such annotation is present, the value given in the
     * {@link #JsonPropertySorter(boolean)} constructor.
     *
     * @param declaringType type for which the properties' default sorting should be determined
     * @return whether properties that are not specifically mentioned in a {@link JsonPropertyOrder} annotation should be sorted alphabetically
     */
    protected boolean shouldSortPropertiesAlphabetically(Class<?> declaringType) {
        return Optional.ofNullable(declaringType.getAnnotation(JsonPropertyOrder.class))
                .map(JsonPropertyOrder::alphabetic)
                .orElse(this.sortAlphabeticallyIfNotAnnotated);
    }

    /**
     * Lookup the list of specifically sorted property names in the given type based on its {@link JsonPropertyOrder} annotation.
     *
     * @param declaringType type for which to lookup the list of specifically sorted property names
     * @return {@link JsonPropertyOrder#value()} or empty list
     */
    private List<String> getAnnotatedPropertyOrder(Class<?> declaringType) {
        return Optional.ofNullable(declaringType.getAnnotation(JsonPropertyOrder.class))
                .map(JsonPropertyOrder::value)
                .filter(valueArray -> valueArray.length != 0)
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList);
    }
}
