/*
 * Copyright 2020 VicTools.
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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generation context for sub-schema definitions.
 */
public interface SchemaGenerationContext {

    /**
     * Getter for the applicable configuration.
     *
     * @return configuration defined for this context
     */
    SchemaGeneratorConfig getGeneratorConfig();

    /**
     * Getter for the type resolution/introspection context in use.
     *
     * @return type resolution/introspection context
     */
    TypeContext getTypeContext();

    /**
     * Create an inline definition for the given targetType. Also respecting any custom definition for the given targetType.
     *
     * @param targetType type to create definition (reference) node for
     * @return designated definition (reference) node for the targetType
     *
     * @see #createStandardDefinition(ResolvedType, CustomDefinitionProviderV2)
     */
    ObjectNode createDefinition(ResolvedType targetType);

    /**
     * Create an inline definition for the given targetType. Ignoring custom definitions up to the given one, but respecting others.
     *
     * @param targetType type to create definition (reference) node for
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return designated definition (reference) node for the targetType
     *
     * @see #createDefinition(ResolvedType)
     */
    ObjectNode createStandardDefinition(ResolvedType targetType, CustomDefinitionProviderV2 ignoredDefinitionProvider);

    /**
     * Ensure that the JSON schema represented by the given node allows for it to be of "type" "null".
     *
     * @param node representation of a JSON schema (part) that should allow a value of "type" "null"
     * @return reference to the given parameter node
     */
    ObjectNode makeNullable(ObjectNode node);
}
