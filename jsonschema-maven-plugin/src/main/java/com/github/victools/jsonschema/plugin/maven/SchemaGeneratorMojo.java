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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger15.SwaggerOption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Maven plugin for the victools/jsonschema-generator.
 */
@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresDependencyCollection = ResolutionScope.COMPILE)
public class SchemaGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Full name of the classes for which the JSON schema will be generated.
     */
    @Parameter(property = "classNames")
    private String[] classNames;

    /**
     * Full name of a package for which for all classes in the package JSON schemas will be generated.
     */
    @Parameter(property = "packageNames")
    private String[] packageNames;

    /**
     * The directory path where the schema files are generated.
     * <br>
     * By default, this is: {@code src/main/resources}
     */
    @Parameter
    private File schemaFilePath;

    /**
     * The name of the file in which the generated schema is written. Allowing for two placeholders:
     * <ul>
     * <li><code>{0}</code> - containing the simple class name (last part of the className)</li>
     * <li><code>{1}</code> - containing the package path (first part of the className)</li>
     * </ul>
     * The default name is: <code>{0}-schema.json</code>
     */
    @Parameter(defaultValue = "{0}-schema.json")
    private String schemaFileName;

    /**
     * The schema version to be used: DRAFT_6, DRAFT_7 or DRAFT_2019_09.
     */
    @Parameter(defaultValue = "DRAFT_7")
    private SchemaVersion schemaVersion;

    /**
     * The options for the generator.
     */
    @Parameter
    private GeneratorOptions options;

    /**
     * Selection of Modules that need to be activated during generation.
     */
    @Parameter
    private GeneratorModule[] modules;

    /**
     * The generator to be used for all schema generations.
     */
    private SchemaGenerator generator = null;

    /**
     * Invoke the schema generator.
     *
     * @throws MojoExecutionException An exception in case of errors and unexpected behavior
     */
    @Override
    public void execute() throws MojoExecutionException {
        this.getLog().debug("Initializing Schema Generator");
        this.getLog().info(getInfoText());

        if (classNames != null) {
            for (String className : classNames) {
                generateSchema(className);
            }
        }

        if (packageNames != null) {
            for (String packageName : packageNames) {
                generateSchemaForPackage(packageName);
            }
        }
    }

    /**
     * Generate JSON schema's for all classes in a package.
     *
     * @param packageName The name of the package
     */
    private void generateSchemaForPackage(String packageName) {
        // TODO: Final step
    }

    /**
     * Generate the JSON schema for the given className.
     *
     * @param className The name of the class
     * @throws MojoExecutionException In case of problems
     */
    private void generateSchema(String className) throws MojoExecutionException {
        // Load the class for which the schema will be generated
        Class<?> schemaClass;
        try {
            schemaClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Error loading class " + className, e);
        }

        // Generate the schema
        JsonNode jsonSchema = getGenerator().generateSchema(schemaClass);
        File file = getSchemaFile(schemaClass);
        this.getLog().info("- Writing schema to file: " + file);
        this.writeToFile(jsonSchema, file);
    }

    /**
     * Build up the initial logging text.
     *
     * @return The info message text
     */
    private String getInfoText() {
        StringBuilder logText = new StringBuilder("Generating JSON Schema for ");
        if (classNames.length > 0) {
            logText.append("class");
            if (classNames.length > 1) {
                logText.append("es");
            }
            logText.append(" ");
            logText.append(String.join(", ", classNames));
            if (packageNames.length > 0) {
                logText.append(" and ");
            }
        }
        if (packageNames.length > 0) {
            logText.append("package");
            if (packageNames.length > 1) {
                logText.append("s");
            }
            logText.append(" ");
            logText.append(String.join(", ", packageNames));
        }

        return logText.toString();
    }

    /**
     * Return the file in which the schema has to be written.
     *
     * <p>The path is determined based on the {@link #schemaFilePath} parameter.
     * <br>
     * The name of the file is determined based on the {@link #schemaFileName} parameter, which allows for two placeholders:
     * <ul>
     * <li><code>{0}</code> - containing the simple class name of the parameter)</li>
     * <li><code>{1}</code> - containing the package path of the parameter)</li>
     * </ul>
     * </p>
     * The default path is: {@code src/main/resources}
     * <br>
     * The default name is: <code>{0}-schema.json</code>
     *
     * @param mainType targeted class for which the schema is being generated
     * @return The full path name of the schema file
     */
    private File getSchemaFile(Class<?> mainType) {
        // At first find the root location where the schema files are written
        File directory;
        if (this.schemaFilePath == null) {
            directory = new File("src" + File.separator + "main" + File.separator + "resources");
            this.getLog().debug("- No 'schemaFilePath' configured. Applying default: " + directory);
        } else {
            directory = this.schemaFilePath;
        }

        // Then build the full qualified file name.
        String fileName = MessageFormat.format(this.schemaFileName,
                // placeholder {0}
                mainType.getSimpleName(),
                // placeholder {1}
                mainType.getPackage().getName().replace('.', '/'));
        File schemaFile = new File(directory, fileName);

        // Make sure the directory is available
        try {
            Files.createDirectories(schemaFile.getParentFile().toPath());
        } catch (IOException e) {
            this.getLog().warn("Failed to ensure existence of " + schemaFile.getParent(), e);
        }

        return schemaFile;
    }

    /**
     * Get the JSON Schema generator. Create it when required.
     * <br>
     * Configuring it the specified options and adding the required modules.
     *
     * @return The configured generator
     * @throws MojoExecutionException Error exception
     */
    private SchemaGenerator getGenerator() throws MojoExecutionException {
        if (this.generator == null) {
            // Start with the generator builder
            SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(this.schemaVersion, this.getOptionPreset());

            // Add options when required
            this.setOptions(configBuilder);

            // Register the modules when specified
            this.setModules(configBuilder);

            // And construct the generator
            SchemaGeneratorConfig config = configBuilder.build();
            this.generator = new SchemaGenerator(config);
        }

        return this.generator;
    }

    /**
     * Determine the standard option preset of the generator. Take it from the configuration or set the default. The default is: PLAIN_JSON
     *
     * @return The OptionPreset
     */
    private OptionPreset getOptionPreset()  {
        if (this.options != null && this.options.preset != null) {
            return this.options.preset.getPreset();
        }
        this.getLog().debug("- No 'options/preset' configured. Applying default: PLAIN_JSON");
        return OptionPreset.PLAIN_JSON;
    }

    /**
     * Set the generator options form the configuration.
     *
     * @param configBuilder The configbuilder on which the options are set
     */
    private void setOptions(SchemaGeneratorConfigBuilder configBuilder) {
        if (this.options == null) {
            return;
        }
        // Enable all the configured options
        if (this.options.enabled != null) {
            for (Option option : this.options.enabled) {
                configBuilder.with(option);
            }
        }

        // Disable all the configured options
        if (this.options.disabled != null) {
            for (Option option : this.options.disabled) {
                configBuilder.without(option);
            }
        }
    }

    /**
     * Configure all the modules on the generator.
     *
     * @param configBuilder The builder on which the modules are added.
     * @throws MojoExecutionException Invalid module name or className configured
     */
    @SuppressWarnings("unchecked")
    private void setModules(SchemaGeneratorConfigBuilder configBuilder) throws MojoExecutionException {
        if (this.modules == null) {
            return;
        }
        for (GeneratorModule module : this.modules) {
            if (module.className != null && !module.className.isEmpty()) {
                try {
                    this.getLog().debug("- Adding custom Module " + module.className);
                    Class<? extends Module> moduleClass = (Class<? extends Module>) Class.forName(module.className);
                    Module moduleInstance = moduleClass.getConstructor().newInstance();
                    configBuilder.with(moduleInstance);
                } catch (ClassCastException | InstantiationException
                        | IllegalAccessException | NoSuchMethodException
                        | InvocationTargetException | ClassNotFoundException e) {
                    throw new MojoExecutionException("Error: Can not instantiate custom module" + module.className, e);
                }
            } else if (module.name != null) {
                switch (module.name) {
                case "Jackson":
                    this.getLog().debug("- Adding Jackson Module");
                    addJacksonModule(configBuilder, module);
                    break;
                case "JavaxValidation":
                    this.getLog().debug("- Adding Javax Validation Module");
                    addJavaxValidationModule(configBuilder, module);
                    break;
                case "Swagger15":
                    this.getLog().debug("- Adding Swagger 1.5 Module");
                    addSwagger15Module(configBuilder, module);
                    break;
                default:
                    throw new MojoExecutionException("Error: Module does not have a name in "
                            + "['Jackson', 'JavaxValidation', 'Swagger15'] or does not have a custom classname.");
                }
            }
        }
    }

    /**
     * Add the Swagger module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module        The modules section form the pom
     * @throws MojoExecutionException in case of problems
     */
    private void addSwagger15Module(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule module) throws MojoExecutionException {
        if (module.options == null || module.options.length == 0) {
            configBuilder.with(new SwaggerModule());
        } else {
            SwaggerOption[] swaggerOptions = new SwaggerOption[module.options.length];
            for (int i = 0; i < module.options.length; i++) {
                try {
                    swaggerOptions[i] = SwaggerOption.valueOf(module.options[i]);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Error: Unknown Swagger option " + module.options[i], e);
                }
            }
            configBuilder.with(new SwaggerModule(swaggerOptions));
        }
    }

    /**
     * Add the Javax Validation module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module        The modules section form the pom
     * @throws MojoExecutionException in case of problems
     */
    private void addJavaxValidationModule(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule module) throws MojoExecutionException {
        if (module.options == null || module.options.length == 0) {
            configBuilder.with(new JavaxValidationModule());
        } else {
            JavaxValidationOption[] javaxValidationOptions = new JavaxValidationOption[module.options.length];
            for (int i = 0; i < module.options.length; i++) {
                try {
                    javaxValidationOptions[i] = JavaxValidationOption.valueOf(module.options[i]);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Error: Unknown JavaxValidation option " + module.options[i], e);
                }
            }
            configBuilder.with(new JavaxValidationModule(javaxValidationOptions));
        }
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
                    throw new MojoExecutionException("Error: Unknown Jackson option " + module.options[i], e);
                }
            }
            configBuilder.with(new JacksonModule(jacksonOptions));
        }
    }

    /**
     * Write generated schema to a file.
     *
     * @param jsonSchema Generated schema to be written
     * @param file       The file to write to
     * @throws MojoExecutionException In case of problems when writing the targeted file
     */
    private void writeToFile(JsonNode jsonSchema, File file) throws MojoExecutionException {
        try (FileOutputStream outputStream = new FileOutputStream(file);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.print(jsonSchema.toPrettyString());
        } catch (IOException e) {
            throw new MojoExecutionException("Error: Can not write to file " + file, e);
        }
    }
}
