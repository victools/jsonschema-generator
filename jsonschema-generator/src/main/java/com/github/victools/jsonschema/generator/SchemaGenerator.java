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

package com.github.victools.jsonschema.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.lang.reflect.Type;

/**
 * Generator for JSON Schema definitions via reflection based analysis of a given class.
 */
public class SchemaGenerator {

    private final SchemaGeneratorConfig config;
    private final TypeContext typeContext;

    /**
     * Constructor.
     *
     * @param config configuration to be applied
     */
    public SchemaGenerator(SchemaGeneratorConfig config) {
        this(config, TypeContextFactory.createDefaultTypeContext(config));
    }

    /**
     * Constructor.
     *
     * @param config configuration to be applied
     * @param context type resolution/introspection context to be used during schema generations (across multiple schema generations)
     */
    public SchemaGenerator(SchemaGeneratorConfig config, TypeContext context) {
        this.config = config;
        this.typeContext = context;
    }

    /**
     * Generate a {@link JsonNode} containing the JSON Schema representation of the given type.
     *
     * @param mainTargetType type for which to generate the JSON Schema
     * @param typeParameters optional type parameters (in case of the {@code mainTargetType} being a parameterised type)
     * @return generated JSON Schema
     */
    public ObjectNode generateSchema(Type mainTargetType, Type... typeParameters) {
        return SchemaBuilder.createSingleTypeSchema(this.config, this.typeContext, mainTargetType, typeParameters);
    }

    /**
     * Create a {@link SchemaBuilder} instance for collecting schema references via
     * {@link SchemaBuilder#createSchemaReference(Type, Type...) createSchemaReference()} until finalizing the generation via
     * {@link SchemaBuilder#collectDefinitions(String) collectDefinitions()}.
     * <br>
     * This is intended when you want to create a document that includes JSON Schema definitions but is not a JSON Schema itself, e.g. creating model
     * descriptions for an OpenAPI definition.
     *
     * @return {@link SchemaBuilder} instance
     * @see SchemaBuilder#createSchemaReference(Type, Type...) : adding a single type to the builder instance
     * @see SchemaBuilder#collectDefinitions(String) : generate an {@link ObjectNode} listing the common schema definitions
     */
    public SchemaBuilder buildMultipleSchemaDefinitions() {
        return SchemaBuilder.forMultipleTypes(this.config, this.typeContext);
    }
}
