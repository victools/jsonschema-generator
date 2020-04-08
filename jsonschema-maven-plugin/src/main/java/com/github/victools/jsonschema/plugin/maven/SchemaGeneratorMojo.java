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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Maven plugin for the victools/jsonschema-generator.
 */
@Mojo(name = "schema-generator",
        defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class SchemaGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Full name of the class for which the JSON schema will be generated.
     */
    @Parameter(property = "className", required = true)
    private String className;

    /**
     * The directory path where the schema files is generated.
     * By default this is in the resources directory.
     */
    @Parameter(property = "schemaFilePath", defaultValue = "src/main/resources")
    private String schemaFilePath;

    /**
     * The name of the file in which the generate schema is written.
     */
    @Parameter(property = "schemaFileName")
    private String schemaFileName;

    /**
     * The schema version to be used: DRAFT_7 or DRAFT_2019_09.
     */
    @Parameter(property = "schemaVersion", defaultValue = "DRAFT_7")
    private String schemaVersion;

    /**
     * The options for the generator.
     */
    @Parameter(property = "options")
    private String[] options;

    /**
     * Selection of Modules that need to be activated during generation.
     */
    @Parameter(property = "modules")
    private GeneratorModule[] modules;

    /**
     * Invoke the schema generator.
     *
     * @throws MojoExecutionException An exception in case of errors and unexpected behavior
     */
    public void execute() throws MojoExecutionException {
        getLog().info("Generating JSON Schema for class " + className);

        // Load the class for which the schema will be generated
        Class<?> schemaClass;
        try {
            schemaClass = getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Error loading class " + className, e);
        }

        // Generate the schema
        ObjectMapper mapper = new ObjectMapper();
        SchemaGenerator generator = createGenerator(mapper);
        JsonNode jsonSchema = generator.generateSchema(schemaClass);

        // Write the result
        String fileName = getSchemaFullFileName();
        try {
            String schema = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
            writeToFile(schema, fileName);
        } catch (JsonProcessingException e) {
            throw new MojoExecutionException("Error writing schema file at " + fileName, e);
        }
    }

    private static URLClassLoader classLoader = null;

    /**
     * Construct the classloader based on the project classpath.
     *
     * @return The classloader
     */
    private URLClassLoader getClassLoader() {
        if (classLoader == null) {
            List<String> runtimeClasspathElements = null;
            try {
                runtimeClasspathElements = project.getRuntimeClasspathElements();
            } catch (DependencyResolutionRequiredException e) {
                e.printStackTrace();
            }

            if (runtimeClasspathElements != null) {
                URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
                for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                    String element = runtimeClasspathElements.get(i);
                    try {
                        runtimeUrls[i] = new File(element).toURI().toURL();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                classLoader = new URLClassLoader(runtimeUrls,
                        Thread.currentThread().getContextClassLoader());
            }
        }

        return classLoader;
    }

    /**
     * Return the name of the file in which the schema has to be written.
     *
     * @return The name of the schema file
     */
    private String getSchemaFullFileName() {
        if (schemaFileName == null || schemaFileName.isEmpty()) {
            return schemaFilePath + "/" + className + ".schema.json";
        }
        return schemaFilePath + "/" + schemaFileName;
    }

    /**
     * Create the JSON Schema generator.
     * Configuring it the specified options and add the required modules
     *
     * @param mapper The mapper that is to be used
     * @return The configured generator
     * @throws MojoExecutionException Error exception
     */
    private SchemaGenerator createGenerator(ObjectMapper mapper) throws MojoExecutionException {
        // Start with the generator builder
        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(mapper, getSchemaVersion(), OptionPreset.PLAIN_JSON);

        // Add options when required
        if (options.length == 0) {
            setDefaultOptions(configBuilder);
        } else {
            setOptionsFromConfig(configBuilder, options);
        }

        // Register the modules
        if (modules.length == 0) {
            configureDefaultModules(configBuilder);
        } else {
            configureModules(configBuilder, modules);
        }

        // And construct the generator
        SchemaGeneratorConfig config = configBuilder.build();
        return new SchemaGenerator(config);
    }

    /**
     * Get the schema version to be used from the configuration.
     *
     * @return The schema version
     * @throws MojoExecutionException Exception in case of an unknowm value
     */
    private SchemaVersion getSchemaVersion() throws MojoExecutionException {
        try {
            return SchemaVersion.valueOf(schemaVersion);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Error: Unknown schema version " + schemaVersion, e);
        }
    }

    /**
     * Set the default generator options.
     *
     * @param configBuilder The builder on which to set the options
     */
    private void setDefaultOptions(SchemaGeneratorConfigBuilder configBuilder) {
        configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
    }

    /**
     * Set the generator options form the configuration.
     *
     * @param configBuilder The configbuilder on which the options are set
     * @param options       The options from the pom file
     * @throws MojoExecutionException An exception in case of unexpected behavior
     */
    private void setOptionsFromConfig(SchemaGeneratorConfigBuilder configBuilder, String[] options) throws MojoExecutionException {
        for (String option : options) {
            try {
                Option optionLiteral = Option.valueOf(option);
                configBuilder = configBuilder.with(optionLiteral);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException("Error: Unknown option " + option, e);
            }
        }
    }

    /**
     * Configure all the modules on the generator.
     *
     * @param configBuilder The builder on which the modules are added.
     * @param modules       The modules configuration part from the the pom.xml
     * @throws MojoExecutionException Error exception
     */
    private void configureModules(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule[] modules) throws MojoExecutionException {
        for (GeneratorModule module : modules) {
            if (module.className != null && !module.className.isEmpty()) {
                try {
                    getLog().info("- Adding CustomModule " + module.className);
                    Class<? extends Module> moduleClass =
                            (Class<? extends Module>) getClassLoader().loadClass(module.className);
                    Module moduleInstance = moduleClass.getConstructor().newInstance();
                    configBuilder.with(moduleInstance);
                } catch (ClassCastException | InstantiationException
                        | IllegalAccessException | NoSuchMethodException
                        | InvocationTargetException | ClassNotFoundException e) {
                    throw new MojoExecutionException("Error: Can not create custom module" + module.className, e);
                }
            } else if (module.name != null) {
                switch (module.name) {
                case "Jackson":
                    getLog().info("- Adding JacksonModule");
                    addJacksonModule(configBuilder, module);
                    break;
                case "Validation":
                    getLog().info("- Adding Javax Validation Module");
                    addValidationModule(configBuilder, module);
                    break;
                case "Swagger":
                    getLog().info("- Adding SwaggerModule");
                    addSwaggerModule(configBuilder, module);
                    break;
                default:
                    throw new MojoExecutionException("Error: Module does not have a name in "
                            + "['Jackson', 'Validation', 'Swagger'] or does not have a custom classname.");
                }
            }
        }
    }

    /**
     * Add the Swagger module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module        The modules section form the pom
     */
    private void addSwaggerModule(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule module) {
        // TODO
    }

    /**
     * Add the Javax Validation module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module        The modules section form the pom
     */
    private void addValidationModule(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule module) {
        // TODO
    }

    /**
     * Add the Jackson module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module        The modules section form the pom
     * @throws MojoExecutionException Exception in case of error
     */
    private void addJacksonModule(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule module) throws MojoExecutionException {
        if (module.options == null || module.options.length == 0) {
            configBuilder.with(new JacksonModule());
        } else {
            JacksonOption[] jacksonOptions = new JacksonOption[module.options.length];
            for (int i = 0; i < module.options.length; i++) {
                try {
                    jacksonOptions[i] = JacksonOption.valueOf(module.options[i]);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Error: Unknown Jackson option " + jacksonOptions[i], e);
                }
            }
            configBuilder.with(new JacksonModule(jacksonOptions));
        }
    }

    /**
     * Configure the default modules.
     * This is used in case no modules are specified in the pom.
     *
     * @param configBuilder The config builder that is configured.
     */
    private void configureDefaultModules(SchemaGeneratorConfigBuilder configBuilder) {
        getLog().info("- Adding JacksonModule");
        configBuilder.with(new JacksonModule());
    }

    /**
     * Write content to a file.
     *
     * @param content  content to be written
     * @param fileName the name of the file
     */
    private static void writeToFile(String content, String fileName) {
        try {
            File file = new File(fileName);
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            PrintWriter pw = new PrintWriter(writer);
            pw.print(content);
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}


