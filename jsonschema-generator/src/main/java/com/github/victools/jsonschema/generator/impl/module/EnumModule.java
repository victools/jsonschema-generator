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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import com.github.victools.jsonschema.generator.impl.Util;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default module being included for the {@code Option.ENUM_AS_STRING}.
 */
public class EnumModule implements Module {

    /**
     * Factory method: creating an {@link EnumModule} instance that treats all enums as plain strings (derived from their constant value names).
     *
     * @return created module instance
     */
    public static EnumModule asStringsFromName() {
        return new EnumModule(Enum::name);
    }

    /**
     * Factory method: creating an {@link EnumModule} instance that treats all enums as plain strings (derived from each value's toString()).
     *
     * @return created module instance
     */
    public static EnumModule asStringsFromToString() {
        return new EnumModule(Enum::toString);
    }

    /**
     * Factory method: creating an {@link EnumModule} instance that treats all enums as objects but hides all methods declared by the general enum
     * interface but {@link Enum#name() name()}. Methods and fields (including the enum constants) declared by their subtypes are not excluded.
     *
     * @return created module instance
     */
    public static EnumModule asObjects() {
        return new EnumModule(null);
    }

    private final Function<Enum<?>, String> enumConstantToString;

    /**
     * Constructor remembering whether to treat enums as plain strings or as objects.
     *
     * @param enumConstantToString how to derive a plain string representation from an enum constant value, may be null to treat them as objects
     */
    public EnumModule(Function<Enum<?>, String> enumConstantToString) {
        this.enumConstantToString = enumConstantToString;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        if (this.enumConstantToString == null) {
            // ignore all direct enum methods but name() - methods declared by a specific enum subtype are not ignored
            builder.forMethods()
                    .withIgnoreCheck(method -> EnumModule.isEnum(method.getDeclaringType()) && !"name".equals(method.getName()))
                    .withNullableCheck(method -> EnumModule.isEnum(method.getDeclaringType()) ? Boolean.FALSE : null)
                    .withEnumResolver(EnumModule::extractEnumValues);
            builder.forFields()
                    .withIgnoreCheck(field -> field.getRawMember().isEnumConstant());
        } else {
            builder.forTypesInGeneral().withCustomDefinitionProvider(new EnumAsStringDefinitionProvider(this.enumConstantToString));
        }
    }

    private static boolean isEnum(ResolvedType type) {
        return type.getErasedType() == Enum.class;
    }

    /**
     * Look-up the given enum type's constant values.
     *
     * @param method targeted method
     * @return collection containing constant enum values
     */
    private static List<String> extractEnumValues(MethodScope method) {
        ResolvedType declaringType = method.getDeclaringType();
        if (EnumModule.isEnum(declaringType)) {
            ResolvedType enumType = declaringType.getTypeParameters().stream().findFirst().orElse(null);
            if (enumType != null) {
                return EnumModule.extractEnumValues(enumType, Enum::name);
            }
        }
        return null;
    }

    /**
     * Look-up the given enum type's constant values.
     *
     * @param enumType targeted enum type
     * @param enumConstantToString how to derive a plain string representation from an enum constant value
     * @return collection containing constant enum values
     */
    private static List<String> extractEnumValues(ResolvedType enumType, Function<Enum<?>, String> enumConstantToString) {
        Class<?> erasedType = enumType.getErasedType();
        if (Util.isNullOrEmpty(erasedType.getEnumConstants())) {
            return null;
        }
        return Stream.of(erasedType.getEnumConstants())
                .map(enumConstant -> enumConstantToString.apply((Enum<?>) enumConstant))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Implementation of the {@link CustomDefinitionProviderV2} interface for treating enum types as plain strings.
     */
    private static class EnumAsStringDefinitionProvider implements CustomDefinitionProviderV2 {

        private final Function<Enum<?>, String> enumConstantToString;

        /**
         * Constructor.
         *
         * @param enumConstantToString how to derive a plain string representation from an enum constant value
         */
        EnumAsStringDefinitionProvider(Function<Enum<?>, String> enumConstantToString) {
            this.enumConstantToString = enumConstantToString;
        }

        @Override
        public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
            if (javaType.isInstanceOf(Enum.class)) {
                ObjectNode customNode = context.getGeneratorConfig().createObjectNode()
                        .put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_STRING));
                new AttributeCollector(context.getGeneratorConfig().getObjectMapper())
                        .setEnum(customNode, EnumModule.extractEnumValues(javaType, this.enumConstantToString), context);
                return new CustomDefinition(customNode);
            }
            return null;
        }
    }
}
