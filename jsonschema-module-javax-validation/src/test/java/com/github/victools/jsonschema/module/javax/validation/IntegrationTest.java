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

package com.github.victools.jsonschema.module.javax.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import javax.validation.Constraint;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Integration test of this module being used in a real SchemaGenerator instance.
 */
public class IntegrationTest {

    @Test
    public void testIntegration() throws Exception {
        // arrange
        // active all optional modules
        JavaxValidationModule module = new JavaxValidationModule(
                JavaxValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED,
                JavaxValidationOption.NOT_NULLABLE_METHOD_IS_REQUIRED,
                JavaxValidationOption.INCLUDE_PATTERN_EXPRESSIONS);
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
                .with(Option.NULLABLE_ARRAY_ITEMS_ALLOWED)
                .with(module)
                .build();
        SchemaGenerator generator = new SchemaGenerator(config);

        // act
        GeneratedSchema[] result = generator.generateSchema(TestClass.class);

        // assert
        JsonNode schema = result[0].getSchema();
        String rawJsonSchema = schema.toString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource("integration-test-result.json"), rawJsonSchema, JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = IntegrationTest.class.getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        return stringBuilder.toString();
    }

    static class TestClass {

        @Null
        public Object nullObject;
        public Book inheritedAnnotation;

        @NotNull
        public List<@Min(2) @Max(2048) Integer> notNullList;
        @NotEmpty
        public List<@DecimalMin(value = "0", inclusive = false) @DecimalMax(value = "1", inclusive = false) Double> notEmptyList;
        @Size(min = 3, max = 25)
        public List<@NotEmpty @Size(max = 100) String> sizeRangeList;
        @Size(min = 3, max = 25)
        public List<String> sizeRangeListWithoutItemAnnotation;

        @Size(min = 4, max = 18)
        public Optional<Integer> optionalSizeRangeString1;
        public Optional<@Size(min = 5, max = 10) Integer> optionalSizeRangeString2;

        @Min(1)
        @Max(8)
        public Optional<Integer> optionalInclusiveRangeInt1;
        public Optional<@Min(2) @Max(5) Integer> optionalInclusiveRangeInt2;

        @MandatoryEmail
        public String notNullEmail;
        @NotEmpty
        @Pattern(regexp = "\\w+")
        public String notEmptyPatternText;
        @NotBlank
        public String notBlankText;
        @NotNullMetaAnnotationMissingConstraint
        @Size(min = 5, max = 12)
        public String sizeRangeText;

        @Min(7)
        @Max(38)
        public int inclusiveRangeInt;

        @DecimalMin(value = "0", inclusive = false)
        @DecimalMax(value = "1", inclusive = false)
        public double exclusiveRangeDouble;
    }

    static class Book implements Publication {
        private String title;

        @Override
        public String getTitle() {
            return this.title;
        }
    }

    interface Publication {
        @NotEmpty
        String getTitle();
    }

    @NotNull
    @Email(regexp = ".+@.+\\..+")
    @Constraint(validatedBy = {})
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface MandatoryEmail {
        // presence of @Constraint annotation allows this kind of meta annotation for grouping common validation annotations
    }

    @NotNull
    @Target({ElementType.PARAMETER, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface NotNullMetaAnnotationMissingConstraint {
        // due to the missing @Constraint annotation, the @NotNull is being ignored
    }
}
