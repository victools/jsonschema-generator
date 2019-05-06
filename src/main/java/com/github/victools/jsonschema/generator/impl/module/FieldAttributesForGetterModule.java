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

import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.impl.ReflectionGetterUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Default module being included if {@code Option.INCLUDE_FIELD_ATTRIBUTES_FOR_GETTERS} is enabled.
 */
public class FieldAttributesForGetterModule implements Module {

    /**
     * Create a method's resolver that applies the given field resolver method on the associated field (if there is one).
     *
     * @param <R> expected attribute value being resolved
     * @param resolver resolver for a field to be applied on a getter method's associated field (if there is one)
     * @return generated resolver for a getter method
     */
    private static <R> Function<Method, R> resolveForField(Function<Field, R> resolver) {
        return method -> Optional.ofNullable(ReflectionGetterUtils.findFieldForGetter(method))
                .map(resolver)
                .orElse(null);
    }

    /**
     * Create a method's resolver that applies the given field resolver method on the associated field (if there is one).
     *
     * @param <D> expected default value type
     * @param <R> expected attribute value being resolved
     * @param resolver resolver for a field to be applied on a getter method's associated field (if there is one)
     * @return generated resolver for a getter method
     */
    private static <D, R> BiFunction<Method, D, R> resolveForFieldWithDefault(BiFunction<Field, D, R> resolver) {
        return (method, defaultValue) -> Optional.ofNullable(ReflectionGetterUtils.findFieldForGetter(method))
                .map(field -> resolver.apply(field, defaultValue))
                .orElse(null);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<Field> fieldConfigPart = builder.forFields();
        builder.forMethods()
                .addArrayMaxItemsResolver(resolveForField(fieldConfigPart::resolveArrayMaxItems))
                .addArrayMinItemsResolver(resolveForField(fieldConfigPart::resolveArrayMinItems))
                .addArrayUniqueItemsResolver(resolveForField(fieldConfigPart::resolveArrayUniqueItems))
                .addDescriptionResolver(resolveForField(fieldConfigPart::resolveDescription))
                .addEnumResolver(resolveForField(fieldConfigPart::resolveEnum))
                .addNullableCheck(resolveForField(fieldConfigPart::isNullable))
                .addNumberExclusiveMaximumResolver(resolveForField(fieldConfigPart::resolveNumberExclusiveMaximum))
                .addNumberExclusiveMinimumResolver(resolveForField(fieldConfigPart::resolveNumberExclusiveMinimum))
                .addNumberInclusiveMaximumResolver(resolveForField(fieldConfigPart::resolveNumberInclusiveMaximum))
                .addNumberInclusiveMinimumResolver(resolveForField(fieldConfigPart::resolveNumberInclusiveMinimum))
                .addNumberMultipleOfResolver(resolveForField(fieldConfigPart::resolveNumberMultipleOf))
                .addStringFormatResolver(resolveForField(fieldConfigPart::resolveStringFormat))
                .addStringMaxLengthResolver(resolveForField(fieldConfigPart::resolveStringMaxLength))
                .addStringMinLengthResolver(resolveForField(fieldConfigPart::resolveStringMinLength))
                .addTargetTypeOverrideResolver(resolveForFieldWithDefault(fieldConfigPart::resolveTargetTypeOverride))
                .addTitleResolver(resolveForField(fieldConfigPart::resolveTitle));
    }
}
