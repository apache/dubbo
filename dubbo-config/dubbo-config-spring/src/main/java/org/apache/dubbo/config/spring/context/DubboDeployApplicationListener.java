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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_START_MODEL;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_STOP_DUBBO_ERROR;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

/**
 * An ApplicationListener to control Dubbo application.
 */
public class DubboDeployApplicationListener
        implements ApplicationListener<ApplicationContextEvent>, ApplicationContextAware, Ordered {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(DubboDeployApplicationListener.class);

    private ApplicationContext applicationContext;

    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.applicationModel = DubboBeanUtils.getApplicationModel(applicationContext);
        this.moduleModel = DubboBeanUtils.getModuleModel(applicationContext);
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
            public void onStopping(ApplicationModel scopeModel) {
                publishApplicationEvent(DeployState.STOPPING);
            }

            @Override
            public void onStopped(ApplicationModel scopeModel) {
                publishApplicationEvent(DeployState.STOPPED);
            }

            @Override
            public void onFailure(ApplicationModel scopeModel, Throwable cause) {
                publishApplicationEvent(DeployState.FAILED, cause);
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
            public void onStopping(ModuleModel scopeModel) {
                publishModuleEvent(DeployState.STOPPING);
            }

            @Override
            public void onStopped(ModuleModel scopeModel) {
                publishModuleEvent(DeployState.STOPPED);
            }

            @Override
            public void onFailure(ModuleModel scopeModel, Throwable cause) {
                publishModuleEvent(DeployState.FAILED, cause);
            }
        });
    }

    private void publishApplicationEvent(DeployState state) {
        applicationContext.publishEvent(new DubboApplicationStateEvent(applicationModel, state));
    }

    private void publishApplicationEvent(DeployState state, Throwable cause) {
        applicationContext.publishEvent(new DubboApplicationStateEvent(applicationModel, state, cause));
    }

    private void publishModuleEvent(DeployState state) {
        applicationContext.publishEvent(new DubboModuleStateEvent(moduleModel, state));
    }

    private void publishModuleEvent(DeployState state, Throwable cause) {
        applicationContext.publishEvent(new DubboModuleStateEvent(moduleModel, state, cause));
    }

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
        if (nullSafeEquals(applicationContext, event.getSource())) {
            if (event instanceof ContextRefreshedEvent) {
                onContextRefreshedEvent((ContextRefreshedEvent) event);
            } else if (event instanceof ContextClosedEvent) {
                onContextClosedEvent((ContextClosedEvent) event);
            }
        }
    }

    private void onContextRefreshedEvent(ContextRefreshedEvent event) {
        ModuleDeployer deployer = moduleModel.getDeployer();
        Assert.notNull(deployer, "Module deployer is null");
        Object singletonMutex = LockUtils.getSingletonMutex(applicationContext);
        // start module
        Future future = null;
        synchronized (singletonMutex) {
            future = deployer.start();
        }

        // if the module does not start in background, await finish
        if (!deployer.isBackground()) {
            try {
                future.get();
            } catch (InterruptedException e) {
                logger.warn(
                        CONFIG_FAILED_START_MODEL,
                        "",
                        "",
                        "Interrupted while waiting for dubbo module start: " + e.getMessage());
            } catch (Exception e) {
                logger.warn(
                        CONFIG_FAILED_START_MODEL,
                        "",
                        "",
                        "An error occurred while waiting for dubbo module start: " + e.getMessage(),
                        e);
            }
        }
    }

    private void onContextClosedEvent(ContextClosedEvent event) {
        try {
            Object value = moduleModel.getAttribute(ModelConstants.KEEP_RUNNING_ON_SPRING_CLOSED);
            if (value == null) {
                value = ConfigurationUtils.getProperty(moduleModel, ModelConstants.KEEP_RUNNING_ON_SPRING_CLOSED_KEY);
            }
            boolean keepRunningOnClosed = Boolean.parseBoolean(String.valueOf(value));
            if (!keepRunningOnClosed && !moduleModel.isDestroyed()) {
                moduleModel.destroy();
            }
        } catch (Exception e) {
            logger.error(
                    CONFIG_STOP_DUBBO_ERROR,
                    "",
                    "",
                    "Unexpected error occurred when stop dubbo module: " + e.getMessage(),
                    e);
        }
        // remove context bind cache
        DubboSpringInitializer.remove(event.getApplicationContext());
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
