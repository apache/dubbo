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
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SysProps;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.client.metadata.MetadataUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_MONITOR_ADDRESS;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;
import static org.apache.dubbo.rpc.model.ApplicationModel.getApplicationConfig;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

/**
 * {@link DubboBootstrap} Test
 *
 * @since 2.7.5
 */
public class DubboBootstrapTest {

    private static File dubboProperties;
    private static TestingServer server;
    private static int zkServerPort = NetUtils.getAvailablePort(NetUtils.getRandomPort());
    private static String zkServerAddress = "zookeeper://127.0.0.1:" + zkServerPort;

    @BeforeAll
    public static void setUp(@TempDir Path folder) {
        DubboBootstrap.reset();
        try {
            server = new TestingServer(zkServerPort, true);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
        dubboProperties = folder.resolve(CommonConstants.DUBBO_PROPERTIES_KEY).toFile();
        System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, dubboProperties.getAbsolutePath());
    }

    @AfterAll
    public static void tearDown() {
        System.clearProperty(CommonConstants.DUBBO_PROPERTIES_KEY);
        try {
            server.stop();
        } catch (IOException e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }

    @AfterEach
    public void afterEach() throws IOException {
        DubboBootstrap.reset();
        ApplicationModel.reset();
        SysProps.clear();
    }

    @Test
    public void checkApplication() {
        SysProps.setProperty("dubbo.application.name", "demo");
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.refresh();
        Assertions.assertEquals("demo", applicationConfig.getName());
    }

    @Test
    public void compatibleApplicationShutdown() {
        try {
            ConfigUtils.setProperties(null);
            System.clearProperty(SHUTDOWN_WAIT_KEY);
            System.clearProperty(SHUTDOWN_WAIT_SECONDS_KEY);

            writeDubboProperties(SHUTDOWN_WAIT_KEY, "100");
            ConfigValidationUtils.validateApplicationConfig(new ApplicationConfig("demo"));
            Assertions.assertEquals("100", System.getProperty(SHUTDOWN_WAIT_KEY));

            System.clearProperty(SHUTDOWN_WAIT_KEY);
            ConfigUtils.setProperties(null);
            writeDubboProperties(SHUTDOWN_WAIT_SECONDS_KEY, "1000");
            ConfigValidationUtils.validateApplicationConfig(new ApplicationConfig("demo"));
            Assertions.assertEquals("1000", System.getProperty(SHUTDOWN_WAIT_SECONDS_KEY));
        } finally {
            ConfigUtils.setProperties(null);
            System.clearProperty("dubbo.application.name");
            System.clearProperty(SHUTDOWN_WAIT_KEY);
            System.clearProperty(SHUTDOWN_WAIT_SECONDS_KEY);
        }
    }

    @Test
    public void testLoadRegistries() {
        SysProps.setProperty("dubbo.registry.address", "addr1");

        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());
        serviceConfig.setApplication(new ApplicationConfig("testLoadRegistries"));

        // load configs from props
        DubboBootstrap.getInstance()
                .initialize();

        serviceConfig.refresh();

        //ApplicationModel.getEnvironment().setDynamicConfiguration(new CompositeDynamicConfiguration());
        List<URL> urls = ConfigValidationUtils.loadRegistries(serviceConfig, true);
        Assertions.assertEquals(2, urls.size());
        for (URL url : urls) {
            Assertions.assertTrue(url.getProtocol().contains("registry"));
            Assertions.assertEquals("addr1:9090", url.getAddress());
            Assertions.assertEquals(RegistryService.class.getName(), url.getPath());
            Assertions.assertTrue(url.getParameters().containsKey("timestamp"));
            Assertions.assertTrue(url.getParameters().containsKey("pid"));
            Assertions.assertTrue(url.getParameters().containsKey("registry"));
            Assertions.assertTrue(url.getParameters().containsKey("dubbo"));
        }
    }

    @Test
    public void testLoadUserMonitor_address_only() {
        // -Ddubbo.monitor.address=monitor-addr:12080
        SysProps.setProperty(DUBBO_MONITOR_ADDRESS, "monitor-addr:12080");
        URL url = ConfigValidationUtils.loadMonitor(getTestInterfaceConfig(new MonitorConfig()), new ServiceConfigURL("dubbo", "addr1", 9090));
        Assertions.assertEquals("monitor-addr:12080", url.getAddress());
        Assertions.assertEquals(MonitorService.class.getName(), url.getParameter("interface"));
        Assertions.assertNotNull(url.getParameter("dubbo"));
        Assertions.assertNotNull(url.getParameter("pid"));
        Assertions.assertNotNull(url.getParameter("timestamp"));
    }

