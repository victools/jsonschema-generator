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

/**
 * Supported JSON Schema Versions.
 */
public enum SchemaVersion {
    DRAFT_6("http://json-schema.org/draft-06/schema#"),
    DRAFT_7("http://json-schema.org/draft-07/schema#"),
    DRAFT_2019_09("https://json-schema.org/draft/2019-09/schema"),
    DRAFT_2020_12("https://json-schema.org/draft/2020-12/schema");

    private final String identifier;

    /**
     * Constructor.
     *
     * @param schemaIdentifier value for the {@code $schema} tag
     */
    private SchemaVersion(String schemaIdentifier) {
        this.identifier = schemaIdentifier;
    }

    /**
     * Getter for the {@code $schema} tag's value.
     *
     * @return value for the {@code $schema} tag
     */
    public String getIdentifier() {
        return this.identifier;
    }
}
