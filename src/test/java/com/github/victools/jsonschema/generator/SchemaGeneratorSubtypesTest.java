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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for {@link SchemaGenerator} class.
 */
@RunWith(JUnitParamsRunner.class)
public class SchemaGeneratorSubtypesTest {

    Object parametersForTestGenerateSchema() {
        return new Object[][]{
            {"testclass-withsupertypereferences-NO_SUBTYPES", Collections.emptyList()},
            {"testclass-withsupertypereferences-ONE_SUBTYPE_VALID", Collections.singletonList(TestSubClass1.class)},
            {"testclass-withsupertypereferences-ONE_SUBTYPE_INVALID", Collections.singletonList(TestSubClass3.class)},
            {"testclass-withsupertypereferences-TWO_SUBTYPES", Arrays.asList(TestSubClass2.class, TestSubClass3.class)},
            {"testclass-withsupertypereferences-THREE_SUBTYPES", Arrays.asList(TestSubClass1.class, TestSubClass2.class, TestSubClass3.class)}
        };
    }

    @Test
    @Parameters
    @TestCaseName(value = "{method}({0}) [{index}]")
    public void testGenerateSchema(String caseTitle, List<Class<?>> subtypes) throws Exception {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(new ObjectMapper(), OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.NULLABLE_FIELDS_BY_DEFAULT);
        configBuilder.forTypesInGeneral()
                .withSubtypeResolver(new TestSubtypeResolver(subtypes))
                .withTitleResolver(TypeScope::getSimpleTypeDescription)
                .withDescriptionResolver(scope -> scope.getType().getErasedType() == TestSuperClass.class ? "supertype-only description" : null);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        JsonNode result = generator.generateSchema(TestClassWithSuperTypeReferences.class);
        JSONAssert.assertEquals('\n' + result.toString() + '\n',
                loadResource(caseTitle + ".json"), result.toString(), JSONCompareMode.STRICT);
    }

    private static String loadResource(String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = SchemaGeneratorSubtypesTest.class
                .getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        String fileAsString = stringBuilder.toString();
        return fileAsString;
    }

    private static class TestSubtypeResolver implements SubtypeResolver {

        private List<Class<?>> subtypes;

        TestSubtypeResolver(List<Class<?>> subtypes) {
            this.subtypes = subtypes;
        }

        @Override
        public List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
            if (declaredType.getErasedType() == TestSuperClass.class) {
                return subtypes.stream()
                        .map(subtype -> {
                            try {
                                return context.getTypeContext().resolveSubtype(declaredType, subtype);
                            } catch (IllegalArgumentException ex) {
                                // possible reasons are mainly:
                                // 1. Conflicting type parameters between declared super type and this particular subtype
                                // 2. Extra generic introduced in subtype that is not present in supertype, i.e. which cannot be resolved
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return null;
        }

    }

    private static class TestClassWithSuperTypeReferences {

        public TestSuperClass<Boolean> booleanSupertypeField;
        public TestSuperClass<String> stringSupertypeField;

    }

    private static class TestSuperClass<T> {

        public T genericFieldInSupertype;

    }

    private static class TestSubClass1<T> extends TestSuperClass<T> {

        public List<T> dependentGenericFieldInSubtype;
        public T sameGenericFieldInSubtype1;

    }

    private static class TestSubClass2<T> extends TestSuperClass<T> {

        public T sameGenericFieldInSubtype2;

    }

    private static class TestSubClass3 extends TestSuperClass<String> {

        public int fieldInSubtype;

    }
}
