/*
 * Copyright 2024 VicTools
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

package com.github.victools.jsonschema.module.jakarta.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import jakarta.validation.Valid;

/**
 * Custom Definition Provider, to realise the {@code JakartaValidationOption.SKIP_WHERE_VALID_ANNOTATION_IS_MISSING}.
 */
public class JakartaSkipCustomPropertyDefinitionProvider<M extends MemberScope<?, ?>> implements CustomPropertyDefinitionProvider<M> {

    private Scope validationScope = Scope.INCLUDE;

    @Override
    public CustomPropertyDefinition provideCustomSchemaDefinition(M scope, SchemaGenerationContext context) {
        if (!this.shouldSkipValidations() && scope.getAnnotationConsideringFieldAndGetter(Valid.class) != null) {
            return null;
        }
        Scope previousValidationScope = this.validationScope;
        this.validationScope = Scope.EXCLUDE;
        JsonNode definitionWithoutValidations;
        if (scope instanceof FieldScope) {
            definitionWithoutValidations = context.createStandardDefinition((FieldScope) scope, (CustomPropertyDefinitionProvider<FieldScope>) this);
        } else if (scope instanceof MethodScope) {
            definitionWithoutValidations = context.createStandardDefinition((MethodScope) scope, (CustomPropertyDefinitionProvider<MethodScope>) this);
        } else {
            throw new IllegalArgumentException("Unsupported member scope type: " + scope.getClass().getName());
        }
        this.validationScope = previousValidationScope;
        if (definitionWithoutValidations instanceof ObjectNode) {
            return new CustomPropertyDefinition((ObjectNode) definitionWithoutValidations, CustomDefinition.AttributeInclusion.YES);
        }
        return null;
    }

    enum Scope {
        INCLUDE, EXCLUDE;
    }

    public boolean shouldSkipValidations() {
        return this.validationScope == Scope.EXCLUDE;
    }

    public boolean hasValidAnnotation(M scope) {
        if (scope.getAnnotationConsideringFieldAndGetter(Valid.class) != null) {
            return true;
        }
        if (!scope.isFakeContainerItemScope()) {
            return false;
        }
        return scope.getContainerItemAnnotationConsideringFieldAndGetterIfSupported(Valid.class) != null;
    }
}
