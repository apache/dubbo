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
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_SHUTDOWN_HOOK;

/**
 * The shutdown hook thread to do the cleanup stuff.
 * This is a singleton in order to ensure there is only one shutdown hook registered.
 * Because {@link ApplicationShutdownHooks} use {@link java.util.IdentityHashMap}
 * to store the shutdown hooks.
 */
public class DubboShutdownHook extends Thread {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboShutdownHook.class);

    private DubboShutdownHook() {
        super("DubboShutdownHook");
    }

    private static volatile DubboShutdownHook instance;

    /**
     * Returns the singleton instance of <code>DubboShutdownHook</code>.
     *
     * @return singleton instance
     */
    public static DubboShutdownHook getInstance() {
        if (instance == null) {
            synchronized (DubboShutdownHook.class) {
                if (instance == null) {
                    instance = new DubboShutdownHook();
                }
            }
        }
        return instance;
    }

    /**
     * Checks whether external managed {@code ModuleModel} exists, and wait until the corresponding <p>
     * {@code ApplicationModel} destroyed or the serverShutdownTimeout expired.
     *
     */
    private void checkAndWaitForTimeout() {
        long start = System.currentTimeMillis();
        /* To avoid shutdown conflicts between Dubbo and Spring,
         * wait for the modules bound to Spring to be handled by Spring until timeout.
         */
        Map<ApplicationModel, Integer> serverShutdownWaits = FrameworkModel.getAllInstances().stream()
                .flatMap(fwkModel -> fwkModel.getAllApplicationModels().stream())
                .distinct()
                .filter(appModel ->
                        appModel.getModuleModels().stream().anyMatch(ModuleModel::isLifeCycleManagedExternally))
                .collect(Collectors.toMap(
                        appModel -> appModel,
                        appModel -> ConfigurationUtils.getServerShutdownTimeout(appModel),
                        (existingValue, newValue) -> existingValue,
                        HashMap::new));

        for (Map.Entry<ApplicationModel, Integer> entry : serverShutdownWaits.entrySet()) {
            ApplicationModel applicationModel = entry.getKey();
            Integer val = entry.getValue();

            if (val.intValue() <= 0) {
                continue;
            }

            logger.info("Waiting for modules(" + applicationModel.getDesc() + ") managed by Spring to be shutdown.");
            boolean hasModuleBindSpring = true;
            while (!applicationModel.isDestroyed()
                    && hasModuleBindSpring
                    && System.currentTimeMillis() - start < val.intValue()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                    hasModuleBindSpring = false;
                    if (!applicationModel.isDestroyed()) {
                        for (ModuleModel module : applicationModel.getModuleModels()) {
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

    @Override
    public void run() {
        if (logger.isInfoEnabled()) {
            logger.info("Run shutdown hook now.");
        }

        checkAndWaitForTimeout();

        // Cleanup all resources arbitrarily when process exit.
        FrameworkModel.destroyAll();
    }

    /**
     * Register the ShutdownHook
     */
    public void register() {
        try {
            Runtime.getRuntime().addShutdownHook(this);
        } catch (IllegalStateException e) {
            logger.warn(CONFIG_FAILED_SHUTDOWN_HOOK, "", "", "register shutdown hook failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.warn(CONFIG_FAILED_SHUTDOWN_HOOK, "", "", "register shutdown hook failed: " + e.getMessage(), e);
        }
    }
}
