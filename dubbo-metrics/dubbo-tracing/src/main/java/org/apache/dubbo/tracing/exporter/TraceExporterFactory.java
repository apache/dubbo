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
package org.apache.dubbo.tracing.exporter;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.nested.ExporterConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.exporter.otlp.OTlpExporter;
import org.apache.dubbo.tracing.exporter.zipkin.ZipkinExporter;

import brave.handler.SpanHandler;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.List;

public class TraceExporterFactory {

    private final static ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(TraceExporterFactory.class);

    /**
     * for OTel
     */
    public static List<SpanExporter> getSpanExporters(ApplicationModel applicationModel, ExporterConfig exporterConfig) {
        ExporterConfig.ZipkinConfig zipkinConfig = exporterConfig.getZipkinConfig();
        ExporterConfig.OtlpConfig otlpConfig = exporterConfig.getOtlpConfig();
        List<SpanExporter> res = new ArrayList<>();
        if (zipkinConfig != null && StringUtils.isNotEmpty(zipkinConfig.getEndpoint())) {
            ZipkinExporter zipkinExporter = new ZipkinExporter(applicationModel, zipkinConfig);
            LOGGER.info("Create zipkin span exporter.");
            res.add(zipkinExporter.getSpanExporter());
        }
        if (otlpConfig != null && StringUtils.isNotEmpty(otlpConfig.getEndpoint())) {
            OTlpExporter otlpExporter = new OTlpExporter(applicationModel, otlpConfig);
            LOGGER.info("Create OTlp span exporter.");
            res.add(otlpExporter.getSpanExporter());
        }

        return res;
    }

    /**
     * for Brave
     */
    public static List<SpanHandler> getSpanHandlers(ApplicationModel applicationModel, ExporterConfig exporterConfig) {
        List<SpanHandler> res = new ArrayList<>();
        // TODO brave SpanHandler impl
        return res;
    }
}
