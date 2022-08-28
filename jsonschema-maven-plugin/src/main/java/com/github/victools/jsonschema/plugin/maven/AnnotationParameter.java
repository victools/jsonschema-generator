package com.github.victools.jsonschema.plugin.maven;

import java.util.HashMap;
import java.util.Map;

public class AnnotationParameter {

    /**
     * The className of the annotation to filter by.
     */
    public String className;
    /**
     * Properties of the annotation to filter.
     * This parameter has no effect currently but might be used in future versions.
     */
    public Map<String, Object> properties;

    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * This is the default setter for maven parameter injection.
     * @param className the classname of the annotation
     */
    public void set(String className) {
        this.className = className;
        this.properties = new HashMap<>();
    }
}
