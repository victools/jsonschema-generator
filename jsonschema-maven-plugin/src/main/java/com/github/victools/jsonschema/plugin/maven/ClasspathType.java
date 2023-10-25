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
    WITH_ALL_DEPENDENCIES,
    /**
     * Classes from the project (including tests), compile time, runtime and test dependencies.
     * Mainly intended for internal use when looking up custom modules.
     */
    WITH_ALL_DEPENDENCIES_AND_TESTS;

    public Collection<String> getClasspathElements(MavenProject project) {
        Collection<String> classpathElements;
        try {
            switch (this) {
            case PROJECT_ONLY:
                classpathElements = Collections.singleton(project.getBuild().getOutputDirectory());
                break;
            case WITH_COMPILE_DEPENDENCIES:
                classpathElements = project.getCompileClasspathElements();
                break;
            case WITH_RUNTIME_DEPENDENCIES:
                classpathElements = project.getRuntimeClasspathElements();
                break;
            case WITH_ALL_DEPENDENCIES:
                // to remove duplicates
                classpathElements = new HashSet<>();
                classpathElements.addAll(project.getRuntimeClasspathElements());
                classpathElements.addAll(project.getCompileClasspathElements());
                break;
            case WITH_ALL_DEPENDENCIES_AND_TESTS:
                // to remove duplicates
                classpathElements = new HashSet<>();
                classpathElements.addAll(project.getRuntimeClasspathElements());
                classpathElements.addAll(project.getCompileClasspathElements());
                classpathElements.addAll(project.getTestClasspathElements());
                break;
            default:
                throw new IllegalArgumentException("ClasspathType " + this + " not supported");
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException("Failed to resolve classpathType elements", e);
        }
        return classpathElements;
    }

    public List<URL> getUrls(MavenProject project) {
        Collection<String> classpathElements = this.getClasspathElements(project);
        List<URL> urls = new ArrayList<>(classpathElements.size());
        for (String element : classpathElements) {
            try {
                urls.add(new File(element).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to resolve classpath element", e);
            }
        }
        return urls;
    }
}
