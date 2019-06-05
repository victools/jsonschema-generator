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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for looking-up various attribute values for a field or method via a given configuration instance.
 */
public class AttributeCollector {

    private static final Logger logger = LoggerFactory.getLogger(AttributeCollector.class);

    private final ObjectMapper objectMapper;

    /**
     * Constructor accepting the object mapper to use.
     *
     * @param objectMapper object mapper
     */
    public AttributeCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Collect a field's contextual attributes (i.e. everything not related to the structure).
     *
     * @param field the field for which to collect JSON schema attributes
     * @param type associated field type
     * @param declaringType origin's declaring type
     * @param config configuration to apply when looking-up attribute values
     * @return node holding all collected attributes (possibly empty)
     */
    public static ObjectNode collectFieldAttributes(ResolvedField field, ResolvedType type, ResolvedTypeWithMembers declaringType,
            SchemaGeneratorConfig config) {
        ObjectNode node = config.createObjectNode();
        AttributeCollector collector = new AttributeCollector(config.getObjectMapper());
        collector.setTitle(node, config.resolveTitle(field, type, declaringType));
        collector.setDescription(node, config.resolveDescription(field, type, declaringType));
        collector.setEnum(node, config.resolveEnum(field, type, declaringType));
        collector.setStringMinLength(node, config.resolveStringMinLength(field, type, declaringType));
        collector.setStringMaxLength(node, config.resolveStringMaxLength(field, type, declaringType));
        collector.setStringFormat(node, config.resolveStringFormat(field, type, declaringType));
        collector.setNumberInclusiveMinimum(node, config.resolveNumberInclusiveMinimum(field, type, declaringType));
        collector.setNumberExclusiveMinimum(node, config.resolveNumberExclusiveMinimum(field, type, declaringType));
        collector.setNumberInclusiveMaximum(node, config.resolveNumberInclusiveMaximum(field, type, declaringType));
        collector.setNumberExclusiveMaximum(node, config.resolveNumberExclusiveMaximum(field, type, declaringType));
        collector.setNumberMultipleOf(node, config.resolveNumberMultipleOf(field, type, declaringType));
        collector.setArrayMinItems(node, config.resolveArrayMinItems(field, type, declaringType));
        collector.setArrayMaxItems(node, config.resolveArrayMaxItems(field, type, declaringType));
        collector.setArrayUniqueItems(node, config.resolveArrayUniqueItems(field, type, declaringType));
        config.getFieldAttributeOverrides()
                .forEach(override -> override.overrideInstanceAttributes(node, field, type, declaringType, config));
        return node;
    }

