package com.github.victools.jsonschema.plugin.maven;

public enum Classpath {
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
    WITH_COMPILE_DEPENDENCIES
}
