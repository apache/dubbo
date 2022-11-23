/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.concurrent;

import org.apache.dubbo.common.resource.Disposable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * <p>
 * A safe and lazy and removable initializer implementation that wraps a
 * {@code Callable} object.
 * </p>
 * @see org.apache.commons.lang3.concurrent.AtomicSafeInitializer
 */
public class CallableSafeInitializer<T> {
    /** A guard which ensures that initialize() is called only once. */
    private final AtomicReference<CallableSafeInitializer<T>> factory =
            new AtomicReference<>();

    /** Holds the reference to the managed object. */
    private final AtomicReference<T> reference = new AtomicReference<>();

    /** The Callable to be executed. */
    private final Callable<T> callable;

    public CallableSafeInitializer(Callable<T> callable) {
        this.callable = callable;
    }

    /**
     * Get (and initialize, if not initialized yet) the required object
     *
     * @return lazily initialized object
     * exception
     */
    //@Override
    public final T get() {
        T result;

        while ((result = reference.get()) == null) {
            if (factory.compareAndSet(null, this)) {
                reference.set(initialize());
            }
        }

        return result;
    }

    /**
     * Creates and initializes the object managed by this
     * {@code AtomicInitializer}. This method is called by {@link #get()} when
     * the managed object is not available yet. An implementation can focus on
     * the creation of the object. No synchronization is needed, as this is
     * already handled by {@code get()}. This method is guaranteed to be called
     * only once.
     *
     * @return the managed data object
     */
    protected T initialize() {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public T remove() {
        return this.remove(null);
    }

    public T remove(Consumer<? super T> action) {
        // release
        T value = reference.getAndSet(null);
        if (value != null) {
            if (action != null) {
                action.accept(value);
            }
            if (value instanceof Disposable) {
                ((Disposable) value).destroy();
            }
        }
        factory.set(null);
        return value;
    }
}
