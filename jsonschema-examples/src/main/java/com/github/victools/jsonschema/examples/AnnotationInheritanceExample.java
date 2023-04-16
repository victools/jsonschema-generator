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

import com.fasterxml.classmate.AnnotationInclusion;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Inherited;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/discussions/333">#333</a>.
 * <br/>
 * Ensuring "type" inclusion on "allOf" parts holding collected attributes, e.g., "additionalProperties".
 */
public class AnnotationInheritanceExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(new JakartaValidationModule());
        SchemaGeneratorConfig config = configBuilder.build();
        // the default AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED requires @Inherited on annotations being looked up
        // using AnnotationInclusion.INCLUDE_AND_INHERIT instead considers all annotations on overridden methods as well
        SchemaGenerator generator = new SchemaGenerator(config,
                TypeContextFactory.createTypeContext(AnnotationInclusion.INCLUDE_AND_INHERIT, config));
        return generator.generateSchema(Book.class);
    }

    static class Book implements Publication {
        private String title;

        @Override
        public String getTitle() {
            return this.title;
        }
    }

    interface Publication {
        @NotEmpty
        String getTitle();
    }
}
