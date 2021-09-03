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
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of a schema generator's configuration.
 */
public interface SchemaGeneratorConfig {

    /**
     * Getter for the designated JSON Schema version.
     *
     * @return target version of the JSON Schema to generate
     */
    SchemaVersion getSchemaVersion();

    /**
     * Getter for the indicated keyword's value in the designated JSON Schema version.
     *
     * @param keyword reference to a tag name or value
     * @return referenced tag name/value in the designated schema version
     * @see #getSchemaVersion()
     * @see SchemaKeyword#forVersion(SchemaVersion)
     */
    String getKeyword(SchemaKeyword keyword);

    /**
     * Determine whether all referenced objects should be listed in the schema's "definitions"/"$defs", even if they only occur once.
     *
     * @return whether to add a definition even for objects occurring only once
     */
    boolean shouldCreateDefinitionsForAllObjects();

    /**
     * Determine whether the schema for the target/main type should be included alongside any "definitions"/"$defs" and be referenced via
     * {@code "$ref": "#"} if necessary. Otherwise, it may be moved into the "definitions"/"$defs" like any other subschema.
     *
     * @return whether to allow "$ref"-erences to the empty fragment "#"
     */
    boolean shouldCreateDefinitionForMainSchema();

    /**
     * Determine whether all sub-schemas should be included in-line, even if they occur multiple times, and not in the schema's "definitions"/"$defs".
     *
     * @return whether to include all sub-schemas in-line
     */
    boolean shouldInlineAllSchemas();

    /**
     * Determine whether the {@link SchemaKeyword#TAG_SCHEMA} attribute with {@link SchemaKeyword#TAG_SCHEMA_VALUE} should be added.
     *
     * @return whether to add the schema version attribute
     */
    boolean shouldIncludeSchemaVersionIndicator();

    /**
     * Determine whether the {@link SchemaKeyword#TAG_REF} values should not just be URI compatible (as expected in JSON Schemas). It should further
     * respect the reduced set of characters as per the following regular expression (as expected by OpenAPI): {@code ^[a-zA-Z0-9\.\-_]+$}
     *
     * @return whether to use only alphanumeric characters, dots, dashes and underscores in {@link SchemaKeyword#TAG_REF} values
     */
    boolean shouldUsePlainDefinitionKeys();

    /**
     * Determine whether extra {@link SchemaKeyword#TAG_FORMAT} values should be included for "simple types".
     *
     * @return whether to include extra {@link SchemaKeyword#TAG_FORMAT} values
     */
    boolean shouldIncludeExtraOpenApiFormatValues();

    /**
     * Determine whether unnecessary {@link SchemaKeyword#TAG_ALLOF} elements should be removed and merged into their declaring schema when there are
     * no conflicts between the sub-schemas.
     *
     * @return whether to clean-up {@link SchemaKeyword#TAG_ALLOF} elements as the last step during schema generation
     */
    boolean shouldCleanupUnnecessaryAllOfElements();

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
     * Determine whether non-void argument-free "getXyZ"/"isXyZ" methods should be represented by the respective field name "xyZ" by default.
     *
     * @return whether to treat argument-free methods as fields in schema
     */
    boolean shouldDeriveFieldsFromArgumentFreeMethods();

    /**
     * Determine whether a single allowed values should be represented by a {@link SchemaKeyword#TAG_CONST}. Otherwise a
     * {@link SchemaKeyword#TAG_ENUM} will be used even for a single allowed value.
     *
     * @return whether to automatically make use of the {@link SchemaKeyword#TAG_CONST "const"} keyword
     */
    boolean shouldRepresentSingleAllowedValueAsConst();

