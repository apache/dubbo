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
package org.apache.dubbo.config.spring.schema.config;

import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.SslConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * {@link GenericDubboConfigBeanDefinitionParser} Test cases
 *
 * @see GenericDubboConfigBeanDefinitionParser
 * @see ModuleConfig
 * @see MetricsConfig
 * @see SslConfig
 * @since 2.7.7
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GenericDubboConfigBeanDefinitionParserTest.class)
@TestPropertySource(properties = {
        "a=1",
        "b=2"
})
@ImportResource(locations = "classpath:/META-INF/spring/generic-dubbo-config-bean-definition-parser-test.xml")
public class GenericDubboConfigBeanDefinitionParserTest {

    @Bean
    public static RegistryConfig mockRegistry() {
        return new RegistryConfig();
    }

    @Bean
    public static MonitorConfig mockMonitor() {
        return new MonitorConfig();
    }

    @Autowired
    private Map<String, ModuleConfig> moduleConfigsMap;

    @Autowired
    @Qualifier("dubbo-metrics-test")
    private MetricsConfig metricsConfig;

    @Autowired
    @Qualifier("dubbo-ssl-test")
    private SslConfig sslConfig;

    @Autowired
    private RegistryConfig mockRegistry;

    @Autowired
    private MonitorConfig mockMonitor;

    @Test
    public void test() {
        testModuleConfig();
        testMetricsConfig();
        testSslConfig();
    }

    public void testModuleConfig() {
        ModuleConfig moduleConfig = moduleConfigsMap.get("dubbo-module-test");
        assertEquals("dubbo-module-test", moduleConfig.getName());
        assertEquals("1.2", moduleConfig.getVersion());
        assertEquals("mercyblitz", moduleConfig.getOwner());
        assertEquals("Apache", moduleConfig.getOrganization());
        assertEquals(mockRegistry, moduleConfig.getRegistry());
        assertEquals(mockMonitor, moduleConfig.getMonitor());
        assertEquals(Boolean.TRUE, moduleConfig.isDefault());
    }

    private void testMetricsConfig() {
        assertEquals("dubbo-metrics-test", metricsConfig.getId());
        assertEquals("http", metricsConfig.getProtocol());
        assertEquals("9090", metricsConfig.getPort());
    }

    private void testSslConfig() {
        assertEquals("dubbo-ssl-test", sslConfig.getId());
        assertEquals("server-chain-path", sslConfig.getServerKeyCertChainPath());
        assertEquals("server-key-path", sslConfig.getServerPrivateKeyPath());
        assertEquals("123456", sslConfig.getServerKeyPassword());
        assertEquals("server-collection-path", sslConfig.getServerTrustCertCollectionPath());
        assertEquals("client-chain-path", sslConfig.getClientKeyCertChainPath());
        assertEquals("client-key-path", sslConfig.getClientPrivateKeyPath());
        assertEquals("654321", sslConfig.getClientKeyPassword());
        assertEquals("client-collection-path", sslConfig.getClientTrustCertCollectionPath());

    }
}
