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
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import com.github.victools.jsonschema.generator.impl.ReflectionTypeUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

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
            // ignore all direct enum methods but name() - methods declared by a specific enum sub type are not ignored
            builder.forMethods()
                    .addIgnoreCheck(method -> method.getDeclaringClass() == Enum.class && !"name".equals(method.getName()))
                    .addEnumResolver(EnumModule::extractEnumValues);
        }
    }

    /**
     * Look-up the given enum type's constant values.
     *
     * @param method targeted method
     * @param returnType method's return type
     * @return collection containing constant enum values
     */
    private static List<String> extractEnumValues(Method method, JavaType returnType) {
        if (method.getDeclaringClass() == Enum.class) {
            Type actualEnumType = returnType.getParentTypeVariables()
                    .resolveGenericTypePlaceholder(Enum.class.getTypeParameters()[0])
                    .getResolvedType();
            return EnumModule.extractEnumValues((Class) actualEnumType);
        }
        return null;
    }

    /**
     * Look-up the given enum type's constant values.
     *
     * @param <E> specific enum type
     * @param enumType targeted enum type
     * @return collection containing constant enum values
     */
    private static <E extends Enum<E>> List<String> extractEnumValues(Class<E> enumType) {
        return EnumSet.allOf(enumType)
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    /**
     * Implementation of the {@link CustomDefinitionProvider} interface for treating enum types as plain strings.
     */
    private static class EnumAsStringDefinitionProvider implements CustomDefinitionProvider {

        private final ObjectMapper objectMapper;

        /**
         * Constructor setting the given object mapper for later use as ObjectNode prodiver.
         *
         * @param objectMapper object node provider
         */
        EnumAsStringDefinitionProvider(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public CustomDefinition provideCustomSchemaDefinition(JavaType javaType) {
            Class rawType = ReflectionTypeUtils.getRawType(javaType.getResolvedType());
            if (rawType != null && rawType.isEnum()) {
                ObjectNode customNode = this.objectMapper.createObjectNode()
                        .put(SchemaConstants.TAG_TYPE, SchemaConstants.TAG_TYPE_STRING);
                new AttributeCollector().setEnum(customNode, EnumModule.extractEnumValues(rawType));
                return new CustomDefinition(customNode);
            }
            return null;
        }
    }
}
