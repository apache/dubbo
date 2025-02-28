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
package org.apache.dubbo.config.bootstrap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.SystemPropertyConfigUtils;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SysProps;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.CommonConstants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_MONITOR_ADDRESS;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;

/**
 * {@link DubboBootstrap} Test
 *
 * @since 2.7.5
 */
class DubboBootstrapTest {

    private static File dubboProperties;

    @BeforeAll
    public static void setUp(@TempDir Path folder) {
        dubboProperties = folder.resolve(CommonConstants.DubboProperty.DUBBO_PROPERTIES_KEY)
                .toFile();
        SystemPropertyConfigUtils.setSystemProperty(
                CommonConstants.DubboProperty.DUBBO_PROPERTIES_KEY, dubboProperties.getAbsolutePath());
    }

    @AfterAll
    public static void tearDown() {
        SystemPropertyConfigUtils.clearSystemProperty(CommonConstants.DubboProperty.DUBBO_PROPERTIES_KEY);
    }

    @AfterEach
    public void afterEach() throws IOException {
        ApplicationModel.reset();
        SysProps.clear();
    }

    @Test
    void checkApplication() {
        SysProps.setProperty("dubbo.application.name", "demo");
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.refresh();
        Assertions.assertEquals("demo", applicationConfig.getName());
    }

    @Test
    void compatibleApplicationShutdown() {
        try {
            System.clearProperty(SHUTDOWN_WAIT_KEY);
            System.clearProperty(SHUTDOWN_WAIT_SECONDS_KEY);

            writeDubboProperties(SHUTDOWN_WAIT_KEY, "100");
            ApplicationModel.defaultModel()
                    .modelEnvironment()
                    .getPropertiesConfiguration()
                    .refresh();
            ConfigValidationUtils.validateApplicationConfig(new ApplicationConfig("demo"));
            Assertions.assertEquals("100", System.getProperty(SHUTDOWN_WAIT_KEY));

            System.clearProperty(SHUTDOWN_WAIT_KEY);
            writeDubboProperties(SHUTDOWN_WAIT_SECONDS_KEY, "1000");
            ApplicationModel.defaultModel()
                    .modelEnvironment()
                    .getPropertiesConfiguration()
                    .refresh();
            ConfigValidationUtils.validateApplicationConfig(new ApplicationConfig("demo"));
            Assertions.assertEquals("1000", System.getProperty(SHUTDOWN_WAIT_SECONDS_KEY));
        } finally {
            System.clearProperty("dubbo.application.name");
            System.clearProperty(SHUTDOWN_WAIT_KEY);
            System.clearProperty(SHUTDOWN_WAIT_SECONDS_KEY);
        }
    }

