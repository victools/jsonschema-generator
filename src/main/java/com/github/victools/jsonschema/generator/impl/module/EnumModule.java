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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProvider;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.TypeAttributeOverride;
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default module being included for the {@code Option.ENUM_AS_STRING}.
 */
public class EnumModule implements Module {

    /**
     * Factory method: creating an {@link EnumModule} instance that treats all enums as plain strings.
     *
     * @return created module instance
     */
    public static EnumModule asStrings() {
        return new EnumModule(true);
    }

    /**
     * Factory method: creating an {@link EnumModule} instance that treats all enums as objects but hides all methods declared by the general enum
     * interface but {@link Enum#name() name()}. Methods and fields (including the enum constants) declared by their sub types are not excluded.
     *
     * @return created module instance
     */
    public static EnumModule asObjects() {
        return new EnumModule(false);
    }

    private final boolean treatAsString;

    /**
     * Constructor remembering whether to treat enums as plain strings or as objects.
     *
     * @param treatAsString whether to treat enums as plain strings
     */
    public EnumModule(boolean treatAsString) {
        this.treatAsString = treatAsString;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        if (this.treatAsString) {
            builder.with(new EnumAsStringDefinitionProvider(builder.getObjectMapper()));
        } else {
            builder.forMethods()
                    .withIgnoreCheck((method, declaringContext) -> declaringContext.getType().hasRawClass(Enum.class));
            builder.forFields()
                    .withIgnoreCheck((field, declaringContext) -> field.getAnnotated().isEnumConstant());

            builder.with(new EnumAsObjectTypeAttributeOverride(builder.getObjectMapper()));
        }
    }

    private static boolean isEnumType(JavaType type) {
        return type.hasRawClass(Enum.class);
    }

    /**
     * Look-up the given enum type's constant values.
     *
     * @param <E> specific enum type
     * @param enumType targeted enum type
     * @return collection containing constant enum values
     */
    private static List<String> extractEnumValues(Class<?> enumType) {
        System.out.println("parent of " + enumType.getSimpleName() + " is " + enumType.getGenericSuperclass().getTypeName());
        return Stream.of(enumType.getDeclaredFields())
                .filter(Field::isEnumConstant)
                .map(Field::getName)
                .collect(Collectors.toList());
    }

    /**
     * Implementation of the {@link CustomDefinitionProvider} interface for treating enum types as plain strings.
     */
    private static class EnumAsStringDefinitionProvider implements CustomDefinitionProvider {

        private final ObjectMapper objectMapper;

        /**
         * Constructor setting the given object mapper for later use as ObjectNode provider.
         *
         * @param objectMapper object node provider
         */
        EnumAsStringDefinitionProvider(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        @SuppressWarnings("unchecked")
        public CustomDefinition provideCustomSchemaDefinition(JavaType javaType) {
            if (javaType.isTypeOrSubTypeOf(Enum.class)) {
                ObjectNode customNode = this.objectMapper.createObjectNode()
                        .put(SchemaConstants.TAG_TYPE, SchemaConstants.TAG_TYPE_STRING);
                new AttributeCollector(this.objectMapper)
                        .setEnum(customNode, EnumModule.extractEnumValues(javaType.getRawClass()));
                return new CustomDefinition(customNode);
            }
            return null;
        }
    }

    private static class EnumAsObjectTypeAttributeOverride implements TypeAttributeOverride {

        private final ObjectMapper objectMapper;

        /**
         * Constructor setting the given object mapper for later use as ObjectNode provider.
         *
         * @param objectMapper object node provider
         */
        EnumAsObjectTypeAttributeOverride(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void overrideTypeAttributes(ObjectNode jsonSchemaTypeNode, JavaType javaType, SchemaGeneratorConfig config) {
            if (javaType.isTypeOrSubTypeOf(Enum.class)
                    && jsonSchemaTypeNode.get(SchemaConstants.TAG_CONST) == null
                    && jsonSchemaTypeNode.get(SchemaConstants.TAG_ENUM) == null) {
                new AttributeCollector(this.objectMapper)
                        .setEnum(jsonSchemaTypeNode, EnumModule.extractEnumValues(javaType.getRawClass()));
            }
        }
    }
}
