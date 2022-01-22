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
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationOption;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger15.SwaggerOption;
import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

/**
 * Maven plugin for the victools/jsonschema-generator.
 */
@Mojo(name = "generate",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        threadSafe = true)
public class SchemaGeneratorMojo extends AbstractMojo {

    /**
     * Full name or glob pattern of the classes for which the JSON schema will be generated.
     */
    @Parameter(property = "classNames")
    private String[] classNames;

    /**
     * Full name or glob pattern of the packages for which a JSON schema will be generated for each contained class.
     */
    @Parameter(property = "packageNames")
    private String[] packageNames;

    /**
     * Full name or glob pattern of the classes NOT to generate a JSON schema for.
     */
    @Parameter(property = "excludeClassNames")
    private String[] excludeClassNames;

    /**
     * The directory path where the schema files are generated.
     * <br>
     * By default, this is: {@code src/main/resources}
     */
    @Parameter(property = "schemaFilePath")
    private File schemaFilePath;

    /**
     * The name of the file in which the generated schema is written. Allowing for two placeholders:
     * <ul>
     * <li><code>{0}</code> - containing the simple class name of the class for which the schema was generated</li>
     * <li><code>{1}</code> - containing the package path of the class for which the schema was generated</li>
     * </ul>
     * The default name is: <code>{0}-schema.json</code>
     */
    @Parameter(property = "schemaFileName", defaultValue = "{0}-schema.json")
    private String schemaFileName;

    /**
     * The schema version to be used: DRAFT_6, DRAFT_7, DRAFT_2019_09 or DRAFT_2020_12.
     */
    @Parameter(property = "schemaVersion", defaultValue = "DRAFT_7")
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
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * The generator to be used for all schema generations.
     */
    private SchemaGenerator generator;

    /**
     * The classloader used for loading generator modules and classes.
     */
    private URLClassLoader classLoader;

    /**
     * The list of all the classes on the classpath.
     */
    private List<PotentialSchemaClass> allTypes;

    /**
     * Invoke the schema generator.
     *
     * @throws MojoExecutionException An exception in case of errors and unexpected behavior
     */
    @Override
    public synchronized void execute() throws MojoExecutionException {
        // trigger initialization of the generator instance
        this.getGenerator();

        if (this.classNames != null) {
            for (String className : this.classNames) {
                this.getLog().info("Generating JSON Schema for <className>" + className + "</className>");
                generateSchema(className, false);
            }
        }

        if (this.packageNames != null) {
            for (String packageName : this.packageNames) {
                this.getLog().info("Generating JSON Schema for <packageName>" + packageName + "</packageName>");
                generateSchema(packageName, true);
            }
        }
    }

    /**
     * Generate the JSON schema for the given className.
     *
     * @param classOrPackageName The name or glob pattern of the class or package
     * @param targetPackage whether the given name or glob pattern refers to a package
     * @throws MojoExecutionException In case of problems
     */
    private void generateSchema(String classOrPackageName, boolean targetPackage) throws MojoExecutionException {
        Pattern filter = GlobHandler.createClassOrPackageNameFilter(classOrPackageName, targetPackage);
        List<PotentialSchemaClass> matchingClasses = this.getAllClassNames().stream()
                .filter(entry -> filter.matcher(entry.getAbsolutePathToMatch()).matches())
                .sorted()
                .collect(Collectors.toList());
        for (PotentialSchemaClass potentialTarget : matchingClasses) {
            if (potentialTarget.isAlreadyGenerated()) {
                this.getLog().info("- Skipping already generated " + potentialTarget.getFullClassName());
            } else {
                // Load the class for which the schema will be generated
                Class<?> schemaClass = this.loadClass(potentialTarget.getFullClassName());
                this.generateSchema(schemaClass);
                potentialTarget.setAlreadyGenerated();
            }
        }
        if (matchingClasses.isEmpty()) {
            StringBuilder message = new StringBuilder("No matching class found for \"")
                    .append(classOrPackageName)
                    .append("\" on classpath");
            if (this.excludeClassNames != null && this.excludeClassNames.length > 0) {
                message.append(" that wasn't excluded");
            }
            throw new MojoExecutionException(message.toString());
        }
    }

    /**
     * Generate the JSON schema for the given className.
     *
     * @param schemaClass The class for which the schema is to be generated
     * @throws MojoExecutionException In case of problems
     */
    private void generateSchema(Class<?> schemaClass) throws MojoExecutionException {
        JsonNode jsonSchema = getGenerator().generateSchema(schemaClass);
        File file = getSchemaFile(schemaClass);
        this.getLog().info("- Writing schema to file: " + file);
        this.writeToFile(jsonSchema, file);
    }

    /**
     * Get all the names of classes on the classpath.
     *
     * @return A set of classes as found on the classpath, that are not explicitly excluded
     */
    private List<PotentialSchemaClass> getAllClassNames() {
        if (this.allTypes == null) {
            Scanner subTypeScanner = Scanners.SubTypes.filterResultsBy(c -> true);
            URLClassLoader urlClassLoader = this.getClassLoader();
            ConfigurationBuilder configBuilder = new ConfigurationBuilder()
                    .forPackage("", urlClassLoader)
                    .addScanners(subTypeScanner);
            if (urlClassLoader != null) {
                configBuilder.addUrls(urlClassLoader.getURLs());
            }
            Reflections reflections = new Reflections(configBuilder);
            Stream<PotentialSchemaClass> allTypesStream = reflections.getAll(subTypeScanner)
                    .stream()
                    .map(PotentialSchemaClass::new);
            if (this.excludeClassNames != null && this.excludeClassNames.length > 0) {
                Set<Pattern> exclusions = Stream.of(this.excludeClassNames)
                        .map(excludeEntry -> GlobHandler.createClassOrPackageNameFilter(excludeEntry, false))
                        .collect(Collectors.toSet());
                allTypesStream = allTypesStream
                        .filter(typeEntry -> exclusions.stream().noneMatch(pattern -> pattern.matcher(typeEntry.getAbsolutePathToMatch()).matches()));
            }
            this.allTypes = allTypesStream.collect(Collectors.toList());
        }
        return this.allTypes;
    }

