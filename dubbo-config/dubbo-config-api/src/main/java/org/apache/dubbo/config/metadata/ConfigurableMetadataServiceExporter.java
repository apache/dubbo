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
import org.apache.dubbo.common.utils.CollectionUtils;
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
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class ConfigurableMetadataServiceExporter implements MetadataServiceExporter, ScopeModelAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private MetadataService metadataService;

    private volatile ServiceConfig<MetadataService> serviceConfig;
    private ApplicationModel applicationModel;
    private AtomicBoolean exported = new AtomicBoolean(false);

    public ConfigurableMetadataServiceExporter() {
    }

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    public void setMetadataService(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Override
    public ConfigurableMetadataServiceExporter export() {

        if (exported.compareAndSet(false, true)) {

            ApplicationConfig applicationConfig = getApplicationConfig();
            ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setScopeModel(applicationModel.getInternalModule());
            serviceConfig.setApplication(applicationConfig);
            serviceConfig.setRegistry(new RegistryConfig("N/A"));
            serviceConfig.setProtocol(generateMetadataProtocol());
            serviceConfig.setInterface(MetadataService.class);
            serviceConfig.setDelay(0);
            serviceConfig.setRef(metadataService);
            serviceConfig.setGroup(applicationConfig.getName());
            serviceConfig.setVersion(metadataService.version());
            serviceConfig.setMethods(generateMethodConfig());

            // add to internal module, do export later
            applicationModel.getInternalModule().getConfigManager().addService(serviceConfig);

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
        methodConfig.setName("getAndListenInstanceMetadata");

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
        exported.set(false);
        return this;
    }

    @Override
    public List<URL> getExportedURLs() {
        return serviceConfig != null ? serviceConfig.getExportedUrls() : emptyList();
    }

    public boolean isExported() {
        return exported.get();
    }

    private ApplicationConfig getApplicationConfig() {
        return applicationModel.getApplicationConfigManager().getApplication().get();
    }

    private ProtocolConfig generateMetadataProtocol() {
        ProtocolConfig defaultProtocol = new ProtocolConfig();
        Integer port = getApplicationConfig().getMetadataServicePort();

        if (port == null || port < -1) {
            if (logger.isInfoEnabled()) {
                logger.info("Metadata Service Port hasn't been set will use default protocol defined in protocols.");
            }
            List<ProtocolConfig> defaultProtocols = applicationModel.getApplicationConfigManager().getDefaultProtocols();

            ProtocolConfig dubboProtocol = findDubboProtocol(defaultProtocols);
            if (dubboProtocol != null) {
                logger.info("Using dubbo protocol " + dubboProtocol + " to export MetadataService.");
                return dubboProtocol;
            } else {
                defaultProtocol.setName(DUBBO_PROTOCOL);
                defaultProtocol.setPort(-1);
            }

        } else {
            defaultProtocol.setName(DUBBO_PROTOCOL);
            defaultProtocol.setPort(port);
        }

        logger.info("Using dubbo protocol " + defaultProtocol + " to export MetadataService.");

        return defaultProtocol;
    }

    private ProtocolConfig findDubboProtocol(List<ProtocolConfig> protocolConfigs) {
        if (CollectionUtils.isEmpty(protocolConfigs)) {
            return null;
        }

        for (ProtocolConfig protocolConfig : protocolConfigs) {
            if (DUBBO_PROTOCOL.equalsIgnoreCase(protocolConfig.getName())) {
                return protocolConfig;
            }
        }

        return null;
    }
}
