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

package org.apache.dubbo.metrics.prometheus;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metrics.report.AbstractMetricsReporterFactory;
import org.apache.dubbo.metrics.report.MetricsReporter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

/**
 * MetricsReporterFactory to create PrometheusMetricsReporter.
 */
public class PrometheusMetricsReporterFactory extends AbstractMetricsReporterFactory {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(PrometheusMetricsReporterFactory.class);

    public PrometheusMetricsReporterFactory(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    @Override
    public MetricsReporter createMetricsReporter(URL url) {
        try {
            return new PrometheusMetricsReporter(url, getApplicationModel());
        } catch (NoClassDefFoundError ncde) {
            String msg = ncde.getMessage();
            if (dependenciesNotFound(msg)) {
                logger.error(INTERNAL_ERROR, "", "", "Failed to load class \"org.apache.dubbo.metrics.prometheus.PrometheusMetricsReporter\".", ncde);
                logger.error(INTERNAL_ERROR, "", "", "Defaulting to no-operation (NOP) metricsReporter implementation", ncde);
                logger.error(INTERNAL_ERROR, "", "", "Introduce the micrometer-core package to use the ability of metrics", ncde);
                return new NopPrometheusMetricsReporter();
            } else {
                logger.error(INTERNAL_ERROR, "", "", "Failed to instantiate PrometheusMetricsReporter", ncde);
                throw ncde;
            }
        }
    }

    private static boolean dependenciesNotFound(String msg) {
        if (msg == null) {
            return false;
        }
        if (msg.contains("io/micrometer/core/instrument/composite/CompositeMeterRegistry")) {
            return true;
        }
        return msg.contains("io.micrometer.core.instrument.composite.CompositeMeterRegistry");
    }
}
