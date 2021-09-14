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

import org.apache.dubbo.common.lang.ShutdownHookCallback;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The shutdown hook thread to do the clean up stuff.
 * This is a singleton in order to ensure there is only one shutdown hook registered.
 * Because {@link ApplicationShutdownHooks} use {@link java.util.IdentityHashMap}
 * to store the shutdown hooks.
 */
public class DubboShutdownHook extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DubboShutdownHook.class);

    private final ShutdownHookCallbacks callbacks;

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
        this.callbacks = applicationModel.getBeanFactory().getBean(ShutdownHookCallbacks.class);
        Assert.notNull(this.callbacks, "ShutdownHookCallbacks is null");
    }

//    public static DubboShutdownHook getDubboShutdownHook() {
//        return DUBBO_SHUTDOWN_HOOK;
//    }

    @Override
    public void run() {
        String disableShutdownHookValue = System.getProperty(ConfigKeys.DUBBO_LIFECYCLE_DISABLE_SHUTDOWN_HOOK, "false");
        if (Boolean.parseBoolean(disableShutdownHookValue)) {
            if (logger.isWarnEnabled()) {
                logger.warn("Shutdown hook is disabled, please shutdown dubbo services by qos manually");
            }
            return;
        }

        if (destroyed.compareAndSet(false, true)) {
            if (logger.isInfoEnabled()) {
                logger.info("Run shutdown hook now.");
            }

            callback();
            doDestroy();
        }
    }

    /**
     * For testing purpose
     */
    void clear() {
        callbacks.clear();
    }

    private void callback() {
        callbacks.callback();
    }

    public DubboShutdownHook addCallback(ShutdownHookCallback callback) {
        callbacks.addCallback(callback);
        return this;
    }

    public Collection<ShutdownHookCallback> getCallbacks() {
        return callbacks.getCallbacks();
    }

    /**
     * Register the ShutdownHook
     */
    public void register() {
        if (registered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(this);
        }
    }

    /**
     * Unregister the ShutdownHook
     */
    public void unregister() {
        if (registered.compareAndSet(true, false)) {
            Runtime.getRuntime().removeShutdownHook(this);
        }
    }

    /**
     * Destroy all the resources, including registries and protocols.
     */
    public void doDestroy() {
        if (logger.isInfoEnabled()) {
            logger.info("Dubbo Service has been destroyed.");
        }
    }

    public boolean getRegistered() {
        return registered.get();
    }

}
