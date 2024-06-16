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
package org.apache.dubbo.metrics.listener;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.event.AbstractDubboListener;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.deploy.event.PrepareApplicationInstanceEvent;
import org.apache.dubbo.metrics.service.MetricsServiceExporter;

public class PrepareApplicationInstanceEventListener extends AbstractDubboListener<PrepareApplicationInstanceEvent> {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(PrepareApplicationInstanceEventListener.class);

    @Override
    public void onEvent(PrepareApplicationInstanceEvent event) {
        MetricsServiceExporter metricsServiceExporter =
                event.getApplicationModel().getBeanFactory().getBean(MetricsServiceExporter.class);
        boolean exportMetrics = event.getApplicationModel()
                .getApplicationConfigManager()
                .getMetrics()
                .map(MetricsConfig::getExportMetricsService)
                .orElse(true);
        if (exportMetrics && metricsServiceExporter != null) {
            try {
                metricsServiceExporter.export();
            } catch (Exception e) {
                logger.error(
                        LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION,
                        "",
                        "",
                        "exportMetricsService an" + " exception occurred when handle starting event",
                        e);
            }
        }
    }
}