    @Test
    public void testLoadUserMonitor_registry() {
        // dubbo.monitor.protocol=registry
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("registry");

        URL url = ConfigValidationUtils.loadMonitor(getTestInterfaceConfig(monitorConfig), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertEquals("dubbo", url.getProtocol());
        Assertions.assertEquals("registry", url.getParameter("protocol"));
    }

    @Test
    public void testLoadUserMonitor_service_discovery() {
        // dubbo.monitor.protocol=service-discovery-registry
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("service-discovery-registry");

        URL url = ConfigValidationUtils.loadMonitor(getTestInterfaceConfig(monitorConfig), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertEquals("dubbo", url.getProtocol());
        Assertions.assertEquals("service-discovery-registry", url.getParameter("protocol"));
    }

    @Test
    public void testLoadUserMonitor_no_monitor() {
        URL url = ConfigValidationUtils.loadMonitor(getTestInterfaceConfig(null), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertNull(url);
    }

    @Test
    public void testLoadUserMonitor_user() {
        // dubbo.monitor.protocol=user
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("user");

        URL url = ConfigValidationUtils.loadMonitor(getTestInterfaceConfig(monitorConfig), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertEquals("user", url.getProtocol());
    }

    @Test
    public void testLoadUserMonitor_user_address() {
        // dubbo.monitor.address=user://1.2.3.4:5678?k=v
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setAddress("user://1.2.3.4:5678?param1=value1");
        URL url = ConfigValidationUtils.loadMonitor(getTestInterfaceConfig(monitorConfig), URL.valueOf("zookeeper://127.0.0.1:2181"));
        Assertions.assertEquals("user", url.getProtocol());
        Assertions.assertEquals("1.2.3.4:5678", url.getAddress());
        Assertions.assertEquals("value1", url.getParameter("param1"));
    }

    private InterfaceConfig getTestInterfaceConfig(MonitorConfig monitorConfig) {
        InterfaceConfig interfaceConfig = new InterfaceConfig();
        interfaceConfig.setApplication(new ApplicationConfig("testLoadMonitor"));
        if(monitorConfig!=null) {
            interfaceConfig.setMonitor(monitorConfig);
        }
        return interfaceConfig;
    }

    @Test
    public void testBootstrapStart() {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(new ApplicationConfig("bootstrap-test"))
            .registry(new RegistryConfig(zkServerAddress))
            .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
            .service(service)
            .start();

        Assertions.assertTrue(bootstrap.isInitialized());
        Assertions.assertTrue(bootstrap.isStarted());
        Assertions.assertFalse(bootstrap.isShutdown());

        Assertions.assertNotNull(bootstrap.serviceInstance);
        Assertions.assertTrue(bootstrap.exportedServices.size() > 0);
        Assertions.assertNotNull(bootstrap.asyncMetadataFuture);
    }

    @Test
    public void testLocalMetadataServiceExporter() {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        int availablePort = NetUtils.getAvailablePort();

        ApplicationConfig applicationConfig = new ApplicationConfig("bootstrap-test");
        applicationConfig.setMetadataServicePort(availablePort);
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(applicationConfig)
            .registry(new RegistryConfig(zkServerAddress))
            .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
            .service(service)
            .start();

        assertMetadataService(bootstrap, availablePort, false);
    }

    @Test
    public void testRemoteMetadataServiceExporter() {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        int availablePort = NetUtils.getAvailablePort();

        ApplicationConfig applicationConfig = new ApplicationConfig("bootstrap-test");
        applicationConfig.setMetadataServicePort(availablePort);
        applicationConfig.setMetadataType(REMOTE_METADATA_STORAGE_TYPE);

        RegistryConfig registryConfig = new RegistryConfig(zkServerAddress);
        registryConfig.setUseAsMetadataCenter(false);
        registryConfig.setUseAsConfigCenter(false);

        Exception exception = null;
        try {
            DubboBootstrap.getInstance()
                .application(applicationConfig)
                .registry(registryConfig)
                .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                .service(service)
                .start();
        } catch (Exception e) {
            exception = e;
            DubboBootstrap.reset();
        }

        Assertions.assertNotNull(exception);

        DubboBootstrap.getInstance()
            .application(applicationConfig)
            .registry(registryConfig)
            .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
            .service(service)
            .metadataReport(new MetadataReportConfig(zkServerAddress))
            .start();

        assertMetadataService(DubboBootstrap.getInstance(), availablePort, true);

    }

    private void assertMetadataService(DubboBootstrap bootstrap, int availablePort, boolean shouldReport) {
        Assertions.assertTrue(bootstrap.metadataServiceExporter.isExported());
        DubboProtocol protocol = DubboProtocol.getDubboProtocol();
        Map<String, Exporter<?>> exporters = protocol.getExporterMap();
        Assertions.assertEquals(2, exporters.size());

        ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setRegistry(new RegistryConfig("N/A"));
        serviceConfig.setInterface(MetadataService.class);
        serviceConfig.setGroup(getApplicationConfig().getName());
        serviceConfig.setVersion(MetadataService.VERSION);
        assertThat(exporters, hasEntry(is(serviceConfig.getUniqueServiceName() + ":" + availablePort), anything()));

        WritableMetadataService metadataService = MetadataUtils.getLocalMetadataService();
        MetadataInfo metadataInfo = metadataService.getDefaultMetadataInfo();
        Assertions.assertNotNull(metadataInfo);
        if (shouldReport) {
            Assertions.assertTrue(metadataInfo.hasReported());
        } else {
            Assertions.assertFalse(metadataInfo.hasReported());
        }
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


    public static class InterfaceConfig extends AbstractInterfaceConfig {

    }

}