    /**
     * Return the file in which the schema has to be written.
     *
     * <p>
     * The path is determined based on the {@link #schemaFilePath} parameter.
     * <br>
     * The name of the file is determined based on the {@link #schemaFileName} parameter, which allows for two placeholders:
     * <ul>
     * <li><code>{0}</code> - containing the simple name of the class the schema was generated for</li>
     * <li><code>{1}</code> - containing the package path of the class the schema was generated for</li>
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
                mainType.getPackage().getName().replace('.', File.separatorChar));
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
            this.getLog().debug("Initializing Schema Generator");

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
    private OptionPreset getOptionPreset() {
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
                    Class<? extends Module> moduleClass = (Class<? extends Module>) this.loadClass(module.className);
                    Module moduleInstance = moduleClass.getConstructor().newInstance();
                    configBuilder.with(moduleInstance);
                } catch (ClassCastException | InstantiationException
                        | IllegalAccessException | NoSuchMethodException
                        | InvocationTargetException e) {
                    throw new MojoExecutionException("Error: Can not instantiate custom module " + module.className, e);
                }
            } else if (module.name != null) {
                switch (module.name) {
                case "Jackson":
                    this.getLog().debug("- Adding Jackson Module");
                    addJacksonModule(configBuilder, module);
                    break;
                case "JakartaValidation":
                    this.getLog().debug("- Adding Jakarta Validation Module");
                    addJakartaValidationModule(configBuilder, module);
                    break;
                case "JavaxValidation":
                    this.getLog().debug("- Adding Javax Validation Module");
                    addJavaxValidationModule(configBuilder, module);
                    break;
                case "Swagger15":
                    this.getLog().debug("- Adding Swagger 1.5 Module");
                    addSwagger15Module(configBuilder, module);
                    break;
                case "Swagger2":
                    this.getLog().debug("- Adding Swagger 2.x Module");
                    addSwagger2Module(configBuilder, module);
                    break;
                default:
                    throw new MojoExecutionException("Error: Module does not have a name in "
                            + "['Jackson', 'JakartaValidation', 'JavaxValidation', 'Swagger15', 'Swagger2'] or does not have a custom classname.");
                }
            }
        }
    }

    /**
     * Construct the classloader based on the project classpath.
     *
     * @return The classloader
     */
    private URLClassLoader getClassLoader() {
        if (this.classLoader == null) {
            List<String> runtimeClasspathElements = null;
            try {
                runtimeClasspathElements = project.getRuntimeClasspathElements();
            } catch (DependencyResolutionRequiredException e) {
                this.getLog().error("Failed to resolve runtime classpath elements", e);
            }

            if (runtimeClasspathElements != null) {
                URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
                for (int i = 0; i < runtimeClasspathElements.size(); i++) {
                    String element = runtimeClasspathElements.get(i);
                    try {
                        runtimeUrls[i] = new File(element).toURI().toURL();
                    } catch (MalformedURLException e) {
                        this.getLog().error("Failed to resolve runtime classpath element", e);
                    }
                }
                this.classLoader = new URLClassLoader(runtimeUrls,
                        Thread.currentThread().getContextClassLoader());
            }
        }

        return this.classLoader;
    }

    /**
     * Load a class from the plugin classpath enriched with the project dependencies.
     *
     * @param className Name of the class to be loaded
     * @return The loaded class
     * @throws MojoExecutionException In case of unexpected behavior
     */
    private Class<?> loadClass(String className) throws MojoExecutionException {
        try {
            return this.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Error loading class " + className, e);
        }
    }

    /**
     * Add the Swagger (1.5) module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module The modules section form the pom
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
     * Add the Swagger (2.x) module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module The modules section form the pom
     * @throws MojoExecutionException in case of problems
     */
    private void addSwagger2Module(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule module) throws MojoExecutionException {
        configBuilder.with(new Swagger2Module());
    }

    /**
     * Add the Javax Validation module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module The modules section form the pom
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
     * Add the Jakarta Validation module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module The modules section form the pom
     * @throws MojoExecutionException in case of problems
     */
    private void addJakartaValidationModule(SchemaGeneratorConfigBuilder configBuilder, GeneratorModule module) throws MojoExecutionException {
        if (module.options == null || module.options.length == 0) {
            configBuilder.with(new JakartaValidationModule());
        } else {
            JakartaValidationOption[] jakartaValidationOptions = new JakartaValidationOption[module.options.length];
            for (int i = 0; i < module.options.length; i++) {
                try {
                    jakartaValidationOptions[i] = JakartaValidationOption.valueOf(module.options[i]);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Error: Unknown JakartaValidation option " + module.options[i], e);
                }
            }
            configBuilder.with(new JakartaValidationModule(jakartaValidationOptions));
        }
    }

    /**
     * Add the Jackson module to the generator config.
     *
     * @param configBuilder The builder on which the config is added
     * @param module The modules section form the pom
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
     * @param file The file to write to
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
