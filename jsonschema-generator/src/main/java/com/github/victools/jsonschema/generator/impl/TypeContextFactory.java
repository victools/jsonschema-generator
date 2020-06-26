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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.classmate.AnnotationConfiguration;
import com.fasterxml.classmate.AnnotationInclusion;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.TypeContext;

/**
 * Factory class for creating {@link TypeContext} instances.
 */
public class TypeContextFactory {

    /**
     * Create the default {@link TypeContext} with {@link AnnotationInclusion#INCLUDE_AND_INHERIT_IF_INHERITED}.
     * <br>
     * This is equivalent to calling: {@code TypeContextFactory.createTypeContext(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED)}
     *
     * @return created {@link TypeContext} instance
     */
    public static TypeContext createDefaultTypeContext() {
        return TypeContextFactory.createTypeContext(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED);
    }

    /**
     * Create the default {@link TypeContext} with {@link AnnotationInclusion#INCLUDE_AND_INHERIT_IF_INHERITED}.<br>
     * This is equivalent to calling: {@code TypeContextFactory.createTypeContext(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED)}
     *
     * @param config configuration to consider
     * @return created {@link TypeContext} instance
     */
    public static TypeContext createDefaultTypeContext(SchemaGeneratorConfig config) {
        return TypeContextFactory.createTypeContext(AnnotationInclusion.INCLUDE_AND_INHERIT_IF_INHERITED, config);
    }

    /**
     * Create the a {@link TypeContext} with the given {@link AnnotationInclusion}.
     * <br>
     * This is equivalent to calling: {@code TypeContextFactory.createTypeContext(new AnnotationConfiguration.StdConfiguration(annotationInclusion))}
     *
     * @param annotationInclusion indication which annotations to include during type resolution/introspection
     * @return created {@link TypeContext} instance
     */
    public static TypeContext createTypeContext(AnnotationInclusion annotationInclusion) {
        return TypeContextFactory.createTypeContext(new AnnotationConfiguration.StdConfiguration(annotationInclusion));
    }

    /**
     * Create the a {@link TypeContext} with the given {@link AnnotationInclusion}.
     * <br>
     * This is equivalent to calling: {@code TypeContextFactory.createTypeContext(new AnnotationConfiguration.StdConfiguration(annotationInclusion))}
     *
     * @param annotationInclusion indication which annotations to include during type resolution/introspection
     * @param config configuration to consider
     * @return created {@link TypeContext} instance
     */
    public static TypeContext createTypeContext(AnnotationInclusion annotationInclusion, SchemaGeneratorConfig config) {
        return TypeContextFactory.createTypeContext(new AnnotationConfiguration.StdConfiguration(annotationInclusion), config);
    }

    /**
     * Create the a {@link TypeContext} with the given {@link AnnotationConfiguration}.
     *
     * @param annotationConfig configuration determining which annotations to include during type resolution/introspection
     * @return created {@link TypeContext} instance
     */
    public static TypeContext createTypeContext(AnnotationConfiguration annotationConfig) {
        return new TypeContext(annotationConfig);
    }

    /**
     * Create the a {@link TypeContext} with the given {@link AnnotationConfiguration}.
     *
     * @param annotationConfig configuration determining which annotations to include during type resolution/introspection
     * @param config configuration to consider
     * @return created {@link TypeContext} instance
     */
    public static TypeContext createTypeContext(AnnotationConfiguration annotationConfig, SchemaGeneratorConfig config) {
        return new TypeContext(annotationConfig, config);
    }
}
