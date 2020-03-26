/*
 * Copyright 2019 VicTools.
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

/**
 * Functional interface for realising one of various configurations.
 *
 * @param <S> type of scope/type representation the configuration applies to
 * @param <R> type of the configuration result
 */
@FunctionalInterface
public interface ConfigFunction<S extends TypeScope, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param target the targeted type representation the configuration applies to
     * @return the function result (may be null to indicate no specific configuration applies)
     */
    R apply(S target);
}
