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
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Metrics export lifecycle.
 */
@Activate(order = -3000)
public class MetricsExportApplicationLifecycle implements ApplicationLifecycle {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetricsExportApplicationLifecycle.class);

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }

    @Override
    public void preModuleChanged(ApplicationContext applicationContext, ModuleModel changedModule, DeployState moduleState) {
        AtomicBoolean hasPreparedApplicationInstance = applicationContext.getHasPreparedApplicationInstance();

        if (!changedModule.isInternal() && moduleState == DeployState.STARTED && !hasPreparedApplicationInstance.get()) {
            exportMetricsService(applicationContext.getModel());
        }
    }

    private void exportMetricsService(ApplicationModel applicationModel) {
        boolean exportMetrics = applicationModel.getApplicationConfigManager().getMetrics()
            .map(MetricsConfig::getExportMetricsService).orElse(true);
        if (exportMetrics) {
            try {
                MetricsServiceExporter exporter = applicationModel.getBeanFactory().getBean(MetricsServiceExporter.class);
                if(exporter == null){
                    exporter =  applicationModel.getExtensionLoader(MetricsServiceExporter.class).getDefaultExtension();
                    applicationModel.getBeanFactory().registerBean(exporter);
                }
                exporter.export();
            } catch (Exception e) {
                logger.error(LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION, "", "",
                    "exportMetricsService an exception occurred when handle starting event", e);
            }
        }
    }
}