    /**
     * Collect a method's contextual attributes (i.e. everything not related to the structure).
     *
     * @param method the method for which to collect JSON schema attributes
     * @param returnType associated return value type
     * @param declaringType origin's declaring type
     * @param config configuration to apply when looking-up attribute values
     * @return node holding all collected attributes (possibly empty)
     */
    public static ObjectNode collectMethodAttributes(ResolvedMethod method, ResolvedType returnType, ResolvedTypeWithMembers declaringType,
            SchemaGeneratorConfig config) {
        ObjectNode node = config.createObjectNode();
        AttributeCollector collector = new AttributeCollector(config.getObjectMapper());
        collector.setTitle(node, config.resolveTitle(method, returnType, declaringType));
        collector.setDescription(node, config.resolveDescription(method, returnType, declaringType));
        collector.setEnum(node, config.resolveEnum(method, returnType, declaringType));
        collector.setStringMinLength(node, config.resolveStringMinLength(method, returnType, declaringType));
        collector.setStringMaxLength(node, config.resolveStringMaxLength(method, returnType, declaringType));
        collector.setStringFormat(node, config.resolveStringFormat(method, returnType, declaringType));
        collector.setNumberInclusiveMinimum(node, config.resolveNumberInclusiveMinimum(method, returnType, declaringType));
        collector.setNumberExclusiveMinimum(node, config.resolveNumberExclusiveMinimum(method, returnType, declaringType));
        collector.setNumberInclusiveMaximum(node, config.resolveNumberInclusiveMaximum(method, returnType, declaringType));
        collector.setNumberExclusiveMaximum(node, config.resolveNumberExclusiveMaximum(method, returnType, declaringType));
        collector.setNumberMultipleOf(node, config.resolveNumberMultipleOf(method, returnType, declaringType));
        collector.setArrayMinItems(node, config.resolveArrayMinItems(method, returnType, declaringType));
        collector.setArrayMaxItems(node, config.resolveArrayMaxItems(method, returnType, declaringType));
        collector.setArrayUniqueItems(node, config.resolveArrayUniqueItems(method, returnType, declaringType));
        config.getMethodAttributeOverrides()
                .forEach(override -> override.overrideInstanceAttributes(node, method, returnType, declaringType, config));
        return node;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_TITLE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param title attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setTitle(ObjectNode node, String title) {
        if (title != null) {
            node.put(SchemaConstants.TAG_TITLE, title);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_DESCRIPTION}" attribute.
     *
     * @param node schema node to set attribute on
     * @param description attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setDescription(ObjectNode node, String description) {
        if (description != null) {
            node.put(SchemaConstants.TAG_DESCRIPTION, description);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_CONST}"/"{@value SchemaConstants#TAG_ENUM}" attribute.
     *
     * @param node schema node to set attribute on
     * @param enumValues attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setEnum(ObjectNode node, Collection<?> enumValues) {
        if (enumValues != null) {
            List<String> valuesAsString = enumValues.stream()
                    .filter(this::isSupportedEnumValue)
                    .map(this::convertObjectToString)
                    .collect(Collectors.toList());
            if (valuesAsString.size() == 1) {
                String singleValue = valuesAsString.get(0);
                node.putPOJO(SchemaConstants.TAG_CONST, singleValue);
            } else if (!valuesAsString.isEmpty()) {
                ArrayNode array = node.arrayNode();
                valuesAsString.forEach(array::addPOJO);
                node.set(SchemaConstants.TAG_ENUM, array);
            }
        }
        return this;
    }

    /**
     * Check whether the given object may be included in a "{@value SchemaConstants#TAG_CONST}"/"{@value SchemaConstants#TAG_ENUM}" attribute.
     *
     * @param target object to check
     * @return whether the given object may be included, otherwise it should be ignored
     */
    private boolean isSupportedEnumValue(Object target) {
        if (target == null) {
            return true;
        }
        Class<?> targetType = target.getClass();
        return targetType.isPrimitive()
                || Number.class.isAssignableFrom(targetType)
                || CharSequence.class.isAssignableFrom(targetType)
                || Enum.class.isAssignableFrom(targetType);
    }

    /**
     * Call {@link ObjectMapper#writeValueAsString(Object)} on the given object – ignoring any occurring {@link JsonProcessingException}.
     *
     * @param target object to convert
     * @return converted object
     */
    private String convertObjectToString(Object target) {
        try {
            return this.objectMapper.writeValueAsString(target);
        } catch (JsonProcessingException ex) {
            logger.warn("Failed to convert value to string via ObjectMapper: {}", target, ex);
            return target == null ? null : ('"' + target.toString() + '"');
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_LENGTH_MIN}" attribute.
     *
     * @param node schema node to set attribute on
     * @param minLength attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setStringMinLength(ObjectNode node, Integer minLength) {
        if (minLength != null) {
            node.put(SchemaConstants.TAG_LENGTH_MIN, minLength);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_LENGTH_MAX}" attribute.
     *
     * @param node schema node to set attribute on
     * @param maxLength attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setStringMaxLength(ObjectNode node, Integer maxLength) {
        if (maxLength != null) {
            node.put(SchemaConstants.TAG_LENGTH_MAX, maxLength);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_FORMAT}" attribute.
     *
     * @param node schema node to set attribute on
     * @param format attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setStringFormat(ObjectNode node, String format) {
        if (format != null) {
            node.put(SchemaConstants.TAG_FORMAT, format);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MINIMUM}" attribute.
     *
     * @param node schema node to set attribute on
     * @param inclusiveMinimum attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberInclusiveMinimum(ObjectNode node, BigDecimal inclusiveMinimum) {
        if (inclusiveMinimum != null) {
            node.put(SchemaConstants.TAG_MINIMUM, inclusiveMinimum);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MINIMUM_EXCLUSIVE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param exclusiveMinimum attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberExclusiveMinimum(ObjectNode node, BigDecimal exclusiveMinimum) {
        if (exclusiveMinimum != null) {
            node.put(SchemaConstants.TAG_MINIMUM_EXCLUSIVE, exclusiveMinimum);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MAXIMUM}" attribute.
     *
     * @param node schema node to set attribute on
     * @param inclusiveMaximum attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberInclusiveMaximum(ObjectNode node, BigDecimal inclusiveMaximum) {
        if (inclusiveMaximum != null) {
            node.put(SchemaConstants.TAG_MAXIMUM, inclusiveMaximum);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MAXIMUM_EXCLUSIVE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param exclusiveMaximum attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberExclusiveMaximum(ObjectNode node, BigDecimal exclusiveMaximum) {
        if (exclusiveMaximum != null) {
            node.put(SchemaConstants.TAG_MAXIMUM_EXCLUSIVE, exclusiveMaximum);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MULTIPLE_OF}" attribute.
     *
     * @param node schema node to set attribute on
     * @param multipleOf attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberMultipleOf(ObjectNode node, BigDecimal multipleOf) {
        if (multipleOf != null) {
            node.put(SchemaConstants.TAG_MULTIPLE_OF, multipleOf);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_ITEMS_MIN}" attribute.
     *
     * @param node schema node to set attribute on
     * @param minItemCount attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setArrayMinItems(ObjectNode node, Integer minItemCount) {
        if (minItemCount != null) {
            node.put(SchemaConstants.TAG_ITEMS_MIN, minItemCount);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_ITEMS_MAX}" attribute.
     *
     * @param node schema node to set attribute on
     * @param maxItemCount attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setArrayMaxItems(ObjectNode node, Integer maxItemCount) {
        if (maxItemCount != null) {
            node.put(SchemaConstants.TAG_ITEMS_MAX, maxItemCount);
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_ITEMS_UNIQUE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param uniqueItems attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setArrayUniqueItems(ObjectNode node, Boolean uniqueItems) {
        if (uniqueItems != null) {
            node.put(SchemaConstants.TAG_ITEMS_UNIQUE, uniqueItems);
        }
        return this;
    }
}
