package com.github.victools.jsonschema.generator;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeneratedSchema {
    private final String version;
    private final ObjectNode schema;

    public GeneratedSchema(String version, ObjectNode schema) {
        this.version = version;
        this.schema = schema;
    }

    public String getVersion() {
        return version;
    }

    public ObjectNode getSchema() {
        return schema;
    }
}
