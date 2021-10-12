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

package org.apache.dubbo.config.metrics;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.metrics.MetricsService;
import org.apache.dubbo.common.metrics.MetricsServiceExporter;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;

/**
 * Default implementation of {@link MetricsServiceExporter}
 */
public class DefaultMetricsServiceExporter implements MetricsServiceExporter, ScopeModelAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ApplicationModel applicationModel;
    private MetricsService metricsService;
    private volatile ServiceConfig<MetricsService> serviceConfig;

    public DefaultMetricsServiceExporter() {
    }

    @Override
    public void init() {
        initialize();
    }

    private void initialize() {
        MetricsConfig metricsConfig = applicationModel.getApplicationConfigManager().getMetrics().orElse(null);
        if (metricsConfig != null) {
            ExtensionLoader<MetricsService> extensionLoader = applicationModel.getExtensionLoader(MetricsService.class);
            if (!extensionLoader.hasExtension(MetricsService.DEFAULT_EXTENSION_NAME)) {
                throw new IllegalStateException("Metrics config exist, but the dubbo-metrics-api dependency is missing. Please check your project dependencies.");
            } else {
                this.metricsService = extensionLoader.getDefaultExtension();
            }
        }
    }

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public MetricsServiceExporter export() {
        if (!isExported()) {

            ApplicationConfig applicationConfig = getApplicationConfig();
            ServiceConfig<MetricsService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setScopeModel(applicationModel.getInternalModule());
            serviceConfig.setApplication(applicationConfig);
            serviceConfig.setRegistry(new RegistryConfig("N/A"));
            serviceConfig.setProtocol(generateMetricsProtocol());
            serviceConfig.setInterface(MetricsService.class);
            serviceConfig.setDelay(0);
            serviceConfig.setRef(metricsService);
            serviceConfig.setGroup(applicationConfig.getName());
            serviceConfig.setVersion(MetricsService.VERSION);

            // export
            serviceConfig.exportOnly();

            if (logger.isInfoEnabled()) {
                logger.info("The MetricsService exports urls : " + serviceConfig.getExportedUrls());
            }

            this.serviceConfig = serviceConfig;

        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("The MetricsService has been exported : " + serviceConfig.getExportedUrls());
            }
        }

        return this;
    }

    @Override
    public MetricsServiceExporter unexport() {
        if (isExported()) {
            serviceConfig.unexport();
        }
        return this;
    }

    private ApplicationConfig getApplicationConfig() {
        return applicationModel.getApplicationConfigManager().getApplication().get();
    }

    private boolean isExported() {
        return serviceConfig != null && serviceConfig.isExported() && !serviceConfig.isUnexported();
    }

    private ProtocolConfig generateMetricsProtocol() {
        ProtocolConfig defaultProtocol = new ProtocolConfig();

        List<ProtocolConfig> defaultProtocols = applicationModel.getApplicationConfigManager().getDefaultProtocols();

        ProtocolConfig dubboProtocol = findDubboProtocol(defaultProtocols);
        if (dubboProtocol != null) {
            return dubboProtocol;
        } else {
            defaultProtocol.setName(DUBBO_PROTOCOL);
            defaultProtocol.setPort(-1);
        }

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
