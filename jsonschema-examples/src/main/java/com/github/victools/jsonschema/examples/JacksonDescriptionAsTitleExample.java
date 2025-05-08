/*
 * Copyright 2023 VicTools.
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

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeScope;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import java.util.UUID;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/issues/343">#343</a>.
 * <br/>
 * Including the "description" values detected in jackson annotation as "title" instead.
 */
public class JacksonDescriptionAsTitleExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new JacksonTitleModule())
                .with(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS, Option.NONSTATIC_NONVOID_NONGETTER_METHODS);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(Example.class);
    }

    @JsonClassDescription("Class Title")
    static class Example {
        @JsonPropertyDescription("Field Title")
        public String field;

        @JsonPropertyDescription("Method Title")
        public UUID getId() {
            return UUID.randomUUID();
        }
    }

    /**
     * Override of the standard JacksonModule, that assigns all description values to the title keyword instead.
     */
    static class JacksonTitleModule extends JacksonModule {
        @Override
        public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
            super.applyToConfigBuilder(builder);

            builder.forTypesInGeneral().withTitleResolver(super::resolveDescriptionForType);
            builder.forFields().withTitleResolver(super::resolveDescription);
            builder.forMethods().withTitleResolver(super::resolveDescription);
        }

        @Override
        protected String resolveDescription(MemberScope<?, ?> member) {
            // skip description look-up, to avoid duplicating the title
            return null;
        }

        @Override
        protected String resolveDescriptionForType(TypeScope scope) {
            // skip description look-up, to avoid duplicating the title
            return null;
        }
    }
}
