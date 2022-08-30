/*
 * Copyright 2022 VicTools.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

public enum ClasspathType {
    /**
     * Only classes in the project.
     */
    PROJECT_ONLY,
    /**
     * Classes from the project and any runtime dependencies.
     */
    WITH_RUNTIME_DEPENDENCIES,
    /**
     * Classes from the project and any compile time dependencies.
     */
    WITH_COMPILE_DEPENDENCIES,
    /**
     * Classes from the project, compile time and runtime dependencies.
     */
    WITH_ALL_DEPENDENCIES;

    public List<URL> getUrls(MavenProject project) {
        Collection<String> classPathElements;
        try {
            switch (this) {
            case PROJECT_ONLY:
                classPathElements = Collections.singleton(project.getBuild().getOutputDirectory());
                break;
            case WITH_COMPILE_DEPENDENCIES:
                classPathElements = project.getCompileClasspathElements();
                break;
            case WITH_RUNTIME_DEPENDENCIES:
                classPathElements = project.getRuntimeClasspathElements();
                break;
            case WITH_ALL_DEPENDENCIES:
                // to remove duplicates
                classPathElements = new HashSet<>();
                classPathElements.addAll(project.getRuntimeClasspathElements());
                classPathElements.addAll(project.getCompileClasspathElements());
                break;
            default:
                throw new IllegalArgumentException("ClasspathType " + this + " not supported");
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException("Failed to resolve classpathType elements", e);
        }

        List<URL> urls = new ArrayList<>(classPathElements.size());
        for (String element : classPathElements) {
            try {
                urls.add(new File(element).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to resolve classpath element", e);
            }
        }
        return urls;
    }
}
