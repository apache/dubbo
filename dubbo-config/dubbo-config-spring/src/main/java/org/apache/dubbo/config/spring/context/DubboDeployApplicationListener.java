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
package org.apache.dubbo.config.spring.context;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.deploy.DeployListenerAdapter;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.deploy.ModuleDeployer;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.spring.context.event.DubboApplicationStateEvent;
import org.apache.dubbo.config.spring.context.event.DubboModuleStateEvent;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.config.spring.util.LockUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModelConstants;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_START_MODEL;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_STOP_DUBBO_ERROR;

/**
 * An ApplicationListener to control Dubbo application.
 */
public class DubboDeployApplicationListener implements SmartLifecycle, ApplicationContextAware, Ordered {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboDeployApplicationListener.class);

    private static final String DUBBO_SHUTDOWN_PHASE_KEY = "dubbo.spring.shutdown.phase";

    private ApplicationContext applicationContext;

    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile int shutdownPhase = Integer.MIN_VALUE + 2000;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.applicationModel = DubboBeanUtils.getApplicationModel(applicationContext);
        this.moduleModel = DubboBeanUtils.getModuleModel(applicationContext);

        try {
            // Parse the user-configured shutdown phase.
            // Spring stops SmartLifecycle beans in descending phase order.
            // To ensure Dubbo shuts down LAST, we use a very LOW phase value by default.
            String configured = ConfigurationUtils.getProperty(moduleModel, DUBBO_SHUTDOWN_PHASE_KEY);
            if (configured != null) {
                shutdownPhase = Math.max(Integer.MIN_VALUE + 1, Integer.parseInt(configured.trim()));
            }
        } catch (Exception e) {
            logger.warn(
                    CONFIG_FAILED_START_MODEL, "", "", "Invalid value for property: " + DUBBO_SHUTDOWN_PHASE_KEY, e);
        }

        // listen deploy events and publish DubboApplicationStateEvent
        applicationModel.getDeployer().addDeployListener(new DeployListenerAdapter<ApplicationModel>() {
            @Override
            public void onStarting(ApplicationModel scopeModel) {
                publishApplicationEvent(DeployState.STARTING);
            }

            @Override
            public void onStarted(ApplicationModel scopeModel) {
                publishApplicationEvent(DeployState.STARTED);
            }

            @Override
            public void onCompletion(ApplicationModel scopeModel) {
                publishApplicationEvent(DeployState.COMPLETION);
            }

            @Override
            public void onStopping(ApplicationModel scopeModel) {
                publishApplicationEvent(DeployState.STOPPING);
            }

            @Override
            public void onStopped(ApplicationModel scopeModel) {
                publishApplicationEvent(DeployState.STOPPED);
            }

            @Override
            public void onFailure(ApplicationModel scopeModel, Throwable cause) {
                publishApplicationEvent(cause);
            }
        });
        moduleModel.getDeployer().addDeployListener(new DeployListenerAdapter<ModuleModel>() {
            @Override
            public void onStarting(ModuleModel scopeModel) {
                publishModuleEvent(DeployState.STARTING);
            }

            @Override
            public void onStarted(ModuleModel scopeModel) {
                publishModuleEvent(DeployState.STARTED);
            }

            @Override
            public void onCompletion(ModuleModel scopeModel) {
                publishModuleEvent(DeployState.COMPLETION);
            }

            @Override
            public void onStopping(ModuleModel scopeModel) {
                publishModuleEvent(DeployState.STOPPING);
            }

            @Override
            public void onStopped(ModuleModel scopeModel) {
                publishModuleEvent(DeployState.STOPPED);
            }

            @Override
            public void onFailure(ModuleModel scopeModel, Throwable cause) {
                publishModuleEvent(cause);
            }
        });
    }

    private void publishApplicationEvent(DeployState state) {
        applicationContext.publishEvent(new DubboApplicationStateEvent(applicationModel, state));
    }

    private void publishApplicationEvent(Throwable cause) {
        applicationContext.publishEvent(new DubboApplicationStateEvent(applicationModel, DeployState.FAILED, cause));
    }

    private void publishModuleEvent(DeployState state) {
        applicationContext.publishEvent(new DubboModuleStateEvent(moduleModel, state));
    }

    private void publishModuleEvent(Throwable cause) {
        applicationContext.publishEvent(new DubboModuleStateEvent(moduleModel, DeployState.FAILED, cause));
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void start() {
        // Atomic check to ensure start logic runs only once.
        if (running.compareAndSet(false, true)) {
            ModuleDeployer deployer = moduleModel.getDeployer();
            Assert.notNull(deployer, "Module deployer is null");
            Object singletonMutex = LockUtils.getSingletonMutex(applicationContext);

            Future<?> future;
            synchronized (singletonMutex) {
                // Start the Dubbo module via the deployer.
                future = deployer.start();
            }

            // If not running in background, wait for the startup to finish.
            if (!deployer.isBackground()) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    // Preserve interrupt status
                    Thread.currentThread().interrupt();
                    logger.warn(
                            CONFIG_FAILED_START_MODEL,
                            "",
                            "",
                            "Interrupted while waiting for dubbo module start: " + e.getMessage());
                } catch (Exception e) {
                    logger.warn(CONFIG_FAILED_START_MODEL, "", "", "Error starting dubbo module: " + e.getMessage(), e);
                    // If start fails, reset the running state to allow proper shutdown
                    running.set(false);
                }
            }
        }
    }

    @Override
    public void stop() {
        stopInternal();
    }

    @Override
    public void stop(@NonNull Runnable callback) {
        try {
            stopInternal();
        } finally {
            try {
                callback.run();
            } catch (Throwable t) {
                logger.warn(
                        CONFIG_STOP_DUBBO_ERROR, "", "", "Exception while executing SmartLifecycle stop callback", t);
            }
        }
    }

    private void stopInternal() {
        // Ensure shutdown logic is executed only once.
        boolean changed = running.compareAndSet(true, false);
        if (changed) {
            logger.info("Stopping Dubbo module (SmartLifecycle) — phase={}", shutdownPhase);
            try {
                // Determine whether Dubbo should remain running after Spring context shutdown
                Object value = moduleModel.getAttribute(ModelConstants.KEEP_RUNNING_ON_SPRING_CLOSED);
                if (value == null) {
                    value = ConfigurationUtils.getProperty(
                            moduleModel, ModelConstants.KEEP_RUNNING_ON_SPRING_CLOSED_KEY);
                }
                boolean keepRunningOnClosed = Boolean.parseBoolean(String.valueOf(value));

                // Destroy the module only if not explicitly configured to keep running.
                if (!keepRunningOnClosed && !moduleModel.isDestroyed()) {
                    moduleModel.destroy();
                } else {
                    logger.info("KEEP_RUNNING_ON_SPRING_CLOSED is true — skipping module destroy");
                }
            } catch (Throwable e) {
                logger.error(CONFIG_STOP_DUBBO_ERROR, "", "", "Error stopping dubbo module: " + e.getMessage(), e);
            } finally {
                try {
                    DubboSpringInitializer.remove(applicationContext);
                } catch (Throwable t) {
                    logger.warn(CONFIG_STOP_DUBBO_ERROR, "", "", "Failed to remove DubboSpringInitializer binding", t);
                }
            }
        } else {
            // Even if already stopped, ensure cleanup happens to be safe.
            try {
                DubboSpringInitializer.remove(applicationContext);
            } catch (Throwable t) {
                logger.warn(
                        CONFIG_STOP_DUBBO_ERROR,
                        "",
                        "",
                        "Failed to remove DubboSpringInitializer binding on repeated stop",
                        t);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return shutdownPhase;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
