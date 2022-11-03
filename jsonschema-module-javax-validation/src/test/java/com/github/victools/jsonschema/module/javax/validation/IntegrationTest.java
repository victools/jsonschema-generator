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
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
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

    /**
     * Test
     *
     * @throws Exception
     */
    @Test
    public void testIntegration() throws Exception {
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
        JsonNode result = generator.generateSchema(TestClass.class);

        String rawJsonSchema = result.toString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource("integration-test-result.json"), rawJsonSchema, JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = IntegrationTest.class
                .getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        String fileAsString = stringBuilder.toString();
        return fileAsString;

    }

    static class TestClass implements TestInterface {

        @Null
        public Object nullObject;

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

        @NotNull
        @Email(regexp = ".+@.+\\..+")
        public String notNullEmail;
        @NotEmpty
        @Pattern(regexp = "\\w+")
        public String notEmptyPatternText;
        @NotBlank
        public String notBlankText;
        @Size(min = 5, max = 12)
        public String sizeRangeText;

        @Min(7)
        @Max(38)
        public int inclusiveRangeInt;

        @DecimalMin(value = "0", inclusive = false)
        @DecimalMax(value = "1", inclusive = false)
        public double exclusiveRangeDouble;
        
        private String valueWithInheritedConstraints;

        @Override
        public String getValueWithInheritedConstraints() {
            return valueWithInheritedConstraints;
        }
    }

    interface TestInterface {
        @Size(min = 1)
        String getValueWithInheritedConstraints();
    }
}
