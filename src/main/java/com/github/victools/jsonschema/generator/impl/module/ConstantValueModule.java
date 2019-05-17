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

import com.github.victools.jsonschema.generator.JavaType;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

/**
 * Default module being included if {@code Option.VALUES_FROM_CONSTANT_FIELDS} is enabled.
 */
public class ConstantValueModule implements Module {

    /**
     * Look-up the given field's constant (i.e. final static) value, or return null.
     *
     * @param field targeted field
     * @param fieldType targeted field's type (ignored here)
     * @return collection containing single constant value; returning null if field has no constant value
     */
    private static List<?> extractConstantFieldValue(Field field, JavaType fieldType) {
        int staticAndFinal = Modifier.STATIC | Modifier.FINAL;
        if ((field.getModifiers() & staticAndFinal) == staticAndFinal && !field.isEnumConstant()) {
            field.setAccessible(true);
            try {
                return Collections.singletonList(field.get(null));
            } catch (IllegalAccessException ex) {
                // exception should never be thrown (due to calling setAccessible(true) before)
                throw new RuntimeException(ex);
            }
        }
        return null;
    }

    /**
     * Determine whether the given field is a constant (i.e. static and final) and if so, indicant whether the constant value is null.
     *
     * @param field targeted field
     * @param fieldType targeted field's type (ignored here)
     * @return true/false depending on constant value being null; otherwise returning null if field is not a constant
     */
    private static Boolean isNullableConstantField(Field field, JavaType fieldType) {
        List<?> constantValues = extractConstantFieldValue(field, fieldType);
        if (constantValues == null) {
            return null;
        }
        return constantValues.get(0) == null;
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields()
                .withEnumResolver(ConstantValueModule::extractConstantFieldValue)
                .withNullableCheck(ConstantValueModule::isNullableConstantField);
    }
}
