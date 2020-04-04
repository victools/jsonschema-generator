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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.impl.AttributeCollector;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the {@link CustomDefinitionProviderV2} interface for treating enum types as plain strings based on a {@link JsonValue} annotation
 * being present with {@code value = true} on exactly one argument-free method and/or {@link JsonProperty} annotations being present on all enum
 * constants. If no such annotations exist, no custom definition will be returned; thereby falling back on whatever is defined in a following custom
 * definition (e.g. from one of the standard generator {@code Option}s).
 */
public class CustomEnumDefinitionProvider implements CustomDefinitionProviderV2 {

    private final boolean checkForJsonValueAnnotatedMethod;
    private final boolean checkForJsonPropertyAnnotations;

    /**
     * Constructor indicating how to attempt to serialise enum constant values. If both flags are provided as {@code true}, the {@link JsonValue}
     * annotated method will take precedence over {@link JsonProperty} annotations.
     *
     * @param checkForJsonValueAnnotatedMethod whether a single {@link JsonValue} annotated method should be invoked on each enum constant value
     * @param checkForJsonPropertyAnnotations whether each enum constant's {@link JsonProperty} annotation's {@code value} should be used
     */
    public CustomEnumDefinitionProvider(boolean checkForJsonValueAnnotatedMethod, boolean checkForJsonPropertyAnnotations) {
        this.checkForJsonValueAnnotatedMethod = checkForJsonValueAnnotatedMethod;
        this.checkForJsonPropertyAnnotations = checkForJsonPropertyAnnotations;
    }

    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
        Object[] enumConstants = javaType.getErasedType().getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            return null;
        }
        List<?> serializedJsonValues = null;
        if (this.checkForJsonValueAnnotatedMethod) {
            serializedJsonValues = this.getSerializedValuesFromJsonValue(javaType, enumConstants, context);
        }
        if (serializedJsonValues == null && this.checkForJsonPropertyAnnotations) {
            serializedJsonValues = this.getSerializedValuesFromJsonProperty(javaType, enumConstants);
        }
        if (serializedJsonValues == null) {
            return null;
        }

        ObjectNode customNode = context.getGeneratorConfig().createObjectNode()
                .put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_STRING));
        AttributeCollector standardAttributeCollector = new AttributeCollector(context.getGeneratorConfig().getObjectMapper());
        standardAttributeCollector.setEnum(customNode, serializedJsonValues, context);
        return new CustomDefinition(customNode);
    }

    /**
     * Check whether the given type is an enum with at least one constant value and a single {@link JsonValue} annotated method with
     * {@code value = true} and no expected arguments.
     *
     * @param javaType encountered type during schema generation
     * @param enumConstants non-empty array of enum constants
     * @param context current generation context
     * @return results from invoking the {@link JsonValue} annotated method for each enum constant (or {@code null} if the criteria are not met)
     */
    protected List<Object> getSerializedValuesFromJsonValue(ResolvedType javaType, Object[] enumConstants, SchemaGenerationContext context) {
        ResolvedMethod jsonValueAnnotatedEnumMethod = this.getJsonValueAnnotatedMethod(javaType, context);
        if (jsonValueAnnotatedEnumMethod == null) {
            return null;
        }
        try {
            List<Object> serializedJsonValues = new ArrayList<>(enumConstants.length);
            for (Object enumConstant : enumConstants) {
                serializedJsonValues.add(jsonValueAnnotatedEnumMethod.getRawMember().invoke(enumConstant));
            }
            return serializedJsonValues;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return null;
        }
    }

    /**
     * Look-up the single {@link JsonValue} annotated method with {@code value = true} and no expected arguments.
     *
     * @param javaType targeted type to look-up serialization method for
     * @param context generation context providing access to type resolution context
     * @return single method with {@link JsonValue} annotation
     */
    protected ResolvedMethod getJsonValueAnnotatedMethod(ResolvedType javaType, SchemaGenerationContext context) {
        ResolvedMethod[] memberMethods = context.getTypeContext().resolveWithMembers(javaType).getMemberMethods();
        Set<ResolvedMethod> jsonValueAnnotatedMethods = Stream.of(memberMethods)
                .filter(method -> method.getArgumentCount() == 0)
                .filter(method -> Optional.ofNullable(method.getAnnotations().get(JsonValue.class)).map(JsonValue::value).orElse(false))
                .collect(Collectors.toSet());
        if (jsonValueAnnotatedMethods.size() == 1) {
            return jsonValueAnnotatedMethods.iterator().next();
        }
        return null;
    }

    /**
     * Check whether the given type is an enum with at least one constant value and each enum constant value has a {@link JsonProperty} annotation.
     *
     * @param javaType encountered type during schema generation
     * @param enumConstants non-empty array of enum constants
     * @return annotated {@link JsonProperty#value()} for each enum constant (or {@code null} if the criteria are not met)
     */
    protected List<String> getSerializedValuesFromJsonProperty(ResolvedType javaType, Object[] enumConstants) {
        try {
            List<String> serializedJsonValues = new ArrayList<>(enumConstants.length);
            for (Object enumConstant : enumConstants) {
                String enumValueName = ((Enum<?>) enumConstant).name();
                JsonProperty annotation = javaType.getErasedType()
                        .getDeclaredField(enumValueName)
                        .getAnnotation(JsonProperty.class);
                if (annotation == null) {
                    // enum constant without @JsonProperty annotation
                    return null;
                }
                serializedJsonValues.add(JsonProperty.USE_DEFAULT_NAME.equals(annotation.value()) ? enumValueName : annotation.value());
            }
            return serializedJsonValues;
        } catch (NoSuchFieldException | SecurityException ex) {
            return null;
        }
    }
}
