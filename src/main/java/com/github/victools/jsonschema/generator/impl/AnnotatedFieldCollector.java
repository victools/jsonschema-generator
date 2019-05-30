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

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotationCollector;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copy of jackson {@link com.fasterxml.jackson.databind.introspect.AnnotatedFieldCollector AnnotatedFieldCollector} that includes static fields.
 * Additionally, fields declared in the a given type's super class are being ignored here and mixIns are ignored for now.
 */
public class AnnotatedFieldCollector extends AnnotatedMemberCollectorBase {

    AnnotatedFieldCollector(AnnotationIntrospector intr) {
        super(intr);
    }

    public static List<AnnotatedField> collectFields(AnnotationIntrospector intr, TypeResolutionContext tc, JavaType type) {
        return new AnnotatedFieldCollector(intr).collect(tc, type);
    }

    private List<AnnotatedField> collect(TypeResolutionContext tc, JavaType type) {
        Map<String, FieldBuilder> foundFields = _findFields(tc, type, new LinkedHashMap<>());
        if (foundFields == null) {
            return Collections.emptyList();
        }
        List<AnnotatedField> result = new ArrayList<>(foundFields.size());
        for (FieldBuilder b : foundFields.values()) {
            result.add(b.build());
        }
        return result;
    }

    private Map<String, FieldBuilder> _findFields(TypeResolutionContext tc, JavaType type, Map<String, FieldBuilder> fields) {
        // First, a quick test: we only care for regular classes (not interfaces,
        // primitive types etc), except for Object.class. A simple check to rule out
        // other cases is to see if there is a super class or not.
        final JavaType parentType = type.getSuperClass();
        if (parentType == null) {
            return fields;
        }
        /*
         * Difference to jackson: don't look-up parent fields here
         */
        final Class<?> rawType = type.getRawClass();
        for (Field f : ClassUtil.getDeclaredFields(rawType)) {
            if (f.isSynthetic()) {
                // ignore synthetic field, but include everything else
                continue;
            }
            // Ok now: we can (and need) not filter out ignorable fields at this point; partly
            // because mix-ins haven't been added, and partly because logic can be done
            // when determining get/settability of the field.
            FieldBuilder b = new FieldBuilder(tc, f);
            if (this._intr != null) {
                b.annotations = collectAnnotations(b.annotations, f.getDeclaredAnnotations());
            }
            fields.put(f.getName(), b);
        }
        return fields;
    }

    private final static class FieldBuilder {

        final TypeResolutionContext typeContext;
        final Field field;

        AnnotationCollector annotations;

        FieldBuilder(TypeResolutionContext tc, Field f) {
            this.typeContext = tc;
            this.field = f;
            this.annotations = AnnotationCollector.emptyCollector();
        }

        AnnotatedField build() {
            return new AnnotatedField(this.typeContext, this.field, this.annotations.asAnnotationMap());
        }
    }
}
