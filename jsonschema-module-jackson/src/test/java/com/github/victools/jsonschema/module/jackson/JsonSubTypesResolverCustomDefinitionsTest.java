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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaVersion;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for the {@link JsonSubTypesResolver}.
 */
@RunWith(JUnitParamsRunner.class)
public class JsonSubTypesResolverCustomDefinitionsTest extends AbstractTypeAwareTest {

    private final JsonSubTypesResolver instance = new JsonSubTypesResolver();

    public JsonSubTypesResolverCustomDefinitionsTest() {
        super(TestClassWithSuperTypeReferences.class);
    }

    @Before
    public void setUp() {
        this.prepareContextForVersion(SchemaVersion.DRAFT_2019_09);
        Answer<String> memberTitleLookUp = invocation -> ((MemberScope<?, ?>) invocation.getArgument(0))
                .getType().getErasedType() == TestSubClass1.class ? "property attribute" : null;
        Mockito.when(this.getContext().getGeneratorConfig().resolveTitle(Mockito.any(FieldScope.class))).thenAnswer(memberTitleLookUp);
        Mockito.when(this.getContext().getGeneratorConfig().resolveTitle(Mockito.any(MethodScope.class))).thenAnswer(memberTitleLookUp);
    }

    private void assertCustomDefinitionsAreEqual(String customDefinition, CustomDefinition result, CustomDefinition.DefinitionType definitionType)
            throws Exception {
        if (customDefinition == null) {
            Assert.assertNull(result);
        } else {
            Assert.assertNotNull(result);
            Assert.assertNotNull(result.getValue());
            JSONAssert.assertEquals('\n' + result.getValue().toString() + '\n',
                    customDefinition, result.getValue().toString(), JSONCompareMode.STRICT);
            Assert.assertEquals(definitionType, result.getDefinitionType());
            Assert.assertEquals(CustomDefinition.AttributeInclusion.NO, result.getAttributeInclusion());
        }
    }

    public Object[] parametersForTestProvideCustomSchemaDefinition() {
        return new Object[][]{
            {TestClassWithSuperTypeReferences.class, null},
            {TestSuperClassWithNameProperty.class, null},
            {TestSubClass1.class, "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_1\"}}}]}"}
        };
    }

    @Test
    @Parameters
    public void testProvideCustomSchemaDefinition(Class<?> erasedTargetType, String customDefinition) throws Exception {
        ResolvedType javaType = this.getContext().getTypeContext().resolve(erasedTargetType);
        CustomDefinition result = this.instance.provideCustomSchemaDefinition(javaType, this.getContext());
        this.assertCustomDefinitionsAreEqual(customDefinition, result, CustomDefinition.DefinitionType.STANDARD);
    }

