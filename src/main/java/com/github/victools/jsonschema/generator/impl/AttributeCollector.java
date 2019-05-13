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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.JavaType;
import com.github.victools.jsonschema.generator.SchemaConstants;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;

/**
 * Helper class for looking-up various attribute values for a field or method via a given configuration instance.
 */
public class AttributeCollector {

    private SchemaGeneratorConfig config;

    /**
     * Constructor for a given configuration instance.
     *
     * @param config configuration to apply when looking-up attribute values
     */
    public AttributeCollector(SchemaGeneratorConfig config) {
        this.config = config;
    }

    /**
     * Collect a field's contextual attributes (i.e. everything not related to the structure).
     *
     * @param field the field for which to collect JSON schema attributes
     * @param type associated field type
     * @return node holding all collected attributes (possibly empty)
     */
    public ObjectNode collectFieldAttributes(Field field, JavaType type) {
        ObjectNode node = this.config.createObjectNode();
        this.setTitle(node, this.config.resolveTitle(field, type));
        this.setDescription(node, this.config.resolveDescription(field, type));
        this.setEnum(node, this.config.resolveEnum(field, type));
        this.setStringMinLength(node, this.config.resolveStringMinLength(field, type));
        this.setStringMaxLength(node, this.config.resolveStringMaxLength(field, type));
        this.setStringFormat(node, this.config.resolveStringFormat(field, type));
        this.setNumberInclusiveMinimum(node, this.config.resolveNumberInclusiveMinimum(field, type));
        this.setNumberExclusiveMinimum(node, this.config.resolveNumberExclusiveMinimum(field, type));
        this.setNumberInclusiveMaximum(node, this.config.resolveNumberInclusiveMaximum(field, type));
        this.setNumberExclusiveMaximum(node, this.config.resolveNumberExclusiveMaximum(field, type));
        this.setNumberMultipleOf(node, this.config.resolveNumberMultipleOf(field, type));
        this.setArrayMinItems(node, this.config.resolveArrayMinItems(field, type));
        this.setArrayMaxItems(node, this.config.resolveArrayMaxItems(field, type));
        this.setArrayUniqueItems(node, this.config.resolveArrayUniqueItems(field, type));
        return node;
    }

