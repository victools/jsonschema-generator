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
            {"superTypeNoAnnotation", null, null},
            {"superTypeNoAnnotation", TestSubClass1.class, null},
            {"superTypeNoAnnotation", TestSubClass2.class, null},
            {"superTypeWithAnnotationOnField", null, null},
            {"superTypeWithAnnotationOnField", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"}}}]}"},
            {"superTypeWithAnnotationOnField", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"}}}]}"},
            {"superTypeWithAnnotationOnGetter", null, null},
            {"superTypeWithAnnotationOnGetter", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_1\"}}}]}"},
            {"superTypeWithAnnotationOnGetter", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_2\"}}}]}"},
            {"superTypeWithAnnotationOnFieldAndGetter", null, null},
            {"superTypeWithAnnotationOnFieldAndGetter", TestSubClass1.class,
                "{\"type\":\"array\",\"items\":[{\"type\":\"string\",\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"},"
                + "{\"allOf\":[{},{\"title\":\"property attribute\"}]}]}"},
            {"superTypeWithAnnotationOnFieldAndGetter", TestSubClass2.class,
                "{\"type\":\"array\",\"items\":[{\"type\":\"string\",\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"},{}]}"},
            {"superInterfaceWithAnnotationOnField", null, null},
            {"superInterfaceWithAnnotationOnField", TestSubClass3.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass3\"}}}]}"},
            {"superInterfaceWithAnnotationOnField", TestSubClass4.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass4\"}}}]}"}
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
            {"getSuperTypeNoAnnotation", null, null},
            {"getSuperTypeNoAnnotation", TestSubClass1.class, null},
            {"getSuperTypeNoAnnotation", TestSubClass2.class, null},
            {"getSuperTypeWithAnnotationOnField", null, null},
            {"getSuperTypeWithAnnotationOnField", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass1\"}}}]}"},
            {"getSuperTypeWithAnnotationOnField", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"fullClass\":{\"const\":"
                + "\"com.github.victools.jsonschema.module.jackson.JsonSubTypesResolverCustomDefinitionsTest$TestSubClass2\"}}}]}"},
            {"getSuperTypeWithAnnotationOnGetter", null, null},
            {"getSuperTypeWithAnnotationOnGetter", TestSubClass1.class,
                "{\"allOf\":[{},{\"title\":\"property attribute\",\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_1\"}}}]}"},
            {"getSuperTypeWithAnnotationOnGetter", TestSubClass2.class,
                "{\"allOf\":[{},{\"type\":\"object\",\"properties\":{\"@type\":{\"const\":\"SUB_CLASS_2\"}}}]}"},
            {"getSuperTypeWithAnnotationOnFieldAndGetter", null, null},
            {"getSuperTypeWithAnnotationOnFieldAndGetter", TestSubClass1.class,
                "{\"type\":\"object\",\"properties\":{\"SUB_CLASS_1\":{\"allOf\":[{},{\"title\":\"property attribute\"}]}}}"},
            {"getSuperTypeWithAnnotationOnFieldAndGetter", TestSubClass2.class,
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

        public TestSuperClassWithNameProperty superTypeNoAnnotation;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "fullClass", include = JsonTypeInfo.As.EXISTING_PROPERTY)
        public TestSuperClassWithNameProperty superTypeWithAnnotationOnField;
        public TestSuperClassWithNameProperty superTypeWithAnnotationOnGetter;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public TestSuperClassWithNameProperty superTypeWithAnnotationOnFieldAndGetter;
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "fullClass", include = JsonTypeInfo.As.PROPERTY)
        public TestSuperInterface superInterfaceWithAnnotationOnField;

        public TestSuperClassWithNameProperty getSuperTypeNoAnnotation() {
            return this.superTypeNoAnnotation;
        }

        public TestSuperClassWithNameProperty getSuperTypeWithAnnotationOnField() {
            return this.superTypeWithAnnotationOnField;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
        public TestSuperClassWithNameProperty getSuperTypeWithAnnotationOnGetter() {
            return this.superTypeWithAnnotationOnGetter;
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
        public TestSuperClassWithNameProperty getSuperTypeWithAnnotationOnFieldAndGetter() {
            return this.superTypeWithAnnotationOnFieldAndGetter;
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(TestSubClass3.class),
        @JsonSubTypes.Type(TestSubClass4.class)
    })
    private interface TestSuperInterface {
    }

    @JsonTypeName("SUB_CLASS_3")
    private static class TestSubClass3 implements TestSuperInterface {
    }

    @JsonTypeName("SUB_CLASS_4")
    private static class TestSubClass4 implements TestSuperInterface {
    }
}
