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
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger15.SwaggerOption;
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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Maven plugin for the victools/jsonschema-generator.
 */
@Mojo(name = "generate-schema",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class SchemaGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Full name of the classes for which the JSON schema will be generated.
     */
    @Parameter(property = "targetTypes")
    private String[] classNames;

    /**
     * Full name of a package for which for all classes in the package JSON schemas will be generated.
     */
    @Parameter(property = "targetPackages")
    private String[] packageNames;

    /**
     * The directory path where the schema files is generated.
     * By default this is in the resources directory.
     */
    @Parameter(property = "schemaFilePath")
    private String schemaFilePath;

    /**
     * The pattern according which the name of the generated schema file is derived.
     */
    @Parameter(property = "schemaFileNamePattern", defaultValue = "{0}-schema.json")
    private String schemaFileNamePattern;

    /**
     * The schema version to be used: DRAFT_6, DRAFT_7 or DRAFT_2019_09.
     */
    @Parameter(property = "schemaVersion", defaultValue = "DRAFT_7")
    private String schemaVersion;

    /**
     * The options for the generator.
     */
    @Parameter(property = "options")
    private GeneratorOptions options;

    /**
     * Selection of Modules that need to be activated during generation.
     */
    @Parameter(property = "modules")
    private GeneratorModule[] modules;

    /**
     * Classloader to find the classes based on the maven specified buildpath
     */
    private URLClassLoader classLoader = null;

    private ObjectMapper mapper;
    private SchemaGenerator generator;

    /**
     * Invoke the schema generator.
     *
     * @throws MojoExecutionException An exception in case of errors and unexpected behavior
     */
    public void execute() throws MojoExecutionException {
        // Inform the user
        logInfo();

        // Create the rightly configured generator
        mapper = new ObjectMapper();
        generator = createGenerator(mapper);

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
     * @param packageName The name of the package
     */
    private void generateSchemaForPackage(String packageName) {

    }

    /**
     * Generate the JSON schema for the given className
     * @param className The name of the class
     * @throws MojoExecutionException In case of problems
     */
    private void generateSchema(String className) throws MojoExecutionException {
        // Load the class for which the schema will be generated
        Class<?> schemaClass;
        try {
            schemaClass = getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Error loading class " + className, e);
        }

        // Generate the schema
        JsonNode jsonSchema = generator.generateSchema(schemaClass);

        // Write the result
        File schemaFile = getSchemaFile(className);
        try {
            String schema = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
            writeToFile(schema, schemaFile);
        } catch (JsonProcessingException e) {
            throw new MojoExecutionException("Error writing schema file at " + schemaFile, e);
        }
    }

    /**
     * Log a properly formatted info string
     */
    private void logInfo() {
        StringBuilder logText = new StringBuilder("Generating JSON Schema for ");
        if (classNames != null) {
            logText.append("class");
            if (classNames.length > 1) {
                logText.append("es");
            }
            logText.append(" ");
            logText.append(String.join(", ", classNames));
            if (packageNames != null) {
                logText.append(" and ");
            }
        }
        if (packageNames != null) {
            logText.append("package");
            if (packageNames.length > 1) {
                logText.append("s");
            }
            logText.append(" ");
            logText.append(String.join(", ", packageNames));
        }
    }

    /**
     * Construct the classloader based on the project classpath.
     *
     * @return The classloader
     * @throws MojoExecutionException in case of problems
     */
    private URLClassLoader getClassLoader() throws MojoExecutionException {
        if (classLoader == null) {
            List<String> runtimeClasspathElements;
            try {
                runtimeClasspathElements = project.getRuntimeClasspathElements();
            } catch (DependencyResolutionRequiredException e) {
                throw new MojoExecutionException("Error: Class path construction problem.", e);
            }

            if (runtimeClasspathElements != null) {
                URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
                for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                    String element = runtimeClasspathElements.get(i);
                    try {
                        runtimeUrls[i] = new File(element).toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new MojoExecutionException("Error: Class path construction problem.", e);
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
     * @param fullClassName The fully qualified name of the class for which a schema file names is determined
     * @return The full path name of the schema file
     */
    private File getSchemaFile(String fullClassName) throws MojoExecutionException {
        // Find the root folder for outputting
        File filePath;
        if (schemaFilePath == null || schemaFilePath.isEmpty()) {
            filePath = new File("src" + File.separator + "main" + File.separator + "resources");
        } else {
            filePath = new File(schemaFilePath);
        }

        // Get only the name of the class. Remove a possible package prefix.
        String[] classNameParts = fullClassName.split(Pattern.quote("."));
        String className = classNameParts[classNameParts.length-1];
        String packagePath =
                String.join("/", Arrays.copyOf(classNameParts, classNameParts.length -1));

        // Apply the given pattern
        String fileName = MessageFormat.format(schemaFileNamePattern, className, packagePath);

        File schemaFile = new File(filePath, fileName);
        File parentFile = schemaFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new MojoExecutionException("Error creating directory: " + parentFile);
        }

        return schemaFile;
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
                new SchemaGeneratorConfigBuilder(mapper, getSchemaVersion(), getOptionPreset());

        // Add options when required
        setOptions(configBuilder, options);

        // Register the modules when specified
        setModules(configBuilder, modules);

        // And construct the generator
        SchemaGeneratorConfig config = configBuilder.build();
        return new SchemaGenerator(config);
    }

    /**
     * Get the schema version to be used from the configuration.
     *
     * @return The schema version
     * @throws MojoExecutionException Exception in case of an unknown value
     */
    private SchemaVersion getSchemaVersion() throws MojoExecutionException {
        try {
            return SchemaVersion.valueOf(schemaVersion);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Error: Unknown schema version " + schemaVersion, e);
        }
    }

    /**
     * Determine the standard option preset of the generator. Take it from the configuration or set the default.
     * The default is: PLAIN_JSON
     *
     * @return The OptionPreset
     * @throws MojoExecutionException In case an unknown preset was used.
     */
    private OptionPreset getOptionPreset() throws MojoExecutionException {
        // Unfortunately OptionPreset is not an Enum. So have to do some hardcoding now.
        if (options.preset != null && !options.preset.isEmpty()) {
            switch (options.preset) {
            case "FULL_DOCUMENTATION":
                return OptionPreset.FULL_DOCUMENTATION;
            case "PLAIN_JSON":
                return OptionPreset.PLAIN_JSON;
            case "JAVA_OBJECT":
                return OptionPreset.JAVA_OBJECT;
            default:
                throw new MojoExecutionException("Error: Option preset is not one of "
                        + "['FULL_DOCUMENTATION', 'PLAIN_JSON', 'JAVA_OBJECT']");
            }
        }

        return OptionPreset.PLAIN_JSON;
    }

    /**
     * Set the generator options form the configuration.
     *
     * @param configBuilder The configbuilder on which the options are set
     * @param options       The options from the pom file
     */
    private void setOptions(SchemaGeneratorConfigBuilder configBuilder, GeneratorOptions options) {
        // Enable all the configured options
        if (options.enabled != null) {
            for (Option option : options.enabled) {
                configBuilder.with(option);
            }
        }

        // Disable all the configured options
        if (options.disabled != null) {
            for (Option option : options.disabled) {
                configBuilder.without(option);
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
    private void setModules(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule[] modules) throws MojoExecutionException {
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
                    getLog().info("- Adding Jackson Module");
                    addJacksonModule(configBuilder, module);
                    break;
                case "JavaxValidation":
                    getLog().info("- Adding Javax Validation Module");
                    addJavaxValidationModule(configBuilder, module);
                    break;
                case "Swagger15":
                    getLog().info("- Adding Swagger 1.5 Module");
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
                    throw new MojoExecutionException("Error: Unknown Swagger option " + swaggerOptions[i], e);
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
                    throw new MojoExecutionException("Error: Unknown JavaxValidation option " + javaxValidationOptions[i], e);
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
                    throw new MojoExecutionException("Error: Unknown Jackson option " + jacksonOptions[i], e);
                }
            }
            configBuilder.with(new JacksonModule(jacksonOptions));
        }
    }

    /**
     * Write content to a file.
     *
     * @param content Content to be written
     * @param file    The file to write to
     * @throws MojoExecutionException In case of problems
     */
    private void writeToFile(String content, File file) throws MojoExecutionException {
        try {
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            PrintWriter pw = new PrintWriter(writer);
            pw.print(content);
            pw.close();
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Error: Can not write to file " + file, e);
        }
    }
}