    /**
     * Collect a method's contextual attributes (i.e. everything not related to the structure).
     *
     * @param method the method for which to collect JSON schema attributes
     * @param returnType associated return value type
     * @return node holding all collected attributes (possibly empty)
     */
    public ObjectNode collectMethodAttributes(Method method, JavaType returnType) {
        ObjectNode node = this.config.createObjectNode();
        this.setTitle(node, this.config.resolveTitle(method, returnType));
        this.setDescription(node, this.config.resolveDescription(method, returnType));
        this.setEnum(node, this.config.resolveEnum(method, returnType));
        this.setStringMinLength(node, this.config.resolveStringMinLength(method, returnType));
        this.setStringMaxLength(node, this.config.resolveStringMaxLength(method, returnType));
        this.setStringFormat(node, this.config.resolveStringFormat(method, returnType));
        this.setNumberInclusiveMinimum(node, this.config.resolveNumberInclusiveMinimum(method, returnType));
        this.setNumberExclusiveMinimum(node, this.config.resolveNumberExclusiveMinimum(method, returnType));
        this.setNumberInclusiveMaximum(node, this.config.resolveNumberInclusiveMaximum(method, returnType));
        this.setNumberExclusiveMaximum(node, this.config.resolveNumberExclusiveMaximum(method, returnType));
        this.setNumberMultipleOf(node, this.config.resolveNumberMultipleOf(method, returnType));
        this.setArrayMinItems(node, this.config.resolveArrayMinItems(method, returnType));
        this.setArrayMaxItems(node, this.config.resolveArrayMaxItems(method, returnType));
        this.setArrayUniqueItems(node, this.config.resolveArrayUniqueItems(method, returnType));
        return node;
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_TITLE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param title attribute value to set
     */
    private void setTitle(ObjectNode node, String title) {
        if (title != null) {
            node.put(SchemaConstants.TAG_TITLE, title);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_DESCRIPTION}" attribute.
     *
     * @param node schema node to set attribute on
     * @param description attribute value to set
     */
    private void setDescription(ObjectNode node, String description) {
        if (description != null) {
            node.put(SchemaConstants.TAG_DESCRIPTION, description);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_CONST}"/"{@value SchemaConstants#TAG_ENUM}" attribute.
     *
     * @param node schema node to set attribute on
     * @param enumValues attribute value to set
     */
    private void setEnum(ObjectNode node, Collection<?> enumValues) {
        if (enumValues != null && !enumValues.isEmpty()) {
            if (enumValues.size() == 1) {
                Object singleValue = enumValues.iterator().next();
                if (singleValue instanceof String) {
                    node.put(SchemaConstants.TAG_CONST, (String) singleValue);
                } else {
                    node.putPOJO(SchemaConstants.TAG_CONST, singleValue);
                }
            } else {
                ArrayNode array = this.config.createArrayNode();
                for (Object singleValue : enumValues) {
                    if (singleValue instanceof String) {
                        array.add((String) singleValue);
                    } else {
                        array.addPOJO(singleValue);
                    }
                }
                node.set(SchemaConstants.TAG_ENUM, array);
            }
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_LENGTH_MIN}" attribute.
     *
     * @param node schema node to set attribute on
     * @param minLength attribute value to set
     */
    private void setStringMinLength(ObjectNode node, Integer minLength) {
        if (minLength != null) {
            node.put(SchemaConstants.TAG_LENGTH_MIN, minLength);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_LENGTH_MAX}" attribute.
     *
     * @param node schema node to set attribute on
     * @param maxLength attribute value to set
     */
    private void setStringMaxLength(ObjectNode node, Integer maxLength) {
        if (maxLength != null) {
            node.put(SchemaConstants.TAG_LENGTH_MAX, maxLength);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_FORMAT}" attribute.
     *
     * @param node schema node to set attribute on
     * @param format attribute value to set
     */
    private void setStringFormat(ObjectNode node, String format) {
        if (format != null) {
            node.put(SchemaConstants.TAG_FORMAT, format);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MINIMUM}" attribute.
     *
     * @param node schema node to set attribute on
     * @param inclusiveMinimum attribute value to set
     */
    private void setNumberInclusiveMinimum(ObjectNode node, BigDecimal inclusiveMinimum) {
        if (inclusiveMinimum != null) {
            node.put(SchemaConstants.TAG_MINIMUM, inclusiveMinimum);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MINIMUM_EXCLUSIVE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param exclusiveMinimum attribute value to set
     */
    private void setNumberExclusiveMinimum(ObjectNode node, BigDecimal exclusiveMinimum) {
        if (exclusiveMinimum != null) {
            node.put(SchemaConstants.TAG_MINIMUM_EXCLUSIVE, exclusiveMinimum);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MAXIMUM}" attribute.
     *
     * @param node schema node to set attribute on
     * @param inclusiveMaximum attribute value to set
     */
    private void setNumberInclusiveMaximum(ObjectNode node, BigDecimal inclusiveMaximum) {
        if (inclusiveMaximum != null) {
            node.put(SchemaConstants.TAG_MAXIMUM, inclusiveMaximum);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MAXIMUM_EXCLUSIVE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param exclusiveMaximum attribute value to set
     */
    private void setNumberExclusiveMaximum(ObjectNode node, BigDecimal exclusiveMaximum) {
        if (exclusiveMaximum != null) {
            node.put(SchemaConstants.TAG_MAXIMUM_EXCLUSIVE, exclusiveMaximum);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_MULTIPLE_OF}" attribute.
     *
     * @param node schema node to set attribute on
     * @param multipleOf attribute value to set
     */
    private void setNumberMultipleOf(ObjectNode node, BigDecimal multipleOf) {
        if (multipleOf != null) {
            node.put(SchemaConstants.TAG_MULTIPLE_OF, multipleOf);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_ITEMS_MIN}" attribute.
     *
     * @param node schema node to set attribute on
     * @param minItemCount attribute value to set
     */
    private void setArrayMinItems(ObjectNode node, Integer minItemCount) {
        if (minItemCount != null) {
            node.put(SchemaConstants.TAG_ITEMS_MIN, minItemCount);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_ITEMS_MAX}" attribute.
     *
     * @param node schema node to set attribute on
     * @param maxItemCount attribute value to set
     */
    private void setArrayMaxItems(ObjectNode node, Integer maxItemCount) {
        if (maxItemCount != null) {
            node.put(SchemaConstants.TAG_ITEMS_MAX, maxItemCount);
        }
    }

    /**
     * Setter for "{@value SchemaConstants#TAG_ITEMS_UNIQUE}" attribute.
     *
     * @param node schema node to set attribute on
     * @param uniqueItems attribute value to set
     */
    private void setArrayUniqueItems(ObjectNode node, Boolean uniqueItems) {
        if (uniqueItems != null) {
            node.put(SchemaConstants.TAG_ITEMS_UNIQUE, uniqueItems);
        }
    }
}
