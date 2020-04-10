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

package com.github.victools.jsonschema.generator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Common helper functions within tests.
 */
public abstract class TestUtils {

    private TestUtils() {
        // not intended for sub-typing
    }

    /**
     * Load a local file (e.g. example JSON Schema) from the given resource path, relative to the package of the indicated test class.
     *
     * @param testClass test class requesting a file to be loaded
     * @param resourcePath relative path to the target file
     * @return file contents
     * @throws IOException when the file loading failed
     */
    public static String loadResource(Class<?> testClass, String resourcePath) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = testClass.getResourceAsStream(resourcePath);
                Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.nextLine()).append('\n');
            }
        }
        String fileAsString = stringBuilder.toString();
        return fileAsString;
    }
}
