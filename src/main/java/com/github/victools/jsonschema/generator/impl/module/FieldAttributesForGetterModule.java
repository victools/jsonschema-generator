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

import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import com.github.victools.jsonschema.generator.impl.ReflectionGetterUtils;
import java.util.Optional;

/**
 * Default module being included if {@code Option.FIELD_ATTRIBUTES_FOR_GETTERS} is enabled.
 */
public class FieldAttributesForGetterModule implements Module {

    /**
     * Create a method's resolver that applies the given field resolver method on the associated field (if there is one).
     *
     * @param <D> expected second parameter
     * @param <R> expected attribute value being resolved
     * @param resolver resolver for a field to be applied on a getter method's associated field (if there is one)
     * @return generated resolver for a getter method
     */
    private static <D, R> ConfigFunction<ResolvedMethod, D, R> resolveForField(ConfigFunction<ResolvedField, D, R> resolver) {
        return (method, defaultValue, declaringType) -> Optional.ofNullable(ReflectionGetterUtils.findFieldForGetter(method, declaringType))
                .map(field -> resolver.apply(field, defaultValue, declaringType))
                .orElse(null);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<ResolvedField> fieldConfigPart = builder.forFields();
        builder.forMethods()
                .withArrayMaxItemsResolver(resolveForField(fieldConfigPart::resolveArrayMaxItems))
                .withArrayMinItemsResolver(resolveForField(fieldConfigPart::resolveArrayMinItems))
                .withArrayUniqueItemsResolver(resolveForField(fieldConfigPart::resolveArrayUniqueItems))
                .withDescriptionResolver(resolveForField(fieldConfigPart::resolveDescription))
                .withEnumResolver(resolveForField(fieldConfigPart::resolveEnum))
                .withNullableCheck(resolveForField(fieldConfigPart::isNullable))
                .withNumberExclusiveMaximumResolver(resolveForField(fieldConfigPart::resolveNumberExclusiveMaximum))
                .withNumberExclusiveMinimumResolver(resolveForField(fieldConfigPart::resolveNumberExclusiveMinimum))
                .withNumberInclusiveMaximumResolver(resolveForField(fieldConfigPart::resolveNumberInclusiveMaximum))
                .withNumberInclusiveMinimumResolver(resolveForField(fieldConfigPart::resolveNumberInclusiveMinimum))
                .withNumberMultipleOfResolver(resolveForField(fieldConfigPart::resolveNumberMultipleOf))
                .withStringFormatResolver(resolveForField(fieldConfigPart::resolveStringFormat))
                .withStringMaxLengthResolver(resolveForField(fieldConfigPart::resolveStringMaxLength))
                .withStringMinLengthResolver(resolveForField(fieldConfigPart::resolveStringMinLength))
                .withTargetTypeOverrideResolver(resolveForField(fieldConfigPart::resolveTargetTypeOverride))
                .withTitleResolver(resolveForField(fieldConfigPart::resolveTitle));
    }
}
