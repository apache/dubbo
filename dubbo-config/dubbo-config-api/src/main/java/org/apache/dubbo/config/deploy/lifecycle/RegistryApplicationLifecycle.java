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
package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPostDestroyEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPostModuleChangeEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPreModuleChangeEvent;
import org.apache.dubbo.metrics.event.MetricsEventBus;
import org.apache.dubbo.metrics.registry.event.RegistryEvent;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_REFRESH_INSTANCE_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_REGISTER_INSTANCE_ERROR;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_METADATA_PUBLISH_DELAY;
import static org.apache.dubbo.metadata.MetadataConstants.METADATA_PUBLISH_DELAY_KEY;

/**
 * Registry lifecycle.
 */
@Activate
public class RegistryApplicationLifecycle implements ApplicationLifecycle {

    private final AtomicInteger instanceRefreshScheduleTimes = new AtomicInteger(0);


    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RegistryApplicationLifecycle.class);

    private ScheduledFuture<?> asyncMetadataFuture;


    @Override
    public boolean needInitialize() {
        return true;
    }

    /**
     * postDestroy.
     */
    @Override
    public void postDestroy(AppPostDestroyEvent appPostDestroyEvent) {
        destroyRegistries(appPostDestroyEvent.getApplicationModel());
    }

    private void destroyRegistries(ApplicationModel applicationModel) {
        RegistryManager.getInstance(applicationModel).destroyAll();
    }

    /**
     * What to do when a module changed.
     */
    @Override
    public void preModuleChanged(AppPreModuleChangeEvent preModuleChangeContext) {

        if (!preModuleChangeContext.getChangedModule().isInternal() && preModuleChangeContext.getModuleState() == DeployState.STARTED &&
            !preModuleChangeContext.getHasPreparedApplicationInstance().get() &&
            isRegisterConsumerInstance(preModuleChangeContext.getApplicationModel()) &&
            preModuleChangeContext.getHasPreparedApplicationInstance().compareAndSet(false,true)
        ) {
            registerServiceInstance(preModuleChangeContext);
        }
    }

    private void registerServiceInstance(AppPreModuleChangeEvent preModuleChangeEvent) {


        ApplicationModel applicationModel = preModuleChangeEvent.getApplicationModel();
        FrameworkExecutorRepository frameworkExecutorRepository = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class);

        try {
            preModuleChangeEvent.registered().set(true);
            MetricsEventBus.post(RegistryEvent.toRegisterEvent(applicationModel),
                () -> {
                    ServiceInstanceMetadataUtils.registerMetadataAndInstance(applicationModel);
                    return null;
                }
            );
        } catch (Exception e) {
            logger.error(CONFIG_REGISTER_INSTANCE_ERROR, "configuration server disconnected", "", "Register instance error.", e);
        }

        if (preModuleChangeEvent.registered().get()) {
            // scheduled task for updating Metadata and ServiceInstance
            asyncMetadataFuture = frameworkExecutorRepository.getSharedScheduledExecutor().scheduleWithFixedDelay(() -> {

                // ignore refresh metadata on stopping
                if (applicationModel.isDestroyed()) {
                    return;
                }

                // refresh for 30 times (default for 30s) when deployer is not started, prevent submit too many revision
                if (instanceRefreshScheduleTimes.incrementAndGet() % 30 != 0 && ! DeployState.STARTED.equals(preModuleChangeEvent.getModuleState())) {
                    return;
                }

                // refresh for 5 times (default for 5s) when services are being updated by other threads, prevent submit too many revision
                // note: should not always wait here
                if (preModuleChangeEvent.getServiceRefreshState().get() != 0 && instanceRefreshScheduleTimes.get() % 5 != 0) {
                    return;
                }

                try {
                    if (!applicationModel.isDestroyed() && preModuleChangeEvent.registered().get()) {
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

    @Override
    public void postModuleChanged(AppPostModuleChangeEvent postModuleChangeContext) {
        if(DeployState.STARTING.equals(postModuleChangeContext.getApplicationOldState()) && DeployState.STARTED.equals(postModuleChangeContext.getApplicationNewState())){
            refreshMetadata(postModuleChangeContext);
        }
    }

    private void refreshMetadata(AppPostModuleChangeEvent postModuleChangeEvent){
        try {
            if (postModuleChangeEvent.registered().get()) {
                ServiceInstanceMetadataUtils.refreshMetadataAndInstance(postModuleChangeEvent.getApplicationModel());
            }
        } catch (Exception e) {
            logger.error(CONFIG_REFRESH_INSTANCE_ERROR, "", "", "Refresh instance and metadata error.", e);
            throw e;
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
