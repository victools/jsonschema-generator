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

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The result of a custom definition look-up.
 */
public class CustomDefinition {

    /**
     * Alternative accessor for {@link DefinitionType#INLINE CustomDefinition.DefinitionType.INLINE}.
     */
    public static final DefinitionType INLINE_DEFINITION = DefinitionType.INLINE;
    /**
     * Alternative accessor for {@link AttributeInclusion#YES CustomDefinition.AttributeInclusion.YES}.
     */
    public static final AttributeInclusion INCLUDING_ATTRIBUTES = AttributeInclusion.YES;
    /**
     * Alternative accessor for {@link AttributeInclusion#NO CustomDefinition.AttributeInclusion.NO}.
     */
    public static final AttributeInclusion EXCLUDING_ATTRIBUTES = AttributeInclusion.NO;

    private final ObjectNode value;
    private final CustomDefinition.DefinitionType definitionType;
    private final AttributeInclusion attributeInclusion;

    /**
     * Constructor for a custom definition that should follow the standard behaviour in regards to be inlined or mentioned in the "definitions".
     *
     * @param value generated custom definition
     */
    public CustomDefinition(ObjectNode value) {
        this(value, DefinitionType.STANDARD, AttributeInclusion.YES);
    }

    /**
     * Constructor for a custom definition.
     *
     * @param value generated custom definition
     * @param meantToBeInline whether the definition should be inlined even if it occurs multiple times; otherwise applying standard behaviour
     */
    public CustomDefinition(ObjectNode value, boolean meantToBeInline) {
        this(value, meantToBeInline ? DefinitionType.INLINE : DefinitionType.STANDARD, AttributeInclusion.YES);
    }

    /**
     * Constructor for a custom definition.
     *
     * @param value generated custom definition
     * @param definitionType whether the definition should be inlined even if it occurs multiple times; otherwise applying standard behaviour
     * @param attributeInclusion whether additional attributes should be applied on top of this custom definition
     */
    public CustomDefinition(ObjectNode value, DefinitionType definitionType, AttributeInclusion attributeInclusion) {
        this.value = value;
        this.definitionType = definitionType;
        this.attributeInclusion = attributeInclusion;
    }

    /**
     * Getter for the actual custom definition.
     *
     * @return node containing the custom definition
     */
    public ObjectNode getValue() {
        return this.value;
    }

    /**
     * Getter for the associated definition type.
     *
     * @return indication whether a custom definition should always be inlined or follow the standard behaviour
     */
    public DefinitionType getDefinitionType() {
        return this.definitionType;
    }

    /**
     * Getter for the flag indicating whether this custom definition should be inlined even if it occurs multiple times.
     *
     * @return whether this custom definition should be inlined even if it occurs multiple times
     */
    public final boolean isMeantToBeInline() {
        return this.definitionType == DefinitionType.INLINE;
    }

    /**
     * Getter for the associated extent of additional attributes being collected and applied.
     *
     * @return indication whether the normal attribute collection should be performed and any attributes should be added to the custom definition
     */
    public AttributeInclusion getAttributeInclusion() {
        return this.attributeInclusion;
    }

    public boolean shouldIncludeAttributes() {
        return this.attributeInclusion == AttributeInclusion.YES;
    }

    /**
     * Indication whether a custom definition should always be inlined or follow the standard behaviour.
     */
    public enum DefinitionType {
        /**
         * Always inline an associated custom definition (with the potential risk of creating infinite loops).
         */
        INLINE,
        /**
         * Follow the standard behaviour of being moved into the {@link SchemaKeyword#TAG_DEFINITIONS} if it is being referenced multiple times or
         * {@link Option#DEFINITIONS_FOR_ALL_OBJECTS} is enabled.
         */
        STANDARD;
    }

    /**
     * Indication whether the normal attribute collection should be performed and any attributes should be added to the custom definition.
     */
    public enum AttributeInclusion {
        /**
         * Collect and add attributes both for a specific field/method as well as for the associated type in general.
         */
        YES,
        /**
         * Skip the attribute collection both for a specific field/method or the type in general if that's the level where this custom definition is
         * being applied. If this is a nested type/definition, a previous (custom) definition may have already decided something else.
         */
        NO;
    }
}
