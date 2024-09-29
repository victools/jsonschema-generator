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
import java.nio.charset.StandardCharsets;
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

public class SchemaGeneratorMojoTest extends AbstractMojoTestCase {

    private static final String CHARSET_NAME = StandardCharsets.UTF_8.name();

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
        File resultFile = new File(generationLocation, testCaseName + "/TestClass-schema.json");
        Assertions.assertTrue(resultFile.exists());
        resultFile.deleteOnExit();

        // Validate that is the same as the reference
        File referenceFile = new File(testCaseLocation, testCaseName + "-reference.json");
        Assertions.assertTrue(referenceFile.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFile, referenceFile, CHARSET_NAME),
                "Generated schema for " + testCaseName + " is not equal to the expected reference.\n"
                        + "Generated:\n"
                        + FileUtils.readFileToString(resultFile, CHARSET_NAME)
                        + "\n----------\n"
                        + "Expected:\n"
                        + FileUtils.readFileToString(referenceFile, CHARSET_NAME));
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
     * Unit test that will generate from a maven pom file fragment and expect no exception being thrown although no classes matches pattern.
     *
     * @throws Exception In case something goes wrong
     */
    @Test
    public void testDontFailIfNoClassesMatch() throws Exception {
        File testCaseLocation = new File("src/test/resources/error-test-cases");
        this.executePom(new File(testCaseLocation, "ClassNotFound-dontFailIfNoClassesMatch-pom.xml"));
        // no error thrown, i.e., build proceeds even without any schema having been generated
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
        File referenceFileA = new File(testCaseLocation, "TestClassA-reference.json");
        Assertions.assertTrue(referenceFileA.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileA, referenceFileA, CHARSET_NAME),
                "Generated schema for TestClassA is not equal to the expected reference.");

        File referenceFileB = new File(testCaseLocation, "TestClassB-reference.json");
        Assertions.assertTrue(referenceFileB.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileB, referenceFileB, CHARSET_NAME),
                "Generated schema for TestClassB is not equal to the expected reference.");
    }

    /**
     * Unit test to test the generation of schemas for multiple classes
     */
    @ParameterizedTest
    @ValueSource(strings = { "WithoutAbstracts", "WithoutInterfaces" })
    public void testPackageName(String scenario) throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources/PackageName" + scenario);

        // Execute the pom
        executePom(new File("src/test/resources/reference-test-cases/PackageName" + scenario + "-pom.xml"));

        // Validate that the schema files are created.
        File resultFileA = new File(generationLocation,"TestClassA-schema.json");
        Assertions.assertTrue(resultFileA.exists());
        resultFileA.deleteOnExit();

        // explicitly excluded, although it is in the target
        File resultFileB = new File(generationLocation,"TestClassB-schema.json");
        Assertions.assertFalse(resultFileB.exists());

        File resultFileC = new File(generationLocation,"TestClassC-schema.json");
        Assertions.assertTrue(resultFileC.exists());
        resultFileC.deleteOnExit();

        File resultFileAbstract = new File(generationLocation,"AbstractTestClass-schema.json");
        Assertions.assertNotEquals("WithoutAbstracts".equals(scenario), resultFileAbstract.exists());
        File resultFileInterface = new File(generationLocation,"TestInterface-schema.json");
        Assertions.assertNotEquals("WithoutInterfaces".equals(scenario), resultFileInterface.exists());

        // Validate that they are the same as the reference
        File referenceFileA = new File(testCaseLocation, "TestClassA-reference.json");
        Assertions.assertTrue(referenceFileA.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileA, referenceFileA, CHARSET_NAME),
                "Generated schema for TestClassA is not equal to the expected reference.");

        File referenceFileC = new File(testCaseLocation, "TestClassC-reference.json");
        Assertions.assertTrue(referenceFileC.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileC, referenceFileC, CHARSET_NAME),
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
        File referenceFileA = new File(testCaseLocation, "TestClassA-reference.json");
        Assertions.assertTrue(referenceFileA.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileA, referenceFileA, CHARSET_NAME),
                "Generated schema for TestClassA is not equal to the expected reference.");

        File referenceFileB = new File(testCaseLocation, "TestClassB-reference.json");
        Assertions.assertTrue(referenceFileB.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileB, referenceFileB, CHARSET_NAME),
                "Generated schema for TestClassB is not equal to the expected reference.");
    }

    /**
     * Unit test to test the generation of schemas for multiple classes bases on annotations
     */
    @Test
    public void testFileSingleAnnotation() throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources/AnnotationSingle");

        // Execute the pom
        executePom(new File("src/test/resources/reference-test-cases/AnnotationSingle-pom.xml"));

        // Validate that the correct schema files are created.

        // TestClassA is annotated with the wrong annotation
        File resultFileA = new File(generationLocation, "TestClassA-schema.json");
        Assertions.assertFalse(resultFileA.exists());
        resultFileA.deleteOnExit();

        // TestClassB has the correct annotation
        File resultFileB = new File(generationLocation, "TestClassB-schema.json");
        Assertions.assertTrue(resultFileB.exists());
        resultFileB.deleteOnExit();

        // Validate that they are the same as the reference
        File referenceFileB = new File(testCaseLocation, "TestClassB-reference.json");
        Assertions.assertTrue(referenceFileB.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileB, referenceFileB, CHARSET_NAME),
            "Generated schema for TestClassB is not equal to the expected reference.");
    }

    /**
     * Unit test to test the generation of schemas for multiple classes bases on multiple annotations
     */
    @Test
    public void testFileMultipleAnnotations() throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources/AnnotationMulti");

        // Execute the pom
        executePom(new File("src/test/resources/reference-test-cases/AnnotationMulti-pom.xml"));

        // Validate that the correct schema files are created.

        // TestClassA is annotated with one annotation
        File resultFileA = new File(generationLocation, "TestClassA-schema.json");
        Assertions.assertTrue(resultFileA.exists());
        resultFileA.deleteOnExit();

        // TestClassB has the correct annotation
        File resultFileB = new File(generationLocation, "TestClassB-schema.json");
        Assertions.assertTrue(resultFileB.exists());
        resultFileB.deleteOnExit();

        // Validate that they are the same as the reference
        File referenceFileA = new File(testCaseLocation, "TestClassA-reference.json");
        Assertions.assertTrue(referenceFileA.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileA, referenceFileA, CHARSET_NAME),
                "Generated schema for TestClassA is not equal to the expected reference.");

        File referenceFileB = new File(testCaseLocation, "TestClassB-reference.json");
        Assertions.assertTrue(referenceFileB.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileB, referenceFileB, CHARSET_NAME),
                "Generated schema for TestClassB is not equal to the expected reference.");
    }

    /**
     * Unit test to test the generation of schemas for multiple classes bases on multiple annotations
     */
    @Test
    public void testFileAnnotationClassnameMixed() throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources/AnnotationMixed");

        // Execute the pom
        executePom(new File("src/test/resources/reference-test-cases/AnnotationMixed-pom.xml"));

        // Validate that the correct schema files are created.

        // TestClassA is annotated with one annotation and mentioned by classname
        File resultFileA = new File(generationLocation, "TestClassA-schema.json");
        Assertions.assertTrue(resultFileA.exists());
        resultFileA.deleteOnExit();

        // TestClassB has the correct annotation but is not mentioned by classname
        File resultFileB = new File(generationLocation, "TestClassB-schema.json");
        Assertions.assertFalse(resultFileB.exists());
        resultFileB.deleteOnExit();

        // Validate that they are the same as the reference
        File referenceFileA = new File(testCaseLocation, "TestClassA-reference.json");
        Assertions.assertTrue(referenceFileA.exists());
        Assertions.assertTrue(FileUtils.contentEqualsIgnoreEOL(resultFileA, referenceFileA, CHARSET_NAME),
            "Generated schema for TestClassA is not equal to the expected reference.");
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
        MavenProject project = new MavenProject();
        // hack to get the plugin think that the classes in the testpackage are part of the project and get scanned
        project.getBuild().setOutputDirectory("target/test-classes");
        // Configure the Mojo
        SchemaGeneratorMojo myMojo = (SchemaGeneratorMojo) this.lookupConfiguredMojo(project, "generate");
        myMojo = (SchemaGeneratorMojo) this.configureMojo(myMojo, configuration);

        // And execute
        myMojo.execute();
    }

}
