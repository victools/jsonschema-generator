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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

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
     * Determine whether the "{@value SchemaConstants#TAG_SCHEMA}" attribute with value "{@value SchemaConstants#TAG_SCHEMA_DRAFT7}" should be added.
     *
     * @return whether to add the schema version attribute
     */
    boolean shouldIncludeSchemaVersionIndicator();

    /**
     * Getter for the underlying object mapper.
     *
     * @return object mapper being used for generating JSON Schema structure
     */
    ObjectMapper getObjectMapper();

    /**
     * Generate an empty JSON node representing an object (which will subsequently be filled by the generator).
     * <br>
     * This is equivalent to calling {@code getObjectMapper().createObjectNode()}
     *
     * @return JSON object node
     */
    ObjectNode createObjectNode();

    /**
     * Generate an empty JSON node representing an array (which will subsequently be filled by the generator).
     * <br>
     * This is equivalent to calling {@code getObjectMapper().createArrayNode()}
     *
     * @return JSON array node
     */
    ArrayNode createArrayNode();

    /**
     * Look-up the non-standard JSON schema definition for a given type. If this returns null, the standard behaviour is expected to be applied.
     *
     * @param javaType generic type to provide custom definition for
     * @return non-standard JSON schema definition (may be null)
     */
    CustomDefinition getCustomDefinition(JavaType javaType);

    /**
     * Getter for the applicable type attribute overrides.
     *
     * @return overrides of a given JSON Schema node's type attributes
     */
    List<TypeAttributeOverride> getTypeAttributeOverrides();

    /**
     * Getter for the applicable instance attribute overrides for fields.
     *
     * @return overrides of a given JSON Schema node's instance attributes
     */
    List<InstanceAttributeOverride<Field>> getFieldAttributeOverrides();

    /**
     * Getter for the applicable instance attribute overrides for methods.
     *
     * @return overrides of a given JSON Schema node's instance attributes
     */
    List<InstanceAttributeOverride<Method>> getMethodAttributeOverrides();

    /**
     * Check whether a field/property is nullable.
     *
     * @param field object's field/property to check
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return whether the field/property is nullable
     */
    boolean isNullable(Field field, JavaType fieldType);

    /**
     * Check whether a method's return value is nullable.
     *
     * @param method method to check
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return whether the method's return value is nullable
     */
    boolean isNullable(Method method, JavaType returnValueType);

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
    JavaType resolveTargetTypeOverride(Field field, JavaType defaultType);

    /**
     * Determine the alternative target type from a method's return value.
     *
     * @param method method for whose return value to determine the target type for
     * @param defaultType default type to be used if no override value is being provided
     * @return target type (may be null)
     */
    JavaType resolveTargetTypeOverride(Method method, JavaType defaultType);

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
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "title" in a JSON Schema (may be null)
     */
    String resolveTitle(Field field, JavaType fieldType);

    /**
     * Determine the "title" of a method's return value.
     *
     * @param method method for whose return value to determine "title" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "title" in a JSON Schema (may be null)
     */
    String resolveTitle(Method method, JavaType returnValueType);

    /**
     * Determine the "description" of an object's field/property.
     *
     * @param field object's field/property to determine "description" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "description" in a JSON Schema (may be null)
     */
    String resolveDescription(Field field, JavaType fieldType);

    /**
     * Determine the "description" of a method's return value.
     *
     * @param method method for whose return value to determine "description" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "description" in a JSON Schema (may be null)
     */
    String resolveDescription(Method method, JavaType returnValueType);

    /**
     * Determine the "enum"/"const" of an object's field/property.
     *
     * @param field object's field/property to determine "enum"/"const" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    Collection<?> resolveEnum(Field field, JavaType fieldType);

    /**
     * Determine the "enum"/"const" of a method's return value.
     *
     * @param method method for whose return value to determine "enum"/"const" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    Collection<?> resolveEnum(Method method, JavaType returnValueType);

    /**
     * Determine the "minLength" of an object's field/property.
     *
     * @param field object's field/property to determine "minLength" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "minLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMinLength(Field field, JavaType fieldType);

    /**
     * Determine the "minLength" of a method's return value.
     *
     * @param method method for whose return value to determine "minLength" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "minLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMinLength(Method method, JavaType returnValueType);

    /**
     * Determine the "maxLength" of an object's field/property.
     *
     * @param field object's field/property to determine "maxLength" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "maxLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMaxLength(Field field, JavaType fieldType);

    /**
     * Determine the "maxLength" of a method's return value.
     *
     * @param method method for whose return value to determine "maxLength" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "maxLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMaxLength(Method method, JavaType returnValueType);

    /**
     * Determine the "format" of an object's field/property.
     *
     * @param field object's field/property to determine "format" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "format" in a JSON Schema (may be null)
     */
    String resolveStringFormat(Field field, JavaType fieldType);

    /**
     * Determine the "format" of a method's return value.
     *
     * @param method method for whose return value to determine "format" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "format" in a JSON Schema (may be null)
     */
    String resolveStringFormat(Method method, JavaType returnValueType);

    /**
     * Determine the "minimum" of an object's field/property.
     *
     * @param field object's field/property to determine "minimum" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "minimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMinimum(Field field, JavaType fieldType);

    /**
     * Determine the "minimum" of a method's return value.
     *
     * @param method method for whose return value to determine "minimum" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "minimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMinimum(Method method, JavaType returnValueType);

    /**
     * Determine the "exclusiveMinimum" of an object's field/property.
     *
     * @param field object's field/property to determine "exclusiveMinimum" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMinimum(Field field, JavaType fieldType);

    /**
     * Determine the "exclusiveMinimum" of a method's return value.
     *
     * @param method method for whose return value to determine "exclusiveMinimum" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMinimum(Method method, JavaType returnValueType);

    /**
     * Determine the "maximum" of an object's field/property.
     *
     * @param field object's field/property to determine "maximum" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "maximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMaximum(Field field, JavaType fieldType);

    /**
     * Determine the "maximum" of a method's return value.
     *
     * @param method method for whose return value to determine "maximum" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "maximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMaximum(Method method, JavaType returnValueType);

    /**
     * Determine the "exclusiveMaximum" of an object's field/property.
     *
     * @param field object's field/property to determine "exclusiveMaximum" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMaximum(Field field, JavaType fieldType);

    /**
     * Determine the "exclusiveMaximum" of a method's return value.
     *
     * @param method method for whose return value to determine "exclusiveMaximum" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMaximum(Method method, JavaType returnValueType);

    /**
     * Determine the "multipleOf" of an object's field/property.
     *
     * @param field object's field/property to determine "multipleOf" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberMultipleOf(Field field, JavaType fieldType);

    /**
     * Determine the "multipleOf" of a method's return value.
     *
     * @param method method for whose return value to determine "multipleOf" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberMultipleOf(Method method, JavaType returnValueType);

    /**
     * Determine the "minItems" of an object's field/property.
     *
     * @param field object's field/property to determine "minItems" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "minItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMinItems(Field field, JavaType fieldType);

    /**
     * Determine the "minItems" of a method's return value.
     *
     * @param method method for whose return value to determine "minItems" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "minItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMinItems(Method method, JavaType returnValueType);

    /**
     * Determine the "maxItems" of an object's field/property.
     *
     * @param field object's field/property to determine "maxItems" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "maxItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMaxItems(Field field, JavaType fieldType);

    /**
     * Determine the "maxItems" of a method's return value.
     *
     * @param method method for whose return value to determine "maxItems" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "maxItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMaxItems(Method method, JavaType returnValueType);

    /**
     * Determine the "uniqueItems" of an object's field/property.
     *
     * @param field object's field/property to determine "uniqueItems" value for
     * @param fieldType associated field type (affected by {@link #resolveTargetTypeOverride(Field, JavaType)})
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    Boolean resolveArrayUniqueItems(Field field, JavaType fieldType);

    /**
     * Determine the "uniqueItems" of a method's return value.
     *
     * @param method method for whose return value to determine "uniqueItems" value for
     * @param returnValueType associated return value type (affected by {@link #resolveTargetTypeOverride(Method, JavaType)})
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    Boolean resolveArrayUniqueItems(Method method, JavaType returnValueType);
}
