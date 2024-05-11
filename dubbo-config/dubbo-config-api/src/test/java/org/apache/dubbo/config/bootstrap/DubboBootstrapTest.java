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
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SysProps;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.config.metadata.ConfigurableMetadataServiceExporter;
import org.apache.dubbo.config.metadata.ExporterDeployListener;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.apache.dubbo.test.check.registrycenter.config.ZookeeperRegistryCenterConfig;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
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

import static org.apache.dubbo.common.constants.CommonConstants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_MONITOR_ADDRESS;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.metadata.MetadataConstants.REPORT_CONSUMER_URL_KEY;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

/**
 * {@link DubboBootstrap} Test
 *
 * @since 2.7.5
 */
class DubboBootstrapTest {

    private static File dubboProperties;
    private static String zkServerAddress;

    @BeforeAll
    public static void setUp(@TempDir Path folder) {
        DubboBootstrap.reset();
        zkServerAddress = System.getProperty("zookeeper.connection.address.1");
        dubboProperties = folder.resolve(CommonConstants.DUBBO_PROPERTIES_KEY).toFile();
        System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, dubboProperties.getAbsolutePath());
    }

    @AfterAll
    public static void tearDown() {
        System.clearProperty(CommonConstants.DUBBO_PROPERTIES_KEY);
    }

    @AfterEach
    public void afterEach() throws IOException {
        DubboBootstrap.reset();
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

        // load configs from props
        DubboBootstrap.getInstance().initialize();

        serviceConfig.refresh();

        // ApplicationModel.defaultModel().getEnvironment().setDynamicConfiguration(new
        // CompositeDynamicConfiguration());
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
    void testRegistryWithMetadataReport() {
        ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(new DemoServiceImpl());

        List<RegistryConfig> registryConfigs = new ArrayList<>();
        List<MetadataReportConfig> metadataReportConfigs = new ArrayList<>();

        String registryId = "nacosRegistry";
        String namespace1 = "test";
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setId(registryId);
        registryConfig.setAddress(zkServerAddress);
        Map<String, String> registryParamMap = Maps.newHashMap();
        registryParamMap.put(CONFIG_NAMESPACE_KEY, namespace1);
        registryConfig.setParameters(registryParamMap);
        registryConfigs.add(registryConfig);

        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setRegistry(registryId);
        metadataReportConfig.setAddress(registryConfig.getAddress());
        Map<String, String> metadataParamMap = Maps.newHashMap();
        metadataParamMap.put(CONFIG_NAMESPACE_KEY, namespace1);
        metadataParamMap.put(REPORT_CONSUMER_URL_KEY, Boolean.TRUE.toString());
        metadataReportConfig.setParameters(metadataParamMap);
        metadataReportConfig.setReportMetadata(true);
        metadataReportConfigs.add(metadataReportConfig);

        String namespace2 = "test2";
        RegistryConfig registryConfig2 = new RegistryConfig();
        registryConfig2.setAddress(zkServerAddress);
        Map<String, String> registryParamMap2 = Maps.newHashMap();
        registryParamMap2.put(CONFIG_NAMESPACE_KEY, namespace2);
        registryConfig2.setParameters(registryParamMap2);
        registryConfigs.add(registryConfig2);

        MetadataReportConfig metadataReportConfig2 = new MetadataReportConfig();
        metadataReportConfig2.setAddress(registryConfig2.getAddress());
        Map<String, String> metadataParamMap2 = Maps.newHashMap();
        metadataParamMap2.put(CONFIG_NAMESPACE_KEY, namespace2);
        metadataParamMap2.put(REPORT_CONSUMER_URL_KEY, Boolean.TRUE.toString());
        metadataReportConfig2.setParameters(metadataParamMap2);
        metadataReportConfig2.setReportMetadata(true);
        metadataReportConfigs.add(metadataReportConfig2);

        serviceConfig.setRegistries(registryConfigs);

        DubboBootstrap.getInstance()
                .application(new ApplicationConfig("testRegistryWithMetadataReport"))
                .registries(registryConfigs)
                .metadataReports(metadataReportConfigs)
                .service(serviceConfig)
                .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                .start();

        ApplicationModel applicationModel = DubboBootstrap.getInstance().getApplicationModel();
        MetadataReportInstance metadataReportInstance =
                applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);

        Map<String, MetadataReport> metadataReports = metadataReportInstance.getMetadataReports(true);
        Assertions.assertEquals(2, metadataReports.size());

        List<URL> urls = ConfigValidationUtils.loadRegistries(serviceConfig, true);
        Assertions.assertEquals(4, urls.size());

        for (URL url : urls) {
            Assertions.assertTrue(metadataReports.containsKey(url.getParameter(REGISTRY_CLUSTER_KEY)));
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
                getTestInterfaceConfig(monitorConfig),
                URL.valueOf(ZookeeperRegistryCenterConfig.getConnectionAddress()));
        Assertions.assertEquals("dubbo", url.getProtocol());
        Assertions.assertEquals("registry", url.getParameter("protocol"));
    }

    @Test
    void testLoadUserMonitor_service_discovery() {
        // dubbo.monitor.protocol=service-discovery-registry
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("service-discovery-registry");

        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(monitorConfig),
                URL.valueOf(ZookeeperRegistryCenterConfig.getConnectionAddress()));
        Assertions.assertEquals("dubbo", url.getProtocol());
        Assertions.assertEquals("service-discovery-registry", url.getParameter("protocol"));
    }

    @Test
    void testLoadUserMonitor_no_monitor() {
        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(null), URL.valueOf(ZookeeperRegistryCenterConfig.getConnectionAddress()));
        Assertions.assertNull(url);
    }

    @Test
    void testLoadUserMonitor_user() {
        // dubbo.monitor.protocol=user
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("user");

        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(monitorConfig),
                URL.valueOf(ZookeeperRegistryCenterConfig.getConnectionAddress()));
        Assertions.assertEquals("user", url.getProtocol());
    }

    @Test
    void testLoadUserMonitor_user_address() {
        // dubbo.monitor.address=user://1.2.3.4:5678?k=v
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setAddress("user://1.2.3.4:5678?param1=value1");
        URL url = ConfigValidationUtils.loadMonitor(
                getTestInterfaceConfig(monitorConfig),
                URL.valueOf(ZookeeperRegistryCenterConfig.getConnectionAddress()));
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

    @Test
    void testBootstrapStart() {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(new ApplicationConfig("bootstrap-test"))
                .registry(new RegistryConfig(zkServerAddress))
                .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                .service(service)
                .start();

        Assertions.assertTrue(bootstrap.isInitialized());
        Assertions.assertTrue(bootstrap.isStarted());
        Assertions.assertFalse(bootstrap.isStopped());

        ApplicationModel applicationModel = bootstrap.getApplicationModel();
        DefaultApplicationDeployer applicationDeployer = getApplicationDeployer(applicationModel);
        Assertions.assertNotNull(ReflectUtils.getFieldValue(applicationDeployer, "asyncMetadataFuture"));
        Assertions.assertTrue(applicationModel
                        .getDefaultModule()
                        .getServiceRepository()
                        .getExportedServices()
                        .size()
                > 0);
    }

    private DefaultApplicationDeployer getApplicationDeployer(ApplicationModel applicationModel) {
        return (DefaultApplicationDeployer) DefaultApplicationDeployer.get(applicationModel);
    }

    @Test
    void testLocalMetadataServiceExporter() {
        ServiceConfig<DemoService> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());

        int availablePort = NetUtils.getAvailablePort();

        ApplicationConfig applicationConfig = new ApplicationConfig("bootstrap-test");
        applicationConfig.setMetadataServicePort(availablePort);
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(applicationConfig)
                .registry(new RegistryConfig(zkServerAddress))
                .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                .service(service)
                .start();

        assertMetadataService(bootstrap, availablePort, true);
    }

    @Test
    void testRemoteMetadataServiceExporter() {
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

        DubboBootstrap.getInstance()
                .application(applicationConfig)
                .registry(registryConfig)
                .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                .service(service)
                .metadataReport(new MetadataReportConfig(zkServerAddress))
                .start();

        assertMetadataService(DubboBootstrap.getInstance(), availablePort, false);
    }

    @Test
    void testRemoteMetadataServiceExporterCheckMetadataType() {

        Assertions.assertThrowsExactly(IllegalStateException.class, () -> {
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

            DubboBootstrap.getInstance()
                    .application(applicationConfig)
                    .registry(registryConfig)
                    .protocol(new ProtocolConfig(CommonConstants.DUBBO_PROTOCOL, -1))
                    .service(service)
                    .start();
        });
    }

    private ExporterDeployListener getListener(ApplicationModel model) {
        return (ExporterDeployListener)
                model.getExtensionLoader(ApplicationDeployListener.class).getExtension("exporter");
    }

    private void assertMetadataService(DubboBootstrap bootstrap, int availablePort, boolean metadataExported) {
        ExporterDeployListener listener = getListener(bootstrap.getApplicationModel());
        ConfigurableMetadataServiceExporter metadataServiceExporter = listener.getMetadataServiceExporter();
        Assertions.assertEquals(metadataExported, metadataServiceExporter.isExported());
        DubboProtocol protocol = DubboProtocol.getDubboProtocol(bootstrap.getApplicationModel());
        Map<String, Exporter<?>> exporters = protocol.getExporterMap();
        if (metadataExported) {
            Assertions.assertEquals(2, exporters.size());

            ServiceConfig<MetadataService> serviceConfig = new ServiceConfig<>();
            serviceConfig.setRegistry(new RegistryConfig("N/A"));
            serviceConfig.setInterface(MetadataService.class);
            serviceConfig.setGroup(
                    ApplicationModel.defaultModel().getCurrentConfig().getName());
            serviceConfig.setVersion(MetadataService.VERSION);
            assertThat(exporters, hasEntry(is(serviceConfig.getUniqueServiceName() + ":" + availablePort), anything()));
        } else {
            Assertions.assertEquals(1, exporters.size());
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

    public static class InterfaceConfig extends AbstractInterfaceConfig {}
}
