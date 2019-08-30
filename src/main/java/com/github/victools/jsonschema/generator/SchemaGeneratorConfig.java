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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * Determine whether static fields should be included in the generated schema.
     *
     * @return whether to include static fields
     */
    boolean shouldIncludeStaticFields();

    /**
     * Determine whether static methods should be included in the generated schema.
     *
     * @return whether to included static methods
     */
    boolean shouldIncludeStaticMethods();

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
     * @param context overall type resolution context being used
     * @return non-standard JSON schema definition (may be null)
     */
    CustomDefinition getCustomDefinition(ResolvedType javaType, TypeContext context);

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
    List<InstanceAttributeOverride<FieldScope>> getFieldAttributeOverrides();

    /**
     * Getter for the applicable instance attribute overrides for methods.
     *
     * @return overrides of a given JSON Schema node's instance attributes
     */
    List<InstanceAttributeOverride<MethodScope>> getMethodAttributeOverrides();

    /**
     * Check whether a field/property is nullable.
     *
     * @param field object's field/property to check
     * @return whether the field/property is nullable
     */
    boolean isNullable(FieldScope field);

    /**
     * Check whether a method's return value is nullable.
     *
     * @param method method to check
     * @return whether the method's return value is nullable
     */
    boolean isNullable(MethodScope method);

    /**
     * Check whether a field/property should be ignored.
     *
     * @param field object's field/property to check
     * @return whether the field/property should be ignored
     */
    boolean shouldIgnore(FieldScope field);

    /**
     * Check whether a method should be ignored.
     *
     * @param method method to check
     * @return whether the method should be ignored
     */
    boolean shouldIgnore(MethodScope method);

    /**
     * Determine the alternative target type from an object's field/property.
     *
     * @param field object's field/property to determine the target type for
     * @return target type (may be null)
     */
    ResolvedType resolveTargetTypeOverride(FieldScope field);

    /**
     * Determine the alternative target type from a method's return value.
     *
     * @param method method for whose return value to determine the target type for
     * @return target type (may be null)
     */
    ResolvedType resolveTargetTypeOverride(MethodScope method);

    /**
     * Determine the alternative name in a parent JSON Schema's "properties" from an object's field/property.
     *
     * @param field object's field/property to determine name in parent JSON Schema's properties for
     * @return name in a parent JSON Schema's "properties" (may be null, thereby falling back on the default value)
     */
    String resolvePropertyNameOverride(FieldScope field);

    /**
     * Determine the alternative name in a parent JSON Schema's "properties" from a method's return value.
     *
     * @param method method for whose return value to determine name in parent JSON Schema's properties for
     * @return name in a parent JSON Schema's "properties" (may be null, thereby falling back on the default value)
     */
    String resolvePropertyNameOverride(MethodScope method);

    /**
     * Determine the "title" of an object's field/property.
     *
     * @param field object's field/property to determine "title" value for
     * @return "title" in a JSON Schema (may be null)
     */
    String resolveTitle(FieldScope field);

    /**
     * Determine the "title" of a method's return value.
     *
     * @param method method for whose return value to determine "title" value for
     * @return "title" in a JSON Schema (may be null)
     */
    String resolveTitle(MethodScope method);

    /**
     * Determine the "description" of an object's field/property.
     *
     * @param field object's field/property to determine "description" value for
     * @return "description" in a JSON Schema (may be null)
     */
    String resolveDescription(FieldScope field);

    /**
     * Determine the "description" of a method's return value.
     *
     * @param method method for whose return value to determine "description" value for
     * @return "description" in a JSON Schema (may be null)
     */
    String resolveDescription(MethodScope method);

    /**
     * Determine the "default" value of a method's return value.
     *
     * @param method method for whose return value to determine "default" value for
     * @return "default" in a JSON Schema (may be null)
     */
    String resolveDefault(MethodScope method);

    /**
     * Determine the "default" value of an object's field/property.
     *
     * @param field object's field/property to determine "default" value for
     * @return "default" in a JSON Schema (may be null)
     */
    String resolveDefault(FieldScope field);

    /**
     * Determine the "enum"/"const" of an object's field/property.
     *
     * @param field object's field/property to determine "enum"/"const" value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    Collection<?> resolveEnum(FieldScope field);

    /**
     * Determine the "enum"/"const" of a method's return value.
     *
     * @param method method for whose return value to determine "enum"/"const" value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    Collection<?> resolveEnum(MethodScope method);

    /**
     * Determine the "minLength" of an object's field/property.
     *
     * @param field object's field/property to determine "minLength" value for
     * @return "minLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMinLength(FieldScope field);

    /**
     * Determine the "minLength" of a method's return value.
     *
     * @param method method for whose return value to determine "minLength" value for
     * @return "minLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMinLength(MethodScope method);

    /**
     * Determine the "maxLength" of an object's field/property.
     *
     * @param field object's field/property to determine "maxLength" value for
     * @return "maxLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMaxLength(FieldScope field);

    /**
     * Determine the "maxLength" of a method's return value.
     *
     * @param method method for whose return value to determine "maxLength" value for
     * @return "maxLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMaxLength(MethodScope method);

    /**
     * Determine the "format" of an object's field/property.
     *
     * @param field object's field/property to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    String resolveStringFormat(FieldScope field);

    /**
     * Determine the "format" of a method's return value.
     *
     * @param method method for whose return value to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    String resolveStringFormat(MethodScope method);

    /**
     * Determine the "minimum" of an object's field/property.
     *
     * @param field object's field/property to determine "minimum" value for
     * @return "minimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMinimum(FieldScope field);

    /**
     * Determine the "minimum" of a method's return value.
     *
     * @param method method for whose return value to determine "minimum" value for
     * @return "minimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMinimum(MethodScope method);

    /**
     * Determine the "exclusiveMinimum" of an object's field/property.
     *
     * @param field object's field/property to determine "exclusiveMinimum" value for
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMinimum(FieldScope field);

    /**
     * Determine the "exclusiveMinimum" of a method's return value.
     *
     * @param method method for whose return value to determine "exclusiveMinimum" value for
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMinimum(MethodScope method);

    /**
     * Determine the "maximum" of an object's field/property.
     *
     * @param field object's field/property to determine "maximum" value for
     * @return "maximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMaximum(FieldScope field);

    /**
     * Determine the "maximum" of a method's return value.
     *
     * @param method method for whose return value to determine "maximum" value for
     * @return "maximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMaximum(MethodScope method);

    /**
     * Determine the "exclusiveMaximum" of an object's field/property.
     *
     * @param field object's field/property to determine "exclusiveMaximum" value for
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMaximum(FieldScope field);

    /**
     * Determine the "exclusiveMaximum" of a method's return value.
     *
     * @param method method for whose return value to determine "exclusiveMaximum" value for
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMaximum(MethodScope method);

    /**
     * Determine the "multipleOf" of an object's field/property.
     *
     * @param field object's field/property to determine "multipleOf" value for
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberMultipleOf(FieldScope field);

    /**
     * Determine the "multipleOf" of a method's return value.
     *
     * @param method method for whose return value to determine "multipleOf" value for
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberMultipleOf(MethodScope method);

    /**
     * Determine the "minItems" of an object's field/property.
     *
     * @param field object's field/property to determine "minItems" value for
     * @return "minItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMinItems(FieldScope field);

    /**
     * Determine the "minItems" of a method's return value.
     *
     * @param method method for whose return value to determine "minItems" value for
     * @return "minItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMinItems(MethodScope method);

    /**
     * Determine the "maxItems" of an object's field/property.
     *
     * @param field object's field/property to determine "maxItems" value for
     * @return "maxItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMaxItems(FieldScope field);

    /**
     * Determine the "maxItems" of a method's return value.
     *
     * @param method method for whose return value to determine "maxItems" value for
     * @return "maxItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMaxItems(MethodScope method);

    /**
     * Determine the "uniqueItems" of an object's field/property.
     *
     * @param field object's field/property to determine "uniqueItems" value for
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    Boolean resolveArrayUniqueItems(FieldScope field);

    /**
     * Determine the "uniqueItems" of a method's return value.
     *
     * @param method method for whose return value to determine "uniqueItems" value for
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    Boolean resolveArrayUniqueItems(MethodScope method);


    /**
     * Check whether a field/property value is required.
     *
     * @param field object's field/property to check
     * @return whether the field/property value should be required
     */
    boolean isRequired(FieldScope field);

    /**
     * Check whether a method value is required.
     *
     * @param method method to check
     * @return whether the method value should be required
     */
    boolean isRequired(MethodScope method);

    /**
     * Determine the metadata of an object's field/property.
     *
     * @param field object's field/property to determine metadata value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    Map<String, String> resolveMetadata(FieldScope field);

    /**
     * Determine the metadata of a method's return value.
     *
     * @param method method for whose return value to determine metadata value for
     * @return metadata in a JSON Schema (may be null)
     */
    Map<String, String> resolveMetadata(MethodScope method);
}
