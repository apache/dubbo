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

import org.apache.dubbo.common.URL;
<<<<<<< HEAD
import org.apache.dubbo.common.logger.Logger;
=======
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
>>>>>>> origin/3.2
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.builders.InternalServiceConfigBuilder;
import org.apache.dubbo.metadata.MetadataService;
<<<<<<< HEAD
import org.apache.dubbo.metadata.MetadataServiceExporter;
=======
import org.apache.dubbo.registry.client.metadata.MetadataServiceDelegation;
>>>>>>> origin/3.2
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
<<<<<<< HEAD
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
=======
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_METADATA_SERVICE_EXPORTED;
>>>>>>> origin/3.2

/**
 * Export metadata service
 */
<<<<<<< HEAD
public class ConfigurableMetadataServiceExporter implements MetadataServiceExporter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MetadataService metadataService;

    private volatile ServiceConfig<MetadataService> serviceConfig;

    public ConfigurableMetadataServiceExporter(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Override
    public ConfigurableMetadataServiceExporter export() {

        if (!isExported()) {

            ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setApplication(getApplicationConfig());
            serviceConfig.setRegistries(getRegistries());
            serviceConfig.setProtocol(generateMetadataProtocol());
            serviceConfig.setInterface(MetadataService.class);
            serviceConfig.setRef(metadataService);
            serviceConfig.setGroup(getApplicationConfig().getName());
            serviceConfig.setVersion(metadataService.version());
=======
public class ConfigurableMetadataServiceExporter {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private MetadataServiceDelegation metadataService;

    private volatile ServiceConfig<MetadataService> serviceConfig;
    private final ApplicationModel applicationModel;

    public ConfigurableMetadataServiceExporter(ApplicationModel applicationModel, MetadataServiceDelegation metadataService) {
        this.applicationModel = applicationModel;
        this.metadataService = metadataService;
    }

    public synchronized ConfigurableMetadataServiceExporter export() {
        if (serviceConfig == null || !isExported()) {
            this.serviceConfig = InternalServiceConfigBuilder.<MetadataService>newBuilder(applicationModel)
                .interfaceClass(MetadataService.class)
                .protocol(getApplicationConfig().getMetadataServiceProtocol(), METADATA_SERVICE_PROTOCOL_KEY)
                .port(getApplicationConfig().getMetadataServicePort(), METADATA_SERVICE_PORT_KEY)
                .registryId("internal-metadata-registry")
                .ref(metadataService)
                .build(configConsumer -> configConsumer.setMethods(generateMethodConfig()));
>>>>>>> origin/3.2

            // export
            serviceConfig.export();

<<<<<<< HEAD
            if (logger.isInfoEnabled()) {
                logger.info("The MetadataService exports urls : " + serviceConfig.getExportedUrls());
            }

            this.serviceConfig = serviceConfig;

        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("The MetadataService has been exported : " + serviceConfig.getExportedUrls());
=======
            metadataService.setMetadataURL(serviceConfig.getExportedUrls().get(0));
            if (logger.isInfoEnabled()) {
                logger.info("The MetadataService exports urls : " + serviceConfig.getExportedUrls());
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn(CONFIG_METADATA_SERVICE_EXPORTED, "", "", "The MetadataService has been exported : " + serviceConfig.getExportedUrls());
>>>>>>> origin/3.2
            }
        }

        return this;
    }

<<<<<<< HEAD
    @Override
=======
>>>>>>> origin/3.2
    public ConfigurableMetadataServiceExporter unexport() {
        if (isExported()) {
            serviceConfig.unexport();
            metadataService.setMetadataURL(null);
        }
        return this;
<<<<<<< HEAD
    }

    @Override
    public List<URL> getExportedURLs() {
        return serviceConfig != null ? serviceConfig.getExportedUrls() : emptyList();
=======
>>>>>>> origin/3.2
    }

    public boolean isExported() {
        return serviceConfig != null && serviceConfig.isExported() && !serviceConfig.isUnexported();
    }

    private ApplicationConfig getApplicationConfig() {
<<<<<<< HEAD
        return ApplicationModel.getConfigManager().getApplication().get();
=======
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
>>>>>>> origin/3.2
    }

    // for unit test
    public void setMetadataService(MetadataServiceDelegation metadataService) {
        this.metadataService = metadataService;
    }

    // for unit test
    public List<URL> getExportedURLs() {
        return serviceConfig != null ? serviceConfig.getExportedUrls() : emptyList();
    }
}
