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

import java.io.File;
import java.io.FileReader;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SchemaGeneratorMojoTest extends AbstractMojoTestCase{

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Unit that will generate from a maven pom file fragment and compare with a reference file
     *
     * @throws Exception In case something goes wrong
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "DefaultConfig",
        "SchemaVersion",
        "JacksonModule",
        "JavaxValidationModule",
        "JakartaValidationModule",
        "Swagger15Module",
        "Swagger2Module",
        "Complete"
    })
    public void testGeneration(String testCaseName) throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources");

        // Execute the pom
        executePom(new File(testCaseLocation, testCaseName + "-pom.xml"));

        // Validate that the schema file is created.
        File resultFile = new File(generationLocation,testCaseName + "/TestClass-schema.json");
        Assertions.assertTrue(resultFile.exists());
        resultFile.deleteOnExit();

        // Validate that is the same as the reference
        File referenceFile = new File(testCaseLocation + "/" + testCaseName + "-reference.json");
        Assertions.assertTrue(referenceFile.exists());
        Assertions.assertTrue(FileUtils.contentEquals(resultFile, referenceFile),
                "Generated schema for " + testCaseName + " is not equal to the expected reference.");
    }

    /**
     * Unit test that will generate from a maven pom file fragment and expect a MojoExecutionException.
     *
     * @param testCaseName Name of the test case and file name prefix of the example {@code pom.xml}
     * @throws Exception In case something goes wrong
     */
    @ParameterizedTest
    @ValueSource(strings = { "ClassNotFound", "UnknownModule" })
    public void testPomErrors(String testCaseName) throws Exception {
        File testCaseLocation = new File("src/test/resources/error-test-cases");
        Assertions.assertThrows(MojoExecutionException.class,
                () -> executePom(new File(testCaseLocation, testCaseName + "-pom.xml")));
    }

    /**
     * Unit test that will generate from a maven pom file fragment and expect a ComponentConfigurationException.
     *
     * @param testCaseName Name of the test case and file name prefix of the example {@code pom.xml}
     * @throws Exception In case something goes wrong
     */
    @ParameterizedTest
    @ValueSource(strings = { "UnknownSchemaVersion", "UnknownGeneratorPreset" })
    public void testPomConfigurationErrors(String testCaseName) throws Exception {
        File testCaseLocation = new File("src/test/resources/error-test-cases");
        Assertions.assertThrows(ComponentConfigurationException.class,
                () -> executePom(new File(testCaseLocation, testCaseName + "-pom.xml")));
    }

    /**
     * Unit test to test the generation of schemas for multiple classes
     */
    @Test
    public void testTwoClasses() throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources/TwoClasses");

        // Execute the pom
        executePom(new File("src/test/resources/reference-test-cases/TwoClasses-pom.xml"));

        // Validate that the schema files are created.
        File resultFileA = new File(generationLocation,"TestClassA.schema");
        Assertions.assertTrue(resultFileA.exists());
        resultFileA.deleteOnExit();

        File resultFileB = new File(generationLocation,"TestClassB.schema");
        Assertions.assertTrue(resultFileB.exists());
        resultFileB.deleteOnExit();

        // Validate that they are the same as the reference
        File referenceFileA = new File(testCaseLocation + "/" + "TestClassA-reference.json");
        Assertions.assertTrue(referenceFileA.exists());
        Assertions.assertTrue(FileUtils.contentEquals(resultFileA, referenceFileA),
                "Generated schema for TestClassA is not equal to the expected reference.");

        File referenceFileB = new File(testCaseLocation + "/" + "TestClassB-reference.json");
        Assertions.assertTrue(referenceFileB.exists());
        Assertions.assertTrue(FileUtils.contentEquals(resultFileB, referenceFileB),
                "Generated schema for TestClassB is not equal to the expected reference.");
    }

    /**
     * Unit test to test the generation of schemas for multiple classes
     */
    @Test
    public void testPackageName() throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources/PackageName");

        // Execute the pom
        executePom(new File("src/test/resources/reference-test-cases/PackageName-pom.xml"));

        // Validate that the schema files are created.
        File resultFileA = new File(generationLocation,"TestClassA-schema.json");
        Assertions.assertTrue(resultFileA.exists());
        resultFileA.deleteOnExit();

        File resultFileB = new File(generationLocation,"TestClassB-schema.json");
        Assertions.assertTrue(resultFileB.exists());
        resultFileB.deleteOnExit();

        File resultFileC = new File(generationLocation,"TestClassC-schema.json");
        Assertions.assertTrue(resultFileC.exists());
        resultFileC.deleteOnExit();

        // Validate that they are the same as the reference
        File referenceFileA = new File(testCaseLocation + "/" + "TestClassA-reference.json");
        Assertions.assertTrue(referenceFileA.exists());
        Assertions.assertTrue(FileUtils.contentEquals(resultFileA, referenceFileA),
                "Generated schema for TestClassA is not equal to the expected reference.");

        File referenceFileB = new File(testCaseLocation + "/" + "TestClassB-reference.json");
        Assertions.assertTrue(referenceFileB.exists());
        Assertions.assertTrue(FileUtils.contentEquals(resultFileB, referenceFileB),
                "Generated schema for TestClassB is not equal to the expected reference.");

        File referenceFileC = new File(testCaseLocation + "/" + "TestClassC-reference.json");
        Assertions.assertTrue(referenceFileC.exists());
        Assertions.assertTrue(FileUtils.contentEquals(resultFileC, referenceFileC),
                "Generated schema for TestClassC is not equal to the expected reference.");
    }

    /**
     * Unit test to test the generation of schemas for multiple classes
     */
    @Test
    public void testFileNamePattern() throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources/SchemaFileName/schemas/"+
                "com/github/victools/jsonschema/plugin/maven/testpackage");

        // Execute the pom
        executePom(new File("src/test/resources/reference-test-cases/SchemaFileName-pom.xml"));

        // Validate that the schema files are created.
        File resultFileA = new File(generationLocation,"TestClassA.schema");
        Assertions.assertTrue(resultFileA.exists());
        resultFileA.deleteOnExit();

        File resultFileB = new File(generationLocation,"TestClassB.schema");
        Assertions.assertTrue(resultFileB.exists());
        resultFileB.deleteOnExit();

        // Validate that they are the same as the reference
        File referenceFileA = new File(testCaseLocation + "/" + "TestClassA-reference.json");
        Assertions.assertTrue(referenceFileA.exists());
        Assertions.assertTrue(FileUtils.contentEquals(resultFileA, referenceFileA),
                "Generated schema for TestClassA is not equal to the expected reference.");

        File referenceFileB = new File(testCaseLocation + "/" + "TestClassB-reference.json");
        Assertions.assertTrue(referenceFileB.exists());
        Assertions.assertTrue(FileUtils.contentEquals(resultFileB, referenceFileB),
                "Generated schema for TestClassB is not equal to the expected reference.");
    }

    /**
     * Execute the schema-generator plugin as define the the given pom file
     *
     * @param pomFile The pom file
     * @throws Exception In case of problems
     */
    private void executePom(File pomFile) throws Exception {
        // Get the maven pom file content
        Xpp3Dom pomDom;
        PlexusConfiguration configuration;
        try (FileReader pomReader = new FileReader(pomFile)) {
            pomDom = Xpp3DomBuilder.build(pomReader);
            configuration = this.extractPluginConfiguration("jsonschema-maven-plugin", pomDom);
        }

        // Configure the Mojo
        SchemaGeneratorMojo myMojo = (SchemaGeneratorMojo) this.lookupConfiguredMojo(new MavenProject(), "generate");
        myMojo = (SchemaGeneratorMojo) this.configureMojo(myMojo, configuration);

        // And execute
        myMojo.execute();
    }

}