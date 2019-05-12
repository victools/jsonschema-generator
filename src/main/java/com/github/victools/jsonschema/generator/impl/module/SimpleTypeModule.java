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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProvider;
import com.github.victools.jsonschema.generator.JavaType;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Default module being included if {@code Option.INCLUDE_FIXED_SIMPLE_TYPES} is enabled.
 */
public class SimpleTypeModule implements Module {

    private final Map<Class<?>, String> fixedJsonSchemaTypes = new HashMap<>();

    /**
     * Constructor initialising the mapping between simple java classes and their corresponding JSON Schema equivalents.
     *
     * @see #applyToConfigBuilder(SchemaGeneratorConfigBuilder)
     */
    public SimpleTypeModule() {
        this.withObjectType(Object.class);

        Stream.of(String.class, Character.class, char.class, CharSequence.class)
                .forEach(this::withStringType);

        Stream.of(Boolean.class, boolean.class)
                .forEach(this::withBooleanType);

        Stream.of(Integer.class, int.class, Long.class, long.class, Short.class, short.class, Byte.class, byte.class)
                .forEach(this::withIntegerType);

        Stream.of(Double.class, double.class, Float.class, float.class)
                .forEach(this::withNumberType);
    }

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
     * Add the given mapping for a (simple) java class to its JSON schema equivalent "type" attribute: "{@value SchemaConstants#TAG_TYPE_OBJECT}".
     *
     * @param javaType java class to map to a fixed JSON schema definition
     * @return this module instance (for chaining)
     */
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

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .addNullableCheck(field -> field.getType().isPrimitive() ? Boolean.FALSE : null);
        builder.forMethods()
                .addNullableCheck(method -> method.getReturnType().isPrimitive() ? Boolean.FALSE : null);

        builder.with(new SimpleTypeDefinitionProvider(builder.getObjectMapper()));
    }

    /**
     * Implementation of the {@link CustomDefinitionProvider} interface for apply fixed schema definition for simple java classes.
     */
    private class SimpleTypeDefinitionProvider implements CustomDefinitionProvider {

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
        public CustomDefinition provideCustomSchemaDefinition(JavaType javaType) {
            Type genericType = javaType.getType();
            if (!(genericType instanceof Class<?>)) {
                return null;
            }
            String jsonSchemaTypeValue = SimpleTypeModule.this.fixedJsonSchemaTypes.get(genericType);
            if (jsonSchemaTypeValue == null) {
                return null;
            }
            // create fixed JSON schema definition, containing only the corresponding "type" attribute
            ObjectNode customSchema = objectMapper.createObjectNode()
                    .put(SchemaConstants.TAG_TYPE, jsonSchemaTypeValue);
            // set true as second parameter to indicate simple types to be always in-lined (i.e. not put into definitions)
            return new CustomDefinition(customSchema, true);
        }
    }
}
