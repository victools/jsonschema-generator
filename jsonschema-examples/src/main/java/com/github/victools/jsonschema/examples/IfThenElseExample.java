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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.generator.TypeAttributeOverrideV2;
import com.github.victools.jsonschema.generator.TypeScope;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Example created in response to <a href="https://github.com/victools/jsonschema-generator/discussions/328">#328</a>.
 * <br/>
 * Creating "if"/"then"/"else" blocks via custom annotations, leveraging the Swagger @Schema annotation.
 */
public class IfThenElseExample implements SchemaGenerationExampleInterface {

    @Override
    public JsonNode generateSchema() {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        configBuilder.forTypesInGeneral().withTypeAttributeOverride(new SchemaConditionAttributeOverride());
        SchemaGeneratorConfig config = configBuilder.build();
        SchemaGenerator generator = new SchemaGenerator(config);
        return generator.generateSchema(TestType.class);
    }

    static class SchemaConditionAttributeOverride implements TypeAttributeOverrideV2 {

        @Override
        public void overrideTypeAttributes(ObjectNode schemaNode, TypeScope scope, SchemaGenerationContext context) {
            SchemaCondition annotation = scope.getContext().getTypeAnnotationConsideringHierarchy(scope.getType(), SchemaCondition.class);
            if (annotation == null) {
                return;
            }
            ObjectNode conditionParentNode;
            if (schemaNode.has(context.getKeyword(SchemaKeyword.TAG_IF))
                    || schemaNode.has(context.getKeyword(SchemaKeyword.TAG_THEN))
                    || schemaNode.has(context.getKeyword(SchemaKeyword.TAG_ELSE))) {
                // add via allOf to avoid conflicts
                conditionParentNode = schemaNode.withArray(context.getKeyword(SchemaKeyword.TAG_ALLOF)).addObject();
            } else {
                conditionParentNode = schemaNode;
            }

            this.populate(conditionParentNode.putObject(context.getKeyword(SchemaKeyword.TAG_IF)), annotation.ifFulFilled(), context);
            if (annotation.thenExpect().length > 0) {
                ObjectNode thenNode = conditionParentNode.putObject(context.getKeyword(SchemaKeyword.TAG_THEN));
                Stream.of(annotation.thenExpect()).forEach(thenCondition -> this.populate(thenNode, thenCondition, context));
            }
            if (annotation.elseExpect().length > 0) {
                ObjectNode elseNode = conditionParentNode.putObject(context.getKeyword(SchemaKeyword.TAG_ELSE));
                Stream.of(annotation.elseExpect()).forEach(elseCondition -> this.populate(elseNode, elseCondition, context));
            }
        }

        private void populate(ObjectNode node, SchemaProperty propertyAnnotation, SchemaGenerationContext context) {
            // mark reference property as required
            node.withArray(context.getKeyword(SchemaKeyword.TAG_REQUIRED)).add(propertyAnnotation.name());
            // check additional requirement
            ObjectNode propertyNode = this.withObject(
                    this.withObject(node, context.getKeyword(SchemaKeyword.TAG_PROPERTIES)),
                    propertyAnnotation.name());
            if (!Objects.equals(propertyAnnotation.constValue(), "")) {
                propertyNode.put(context.getKeyword(SchemaKeyword.TAG_CONST), propertyAnnotation.constValue());
            } else if (!Objects.equals(propertyAnnotation.pattern(), "")) {
                propertyNode.put(context.getKeyword(SchemaKeyword.TAG_PATTERN), propertyAnnotation.pattern());
            }
        }

        private ObjectNode withObject(ObjectNode node, String keyword) {
            return node.has(keyword) ? (ObjectNode) node.get(keyword) : node.putObject(keyword);
        }
    }

    @SchemaCondition(
            ifFulFilled = @SchemaProperty(name = "country", constValue = "US"),
            thenExpect = @SchemaProperty(name = "postalCode", pattern = "^\\d{5}(?:[-\\s]\\d{4})?$"),
            elseExpect = @SchemaProperty(name = "postalCode", pattern = "^[-\\s\\d]+$")
    )
    static class TestType {

        String country;
        String postalCode;
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface SchemaCondition {
        SchemaProperty ifFulFilled();
        SchemaProperty[] thenExpect() default {};
        SchemaProperty[] elseExpect() default {};
    }

    @Target({ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface SchemaProperty {
        String name();
        String constValue() default "";
        String pattern() default "";
    }
}
