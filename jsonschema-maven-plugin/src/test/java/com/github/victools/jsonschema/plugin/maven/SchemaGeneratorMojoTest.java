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
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class SchemaGeneratorMojoTest {

    @Rule
    public MojoRule rule = new MojoRule();

    public Object[] parametersForTestGeneration() {
        return new Object[][]{
                {"DefaultConfig"},
                {"SchemaVersion"},
                {"JacksonModule"},
                {"Complete"}
        };
    }

    /**
     * Unit that will generate from a maven pom file fragment and compare with a reference file
     *
     * @throws Exception In case something goes wrong
     */
    @Test
    @Parameters
    public void testGeneration(String testCaseName) throws Exception {
        File testCaseLocation = new File("src/test/resources/reference-test-cases");
        File generationLocation = new File("target/generated-test-sources");

        // Execute the pom
        executePom(new File(testCaseLocation, testCaseName + "-pom.xml"));

        // Validate that the schema files is created.
        File resultFile = new File(generationLocation,testCaseName + ".json");
        Assert.assertTrue(resultFile.exists());

        // Validate that is the same as the reference
        File referenceFile = new File(testCaseLocation + "/" + testCaseName + "-reference.json");
        Assert.assertTrue(referenceFile.exists());
        Assert.assertTrue("Generated schema for " + testCaseName + " is not equal to the expected reference.",
                FileUtils.contentEquals(resultFile, referenceFile));
    }

    public Object[] parametersForTestPomErrors() {
        return new Object[][]{
                {"ClassNotFound"},
                {"UnknownModule"}
        };
    }

    /**
     * Unit test that will generate from a maven pom file fragment and expect a MojoExecutionException.
     *
     * @param testCaseName Name of the test case and file name prefix of the example {@code pom.xml}
     * @throws Exception In case something goes wrong
     */
    @Test(expected = MojoExecutionException.class)
    @Parameters
    public void testPomErrors(String testCaseName) throws Exception {
        File testCaseLocation = new File("src/test/resources/error-test-cases");
        executePom(new File(testCaseLocation, testCaseName + "-pom.xml"));
    }

    public Object[] parametersForTestPomConfigurationErrors() {
        return new Object[][]{
                {"UnknownSchemaVersion"},
                {"UnknownGeneratorPreset"}
        };
    }

    /**
     * Unit test that will generate from a maven pom file fragment and expect a ComponentConfigurationException.
     *
     * @param testCaseName Name of the test case and file name prefix of the example {@code pom.xml}
     * @throws Exception In case something goes wrong
     */
    @Test(expected = ComponentConfigurationException.class)
    @Parameters
    public void testPomConfigurationErrors(String testCaseName) throws Exception {
        File testCaseLocation = new File("src/test/resources/error-test-cases");
        executePom(new File(testCaseLocation, testCaseName + "-pom.xml"));
    }

    /**
     * Execute the schema-generator plugin as define the the given pom file
     *
     * @param pomFile The pom file
     * @throws Exception In case of problems
     */
    private void executePom(File pomFile) throws Exception {
        // Get the maven pom file content
        Xpp3Dom pomDom = Xpp3DomBuilder.build(new FileReader(pomFile));
        PlexusConfiguration configuration = rule.extractPluginConfiguration("jsonschema-maven-plugin", pomDom);

        // Configure the Mojo
        SchemaGeneratorMojo myMojo = (SchemaGeneratorMojo) rule.lookupConfiguredMojo(new MavenProject(), "generate");
        myMojo = (SchemaGeneratorMojo) rule.configureMojo(myMojo, configuration);

        // And execute
        myMojo.execute();
    }

}