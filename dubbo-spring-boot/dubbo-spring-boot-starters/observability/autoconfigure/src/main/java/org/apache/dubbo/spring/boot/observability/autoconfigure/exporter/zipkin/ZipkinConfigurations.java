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

import org.apache.dubbo.config.nested.ExporterConfig;
import org.apache.dubbo.spring.boot.autoconfigure.DubboConfigurationProperties;
import org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.zipkin.customizer.ZipkinRestTemplateBuilderCustomizer;
import org.apache.dubbo.spring.boot.observability.autoconfigure.exporter.zipkin.customizer.ZipkinWebClientBuilderCustomizer;

import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import static org.apache.dubbo.spring.boot.observability.autoconfigure.ObservabilityUtils.DUBBO_TRACING_ZIPKIN_CONFIG_PREFIX;

/**
 * Configurations for Zipkin. Those are imported by {@link ZipkinAutoConfiguration}.
 */
class ZipkinConfigurations {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = DUBBO_TRACING_ZIPKIN_CONFIG_PREFIX, name = "endpoint")
    @Import({
        UrlConnectionSenderConfiguration.class,
        WebClientSenderConfiguration.class,
        RestTemplateSenderConfiguration.class
    })
    static class SenderConfiguration {}

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(URLConnectionSender.class)
    @EnableConfigurationProperties(DubboConfigurationProperties.class)
    static class UrlConnectionSenderConfiguration {

        @Bean
        @ConditionalOnMissingBean(Sender.class)
        URLConnectionSender urlConnectionSender(DubboConfigurationProperties properties) {
            URLConnectionSender.Builder builder = URLConnectionSender.newBuilder();
            ExporterConfig.ZipkinConfig zipkinConfig =
                    properties.getTracing().getTracingExporter().getZipkinConfig();
            builder.connectTimeout((int) zipkinConfig.getConnectTimeout().toMillis());
            builder.readTimeout((int) zipkinConfig.getReadTimeout().toMillis());
            builder.endpoint(zipkinConfig.getEndpoint());
            return builder.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestTemplate.class)
    @EnableConfigurationProperties(DubboConfigurationProperties.class)
    static class RestTemplateSenderConfiguration {

        @Bean
        @ConditionalOnMissingBean(Sender.class)
        ZipkinRestTemplateSender restTemplateSender(
                DubboConfigurationProperties properties,
                ObjectProvider<ZipkinRestTemplateBuilderCustomizer> customizers) {
            ExporterConfig.ZipkinConfig zipkinConfig =
                    properties.getTracing().getTracingExporter().getZipkinConfig();
            RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder()
                    .setConnectTimeout(zipkinConfig.getConnectTimeout())
                    .setReadTimeout(zipkinConfig.getReadTimeout());
            restTemplateBuilder = applyCustomizers(restTemplateBuilder, customizers);
            return new ZipkinRestTemplateSender(zipkinConfig.getEndpoint(), restTemplateBuilder.build());
        }

        private RestTemplateBuilder applyCustomizers(
                RestTemplateBuilder restTemplateBuilder,
                ObjectProvider<ZipkinRestTemplateBuilderCustomizer> customizers) {
            Iterable<ZipkinRestTemplateBuilderCustomizer> orderedCustomizers =
                    () -> customizers.orderedStream().iterator();
            RestTemplateBuilder currentBuilder = restTemplateBuilder;
            for (ZipkinRestTemplateBuilderCustomizer customizer : orderedCustomizers) {
                currentBuilder = customizer.customize(currentBuilder);
            }
            return currentBuilder;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebClient.class)
    @EnableConfigurationProperties(DubboConfigurationProperties.class)
    static class WebClientSenderConfiguration {

        @Bean
        @ConditionalOnMissingBean(Sender.class)
        ZipkinWebClientSender webClientSender(
                DubboConfigurationProperties properties, ObjectProvider<ZipkinWebClientBuilderCustomizer> customizers) {
            ExporterConfig.ZipkinConfig zipkinConfig =
                    properties.getTracing().getTracingExporter().getZipkinConfig();
            WebClient.Builder builder = WebClient.builder();
            customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
            return new ZipkinWebClientSender(zipkinConfig.getEndpoint(), builder.build());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ReporterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(Sender.class)
        AsyncReporter<Span> spanReporter(Sender sender, BytesEncoder<Span> encoder) {
            return AsyncReporter.builder(sender).build((zipkin2.reporter.BytesEncoder<Span>) encoder);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ZipkinSpanHandler.class)
    static class BraveConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(Reporter.class)
        ZipkinSpanHandler zipkinSpanHandler(Reporter<Span> spanReporter) {
            return (ZipkinSpanHandler)
                    ZipkinSpanHandler.newBuilder(spanReporter).build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ZipkinSpanExporter.class)
    @ConditionalOnProperty(prefix = DUBBO_TRACING_ZIPKIN_CONFIG_PREFIX, name = "endpoint")
    @EnableConfigurationProperties(DubboConfigurationProperties.class)
    static class OpenTelemetryConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ZipkinSpanExporter zipkinSpanExporter(
                DubboConfigurationProperties properties, BytesEncoder<Span> encoder, ObjectProvider<Sender> senders) {
            AtomicReference<Sender> senderRef = new AtomicReference<>();
            senders.orderedStream().findFirst().ifPresent(senderRef::set);
            Sender sender = senderRef.get();
            if (sender == null) {
                ExporterConfig.ZipkinConfig zipkinConfig =
                        properties.getTracing().getTracingExporter().getZipkinConfig();
                return ZipkinSpanExporter.builder()
                        .setEncoder(encoder)
                        .setEndpoint(zipkinConfig.getEndpoint())
                        .setReadTimeout(zipkinConfig.getReadTimeout())
                        .build();
            }
            return ZipkinSpanExporter.builder()
                    .setEncoder(encoder)
                    .setSender(sender)
                    .build();
        }
    }
}
