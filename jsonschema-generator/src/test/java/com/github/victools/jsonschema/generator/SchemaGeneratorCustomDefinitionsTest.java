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

package com.github.victools.jsonschema.generator;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.SchemaDefinitionNamingStrategy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for {@link SchemaGenerator} class.
 */
public class SchemaGeneratorCustomDefinitionsTest {

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testGenerateSchema_CustomDefinition(SchemaVersion schemaVersion) throws Exception {
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> javaType.getErasedType() == Integer.class
                ? new CustomDefinition(context.createDefinition(context.getTypeContext().resolve(String.class)))
                : null;
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion);
        configBuilder.forTypesInGeneral().withCustomDefinitionProvider(customDefinitionProvider);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        JsonNode result = generator.generateSchema(Integer.class);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(schemaVersion),
                result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testGenerateSchema_CustomCollectionDefinition(SchemaVersion schemaVersion) throws Exception {
        String accessProperty = "stream().findFirst().orElse(null)";
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> {
            if (!javaType.isInstanceOf(Collection.class)) {
                return null;
            }
            ResolvedType generic = context.getTypeContext().getContainerItemType(javaType);
            SchemaGeneratorConfig config = context.getGeneratorConfig();
            return new CustomDefinition(context.getGeneratorConfig().createObjectNode()
                    .put(config.getKeyword(SchemaKeyword.TAG_TYPE), config.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                    .set(config.getKeyword(SchemaKeyword.TAG_PROPERTIES), config.createObjectNode()
                            .set(accessProperty, context.makeNullable(context.createDefinition(generic)))));
        };
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion);
        configBuilder.forTypesInGeneral().withCustomDefinitionProvider(customDefinitionProvider);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        JsonNode result = generator.generateSchema(ArrayList.class, String.class);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(SchemaKeyword.TAG_TYPE_OBJECT.forVersion(schemaVersion),
                result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        Assertions.assertNotNull(result.get(SchemaKeyword.TAG_PROPERTIES.forVersion(schemaVersion)));
        Assertions.assertNotNull(result.get(SchemaKeyword.TAG_PROPERTIES.forVersion(schemaVersion)).get(accessProperty));
        JsonNode accessPropertyType = result.get(SchemaKeyword.TAG_PROPERTIES.forVersion(schemaVersion))
                .get(accessProperty).get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion));
        Assertions.assertNotNull(accessPropertyType);
        Assertions.assertEquals(SchemaKeyword.TAG_TYPE_STRING.forVersion(schemaVersion), accessPropertyType.get(0).asText());
        Assertions.assertEquals(SchemaKeyword.TAG_TYPE_NULL.forVersion(schemaVersion), accessPropertyType.get(1).asText());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testGenerateSchema_CustomInlineStandardDefinition(SchemaVersion schemaVersion) throws Exception {
        CustomDefinitionProviderV2 customDefinitionProvider = new CustomDefinitionProviderV2() {
            @Override
            public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
                if (javaType.getErasedType() == Integer.class) {
                    // using SchemaGenerationContext.createStandardDefinition() to avoid endless loop with this custom definition
                    ObjectNode standardDefinition = context.createStandardDefinition(context.getTypeContext().resolve(Integer.class), this);
                    standardDefinition.put("$comment", "custom override of Integer");
                    standardDefinition.put(context.getKeyword(SchemaKeyword.TAG_TITLE), "custom title");
                    return new CustomDefinition(standardDefinition);
                }
                return null;
            }
        };
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion);
        configBuilder.forTypesInGeneral()
                .withTitleResolver(_scope -> "type title")
                .withDescriptionResolver(_scope -> "type description")
                .withCustomDefinitionProvider(customDefinitionProvider);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        JsonNode result = generator.generateSchema(Integer.class);
        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals(SchemaKeyword.TAG_TYPE_INTEGER.forVersion(schemaVersion),
                result.get(SchemaKeyword.TAG_TYPE.forVersion(schemaVersion)).asText());
        Assertions.assertEquals("custom override of Integer", result.get("$comment").asText());
        Assertions.assertEquals("custom title", result.get(SchemaKeyword.TAG_TITLE.forVersion(schemaVersion)).asText());
        Assertions.assertEquals("type description", result.get(SchemaKeyword.TAG_DESCRIPTION.forVersion(schemaVersion)).asText());
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testGenerateSchema_CustomStandardDefinition(SchemaVersion schemaVersion) throws Exception {
        CustomDefinitionProviderV2 customDefinitionProviderOne = new CustomDefinitionProviderV2() {
            @Override
            public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
                ObjectNode customDefinition = context.getGeneratorConfig().createObjectNode()
                        .put(context.getKeyword(SchemaKeyword.TAG_TITLE),
                                "Custom Definition #1 for " + context.getTypeContext().getSimpleTypeDescription(javaType));
                // using SchemaGenerationContext.createStandardDefinitionReference() to avoid endless loop with this custom definition
                customDefinition.withArray(context.getKeyword(SchemaKeyword.TAG_ANYOF))
                        .add(context.createStandardDefinitionReference(javaType, this))
                        .addObject().put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
                return new CustomDefinition(customDefinition);
            }
        };
        CustomDefinitionProviderV2 customDefinitionProviderTwo = new CustomDefinitionProviderV2() {
            @Override
            public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
                if (javaType.getErasedType() == String.class) {
                    return null;
                }
                ObjectNode customDefinition = context.getGeneratorConfig().createObjectNode()
                        .put(context.getKeyword(SchemaKeyword.TAG_TITLE),
                                "Custom Definition #2 for " + context.getTypeContext().getFullTypeDescription(javaType));
                // using SchemaGenerationContext.createStandardDefinitionReference() to avoid endless loop with this custom definition
                customDefinition.withArray(context.getKeyword(SchemaKeyword.TAG_ANYOF))
                        .add(context.createStandardDefinitionReference(javaType, this))
                        .addObject().put(context.getKeyword(SchemaKeyword.TAG_TYPE), context.getKeyword(SchemaKeyword.TAG_TYPE_NULL));
                return new CustomDefinition(customDefinition);
            }
        };
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .without(Option.NULLABLE_FIELDS_BY_DEFAULT);
        configBuilder.forTypesInGeneral()
                .withCustomDefinitionProvider(customDefinitionProviderOne)
                .withCustomDefinitionProvider(customDefinitionProviderTwo)
                .withDefinitionNamingStrategy(new SchemaDefinitionNamingStrategy() {
                    @Override
                    public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext generationContext) {
                        return key.getType().getErasedType().getSimpleName().toLowerCase();
                    }

                    @Override
                    public void adjustDuplicateNames(Map<DefinitionKey, String> duplicateNames, SchemaGenerationContext context) {
                        char suffix = 'a';
                        for (Map.Entry<DefinitionKey, String> singleEntry : duplicateNames.entrySet()) {
                            singleEntry.setValue(singleEntry.getValue() + " (" + suffix + ")");
                            suffix++;
                        }
                    }
                });
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        JsonNode result = generator.generateSchema(TestDirectCircularClass.class);
        TestUtils.assertGeneratedSchema(result, this.getClass(), "multiple-definitions-one-type-" + schemaVersion.name() + ".json");
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testGenerateSchema_CircularCustomStandardDefinition(SchemaVersion schemaVersion) throws Exception {
        String accessProperty = "get(0)";
        CustomDefinitionProviderV2 customDefinitionProvider = (javaType, context) -> {
            if (!javaType.isInstanceOf(List.class)) {
                return null;
            }
            ResolvedType generic = context.getTypeContext().getContainerItemType(javaType);
            SchemaGeneratorConfig config = context.getGeneratorConfig();
            return new CustomDefinition(config.createObjectNode()
                    .put(config.getKeyword(SchemaKeyword.TAG_TYPE), config.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))
                    .set(config.getKeyword(SchemaKeyword.TAG_PROPERTIES), config.createObjectNode()
                            .set(accessProperty, context.createDefinitionReference(generic))));
        };
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion);
        configBuilder.with(Option.INLINE_NULLABLE_SCHEMAS);
        configBuilder.forTypesInGeneral()
                .withCustomDefinitionProvider(customDefinitionProvider);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        JsonNode result = generator.generateSchema(TestCircularClass1.class);
        TestUtils.assertGeneratedSchema(result, this.getClass(), "circular-custom-definition-" + schemaVersion.name() + ".json");
    }

    @ParameterizedTest
    @EnumSource(SchemaVersion.class)
    public void testGenerateSchema_CustomPropertyDefinition(SchemaVersion schemaVersion) throws Exception {
        CustomPropertyDefinitionProvider<FieldScope> customPropertyDefinitionProvider = (field, context) -> {
            if (field.getType().getErasedType() == int.class) {
                return new CustomPropertyDefinition(context.createDefinition(field.getType())
                        .put(context.getKeyword(SchemaKeyword.TAG_TITLE), "custom title"));
            }
            if (field.getType().getErasedType() == String.class) {
                return new CustomPropertyDefinition(context.createDefinition(field.getType())
                        .put(context.getKeyword(SchemaKeyword.TAG_DESCRIPTION), "custom description"));
            }
            // avoid an endless loop by including a reference (almost equivalent and usually better: just return null)
            switch (field.getName()) {
            case "selfCustomRefWithAttributes":
                // blocks all member attributes
                return new CustomPropertyDefinition(context.createDefinitionReference(field.getType()), CustomDefinition.AttributeInclusion.YES);
            case "selfCustomRefNoAttributes":
                // duplicates type attributes that are also in the referenced schema
                return new CustomPropertyDefinition(context.createDefinitionReference(field.getType()), CustomDefinition.AttributeInclusion.NO);
            default:
                // includes member attributes, but leaves type attributes in the referenced schema
                return null;
            }
        };
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(schemaVersion, OptionPreset.PLAIN_JSON);
        configBuilder.forTypesInGeneral()
                .withTitleResolver(_scope -> "type title");
        configBuilder.forFields()
                .withDescriptionResolver(_field -> "field description")
                .withCustomDefinitionProvider(customPropertyDefinitionProvider);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        JsonNode result = generator.generateSchema(TestDirectCircularClassMultipleProperties.class);
        TestUtils.assertGeneratedSchema(result, this.getClass(), "custom-property-definition-" + schemaVersion.name() + ".json");
    }

    @Test
    public void testGenerateSchema_CustomPropertyDefinitionForVoidMethod() throws Exception {
        CustomPropertyDefinitionProvider<MethodScope> customPropertyDefinitionProvider = (method, context) -> {
            if (method.isVoid()) {
                return new CustomPropertyDefinition(context.getGeneratorConfig().createObjectNode()
                        .put(context.getKeyword(SchemaKeyword.TAG_DESCRIPTION), "this method is void"));
            }
            return null;
        };
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.JAVA_OBJECT);
        configBuilder.with(Option.VOID_METHODS);
        configBuilder.forMethods()
                .withCustomDefinitionProvider(customPropertyDefinitionProvider);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        JsonNode result = generator.generateSchema(TestClassWithVoidMethod.class);
        JSONAssert.assertEquals("{\"type\":\"object\",\"properties\":{\"updateSomething()\":{\"description\":\"this method is void\"}}}",
                result.toString(), JSONCompareMode.STRICT);
    }

    private static class TestDirectCircularClass {

        public int number;
        public TestDirectCircularClass self;
        public String text;
    }

    private static class TestDirectCircularClassMultipleProperties {

        public int number;
        public TestDirectCircularClassMultipleProperties selfStandardRef;
        public TestDirectCircularClassMultipleProperties selfCustomRefWithAttributes;
        public TestDirectCircularClassMultipleProperties selfCustomRefNoAttributes;
        public String text;
    }

    private static class TestCircularClass1 {

        public List<TestCircularClass2> list2;

    }

    private static class TestCircularClass2 {

        public List<TestCircularClass1> list1;

    }

    private static class TestClassWithVoidMethod {
        public void updateSomething() {
            // perform an action
        }
    }
}
