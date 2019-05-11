/*
 * Copyright 2019 VicTools.
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

package com.github.victools.jsonschema.generator;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;

/**
 * Default implementation of a schema generator's configuration.
 */
public interface SchemaGeneratorConfig {

    /**
     * Determine whether all referenced objects should be listed in the schema's "definitions", even if they only occur once.
     *
     * @return whether to add a definition even for objects occurring only once
     */
    boolean shouldCreateDefinitionsForAllObjects();

    /**
     * Generate an empty JSON node representing an object (which will subsequently be filled by the generator).
     *
     * @return JSON object node
     */
    ObjectNode createObjectNode();

    /**
     * Generate an empty JSON node representing an array (which will subsequently be filled by the generator).
     *
     * @return JSON array node
     */
    ArrayNode createArrayNode();

    /**
     * Look-up the non-standard JSON schema definition for a given type. If this returns null, the standard behaviour is expected to be applied.
     *
     * @param javaType generic type to provide custom definition for
     * @param placeholderResolver resolver for type variables in the specific context (if the javaType is a generic/parameterized type)
     * @return non-standard JSON schema definition (may be null)
     */
    CustomDefinition getCustomDefinition(Type javaType, TypePlaceholderResolver placeholderResolver);

    /**
     * Check whether a field/property is nullable.
     *
     * @param field object's field/property to check
     * @return whether the field/property is nullable
     */
    boolean isNullable(Field field);

    /**
     * Check whether a method's return value is nullable.
     *
     * @param method method to check
     * @return whether the method's return value is nullable
     */
    boolean isNullable(Method method);

    /**
     * Check whether a field/property should be ignored.
     *
     * @param field object's field/property to check
     * @return whether the field/property should be ignored
     */
    boolean shouldIgnore(Field field);

    /**
     * Check whether a method should be ignored.
     *
     * @param method method to check
     * @return whether the method should be ignored
     */
    boolean shouldIgnore(Method method);

    /**
     * Determine the alternative target type from an object's field/property.
     *
     * @param field object's field/property to determine the target type for
     * @param defaultType default type to be used if no override value is being provided
     * @return target type (may be null)
     */
    Type resolveTargetTypeOverride(Field field, Type defaultType);

    /**
     * Determine the alternative target type from a method's return value.
     *
     * @param method method for whose return value to determine the target type for
     * @param defaultType default type to be used if no override value is being provided
     * @return target type (may be null)
     */
    Type resolveTargetTypeOverride(Method method, Type defaultType);

    /**
     * Determine the alternative name in a parent JSON Schema's "properties" from an object's field/property.
     *
     * @param field object's field/property to determine name in parent JSON Schema's properties for
     * @param defaultName default name to be set if no override value is being provided
     * @return name in a parent JSON Schema's "properties" (may be null, thereby falling back on the default value)
     */
    String resolvePropertyNameOverride(Field field, String defaultName);

    /**
     * Determine the alternative name in a parent JSON Schema's "properties" from a method's return value.
     *
     * @param method method for whose return value to determine name in parent JSON Schema's properties for
     * @param defaultName default name to be set if no override value is being provided
     * @return name in a parent JSON Schema's "properties" (may be null, thereby falling back on the default value)
     */
    String resolvePropertyNameOverride(Method method, String defaultName);

    /**
     * Determine the "title" of an object's field/property.
     *
     * @param field object's field/property to determine "title" value for
     * @return "title" in a JSON Schema (may be null)
     */
    String resolveTitle(Field field);

    /**
     * Determine the "title" of a method's return value.
     *
     * @param method method for whose return value to determine "title" value for
     * @return "title" in a JSON Schema (may be null)
     */
    String resolveTitle(Method method);

    /**
     * Determine the "description" of an object's field/property.
     *
     * @param field object's field/property to determine "description" value for
     * @return "description" in a JSON Schema (may be null)
     */
    String resolveDescription(Field field);

    /**
     * Determine the "description" of a method's return value.
     *
     * @param method method for whose return value to determine "description" value for
     * @return "description" in a JSON Schema (may be null)
     */
    String resolveDescription(Method method);

