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

package org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.zipkin;

import org.apache.dubbo.spring.boot.observability.autoconfigure.annotation.ConditionalOnDubboTracingEnable;
import org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.zipkin.ZipkinConfigurations.BraveConfiguration;
import org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.zipkin.ZipkinConfigurations.OpenTelemetryConfiguration;
import org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.zipkin.ZipkinConfigurations.ReporterConfiguration;
import org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.zipkin.ZipkinConfigurations.SenderConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.Sender;


/**
 * {@link EnableAutoConfiguration Auto-configuration} for Zipkin.
 * <p>
 * It uses imports on {@link ZipkinConfigurations} to guarantee the correct configuration ordering.
 *
 * @since 3.2.0
 */
@AutoConfiguration(after = RestTemplateAutoConfiguration.class)
@ConditionalOnClass(Sender.class)
@Import({SenderConfiguration.class,
        ReporterConfiguration.class, BraveConfiguration.class,
        OpenTelemetryConfiguration.class})
@ConditionalOnDubboTracingEnable
public class ZipkinAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BytesEncoder<Span> spanBytesEncoder() {
        return SpanBytesEncoder.JSON_V2;
    }

}
