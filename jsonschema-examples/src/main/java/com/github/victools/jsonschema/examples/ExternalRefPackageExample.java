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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/issues/248">#248</a>.
 * <br/>
 * For use via the Maven plugin where schemas for all types in a given package will be generated and should reference each other.
 */
public class ExternalRefPackageExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfig config = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new ModuleImpl())
                .build();
        return new SchemaGenerator(config).generateSchema(Example.class);
    }

    /**
     * Wrapped as module with zero-parameter constructor for use via the Maven plugin.
     */
    public static class ModuleImpl implements Module {

        private static final String PACKAGE_FOR_EXTERNAL_REFS = "com.github.victools.jsonschema.example";
        private static final String EXTERNAL_REF_PREFIX = "http://foo.bar/";

        @Override
        public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
            SchemaRefDefinitionProvider definitionProvider = new SchemaRefDefinitionProvider(PACKAGE_FOR_EXTERNAL_REFS, EXTERNAL_REF_PREFIX);
            builder.forTypesInGeneral()
                    .withCustomDefinitionProvider(definitionProvider)
                    .withIdResolver(scope -> definitionProvider.isMainType(scope.getType())
                            ? definitionProvider.getExternalRef(scope.getType()) : null);
        }

        static class SchemaRefDefinitionProvider implements CustomDefinitionProviderV2 {

            private final String packageForExternalRefs;
            private final String externalRefPrefix;
            private Class<?> mainType;

            SchemaRefDefinitionProvider(String packageForExternalRefs, String externalRefPrefix) {
                this.packageForExternalRefs = packageForExternalRefs;
                this.externalRefPrefix = externalRefPrefix;
            }

            @Override
            public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
                Class<?> erasedType = javaType.getErasedType();
                if (this.mainType == null) {
                    this.mainType = erasedType;
                } else if (!this.isMainType(javaType)
                        && erasedType.getPackage() != null
                        && erasedType.getPackage().getName().startsWith(this.packageForExternalRefs)) {
                    ObjectNode schema = context.getGeneratorConfig().createObjectNode()
                            .put(context.getKeyword(SchemaKeyword.TAG_REF), this.getExternalRef(javaType));
                    return new CustomDefinition(schema, CustomDefinition.INLINE_DEFINITION, CustomDefinition.INCLUDING_ATTRIBUTES);
                }
                return null;
            }

            boolean isMainType(ResolvedType javaType) {
                return this.mainType == javaType.getErasedType();
            }

            String getExternalRef(ResolvedType javaType) {
                return this.externalRefPrefix + javaType.getErasedType().getName();
            }

            @Override
            public void resetAfterSchemaGenerationFinished() {
                this.mainType = null;
            }
        }
    }

    static class Example {
        public String text;
        public Foo foo;
        public Bar bar;
    }

    static class Foo {
    }

    static class Bar {
    }
}
