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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public static ObjectNode collectFieldAttributes(FieldScope field, SchemaGenerationContext generationContext) {
        SchemaGeneratorConfig config = generationContext.getGeneratorConfig();
        ObjectNode node = config.createObjectNode();
        AttributeCollector collector = new AttributeCollector(config.getObjectMapper());
        collector.setTitle(node, config.resolveTitle(field), generationContext);
        collector.setDescription(node, config.resolveDescription(field), generationContext);
        collector.setDefault(node, config.resolveDefault(field), generationContext);
        collector.setEnum(node, config.resolveEnum(field), generationContext);
        collector.setReadOnly(node, config.isReadOnly(field), generationContext);
        collector.setWriteOnly(node, config.isWriteOnly(field), generationContext);
        collector.setAdditionalProperties(node, config.resolveAdditionalProperties(field), generationContext);
        collector.setPatternProperties(node, config.resolvePatternProperties(field), generationContext);
        collector.setStringMinLength(node, config.resolveStringMinLength(field), generationContext);
        collector.setStringMaxLength(node, config.resolveStringMaxLength(field), generationContext);
        collector.setStringFormat(node, config.resolveStringFormat(field), generationContext);
        collector.setStringPattern(node, config.resolveStringPattern(field), generationContext);
        collector.setNumberInclusiveMinimum(node, config.resolveNumberInclusiveMinimum(field), generationContext);
        collector.setNumberExclusiveMinimum(node, config.resolveNumberExclusiveMinimum(field), generationContext);
        collector.setNumberInclusiveMaximum(node, config.resolveNumberInclusiveMaximum(field), generationContext);
        collector.setNumberExclusiveMaximum(node, config.resolveNumberExclusiveMaximum(field), generationContext);
        collector.setNumberMultipleOf(node, config.resolveNumberMultipleOf(field), generationContext);
        collector.setArrayMinItems(node, config.resolveArrayMinItems(field), generationContext);
        collector.setArrayMaxItems(node, config.resolveArrayMaxItems(field), generationContext);
        collector.setArrayUniqueItems(node, config.resolveArrayUniqueItems(field), generationContext);
        config.getFieldAttributeOverrides()
                .forEach(override -> override.overrideInstanceAttributes(node, field, generationContext));
        return node;
    }

    /**
     * Collect a method's contextual attributes (i.e. everything not related to the structure).
     *
     * @param method the method for which to collect JSON schema attributes
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return node holding all collected attributes (possibly empty)
     */
    public static ObjectNode collectMethodAttributes(MethodScope method, SchemaGenerationContext generationContext) {
        SchemaGeneratorConfig config = generationContext.getGeneratorConfig();
        ObjectNode node = config.createObjectNode();
        AttributeCollector collector = new AttributeCollector(config.getObjectMapper());
        collector.setTitle(node, config.resolveTitle(method), generationContext);
        collector.setDescription(node, config.resolveDescription(method), generationContext);
        collector.setDefault(node, config.resolveDefault(method), generationContext);
        collector.setEnum(node, config.resolveEnum(method), generationContext);
        collector.setReadOnly(node, config.isReadOnly(method), generationContext);
        collector.setWriteOnly(node, config.isWriteOnly(method), generationContext);
        collector.setAdditionalProperties(node, config.resolveAdditionalProperties(method), generationContext);
        collector.setPatternProperties(node, config.resolvePatternProperties(method), generationContext);
        collector.setStringMinLength(node, config.resolveStringMinLength(method), generationContext);
        collector.setStringMaxLength(node, config.resolveStringMaxLength(method), generationContext);
        collector.setStringFormat(node, config.resolveStringFormat(method), generationContext);
        collector.setStringPattern(node, config.resolveStringPattern(method), generationContext);
        collector.setNumberInclusiveMinimum(node, config.resolveNumberInclusiveMinimum(method), generationContext);
        collector.setNumberExclusiveMinimum(node, config.resolveNumberExclusiveMinimum(method), generationContext);
        collector.setNumberInclusiveMaximum(node, config.resolveNumberInclusiveMaximum(method), generationContext);
        collector.setNumberExclusiveMaximum(node, config.resolveNumberExclusiveMaximum(method), generationContext);
        collector.setNumberMultipleOf(node, config.resolveNumberMultipleOf(method), generationContext);
        collector.setArrayMinItems(node, config.resolveArrayMinItems(method), generationContext);
        collector.setArrayMaxItems(node, config.resolveArrayMaxItems(method), generationContext);
        collector.setArrayUniqueItems(node, config.resolveArrayUniqueItems(method), generationContext);
        config.getMethodAttributeOverrides()
                .forEach(override -> override.overrideInstanceAttributes(node, method, generationContext));
        return node;
    }

    /**
     * Collect a given scope's general type attributes (i.e. everything not related to the structure).
     *
     * @param scope the scope/type representation for which to collect JSON schema attributes
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @param allowedSchemaTypes declared schema types determining which attributes are meaningful to be included
     * @return node holding all collected attributes (possibly empty)
     */
    public static ObjectNode collectTypeAttributes(TypeScope scope, SchemaGenerationContext generationContext,
            Set<String> allowedSchemaTypes) {
        SchemaGeneratorConfig config = generationContext.getGeneratorConfig();
        ObjectNode node = config.createObjectNode();
        AttributeCollector collector = new AttributeCollector(config.getObjectMapper());
        collector.setId(node, config.resolveIdForType(scope), generationContext);
        collector.setAnchor(node, config.resolveAnchorForType(scope), generationContext);
        collector.setTitle(node, config.resolveTitleForType(scope), generationContext);
        collector.setDescription(node, config.resolveDescriptionForType(scope), generationContext);
        collector.setDefault(node, config.resolveDefaultForType(scope), generationContext);
        collector.setEnum(node, config.resolveEnumForType(scope), generationContext);
        if (allowedSchemaTypes.isEmpty() || allowedSchemaTypes.contains(config.getKeyword(SchemaKeyword.TAG_TYPE_OBJECT))) {
            collector.setAdditionalProperties(node, config.resolveAdditionalPropertiesForType(scope), generationContext);
            collector.setPatternProperties(node, config.resolvePatternPropertiesForType(scope), generationContext);
        }
        if (allowedSchemaTypes.isEmpty() || allowedSchemaTypes.contains(config.getKeyword(SchemaKeyword.TAG_TYPE_STRING))) {
            collector.setStringMinLength(node, config.resolveStringMinLengthForType(scope), generationContext);
            collector.setStringMaxLength(node, config.resolveStringMaxLengthForType(scope), generationContext);
            collector.setStringFormat(node, config.resolveStringFormatForType(scope), generationContext);
            collector.setStringPattern(node, config.resolveStringPatternForType(scope), generationContext);
        }
        if (allowedSchemaTypes.isEmpty() || allowedSchemaTypes.contains(config.getKeyword(SchemaKeyword.TAG_TYPE_INTEGER))
                || allowedSchemaTypes.contains(config.getKeyword(SchemaKeyword.TAG_TYPE_NUMBER))) {
            collector.setNumberInclusiveMinimum(node, config.resolveNumberInclusiveMinimumForType(scope), generationContext);
            collector.setNumberExclusiveMinimum(node, config.resolveNumberExclusiveMinimumForType(scope), generationContext);
            collector.setNumberInclusiveMaximum(node, config.resolveNumberInclusiveMaximumForType(scope), generationContext);
            collector.setNumberExclusiveMaximum(node, config.resolveNumberExclusiveMaximumForType(scope), generationContext);
            collector.setNumberMultipleOf(node, config.resolveNumberMultipleOfForType(scope), generationContext);
        }
        if (allowedSchemaTypes.isEmpty() || allowedSchemaTypes.contains(config.getKeyword(SchemaKeyword.TAG_TYPE_ARRAY))) {
            collector.setArrayMinItems(node, config.resolveArrayMinItemsForType(scope), generationContext);
            collector.setArrayMaxItems(node, config.resolveArrayMaxItemsForType(scope), generationContext);
            collector.setArrayUniqueItems(node, config.resolveArrayUniqueItemsForType(scope), generationContext);
        }
        return node;
    }

    /**
     * Merge the second node's attributes into the first, skipping those attributes that are already contained in the first node.
     *
     * @param targetNode node to add non-existent attributes to
     * @param attributeContainer container holding attributes to add to the first node
     */
    public static void mergeMissingAttributes(ObjectNode targetNode, ObjectNode attributeContainer) {
        if (attributeContainer == null) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> attributeIterator = attributeContainer.fields();
        while (attributeIterator.hasNext()) {
            Map.Entry<String, JsonNode> attribute = attributeIterator.next();
            if (!targetNode.has(attribute.getKey())) {
                targetNode.set(attribute.getKey(), attribute.getValue());
            }
        }
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ID}" attribute.
     *
     * @param node schema node to set attribute on
     * @param id attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setId(ObjectNode node, String id, SchemaGenerationContext generationContext) {
        if (id != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_ID), id);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ANCHOR}" attribute.
     *
     * @param node schema node to set attribute on
     * @param anchor attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setAnchor(ObjectNode node, String anchor, SchemaGenerationContext generationContext) {
        if (anchor != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_ANCHOR), anchor);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_TITLE}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param title attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setTitle(ObjectNode, String, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setTitle(ObjectNode node, String title) {
        if (title != null) {
            node.put(SchemaKeyword.TAG_TITLE.forVersion(SchemaVersion.DRAFT_7), title);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_TITLE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param title attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setTitle(ObjectNode node, String title, SchemaGenerationContext generationContext) {
        if (title != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_TITLE), title);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_DESCRIPTION}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param description attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setDescription(ObjectNode, String, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setDescription(ObjectNode node, String description) {
        if (description != null) {
            node.put(SchemaKeyword.TAG_DESCRIPTION.forVersion(SchemaVersion.DRAFT_7), description);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_DESCRIPTION}" attribute.
     *
     * @param node schema node to set attribute on
     * @param description attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setDescription(ObjectNode node, String description, SchemaGenerationContext generationContext) {
        if (description != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_DESCRIPTION), description);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_DEFAULT}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param defaultValue attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setDefault(ObjectNode, Object, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setDefault(ObjectNode node, Object defaultValue) {
        if (defaultValue != null) {
            final String defaultTag = SchemaKeyword.TAG_DEFAULT.forVersion(SchemaVersion.DRAFT_7);
            // need to specifically add simple/primitive values by type
            if (defaultValue instanceof String) {
                node.put(defaultTag, (String) defaultValue);
            } else if (defaultValue instanceof BigDecimal) {
                node.put(defaultTag, (BigDecimal) defaultValue);
            } else if (defaultValue instanceof BigInteger) {
                node.put(defaultTag, (BigInteger) defaultValue);
            } else if (defaultValue instanceof Boolean) {
                node.put(defaultTag, (Boolean) defaultValue);
            } else if (defaultValue instanceof Double) {
                node.put(defaultTag, (Double) defaultValue);
            } else if (defaultValue instanceof Float) {
                node.put(defaultTag, (Float) defaultValue);
            } else if (defaultValue instanceof Integer) {
                node.put(defaultTag, (Integer) defaultValue);
            } else {
                // everything else is simply forwarded as-is to the JSON Schema, it's up to the configurator to ensure the value's correctness
                node.putPOJO(defaultTag, defaultValue);
            }
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_DEFAULT}" attribute.
     *
     * @param node schema node to set attribute on
     * @param defaultValue attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setDefault(ObjectNode node, Object defaultValue, SchemaGenerationContext generationContext) {
        if (defaultValue != null) {
            final String defaultTag = generationContext.getKeyword(SchemaKeyword.TAG_DEFAULT);
            // need to specifically add simple/primitive values by type
            if (defaultValue instanceof String) {
                node.put(defaultTag, (String) defaultValue);
            } else if (defaultValue instanceof BigDecimal) {
                node.put(defaultTag, (BigDecimal) defaultValue);
            } else if (defaultValue instanceof BigInteger) {
                node.put(defaultTag, (BigInteger) defaultValue);
            } else if (defaultValue instanceof Boolean) {
                node.put(defaultTag, (Boolean) defaultValue);
            } else if (defaultValue instanceof Double) {
                node.put(defaultTag, (Double) defaultValue);
            } else if (defaultValue instanceof Float) {
                node.put(defaultTag, (Float) defaultValue);
            } else if (defaultValue instanceof Integer) {
                node.put(defaultTag, (Integer) defaultValue);
            } else {
                // everything else is simply forwarded as-is to the JSON Schema, it's up to the configurator to ensure the value's correctness
                node.putPOJO(defaultTag, defaultValue);
            }
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_CONST}"/"{@link SchemaKeyword#TAG_ENUM}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param enumValues attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setEnum(ObjectNode, Collection, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setEnum(ObjectNode node, Collection<?> enumValues) {
        if (enumValues != null) {
            SchemaVersion schemaVersion = SchemaVersion.DRAFT_7;
            List<Object> values = enumValues.stream()
                    .filter(this::isSupportedEnumValue)
                    .filter(this::canBeConvertedToString)
                    .collect(Collectors.toList());
            if (values.size() == 1) {
                Object singleValue = values.get(0);
                if (singleValue instanceof String) {
                    node.put(SchemaKeyword.TAG_CONST.forVersion(schemaVersion), (String) singleValue);
                } else {
                    node.putPOJO(SchemaKeyword.TAG_CONST.forVersion(schemaVersion), singleValue);
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
                node.set(SchemaKeyword.TAG_ENUM.forVersion(schemaVersion), array);
            }
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_CONST}"/"{@link SchemaKeyword#TAG_ENUM}" attribute.
     *
     * @param node schema node to set attribute on
     * @param enumValues attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setEnum(ObjectNode node, Collection<?> enumValues, SchemaGenerationContext generationContext) {
        if (enumValues != null) {
            List<Object> values = enumValues.stream()
                    .filter(this::isSupportedEnumValue)
                    .filter(this::canBeConvertedToString)
                    .collect(Collectors.toList());
            if (values.size() == 1 && generationContext.getGeneratorConfig().shouldRepresentSingleAllowedValueAsConst()) {
                Object singleValue = values.get(0);
                if (singleValue instanceof String) {
                    node.put(generationContext.getKeyword(SchemaKeyword.TAG_CONST), (String) singleValue);
                } else {
                    node.putPOJO(generationContext.getKeyword(SchemaKeyword.TAG_CONST), singleValue);
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
                node.set(generationContext.getKeyword(SchemaKeyword.TAG_ENUM), array);
            }
        }
        return this;
    }

    /**
     * Check whether the given object may be included in a "{@link SchemaKeyword#TAG_CONST}"/"{@link SchemaKeyword#TAG_ENUM}" attribute.
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
     * Setter for "{@link SchemaKeyword#TAG_READ_ONLY}" attribute.
     *
     * @param node schema node to set attribute on
     * @param readOnly attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setReadOnly(ObjectNode node, boolean readOnly, SchemaGenerationContext generationContext) {
        if (readOnly) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_READ_ONLY), readOnly);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_WRITE_ONLY}" attribute.
     *
     * @param node schema node to set attribute on
     * @param writeOnly attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setWriteOnly(ObjectNode node, boolean writeOnly, SchemaGenerationContext generationContext) {
        if (writeOnly) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_WRITE_ONLY), writeOnly);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ADDITIONAL_PROPERTIES}" attribute.
     *
     * @param node schema node to set attribute on
     * @param additionalProperties attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setAdditionalProperties(ObjectNode node, Type additionalProperties, SchemaGenerationContext generationContext) {
        if (additionalProperties == Void.class || additionalProperties == Void.TYPE) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES), false);
        } else if (additionalProperties != null) {
            ResolvedType targetType = generationContext.getTypeContext().resolve(additionalProperties);
            if (targetType.getErasedType() != Object.class) {
                ObjectNode additionalPropertiesSchema = generationContext.createDefinitionReference(targetType);
                node.set(generationContext.getKeyword(SchemaKeyword.TAG_ADDITIONAL_PROPERTIES),
                        additionalPropertiesSchema);
            }
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_PATTERN_PROPERTIES}" attribute.
     *
     * @param node schema node to set attribute on
     * @param patternProperties resolved attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setPatternProperties(ObjectNode node, Map<String, Type> patternProperties,
            SchemaGenerationContext generationContext) {
        if (patternProperties != null && !patternProperties.isEmpty()) {
            ObjectNode patternPropertiesNode = this.objectMapper.createObjectNode();
            for (Map.Entry<String, Type> entry : patternProperties.entrySet()) {
                ResolvedType targetType = generationContext.getTypeContext().resolve(entry.getValue());
                ObjectNode singlePatternSchema = generationContext.createDefinitionReference(targetType);
                patternPropertiesNode.set(entry.getKey(), singlePatternSchema);
            }
            node.set(generationContext.getKeyword(SchemaKeyword.TAG_PATTERN_PROPERTIES), patternPropertiesNode);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_LENGTH_MIN}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param minLength attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setStringMinLength(ObjectNode, Integer, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setStringMinLength(ObjectNode node, Integer minLength) {
        if (minLength != null) {
            node.put(SchemaKeyword.TAG_LENGTH_MIN.forVersion(SchemaVersion.DRAFT_7), minLength);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_LENGTH_MIN}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param minLength attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setStringMinLength(ObjectNode node, Integer minLength, SchemaGenerationContext generationContext) {
        if (minLength != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_LENGTH_MIN), minLength);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_LENGTH_MAX}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param maxLength attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setStringMaxLength(ObjectNode, Integer, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setStringMaxLength(ObjectNode node, Integer maxLength) {
        if (maxLength != null) {
            node.put(SchemaKeyword.TAG_LENGTH_MAX.forVersion(SchemaVersion.DRAFT_7), maxLength);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_LENGTH_MAX}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param maxLength attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setStringMaxLength(ObjectNode node, Integer maxLength, SchemaGenerationContext generationContext) {
        if (maxLength != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_LENGTH_MAX), maxLength);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_FORMAT}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param format attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setStringFormat(ObjectNode, String, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setStringFormat(ObjectNode node, String format) {
        if (format != null) {
            node.put(SchemaKeyword.TAG_FORMAT.forVersion(SchemaVersion.DRAFT_7), format);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_FORMAT}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param format attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setStringFormat(ObjectNode node, String format, SchemaGenerationContext generationContext) {
        if (format != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_FORMAT), format);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_PATTERN}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param pattern attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setStringPattern(ObjectNode, String, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setStringPattern(ObjectNode node, String pattern) {
        if (pattern != null) {
            node.put(SchemaKeyword.TAG_PATTERN.forVersion(SchemaVersion.DRAFT_7), pattern);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_PATTERN}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param pattern attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setStringPattern(ObjectNode node, String pattern, SchemaGenerationContext generationContext) {
        if (pattern != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_PATTERN), pattern);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MINIMUM}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param inclusiveMinimum attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setNumberInclusiveMinimum(ObjectNode, BigDecimal, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setNumberInclusiveMinimum(ObjectNode node, BigDecimal inclusiveMinimum) {
        if (inclusiveMinimum != null) {
            node.put(SchemaKeyword.TAG_MINIMUM.forVersion(SchemaVersion.DRAFT_7), inclusiveMinimum);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MINIMUM}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param inclusiveMinimum attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberInclusiveMinimum(ObjectNode node, BigDecimal inclusiveMinimum, SchemaGenerationContext generationContext) {
        if (inclusiveMinimum != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_MINIMUM), inclusiveMinimum);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MINIMUM_EXCLUSIVE}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param exclusiveMinimum attribute value to set
     * @return this instance (for chaining)
     * @deprecated user {@link #setNumberExclusiveMinimum(ObjectNode, BigDecimal, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setNumberExclusiveMinimum(ObjectNode node, BigDecimal exclusiveMinimum) {
        if (exclusiveMinimum != null) {
            node.put(SchemaKeyword.TAG_MINIMUM_EXCLUSIVE.forVersion(SchemaVersion.DRAFT_7), exclusiveMinimum);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MINIMUM_EXCLUSIVE}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param exclusiveMinimum attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberExclusiveMinimum(ObjectNode node, BigDecimal exclusiveMinimum, SchemaGenerationContext generationContext) {
        if (exclusiveMinimum != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_MINIMUM_EXCLUSIVE), exclusiveMinimum);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MAXIMUM}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param inclusiveMaximum attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setNumberInclusiveMaximum(ObjectNode, BigDecimal, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setNumberInclusiveMaximum(ObjectNode node, BigDecimal inclusiveMaximum) {
        if (inclusiveMaximum != null) {
            node.put(SchemaKeyword.TAG_MAXIMUM.forVersion(SchemaVersion.DRAFT_7), inclusiveMaximum);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MAXIMUM}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param inclusiveMaximum attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberInclusiveMaximum(ObjectNode node, BigDecimal inclusiveMaximum, SchemaGenerationContext generationContext) {
        if (inclusiveMaximum != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_MAXIMUM), inclusiveMaximum);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MAXIMUM_EXCLUSIVE}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param exclusiveMaximum attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setNumberExclusiveMaximum(ObjectNode, BigDecimal, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setNumberExclusiveMaximum(ObjectNode node, BigDecimal exclusiveMaximum) {
        if (exclusiveMaximum != null) {
            node.put(SchemaKeyword.TAG_MAXIMUM_EXCLUSIVE.forVersion(SchemaVersion.DRAFT_7), exclusiveMaximum);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MAXIMUM_EXCLUSIVE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param exclusiveMaximum attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberExclusiveMaximum(ObjectNode node, BigDecimal exclusiveMaximum, SchemaGenerationContext generationContext) {
        if (exclusiveMaximum != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_MAXIMUM_EXCLUSIVE), exclusiveMaximum);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MULTIPLE_OF}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param multipleOf attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setNumberMultipleOf(ObjectNode, BigDecimal, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setNumberMultipleOf(ObjectNode node, BigDecimal multipleOf) {
        if (multipleOf != null) {
            node.put(SchemaKeyword.TAG_MULTIPLE_OF.forVersion(SchemaVersion.DRAFT_7), multipleOf);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_MULTIPLE_OF}" attribute.
     *
     * @param node schema node to set attribute on
     * @param multipleOf attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setNumberMultipleOf(ObjectNode node, BigDecimal multipleOf, SchemaGenerationContext generationContext) {
        if (multipleOf != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_MULTIPLE_OF), multipleOf);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ITEMS_MIN}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param minItemCount attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setArrayMinItems(ObjectNode, Integer, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setArrayMinItems(ObjectNode node, Integer minItemCount) {
        if (minItemCount != null) {
            node.put(SchemaKeyword.TAG_ITEMS_MIN.forVersion(SchemaVersion.DRAFT_7), minItemCount);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ITEMS_MIN}" attribute.
     *
     * @param node schema node to set attribute on
     * @param minItemCount attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setArrayMinItems(ObjectNode node, Integer minItemCount, SchemaGenerationContext generationContext) {
        if (minItemCount != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_ITEMS_MIN), minItemCount);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ITEMS_MAX}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param maxItemCount attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setArrayMaxItems(ObjectNode, Integer, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setArrayMaxItems(ObjectNode node, Integer maxItemCount) {
        if (maxItemCount != null) {
            node.put(SchemaKeyword.TAG_ITEMS_MAX.forVersion(SchemaVersion.DRAFT_7), maxItemCount);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ITEMS_MAX}" attribute.
     *
     * @param node schema node to set attribute on
     * @param maxItemCount attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setArrayMaxItems(ObjectNode node, Integer maxItemCount, SchemaGenerationContext generationContext) {
        if (maxItemCount != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_ITEMS_MAX), maxItemCount);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ITEMS_UNIQUE}" attribute (considering Draft 7).
     *
     * @param node schema node to set attribute on
     * @param uniqueItems attribute value to set
     * @return this instance (for chaining)
     * @deprecated use {@link #setArrayUniqueItems(ObjectNode, Boolean, SchemaGenerationContext)} instead
     */
    @Deprecated
    public AttributeCollector setArrayUniqueItems(ObjectNode node, Boolean uniqueItems) {
        if (uniqueItems != null) {
            node.put(SchemaKeyword.TAG_ITEMS_UNIQUE.forVersion(SchemaVersion.DRAFT_7), uniqueItems);
        }
        return this;
    }

    /**
     * Setter for "{@link SchemaKeyword#TAG_ITEMS_UNIQUE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param uniqueItems attribute value to set
     * @param generationContext generation context, including configuration to apply when looking-up attribute values
     * @return this instance (for chaining)
     */
    public AttributeCollector setArrayUniqueItems(ObjectNode node, Boolean uniqueItems, SchemaGenerationContext generationContext) {
        if (uniqueItems != null) {
            node.put(generationContext.getKeyword(SchemaKeyword.TAG_ITEMS_UNIQUE), uniqueItems);
        }
        return this;
    }
}
