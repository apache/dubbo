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

import java.time.Duration;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OTlpSpanExporterTest {

    @Test
    void getSpanExporter() {
        ExporterConfig.OtlpConfig otlpConfig = mock(ExporterConfig.OtlpConfig.class);
        when(otlpConfig.getEndpoint()).thenReturn("http://localhost:9411/api/v2/spans");
        when(otlpConfig.getTimeout()).thenReturn(Duration.ofSeconds(5));
        when(otlpConfig.getCompressionMethod()).thenReturn("gzip");

        SpanExporter spanExporter = OTlpSpanExporter.getSpanExporter(ApplicationModel.defaultModel(), otlpConfig);
        Assertions.assertNotNull(spanExporter);
    }
}