    /**
     * Determine the "enum"/"const" of an object's field/property.
     *
     * @param field object's field/property to determine "enum"/"const" value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    Collection<?> resolveEnum(Field field);

    /**
     * Determine the "enum"/"const" of a method's return value.
     *
     * @param method method for whose return value to determine "enum"/"const" value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    Collection<?> resolveEnum(Method method);

    /**
     * Determine the "minLength" of an object's field/property.
     *
     * @param field object's field/property to determine "minLength" value for
     * @return "minLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMinLength(Field field);

    /**
     * Determine the "minLength" of a method's return value.
     *
     * @param method method for whose return value to determine "minLength" value for
     * @return "minLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMinLength(Method method);

    /**
     * Determine the "maxLength" of an object's field/property.
     *
     * @param field object's field/property to determine "maxLength" value for
     * @return "maxLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMaxLength(Field field);

    /**
     * Determine the "maxLength" of a method's return value.
     *
     * @param method method for whose return value to determine "maxLength" value for
     * @return "maxLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMaxLength(Method method);

    /**
     * Determine the "format" of an object's field/property.
     *
     * @param field object's field/property to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    String resolveStringFormat(Field field);

    /**
     * Determine the "format" of a method's return value.
     *
     * @param method method for whose return value to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    String resolveStringFormat(Method method);

    /**
     * Determine the "minimum" of an object's field/property.
     *
     * @param field object's field/property to determine "minimum" value for
     * @return "minimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMinimum(Field field);

    /**
     * Determine the "minimum" of a method's return value.
     *
     * @param method method for whose return value to determine "minimum" value for
     * @return "minimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMinimum(Method method);

    /**
     * Determine the "exclusiveMinimum" of an object's field/property.
     *
     * @param field object's field/property to determine "exclusiveMinimum" value for
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMinimum(Field field);

    /**
     * Determine the "exclusiveMinimum" of a method's return value.
     *
     * @param method method for whose return value to determine "exclusiveMinimum" value for
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMinimum(Method method);

    /**
     * Determine the "maximum" of an object's field/property.
     *
     * @param field object's field/property to determine "maximum" value for
     * @return "maximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMaximum(Field field);

    /**
     * Determine the "maximum" of a method's return value.
     *
     * @param method method for whose return value to determine "maximum" value for
     * @return "maximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMaximum(Method method);

    /**
     * Determine the "exclusiveMaximum" of an object's field/property.
     *
     * @param field object's field/property to determine "exclusiveMaximum" value for
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMaximum(Field field);

    /**
     * Determine the "exclusiveMaximum" of a method's return value.
     *
     * @param method method for whose return value to determine "exclusiveMaximum" value for
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMaximum(Method method);

    /**
     * Determine the "multipleOf" of an object's field/property.
     *
     * @param field object's field/property to determine "multipleOf" value for
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberMultipleOf(Field field);

    /**
     * Determine the "multipleOf" of a method's return value.
     *
     * @param method method for whose return value to determine "multipleOf" value for
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberMultipleOf(Method method);

    /**
     * Determine the "minItems" of an object's field/property.
     *
     * @param field object's field/property to determine "minItems" value for
     * @return "minItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMinItems(Field field);

    /**
     * Determine the "minItems" of a method's return value.
     *
     * @param method method for whose return value to determine "minItems" value for
     * @return "minItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMinItems(Method method);

    /**
     * Determine the "maxItems" of an object's field/property.
     *
     * @param field object's field/property to determine "maxItems" value for
     * @return "maxItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMaxItems(Field field);

    /**
     * Determine the "maxItems" of a method's return value.
     *
     * @param method method for whose return value to determine "maxItems" value for
     * @return "maxItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMaxItems(Method method);

    /**
     * Determine the "uniqueItems" of an object's field/property.
     *
     * @param field object's field/property to determine "uniqueItems" value for
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    Boolean resolveArrayUniqueItems(Field field);

    /**
     * Determine the "uniqueItems" of a method's return value.
     *
     * @param method method for whose return value to determine "uniqueItems" value for
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    Boolean resolveArrayUniqueItems(Method method);
}
