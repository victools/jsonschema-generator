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
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.impl.TypeContextFactory;
import java.util.stream.Stream;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * Abstract test class catering for the type resolution of a dummy/test class to perform tests against introspected fields/methods.
 */
public class AbstractTypeAwareTest {

    private final Class<?> testClass;
    private SchemaGenerationContext context;
    private MemberScope.DeclarationDetails declarationDetails;

    protected AbstractTypeAwareTest(Class<?> testClass) {
        this.testClass = testClass;
    }

    /**
     * Override generation context mock methods that are version dependent.
     *
     * @param schemaVersion designated JSON Schema version
     */
    protected void prepareContextForVersion(SchemaVersion schemaVersion) {
        TypeContext typeContext = Mockito.spy(TypeContextFactory.createDefaultTypeContext(Mockito.mock(SchemaGeneratorConfig.class)));
        ResolvedType resolvedTestClass = typeContext.resolve(this.testClass);
        this.declarationDetails = new MemberScope.DeclarationDetails(resolvedTestClass, typeContext.resolveWithMembers(resolvedTestClass));
        this.context = Mockito.mock(SchemaGenerationContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(this.context.getTypeContext()).thenReturn(typeContext);
        ObjectMapper objectMapper = new ObjectMapper();
        Mockito.when(this.context.getGeneratorConfig().getObjectMapper()).thenReturn(objectMapper);
        Mockito.when(this.context.getGeneratorConfig().createArrayNode()).thenAnswer(_invocation -> objectMapper.createArrayNode());
        Mockito.when(this.context.getGeneratorConfig().createObjectNode()).thenAnswer(_invocation -> objectMapper.createObjectNode());
        Mockito.when(this.context.getGeneratorConfig().getSchemaVersion()).thenReturn(schemaVersion);

        Answer<String> keywordLookup = invocation -> ((SchemaKeyword) invocation.getArgument(0)).forVersion(schemaVersion);
        Mockito.when(this.context.getGeneratorConfig().getKeyword(Mockito.any())).thenAnswer(keywordLookup);
        Mockito.when(this.context.getKeyword(Mockito.any())).thenAnswer(keywordLookup);
    }

    protected SchemaGenerationContext getContext() {
        return this.context;
    }

    protected FieldScope getTestClassField(String fieldName) {
        ResolvedField resolvedField = Stream.of(this.declarationDetails.getDeclaringTypeMembers().getMemberFields())
                .filter(field -> fieldName.equals(field.getName()))
                .findAny()
                .orElseGet(
                        () -> Stream.of(this.declarationDetails.getDeclaringTypeMembers().getStaticFields())
                                .filter(field -> fieldName.equals(field.getName()))
                                .findAny()
                                .get());
        return this.context.getTypeContext().createFieldScope(resolvedField, this.declarationDetails);
    }

    protected MethodScope getTestClassMethod(String methodName) {
        ResolvedMethod resolvedMethod = Stream.of(this.declarationDetails.getDeclaringTypeMembers().getMemberMethods())
                .filter(method -> methodName.equals(method.getName()))
                .findAny()
                .orElseGet(
                        () -> Stream.of(this.declarationDetails.getDeclaringTypeMembers().getStaticMethods())
                                .filter(method -> methodName.equals(method.getName()))
                                .findAny()
                                .get());
        return this.context.getTypeContext().createMethodScope(resolvedMethod, this.declarationDetails);
    }
}
