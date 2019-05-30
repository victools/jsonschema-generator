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

package com.github.victools.jsonschema.generator;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotationMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Map;

/**
 * 
 */
public class AbstractAnnotationAwareTest {

    protected Map.Entry<AnnotatedField, BeanDescription> wrapField(Field field) {
        SerializationConfig serializationConfig = new ObjectMapper().getSerializationConfig();
        JavaType declaringType = serializationConfig.getTypeFactory().constructType(field.getDeclaringClass());
        BeanDescription declaringContext = serializationConfig.introspect(declaringType);
        AnnotatedField wrapped = new AnnotatedField(declaringContext.getClassInfo(), field, null);
        return new AbstractMap.SimpleEntry<>(wrapped, declaringContext);
    }

    protected Map.Entry<AnnotatedMethod, BeanDescription> wrapMethod(Method method) {
        SerializationConfig serializationConfig = new ObjectMapper().getSerializationConfig();
        JavaType declaringType = serializationConfig.getTypeFactory().constructType(method.getDeclaringClass());
        BeanDescription declaringContext = serializationConfig.introspect(declaringType);
        AnnotatedMethod wrapped = new AnnotatedMethod(declaringContext.getClassInfo(), method, null, new AnnotationMap[method.getParameterCount()]);
        return new AbstractMap.SimpleEntry<>(wrapped, declaringContext);
    }
}
