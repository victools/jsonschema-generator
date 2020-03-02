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

package com.github.victools.jsonschema.generator.impl.module;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Default module being included for the {@code Option.ADDITIONAL_FIXED_TYPES}.
 */
public class SimpleTypeModule implements Module {

    /**
     * Factory method: creating an instance of the {@link SimpleTypeModule} containing mappings for various primitive types and their non-primitive
     * counter parts (e.g. {@code boolean} and {@code Boolean}).
     *
     * @return created module instance
     * @see #forPrimitiveAndAdditionalTypes()
     */
    public static SimpleTypeModule forPrimitiveTypes() {
        SimpleTypeModule module = new SimpleTypeModule();

        module.withEmptySchema(Object.class);

        Stream.of(String.class, Character.class, char.class, CharSequence.class)
                .forEach(module::withStringType);

        Stream.of(Boolean.class, boolean.class)
                .forEach(module::withBooleanType);

        Stream.of(Integer.class, int.class, Long.class, long.class, Short.class, short.class, Byte.class, byte.class)
                .forEach(module::withIntegerType);

        Stream.of(Double.class, double.class, Float.class, float.class)
                .forEach(module::withNumberType);

        return module;
    }

    /**
     * Factory method: creating an instance of the {@link SimpleTypeModule} containing mappings for various primitive types and their non-primitive
     * counter parts (e.g. {@code boolean} and {@code Boolean}) as well as other classes that are normally serialised in a JSON as non-objects, e.g.
     * <br>{@link BigDecimal}, {@link BigInteger}, {@link UUID}, {@link LocalDate}, {@link LocalDateTime}, and a number of other date-time types.
     *
     * @return created module instance
     * @see #forPrimitiveTypes()
     */
    public static SimpleTypeModule forPrimitiveAndAdditionalTypes() {
        SimpleTypeModule module = SimpleTypeModule.forPrimitiveTypes();

        Stream.of(LocalDate.class, LocalDateTime.class, LocalTime.class, ZonedDateTime.class, OffsetDateTime.class, OffsetTime.class,
                Instant.class, ZoneId.class, Date.class, Calendar.class, UUID.class)
                .forEach(module::withStringType);

        module.withIntegerType(BigInteger.class);

        Stream.of(BigDecimal.class, Number.class)
                .forEach(module::withNumberType);

        return module;
    }

    private final Map<Class<?>, String> fixedJsonSchemaTypes = new HashMap<>();

    /**
     * Add the given mapping for a (simple) java class to its JSON schema equivalent "type" attribute.
     *
     * @param javaType java class to map to a fixed JSON schema definition
     * @param jsonSchemaTypeValue "type" attribute value to set
     * @return this module instance (for chaining)
     */
    private SimpleTypeModule with(Class<?> javaType, String jsonSchemaTypeValue) {
        this.fixedJsonSchemaTypes.put(javaType, jsonSchemaTypeValue);
        return this;
    }

    /**
     * Add the given mapping for a (simple) java class that should be represented by an empty schema.
     *
     * @param javaType java class to map to a fixed JSON schema definition
     * @return this module instance (for chaining)
     */
    public final SimpleTypeModule withEmptySchema(Class<?> javaType) {
        return this.with(javaType, "");
    }

    /**
     * Add the given mapping for a (simple) java class to its JSON schema equivalent "type" attribute: "{@value SchemaConstants#TAG_TYPE_OBJECT}".
     *
     * @param javaType java class to map to a fixed JSON schema definition
     * @return this module instance (for chaining)
     * @deprecated rather use {@link SimpleTypeModule#withEmptySchema(Class)} instead to really allow any value
     */
    @Deprecated
    public final SimpleTypeModule withObjectType(Class<?> javaType) {
        return this.with(javaType, SchemaConstants.TAG_TYPE_OBJECT);
    }

    /**
     * Add the given mapping for a (simple) java class to its JSON schema equivalent "type" attribute: "{@value SchemaConstants#TAG_TYPE_STRING}".
     *
     * @param javaType java class to map to a fixed JSON schema definition
     * @return this module instance (for chaining)
     */
    public final SimpleTypeModule withStringType(Class<?> javaType) {
        return this.with(javaType, SchemaConstants.TAG_TYPE_STRING);
    }

