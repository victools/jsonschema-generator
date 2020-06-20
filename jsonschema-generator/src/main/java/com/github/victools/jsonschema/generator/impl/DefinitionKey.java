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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;

/**
 * Identifier for a particular sub-schema definition. This consists of the encountered type as well as some contextual information, as there may be
 * multiple alternative definitions (e.g. a standard definition and one or multiple custom definitions) for a single type.
 */
public class DefinitionKey {

    private final ResolvedType type;
    private final CustomDefinitionProviderV2 ignoredDefinitionProvider;

    /**
     * Constructor.
     *
     * @param type encountered type a schema definition is associated with
     * @param ignoredDefinitionProvider first custom definition provider that was ignored when creating the definition (is null in most cases)
     */
    protected DefinitionKey(ResolvedType type, CustomDefinitionProviderV2 ignoredDefinitionProvider) {
        this.type = type;
        this.ignoredDefinitionProvider = ignoredDefinitionProvider;
    }

    /**
     * Getter for the associated java type.
     *
     * @return encountered type a schema definition is associated with
     */
    public ResolvedType getType() {
        return this.type;
    }

    /**
     * Getter for the a custom definition provider that was the first to be skipped during the generation of the schema definition. Ignoring a custom
     * definition provider allows for the next custom definition provider to be applied, or if there is none or no custom definition is returned:
     * falling-back on the standard definition for the targeted type.
     *
     * @return first custom definition provider that was ignored when creating the definition (is null in most cases)
     */
    public CustomDefinitionProviderV2 getIgnoredDefinitionProvider() {
        return this.ignoredDefinitionProvider;
    }

    /*
     * Specific implementations of hashCode() and equals(Object) are required as this class is intended to be used as key in a Map.
     */

    @Override
    public int hashCode() {
        return this.type.hashCode()
                + (this.ignoredDefinitionProvider == null ? 0 : this.ignoredDefinitionProvider.hashCode());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DefinitionKey)) {
            return false;
        }
        DefinitionKey otherReference = (DefinitionKey) other;
        return this.type.equals(otherReference.getType()) && this.ignoredDefinitionProvider == otherReference.getIgnoredDefinitionProvider();
    }
}
