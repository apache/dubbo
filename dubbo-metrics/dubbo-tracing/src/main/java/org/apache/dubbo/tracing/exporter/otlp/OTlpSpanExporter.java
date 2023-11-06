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
package org.apache.dubbo.tracing.exporter.otlp;

import org.apache.dubbo.config.nested.ExporterConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * OTlp span exporter for OTel.
 */
public class OTlpSpanExporter {

    public static SpanExporter getSpanExporter(
            ApplicationModel applicationModel, ExporterConfig.OtlpConfig otlpConfig) {
        OtlpGrpcSpanExporter externalOTlpGrpcSpanExporter =
                applicationModel.getBeanFactory().getBean(OtlpGrpcSpanExporter.class);
        if (externalOTlpGrpcSpanExporter != null) {
            return externalOTlpGrpcSpanExporter;
        }
        OtlpHttpSpanExporter externalOtlpHttpSpanExporter =
                applicationModel.getBeanFactory().getBean(OtlpHttpSpanExporter.class);
        if (externalOtlpHttpSpanExporter != null) {
            return externalOtlpHttpSpanExporter;
        }
        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpConfig.getEndpoint())
                .setTimeout(otlpConfig.getTimeout())
                .setCompression(otlpConfig.getCompressionMethod());
        for (Map.Entry<String, String> entry : otlpConfig.getHeaders().entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
