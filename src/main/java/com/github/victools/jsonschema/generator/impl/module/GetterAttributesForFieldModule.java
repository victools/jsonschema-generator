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

/**
 * Default module being included if {@code Option.GETTER_ATTRIBUTES_FOR_FIELDS} is enabled.
 */
public class GetterAttributesForFieldModule implements Module {

    /**
     * Create a field's resolver method that applies the given method resolver on the field's getter (if there is one).
     *
     * @param <D> expected second parameter
     * @param <R> expected attribute value being resolved
     * @param resolver resolver for a method to be applied on a field's getter (if there is one)
     * @return generated resolver for a field
     */
    private static <D, R> BiFunction<Field, D, R> resolveForGetter(BiFunction<Method, D, R> resolver) {
        return (field, defaultValue) -> Optional.ofNullable(ReflectionGetterUtils.findGetterForField(field))
                .map(method -> resolver.apply(method, defaultValue))
                .orElse(null);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<Method> methodConfigPart = builder.forMethods();
        builder.forFields()
                .withArrayMaxItemsResolver(resolveForGetter(methodConfigPart::resolveArrayMaxItems))
                .withArrayMinItemsResolver(resolveForGetter(methodConfigPart::resolveArrayMinItems))
                .withArrayUniqueItemsResolver(resolveForGetter(methodConfigPart::resolveArrayUniqueItems))
                .withDescriptionResolver(resolveForGetter(methodConfigPart::resolveDescription))
                .withEnumResolver(resolveForGetter(methodConfigPart::resolveEnum))
                .withNullableCheck(resolveForGetter(methodConfigPart::isNullable))
                .withNumberExclusiveMaximumResolver(resolveForGetter(methodConfigPart::resolveNumberExclusiveMaximum))
                .withNumberExclusiveMinimumResolver(resolveForGetter(methodConfigPart::resolveNumberExclusiveMinimum))
                .withNumberInclusiveMaximumResolver(resolveForGetter(methodConfigPart::resolveNumberInclusiveMaximum))
                .withNumberInclusiveMinimumResolver(resolveForGetter(methodConfigPart::resolveNumberInclusiveMinimum))
                .withNumberMultipleOfResolver(resolveForGetter(methodConfigPart::resolveNumberMultipleOf))
                .withStringFormatResolver(resolveForGetter(methodConfigPart::resolveStringFormat))
                .withStringMaxLengthResolver(resolveForGetter(methodConfigPart::resolveStringMaxLength))
                .withStringMinLengthResolver(resolveForGetter(methodConfigPart::resolveStringMinLength))
                .withTargetTypeOverrideResolver(resolveForGetter(methodConfigPart::resolveTargetTypeOverride))
                .withTitleResolver(resolveForGetter(methodConfigPart::resolveTitle));
    }
}
