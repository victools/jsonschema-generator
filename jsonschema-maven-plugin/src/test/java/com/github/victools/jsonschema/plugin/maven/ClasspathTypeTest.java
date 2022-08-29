/*
 * Copyright 2022 VicTools.
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

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClasspathTypeTest {

    private static final String BASE_DIR =  "src/test/resources/dummy-classpathes";
    private static final String RUNTIME_PATH = BASE_DIR + "/runtime";
    private static final String COMPILE_PATH = BASE_DIR + "/compile";
    private static final String RUN_AND_COMPILE_PATH = BASE_DIR + "/both";
    private static final String PROJECT_PATH = BASE_DIR + "/project";

    private MavenProjectStub project;

    @BeforeEach
    public void setUp() {
        this.project = new MavenProjectStub();
        this.project.setRuntimeClasspathElements(Arrays.asList(RUNTIME_PATH, RUN_AND_COMPILE_PATH));
        this.project.setCompileSourceRoots(Arrays.asList(COMPILE_PATH, RUN_AND_COMPILE_PATH));

        Build build = new Build();
        build.setOutputDirectory(PROJECT_PATH);
        this.project.setBuild(build);
    }

    @Test
    public void testGetUrlsCompile() {
        List<URL> urls = ClasspathType.WITH_COMPILE_DEPENDENCIES.getUrls(this.project);

        Assertions.assertEquals(2, urls.size());

        String path1 = urls.get(0).getPath();
        String path2 = urls.get(1).getPath();
        Assertions.assertTrue(path1.contains(COMPILE_PATH) || path2.contains(COMPILE_PATH));
        Assertions.assertTrue(path1.contains(RUN_AND_COMPILE_PATH) || path2.contains(RUN_AND_COMPILE_PATH));
    }

    @Test
    public void testGetUrlsRuntime() {
        List<URL> urls = ClasspathType.WITH_RUNTIME_DEPENDENCIES.getUrls(this.project);

        Assertions.assertEquals(2, urls.size());

        String path1 = urls.get(0).getPath();
        String path2 = urls.get(1).getPath();
        Assertions.assertTrue(path1.contains(RUNTIME_PATH) || path2.contains(RUNTIME_PATH));
        Assertions.assertTrue(path1.contains(RUN_AND_COMPILE_PATH) || path2.contains(RUN_AND_COMPILE_PATH));
    }

    @Test
    public void testGetUrlsAll() {
        List<URL> urls = ClasspathType.WITH_ALL_DEPENDENCIES.getUrls(this.project);

        Assertions.assertEquals(3, urls.size()); // no double inclusion

        String path1 = urls.get(0).getPath();
        String path2 = urls.get(1).getPath();
        String path3 = urls.get(2).getPath();
        Assertions.assertTrue(path1.contains(COMPILE_PATH) || path2.contains(COMPILE_PATH) || path3.contains(COMPILE_PATH));
        Assertions.assertTrue(path1.contains(RUNTIME_PATH) || path2.contains(RUNTIME_PATH) || path3.contains(RUNTIME_PATH));
        Assertions.assertTrue(path1.contains(RUN_AND_COMPILE_PATH) || path2.contains(RUN_AND_COMPILE_PATH) || path3.contains(RUN_AND_COMPILE_PATH));
    }

    @Test
    public void testGetUrlsProject() {
        List<URL> urls = ClasspathType.PROJECT_ONLY.getUrls(this.project);

        Assertions.assertEquals(1, urls.size());

        String path1 = urls.get(0).getPath();
        Assertions.assertTrue(path1.contains(PROJECT_PATH));
    }

}
