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

package com.github.victools.jsonschema.generator.impl;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.HierarchicType;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.TypeContext;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generation context for collecting the members of a single type.
 */
class MemberCollectionContextImpl {

    private static final Logger logger = LoggerFactory.getLogger(MemberCollectionContextImpl.class);

    private final ResolvedType schemaTargetType;
    private final SchemaGeneratorConfig generatorConfig;
    private final TypeContext typeContext;
    /*
     * Collecting fields and methods according to their order in the Java Byte Code.
     * Depending on the compiler implementation, that order may or may not match the declaration order in the source code.
     */
    private final Map<String, MemberScope<?, ?>> collectedProperties = new LinkedHashMap<>();
    private final Set<String> requiredPropertyNames = new HashSet<>();

    /**
     * Constructor initialising the context with an empty set of collected properties.
     *
     * @param schemaTargetType targeted type for which to collect the contained fields and methods
     * @param generatorConfig configuration object
     * @param typeContext type context to consider when creating the respective {@code FieldScope} and {@code MethodScope} instances
     */
    public MemberCollectionContextImpl(ResolvedType schemaTargetType, SchemaGeneratorConfig generatorConfig, TypeContext typeContext) {
        this.schemaTargetType = schemaTargetType;
        this.generatorConfig = generatorConfig;
        this.typeContext = typeContext;
    }

    /**
     * Retrieve the collected properties in the order as per the configured property sorting mechanism.
     *
     * @return sorted collected properties
     */
    public List<MemberScope<?, ?>> getSortedProperties() {
        return this.collectedProperties.values().stream()
                .sorted(this.generatorConfig::sortProperties)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the names (as to be included in the schema definition) of all required properties (in undefined order).
     *
     * @return names of required properties
     */
    public Set<String> getRequiredPropertyNames() {
        return Collections.unmodifiableSet(this.requiredPropertyNames);
    }

    /**
     * Recursively collect all properties of the given object type and add them to the respective maps.
     */
    public void collectProperties() {
        logger.debug("collecting non-static fields and methods from {}", this.schemaTargetType);
        ResolvedTypeWithMembers typeToCollectMembersFrom = this.typeContext.resolveWithMembers(this.schemaTargetType);
        MemberScope.DeclarationDetails declarationDetails = new MemberScope.DeclarationDetails(this.schemaTargetType, typeToCollectMembersFrom);
        // member fields and methods are being collected from the targeted type as well as its super types
        this.collectFields(typeToCollectMembersFrom.getMemberFields(), declarationDetails);
        this.collectMethods(typeToCollectMembersFrom.getMemberMethods(), declarationDetails);

        if (this.generatorConfig.shouldIncludeStaticFields() || this.generatorConfig.shouldIncludeStaticMethods()) {
            // static fields and methods are being collected only for the targeted type itself, i.e. need to iterate over super types specifically
            for (HierarchicType singleHierarchy : typeToCollectMembersFrom.allTypesAndOverrides()) {
                this.collectStaticMembers(singleHierarchy);
            }
        }
    }

    private void collectStaticMembers(HierarchicType singleHierarchy) {
        ResolvedType hierarchyType = singleHierarchy.getType();
        logger.debug("collecting static fields and methods from {}", hierarchyType);
        // static members need to be looked up from the declaring type directly as they are not inherited as such
        ResolvedTypeWithMembers hierarchyTypeWithMembers = this.typeContext.resolveWithMembers(hierarchyType);
        MemberScope.DeclarationDetails declarationDetails = new MemberScope.DeclarationDetails(this.schemaTargetType, hierarchyTypeWithMembers);
        if (this.generatorConfig.shouldIncludeStaticFields()) {
            this.collectFields(hierarchyTypeWithMembers.getStaticFields(), declarationDetails);
        }
        if (this.generatorConfig.shouldIncludeStaticMethods()) {
            this.collectMethods(hierarchyTypeWithMembers.getStaticMethods(), declarationDetails);
        }
    }

    /**
     * Preparation Step: collect the designated fields.
     *
     * @param fields targeted fields
     * @param declarationDetails common declaration context for all given fields
     */
    private void collectFields(ResolvedField[] fields, MemberScope.DeclarationDetails declarationDetails) {
        Stream.of(fields)
                .map(declaredField -> this.typeContext.createFieldScope(declaredField, declarationDetails))
                .map(this::getMemberWithNameOverride)
                .filter(fieldScope -> !this.generatorConfig.shouldIgnore(fieldScope))
                .forEach(this::collect);
    }

    /**
     * Preparation Step: collect the designated methods.
     *
     * @param methods targeted methods
     * @param declarationDetails common declaration context for all given methods
     */
    private void collectMethods(ResolvedMethod[] methods, MemberScope.DeclarationDetails declarationDetails) {
        Stream.of(methods)
                .map(declaredMethod -> this.typeContext.createMethodScope(declaredMethod, declarationDetails))
                .map(this::getMemberWithNameOverride)
                .filter(methodScope -> !this.generatorConfig.shouldIgnore(methodScope))
                .forEach(this::collect);
    }

    /**
     * Add the given field or method to this context's list of collected properties.
     *
     * @param member field/method to add
     */
    public void collect(MemberScope<?, ?> member) {
        String propertyName = member.getSchemaPropertyName();
        if (member.isFakeContainerItemScope()) {
            this.collectedProperties.put(propertyName, member);
            return;
        }
        this.registerIfRequired(member);
        if (this.collectedProperties.containsKey(propertyName)) {
            logger.debug("ignoring overridden {}.{}", member.getDeclaringType(), member.getDeclaredName());
        } else {
            this.collectedProperties.put(propertyName, member);
        }
    }

    private <M extends MemberScope<?, ?>> M getMemberWithNameOverride(M member) {
        String propertyNameOverride = member.getContext().performActionOnMember(member,
                this.generatorConfig::resolvePropertyNameOverride, this.generatorConfig::resolvePropertyNameOverride);
        if (propertyNameOverride == null) {
            return member;
        }
        return (M) member.withOverriddenName(propertyNameOverride);
    }

    private void registerIfRequired(MemberScope<?, ?> member) {
        if (member.getContext().performActionOnMember(member, this.generatorConfig::isRequired, this.generatorConfig::isRequired)) {
            this.requiredPropertyNames.add(member.getSchemaPropertyName());
        }
    }
}
