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

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import java.time.LocalDate;
import tools.jackson.databind.node.ObjectNode;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/issues/407">#407</a>.
 * <br>
 * Reference separate definitions for common super types. Show-casing capabilities; but not recommended due to risk of infinite loop.
 */
public class InheritanceRefExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
        // ignore all fields declared in super types
        configBuilder.forFields()
                .withIgnoreCheck(field -> !field.getDeclaringType().equals(field.getDeclarationDetails().getSchemaTargetType()));
        // reference super types explicitly
        configBuilder.forTypesInGeneral()
                // including SubtypeResolver would result in a StackOverflowError as the super reference is again replaced with it subtypes
                //.withSubtypeResolver(new SubtypeLookUpExample.ClassGraphSubtypeResolver())
                .withCustomDefinitionProvider(new ParentReferenceDefinitionProvider());
        SchemaGeneratorConfig config = configBuilder.build();
        return new SchemaGenerator(config).generateSchema(TestType.class);
    }

    static class ParentReferenceDefinitionProvider implements CustomDefinitionProviderV2 {
        @Override
        public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
            if (javaType.getErasedType().getSuperclass() == null || javaType.getErasedType().getSuperclass() == Object.class) {
                return null;
            }
            ResolvedType superType = context.getTypeContext().resolve(javaType.getErasedType().getSuperclass());
            // two options here:
            // 1. providing "null" as second parameter = multiple hierarchy levels are respected
            // 2. providing "this" as second parameter = only one hierarchy level is respected
            ObjectNode superTypeReference = context.createStandardDefinitionReference(superType, null);
            ObjectNode definition = context.createStandardDefinition(javaType, this);
            definition.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF))
                    .add(superTypeReference);
            return new CustomDefinition(definition, CustomDefinition.DefinitionType.STANDARD, CustomDefinition.AttributeInclusion.YES);
        }
    }

    static class TestType {
        public Book favoriteBook;
        public Poster favoritePoster;
    }

    static class Book extends PrintPublication {
        public int pageCount;
    }

    static class Poster extends PrintPublication {
        public boolean forMovie;
    }

    static class PrintPublication extends Publication {
        public String author;
        public String publisher;
    }

    static class Publication {
        public LocalDate published;
        public String title;
    }
}
