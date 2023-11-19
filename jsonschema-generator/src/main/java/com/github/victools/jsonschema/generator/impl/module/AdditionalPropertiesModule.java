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

package com.github.victools.jsonschema.generator.impl.module;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Default module being included if {@code Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT} is enabled.
 */
public class AdditionalPropertiesModule implements Module {

    /**
     * Create module instance that specifically allows additional properties on {@link Map} instances.
     *
     * @return module instance
     */
    public static AdditionalPropertiesModule forMapValues() {
        ConfigFunction<TypeScope, Type> generalResolver = scope -> {
            if (scope.getType().isInstanceOf(Map.class)) {
                // within a Map<Key, Value> allow additionalProperties of the Value type
                // if no type parameters are defined, this will result in additionalProperties to be omitted (by way of returning Object.class)
                return scope.getTypeParameterFor(Map.class, 1);
            }
            return null;
        };
        return new AdditionalPropertiesModule(generalResolver,
                AdditionalPropertiesModule::createDefinitionForMemberMap,
                AdditionalPropertiesModule::createDefinitionForMemberMap);
    }

    private static JsonNode createDefinitionForMemberMap(MemberScope<?, ?> member, SchemaGenerationContext context) {
        if (!member.getType().isInstanceOf(Map.class)) {
            return null;
        }
        // within a Map<Key, Value> allow additionalProperties of the Value type
        // if no type parameters are defined, this will result in additionalProperties to be omitted (by way of returning Object.class)
        ResolvedType valueType = member.getTypeParameterFor(Map.class, 1);
        if (valueType == null || valueType.getErasedType() == Object.class) {
            return null;
        }
        return member.getContext().performActionOnMember(member.asFakeContainerItemScope(Map.class, 1),
                field -> context.createStandardDefinitionReference(field, null),
                method -> context.createStandardDefinitionReference(method, null));
    }

    /**
     * Create module instance that forbids additional properties everywhere but on container types.
     * <br>
     * This assumes that the respective {@link SimpleTypeModule} instance is being applied first and already enforces the "additionalProperties"
     * keyword to be omitted on other non-object schemas.
     *
     * @return module instance
     */
    public static AdditionalPropertiesModule forbiddenForAllObjectsButContainers() {
        return new AdditionalPropertiesModule(scope -> scope.isContainerType() ? null : Void.class);
    }

    private final ConfigFunction<TypeScope, Type> generalAdditionalPropertiesResolver;
    private final BiFunction<FieldScope, SchemaGenerationContext, JsonNode> fieldAdditionalPropertiesResolver;
    private final BiFunction<MethodScope, SchemaGenerationContext, JsonNode> methodAdditionalPropertiesResolver;

    /**
     * Constructor.
     *
     * @param generalAdditionalPropertiesResolver resolver for additionalProperties of types in general
     */
    public AdditionalPropertiesModule(ConfigFunction<TypeScope, Type> generalAdditionalPropertiesResolver) {
        this(generalAdditionalPropertiesResolver, null, null);
    }

    /**
     * Constructor.
     *
     * @param generalAdditionalPropertiesResolver resolver for additionalProperties of types in general
     * @param fieldAdditionalPropertiesResolver resolver for additionalProperties for fields
     * @param methodAdditionalPropertiesResolver resolver for additionalProperties for methods
     */
    public AdditionalPropertiesModule(ConfigFunction<TypeScope, Type> generalAdditionalPropertiesResolver,
            BiFunction<FieldScope, SchemaGenerationContext, JsonNode> fieldAdditionalPropertiesResolver,
            BiFunction<MethodScope, SchemaGenerationContext, JsonNode> methodAdditionalPropertiesResolver) {
        this.generalAdditionalPropertiesResolver = generalAdditionalPropertiesResolver;
        this.fieldAdditionalPropertiesResolver = fieldAdditionalPropertiesResolver;
        this.methodAdditionalPropertiesResolver = methodAdditionalPropertiesResolver;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        if (this.generalAdditionalPropertiesResolver != null) {
            builder.forTypesInGeneral()
                    .withAdditionalPropertiesResolver(this.generalAdditionalPropertiesResolver);
        }
        if (this.fieldAdditionalPropertiesResolver != null) {
            builder.forFields()
                    .withAdditionalPropertiesResolver(this.fieldAdditionalPropertiesResolver);
        }
        if (this.methodAdditionalPropertiesResolver != null) {
            builder.forMethods()
                    .withAdditionalPropertiesResolver(this.methodAdditionalPropertiesResolver);
        }
    }
}
