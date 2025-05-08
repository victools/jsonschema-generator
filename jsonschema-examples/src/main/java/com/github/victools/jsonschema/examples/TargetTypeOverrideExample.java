/*
 * Copyright 2024 VicTools.
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

package com.github.victools.jsonschema.examples;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.List;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/discussions/451">#451</a>.
 * <br/>
 * Demonstrating target type overrides to represent a {@code byte[]} as {@code string} with particular content encoding.
 */
public class TargetTypeOverrideExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.with(new JakartaValidationModule());
        configBuilder.forFields()
                .withTargetTypeOverridesResolver(this::treatByteArrayAsString)
                .withInstanceAttributeOverride(this::addContentEncodingForByteArray);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(Example.class);
    }

    private List<ResolvedType> treatByteArrayAsString(MemberScope<?, ?> scope) {
        ResolvedType type = scope.getType();
        if (type != null && type.getErasedType().equals(byte[].class)) {
            return Collections.singletonList(scope.getContext().resolve(String.class));
        }
        return null;
    }

    private void addContentEncodingForByteArray(ObjectNode schema, MemberScope<?, ?> scope, SchemaGenerationContext context) {
        ResolvedType declaredType = scope.getDeclaredType();
        if (declaredType != null && declaredType.getErasedType().equals(byte[].class)) {
            schema.put("contentEncoding", "base64");
        }
    }

    static class Example {
        @NotNull
        @Size(min = 1, max = 100)
        public String someText;
        @NotNull
        @Size(min = 1, max = 10485760)
        public byte[] byteArray;
    }
}
