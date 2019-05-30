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
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotationCollector;
import com.fasterxml.jackson.databind.introspect.MemberKey;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Copy of jackson {@link com.fasterxml.jackson.databind.introspect.AnnotatedMethodCollector AnnotatedMethodCollector} that includes static methods.
 * Additionally, method declared in the a given type's super class are being ignored here and mixIns are ignored for now.
 */
public class AnnotatedMethodCollector extends AnnotatedMemberCollectorBase {

    AnnotatedMethodCollector(AnnotationIntrospector intr) {
        super(intr);
    }

    public static Collection<AnnotatedMethod> collectMethods(AnnotationIntrospector intr, TypeResolutionContext tc, TypeFactory tf, JavaType type) {
        return new AnnotatedMethodCollector(intr).collect(tc, tf, type);
    }

    private List<AnnotatedMethod> collect(TypeResolutionContext tc, TypeFactory tf, JavaType mainType) {
        Map<MemberKey, MethodBuilder> methods = new LinkedHashMap<>();

        // first: methods from the class itself
        _addMemberMethods(tc, mainType.getRawClass(), methods, false);

        // and then augment these with annotations from super-types:
        JavaType superClass = mainType.getSuperClass();
        if (superClass != null) {
            _addMemberMethods(new TypeResolutionContext.Basic(tf, superClass.getBindings()), superClass.getRawClass(), methods, true);
        }
        for (JavaType superInterface : mainType.getInterfaces()) {
            _addMemberMethods(new TypeResolutionContext.Basic(tf, superInterface.getBindings()), superInterface.getRawClass(), methods, true);
        }

        return methods.values()
                .stream()
                .map(MethodBuilder::build)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void _addMemberMethods(TypeResolutionContext tc, Class<?> cls, Map<MemberKey, MethodBuilder> methods, boolean augmentExistingMethods) {
        // then methods from the class itself
        for (Method m : ClassUtil.getClassMethods(cls)) {
            if (m.isSynthetic() || m.isBridge() || Modifier.isPrivate(m.getModifiers())) {
                continue;
            }
            final MemberKey key = new MemberKey(m);
            MethodBuilder b = methods.get(key);
            if (b == null) {
                if (augmentExistingMethods || !Modifier.isPublic(m.getModifiers())) {
                    // only include public methods for the target class, but not when collecting annotations from overridden methods
                    continue;
                }
                AnnotationCollector c = AnnotationCollector.emptyCollector();
                if (this._intr != null) {
                    c = collectAnnotations(c, m.getDeclaredAnnotations());
                }
                methods.put(key, new MethodBuilder(tc, m, c));
            } else {
                if (this._intr != null) {
                    b.annotations = collectDefaultAnnotations(b.annotations, m.getDeclaredAnnotations());
                }
                Method old = b.method;
                if (old == null) { // had "mix-over", replace
                    b.method = m;
                } else if (Modifier.isAbstract(old.getModifiers())
                        && !Modifier.isAbstract(m.getModifiers())) {
                    // 06-Jan-2010, tatu: Except that if method we saw first is
                    // from an interface, and we now find a non-interface definition, we should
                    //   use this method, but with combination of annotations.
                    //   This helps (or rather, is essential) with JAXB annotations and
                    //   may also result in faster method calls (interface calls are slightly
                    //   costlier than regular method calls)
                    b.method = m;
                    // 23-Aug-2017, tatu: [databind#1705] Also need to change the type resolution context if so
                    //    (note: mix-over case above shouldn't need it)
                    b.typeContext = tc;
                }
            }
        }
    }

    private final static class MethodBuilder {

        TypeResolutionContext typeContext;

        // Method left empty for "floating" mix-in, filled in as need be
        Method method;
        AnnotationCollector annotations;

        MethodBuilder(TypeResolutionContext tc, Method m,
                AnnotationCollector ann) {
            typeContext = tc;
            method = m;
            annotations = ann;
        }

        AnnotatedMethod build() {
            if (method == null) {
                return null;
            }
            // 12-Apr-2017, tatu: Note that parameter annotations are NOT collected -- we could
            //   collect them if that'd make sense but...
            return new AnnotatedMethod(typeContext, method, annotations.asAnnotationMap(), null);
        }
    }
}
