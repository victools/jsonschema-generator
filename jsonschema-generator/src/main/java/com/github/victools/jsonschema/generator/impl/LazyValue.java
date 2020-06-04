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

package com.github.victools.jsonschema.generator.impl;

import java.util.function.Supplier;

/**
 * Wrapper for a value that should only be lazily initialised when being accessed for the first time.
 *
 * @param <T> type of wrapped value
 */
public class LazyValue<T> {

    private final Supplier<? extends T> supplier;
    private boolean initPending;
    private T value;

    /**
     * Constructor, not yet invoking the given {@link Supplier}.
     *
     * @param supplier value look-up to be performed the first time {@link #get()} is being called
     */
    public LazyValue(Supplier<? extends T> supplier) {
        this.supplier = supplier;
        this.initPending = true;
    }

    /**
     * Look-up the wrapped value, loading it on first invocation.
     *
     * @return wrapped value
     */
    public T get() {
        if (this.initPending) {
            // explicitly NOT thread-safe as it seems unnecessary here to add that overhead
            this.value = this.supplier.get();
            this.initPending = false;
        }
        return this.value;
    }
}
