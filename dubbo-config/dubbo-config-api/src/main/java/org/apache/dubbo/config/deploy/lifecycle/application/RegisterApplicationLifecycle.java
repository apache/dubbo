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
package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_REFRESH_INSTANCE_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_REGISTER_INSTANCE_ERROR;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_PUBLISH_DELAY;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;

@Activate(order = -1000)
public class RegisterApplicationLifecycle implements ApplicationLifecycle {

    private ScheduledFuture<?> asyncMetadataFuture;

    private final AtomicInteger instanceRefreshScheduleTimes = new AtomicInteger(0);

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RegisterApplicationLifecycle.class);

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }

    @Override
    public void preModuleChanged(ApplicationContext applicationContext, ModuleModel changedModule, DeployState moduleState) {
        if (!changedModule.isInternal() && moduleState == DeployState.STARTED &&
            !applicationContext.isHasPreparedApplicationInstance() &&
            isRegisterConsumerInstance(applicationContext.getModel()) &&
            applicationContext.getHasPreparedApplicationInstance().compareAndSet(false,true)
        ) {
            registerServiceInstance(applicationContext);
        }
    }


    private void registerServiceInstance(ApplicationContext applicationContext) {
        ApplicationModel applicationModel = applicationContext.getModel();
        FrameworkExecutorRepository frameworkExecutorRepository = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);

        try {
            applicationContext.getRegistered().set(true);
            ServiceInstanceMetadataUtils.registerMetadataAndInstance(applicationModel);
        } catch (Exception e) {
            logger.error(CONFIG_REGISTER_INSTANCE_ERROR, "configuration server disconnected", "", "Register instance error.", e);
        }

        if (applicationContext.registered()) {
            // scheduled task for updating Metadata and ServiceInstance
            asyncMetadataFuture = frameworkExecutorRepository.getSharedScheduledExecutor().scheduleWithFixedDelay(() -> {

                // ignore refresh metadata on stopping
                if (applicationModel.isDestroyed()) {
                    return;
                }

                // refresh for 30 times (default for 30s) when deployer is not started, prevent submit too many revision
                if (instanceRefreshScheduleTimes.incrementAndGet() % 30 != 0 && ! DeployState.STARTED.equals(applicationContext.getCurrentState())) {
                    return;
                }

                // refresh for 5 times (default for 5s) when services are being updated by other threads, prevent submit too many revision
                // note: should not always wait here
                if (applicationContext.getServiceRefreshState().get() != 0 && instanceRefreshScheduleTimes.get() % 5 != 0) {
                    return;
                }

                try {
                    if (!applicationModel.isDestroyed() && applicationContext.registered()) {
                        ServiceInstanceMetadataUtils.refreshMetadataAndInstance(applicationModel);
                    }
                } catch (Exception e) {
                    if (!applicationModel.isDestroyed()) {
                        logger.error(CONFIG_REFRESH_INSTANCE_ERROR, "", "", "Refresh instance and metadata error.", e);
                    }
                }
            }, 0, ConfigurationUtils.get(applicationModel, METADATA_PUBLISH_DELAY_KEY, DEFAULT_METADATA_PUBLISH_DELAY), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Close registration of instance for pure Consumer process by setting registerConsumer to 'false'
     * by default is true.
     */
    public boolean isRegisterConsumerInstance(ApplicationModel applicationModel) {
        Boolean registerConsumer = applicationModel.getApplicationConfigManager().getApplicationOrElseThrow().getRegisterConsumer();
        if (registerConsumer == null) {
            return true;
        }
        return Boolean.TRUE.equals(registerConsumer);
    }


    public ScheduledFuture<?> getAsyncMetadataFuture() {
        return asyncMetadataFuture;
    }
}
