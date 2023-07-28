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

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;

import java.util.concurrent.atomic.AtomicBoolean;

@Activate(order = -1)
public class MetrisExportApplicationLifecycle implements ApplicationLifecycle {

    private DefaultApplicationDeployer applicationDeployer;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetrisExportApplicationLifecycle.class);

    @Override
    public void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer) {
         this.applicationDeployer= defaultApplicationDeployer;
    }

    /**
     * If this lifecycle need to initialize.
     */
    @Override
    public boolean needInitialize() {
        return true;
    }

    /**
     * {@link ApplicationDeployer#start()}
     *
     * @param hasPreparedApplicationInstance
     */
    @Override
    public void start(AtomicBoolean hasPreparedApplicationInstance) {
        if(!hasPreparedApplicationInstance.get()) {
            exportMetricsService();
        }
    }

    private void exportMetricsService() {

        boolean exportMetrics = applicationDeployer.getApplicationModel().getApplicationConfigManager().getMetrics()
            .map(MetricsConfig::getExportMetricsService).orElse(true);

        if (exportMetrics) {
            try {

                MetricsApplicationLifecycle metricsApplicationLifecycle = applicationDeployer.getApplicationModel().getBeanFactory().getBean(MetricsApplicationLifecycle.class);
                MetricsServiceExporter metricsServiceExporter = null;

                if(metricsApplicationLifecycle != null){
                    metricsServiceExporter = metricsApplicationLifecycle.getMetricsServiceExporter();
                }
                if(metricsApplicationLifecycle != null) {
                    metricsServiceExporter.export();
                }
            } catch (Exception e) {
                logger.error(LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION, "", "",
                    "exportMetricsService an exception occurred when handle starting event", e);
            }
        }
    }
}
