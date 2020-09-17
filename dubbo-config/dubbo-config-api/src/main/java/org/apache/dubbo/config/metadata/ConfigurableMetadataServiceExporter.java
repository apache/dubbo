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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;

/**
 * {@link MetadataServiceExporter} implementation based on {@link ConfigManager Dubbo configurations}, the clients
 * should make sure the {@link ApplicationConfig}, {@link RegistryConfig} and {@link ProtocolConfig} are ready before
 * {@link #export()}.
 * <p>
 * Typically, do not worry about their ready status, because they are initialized before
 * any {@link ServiceConfig} exports, or The Dubbo export will be failed.
 * <p>
 * Being aware of it's not a thread-safe implementation.
 *
 * @see MetadataServiceExporter
 * @see ServiceConfig
 * @see ConfigManager
 * @since 2.7.5
 */
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
            serviceConfig.setMethods(generateMethodConfig());

            // export
            serviceConfig.export();

            if (logger.isInfoEnabled()) {
                logger.info("The MetadataService exports urls : " + serviceConfig.getExportedUrls());
            }

            this.serviceConfig = serviceConfig;

        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("The MetadataService has been exported : " + serviceConfig.getExportedUrls());
            }
        }

        return this;
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
        methodConfig.setName("getAndListenServiceDiscoveryMetadata");

        ArgumentConfig argumentConfig = new ArgumentConfig();
        argumentConfig.setIndex(1);
        argumentConfig.setCallback(true);

        methodConfig.setArguments(Collections.singletonList(argumentConfig));

        return Collections.singletonList(methodConfig);
    }

    @Override
    public ConfigurableMetadataServiceExporter unexport() {
        if (isExported()) {
            serviceConfig.unexport();
        }
        return this;
    }

    @Override
    public List<URL> getExportedURLs() {
        return serviceConfig != null ? serviceConfig.getExportedUrls() : emptyList();
    }

    public boolean isExported() {
        return serviceConfig != null && serviceConfig.isExported();
    }

    private ApplicationConfig getApplicationConfig() {
        return ApplicationModel.getConfigManager().getApplication().get();
    }

    private List<RegistryConfig> getRegistries() {
        return new ArrayList<>(ApplicationModel.getConfigManager().getRegistries());
    }

    private ProtocolConfig generateMetadataProtocol() {
        ProtocolConfig defaultProtocol = new ProtocolConfig();
        Integer port = getApplicationConfig().getMetadataServicePort();

        if (port == null || port < -1) {
            if (logger.isInfoEnabled()) {
                logger.info("Metadata Service Port hasn't been set. " +
                        "Use default protocol defined in protocols.");
            }
            List<ProtocolConfig> defaultProtocols = ApplicationModel.getConfigManager().getDefaultProtocols();

            if (defaultProtocols.isEmpty()) {
                defaultProtocol.setName(DUBBO_PROTOCOL);
                defaultProtocol.setPort(-1);
            } else {
                return defaultProtocols.get(0);
            }

        } else {
            defaultProtocol.setName(DUBBO_PROTOCOL);
            defaultProtocol.setPort(port);
        }

        return defaultProtocol;
    }
}