    @Test
    void testLoadRegistries() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());
        serviceConfig.setApplication(new ApplicationConfig("testLoadRegistries"));
        serviceConfig.setProtocols(Arrays.asList(new ProtocolConfig("dubbo", 20880)));

        String registryId = "nacosRegistry";
        String namespace1 = "test";
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setId(registryId);
        registryConfig.setAddress("nacos://addr1:8848");
        Map<String, String> registryParamMap = Maps.newHashMap();
        registryParamMap.put(CONFIG_NAMESPACE_KEY, namespace1);
        registryConfig.setParameters(registryParamMap);

        String namespace2 = "test2";
        RegistryConfig registryConfig2 = new RegistryConfig();
        registryConfig2.setAddress("polaris://addr1:9999");
        Map<String, String> registryParamMap2 = Maps.newHashMap();
        registryParamMap2.put(CONFIG_NAMESPACE_KEY, namespace2);
        registryConfig2.setParameters(registryParamMap2);

        serviceConfig.setRegistries(Arrays.asList(registryConfig, registryConfig2));
        ProviderConfig providerConfig = Mockito.mock(ProviderConfig.class);
        serviceConfig.setProvider(providerConfig);

        serviceConfig.refresh();

        List<URL> urls = ConfigValidationUtils.loadRegistries(serviceConfig, true);
        Assertions.assertEquals(4, urls.size());

        Map<String, List<URL>> urlsMap =
                urls.stream().collect(Collectors.groupingBy(url -> url.getParameter(REGISTRY_KEY)));
        Assertions.assertEquals(2, urlsMap.get("nacos").size());
        for (URL url : urlsMap.get("nacos")) {
            Assertions.assertTrue(url.getProtocol().contains("registry"));
            Assertions.assertEquals("addr1:8848", url.getAddress());
            Assertions.assertEquals(RegistryService.class.getName(), url.getPath());
            Assertions.assertEquals(registryId + ":" + namespace1, url.getParameter(REGISTRY_CLUSTER_KEY));
            Assertions.assertTrue(url.getParameters().containsKey("timestamp"));
            Assertions.assertTrue(url.getParameters().containsKey("pid"));
            Assertions.assertTrue(url.getParameters().containsKey("registry"));
            Assertions.assertTrue(url.getParameters().containsKey("dubbo"));
        }

        Assertions.assertEquals(2, urlsMap.get("polaris").size());
        for (URL url : urlsMap.get("polaris")) {
            Assertions.assertTrue(url.getProtocol().contains("registry"));
            Assertions.assertEquals("addr1:9999", url.getAddress());
            Assertions.assertEquals(RegistryService.class.getName(), url.getPath());
            Assertions.assertEquals(DEFAULT_KEY + ":" + namespace2, url.getParameter(REGISTRY_CLUSTER_KEY));
            Assertions.assertTrue(url.getParameters().containsKey("timestamp"));
            Assertions.assertTrue(url.getParameters().containsKey("pid"));
            Assertions.assertTrue(url.getParameters().containsKey("registry"));
            Assertions.assertTrue(url.getParameters().containsKey("dubbo"));
        }
    }

    @Test
    void testLoadUserMonitor_address_only() {
        // -Ddubbo.monitor.address=monitor-addr:12080
        SysProps.setProperty(DUBBO_MONITOR_ADDRESS, "monitor-addr:12080");
        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(new MonitorConfig()), new ServiceConfigURL("dubbo", "addr1", 9090));
        Assertions.assertEquals("monitor-addr:12080", url.getAddress());
        Assertions.assertEquals(MonitorService.class.getName(), url.getParameter("interface"));
        Assertions.assertNotNull(url.getParameter("dubbo"));
        Assertions.assertNotNull(url.getParameter("pid"));
        Assertions.assertNotNull(url.getParameter("timestamp"));
    }

    @Test
    void testLoadUserMonitor_registry() {
        // dubbo.monitor.protocol=registry
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("registry");

        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(monitorConfig), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertEquals("dubbo", url.getProtocol());
        Assertions.assertEquals("registry", url.getParameter("protocol"));
    }

    @Test
    void testLoadUserMonitor_service_discovery() {
        // dubbo.monitor.protocol=service-discovery-registry
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("service-discovery-registry");

        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(monitorConfig), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertEquals("dubbo", url.getProtocol());
        Assertions.assertEquals("service-discovery-registry", url.getParameter("protocol"));
    }

    @Test
    void testLoadUserMonitor_no_monitor() {
        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(null), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertNull(url);
    }

    @Test
    void testLoadUserMonitor_user() {
        // dubbo.monitor.protocol=user
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("user");

        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(monitorConfig), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertEquals("user", url.getProtocol());
    }

    @Test
    void testLoadUserMonitor_user_address() {
        // dubbo.monitor.address=user://1.2.3.4:5678?k=v
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setAddress("user://1.2.3.4:5678?param1=value1");
        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(monitorConfig), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertEquals("user", url.getProtocol());
        Assertions.assertEquals("1.2.3.4:5678", url.getAddress());
        Assertions.assertEquals("value1", url.getParameter("param1"));
    }

    private InterfaceConfig getTestInterfaceConfig(MonitorConfig monitorConfig) {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setApplication(new ApplicationConfig("testLoadMonitor"));
        if (monitorConfig != null) {
            interfaceConfig.setMonitor(monitorConfig);
        }
        return interfaceConfig;
    }

    private void writeDubboProperties(String key, String value) {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(dubboProperties));
            Properties properties = new Properties();
            properties.put(key, value);
            properties.store(os, "");
            os.close();
        } catch (IOException e) {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    public static class InterfaceConfig extends AbstractInterfaceConfig {}
}
