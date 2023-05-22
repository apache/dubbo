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
package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.builders.InternalServiceConfigBuilder;
import org.apache.dubbo.metrics.service.MetricsService;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;
import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

/**
 * Export metrics service
 */
public class DefaultMetricsServiceExporter implements MetricsServiceExporter, ScopeModelAware {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private          ApplicationModel              applicationModel;
    private          MetricsService                metricsService;
    private volatile ServiceConfig<MetricsService> serviceConfig;

    @Override
    public void init() {
        initialize();
    }

    private void initialize() {
        MetricsConfig metricsConfig = applicationModel.getApplicationConfigManager().getMetrics().orElse(null);
        // TODO compatible with old usage of metrics, remove protocol check after new metrics is ready for use.
        if (metricsConfig != null &&  metricsService == null) {
            if (PROTOCOL_PROMETHEUS.equals(metricsConfig.getProtocol()) ) {
                this.metricsService  = applicationModel.getExtensionLoader(MetricsService.class).getDefaultExtension();
            } else {
                logger.warn(COMMON_METRICS_COLLECTOR_EXCEPTION, "", "", "Protocol " + metricsConfig.getProtocol() + " not support for new metrics mechanism. " +
                    "Using old metrics mechanism instead.");
            }
        }
    }

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public MetricsServiceExporter export() {
        if (metricsService != null) {
            if (!isExported()) {
                ExecutorService internalServiceExecutor = applicationModel.getFrameworkModel().getBeanFactory()
                    .getBean(FrameworkExecutorRepository.class).getInternalServiceExecutor();
                ServiceConfig<MetricsService> serviceConfig = InternalServiceConfigBuilder.<MetricsService>newBuilder(applicationModel)
                    .interfaceClass(MetricsService.class)
                    .protocol(getMetricsConfig().getExportServiceProtocol())
                    .port(getMetricsConfig().getExportServicePort())
                    .executor(internalServiceExecutor)
                    .ref(metricsService)
                    .registryId("internal-metrics-registry")
                    .build();

                // export
                serviceConfig.export();

                if (logger.isInfoEnabled()) {
                    logger.info("The MetricsService exports urls : " + serviceConfig.getExportedUrls());
                }
                this.serviceConfig = serviceConfig;
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn(LoggerCodeConstants.INTERNAL_ERROR, "", "", "The MetricsService has been exported : " + serviceConfig.getExportedUrls());
                }
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("The MetricsConfig not exist, will not export metrics service.");
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

    private MetricsConfig getMetricsConfig() {
        Optional<MetricsConfig> metricsConfig = applicationModel.getApplicationConfigManager().getMetrics();
        if (metricsConfig.isPresent()) {
            return metricsConfig.get();
        } else {
            throw new IllegalStateException("There's no MetricsConfig specified.");
        }
    }

    private boolean isExported() {
        return serviceConfig != null && serviceConfig.isExported() && !serviceConfig.isUnexported();
    }

}
