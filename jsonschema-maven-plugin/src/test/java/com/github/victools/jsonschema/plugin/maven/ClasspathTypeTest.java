package com.github.victools.jsonschema.plugin.maven;

import static org.junit.jupiter.api.Assertions.*;


import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClasspathTypeTest {


    String baseDir = String.join("/", "src", "test", "resources", "dummy-classpathes");

    MavenProjectStub project;
    private String runtimePath;
    private String compilePath;
    private String runAndCompilePath;
    private String projectPath;


    @BeforeEach
    void setUp() {
        project = new MavenProjectStub();

        runtimePath = String.join("/", baseDir, "runtime");
        compilePath = String.join("/", baseDir, "compile");
        runAndCompilePath = String.join("/", baseDir, "both");
        projectPath = String.join("/", baseDir, "project");

        List<String> runtimeElements = new ArrayList<>();
        runtimeElements.add(runtimePath);
        runtimeElements.add(runAndCompilePath);
        project.setRuntimeClasspathElements(runtimeElements);

        List<String> compileElements = new ArrayList<>();
        compileElements.add(compilePath);
        compileElements.add(runAndCompilePath);
        project.setCompileSourceRoots(compileElements);

        Build build = new Build();
        build.setOutputDirectory(projectPath);
        project.setBuild(build);

    }

    @Test
    void testGetUrlsCompile() {
        List<URL> urls = ClasspathType.WITH_COMPILE_DEPENDENCIES.getUrls(project);

        assertEquals(2, urls.size());

        String path1 = urls.get(0).getPath();
        String path2 = urls.get(1).getPath();
        assertTrue(path1.contains(compilePath) || path2.contains(compilePath));
        assertTrue(path1.contains(runAndCompilePath) || path2.contains(runAndCompilePath));
    }

    @Test
    void testGetUrlsRuntime() {
        List<URL> urls = ClasspathType.WITH_RUNTIME_DEPENDENCIES.getUrls(project);

        assertEquals(2, urls.size());

        String path1 = urls.get(0).getPath();
        String path2 = urls.get(1).getPath();
        assertTrue(path1.contains(runtimePath) || path2.contains(runtimePath));
        assertTrue(path1.contains(runAndCompilePath) || path2.contains(runAndCompilePath));
    }

    @Test
    void testGetUrlsAll() {
        List<URL> urls = ClasspathType.WITH_ALL_DEPENDENCIES.getUrls(project);

        assertEquals(3, urls.size()); // no double inclusion

        String path1 = urls.get(0).getPath();
        String path2 = urls.get(1).getPath();
        String path3 = urls.get(2).getPath();
        assertTrue(path1.contains(compilePath) || path2.contains(compilePath) || path3.contains(compilePath));
        assertTrue(path1.contains(runtimePath) || path2.contains(runtimePath) || path3.contains(runtimePath));
        assertTrue(path1.contains(runAndCompilePath) || path2.contains(runAndCompilePath) || path3.contains(
            runAndCompilePath));
    }

    @Test
    void testGetUrlsProject() {
        List<URL> urls = ClasspathType.PROJECT_ONLY.getUrls(project);

        assertEquals(1, urls.size());

        String path1 = urls.get(0).getPath();
        assertTrue(path1.contains(projectPath));
    }



}
