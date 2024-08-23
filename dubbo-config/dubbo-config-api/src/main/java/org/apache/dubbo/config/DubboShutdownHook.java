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
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static class SingletonHelper {
        private static final DubboShutdownHook INSTANCE = new DubboShutdownHook();
    }

    /**
     * Returns the singleton instance of <code>DubboShutdownHook</code>.
     *
     * @return singleton instance
     */
    public static DubboShutdownHook getInstance() {
        return SingletonHelper.INSTANCE;
    }

    private boolean checkExternalManagedModule(ApplicationModel applicationModel) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            if (moduleModel.isLifeCycleManagedExternally()) {
                return true;
            }
        }
        return false;
    }

    protected static int getServerShutdownTimeout(ApplicationModel appModel) {
        try {
            return ConfigurationUtils.getServerShutdownTimeout(appModel);
        } catch (RuntimeException re) {
            return 0;
        }
    }

    /**
     * Checks whether external managed {@code ModuleModel} exists, and wait until the corresponding external
     * managed modules of {@code ApplicationModel} has been destroyed or the serverShutdownTimeout expired.
     *
     */
    private void checkAndWaitUntilTimeout() {
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
                        DubboShutdownHook::getServerShutdownTimeout,
                        (existingValue, newValue) -> existingValue,
                        HashMap::new));

        final long retryInterval = 10;
        for (Map.Entry<ApplicationModel, Integer> entry : serverShutdownWaits.entrySet()) {
            ApplicationModel applicationModel = entry.getKey();
            Integer val = entry.getValue();

            if (val.intValue() <= 0 || applicationModel.isDestroyed()) {
                continue;
            }

            logger.info("Waiting for modules(" + applicationModel.getDesc() + ") managed by Spring to be shutdown.");
            boolean hasExternalBinding = true;
            while (hasExternalBinding
                    && !applicationModel.isDestroyed()
                    && System.currentTimeMillis() - start < val.intValue()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(retryInterval);
                } catch (InterruptedException e) {
                    logger.warn(LoggerCodeConstants.INTERNAL_INTERRUPTED, "", "", e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
                hasExternalBinding = checkExternalManagedModule(applicationModel);
            }
            if (hasExternalBinding) {
                long usage = System.currentTimeMillis() - start;
                logger.info("Dubbo wait for application(" + applicationModel.getDesc()
                        + ") managed by Spring to be shutdown failed, external managed module exists. Total time usage: "
                        + usage + "ms.");
            } else {
                logger.info("Module(" + applicationModel.getDesc()
                        + ") managed by Spring has been destroyed successfully.");
            }
        }
    }

    @Override
    public void run() {
        if (logger.isInfoEnabled()) {
            logger.info("Run shutdown hook now.");
        }

        checkAndWaitUntilTimeout();

        // Cleanup all resources arbitrarily when process exit.
        FrameworkModel.destroyAll();

        if (logger.isInfoEnabled()) {
            logger.info("Complete shutdown hook now.");
        }
    }

    /**
     * Ensure register only once.
     */
    private final AtomicBoolean registered = new AtomicBoolean(false);

    /**
     * Register the ShutdownHook
     */
    public void register() {
        if (!registered.compareAndSet(false, true)) {
            try {
                Runtime.getRuntime().addShutdownHook(this);
            } catch (Exception e) {
                logger.warn(CONFIG_FAILED_SHUTDOWN_HOOK, "", "", "register shutdown hook failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     *
     * @return indicator of hook registered or not.
     */
    public boolean isRegistered() {
        return registered.get();
    }
}
