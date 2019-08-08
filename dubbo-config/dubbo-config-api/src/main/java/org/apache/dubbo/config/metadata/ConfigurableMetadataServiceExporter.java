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
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.MetadataServiceExporter;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.metadata.store.RemoteWritableMetadataService;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * {@link MetadataServiceExporter} implementation based on {@link AbstractConfig Dubbo configurations}, the clients
 * should make sure the {@link ApplicationConfig}, {@link RegistryConfig} and {@link ProtocolConfig} are ready before
 * {@link #export()}.
 * <p>
 * Typically, do not worry about their ready status, because they are initialized before
 * any {@link ServiceConfig} exports, or The Dubbo export will be failed.
 *
 * @see MetadataServiceExporter
 * @see ServiceConfig
 * @see ConfigManager
 * @since 2.7.4
 */
public class ConfigurableMetadataServiceExporter implements MetadataServiceExporter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile ServiceConfig<MetadataService> serviceConfig;

    private ApplicationConfig applicationConfig;
    private MetadataReportConfig metadataReportConfig;

    private List<RegistryConfig> registries = new LinkedList<>();

    private List<ProtocolConfig> protocols = new LinkedList<>();

    public void setApplicationConfig(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public void setRegistries(Collection<RegistryConfig> registries) {
        this.registries.clear();
        this.registries.addAll(registries);
    }

    public void setProtocols(Collection<ProtocolConfig> protocols) {
        this.protocols.clear();
        this.protocols.addAll(protocols);
    }

    @Override
    public List<URL> export() {

        if (!isExported()) {

            WritableMetadataService metadataService = WritableMetadataService.getDefaultExtension();

            ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setApplication(applicationConfig);
            serviceConfig.setRegistries(registries);
            serviceConfig.setProtocols(protocols);
            serviceConfig.setInterface(MetadataService.class);
            serviceConfig.setRef(metadataService);
            serviceConfig.setGroup(getApplicationConfig().getName());
            serviceConfig.setVersion(metadataService.version());

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
        return serviceConfig.getExportedUrls();
    }

    @Override
    public void unexport() {
        if (isExported()) {
            serviceConfig.unexport();
        }
    }

    private List<ProtocolConfig> getProtocols() {
        return unmodifiableList(protocols);
    }

    private List<RegistryConfig> getRegistries() {
        return unmodifiableList(registries);
    }

    private ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    private boolean isExported() {
        return serviceConfig != null && serviceConfig.isExported();
    }
}
