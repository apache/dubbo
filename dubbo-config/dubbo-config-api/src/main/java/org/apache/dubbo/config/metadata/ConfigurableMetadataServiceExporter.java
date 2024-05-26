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
package org.apache.dubbo.config.metadata;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.builders.InternalServiceConfigBuilder;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceV2;
import org.apache.dubbo.metadata.util.MetadataServiceVersionUtils;
import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegation;
import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegationV2;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TRIPLE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_METADATA_SERVICE_EXPORTED;
import static org.apache.dubbo.metadata.util.MetadataServiceVersionUtils.V1;
import static org.apache.dubbo.metadata.util.MetadataServiceVersionUtils.V2;

/**
 * Export metadata service
 */
public class ConfigurableMetadataServiceExporter {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    @Deprecated
    private final MetadataServiceDelegation metadataService;

    private final MetadataServiceDelegationV2 metadataServiceV2;

    @Deprecated
    private volatile ServiceConfig<MetadataService> serviceConfig;

    private volatile ServiceConfig<MetadataServiceV2> serviceConfigV2;

    private final ApplicationModel applicationModel;

    public ConfigurableMetadataServiceExporter(
            ApplicationModel applicationModel,
            MetadataServiceDelegation metadataService,
            MetadataServiceDelegationV2 metadataServiceV2) {
        this.applicationModel = applicationModel;
        this.metadataService = metadataService;
        this.metadataServiceV2 = metadataServiceV2;
    }

    public synchronized ConfigurableMetadataServiceExporter export() {
        if (serviceConfig == null || !isExported()) {
            if (MetadataServiceVersionUtils.needExportV1(applicationModel)) {
                exportV1();
            }
            if (MetadataServiceVersionUtils.needExportV2(applicationModel)) {
                exportV2();
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn(
                        CONFIG_METADATA_SERVICE_EXPORTED,
                        "",
                        "",
                        "The MetadataService has been exported : " + serviceConfig.getExportedUrls());
            }
        }

        return this;
    }

    private static final String INTERNAL_METADATA_REGISTRY_ID = "internal-metadata-registry";

    private void exportV1() {
        ExecutorService internalServiceExecutor = applicationModel
                .getFrameworkModel()
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getInternalServiceExecutor();
        this.serviceConfig = InternalServiceConfigBuilder.<MetadataService>newBuilder(applicationModel)
                .interfaceClass(MetadataService.class)
                .protocol(getApplicationConfig().getMetadataServiceProtocol(), METADATA_SERVICE_PROTOCOL_KEY)
                .port(getApplicationConfig().getMetadataServicePort(), METADATA_SERVICE_PORT_KEY)
                .registryId(INTERNAL_METADATA_REGISTRY_ID)
                .executor(internalServiceExecutor)
                .ref(metadataService)
                .version(V1)
                .build(configConsumer -> configConsumer.setMethods(generateMethodConfig()));

        serviceConfig.export();
        metadataService.setMetadataURL(serviceConfig.getExportedUrls().get(0));

        if (logger.isInfoEnabled()) {
            logger.info("The MetadataService exports urls : " + serviceConfig.getExportedUrls());
        }
    }

    private void exportV2() {
        ExecutorService internalServiceExecutor = applicationModel
                .getFrameworkModel()
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getInternalServiceExecutor();
        this.serviceConfigV2 = InternalServiceConfigBuilder.<MetadataServiceV2>newBuilder(applicationModel)
                .interfaceClass(MetadataServiceV2.class)
                .protocol(TRIPLE, METADATA_SERVICE_PROTOCOL_KEY)
                .port(getApplicationConfig().getMetadataServicePort(), METADATA_SERVICE_PORT_KEY)
                .registryId(INTERNAL_METADATA_REGISTRY_ID)
                .executor(internalServiceExecutor)
                .ref(metadataServiceV2)
                .version(V2)
                .build();

        serviceConfigV2.export();
        metadataServiceV2.setMetadataUrl(serviceConfigV2.getExportedUrls().get(0));

        if (logger.isInfoEnabled()) {
            logger.info("The MetadataServiceV2 exports urls : " + serviceConfigV2.getExportedUrls());
        }
    }

    public ConfigurableMetadataServiceExporter unexport() {
        if (isExported()) {
            serviceConfig.unexport();
            serviceConfigV2.unexport();
            metadataService.setMetadataURL(null);
        }
        return this;
    }

    private boolean v1Exported() {
        return serviceConfig != null && serviceConfig.isExported() && !serviceConfig.isUnexported();
    }

    private boolean v2Exported() {
        return serviceConfigV2 != null && serviceConfigV2.isExported() && !serviceConfigV2.isUnexported();
    }

    public boolean isExported() {
        return v1Exported() || v2Exported();
    }

    private ApplicationConfig getApplicationConfig() {
        return applicationModel.getApplicationConfigManager().getApplication().get();
    }

    /**
     * Generate Method Config for Service Discovery Metadata <p/>
     * <p>
     * Make {@link MetadataService} support argument callback,
     * used to notify {@link org.apache.dubbo.registry.client.ServiceInstance}'s
     * metadata change event
     *
     * @since 3.0
     */
    private List<MethodConfig> generateMethodConfig() {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("getAndListenInstanceMetadata");

        ArgumentConfig argumentConfig = new ArgumentConfig();
        argumentConfig.setIndex(1);
        argumentConfig.setCallback(true);

        methodConfig.setArguments(Collections.singletonList(argumentConfig));

        return Collections.singletonList(methodConfig);
    }
}
