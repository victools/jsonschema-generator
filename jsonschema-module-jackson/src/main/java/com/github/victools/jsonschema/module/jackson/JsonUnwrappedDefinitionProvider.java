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

package com.github.victools.jsonschema.module.jackson;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedMember;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Definition provider handling the integration of properties with the {@link JsonUnwrapped} annotation.
 *
 * @since 4.32.0
 */
public class JsonUnwrappedDefinitionProvider implements CustomDefinitionProviderV2 {

    @Override
    public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
        if (javaType == null) {
            // since 4.37.0: not for void methods
            return null;
        }
        ResolvedTypeWithMembers typeWithMembers = context.getTypeContext().resolveWithMembers(javaType);

        if (Arrays.stream(typeWithMembers.getMemberFields()).noneMatch(this::hasJsonUnwrappedAnnotation)
                && Arrays.stream(typeWithMembers.getMemberMethods()).noneMatch(this::hasJsonUnwrappedAnnotation)) {
            // no need for custom handling here, if no relevant annotation is present
            return null;
        }
        // include the target type itself (assuming the annotated members are being ignored then)
        ObjectNode definition = context.createStandardDefinition(javaType, this);
        ArrayNode allOf = definition.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF));

        // include each annotated member's type considering the optional prefix and/or suffix
        Stream.concat(Stream.of(typeWithMembers.getMemberFields()), Stream.of(typeWithMembers.getMemberMethods()))
                .map(member -> this.createUnwrappedMemberSchema(member, context))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEachOrdered(allOf::add);

        return new CustomDefinition(definition);
    }

    /**
     * Check whether the given field/method's type should be "unwrapped", i.e., elevating their properties to this member's type.
     *
     * @param member field/method to check
     * @return whether the given member has an {@code enabled} {@link JsonUnwrapped @JsonUnwrapped} annotation
     */
    private boolean hasJsonUnwrappedAnnotation(ResolvedMember<?> member) {
        return lookupEnabledJsonUnwrappedAnnotation(member.getAnnotations()).isPresent();
    }

    /**
     * Returns the first enabled occurrence of the {@link JsonUnwrapped} annotation found in the specified annotations parameter,
     * unwrapping "JacksonAnnotationsInside" combo annotations.
     * 
     * @param annotations annotations to crawl through
     * @return a present value if, and only if, an enabled instance was found.
     */
    private Optional<JsonUnwrapped> lookupEnabledJsonUnwrappedAnnotation(Iterable<Annotation> annotations) {
        Deque<Iterator<Annotation>> iterators = new LinkedList<>();
        iterators.add(annotations.iterator());
        while (!iterators.isEmpty()) {
            Iterator<Annotation> iterator = iterators.peek();
            if (!iterator.hasNext()) {
                iterators.remove();
                continue;
            }
            Annotation annotation = iterator.next();
            if (annotation instanceof JsonUnwrapped && ((JsonUnwrapped) annotation).enabled()) {
                return Optional.of((JsonUnwrapped) annotation);
            }
            final Class<? extends Annotation> annotationClass = annotation.annotationType();
            if (annotationClass.isAnnotationPresent(JacksonAnnotationsInside.class)) {
                iterators.addFirst(Arrays.asList(annotationClass.getAnnotations()).iterator());
            }
        }
        return Optional.empty();
    }
    
    /**
     * Create a schema representing an unwrapped member's type. Contained properties may get a certain prefix and/or suffix applied to their names.
     *
     * @param member field/method of which to unwrap the associated type
     * @param context generation context
     * @return created schema
     */
    private Optional<ObjectNode> createUnwrappedMemberSchema(ResolvedMember<?> member, SchemaGenerationContext context) {
        final Optional<JsonUnwrapped> optAnnotation = lookupEnabledJsonUnwrappedAnnotation(member.getAnnotations());
        return optAnnotation.map(annotation -> {
            ObjectNode definition = context.createStandardDefinition(member.getType(), null);
            if (!annotation.prefix().isEmpty() || !annotation.suffix().isEmpty()) {
                this.applyPrefixAndSuffixToPropertyNames(definition, annotation.prefix(), annotation.suffix(), context);
            }
            return definition;
        });
    }

    /**
     * Rename the properties defined in the given schema by prepending the given suffix and appending the given suffix.
     *
     * @param definition schema in which to alter contained properties' names
     * @param prefix prefix to prepend to all contained properties' names (may be an empty string)
     * @param suffix suffix to append to all contained properties' names (may be an empty string)
     * @param context generation context
     */
    private void applyPrefixAndSuffixToPropertyNames(JsonNode definition, String prefix, String suffix, SchemaGenerationContext context) {
        JsonNode properties = definition.get(context.getKeyword(SchemaKeyword.TAG_PROPERTIES));
        if (properties instanceof ObjectNode && !properties.isEmpty()) {
            List<String> fieldNames = new ArrayList<>();
            properties.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                JsonNode propertySchema = ((ObjectNode) properties).remove(fieldName);
                ((ObjectNode) properties).set(prefix + fieldName + suffix, propertySchema);
            }
        }
        JsonNode allOf = definition.get(context.getKeyword(SchemaKeyword.TAG_ALLOF));
        if (allOf instanceof ArrayNode) {
            // this only considers inlined parts and not any to-be-referenced subschema
            allOf.forEach(allOfEntry -> this.applyPrefixAndSuffixToPropertyNames(allOfEntry, prefix, suffix, context));
        }
        // keeping it simple for now (version 4.32.0) and not considering all potential nested properties
    }
}
