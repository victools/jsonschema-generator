/*
 * Copyright 2025 VicTools.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Iterator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/discussions/525">#525</a>.
 * <br/>
 * Demonstrating the schema generation for a Java type documenting its methods.
 */
public class OpenAiFunctionCallingSchemaExample implements SchemaGenerationExampleInterface, CustomPropertyDefinitionProvider<FieldScope> {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.with(Option.INLINE_ALL_SCHEMAS);
        configBuilder.with(new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED));
        configBuilder.forFields().withCustomDefinitionProvider(this);
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        Iterator<JsonNode> functions = generator.generateSchema(Functions.class)
                .get(config.getKeyword(SchemaKeyword.TAG_PROPERTIES))
                .iterator();
        ObjectNode result = config.createObjectNode();
        ArrayNode tools = result.putArray("tools");
        while (functions.hasNext()) {
            ObjectNode singleFunction = (ObjectNode) functions.next();
            // override standard type to unsupported "function" in this case
            singleFunction.put(config.getKeyword(SchemaKeyword.TAG_TYPE), "function");
            // rename "items" to "parameters"
            singleFunction.set("parameters", singleFunction.remove(config.getKeyword(SchemaKeyword.TAG_ITEMS)));
            tools.add(singleFunction);
        }
        return result;
    }

    @Override
    public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope scope, SchemaGenerationContext context) {
        FunctionParameter annotation = scope.getAnnotation(FunctionParameter.class);
        if (annotation == null) {
            return null;
        }
        ObjectNode node = context.getGeneratorConfig().createObjectNode();
        // don't set the unsupported type "function" immediately, in order to still use standard clean-up features
        node.put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY));
        node.put("name", annotation.name());
        if (!annotation.description().isEmpty()) {
            node.put(context.getKeyword(SchemaKeyword.TAG_DESCRIPTION), annotation.description());
        }
        node.set(context.getKeyword(SchemaKeyword.TAG_ITEMS), context.createStandardDefinitionReference(scope, this));
        return new CustomPropertyDefinition(node, CustomDefinition.AttributeInclusion.NO);
    }

    static class Functions {

        @FunctionParameter(name = "get_humidity", description = "Get current humidity for a given location.")
        public LocationDetails getHumidity;

        @FunctionParameter(name = "get_temperature", description = "Get current temperature for a given location.")
        public LocationDetails getTemperature;
    }

    static class LocationDetails {
        @JsonProperty(required = true)
        @JsonPropertyDescription("City and country e.g. Bogotá, Colombia")
        String location;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface FunctionParameter {
        String name();
        String description() default "";
    }
}
