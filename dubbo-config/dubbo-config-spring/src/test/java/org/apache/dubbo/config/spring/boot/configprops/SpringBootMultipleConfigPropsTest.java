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
package org.apache.dubbo.config.spring.boot.configprops;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.spring.registrycenter.ZooKeeperServer;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

@SpringBootTest(
        properties = {
                "dubbo.applications.application1.name = dubbo-demo-application",
                "dubbo.modules.demo-module.name = dubbo-demo-module",
                "dubbo.registries.my-registry.address = zookeeper://192.168.99.100:32770",
                "dubbo.protocols.dubbo.port=20880",
                "dubbo.metricses.my-metrics.protocol=dubbo",
                "dubbo.metricses.my-metrics.port=20880",
                "dubbo.monitors.my-monitor.address=zookeeper://127.0.0.1:32770",
                "dubbo.config-centers.my-configcenter.address=zookeeper://127.0.0.1:2181",
                "dubbo.config-centers.my-configcenter.group=group1",
                "dubbo.metadata-reports.my-metadata.address=zookeeper://127.0.0.1:2182",
                "dubbo.metadata-reports.my-metadata.username=User",
                "dubbo.providers.my-provider.host=127.0.0.1",
                "dubbo.consumers.my-consumer.client=netty"
        },
        classes = {
                SpringBootMultipleConfigPropsTest.class
        }
)
@Configuration
@ComponentScan
@EnableDubbo
public class SpringBootMultipleConfigPropsTest {

    @BeforeAll
    public static void beforeAll() {
        ZooKeeperServer.start();
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
        ZooKeeperServer.shutdown();
    }

    @Autowired
    private ConfigManager configManager;

    @Test
    public void testConfigProps() {

        ApplicationConfig applicationConfig = configManager.getApplicationOrElseThrow();
        Assertions.assertEquals("dubbo-demo-application", applicationConfig.getName());

        ModuleConfig moduleConfig = configManager.getModule().get();
        Assertions.assertEquals("dubbo-demo-module", moduleConfig.getName());

        MonitorConfig monitorConfig = configManager.getMonitor().get();
        Assertions.assertEquals("zookeeper://127.0.0.1:32770", monitorConfig.getAddress());

        MetricsConfig metricsConfig = configManager.getMetrics().get();
        Assertions.assertEquals("dubbo", metricsConfig.getProtocol());
        Assertions.assertEquals("20880", metricsConfig.getPort());

        List<ProtocolConfig> defaultProtocols = configManager.getDefaultProtocols();
        Assertions.assertEquals(1, defaultProtocols.size());
        ProtocolConfig protocolConfig = defaultProtocols.get(0);
        Assertions.assertEquals("dubbo", protocolConfig.getName());
        Assertions.assertEquals(20880, protocolConfig.getPort());

        List<RegistryConfig> defaultRegistries = configManager.getDefaultRegistries();
        Assertions.assertEquals(1, defaultRegistries.size());
        RegistryConfig registryConfig = defaultRegistries.get(0);
        Assertions.assertEquals("zookeeper://192.168.99.100:32770", registryConfig.getAddress());

        Collection<ConfigCenterConfig> configCenters = configManager.getConfigCenters();
        Assertions.assertEquals(1, configCenters.size());
        ConfigCenterConfig centerConfig = configCenters.iterator().next();
        Assertions.assertEquals("zookeeper://127.0.0.1:2181", centerConfig.getAddress());
        Assertions.assertEquals("group1", centerConfig.getGroup());

        Collection<MetadataReportConfig> metadataConfigs = configManager.getMetadataConfigs();
        Assertions.assertEquals(1, metadataConfigs.size());
        MetadataReportConfig reportConfig = metadataConfigs.iterator().next();
        Assertions.assertEquals("zookeeper://127.0.0.1:2182", reportConfig.getAddress());
        Assertions.assertEquals("User", reportConfig.getUsername());

        ProviderConfig providerConfig = configManager.getDefaultProvider().get();
        Assertions.assertEquals("127.0.0.1", providerConfig.getHost());

        ConsumerConfig consumerConfig = configManager.getDefaultConsumer().get();
        Assertions.assertEquals("netty", consumerConfig.getClient());

    }

}