    /**
     * Add the given mapping for a (simple) java class to its JSON schema equivalent "type" attribute: "{@value SchemaConstants#TAG_TYPE_BOOLEAN}".
     *
     * @param javaType java class to map to a fixed JSON schema definition
     * @return this module instance (for chaining)
     */
    public final SimpleTypeModule withBooleanType(Class<?> javaType) {
        return this.with(javaType, SchemaConstants.TAG_TYPE_BOOLEAN);
    }

    /**
     * Add the given mapping for a (simple) java class to its JSON schema equivalent "type" attribute: "{@value SchemaConstants#TAG_TYPE_INTEGER}".
     *
     * @param javaType java class to map to a fixed JSON schema definition
     * @return this module instance (for chaining)
     */
    public final SimpleTypeModule withIntegerType(Class<?> javaType) {
        return this.with(javaType, SchemaConstants.TAG_TYPE_INTEGER);
    }

    /**
     * Add the given mapping for a (simple) java class to its JSON schema equivalent "type" attribute: "{@value SchemaConstants#TAG_TYPE_NUMBER}".
     *
     * @param javaType java class to map to a fixed JSON schema definition
     * @return this module instance (for chaining)
     */
    public final SimpleTypeModule withNumberType(Class<?> javaType) {
        return this.with(javaType, SchemaConstants.TAG_TYPE_NUMBER);
    }

    /**
     * Determine whether a given type is nullable – returning false if the type refers to a primitive type.
     *
     * @param fieldOrMethod reference to the method/field (which is being ignored here)
     * @return false if type is a primitive, otherwise null
     */
    private Boolean isNullableType(MemberScope<?, ?> fieldOrMethod) {
        // no need to resolve the JavaType, as generics cannot contain primitive types
        return fieldOrMethod.getType().isPrimitive() ? Boolean.FALSE : null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withNullableCheck(this::isNullableType);
        builder.forMethods()
                .withNullableCheck(this::isNullableType);
        builder.forTypesInGeneral()
                .withAdditionalPropertiesResolver(this::resolveAdditionalProperties)
                .withPatternPropertiesResolver(this::resolvePatternProperties);

        builder.with(new SimpleTypeDefinitionProvider(builder.getObjectMapper()));
    }

    /**
     * Specifically omit the "additionalProperties" keyword for non-object types.
     *
     * @param scope the scope to check for fixed non-object types
     * @return either Object.class to cause omission of the "additonalProperties" keyword or null to leave it up to following configurations
     */
    private Type resolveAdditionalProperties(TypeScope scope) {
        if (scope.getType().getTypeParameters().isEmpty() && "".equals(this.fixedJsonSchemaTypes.get(scope.getType().getErasedType()))) {
            // indicate no specific additionalProperties type - thereby causing it to be omitted from the generated schema
            return Object.class;
        }
        return null;
    }

    /**
     * Specifically omit the "patternProperties" keyword for non-object types.
     *
     * @param scope the scope to check for fixed non-object types
     * @return either an empty map to cause omission of the "patternProperties" keyword or null to leave it up to following configurations
     */
    private Map<String, Type> resolvePatternProperties(TypeScope scope) {
        if (scope.getType().getTypeParameters().isEmpty() && "".equals(this.fixedJsonSchemaTypes.get(scope.getType().getErasedType()))) {
            // indicate no specific patternProperties - thereby causing it to be omitted from the generated schema
            return Collections.emptyMap();
        }
        return null;
    }

    /**
     * Implementation of the {@link CustomDefinitionProviderV2} interface for applying fixed schema definitions for simple java types.
     */
    private class SimpleTypeDefinitionProvider implements CustomDefinitionProviderV2 {

        private final ObjectMapper objectMapper;

        /**
         * Constructor setting the object mapper to use for creating a custom schema definition for later use.
         *
         * @param objectMapper supplier for object and array nodes for the JSON structure being generated
         */
        SimpleTypeDefinitionProvider(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
            if (!javaType.getTypeParameters().isEmpty()) {
                return null;
            }
            String jsonSchemaTypeValue = SimpleTypeModule.this.fixedJsonSchemaTypes.get(javaType.getErasedType());
            if (jsonSchemaTypeValue == null) {
                return null;
            }
            // create fixed JSON schema definition, containing only the corresponding "type" attribute
            ObjectNode customSchema = this.objectMapper.createObjectNode();
            if (!jsonSchemaTypeValue.isEmpty()) {
                customSchema.put(SchemaConstants.TAG_TYPE, jsonSchemaTypeValue);
            }
            // set true as second parameter to indicate simple types to be always in-lined (i.e. not put into definitions)
            return new CustomDefinition(customSchema, true);
        }
    }
}
