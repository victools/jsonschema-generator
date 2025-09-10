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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.GeneratedSchema;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/issues/319">#319</a>.
 * <br/>
 * Based on Swagger2 {@code @Schema} annotations, allowing to reference "external" files.
 */
public class ExternalRefAnnotationExample implements SchemaGenerationExampleInterface {

    @Override
    public GeneratedSchema[] generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON);
        configBuilder.with(new Swagger2Module());
        // add "$schema" property in Example.class' schema
        configBuilder.forTypesInGeneral().withTypeAttributeOverride((schema, scope, context) -> {
            if (scope.getType().getErasedType() == Example.class) {
                ((ObjectNode) schema.putIfAbsent(context.getKeyword(SchemaKeyword.TAG_PROPERTIES), schema.objectNode()))
                        .set(context.getKeyword(SchemaKeyword.TAG_SCHEMA),
                                context.createStandardDefinition(context.getTypeContext().resolve(String.class), null));
            }
        });
        // add external "$ref", if one is referenced by @Schema(ref = "...")
        configBuilder.forTypesInGeneral().withCustomDefinitionProvider(new SchemaRefDefinitionProvider());
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(Example.class);
    }

    private static final class SchemaRefDefinitionProvider implements CustomDefinitionProviderV2 {

        private Class<?> mainType;

        @Override
        public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
            Class<?> erasedType = javaType.getErasedType();
            if (this.mainType == null) {
                this.mainType = erasedType;
            }
            if (this.mainType == erasedType) {
                // avoid external ref to the "main" schema being generated
                return null;
            }
            // look-up external reference from @Schema(ref = "...")
            return Optional.ofNullable(erasedType.getAnnotation(Schema.class))
                    .map(Schema::ref)
                    .filter(ref -> !ref.isEmpty())
                    .map(ref -> context.getGeneratorConfig().createObjectNode()
                            .put(context.getKeyword(SchemaKeyword.TAG_REF), ref))
                    .map(schema -> new CustomDefinition(schema,
                            CustomDefinition.INLINE_DEFINITION,
                            CustomDefinition.INCLUDING_ATTRIBUTES))
                    .orElse(null);
        }

        @Override
        public void resetAfterSchemaGenerationFinished() {
            this.mainType = null;
        }
    }

    static class Example {
        @Schema(description = "alpha")
        private String alpha;
        @Schema(ref = "./BetaSchema.json")
        private Object beta;
        @Schema(description = "sigma")
        private Double sigma;
        @ArraySchema(schema = @Schema(oneOf = {Theta.class, Tau.class}))
        private List<Omega> omega;
    }

    static class Omega {
    }

    @Schema(
            title = "Theta",
            description = "Theta",
            additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
            ref = "./ThetaSchema.json")
    static class Theta extends Omega {
    }

    // Implementation omitted for brevity
    @Schema(
            title = "Tau",
            description = "Tau",
            additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
            ref = "./TauSchema.json")
    static class Tau extends Omega {
    }
}