    /**
     * Determine whether "fake" container/array items should be subject to the same nullable checks as the actual declared member type.
     *
     * @return whether to perform nullable checks for "fake" container/array item typees
     *
     * @since 4.20.0
     */
    boolean shouldAllowNullableArrayItems();

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
     * Implementation of the {@link java.util.Comparator#compare(Object, Object)} interface method to determine the order of fields and methods in an
     * object's {@code "properties"}.
     *
     * @param first first field/method to compare to {@code second}
     * @param second second field/method to compare to {@code first}
     * @return a negative/positive integer as the first field/method should be positioned before/after the second respectively
     */
    int sortProperties(MemberScope<?, ?> first, MemberScope<?, ?> second);

    /**
     * Getter for the naming strategy to be applied when determining key names in the "definitions"/"$defs".
     *
     * @return definition key naming strategy
     */
    SchemaDefinitionNamingStrategy getDefinitionNamingStrategy();

    /**
     * Look-up the non-standard JSON schema definition for a given property. Falling-back on the per-type custom definitions.
     *
     * @param <M> type of targeted property
     * @param scope targeted scope for which to provide a custom definition
     * @param context generation context allowing to let the standard generation take over nested parts of the custom definition
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return non-standard JSON schema definition for the given field/method (may be null)
     * @see #getCustomDefinition(ResolvedType, SchemaGenerationContext, CustomDefinitionProviderV2)
     */
    <M extends MemberScope<?, ?>> CustomDefinition getCustomDefinition(M scope, SchemaGenerationContext context,
            CustomPropertyDefinitionProvider<M> ignoredDefinitionProvider);

    /**
     * Look-up the non-standard JSON schema definition for a given type. If this returns null, the standard behaviour is expected to be applied.
     *
     * @param javaType generic type to provide custom definition for
     * @param context generation context allowing to let the standard generation take over nested parts of the custom definition
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return non-standard JSON schema definition (may be null)
     */
    CustomDefinition getCustomDefinition(ResolvedType javaType, SchemaGenerationContext context,
            CustomDefinitionProviderV2 ignoredDefinitionProvider);

    /**
     * Look-up a declared type's subtypes in order to list those specifically (in an {@link SchemaKeyword#TAG_ANYOF}).
     *
     * @param javaType declared type to look-up subtypes for
     * @param context generation context (including a reference to the {@code TypeContext} for deriving a {@link ResolvedType} from a {@link Class})
     * @return subtypes to list as possible alternatives for the declared type (may be empty)
     */
    List<ResolvedType> resolveSubtypes(ResolvedType javaType, SchemaGenerationContext context);

    /**
     * Getter for the applicable type attribute overrides.
     *
     * @return overrides of a given JSON Schema node's type attributes
     */
    List<TypeAttributeOverrideV2> getTypeAttributeOverrides();

    /**
     * Getter for the applicable instance attribute overrides for fields.
     *
     * @return overrides of a given JSON Schema node's instance attributes
     */
    List<InstanceAttributeOverrideV2<FieldScope>> getFieldAttributeOverrides();

    /**
     * Getter for the applicable instance attribute overrides for methods.
     *
     * @return overrides of a given JSON Schema node's instance attributes
     */
    List<InstanceAttributeOverrideV2<MethodScope>> getMethodAttributeOverrides();

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
     * Check whether a field/property value is deemed read-only, i.e., may be ignored or rejected when included in a request.
     *
     * @param field object's field/property to check
     * @return whether the field/property value should be read-only
     */
    boolean isReadOnly(FieldScope field);

    /**
     * Check whether a method value is deemed read-only, i.e., may be ignored or rejected when included in a request.
     *
     * @param method method to check
     * @return whether the method value should be read-only
     */
    boolean isReadOnly(MethodScope method);

    /**
     * Check whether a field/property value is deemed write-only, i.e., is not being returned in responses.
     *
     * @param field object's field/property to check
     * @return whether the field/property value should be write-only
     */
    boolean isWriteOnly(FieldScope field);

    /**
     * Check whether a method value is deemed write-only, i.e., is not being returned in responses.
     *
     * @param method method to check
     * @return whether the method value should be write-only
     */
    boolean isWriteOnly(MethodScope method);

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
     * @deprecated use {@link #resolveTargetTypeOverrides(FieldScope)} instead
     */
    @Deprecated
    default ResolvedType resolveTargetTypeOverride(FieldScope field) {
        List<ResolvedType> result = this.resolveTargetTypeOverrides(field);
        return result == null || result.isEmpty() ? null : result.get(0);
    }

    /**
     * Determine the alternative target type from a method's return value.
     *
     * @param method method for whose return value to determine the target type for
     * @return target type (may be null)
     * @deprecated use {@link #resolveTargetTypeOverrides(MethodScope)} instead
     */
    @Deprecated
    default ResolvedType resolveTargetTypeOverride(MethodScope method) {
        List<ResolvedType> result = this.resolveTargetTypeOverrides(method);
        return result == null || result.isEmpty() ? null : result.get(0);
    }

    /**
     * Determine the alternative target types from an object's field/property.
     *
     * @param field object's field/property to determine the target type for
     * @return target types (may be null or empty)
     */
    List<ResolvedType> resolveTargetTypeOverrides(FieldScope field);

    /**
     * Determine the alternative target types from a method's return value.
     *
     * @param method method for whose return value to determine the target type for
     * @return target types (may be null or empty)
     */
    List<ResolvedType> resolveTargetTypeOverrides(MethodScope method);

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
     * Determine the "$id" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "$id" value for
     * @return "$id" in a JSON Schema (may be null)
     */
    String resolveIdForType(TypeScope scope);

    /**
     * Determine the "$anchor" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "$anchor" value for
     * @return "$anchor" in a JSON Schema (may be null)
     */
    String resolveAnchorForType(TypeScope scope);

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
     * Determine the "title" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "title" value for
     * @return "title" in a JSON Schema (may be null)
     */
    String resolveTitleForType(TypeScope scope);

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
     * Determine the "description" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "description" value for
     * @return "description" in a JSON Schema (may be null)
     */
    String resolveDescriptionForType(TypeScope scope);

    /**
     * Determine the "default" value of an object's field/property.
     *
     * @param field object's field/property to determine "default" value for
     * @return "default" in a JSON Schema (may be null)
     */
    Object resolveDefault(FieldScope field);

    /**
     * Determine the "default" value of a method's return value.
     *
     * @param method method for whose return value to determine "default" value for
     * @return "default" in a JSON Schema (may be null)
     */
    Object resolveDefault(MethodScope method);

    /**
     * Determine the "default" value of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "default" value for
     * @return "default" in a JSON Schema (may be null)
     */
    Object resolveDefaultForType(TypeScope scope);

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
     * Determine the "enum"/"const" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "enum"/"const" value for
     * @return "enum"/"const" in a JSON Schema (may be null)
     */
    Collection<?> resolveEnumForType(TypeScope scope);

    /**
     * Determine the "additionalProperties" of an object's field/property.
     *
     * @param field object's field/property to determine "additionalProperties" value for
     * @return "additionalProperties" in a JSON Schema (may be {@link Void}) to indicate no additional properties being allowed or may be null)
     */
    Type resolveAdditionalProperties(FieldScope field);

    /**
     * Determine the "additionalProperties" of a method's return value.
     *
     * @param method method for whose return value to determine "additionalProperties" value for
     * @return "additionalProperties" in a JSON Schema (may be {@link Void}) to indicate no additional properties being allowed or may be null)
     */
    Type resolveAdditionalProperties(MethodScope method);

    /**
     * Determine the "additionalProperties" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "additionalProperties" value for
     * @return "additionalProperties" in a JSON Schema (may be {@link Void}) to indicate no additional properties being allowed or may be null)
     */
    Type resolveAdditionalPropertiesForType(TypeScope scope);

    /**
     * Determine the "patternProperties" of an object's field/property.
     *
     * @param field object's field/property to determine "patternProperties" value for
     * @return "patternProperties" in a JSON Schema (may be null), the keys representing the patterns and the mapped values their corresponding types
     */
    Map<String, Type> resolvePatternProperties(FieldScope field);

    /**
     * Determine the "patternProperties" of a method's return value.
     *
     * @param method method for whose return value to determine "patternProperties" value for
     * @return "patternProperties" in a JSON Schema (may be null), the keys representing the patterns and the mapped values their corresponding types
     */
    Map<String, Type> resolvePatternProperties(MethodScope method);

    /**
     * Determine the "patternProperties" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "patternProperties" value for
     * @return "patternProperties" in a JSON Schema (may be null), the keys representing the patterns and the mapped values their corresponding types
     */
    Map<String, Type> resolvePatternPropertiesForType(TypeScope scope);

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
     * Determine the "minLength" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "minLength" value for
     * @return "minLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMinLengthForType(TypeScope scope);

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
     * Determine the "maxLength" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "maxLength" value for
     * @return "maxLength" in a JSON Schema (may be null)
     */
    Integer resolveStringMaxLengthForType(TypeScope scope);

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
     * Determine the "format" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "format" value for
     * @return "format" in a JSON Schema (may be null)
     */
    String resolveStringFormatForType(TypeScope scope);

    /**
     * Determine the "pattern" of an object's field/property.
     *
     * @param field object's field/property to determine "pattern" value for
     * @return "pattern" in a JSON Schema (may be null)
     */
    String resolveStringPattern(FieldScope field);

    /**
     * Determine the "pattern" of a method's return value.
     *
     * @param method method for whose return value to determine "pattern" value for
     * @return "pattern" in a JSON Schema (may be null)
     */
    String resolveStringPattern(MethodScope method);

    /**
     * Determine the "pattern" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "pattern" value for
     * @return "pattern" in a JSON Schema (may be null)
     */
    String resolveStringPatternForType(TypeScope scope);

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
     * Determine the "minimum" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "minimum" value for
     * @return "minimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMinimumForType(TypeScope scope);

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
     * Determine the "exclusiveMinimum" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "exclusiveMinimum" value for
     * @return "exclusiveMinimum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMinimumForType(TypeScope scope);

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
     * Determine the "maximum" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "maximum" value for
     * @return "maximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberInclusiveMaximumForType(TypeScope scope);

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
     * Determine the "exclusiveMaximum" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "exclusiveMaximum" value for
     * @return "exclusiveMaximum" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberExclusiveMaximumForType(TypeScope scope);

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
     * Determine the "multipleOf" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "multipleOf" value for
     * @return "multipleOf" in a JSON Schema (may be null)
     */
    BigDecimal resolveNumberMultipleOfForType(TypeScope scope);

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
     * Determine the "minItems" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "minItems" value for
     * @return "minItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMinItemsForType(TypeScope scope);

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
     * Determine the "maxItems" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "maxItems" value for
     * @return "maxItems" in a JSON Schema (may be null)
     */
    Integer resolveArrayMaxItemsForType(TypeScope scope);

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
     * Determine the "uniqueItems" of a context-independent type representation.
     *
     * @param scope context-independent type representation to determine "uniqueItems" value for
     * @return "uniqueItems" in a JSON Schema (may be null)
     */
    Boolean resolveArrayUniqueItemsForType(TypeScope scope);
}
