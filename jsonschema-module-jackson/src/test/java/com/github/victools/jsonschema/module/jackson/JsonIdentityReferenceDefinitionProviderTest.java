/*
 * Copyright 2022 VicTools.
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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeContext;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for the {@link JsonIdentityReferenceDefinitionProvider}.
 */
public class JsonIdentityReferenceDefinitionProviderTest extends AbstractTypeAwareTest {

    private final JsonIdentityReferenceDefinitionProvider provider = new JsonIdentityReferenceDefinitionProvider();

    public JsonIdentityReferenceDefinitionProviderTest() {
        super(TestTypeWithReferences.class);
    }

    @BeforeEach
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2020_12);
    }

    static Stream<Arguments> parametersForTestGetIdentityReferenceType_byType() {
        return Stream.of(
            Arguments.of(TestTypeReferencedAsInteger.class, Integer.class),
            Arguments.of(TestTypeReferencedAsString.class, String.class),
            Arguments.of(TestTypeReferencedAsUuid.class, UUID.class),
            Arguments.of(TestTypeReferencedByLongId.class, Long.class),
            Arguments.of(TestTypeReferencedByObjectId.class, TestTypeReferencedByObjectId.IdType.class),
            Arguments.of(TestTypeWithObjectIdRequiringReferenceAnnotation.class, null),
            Arguments.of(JsonIdentityReferenceDefinitionProviderTest.class, null)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestGetIdentityReferenceType_byType")
    public void testGetIdentityReferenceType_byType(Class<?> testType, Class<?> expectedErasedResultType) {
        TypeContext typeContext = this.getContext().getTypeContext();
        ResolvedType targetType = typeContext.resolve(testType);
        Optional<ResolvedType> result = this.provider.getIdentityReferenceType(targetType, typeContext);
        Assertions.assertNotNull(result);
        if (expectedErasedResultType == null) {
            Assertions.assertFalse(result.isPresent());
        } else {
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals(expectedErasedResultType, result.get().getErasedType());
        }
    }

    static Stream<Arguments> parametersForTestGetIdentityReferenceType_byField() {
        return Stream.of(
            Arguments.of("integerReference", Integer.class),
            Arguments.of("integerReferenceWithoutAnnotation", null),
            Arguments.of("stringReference", String.class),
            Arguments.of("uuidReference", UUID.class),
            Arguments.of("longReference", Long.class),
            Arguments.of("objectReference", TestTypeReferencedByObjectId.IdType.class),
            Arguments.of("objectReferenceWithAnnotation", TestTypeWithObjectIdRequiringReferenceAnnotation.IdType.class),
            Arguments.of("objectReferenceWithoutAnnotation", null)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersForTestGetIdentityReferenceType_byField")
    public void testGetIdentityReferenceType_byField(String fieldName, Class<?> expectedErasedResultType) {
        FieldScope targetField = this.getTestClassField(fieldName);
        Optional<ResolvedType> result = this.provider.getIdentityReferenceType(targetField);
        Assertions.assertNotNull(result);
        if (expectedErasedResultType == null) {
            Assertions.assertFalse(result.isPresent());
        } else {
            Assertions.assertTrue(result.isPresent());
            Assertions.assertEquals(expectedErasedResultType, result.get().getErasedType());
        }
    }

    private static class TestTypeWithReferences {

        @JsonIdentityReference(alwaysAsId = true)
        private TestTypeReferencedAsInteger integerReference;
        // the generator is expected to ask once more by the type alone, which will handle this case in the end
        private TestTypeReferencedAsInteger integerReferenceWithoutAnnotation;
        @JsonIdentityReference(alwaysAsId = true)
        private TestTypeReferencedAsString stringReference;
        @JsonIdentityReference(alwaysAsId = true)
        private TestTypeReferencedAsUuid uuidReference;
        @JsonIdentityReference(alwaysAsId = true)
        private TestTypeReferencedByLongId longReference;
        @JsonIdentityReference(alwaysAsId = true)
        private TestTypeReferencedByObjectId objectReference;
        @JsonIdentityReference(alwaysAsId = true)
        private TestTypeWithObjectIdRequiringReferenceAnnotation objectReferenceWithAnnotation;
        private TestTypeWithObjectIdRequiringReferenceAnnotation objectReferenceWithoutAnnotation;
        
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    @JsonIdentityReference(alwaysAsId = true)
    private static class TestTypeReferencedAsInteger {
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    @JsonIdentityReference(alwaysAsId = true)
    private static class TestTypeReferencedAsString {
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class)
    @JsonIdentityReference(alwaysAsId = true)
    private static class TestTypeReferencedAsUuid {
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private static class TestTypeReferencedByLongId {
        private Long id;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private static class TestTypeReferencedByObjectId {
        private IdType id;

        private class IdType {
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    private static class TestTypeWithObjectIdRequiringReferenceAnnotation {
        private IdType id;

        private class IdType {
        }
    }
}
