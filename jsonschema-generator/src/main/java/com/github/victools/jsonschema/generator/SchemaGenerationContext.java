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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
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
     * Look-up a given keyword's associated tag name or value for the designated JSON Schema version.
     *
     * @param keyword reference to a tag name or value
     * @return specific tag name or value in the designated JSON Schema version
     * @see SchemaGeneratorConfig#getKeyword(SchemaKeyword)
     */
    String getKeyword(SchemaKeyword keyword);

    /**
     * Getter for the type resolution/introspection context in use.
     *
     * @return type resolution/introspection context
     */
    TypeContext getTypeContext();

    /**
     * Create an inline definition for the given targetType. Also respecting any custom definition for the given targetType.
     * <br>
     * This is equivalent to calling {@code createStandardDefinition(targetType, null)}
     *
     * @param targetType type to create definition node for
     * @return designated definition node for the targetType
     *
     * @see #createDefinitionReference(ResolvedType)
     * @see #createStandardDefinition(ResolvedType, CustomDefinitionProviderV2)
     */
    ObjectNode createDefinition(ResolvedType targetType);

    /**
     * Create a definition for the given targetType. Also respecting any custom definition for the given targetType.
     * <br>
     * The returned node will be empty, but is being remembered internally and populated later, i.e. it should not be changed!
     * <br>
     * This is equivalent to calling {@code createStandardDefinitionReference(targetType, null)}
     *
     * @param targetType type to create definition (reference) node for
     * @return (temporarily) empty reference node for the targetType that will only be populated at the very end of the schema generation
     *
     * @see #createDefinition(ResolvedType)
     * @see #createStandardDefinitionReference(ResolvedType, CustomDefinitionProviderV2)
     */
    ObjectNode createDefinitionReference(ResolvedType targetType);

    /**
     * Create an inline definition for the given targetType. Ignoring custom definitions up to the given one, but respecting others.
     *
     * @param targetType type to create definition node for
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return designated definition node for the targetType
     *
     * @see #createDefinition(ResolvedType)
     * @see #createStandardDefinitionReference(ResolvedType, CustomDefinitionProviderV2)
     */
    ObjectNode createStandardDefinition(ResolvedType targetType, CustomDefinitionProviderV2 ignoredDefinitionProvider);

    /**
     * Create a standard definition for the given property. Ignoring custom definitions up to the given one, but respecting others.
     * <br>
     * The returned schema will be inlined and fully populated, i.e. it may be further manipulated - at the risk of an endless loop in case of a
     * circular reference.
     *
     * @param targetScope property to create definition node for
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return inline definition for the given field
     *
     * @see #createDefinitionReference(ResolvedType)
     * @see #createStandardDefinitionReference(FieldScope, CustomPropertyDefinitionProvider)
     */
    ObjectNode createStandardDefinition(FieldScope targetScope, CustomPropertyDefinitionProvider<FieldScope> ignoredDefinitionProvider);

    /**
     * Create a standard definition for the given property. Ignoring custom definitions up to the given one, but respecting others.
     * <br>
     * The returned schema will be inlined and fully populated, i.e. it may be further manipulated - at the risk of an endless loop in case of a
     * circular reference.
     * <br>
     * The returned type is always an {@link ObjectNode} unless the given method is {@code void}, which will result in a {@link BooleanNode#FALSE}.
     *
     * @param targetScope property to create definition node for
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return inline definition for the given method
     *
     * @see #createDefinition(ResolvedType)
     * @see #createStandardDefinitionReference(MethodScope, CustomPropertyDefinitionProvider)
     */
    JsonNode createStandardDefinition(MethodScope targetScope, CustomPropertyDefinitionProvider<MethodScope> ignoredDefinitionProvider);

    /**
     * Create a standard definition for the given targetType. Ignoring custom definitions up to the given one, but respecting others.
     * <br>
     * The returned node will be empty, but is being remembered internally and populated later, i.e. it should not be changed!
     *
     * @param targetType type to create definition (reference) node for
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return (temporarily) empty reference node for the targetType that will only be populated at the very end of the schema generation
     *
     * @see #createDefinitionReference(ResolvedType)
     * @see #createStandardDefinition(ResolvedType, CustomDefinitionProviderV2)
     */
    ObjectNode createStandardDefinitionReference(ResolvedType targetType, CustomDefinitionProviderV2 ignoredDefinitionProvider);

    /**
     * Create a standard definition for the given property. Ignoring custom definitions up to the given one, but respecting others.
     * <br>
     * If a specific custom definition for this field is being applied, it will be inlined and fully populated; that may be further manipulated.
     * Otherwise, the returned node will be empty and is populated only later, i.e. it should not be changed in that case!
     *
     * @param targetScope property to create definition (reference) node for
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return either custom inline definition for this field or a (temporarily) empty reference node for the targetType that will only be populated
     *         at the very end of the schema generation
     *
     * @see #createDefinitionReference(ResolvedType)
     * @see #createStandardDefinition(FieldScope, CustomPropertyDefinitionProvider)
     */
    ObjectNode createStandardDefinitionReference(FieldScope targetScope, CustomPropertyDefinitionProvider<FieldScope> ignoredDefinitionProvider);

    /**
     * Create a standard definition for the given property. Ignoring custom definitions up to the given one, but respecting others.
     * <br>
     * If a specific custom definition for this method is being applied, it will be inlined and fully populated; that may be further manipulated.
     * Otherwise, the returned node will be empty and is populated only later, i.e. it should not be changed in that case!
     * <br>
     * The returned type is always an {@link ObjectNode} unless the given method is {@code void}, which will result in a {@link BooleanNode#FALSE}.
     *
     * @param targetScope property to create definition (reference) node for
     * @param ignoredDefinitionProvider custom definition provider to ignore
     * @return either custom inline definition for this method or a (temporarily) empty reference node for the targetType that will only be populated
     *         at the very end of the schema generation
     *
     * @see #createDefinitionReference(ResolvedType)
     * @see #createStandardDefinition(MethodScope, CustomPropertyDefinitionProvider)
     */
    JsonNode createStandardDefinitionReference(MethodScope targetScope, CustomPropertyDefinitionProvider<MethodScope> ignoredDefinitionProvider);

    /**
     * Ensure that the JSON schema represented by the given node allows for it to be of "type" "null".
     *
     * @param node representation of a JSON schema (part) that should allow a value of "type" "null"
     * @return reference to the given parameter node
     */
    ObjectNode makeNullable(ObjectNode node);
}
