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
package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.config.CompositeConfiguration;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;

import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.listener.ListenerInvokerWrapper;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.curator.test.TestingServer;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.protocol.injvm.InjvmInvoker;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.BROADCAST_CLUSTER;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METRICS_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.METRICS_PORT;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUMP_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_LOCAL_FILE_CACHE_ENABLED;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LIVENESS_PROBE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.READINESS_PROBE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.STARTUP_PROBE;
import static org.apache.dubbo.common.constants.CommonConstants.URL_MERGE_PROCESSOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFER_THREAD_NUM_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFER_BACKGROUND_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFER_ASYNC_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GENERIC_KEY;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PUBLISH_INSTANCE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PUBLISH_INTERFACE_KEY;
import static org.apache.dubbo.registry.Constants.ENABLE_CONFIGURATION_LISTEN;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.DEFAULT_STUB_EVENT;
import static org.apache.dubbo.rpc.Constants.LOCAL_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReferenceConfigTest {
    private TestingServer zkServer;
    private String zkUrl;
    private String registryUrl;

    @BeforeEach
    public void setUp() throws Exception {
        DubboBootstrap.reset();
        int zkServerPort = NetUtils.getAvailablePort(NetUtils.getRandomPort());
        this.zkServer = new TestingServer(zkServerPort, true);
        this.zkServer.start();
        this.zkUrl = "zookeeper://localhost:" + zkServerPort;
        this.registryUrl = "registry://localhost:" + zkServerPort+"?registry=zookeeper";
        ApplicationModel.getConfigManager();
        DubboBootstrap.getInstance();
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        zkServer.stop();
        Mockito.framework().clearInlineMocks();

    }

    /**
     * Test whether the configuration required for the aggregation service reference meets expectations
     */
    @Test
    public void testAppendConfig() {

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setClient("netty");
        referenceConfig.setGeneric(Boolean.FALSE.toString());
        referenceConfig.setProtocol("dubbo");
        referenceConfig.setInit(true);
        referenceConfig.setLazy(false);
        referenceConfig.setInjvm(false);
        referenceConfig.setReconnect("reconnect");
        referenceConfig.setSticky(false);
        referenceConfig.setStub(DEFAULT_STUB_EVENT);
        referenceConfig.setRouter("default");
        referenceConfig.setReferAsync(true);

        DubboBootstrap.getInstance()
            .application("application1")
            .initialize();
        referenceConfig.setBootstrap(DubboBootstrap.getInstance());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        applicationConfig.setVersion("v1");
        applicationConfig.setOwner("owner1");
        applicationConfig.setOrganization("bu1");
        applicationConfig.setArchitecture("architecture1");
        applicationConfig.setEnvironment("test");
        applicationConfig.setCompiler("javassist");
        applicationConfig.setLogger("log4j");
        applicationConfig.setDumpDirectory("/");
        applicationConfig.setQosEnable(false);
        applicationConfig.setQosHost("127.0.0.1");
        applicationConfig.setQosPort(77777);
        applicationConfig.setQosAcceptForeignIp(false);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);
        applicationConfig.setShutwait("5");
        applicationConfig.setMetadataType("local");
        applicationConfig.setRegisterConsumer(false);
        applicationConfig.setRepository("repository1");
        applicationConfig.setEnableFileCache(false);
        applicationConfig.setPublishInstance(false);
        applicationConfig.setPublishInterface(false);
        applicationConfig.setProtocol("dubbo");
        applicationConfig.setMetadataServicePort(88888);
        applicationConfig.setLivenessProbe("livenessProbe");
        applicationConfig.setReadinessProbe("readinessProb");
        applicationConfig.setStartupProbe("startupProbe");

        MonitorConfig monitorConfig = new MonitorConfig();
        applicationConfig.setMonitor(monitorConfig);

        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setProtocol("metricProtocol");
        metricsConfig.setPort("55555");

        ModuleConfig moduleConfig = new ModuleConfig();
        moduleConfig.setMonitor("default");
        moduleConfig.setName("module1");
        moduleConfig.setOrganization("application1");
        moduleConfig.setVersion("v1");
        moduleConfig.setOwner("owner1");

        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setClient("netty");
        consumerConfig.setThreadpool("fixed");
        consumerConfig.setCorethreads(200);
        consumerConfig.setQueues(500);
        consumerConfig.setThreads(300);
        consumerConfig.setShareconnections(10);
        consumerConfig.setUrlMergeProcessor("default");
        consumerConfig.setReferThreadNum(20);
        consumerConfig.setReferBackground(false);
        referenceConfig.setConsumer(consumerConfig);

        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("method1");
        methodConfig.setStat(1);
        methodConfig.setRetries(0);
        methodConfig.setExecutes(10);
        methodConfig.setDeprecated(false);
        methodConfig.setSticky(false);
        methodConfig.setReturn(false);
        methodConfig.setService("service");
        methodConfig.setServiceId(DemoService.class.getName());
        methodConfig.setParentPrefix("demo");


        referenceConfig.setMethods(Collections.singletonList(methodConfig));

        ConfigManager configManager = mock(ConfigManager.class);
        Environment environment = mock(Environment.class);
        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        Configuration dynamicGlobalConfiguration = mock(Configuration.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        ConsumerModel consumerModel = mock(ConsumerModel.class);

        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);
        when(configManager.getMetrics()).thenReturn(Optional.of(metricsConfig));
        when(configManager.getModule()).thenReturn(Optional.of(moduleConfig));

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);
        applicationModelMockedStatic.when(ApplicationModel::getServiceRepository).thenReturn(serviceRepository);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        MockedStatic<MetadataReportInstance> metadataReportInstanceMockedStatic =
            Mockito.mockStatic(MetadataReportInstance.class);

        MetadataReport metadataReport = mock(MetadataReport.class);
        metadataReportInstanceMockedStatic.when(() -> MetadataReportInstance.getMetadataReport("default"))
            .thenReturn(metadataReport);


        when(serviceRepository.lookupReferredService("org.apache.dubbo.config.api.DemoService"))
            .thenReturn(consumerModel);

        referenceConfig.refreshed.set(true);
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl);
        applicationConfig.setRegistries(Collections.singletonList(registry));
        applicationConfig.setRegistryIds(registry.getId());
        moduleConfig.setRegistries(Collections.singletonList(registry));

        referenceConfig.setRegistry(registry);

        referenceConfig.init();

        ServiceMetadata serviceMetadata = referenceConfig.getServiceMetadata();

        // verify additional side parameter
        Assertions.assertEquals(CONSUMER_SIDE, serviceMetadata.getAttachments().get(SIDE_KEY));

        // verify additional interface parameter
        Assertions.assertEquals(DemoService.class.getName(), serviceMetadata.getAttachments().get(INTERFACE_KEY));

        // verify additional metadata-type parameter
        Assertions.assertEquals(DEFAULT_METADATA_STORAGE_TYPE, serviceMetadata.getAttachments().get(METADATA_KEY));

        // verify additional register.ip parameter
        Assertions.assertEquals(NetUtils.getLocalHost(), serviceMetadata.getAttachments().get(REGISTER_IP_KEY));

        // verify additional runtime parameters
        Assertions.assertEquals(Version.getProtocolVersion(), serviceMetadata.getAttachments().get(DUBBO_VERSION_KEY));
        Assertions.assertEquals(Version.getVersion(), serviceMetadata.getAttachments().get(RELEASE_KEY));
        Assertions.assertTrue(serviceMetadata.getAttachments().containsKey(TIMESTAMP_KEY));
        Assertions.assertEquals(String.valueOf(ConfigUtils.getPid()), serviceMetadata.getAttachments().get(PID_KEY));

        // verify additional metric config
        Assertions.assertEquals(metricsConfig.getProtocol(), serviceMetadata.getAttachments().get(METRICS_PROTOCOL));
        Assertions.assertEquals(metricsConfig.getPort(), serviceMetadata.getAttachments().get(METRICS_PORT));

        // verify additional application config
        Assertions.assertEquals(applicationConfig.getName(), serviceMetadata.getAttachments().get(APPLICATION_KEY));
        Assertions.assertEquals(applicationConfig.getOwner(), serviceMetadata.getAttachments().get("owner"));
        Assertions.assertEquals(applicationConfig.getVersion(),
            serviceMetadata.getAttachments().get(APPLICATION_VERSION_KEY));
        Assertions.assertEquals(applicationConfig.getOrganization(),
            serviceMetadata.getAttachments().get("organization"));
        Assertions.assertEquals(applicationConfig.getArchitecture(),
            serviceMetadata.getAttachments().get("architecture"));
        Assertions.assertEquals(applicationConfig.getEnvironment(),
            serviceMetadata.getAttachments().get("environment"));
        Assertions.assertEquals(applicationConfig.getCompiler(), serviceMetadata.getAttachments().get("compiler"));
        Assertions.assertEquals(applicationConfig.getLogger(), serviceMetadata.getAttachments().get("logger"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("registries"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("registry.ids"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("monitor"));
        Assertions.assertEquals(applicationConfig.getDumpDirectory(),
            serviceMetadata.getAttachments().get(DUMP_DIRECTORY));
        Assertions.assertEquals(applicationConfig.getQosEnable().toString(),
            serviceMetadata.getAttachments().get(QOS_ENABLE));
        Assertions.assertEquals(applicationConfig.getQosHost(),
            serviceMetadata.getAttachments().get(QOS_HOST));
        Assertions.assertEquals(applicationConfig.getQosPort().toString(),
            serviceMetadata.getAttachments().get(QOS_PORT));
        Assertions.assertEquals(applicationConfig.getQosAcceptForeignIp().toString(),
            serviceMetadata.getAttachments().get(ACCEPT_FOREIGN_IP));
        Assertions.assertEquals(applicationConfig.getParameters().get("key1"),
            serviceMetadata.getAttachments().get("key1"));
        Assertions.assertEquals(applicationConfig.getParameters().get("key2"),
            serviceMetadata.getAttachments().get("key2"));
        Assertions.assertEquals(applicationConfig.getShutwait(),
            serviceMetadata.getAttachments().get("shutwait"));
        Assertions.assertEquals(applicationConfig.getMetadataType(),
            serviceMetadata.getAttachments().get(METADATA_KEY));
        Assertions.assertEquals(applicationConfig.getRegisterConsumer().toString(),
            serviceMetadata.getAttachments().get("register.consumer"));
        Assertions.assertEquals(applicationConfig.getRepository(),
            serviceMetadata.getAttachments().get("repository"));
        Assertions.assertEquals(applicationConfig.getEnableFileCache().toString(),
            serviceMetadata.getAttachments().get(REGISTRY_LOCAL_FILE_CACHE_ENABLED));
        Assertions.assertEquals(applicationConfig.getPublishInstance().toString(),
            serviceMetadata.getAttachments().get(REGISTRY_PUBLISH_INSTANCE_KEY));
        Assertions.assertEquals(applicationConfig.getPublishInterface().toString(),
            serviceMetadata.getAttachments().get(REGISTRY_PUBLISH_INTERFACE_KEY));
        Assertions.assertTrue(serviceMetadata.getAttachments().containsKey(REGISTRY_PUBLISH_INTERFACE_KEY));
        Assertions.assertEquals(applicationConfig.getMetadataServicePort().toString(),
            serviceMetadata.getAttachments().get(METADATA_SERVICE_PORT_KEY));
        Assertions.assertEquals(applicationConfig.getLivenessProbe(),
            serviceMetadata.getAttachments().get(LIVENESS_PROBE_KEY));
        Assertions.assertEquals(applicationConfig.getReadinessProbe(),
            serviceMetadata.getAttachments().get(READINESS_PROBE_KEY));
        Assertions.assertEquals(applicationConfig.getStartupProbe(),
            serviceMetadata.getAttachments().get(STARTUP_PROBE));

        // verify additional module config
        Assertions.assertEquals(moduleConfig.getName(), serviceMetadata.getAttachments().get("module"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("monitor"));
        Assertions.assertEquals(moduleConfig.getOrganization(),
            serviceMetadata.getAttachments().get("module.organization"));
        Assertions.assertEquals(moduleConfig.getOwner(), serviceMetadata.getAttachments().get("module.owner"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("registries"));
        Assertions.assertEquals(moduleConfig.getVersion(), serviceMetadata.getAttachments().get("module.version"));

        // verify additional consumer config
        Assertions.assertEquals(consumerConfig.getClient(), serviceMetadata.getAttachments().get("client"));
        Assertions.assertEquals(consumerConfig.getThreadpool(), serviceMetadata.getAttachments().get("threadpool"));
        Assertions.assertEquals(consumerConfig.getCorethreads().toString(),
            serviceMetadata.getAttachments().get("corethreads"));
        Assertions.assertEquals(consumerConfig.getQueues().toString(),
            serviceMetadata.getAttachments().get("queues"));
        Assertions.assertEquals(consumerConfig.getThreads().toString(),
            serviceMetadata.getAttachments().get("threads"));
        Assertions.assertEquals(consumerConfig.getShareconnections().toString(),
            serviceMetadata.getAttachments().get("shareconnections"));
        Assertions.assertEquals(consumerConfig.getUrlMergeProcessor(),
            serviceMetadata.getAttachments().get(URL_MERGE_PROCESSOR_KEY));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey(REFER_THREAD_NUM_KEY));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey(REFER_BACKGROUND_KEY));

        // verify additional reference config
        Assertions.assertEquals(referenceConfig.getClient(), serviceMetadata.getAttachments().get("client"));
        Assertions.assertEquals(referenceConfig.getGeneric(), serviceMetadata.getAttachments().get("generic"));
        Assertions.assertEquals(referenceConfig.getProtocol(), serviceMetadata.getAttachments().get("protocol"));
        Assertions.assertEquals(referenceConfig.isInit().toString(), serviceMetadata.getAttachments().get("init"));
        Assertions.assertEquals(referenceConfig.getLazy().toString(), serviceMetadata.getAttachments().get("lazy"));
        Assertions.assertEquals(referenceConfig.isInjvm().toString(), serviceMetadata.getAttachments().get("injvm"));
        Assertions.assertEquals(referenceConfig.getReconnect(), serviceMetadata.getAttachments().get("reconnect"));
        Assertions.assertEquals(referenceConfig.getSticky().toString(), serviceMetadata.getAttachments().get("sticky"));
        Assertions.assertEquals(referenceConfig.getStub(), serviceMetadata.getAttachments().get("stub"));
        Assertions.assertEquals(referenceConfig.getProvidedBy(), serviceMetadata.getAttachments().get("provided-by"));
        Assertions.assertEquals(referenceConfig.getRouter(), serviceMetadata.getAttachments().get("router"));
        Assertions.assertEquals(referenceConfig.getReferAsync().toString(),
            serviceMetadata.getAttachments().get(REFER_ASYNC_KEY));

        // verify additional method config
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("name"));
        Assertions.assertEquals(methodConfig.getStat().toString(),
            serviceMetadata.getAttachments().get("method1.stat"));
        Assertions.assertEquals(methodConfig.getRetries().toString(),
            serviceMetadata.getAttachments().get("method1.retries"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("method1.reliable"));
        Assertions.assertEquals(methodConfig.getExecutes().toString(),
            serviceMetadata.getAttachments().get("method1.executes"));
        Assertions.assertEquals(methodConfig.getDeprecated().toString(),
            serviceMetadata.getAttachments().get("method1.deprecated"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("method1.stick"));
        Assertions.assertEquals(methodConfig.isReturn().toString(),
            serviceMetadata.getAttachments().get("method1.return"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("method1.service"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("method1.service.id"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("method1.parent.prefix"));

        // verify additional revision and methods parameter
        Assertions.assertEquals(Version.getVersion(referenceConfig.getInterfaceClass(), referenceConfig.getVersion()),
            serviceMetadata.getAttachments().get(REVISION_KEY));
        Assertions.assertTrue(serviceMetadata.getAttachments().containsKey(METHODS_KEY));
        Assertions.assertEquals(DemoService.class.getMethods().length,
            StringUtils.split((String) serviceMetadata.getAttachments().get(METHODS_KEY), ',').length);

        applicationModelMockedStatic.closeOnDemand();
        metadataReportInstanceMockedStatic.closeOnDemand();
    }

    @Test
    public void testShouldJvmRefer() {

        Map<String, String> parameters = new HashMap<>();

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();

        // verify that if injvm is configured as true, local references should be made
        referenceConfig.setInjvm(true);
        Assertions.assertTrue(referenceConfig.shouldJvmRefer(parameters));

        // verify that if injvm is configured as false, local references should not be made
        referenceConfig.setInjvm(false);
        Assertions.assertFalse(referenceConfig.shouldJvmRefer(parameters));

        // verify that if url is configured, local reference should not be made
        referenceConfig.setInjvm(null);
        referenceConfig.setUrl("dubbo://127.0.0.1:20880/DemoService");
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        Assertions.assertFalse(referenceConfig.shouldJvmRefer(parameters));
        parameters.clear();

        // verify that if scope is configured as local, local references should be made
        referenceConfig.setInjvm(null);
        referenceConfig.setUrl(null);
        parameters.put(SCOPE_KEY, SCOPE_LOCAL);
        Assertions.assertTrue(referenceConfig.shouldJvmRefer(parameters));
        parameters.clear();

        // verify that if url protocol is configured as injvm, local references should be made
        referenceConfig.setInjvm(null);
        referenceConfig.setUrl(null);
        parameters.put(LOCAL_PROTOCOL, "true");
        Assertions.assertTrue(referenceConfig.shouldJvmRefer(parameters));
        parameters.clear();

        // verify that if generic is configured as true, local references should not be made
        referenceConfig.setInjvm(null);
        referenceConfig.setUrl(null);
        parameters.put(GENERIC_KEY, "true");
        Assertions.assertFalse(referenceConfig.shouldJvmRefer(parameters));
        parameters.clear();

        // verify that if the service has been exposed, and the cluster is not configured with broadcast, local reference should be made
        referenceConfig.setInjvm(null);
        referenceConfig.setUrl(null);
        ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        DemoService service = new DemoServiceImpl();
        URL url = URL.valueOf("dubbo://127.0.0.1/DemoService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName());
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        Exporter<?> exporter = InjvmProtocol.getInjvmProtocol().export(proxy.getInvoker(service, DemoService.class, url));
        InjvmProtocol.getInjvmProtocol().getExporterMap().put(DemoService.class.getName(), exporter);
        Assertions.assertTrue(referenceConfig.shouldJvmRefer(parameters));

        // verify that if the service has been exposed, and the cluster is configured with broadcast, local reference should not be made
        parameters.put(CLUSTER_KEY, BROADCAST_CLUSTER);
        Assertions.assertFalse(referenceConfig.shouldJvmRefer(parameters));
        parameters.clear();
        InjvmProtocol.getInjvmProtocol().destroy();
    }

    @Test
    public void testCreateInvokerForLocalRefer() {

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setScope(LOCAL_KEY);

        DubboBootstrap.getInstance()
            .application("application1")
            .initialize();
        referenceConfig.setBootstrap(DubboBootstrap.getInstance());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);

        ConfigManager configManager = mock(ConfigManager.class);
        Environment environment = mock(Environment.class);
        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        Configuration dynamicGlobalConfiguration = mock(Configuration.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        ConsumerModel consumerModel = mock(ConsumerModel.class);

        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);
        applicationModelMockedStatic.when(ApplicationModel::getServiceRepository).thenReturn(serviceRepository);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        MockedStatic<MetadataReportInstance> metadataReportInstanceMockedStatic =
            Mockito.mockStatic(MetadataReportInstance.class);

        MetadataReport metadataReport = mock(MetadataReport.class);
        metadataReportInstanceMockedStatic.when(() -> MetadataReportInstance.getMetadataReport("default"))
            .thenReturn(metadataReport);


        when(serviceRepository.lookupReferredService("org.apache.dubbo.config.api.DemoService"))
            .thenReturn(consumerModel);

        referenceConfig.refreshed.set(true);
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);

        referenceConfig.init();
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof ListenerInvokerWrapper);
        Assertions.assertTrue(((ListenerInvokerWrapper<?>) referenceConfig.getInvoker()).getInvoker() instanceof InjvmInvoker);
        URL url = ((ListenerInvokerWrapper<?>) referenceConfig.getInvoker()).getInvoker().getUrl();
        Assertions.assertEquals("application1", url.getParameter("application"));
        Assertions.assertEquals("value1", url.getParameter("key1"));
        Assertions.assertEquals("value2", url.getParameter("key2"));

        applicationModelMockedStatic.closeOnDemand();
        metadataReportInstanceMockedStatic.closeOnDemand();
    }


    /**
     * Verify the configuration of the registry protocol for remote reference
     */
    @Test
    public  void testCreateInvokerForRemoteRefer(){

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setGeneric(Boolean.FALSE.toString());
        referenceConfig.setProtocol("dubbo");
        referenceConfig.setInit(true);
        referenceConfig.setLazy(false);
        referenceConfig.setInjvm(false);

        DubboBootstrap.getInstance()
            .application("application1")
            .initialize();
        referenceConfig.setBootstrap(DubboBootstrap.getInstance());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);

        ConfigManager configManager = mock(ConfigManager.class);
        Environment environment = mock(Environment.class);
        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        Configuration dynamicGlobalConfiguration = mock(Configuration.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        ConsumerModel consumerModel = mock(ConsumerModel.class);

        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);
        applicationModelMockedStatic.when(ApplicationModel::getServiceRepository).thenReturn(serviceRepository);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        MockedStatic<MetadataReportInstance> metadataReportInstanceMockedStatic =
            Mockito.mockStatic(MetadataReportInstance.class);

        MetadataReport metadataReport = mock(MetadataReport.class);
        metadataReportInstanceMockedStatic.when(() -> MetadataReportInstance.getMetadataReport("default"))
            .thenReturn(metadataReport);


        when(serviceRepository.lookupReferredService("org.apache.dubbo.config.api.DemoService"))
            .thenReturn(consumerModel);

        referenceConfig.refreshed.set(true);
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl);
        applicationConfig.setRegistries(Collections.singletonList(registry));
        applicationConfig.setRegistryIds(registry.getId());

        referenceConfig.setRegistry(registry);

        referenceConfig.init();
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof MigrationInvoker);

        applicationModelMockedStatic.closeOnDemand();
        metadataReportInstanceMockedStatic.closeOnDemand();
    }

    /**
     * Verify that the registry url is directly configured for remote reference
     */
    @Test
    public  void testCreateInvokerWithRegistryUrlForRemoteRefer(){

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setGeneric(Boolean.FALSE.toString());
        referenceConfig.setProtocol("dubbo");
        referenceConfig.setInit(true);
        referenceConfig.setLazy(false);
        referenceConfig.setInjvm(false);

        DubboBootstrap.getInstance()
            .application("application1")
            .initialize();
        referenceConfig.setBootstrap(DubboBootstrap.getInstance());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);

        ConfigManager configManager = mock(ConfigManager.class);
        Environment environment = mock(Environment.class);
        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        Configuration dynamicGlobalConfiguration = mock(Configuration.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        ConsumerModel consumerModel = mock(ConsumerModel.class);

        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);
        applicationModelMockedStatic.when(ApplicationModel::getServiceRepository).thenReturn(serviceRepository);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        MockedStatic<MetadataReportInstance> metadataReportInstanceMockedStatic =
            Mockito.mockStatic(MetadataReportInstance.class);

        MetadataReport metadataReport = mock(MetadataReport.class);
        metadataReportInstanceMockedStatic.when(() -> MetadataReportInstance.getMetadataReport("default"))
            .thenReturn(metadataReport);


        when(serviceRepository.lookupReferredService("org.apache.dubbo.config.api.DemoService"))
            .thenReturn(consumerModel);

        referenceConfig.refreshed.set(true);
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);

        referenceConfig.setUrl(registryUrl);
        referenceConfig.init();
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof MigrationInvoker);

        applicationModelMockedStatic.closeOnDemand();
        metadataReportInstanceMockedStatic.closeOnDemand();
    }

    @Test
    @Disabled("Disabled due to Github Actions environment")
    public void testInjvm() throws Exception {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("test-protocol-random-port");
        application.setEnableFileCache(false);
        ApplicationModel.getConfigManager().setApplication(application);

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl);

        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");

        ServiceConfig<DemoService> demoService;
        demoService = new ServiceConfig<>();
        demoService.setInterface(DemoService.class);
        demoService.setRef(new DemoServiceImpl());
        demoService.setRegistry(registry);
        demoService.setProtocol(protocol);

        ReferenceConfig<DemoService> rc = new ReferenceConfig<>();
        rc.setRegistry(registry);
        rc.setInterface(DemoService.class.getName());
        rc.setScope(SCOPE_REMOTE);

        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            demoService.export();
            rc.get();
            Assertions.assertFalse(LOCAL_PROTOCOL.equalsIgnoreCase(
                rc.getInvoker().getUrl().getProtocol()));
        } finally {
            System.clearProperty("java.net.preferIPv4Stack");
            rc.destroy();
            demoService.unexport();
        }

        // Manually trigger dubbo resource recycling.
        DubboBootstrap.getInstance().destroy();
    }

    /**
     * unit test for dubbo-1765
     */
    @Test
    public void test1ReferenceRetry() {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("test-reference-retry");
        application.setEnableFileCache(false);
        ApplicationModel.getConfigManager().setApplication(application);

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl);
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("injvm");

        ReferenceConfig<DemoService> rc = new ReferenceConfig<>();
        rc.setRegistry(registry);
        rc.setInterface(DemoService.class.getName());

        boolean success = false;
        DemoService demoService = null;
        try {
            demoService = rc.get();
            success = true;
        } catch (Exception e) {
            // ignore
        }
        Assertions.assertFalse(success);
        Assertions.assertNull(demoService);

        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
            DemoService service = new DemoServiceImpl();
            URL url = URL.valueOf("dubbo://127.0.0.1/DemoService")
                .addParameter(INTERFACE_KEY, DemoService.class.getName());
            InjvmProtocol.getInjvmProtocol().export(proxy.getInvoker(service, DemoService.class, url));
            demoService = rc.get();
            success = true;
        } catch (Exception e) {
            // ignore
        } finally {
            rc.destroy();
            InjvmProtocol.getInjvmProtocol().destroy();
            System.clearProperty("java.net.preferIPv4Stack");

        }
        Assertions.assertTrue(success);
        Assertions.assertNotNull(demoService);

    }

    @Test
    public void testMetaData() {
        ReferenceConfig config = new ReferenceConfig();
        Map<String, String> metaData = config.getMetaData();
        Assertions.assertEquals(0, metaData.size(), "Expect empty metadata but found: " + metaData);

        // test merged and override consumer attributes
        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setAsync(true);
        consumerConfig.setActives(10);
        config.setConsumer(consumerConfig);
        config.setAsync(false);// override

        metaData = config.getMetaData();
        Assertions.assertEquals(2, metaData.size());
        Assertions.assertEquals("" + consumerConfig.getActives(), metaData.get("actives"));
        Assertions.assertEquals("" + config.isAsync(), metaData.get("async"));

    }

    @Test
    public void testGetPrefixes() {

        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setInterface(DemoService.class);

        List<String> prefixes = referenceConfig.getPrefixes();
        Assertions.assertTrue(prefixes.contains("dubbo.reference." + referenceConfig.getInterface()));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            referenceConfig.getPrefixes();
        }
        long end = System.currentTimeMillis();
        System.out.println("ReferenceConfig get prefixes cost: " + (end - start));

    }

    @Test
    public void testLargeReferences() throws InterruptedException {
        int amount = 10000;
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("test-app");
        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setAddress("metadata://");
        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
        configCenterConfig.setAddress("diamond://");

        testInitReferences(0, amount, applicationConfig, metadataReportConfig, configCenterConfig);
        ApplicationModel.getConfigManager().clear();
        testInitReferences(0, 1, applicationConfig, metadataReportConfig, configCenterConfig);

        long t1 = System.currentTimeMillis();
        int nThreads = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (int i = 0; i < nThreads; i++) {
            int perCount = (int) (1.0 * amount / nThreads);
            int start = perCount * i;
            int end = start + perCount;
            if (i == nThreads - 1) {
                end = amount;
            }
            int finalEnd = end;
            System.out.println(String.format("start thread %s: range: %s - %s, count: %s", i, start, end, (end - start)));
            executorService.submit(() -> {
                testInitReferences(start, finalEnd, applicationConfig, metadataReportConfig, configCenterConfig);
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(100, TimeUnit.SECONDS);

        long t2 = System.currentTimeMillis();
        long cost = t2 - t1;
        System.out.println("Init large references cost: " + cost + "ms");
        Assertions.assertEquals(amount, DubboBootstrap.getInstance().getConfigManager().getReferences().size());
        Assertions.assertTrue(cost < 1000, "Init large references too slowly: " + cost);

        //test equals
        testSearchReferences();

    }

    private void testSearchReferences() {
        long t1 = System.currentTimeMillis();
        Collection<ReferenceConfigBase<?>> references = DubboBootstrap.getInstance().getConfigManager().getReferences();
        List<ReferenceConfigBase<?>> results = references.stream().filter(rc -> rc.equals(references.iterator().next()))
            .collect(Collectors.toList());
        long t2 = System.currentTimeMillis();
        long cost = t2 - t1;
        System.out.println("Search large references cost: " + cost + "ms");
        Assertions.assertEquals(1, results.size());
        Assertions.assertTrue(cost < 1000, "Search large references too slowly: " + cost);
    }

    private long testInitReferences(int start, int end, ApplicationConfig applicationConfig, MetadataReportConfig metadataReportConfig, ConfigCenterConfig configCenterConfig) {
        // test add large number of references
        long t1 = System.currentTimeMillis();
        try {
            for (int i = start; i < end; i++) {
                ReferenceConfig referenceConfig = new ReferenceConfig();
                referenceConfig.setInterface("com.test.TestService" + i);
                referenceConfig.setApplication(applicationConfig);
                referenceConfig.setMetadataReportConfig(metadataReportConfig);
                referenceConfig.setConfigCenter(configCenterConfig);
                DubboBootstrap.getInstance().reference(referenceConfig);

                //ApplicationModel.getConfigManager().getConfigCenters();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        long t2 = System.currentTimeMillis();
        return t2 - t1;
    }

    @Test
    public void testConstructWithReferenceAnnotation() throws NoSuchFieldException {
        Reference reference = getClass().getDeclaredField("innerTest").getAnnotation(Reference.class);
        ReferenceConfig referenceConfig = new ReferenceConfig(reference);
        Assertions.assertEquals(1, referenceConfig.getMethods().size());
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getName(), "sayHello");
        Assertions.assertEquals(1300, (int) (referenceConfig.getMethods().get(0)).getTimeout());
        Assertions.assertEquals(4, (int) (referenceConfig.getMethods().get(0)).getRetries());
        Assertions.assertEquals(( referenceConfig.getMethods().get(0)).getLoadbalance(), "random");
        Assertions.assertEquals(3, (int) (referenceConfig.getMethods().get(0)).getActives());
        Assertions.assertEquals(5, (int) (referenceConfig.getMethods().get(0)).getExecutes());
        Assertions.assertTrue((referenceConfig.getMethods().get(0)).isAsync());
        Assertions.assertEquals(( referenceConfig.getMethods().get(0)).getOninvokeMethod(), "i");
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getOnreturnMethod(), "r");
        Assertions.assertEquals(( referenceConfig.getMethods().get(0)).getOnthrowMethod(), "t");
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getCache(), "c");
    }


    @Reference(methods = {@Method(name = "sayHello", timeout = 1300, retries = 4, loadbalance = "random", async = true,
        actives = 3, executes = 5, deprecated = true, sticky = true, oninvoke = "instance.i", onthrow = "instance.t", onreturn = "instance.r", cache = "c", validation = "v",
        arguments = {@Argument(index = 24, callback = true, type = "sss")})})
    private InnerTest innerTest;

    private class InnerTest {

    }
}