    public Object[] parametersForTestProvideCustomPropertySchemaDefinitionForField() {
        return new Object[][]{
            {"supertypeNoAnnotation", null, null},
            {"supertypeNoAnnotation", TestSubClass1.class, null},
            {"supertypeNoAnnotation", TestSubClass2.class, null},
            {"supertypeWithAnnotationOnField", null, null},
            {"supertypeWithAnnotationOnField", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"}}}]}"},
            {"supertypeWithAnnotationOnField", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"}}}]}"},
            {"supertypeWithAnnotationOnGetter", null, null},
            {"supertypeWithAnnotationOnGetter", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_1\"}}}]}"},
            {"supertypeWithAnnotationOnGetter", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_2\"}}}]}"},
            {"supertypeWithAnnotationOnFieldAndGetter", null, null},
            {"supertypeWithAnnotationOnFieldAndGetter", TestSubClass1.class,
                "{\"type\":\"array\",\"items\":[{\"type\":\"string\",\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"},"
                + "{\"allOf\":[{},{\"title\":\"property attribute\"}]}]}"},
            {"supertypeWithAnnotationOnFieldAndGetter", TestSubClass2.class,
                "{\"type\":\"array\",\"items\":[{\"type\":\"string\",\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"},{}]}"}
        };
    }

    @Test
    @Parameters
    public void testProvideCustomPropertySchemaDefinitionForField(String fieldName, Class<?> typeOverride, String customDefinition)
            throws Exception {
        FieldScope field = this.getTestClassField(fieldName);
        if (typeOverride != null) {
            field = field.withOverriddenType(this.getContext().getTypeContext().resolve(typeOverride));
        }
        CustomDefinition result = this.instance.provideCustomPropertySchemaDefinition(field, this.getContext());
        this.assertCustomDefinitionsAreEqual(customDefinition, result, CustomDefinition.DefinitionType.INLINE);
    }

    public Object[] parametersForTestProvideCustomPropertySchemaDefinitionForMethod() {
        return new Object[][]{
            {"getSupertypeNoAnnotation", null, null},
            {"getSupertypeNoAnnotation", TestSubClass1.class, null},
            {"getSupertypeNoAnnotation", TestSubClass2.class, null},
            {"getSupertypeWithAnnotationOnField", null, null},
            {"getSupertypeWithAnnotationOnField", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"}}}]}"},
            {"getSupertypeWithAnnotationOnField", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"}}}]}"},
            {"getSupertypeWithAnnotationOnGetter", null, null},
            {"getSupertypeWithAnnotationOnGetter", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_1\"}}}]}"},
            {"getSupertypeWithAnnotationOnGetter", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_2\"}}}]}"},
            {"getSupertypeWithAnnotationOnFieldAndGetter", null, null},
            {"getSupertypeWithAnnotationOnFieldAndGetter", TestSubClass1.class,
                "{\"type\":\"object\",\"properties\":{\"SUB_CLASS_1\":{\"allOf\":[{},{\"title\":\"property attribute\"}]}}}"},
            {"getSupertypeWithAnnotationOnFieldAndGetter", TestSubClass2.class,
                "{\"type\":\"object\",\"properties\":{\"SUB_CLASS_2\":{}}}"}
        };
    }

    @Test
    @Parameters
    public void testProvideCustomPropertySchemaDefinitionForMethod(String methodName, Class<?> typeOverride, String customDefinition)
            throws Exception {
        MethodScope method = this.getTestClassMethod(methodName);
        if (typeOverride != null) {
            method = method.withOverriddenType(this.getContext().getTypeContext().resolve(typeOverride));
        }
        CustomDefinition result = this.instance.provideCustomPropertySchemaDefinition(method, this.getContext());
        this.assertCustomDefinitionsAreEqual(customDefinition, result, CustomDefinition.DefinitionType.INLINE);
    }

    private static class TestClassWithSuperTypeReferences {

        public TestSuperClassWithNameProperty supertypeNoAnnotation;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "fullClass", include = JsonTypeInfo.As.EXISTING_PROPERTY)
        public TestSuperClassWithNameProperty supertypeWithAnnotationOnField;
        public TestSuperClassWithNameProperty supertypeWithAnnotationOnGetter;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public TestSuperClassWithNameProperty supertypeWithAnnotationOnFieldAndGetter;

        public TestSuperClassWithNameProperty getSupertypeNoAnnotation() {
            return this.supertypeNoAnnotation;
        }

        public TestSuperClassWithNameProperty getSupertypeWithAnnotationOnField() {
            return this.supertypeWithAnnotationOnField;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
        public TestSuperClassWithNameProperty getSupertypeWithAnnotationOnGetter() {
            return this.supertypeWithAnnotationOnGetter;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
        public TestSuperClassWithNameProperty getSupertypeWithAnnotationOnFieldAndGetter() {
            return this.supertypeWithAnnotationOnFieldAndGetter;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(TestSubClass1.class),
        @JsonSubTypes.Type(TestSubClass2.class)
    })
    private static class TestSuperClassWithNameProperty {

        public String fullClass;
    }

    @JsonTypeName("SUB_CLASS_1")
    private static class TestSubClass1 extends TestSuperClassWithNameProperty {
    }

    @JsonTypeName("SUB_CLASS_2")
    private static class TestSubClass2 extends TestSuperClassWithNameProperty {
    }
}
