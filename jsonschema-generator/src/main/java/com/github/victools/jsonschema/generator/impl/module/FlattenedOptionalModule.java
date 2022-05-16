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

import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.util.Optional;

/**
 * Default module being included if {@code Option.FLATTENED_OPTIONALS} is enabled.
 */
public class FlattenedOptionalModule extends FlattenedWrapperModule<Optional> {

    /**
     * Constructor declaring {@link Optional} as the target type to unwrap.
     */
    public FlattenedOptionalModule() {
        super(Optional.class);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        super.applyToConfigBuilder(builder);
        builder.forFields()
                // need to getDeclaredType() to avoid the target-type-override applied by generic wrapper module
                .withNullableCheck(field -> this.hasMemberWrapperType(field) ? Boolean.TRUE : null);
        builder.forMethods()
                // need to getDeclaredType() to avoid the target-type-override applied by generic wrapper module
                .withNullableCheck(method -> this.hasMemberWrapperType(method) ? Boolean.TRUE : null);
    }
}
