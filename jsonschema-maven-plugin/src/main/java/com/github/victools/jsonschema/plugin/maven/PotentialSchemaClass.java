/*
 * Copyright 2020 VicTools.
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

package com.github.victools.jsonschema.plugin.maven;

import io.github.classgraph.ClassInfo;

/**
 * Wrapper for a class on the classpath for which a schema may be generated.
 */
public class PotentialSchemaClass implements Comparable<PotentialSchemaClass> {

    private final String fullClassName;
    private final String absolutePathToMatch;
    private boolean alreadyGenerated;

    /**
     * Constructor expecting the collected class info for a single type.
     *
     * @param classInfo targeted class's info
     */
    public PotentialSchemaClass(ClassInfo classInfo) {
        this.fullClassName = classInfo.getName();
        this.absolutePathToMatch = this.fullClassName.replace('.', '/');
        this.alreadyGenerated = false;
    }

    @Override
    public int compareTo(PotentialSchemaClass other) {
        return this.getFullClassName().compareTo(other.getFullClassName());
    }

    public String getFullClassName() {
        return this.fullClassName;
    }

    public String getAbsolutePathToMatch() {
        return this.absolutePathToMatch;
    }

    public boolean isAlreadyGenerated() {
        return alreadyGenerated;
    }

    public void setAlreadyGenerated() {
        this.alreadyGenerated = true;
    }
}
