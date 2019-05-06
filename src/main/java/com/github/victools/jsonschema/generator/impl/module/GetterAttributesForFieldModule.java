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
 * Default module being included if {@code Option.INCLUDE_GETTER_ATTRIBUTES_FOR_FIELDS} is enabled.
 */
public class GetterAttributesForFieldModule implements Module {

    /**
     * Create a field's resolver method that applies the given method resolver on the field's getter (if there is one).
     *
     * @param <R> expected attribute value being resolved
     * @param resolver resolver for a method to be applied on a field's getter (if there is one)
     * @return generated resolver for a field
     */
    private static <R> Function<Field, R> resolveForGetterMethod(Function<Method, R> resolver) {
        return field -> Optional.ofNullable(ReflectionGetterUtils.findGetterForField(field))
                .map(resolver)
                .orElse(null);
    }

    /**
     * Create a field's resolver method that applies the given method resolver on the field's getter (if there is one).
     *
     * @param <D> expected default value type
     * @param <R> expected attribute value being resolved
     * @param resolver resolver for a method to be applied on a field's getter (if there is one)
     * @return generated resolver for a field
     */
    private static <D, R> BiFunction<Field, D, R> resolveForGetterMethodWithDefault(BiFunction<Method, D, R> resolver) {
        return (field, defaultValue) -> Optional.ofNullable(ReflectionGetterUtils.findGetterForField(field))
                .map(method -> resolver.apply(method, defaultValue))
                .orElse(null);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<Method> methodConfigPart = builder.forMethods();
        builder.forFields()
                .addArrayMaxItemsResolver(resolveForGetterMethod(methodConfigPart::resolveArrayMaxItems))
                .addArrayMinItemsResolver(resolveForGetterMethod(methodConfigPart::resolveArrayMinItems))
                .addArrayUniqueItemsResolver(resolveForGetterMethod(methodConfigPart::resolveArrayUniqueItems))
                .addDescriptionResolver(resolveForGetterMethod(methodConfigPart::resolveDescription))
                .addEnumResolver(resolveForGetterMethod(methodConfigPart::resolveEnum))
                .addNullableCheck(resolveForGetterMethod(methodConfigPart::isNullable))
                .addNumberExclusiveMaximumResolver(resolveForGetterMethod(methodConfigPart::resolveNumberExclusiveMaximum))
                .addNumberExclusiveMinimumResolver(resolveForGetterMethod(methodConfigPart::resolveNumberExclusiveMinimum))
                .addNumberInclusiveMaximumResolver(resolveForGetterMethod(methodConfigPart::resolveNumberInclusiveMaximum))
                .addNumberInclusiveMinimumResolver(resolveForGetterMethod(methodConfigPart::resolveNumberInclusiveMinimum))
                .addNumberMultipleOfResolver(resolveForGetterMethod(methodConfigPart::resolveNumberMultipleOf))
                .addStringFormatResolver(resolveForGetterMethod(methodConfigPart::resolveStringFormat))
                .addStringMaxLengthResolver(resolveForGetterMethod(methodConfigPart::resolveStringMaxLength))
                .addStringMinLengthResolver(resolveForGetterMethod(methodConfigPart::resolveStringMinLength))
                .addTargetTypeOverrideResolver(resolveForGetterMethodWithDefault(methodConfigPart::resolveTargetTypeOverride))
                .addTitleResolver(resolveForGetterMethod(methodConfigPart::resolveTitle));
    }
}
