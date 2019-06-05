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

import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedMember;

/**
 *
 * @param <O> type of reference/context the configuration applies to
 * @param <P> type of additional argument expected to be provided (e.g. associated type of origin or default value that may be overridden)
 * @param <R> type of the configuration result
 */
@FunctionalInterface
public interface ConfigFunction<O extends ResolvedMember<?>, P, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param origin the reference/context the configuration applies to
     * @param secondArg additional argument expected to be provided (e.g. associated type of origin or default value that may be overridden)
     * @param declaringType the origin's parent type
     * @return the function result (may be null to indicate no specific configuration applies)
     */
    R apply(O origin, P secondArg, ResolvedTypeWithMembers declaringType);
}
