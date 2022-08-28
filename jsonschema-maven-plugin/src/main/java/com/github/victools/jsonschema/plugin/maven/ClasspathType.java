package com.github.victools.jsonschema.plugin.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
        List<String> classPathElements = null;
        try {
            switch (this) {
            case PROJECT_ONLY:
                classPathElements = new ArrayList<>();
                classPathElements.add(project.getBuild().getOutputDirectory());
                break;
            case WITH_COMPILE_DEPENDENCIES:
                classPathElements = project.getCompileClasspathElements();
                break;
            case WITH_RUNTIME_DEPENDENCIES:
                classPathElements = project.getRuntimeClasspathElements();
                break;
            case WITH_ALL_DEPENDENCIES:
                // to remove duplicates
                HashSet<String> set = new HashSet<>();
                set.addAll(project.getRuntimeClasspathElements());
                set.addAll(project.getCompileClasspathElements());
                classPathElements = new ArrayList<>(set);
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
