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
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

@SpringBootTest(
    properties = {
        "dubbo.applications.application1.name = dubbo-demo-application",
        "dubbo.modules.demo-module.name = dubbo-demo-module",
        "dubbo.registries.my-registry.address = zookeeper://192.168.99.100:32770",
        "dubbo.protocols.dubbo.port=20880",
        "dubbo.metricses.my-metrics.protocol=prometheus",
        "dubbo.metricses.my-metrics.prometheus.pushgateway.enabled=true",
        "dubbo.metricses.my-metrics.prometheus.pushgateway.base-url=localhost:9091",
        "dubbo.metricses.my-metrics.prometheus.pushgateway.username=username",
        "dubbo.metricses.my-metrics.prometheus.pushgateway.password=password",
        "dubbo.metricses.my-metrics.prometheus.pushgateway.job=job",
        "dubbo.metricses.my-metrics.prometheus.pushgateway.push-interval=30",
        "dubbo.metricses.my-metrics.aggregation.enabled=true",
        "dubbo.metricses.my-metrics.aggregation.bucket-num=5",
        "dubbo.metricses.my-metrics.aggregation.time-window-seconds=120",
        "dubbo.monitors.my-monitor.address=zookeeper://127.0.0.1:32770",
        "dubbo.config-centers.my-configcenter.address=${zookeeper.connection.address.1}",
        "dubbo.config-centers.my-configcenter.group=group1",
        "dubbo.metadata-reports.my-metadata.address=${zookeeper.connection.address.2}",
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
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }

    @Autowired
    private ConfigManager configManager;

    @Autowired
    private ModuleModel moduleModel;

    @Test
    public void testConfigProps() {

        ApplicationConfig applicationConfig = configManager.getApplicationOrElseThrow();
        Assertions.assertEquals("dubbo-demo-application", applicationConfig.getName());

        MonitorConfig monitorConfig = configManager.getMonitor().get();
        Assertions.assertEquals("zookeeper://127.0.0.1:32770", monitorConfig.getAddress());

        MetricsConfig metricsConfig = configManager.getMetrics().get();
        Assertions.assertEquals(PROTOCOL_PROMETHEUS, metricsConfig.getProtocol());
        Assertions.assertTrue(metricsConfig.getPrometheus().getPushgateway().getEnabled());
        Assertions.assertEquals("localhost:9091", metricsConfig.getPrometheus().getPushgateway().getBaseUrl());
        Assertions.assertEquals("username", metricsConfig.getPrometheus().getPushgateway().getUsername());
        Assertions.assertEquals("password", metricsConfig.getPrometheus().getPushgateway().getPassword());
        Assertions.assertEquals("job", metricsConfig.getPrometheus().getPushgateway().getJob());
        Assertions.assertEquals(30, metricsConfig.getPrometheus().getPushgateway().getPushInterval());
        Assertions.assertEquals(5, metricsConfig.getAggregation().getBucketNum());
        Assertions.assertEquals(120, metricsConfig.getAggregation().getTimeWindowSeconds());
        Assertions.assertTrue(metricsConfig.getAggregation().getEnabled());

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
        Assertions.assertEquals(ZookeeperRegistryCenterConfig.getConnectionAddress1(), centerConfig.getAddress());
        Assertions.assertEquals("group1", centerConfig.getGroup());

        Collection<MetadataReportConfig> metadataConfigs = configManager.getMetadataConfigs();
        Assertions.assertEquals(1, metadataConfigs.size());
        MetadataReportConfig reportConfig = metadataConfigs.iterator().next();
        Assertions.assertEquals(ZookeeperRegistryCenterConfig.getConnectionAddress2(), reportConfig.getAddress());
        Assertions.assertEquals("User", reportConfig.getUsername());

        // module configs
        ModuleConfigManager moduleConfigManager = moduleModel.getConfigManager();

        ModuleConfig moduleConfig = moduleConfigManager.getModule().get();
        Assertions.assertEquals("dubbo-demo-module", moduleConfig.getName());

        ProviderConfig providerConfig = moduleConfigManager.getDefaultProvider().get();
        Assertions.assertEquals("127.0.0.1", providerConfig.getHost());

        ConsumerConfig consumerConfig = moduleConfigManager.getDefaultConsumer().get();
        Assertions.assertEquals("netty", consumerConfig.getClient());

    }

}
