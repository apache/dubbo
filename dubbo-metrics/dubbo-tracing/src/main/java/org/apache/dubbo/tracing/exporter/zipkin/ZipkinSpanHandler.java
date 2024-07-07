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

import brave.handler.SpanHandler;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.SpanBytesEncoder;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * Zipkin span handler for Brave.
 */
public class ZipkinSpanHandler {

    public static SpanHandler getSpanHandler(
            ApplicationModel applicationModel, ExporterConfig.ZipkinConfig zipkinConfig) {
        URLConnectionSender sender = applicationModel.getBeanFactory().getBean(URLConnectionSender.class);
        if (sender == null) {
            URLConnectionSender.Builder builder = URLConnectionSender.newBuilder();
            builder.connectTimeout((int) zipkinConfig.getConnectTimeout().toMillis());
            builder.readTimeout((int) zipkinConfig.getReadTimeout().toMillis());
            builder.endpoint(zipkinConfig.getEndpoint());

            sender = builder.build();
        }

        BytesEncoder<Span> spanBytesEncoder = getSpanBytesEncoder(applicationModel);
        AsyncReporter<Span> spanReporter = AsyncReporter.builder(sender).build(spanBytesEncoder);
        return zipkin2.reporter.brave.ZipkinSpanHandler.newBuilder(spanReporter).build();
    }

    private static BytesEncoder<zipkin2.Span> getSpanBytesEncoder(ApplicationModel applicationModel) {
        BytesEncoder<zipkin2.Span> encoder = applicationModel.getBeanFactory().getBean(BytesEncoder.class);
        return encoder == null ? SpanBytesEncoder.JSON_V2 : encoder;
    }
}
