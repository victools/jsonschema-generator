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
import com.fasterxml.jackson.databind.introspect.AnnotationCollector;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Adjusted copy of jackson {@link com.fasterxml.jackson.databind.introspect.CollectorBase CollectorBase}.
 */
public class AnnotatedMemberCollectorBase {

    protected final AnnotationIntrospector _intr;

    AnnotatedMemberCollectorBase(AnnotationIntrospector intr) {
        this._intr = intr;
    }

    protected AnnotationCollector collectAnnotations(AnnotationCollector c, Annotation[] anns) {
        return this.collectAnnotations(c, anns, true);
    }

    protected AnnotationCollector collectDefaultAnnotations(AnnotationCollector c, Annotation[] anns) {
        return this.collectAnnotations(c, anns, false);
    }

    private AnnotationCollector collectAnnotations(AnnotationCollector c, Annotation[] anns, boolean overrideExisting) {
        AnnotationCollector collector = c;
        for (Annotation ann : anns) {
            // minor optimization: by-pass 2 common JDK meta-annotations
            if (_ignorableAnnotation(ann)) {
                continue;
            }
            if (!collector.isPresent(ann) || overrideExisting) {
                collector = collector.addOrOverride(ann);
                if (this._intr.isAnnotationBundle(ann)) {
                    collector = this.collectFromBundle(collector, ann, overrideExisting);
                }
            }
        }
        return collector;
    }

    private AnnotationCollector collectFromBundle(AnnotationCollector c, Annotation bundle, boolean overrideExisting) {
        AnnotationCollector collector = c;
        Annotation[] anns = ClassUtil.findClassAnnotations(bundle.annotationType());
        for (Annotation ann : anns) {
            // minor optimization: by-pass 2 common JDK meta-annotations
            if (_ignorableAnnotation(ann)) {
                continue;
            }
            if (!collector.isPresent(ann) || overrideExisting) {
                if (this._intr.isAnnotationBundle(ann)) {
                    // 11-Apr-2017, tatu: Also must guard against recursive definitions...
                    if (!collector.isPresent(ann)) {
                        collector = collector.addOrOverride(ann);
                        collector = this.collectFromBundle(collector, ann, overrideExisting);
                    }
                } else {
                    collector = collector.addOrOverride(ann);
                }
            }
        }
        return collector;
    }

    private static boolean _ignorableAnnotation(Annotation a) {
        return (a instanceof Target) || (a instanceof Retention);
    }
}
