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
package org.apache.dubbo.tracing.exporter.zipkin;

import org.apache.dubbo.config.nested.ExporterConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import zipkin2.Span;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.SpanBytesEncoder;

/**
 * Zipkin span exporter for OTel.
 */
public class ZipkinSpanExporter {

    public static io.opentelemetry.sdk.trace.export.SpanExporter getSpanExporter(
            ApplicationModel applicationModel, ExporterConfig.ZipkinConfig zipkinConfig) {
        BytesEncoder<Span> spanBytesEncoder = getSpanBytesEncoder(applicationModel);
        return io.opentelemetry.exporter.zipkin.ZipkinSpanExporter.builder()
                .setEncoder(spanBytesEncoder)
                .setEndpoint(zipkinConfig.getEndpoint())
                .setReadTimeout(zipkinConfig.getReadTimeout())
                .build();
    }

    private static BytesEncoder<Span> getSpanBytesEncoder(ApplicationModel applicationModel) {
        BytesEncoder<zipkin2.Span> encoder = applicationModel.getBeanFactory().getBean(BytesEncoder.class);
        return encoder == null ? SpanBytesEncoder.JSON_V2 : encoder;
    }
}
