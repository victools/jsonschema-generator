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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return node holding all collected attributes (possibly empty)
     */
    public static ObjectNode collectFieldAttributes(FieldScope field, SchemaGenerationContextImpl generationContext) {
        SchemaGeneratorConfig config = generationContext.getGeneratorConfig();
        ObjectNode node = config.createObjectNode();
        AttributeCollector collector = new AttributeCollector(config.getObjectMapper());
        collector.setTitle(node, config.resolveTitle(field));
        collector.setDescription(node, config.resolveDescription(field));
        collector.setDefault(node, config.resolveDefault(field));
        collector.setEnum(node, config.resolveEnum(field));
        collector.setAdditionalProperties(node, config.resolveAdditionalProperties(field), generationContext);
        collector.setPatternProperties(node, config.resolvePatternProperties(field), generationContext);
        collector.setStringMinLength(node, config.resolveStringMinLength(field));
        collector.setStringMaxLength(node, config.resolveStringMaxLength(field));
        collector.setStringFormat(node, config.resolveStringFormat(field));
        collector.setStringPattern(node, config.resolveStringPattern(field));
        collector.setNumberInclusiveMinimum(node, config.resolveNumberInclusiveMinimum(field));
        collector.setNumberExclusiveMinimum(node, config.resolveNumberExclusiveMinimum(field));
        collector.setNumberInclusiveMaximum(node, config.resolveNumberInclusiveMaximum(field));
        collector.setNumberExclusiveMaximum(node, config.resolveNumberExclusiveMaximum(field));
        collector.setNumberMultipleOf(node, config.resolveNumberMultipleOf(field));
        collector.setArrayMinItems(node, config.resolveArrayMinItems(field));
        collector.setArrayMaxItems(node, config.resolveArrayMaxItems(field));
        collector.setArrayUniqueItems(node, config.resolveArrayUniqueItems(field));
        config.getFieldAttributeOverrides()
                .forEach(override -> override.overrideInstanceAttributes(node, field));
        return node;
    }

    /**
     * Collect a method's contextual attributes (i.e. everything not related to the structure).
     *
     * @param method the method for which to collect JSON schema attributes
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return node holding all collected attributes (possibly empty)
     */
    public static ObjectNode collectMethodAttributes(MethodScope method, SchemaGenerationContextImpl generationContext) {
        SchemaGeneratorConfig config = generationContext.getGeneratorConfig();
        ObjectNode node = config.createObjectNode();
        AttributeCollector collector = new AttributeCollector(config.getObjectMapper());
        collector.setTitle(node, config.resolveTitle(method));
        collector.setDescription(node, config.resolveDescription(method));
        collector.setDefault(node, config.resolveDefault(method));
        collector.setEnum(node, config.resolveEnum(method));
        collector.setAdditionalProperties(node, config.resolveAdditionalProperties(method), generationContext);
        collector.setPatternProperties(node, config.resolvePatternProperties(method), generationContext);
        collector.setStringMinLength(node, config.resolveStringMinLength(method));
        collector.setStringMaxLength(node, config.resolveStringMaxLength(method));
        collector.setStringFormat(node, config.resolveStringFormat(method));
        collector.setStringPattern(node, config.resolveStringPattern(method));
        collector.setNumberInclusiveMinimum(node, config.resolveNumberInclusiveMinimum(method));
        collector.setNumberExclusiveMinimum(node, config.resolveNumberExclusiveMinimum(method));
        collector.setNumberInclusiveMaximum(node, config.resolveNumberInclusiveMaximum(method));
        collector.setNumberExclusiveMaximum(node, config.resolveNumberExclusiveMaximum(method));
        collector.setNumberMultipleOf(node, config.resolveNumberMultipleOf(method));
        collector.setArrayMinItems(node, config.resolveArrayMinItems(method));
        collector.setArrayMaxItems(node, config.resolveArrayMaxItems(method));
        collector.setArrayUniqueItems(node, config.resolveArrayUniqueItems(method));
        config.getMethodAttributeOverrides()
                .forEach(override -> override.overrideInstanceAttributes(node, method));
        return node;
    }

    /**
     * Collect a given scope's general type attributes (i.e. everything not related to the structure).
     *
     * @param scope the scope/type representation for which to collect JSON schema attributes
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return node holding all collected attributes (possibly empty)
     */
    public static ObjectNode collectTypeAttributes(TypeScope scope, SchemaGenerationContextImpl generationContext) {
        SchemaGeneratorConfig config = generationContext.getGeneratorConfig();
        ObjectNode node = config.createObjectNode();
        AttributeCollector collector = new AttributeCollector(config.getObjectMapper());
        collector.setTitle(node, config.resolveTitleForType(scope));
        collector.setDescription(node, config.resolveDescriptionForType(scope));
        collector.setDefault(node, config.resolveDefaultForType(scope));
        collector.setEnum(node, config.resolveEnumForType(scope));
        collector.setAdditionalProperties(node, config.resolveAdditionalPropertiesForType(scope), generationContext);
        collector.setPatternProperties(node, config.resolvePatternPropertiesForType(scope), generationContext);
        collector.setStringMinLength(node, config.resolveStringMinLengthForType(scope));
        collector.setStringMaxLength(node, config.resolveStringMaxLengthForType(scope));
        collector.setStringFormat(node, config.resolveStringFormatForType(scope));
        collector.setStringPattern(node, config.resolveStringPatternForType(scope));
        collector.setNumberInclusiveMinimum(node, config.resolveNumberInclusiveMinimumForType(scope));
        collector.setNumberExclusiveMinimum(node, config.resolveNumberExclusiveMinimumForType(scope));
        collector.setNumberInclusiveMaximum(node, config.resolveNumberInclusiveMaximumForType(scope));
        collector.setNumberExclusiveMaximum(node, config.resolveNumberExclusiveMaximumForType(scope));
        collector.setNumberMultipleOf(node, config.resolveNumberMultipleOfForType(scope));
        collector.setArrayMinItems(node, config.resolveArrayMinItemsForType(scope));
        collector.setArrayMaxItems(node, config.resolveArrayMaxItemsForType(scope));
        collector.setArrayUniqueItems(node, config.resolveArrayUniqueItemsForType(scope));
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
     * Setter for "{@value SchemaConstants#TAG_DEFAULT}" attribute.
     *
     * @param node schema node to set attribute on
     * @param defaultValue attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setDefault(ObjectNode node, Object defaultValue) {
        if (defaultValue != null) {
            // need to specifically add simple/primitive values by type
            if (defaultValue instanceof String) {
                node.put(SchemaConstants.TAG_DEFAULT, (String) defaultValue);
            } else if (defaultValue instanceof BigDecimal) {
                node.put(SchemaConstants.TAG_DEFAULT, (BigDecimal) defaultValue);
            } else if (defaultValue instanceof BigInteger) {
                node.put(SchemaConstants.TAG_DEFAULT, (BigInteger) defaultValue);
            } else if (defaultValue instanceof Boolean) {
                node.put(SchemaConstants.TAG_DEFAULT, (Boolean) defaultValue);
            } else if (defaultValue instanceof Double) {
                node.put(SchemaConstants.TAG_DEFAULT, (Double) defaultValue);
            } else if (defaultValue instanceof Float) {
                node.put(SchemaConstants.TAG_DEFAULT, (Float) defaultValue);
            } else if (defaultValue instanceof Integer) {
                node.put(SchemaConstants.TAG_DEFAULT, (Integer) defaultValue);
            } else {
                // everything else is simply forwarded as-is to the JSON Schema, it's up to the configurator to ensure the value's correctness
                node.putPOJO(SchemaConstants.TAG_DEFAULT, defaultValue);
            }
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
            List<Object> values = enumValues.stream()
                    .filter(this::isSupportedEnumValue)
                    .filter(this::canBeConvertedToString)
                    .collect(Collectors.toList());
            if (values.size() == 1) {
                Object singleValue = values.get(0);
                if (singleValue instanceof String) {
                    node.put(SchemaConstants.TAG_CONST, (String) singleValue);
                } else {
                    node.putPOJO(SchemaConstants.TAG_CONST, singleValue);
                }
            } else if (!values.isEmpty()) {
                ArrayNode array = node.arrayNode();
                for (Object singleValue : values) {
                    if (singleValue instanceof String) {
                        array.add((String) singleValue);
                    } else {
                        array.addPOJO(singleValue);
                    }
                }
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
     * Call {@link ObjectMapper#writeValueAsString(Object)} on the given object and determining whether any {@link JsonProcessingException} occurs.
     *
     * @param target object to convert
     * @return whether the underlying ObjectMapper is able to convert the given object without encountering an exception
     */
    private boolean canBeConvertedToString(Object target) {
        try {
            return target == null || this.objectMapper.writeValueAsString(target) != null;
        } catch (JsonProcessingException ex) {
            logger.warn("Failed to convert value to string via ObjectMapper: {}", target, ex);
            return false;
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_ADDITIONAL_PROPERTIES}" attribute.
     *
     * @param node schema node to set attribute on
     * @param additionalProperties attribute value to set
     * @param generationContext generation context allowing for standard definitions to be included as attributes
     * @return this instance (for chaining)
     */
    public AttributeCollector setAdditionalProperties(ObjectNode node, Type additionalProperties, SchemaGenerationContextImpl generationContext) {
        if (additionalProperties == Void.class || additionalProperties == Void.TYPE) {
            node.put(SchemaConstants.TAG_ADDITIONAL_PROPERTIES, false);
        } else if (additionalProperties != null) {
            ResolvedType targetType = generationContext.getTypeContext().resolve(additionalProperties);
            if (targetType.getErasedType() != Object.class) {
                ObjectNode additionalPropertiesSchema = generationContext.getGeneratorConfig().createObjectNode();
                generationContext.traverseGenericType(targetType, additionalPropertiesSchema, false);
                node.set(SchemaConstants.TAG_ADDITIONAL_PROPERTIES, additionalPropertiesSchema);
            }
        }
        return this;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_PATTERN_PROPERTIES}" attribute.
     *
     * @param node schema node to set attribute on
     * @param patternProperties resolved attribute value to set
     * @param generationContext generation context allowing for standard definitions to be included as attributes
     * @return this instance (for chaining)
     */
    public AttributeCollector setPatternProperties(ObjectNode node, Map<String, Type> patternProperties,
            SchemaGenerationContextImpl generationContext) {
        if (patternProperties != null && !patternProperties.isEmpty()) {
            ObjectNode patternPropertiesNode = generationContext.getGeneratorConfig().createObjectNode();
            for (Map.Entry<String, Type> entry : patternProperties.entrySet()) {
                ObjectNode singlePatternSchema = generationContext.getGeneratorConfig().createObjectNode();
                ResolvedType targetType = generationContext.getTypeContext().resolve(entry.getValue());
                if (targetType.getErasedType() != Object.class) {
                    generationContext.traverseGenericType(targetType, singlePatternSchema, false);
                }
                patternPropertiesNode.set(entry.getKey(), singlePatternSchema);
            }
            node.set(SchemaConstants.TAG_PATTERN_PROPERTIES, patternPropertiesNode);
        }
        return this;
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
     * Setter for "{@value SchemaConstants#TAG_PATTERN}" attribute.
     *
     * @param node schema node to set attribute on
     * @param pattern attribute value to set
     * @return this instance (for chaining)
     */
    public AttributeCollector setStringPattern(ObjectNode node, String pattern) {
        if (pattern != null) {
            node.put(SchemaConstants.TAG_PATTERN, pattern);
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
