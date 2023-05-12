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

package org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.otlp;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import org.apache.dubbo.config.nested.ExporterConfig.OtlpConfig;
import org.apache.dubbo.spring.boot.autoconfigure.DubboConfigurationProperties;
import org.apache.dubbo.spring.boot.observability.autoconfigure.annotation.ConditionalOnDubboTracingEnable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import static org.apache.dubbo.spring.boot.observability.autoconfigure.ObservabilityUtils.DUBBO_TRACING_OTLP_CONFIG_PREFIX;
import static org.apache.dubbo.spring.boot.observability.autoconfigure.ObservabilityUtils.DUBBO_TRACING_PREFIX;

@AutoConfiguration
@ConditionalOnProperty(prefix = DUBBO_TRACING_PREFIX, name = "enabled", havingValue = "true")
@ConditionalOnDubboTracingEnable
@EnableConfigurationProperties(DubboConfigurationProperties.class)
public class OtlpAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = DUBBO_TRACING_OTLP_CONFIG_PREFIX, name = "endpoint")
    @ConditionalOnClass(OtlpGrpcSpanExporter.class)
    OtlpGrpcSpanExporter otlpGrpcSpanExporter(DubboConfigurationProperties properties) {
        OtlpConfig cfg = properties.getTracing().getTracingExporter().getOtlpConfig();
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(cfg.getEndpoint())
                .setTimeout(cfg.getTimeout())
                .setCompression(cfg.getCompressionMethod())
                .build();
    }
}
