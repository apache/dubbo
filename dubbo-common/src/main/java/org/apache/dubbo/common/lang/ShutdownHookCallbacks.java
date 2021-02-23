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
package org.apache.dubbo.common.lang;

import org.apache.dubbo.common.extension.ExtensionLoader;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Phaser;

import static java.util.Collections.sort;
import static org.apache.dubbo.common.function.ThrowableAction.execute;

/**
 * The compose {@link ShutdownHookCallback} class to manipulate one and more {@link ShutdownHookCallback} instances
 *
 * @since 2.7.5
 */
public class ShutdownHookCallbacks {

    public static final ShutdownHookCallbacks INSTANCE = new ShutdownHookCallbacks();

    private final List<ShutdownHookCallback> callbacks = new LinkedList<>();

    /**
     * This variable is used to make sure that the asynchronous thread can be executed correctly
     * in the case of graceful shutdown.
     * <br/><br/>
     * When you have a thread that must be run asynchronously when shutting down.
     * You can register the phaser before the thread run, and unregister this when the thread is finished.
     */
    public static final Phaser SHUTDOWN_PHASER = new Phaser(1);

    ShutdownHookCallbacks() {
        loadCallbacks();
    }

    public ShutdownHookCallbacks addCallback(ShutdownHookCallback callback) {
        synchronized (this) {
            this.callbacks.add(callback);
        }
        return this;
    }

    public Collection<ShutdownHookCallback> getCallbacks() {
        synchronized (this) {
            sort(this.callbacks);
            return this.callbacks;
        }
    }

    public void clear() {
        synchronized (this) {
            callbacks.clear();
        }
    }

    private void loadCallbacks() {
        ExtensionLoader<ShutdownHookCallback> loader =
                ExtensionLoader.getExtensionLoader(ShutdownHookCallback.class);
        loader.getSupportedExtensionInstances().forEach(this::addCallback);
    }

    public void callback() {
        getCallbacks().forEach(callback -> execute(callback::callback));

        // Waiting for all threads to finish.
        SHUTDOWN_PHASER.arriveAndAwaitAdvance();
    }
}