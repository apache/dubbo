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

import org.apache.dubbo.bootstrap.DubboBootstrap;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.rpc.Exporter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;

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
 * @since 2.7.4
 */
public class ConfigurableMetadataServiceExporter implements MetadataServiceExporter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConfigManager configManager;

    private final MetadataService metadataService;

    private volatile ServiceConfig<MetadataService> serviceConfig;

    private volatile boolean exported;

    private volatile List<Exporter<?>> exporters;

    public ConfigurableMetadataServiceExporter(MetadataService metadataService) {
        this.configManager = ConfigManager.getInstance();
        this.metadataService = metadataService;
    }

    @Override
    public ConfigurableMetadataServiceExporter export() {

        if (!isExported()) {

            ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setApplication(getApplicationConfig());
            serviceConfig.setRegistries(getRegistries());
            serviceConfig.setProtocols(getProtocols());
            serviceConfig.setInterface(MetadataService.class);
            serviceConfig.setRef(metadataService);
            serviceConfig.setGroup(getApplicationConfig().getName());
            serviceConfig.setVersion(metadataService.version());

            // export
            exporters = DubboBootstrap.Helper.exportSync(serviceConfig);
            exported = true;

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

    @Override
    public ConfigurableMetadataServiceExporter unexport() {
        if (isExported()) {
            exporters.get(0).unexport();
            exported = false;
        }
        return this;
    }

    @Override
    public List<URL> getExportedURLs() {
        return serviceConfig != null ? serviceConfig.getExportedUrls() : emptyList();
    }

    public boolean isExported() {
        return serviceConfig != null && exported;
    }

    private ApplicationConfig getApplicationConfig() {
        return configManager.getApplication().get();
    }

    private List<RegistryConfig> getRegistries() {
        return new ArrayList<>(configManager.getRegistries());
    }

    private List<ProtocolConfig> getProtocols() {
        return asList(getDefaultProtocol());
    }

    private ProtocolConfig getDefaultProtocol() {
        ProtocolConfig defaultProtocol = new ProtocolConfig();
        defaultProtocol.setName(DUBBO);
        // auto-increment port
        defaultProtocol.setPort(-1);
        return defaultProtocol;
    }
}
