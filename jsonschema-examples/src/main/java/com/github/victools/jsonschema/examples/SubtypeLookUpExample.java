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
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.SubtypeResolver;
import com.github.victools.jsonschema.generator.TypeContext;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Example created to show-case generic subtype look-up.
 * <br/>
 * Using classgraph for determining type hierarchy.
 */
public class SubtypeLookUpExample implements SchemaGenerationExampleInterface {

    @Override
    public ObjectNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS, Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES);
        configBuilder.forTypesInGeneral()
                .withSubtypeResolver(new ClassGraphSubtypeResolver());
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(Example.class);
    }

    /**
     * Simple implementation of a reflection based subtype resolver, considering only subtypes from a certain package.
     */
    static class ClassGraphSubtypeResolver implements SubtypeResolver {

        private final ClassGraph classGraphConfig;
        private ScanResult scanResult;

        ClassGraphSubtypeResolver() {
            this.classGraphConfig = new ClassGraph()
                    .enableClassInfo()
                    .enableInterClassDependencies()
                    // in this example, only consider a certain set of potential subtypes
                    .acceptPackages("com.github.victools.jsonschema.examples");
        }

        private ScanResult getScanResult() {
            if (this.scanResult == null) {
                this.scanResult = this.classGraphConfig.scan();
            }
            return this.scanResult;
        }

        @Override
        public void resetAfterSchemaGenerationFinished() {
            if (this.scanResult != null) {
                this.scanResult.close();
                this.scanResult = null;
            }
        }

        @Override
        public List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
            if (declaredType.getErasedType() == Object.class) {
                return null;
            }
            ClassInfoList subtypes;
            if (declaredType.isInterface()) {
                subtypes = this.getScanResult().getClassesImplementing(declaredType.getErasedType());
            } else {
                subtypes = this.getScanResult().getSubclasses(declaredType.getErasedType());
            }
            if (!subtypes.isEmpty()) {
                TypeContext typeContext = context.getTypeContext();
                return subtypes.loadClasses(true)
                        .stream()
                        .map(subclass -> typeContext.resolveSubtype(declaredType, subclass))
                        .collect(Collectors.toList());
            }
            return null;
        }
    }

    static class Example {
        public BaseType declaredAsBaseType;
    }

    interface BaseType {
    }

    static class SubType1 implements BaseType {
        public String text;
    }

    static class SubType2 implements BaseType {
        public List<BaseType> nested;
    }
}
