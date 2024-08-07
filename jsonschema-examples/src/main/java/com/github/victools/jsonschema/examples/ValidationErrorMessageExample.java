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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/issues/373">#373</a>.
 * <br>
 * Populate custom "message" property in schema based on validation annotations.
 * <br>
 * This is a limited example! Theoretically, all possible validations need to be considered explicitly again.
 */
public class ValidationErrorMessageExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new JakartaValidationModule(JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED));
        configBuilder.forFields()
                .withInstanceAttributeOverride(this::includeMessageAttribute);
        SchemaGeneratorConfig config = configBuilder.build();
        return new SchemaGenerator(config).generateSchema(TestClass.class);
    }

    private <M extends MemberScope<?, ?>> void includeMessageAttribute(ObjectNode collectedMemberAttributes, M member,
                SchemaGenerationContext context) {
        final Map<SchemaKeyword, String> validationMessages = new HashMap<>();
        final NotNull notNullAnnotation = this.getAnnotation(member, NotNull.class);
        final NotEmpty notEmptyAnnotation = this.getAnnotation(member, NotEmpty.class);
        final NotBlank notBlankAnnotation = this.getAnnotation(member, NotBlank.class);
        final Size sizeAnnotation = this.getAnnotation(member, Size.class);
        final Min minAnnotation = this.getAnnotation(member, Min.class);
        final Max maxAnnotation = this.getAnnotation(member, Max.class);

        if (!member.isFakeContainerItemScope()) {
            if (notNullAnnotation != null) {
                validationMessages.put(SchemaKeyword.TAG_REQUIRED, notNullAnnotation.message());
            } else if (notEmptyAnnotation != null) {
                validationMessages.put(SchemaKeyword.TAG_REQUIRED, notEmptyAnnotation.message());
            } else if (notBlankAnnotation != null) {
                validationMessages.put(SchemaKeyword.TAG_REQUIRED, notBlankAnnotation.message());
            }
        }
        if (member.isContainerType()) {
            this.handleArrayValidation(validationMessages, sizeAnnotation, notEmptyAnnotation);
        }
        if (member.getType().getErasedType() == String.class) {
            this.handleStringValidation(validationMessages, sizeAnnotation, notBlankAnnotation, notEmptyAnnotation);
        }
        if (Number.class.isAssignableFrom(member.getType().getErasedType())) {
            this.handleNumberValidation(validationMessages, minAnnotation, maxAnnotation);
        }

        if (!validationMessages.isEmpty()) {
            ObjectNode messageNode = collectedMemberAttributes.putObject("message");
            validationMessages.forEach((key, message) -> messageNode.put(context.getKeyword(key),
                    /* resolve message key here before including in schema */ message));
        }
    }

    private void handleArrayValidation(Map<SchemaKeyword, String> validationMessages, Size sizeAnnotation,
            NotEmpty notEmptyAnnotation) {
        if (sizeAnnotation != null && sizeAnnotation.min() > 0) {
            validationMessages.put(SchemaKeyword.TAG_ITEMS_MIN, sizeAnnotation.message());
        } else if (notEmptyAnnotation != null) {
            validationMessages.put(SchemaKeyword.TAG_ITEMS_MIN, notEmptyAnnotation.message());
        }
        if (sizeAnnotation != null && sizeAnnotation.max() < Integer.MAX_VALUE) {
            validationMessages.put(SchemaKeyword.TAG_ITEMS_MAX, sizeAnnotation.message());
        }
    }

    private void handleStringValidation(Map<SchemaKeyword, String> validationMessages, Size sizeAnnotation,
            NotBlank notBlankAnnotation, NotEmpty notEmptyAnnotation) {
        if (sizeAnnotation != null && sizeAnnotation.min() > 0) {
            validationMessages.put(SchemaKeyword.TAG_LENGTH_MIN, sizeAnnotation.message());
        } else if (notBlankAnnotation != null) {
            validationMessages.put(SchemaKeyword.TAG_LENGTH_MIN, notBlankAnnotation.message());
        } else if (notEmptyAnnotation != null) {
            validationMessages.put(SchemaKeyword.TAG_LENGTH_MIN, notEmptyAnnotation.message());
        }
        if (sizeAnnotation != null && sizeAnnotation.max() < Integer.MAX_VALUE) {
            validationMessages.put(SchemaKeyword.TAG_LENGTH_MAX, sizeAnnotation.message());
        }
    }

    private void handleNumberValidation(Map<SchemaKeyword, String> validationMessages, Min minAnnotation, Max maxAnnotation) {
        if (minAnnotation != null) {
            validationMessages.put(SchemaKeyword.TAG_MINIMUM, minAnnotation.message());
        }
        if (maxAnnotation != null) {
            validationMessages.put(SchemaKeyword.TAG_MAXIMUM, maxAnnotation.message());
        }
    }

    private <M extends MemberScope<?, ?>, A extends Annotation> A getAnnotation(M member, Class<A> annotationType) {
        if (member.isFakeContainerItemScope()) {
            return member.getContainerItemAnnotationConsideringFieldAndGetterIfSupported(annotationType);
        }
        return member.getAnnotationConsideringFieldAndGetterIfSupported(annotationType);
    }

    static class TestClass {
        @NotBlank(message = "Mandatory to be present and contain not only whitespaces")
        @Size(min = 5, max = 100, message = "Must be between 5 and 100 characters long")
        public String title;

        @NotEmpty(message = "Mandatory to be present and contain at least one item")
        public List<
                    @NotNull(message = "No null values in list")
                    @Min(value = 0, message = "less than 0% is unfair")
                    @Max(value = 100, message = "more than 100% is unrealistic")
                    BigDecimal
                > results;
    }
}
