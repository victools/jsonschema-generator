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

import com.fasterxml.jackson.databind.BeanDescription;
import java.lang.reflect.Member;

/**
 * Represents a function that accepts three arguments and produces a result. This is an extension of {@link java.util.function.BiFunction}.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #apply(Member, Object, BeanDescription)}.
 *
 * @param <O> the type of the first argument to the function (i.e. the reference/context the configuration refers to)
 * @param <P> the type of the second argument to the function
 * @param <R> the type of the result of the function (i.e. the configuration result)
 */
@FunctionalInterface
public interface ConfigFunction<O, P, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param origin the first function argument (i.e. the reference/context the configuration refers to)
     * @param param the second function argument
     * @param declaringContext the declaring type's description
     * @return the configuration result (or {@code null} if no special behaviour is desired after all)
     */
    R apply(O origin, P param, BeanDescription declaringContext);

}
