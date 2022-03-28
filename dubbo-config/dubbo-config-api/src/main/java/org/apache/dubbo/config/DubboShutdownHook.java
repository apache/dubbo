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
package org.apache.dubbo.config;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The shutdown hook thread to do the clean up stuff.
 * This is a singleton in order to ensure there is only one shutdown hook registered.
 * Because {@link ApplicationShutdownHooks} use {@link java.util.IdentityHashMap}
 * to store the shutdown hooks.
 */
public class DubboShutdownHook extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DubboShutdownHook.class);

    private final ApplicationModel applicationModel;

    /**
     * Has it already been registered or not?
     */
    private final AtomicBoolean registered = new AtomicBoolean(false);

    /**
     * Has it already been destroyed or not?
     */
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public DubboShutdownHook(ApplicationModel applicationModel) {
        super("DubboShutdownHook");
        this.applicationModel = applicationModel;
        Assert.notNull(this.applicationModel, "ApplicationModel is null");
    }

    @Override
    public void run() {

        if (destroyed.compareAndSet(false, true)) {
            if (logger.isInfoEnabled()) {
                logger.info("Run shutdown hook now.");
            }

            doDestroy();
        }
    }

    private void doDestroy() {
        applicationModel.destroy();
    }

    /**
     * Register the ShutdownHook
     */
    public void register() {
        if (registered.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime().addShutdownHook(this);
            } catch (IllegalStateException e) {
                logger.warn("register shutdown hook failed: " + e.getMessage());
            } catch (Exception e) {
                logger.warn("register shutdown hook failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Unregister the ShutdownHook
     */
    public void unregister() {
        if (registered.compareAndSet(true, false)) {
            if (this.isAlive()) {
                // DubboShutdownHook thread is running
                return;
            }
            try {
                Runtime.getRuntime().removeShutdownHook(this);
            } catch (IllegalStateException e) {
                logger.warn("unregister shutdown hook failed: " + e.getMessage());
            } catch (Exception e) {
                logger.warn("unregister shutdown hook failed: " + e.getMessage(), e);
            }
        }
    }

    public boolean getRegistered() {
        return registered.get();
    }

}
