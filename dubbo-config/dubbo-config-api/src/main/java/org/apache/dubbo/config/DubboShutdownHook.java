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

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_SHUTDOWN_HOOK;

/**
 * The shutdown hook thread to do the cleanup stuff.
 * This is a singleton in order to ensure there is only one shutdown hook registered.
 * Because {@link ApplicationShutdownHooks} use {@link java.util.IdentityHashMap}
 * to store the shutdown hooks.
 */
public class DubboShutdownHook extends Thread {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboShutdownHook.class);

    private final ApplicationModel applicationModel;

    /**
     * Has it already been registered or not?
     */
    private final AtomicBoolean registered = new AtomicBoolean(false);

    /**
     * Has it already been destroyed or not?
     */
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    /**
     * Whether ignore listen on shutdown hook?
     */
    private final boolean ignoreListenShutdownHook;

    public DubboShutdownHook(ApplicationModel applicationModel) {
        super("DubboShutdownHook");
        this.applicationModel = applicationModel;
        Assert.notNull(this.applicationModel, "ApplicationModel is null");
        ignoreListenShutdownHook = Boolean.parseBoolean(ConfigurationUtils.getProperty(applicationModel, CommonConstants.IGNORE_LISTEN_SHUTDOWN_HOOK));
        if (ignoreListenShutdownHook) {
            logger.info(CommonConstants.IGNORE_LISTEN_SHUTDOWN_HOOK + " configured, will ignore add shutdown hook to jvm.");
        }
    }

    @Override
    public void run() {

        if (!ignoreListenShutdownHook && destroyed.compareAndSet(false, true)) {
            if (logger.isInfoEnabled()) {
                logger.info("Run shutdown hook now.");
            }

            doDestroy();
        }
    }

    private void doDestroy() {
        boolean hasModuleBindSpring = false;
        // check if any modules are bound to Spring
        for (ModuleModel module: applicationModel.getModuleModels()) {
            if (module.isLifeCycleManagedExternally()) {
                hasModuleBindSpring = true;
                break;
            }
        }
        if (hasModuleBindSpring) {
            int timeout = ConfigurationUtils.getServerShutdownTimeout(applicationModel);
            if (timeout > 0) {
                long start = System.currentTimeMillis();
                /**
                 * To avoid shutdown conflicts between Dubbo and Spring,
                 * wait for the modules bound to Spring to be handled by Spring util timeout.
                 */
                logger.info("Waiting for modules managed by Spring to be shut down.");
                while (!applicationModel.isDestroyed() && hasModuleBindSpring
                    && (System.currentTimeMillis() - start) < timeout) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                        hasModuleBindSpring = false;
                        if (!applicationModel.isDestroyed()) {
                            for (ModuleModel module: applicationModel.getModuleModels()) {
                                if (module.isLifeCycleManagedExternally()) {
                                    hasModuleBindSpring = true;
                                    break;
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        logger.warn(LoggerCodeConstants.INTERNAL_INTERRUPTED, "", "", e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        if (!applicationModel.isDestroyed()) {
            logger.info("Dubbo shuts down application " +
                "after Spring fails to do in time or doesn't do it completely.");
            applicationModel.destroy();
        }
    }

    /**
     * Register the ShutdownHook
     */
    public void register() {
        if (!ignoreListenShutdownHook && registered.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime().addShutdownHook(this);
            } catch (IllegalStateException e) {
                logger.warn(CONFIG_FAILED_SHUTDOWN_HOOK, "", "", "register shutdown hook failed: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.warn(CONFIG_FAILED_SHUTDOWN_HOOK, "", "", "register shutdown hook failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Unregister the ShutdownHook
     */
    public void unregister() {
        if (!ignoreListenShutdownHook && registered.compareAndSet(true, false)) {
            if (this.isAlive()) {
                // DubboShutdownHook thread is running
                return;
            }
            try {
                Runtime.getRuntime().removeShutdownHook(this);
            } catch (IllegalStateException e) {
                logger.warn(CONFIG_FAILED_SHUTDOWN_HOOK, "", "", "unregister shutdown hook failed: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.warn(CONFIG_FAILED_SHUTDOWN_HOOK, "", "", "unregister shutdown hook failed: " + e.getMessage(), e);
            }
        }
    }

    public boolean getRegistered() {
        return registered.get();
    }

}
