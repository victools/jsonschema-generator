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
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

/**
 * Default module being included if {@code Option.INCLUDE_FIXED_SIMPLE_TYPES} is enabled.
 */
public class SimpleTypeModule implements Module {

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields().addNullableCheck(field -> field.getType().isPrimitive() ? Boolean.FALSE : null);
        builder.forMethods().addNullableCheck(method -> method.getReturnType().isPrimitive() ? Boolean.FALSE : null);

        builder.withFixedTypeMapping(SchemaConstants.TAG_TYPE_STRING,
                String.class, Character.class, char.class, CharSequence.class);

        builder.withFixedTypeMapping(SchemaConstants.TAG_TYPE_BOOLEAN,
                Boolean.class, boolean.class);

        builder.withFixedTypeMapping(SchemaConstants.TAG_TYPE_INTEGER,
                Integer.class, int.class, Short.class, short.class, Long.class, long.class);

        builder.withFixedTypeMapping(SchemaConstants.TAG_TYPE_NUMBER,
                Double.class, double.class, Float.class, float.class, Byte.class, byte.class);

        builder.withFixedTypeMapping(SchemaConstants.TAG_TYPE_OBJECT,
                Object.class);
    }
}
