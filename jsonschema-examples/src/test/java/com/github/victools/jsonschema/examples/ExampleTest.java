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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class ExampleTest {

    @ParameterizedTest
    @ValueSource(classes = {
            AnnotationInheritanceExample.class,
            DependentRequiredExample.class,
            EnumMapExample.class,
            ExternalRefAnnotationExample.class,
            ExternalRefPackageExample.class,
            IfThenElseExample.class,
            InheritanceRefExample.class,
            JacksonDescriptionAsTitleExample.class,
            JacksonSubtypeDefinitionExample.class,
            NamingStrategyExample.class,
            SingleArrayItemExample.class,
            StrictTypeInfoExample.class,
            SubtypeLookUpExample.class,
            TargetTypeOverrideExample.class,
            ValidationErrorMessageExample.class
    })
    public void testExample(Class<? extends SchemaGenerationExampleInterface> exampleType) throws Exception {
        SchemaGenerationExampleInterface exampleImplementation = exampleType.getDeclaredConstructor().newInstance();
        JsonNode result = exampleImplementation.generateSchema();
        String rawJsonSchema = result.toPrettyString();
        JSONAssert.assertEquals('\n' + rawJsonSchema + '\n',
                loadResource(exampleType.getSimpleName() + "-result.json"), rawJsonSchema,
                JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = Objects.requireNonNull(ExampleTest.class.getResourceAsStream(resourcePath));
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        return stringBuilder.toString();
    }
}
