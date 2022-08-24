package com.github.victools.jsonschema.plugin.maven;

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
     * Classes from the project, compile time and runtime dependencies
     */
    WITH_ALL_DEPENDENCIES
}
