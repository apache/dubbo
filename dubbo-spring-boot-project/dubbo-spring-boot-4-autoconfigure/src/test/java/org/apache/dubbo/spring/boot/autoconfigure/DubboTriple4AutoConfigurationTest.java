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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.rpc.protocol.tri.servlet.jakarta.TripleFilter;
import org.apache.dubbo.rpc.protocol.tri.websocket.jakarta.TripleWebSocketFilter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class DubboTriple4AutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DubboTriple4AutoConfiguration.class));

    @Test
    void registersTripleServletFilterWhenEnabled() {
        this.contextRunner
                .withPropertyValues(
                        "dubbo.protocol.triple.servlet.enabled=true",
                        "dubbo.protocol.triple.servlet.filter-url-patterns=/tri/*",
                        "dubbo.protocol.triple.servlet.filter-order=10",
                        "server.port=9090")
                .run((context) -> {
                    assertThat(context).hasBean("tripleProtocolFilter");
                    FilterRegistrationBean<?> registrationBean =
                            context.getBean("tripleProtocolFilter", FilterRegistrationBean.class);
                    assertThat(registrationBean.getFilter()).isInstanceOf(TripleFilter.class);
                    assertThat(registrationBean.getUrlPatterns()).containsExactly("/tri/*");
                    assertThat(registrationBean.getOrder()).isEqualTo(10);
                });
    }

    @Test
    void registersTripleWebSocketFilterWhenEnabled() {
        this.contextRunner
                .withPropertyValues(
                        "dubbo.protocol.triple.websocket.enabled=true",
                        "dubbo.protocol.triple.websocket.filter-url-patterns=/tri-ws/*",
                        "dubbo.protocol.triple.websocket.filter-order=20",
                        "server.port=9091")
                .run((context) -> {
                    assertThat(context).hasBean("tripleWebSocketFilter");
                    FilterRegistrationBean<?> registrationBean =
                            context.getBean("tripleWebSocketFilter", FilterRegistrationBean.class);
                    assertThat(registrationBean.getFilter()).isInstanceOf(TripleWebSocketFilter.class);
                    assertThat(registrationBean.getUrlPatterns()).containsExactly("/tri-ws/*");
                    assertThat(registrationBean.getOrder()).isEqualTo(20);
                });
    }

    @Test
    void doesNotRegisterTripleFiltersByDefault() {
        this.contextRunner.run((context) -> {
            assertThat(context).doesNotHaveBean("tripleProtocolFilter");
            assertThat(context).doesNotHaveBean("tripleWebSocketFilter");
        });
    }

    @Test
    void registersTomcatHttp2CustomizerWhenMaxConcurrentStreamsIsConfigured() {
        this.contextRunner
                .withPropertyValues(
                        "dubbo.protocol.triple.servlet.enabled=true",
                        "dubbo.protocol.triple.servlet.max-concurrent-streams=128")
                .run((context) -> {
                    assertThat(context).hasBean("tripleTomcatHttp2Customizer");
                    assertThat(context.getBean("tripleTomcatHttp2Customizer"))
                            .isInstanceOf(WebServerFactoryCustomizer.class);
                });
    }
}
