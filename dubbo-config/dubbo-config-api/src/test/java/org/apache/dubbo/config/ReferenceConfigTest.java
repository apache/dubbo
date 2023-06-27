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
import org.apache.dubbo.common.compiler.support.CtClassBuilder;
import org.apache.dubbo.common.compiler.support.JavassistCompiler;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.context.ModuleConfigManager;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.filter.FilterChainBuilder;
import org.apache.dubbo.rpc.cluster.support.registry.ZoneAwareClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.wrapper.MockClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.wrapper.ScopeClusterInvoker;
import org.apache.dubbo.rpc.listener.ListenerInvokerWrapper;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.ReferenceCountInvokerWrapper;
import org.apache.dubbo.rpc.protocol.injvm.InjvmInvoker;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;
import org.apache.dubbo.rpc.service.GenericService;

import demo.MultiClassLoaderService;
import demo.MultiClassLoaderServiceImpl;
import demo.MultiClassLoaderServiceRequest;
import demo.MultiClassLoaderServiceResult;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUMP_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.EXPORTER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LIVENESS_PROBE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.READINESS_PROBE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFER_ASYNC_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFER_BACKGROUND_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFER_THREAD_NUM_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_LOCAL_FILE_CACHE_ENABLED;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.STARTUP_PROBE;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.URL_MERGE_PROCESSOR_KEY;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.rpc.Constants.DEFAULT_STUB_EVENT;
import static org.apache.dubbo.rpc.Constants.LOCAL_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;
import static org.apache.dubbo.rpc.cluster.Constants.PEER_KEY;

class ReferenceConfigTest {
    private static String zkUrl1;
    private static String zkUrl2;
    private static String registryUrl1;

    @BeforeAll
    public static void beforeAll() {
        int zkServerPort1 = 2181;
        int zkServerPort2 = 2182;
        zkUrl1 = "zookeeper://localhost:" + zkServerPort1;
        zkUrl2 = "zookeeper://localhost:" + zkServerPort2;
        registryUrl1 = "registry://localhost:" + zkServerPort1 + "?registry=zookeeper";
    }

    @BeforeEach
    public void setUp() throws Exception {
        DubboBootstrap.reset();
        ApplicationModel.defaultModel().getApplicationConfigManager();
        DubboBootstrap.getInstance();
    }

    @AfterEach
    public void tearDown() throws IOException {
        DubboBootstrap.reset();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test whether the configuration required for the aggregation service reference meets expectations
     */
    @Test
    void testAppendConfig() {

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
        applicationConfig.setProtocol("dubbo");
        applicationConfig.setMetadataServicePort(88888);
        applicationConfig.setMetadataServiceProtocol("tri");
        applicationConfig.setLivenessProbe("livenessProbe");
        applicationConfig.setReadinessProbe("readinessProb");
        applicationConfig.setStartupProbe("startupProbe");

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

        MonitorConfig monitorConfig = new MonitorConfig();
        applicationConfig.setMonitor(monitorConfig);

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
        methodConfig.setName("sayName");
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

        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl1);
        applicationConfig.setRegistries(Collections.singletonList(registry));
        applicationConfig.setRegistryIds(registry.getId());
        moduleConfig.setRegistries(Collections.singletonList(registry));

        referenceConfig.setRegistry(registry);

        DubboBootstrap dubboBootstrap = DubboBootstrap.newInstance(FrameworkModel.defaultModel());
        dubboBootstrap.application(applicationConfig)
            .reference(referenceConfig)
            .registry(registry)
            .module(moduleConfig)
            .initialize();

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
        Assertions.assertEquals(applicationConfig.getMetadataServicePort().toString(),
            serviceMetadata.getAttachments().get(METADATA_SERVICE_PORT_KEY));
        Assertions.assertEquals(applicationConfig.getMetadataServiceProtocol().toString(),
            serviceMetadata.getAttachments().get(METADATA_SERVICE_PROTOCOL_KEY));
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
            serviceMetadata.getAttachments().get("sayName.stat"));
        Assertions.assertEquals(methodConfig.getRetries().toString(),
            serviceMetadata.getAttachments().get("sayName.retries"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.reliable"));
        Assertions.assertEquals(methodConfig.getExecutes().toString(),
            serviceMetadata.getAttachments().get("sayName.executes"));
        Assertions.assertEquals(methodConfig.getDeprecated().toString(),
            serviceMetadata.getAttachments().get("sayName.deprecated"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.stick"));
        Assertions.assertEquals(methodConfig.isReturn().toString(),
            serviceMetadata.getAttachments().get("sayName.return"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.service"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.service.id"));
        Assertions.assertFalse(serviceMetadata.getAttachments().containsKey("sayName.parent.prefix"));

        // verify additional revision and methods parameter
        Assertions.assertEquals(Version.getVersion(referenceConfig.getInterfaceClass(), referenceConfig.getVersion()),
            serviceMetadata.getAttachments().get(REVISION_KEY));
        Assertions.assertTrue(serviceMetadata.getAttachments().containsKey(METHODS_KEY));
        Assertions.assertEquals(DemoService.class.getMethods().length,
            StringUtils.split((String) serviceMetadata.getAttachments().get(METHODS_KEY), ',').length);

        dubboBootstrap.stop();
    }

    @Test
    void testCreateInvokerForLocalRefer() {

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setScope(LOCAL_KEY);

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);

        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.newInstance(FrameworkModel.defaultModel());
        dubboBootstrap.application(applicationConfig)
            .reference(referenceConfig)
            .initialize();

        referenceConfig.init();
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof ScopeClusterInvoker);
        ScopeClusterInvoker<?> scopeClusterInvoker = (ScopeClusterInvoker<?>) referenceConfig.getInvoker();
        Invoker<?> mockInvoker = scopeClusterInvoker.getInvoker();
        Assertions.assertTrue(mockInvoker instanceof MockClusterInvoker);
        Invoker<?> withCount = ((MockClusterInvoker<?>) mockInvoker).getDirectory().getAllInvokers().get(0);

        Assertions.assertTrue(withCount instanceof ReferenceCountInvokerWrapper);
        Invoker<?> withFilter = ((ReferenceCountInvokerWrapper<?>) withCount).getInvoker();
        Assertions.assertTrue(withFilter instanceof ListenerInvokerWrapper
            || withFilter instanceof FilterChainBuilder.CallbackRegistrationInvoker);
        if (withFilter instanceof ListenerInvokerWrapper) {
            Assertions.assertTrue(((ListenerInvokerWrapper<?>) (((ReferenceCountInvokerWrapper<?>) withCount).getInvoker())).getInvoker() instanceof InjvmInvoker);
        }
        if (withFilter instanceof FilterChainBuilder.CallbackRegistrationInvoker) {
            Invoker filterInvoker = ((FilterChainBuilder.CallbackRegistrationInvoker) withFilter).getFilterInvoker();
            FilterChainBuilder.CopyOfFilterChainNode filterInvoker1 = (FilterChainBuilder.CopyOfFilterChainNode) filterInvoker;
            ListenerInvokerWrapper originalInvoker = (ListenerInvokerWrapper) filterInvoker1.getOriginalInvoker();
            Invoker invoker = originalInvoker.getInvoker();
            Assertions.assertTrue(invoker instanceof InjvmInvoker);
        }
        URL url = withFilter.getUrl();
        Assertions.assertEquals("application1", url.getParameter("application"));
        Assertions.assertEquals("value1", url.getParameter("key1"));
        Assertions.assertEquals("value2", url.getParameter("key2"));

        dubboBootstrap.stop();
    }

    /**
     * Verify the configuration of the registry protocol for remote reference
     */
    @Test
    void testCreateInvokerForRemoteRefer() {

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setGeneric(Boolean.FALSE.toString());
        referenceConfig.setProtocol("dubbo");
        referenceConfig.setInit(true);
        referenceConfig.setLazy(false);
        referenceConfig.setInjvm(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.newInstance(FrameworkModel.defaultModel());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);

        referenceConfig.refreshed.set(true);
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl1);
        applicationConfig.setRegistries(Collections.singletonList(registry));
        applicationConfig.setRegistryIds(registry.getId());

        referenceConfig.setRegistry(registry);

        dubboBootstrap
            .application(applicationConfig)
            .reference(referenceConfig)
            .initialize();

        referenceConfig.init();
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof MigrationInvoker);

        dubboBootstrap.destroy();
    }


    /**
     * Verify that the remote url is directly configured for remote reference
     */
    @Test
    void testCreateInvokerWithRemoteUrlForRemoteRefer() {

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setGeneric(Boolean.FALSE.toString());
        referenceConfig.setProtocol("dubbo");
        referenceConfig.setInit(true);
        referenceConfig.setLazy(false);
        referenceConfig.setInjvm(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.newInstance(FrameworkModel.defaultModel());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);

        referenceConfig.refreshed.set(true);
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);

        referenceConfig.setUrl("dubbo://127.0.0.1:20880");

        dubboBootstrap
            .application(applicationConfig)
            .reference(referenceConfig)
            .initialize();

        referenceConfig.init();
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof ScopeClusterInvoker);
        Invoker scopeClusterInvoker = referenceConfig.getInvoker();
        Assertions.assertTrue(((ScopeClusterInvoker) scopeClusterInvoker).getInvoker() instanceof MockClusterInvoker);
        Assertions.assertEquals(Boolean.TRUE, ((ScopeClusterInvoker) scopeClusterInvoker).getInvoker().getUrl().getAttribute(PEER_KEY));
        dubboBootstrap.destroy();

    }

    /**
     * Verify that the registry url is directly configured for remote reference
     */
    @Test
    void testCreateInvokerWithRegistryUrlForRemoteRefer() {

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setGeneric(Boolean.FALSE.toString());
        referenceConfig.setProtocol("dubbo");
        referenceConfig.setInit(true);
        referenceConfig.setLazy(false);
        referenceConfig.setInjvm(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.newInstance(FrameworkModel.defaultModel());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);

        referenceConfig.refreshed.set(true);
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);

        referenceConfig.setUrl(registryUrl1);

        dubboBootstrap
            .application(applicationConfig)
            .reference(referenceConfig)
            .initialize();

        referenceConfig.init();
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof MigrationInvoker);
        dubboBootstrap.destroy();

    }

    /**
     * Verify the service reference of multiple registries
     */
    @Test
    void testMultipleRegistryForRemoteRefer() {
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setGeneric(Boolean.FALSE.toString());
        referenceConfig.setProtocol("dubbo");
        referenceConfig.setInit(true);
        referenceConfig.setLazy(false);
        referenceConfig.setInjvm(false);

        DubboBootstrap dubboBootstrap = DubboBootstrap.newInstance(FrameworkModel.defaultModel());

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        applicationConfig.setParameters(parameters);

        referenceConfig.refreshed.set(true);
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.getInterfaceClass();
        referenceConfig.setCheck(false);
        RegistryConfig registry1 = new RegistryConfig();
        registry1.setAddress(zkUrl1);
        registry1.setId("zk1");

        RegistryConfig registry2 = new RegistryConfig();
        registry2.setAddress(zkUrl2);
        registry2.setId("zk2");

        List<RegistryConfig> registryConfigs = new ArrayList<>();
        registryConfigs.add(registry1);
        registryConfigs.add(registry2);
        applicationConfig.setRegistries(registryConfigs);
        applicationConfig.setRegistryIds("zk1,zk2");

        referenceConfig.setRegistries(registryConfigs);

        dubboBootstrap
            .application(applicationConfig)
            .reference(referenceConfig)
            .initialize();

        referenceConfig.init();
        Assertions.assertTrue(referenceConfig.getInvoker() instanceof ZoneAwareClusterInvoker);

        dubboBootstrap.destroy();
    }

    @Test
    @Disabled("Disabled due to Github Actions environment")
    public void testInjvm() throws Exception {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("test-protocol-random-port");
        application.setEnableFileCache(false);
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(application);

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl1);

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
    void test1ReferenceRetry() {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("test-reference-retry");
        application.setEnableFileCache(false);
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(application);

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl1);

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
            URL url = URL.valueOf("injvm://127.0.0.1/DemoService")
                .addParameter(INTERFACE_KEY, DemoService.class.getName()).setScopeModel(ApplicationModel.defaultModel().getDefaultModule());
            url = url.addParameter(EXPORTER_LISTENER_KEY, LOCAL_PROTOCOL);
            Protocol protocolSPI = ApplicationModel.defaultModel().getExtensionLoader(Protocol.class).getAdaptiveExtension();
            protocolSPI.export(proxy.getInvoker(service, DemoService.class, url));
            demoService = rc.get();
            success = true;
        } catch (Exception e) {
            // ignore
        } finally {
            rc.destroy();
            InjvmProtocol.getInjvmProtocol(FrameworkModel.defaultModel()).destroy();
            System.clearProperty("java.net.preferIPv4Stack");

        }
        Assertions.assertTrue(success);
        Assertions.assertNotNull(demoService);

    }

    @Test
    void test2ReferenceRetry() {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("test-reference-retry2");
        application.setEnableFileCache(false);
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(application);

        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(zkUrl1);
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("mockprotocol");

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

        ServiceConfig<DemoService> sc = new ServiceConfig<>();
        sc.setInterface(DemoService.class.getName());
        sc.setRef(new DemoServiceImpl());
        sc.setRegistry(registry);
        sc.setProtocol(protocol);

        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            sc.export();
            demoService = rc.get();
            success = true;
        } catch (Exception e) {
            // ignore
        } finally {
            rc.destroy();
            sc.unexport();
            System.clearProperty("java.net.preferIPv4Stack");

        }
        Assertions.assertTrue(success);
        Assertions.assertNotNull(demoService);

    }

    @Test
    void testMetaData() {
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
        Assertions.assertEquals(String.valueOf(consumerConfig.getActives()), metaData.get("actives"));
        Assertions.assertEquals(String.valueOf(config.isAsync()), metaData.get("async"));

    }

    @Test
    void testGetPrefixes() {

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
    void testGenericAndInterfaceConflicts() {

        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setGeneric("true");

        DubboBootstrap.getInstance()
            .application("demo app")
            .reference(referenceConfig)
            .initialize();

        Assertions.assertEquals(GenericService.class, referenceConfig.getInterfaceClass());
    }


    @Test
    void testLargeReferences() throws InterruptedException {
        int amount = 10000;
        ModuleConfigManager configManager = DubboBootstrap.getInstance().getApplicationModel().getDefaultModule().getConfigManager();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("test-app");
        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
        metadataReportConfig.setAddress("metadata://");
        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();
        configCenterConfig.setAddress("diamond://");

        testInitReferences(0, amount, applicationConfig, metadataReportConfig, configCenterConfig);
        configManager.clear();
        testInitReferences(0, 1, applicationConfig, metadataReportConfig, configCenterConfig);
        configManager.clear();

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
        Assertions.assertEquals(amount, configManager.getReferences().size());
        Assertions.assertTrue(cost < 1000, "Init large references too slowly: " + cost);

        //test equals
        testSearchReferences();

    }

    private void testSearchReferences() {
        long t1 = System.currentTimeMillis();
        Collection<ReferenceConfigBase<?>> references = DubboBootstrap.getInstance().getApplicationModel().getDefaultModule().getConfigManager().getReferences();
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

                //ApplicationModel.defaultModel().getConfigManager().getConfigCenters();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        long t2 = System.currentTimeMillis();
        return t2 - t1;
    }

    @Test
    void testConstructWithReferenceAnnotation() throws NoSuchFieldException {
        Reference reference = getClass().getDeclaredField("innerTest").getAnnotation(Reference.class);
        ReferenceConfig referenceConfig = new ReferenceConfig(reference);
        Assertions.assertEquals(1, referenceConfig.getMethods().size());
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getName(), "sayHello");
        Assertions.assertEquals(1300, (int) (referenceConfig.getMethods().get(0)).getTimeout());
        Assertions.assertEquals(4, (int) (referenceConfig.getMethods().get(0)).getRetries());
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getLoadbalance(), "random");
        Assertions.assertEquals(3, (int) (referenceConfig.getMethods().get(0)).getActives());
        Assertions.assertEquals(5, (int) (referenceConfig.getMethods().get(0)).getExecutes());
        Assertions.assertTrue((referenceConfig.getMethods().get(0)).isAsync());
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getOninvokeMethod(), "i");
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getOnreturnMethod(), "r");
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getOnthrowMethod(), "t");
        Assertions.assertEquals((referenceConfig.getMethods().get(0)).getCache(), "c");
    }

    @Test
    void testDifferentClassLoader() throws Exception {
        ApplicationConfig applicationConfig = new ApplicationConfig("TestApp");
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ModuleModel moduleModel = applicationModel.newModule();

        DemoService demoService = new DemoServiceImpl();
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRegistry(new RegistryConfig(zkUrl1));
        serviceConfig.setScopeModel(moduleModel);
        serviceConfig.setRef(demoService);
        serviceConfig.export();

        String basePath = DemoService.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        basePath = URLDecoder.decode(basePath, "UTF-8");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        TestClassLoader classLoader1 = new TestClassLoader(classLoader, basePath);
        TestClassLoader classLoader2 = new TestClassLoader(classLoader, basePath);

        Class<?> class1 = classLoader1.loadClass(DemoService.class.getName(), false);
        Class<?> class2 = classLoader2.loadClass(DemoService.class.getName(), false);

        Assertions.assertNotEquals(class1, class2);

        ReferenceConfig<DemoService> referenceConfig1 = new ReferenceConfig<>();
        referenceConfig1.setInterface(class1);
        referenceConfig1.setRegistry(new RegistryConfig(zkUrl1));
        referenceConfig1.setScopeModel(moduleModel);
        referenceConfig1.setScope("remote");
        Object demoService1 = referenceConfig1.get();

        for (Class<?> anInterface : demoService1.getClass().getInterfaces()) {
            Assertions.assertNotEquals(DemoService.class, anInterface);
        }
        Assertions.assertTrue(Arrays.stream(demoService1.getClass().getInterfaces()).anyMatch((clazz) -> clazz.getClassLoader().equals(classLoader1)));

        java.lang.reflect.Method callBean1 = demoService1.getClass().getDeclaredMethod("callInnerClass");
        callBean1.setAccessible(true);
        Object result1 = callBean1.invoke(demoService1);

        Assertions.assertNotEquals(result1.getClass(), DemoService.InnerClass.class);
        Assertions.assertEquals(classLoader1, result1.getClass().getClassLoader());

        ReferenceConfig<DemoService> referenceConfig2 = new ReferenceConfig<>();
        referenceConfig2.setInterface(class2);
        referenceConfig2.setRegistry(new RegistryConfig(zkUrl1));
        referenceConfig2.setScopeModel(moduleModel);
        referenceConfig2.setScope("remote");
        Object demoService2 = referenceConfig2.get();

        for (Class<?> anInterface : demoService2.getClass().getInterfaces()) {
            Assertions.assertNotEquals(DemoService.class, anInterface);
        }
        Assertions.assertTrue(Arrays.stream(demoService2.getClass().getInterfaces()).anyMatch((clazz) -> clazz.getClassLoader().equals(classLoader2)));

        java.lang.reflect.Method callBean2 = demoService2.getClass().getDeclaredMethod("callInnerClass");
        callBean2.setAccessible(true);
        Object result2 = callBean2.invoke(demoService2);

        Assertions.assertNotEquals(callBean1, callBean2);
        Assertions.assertNotEquals(result2.getClass(), DemoService.InnerClass.class);
        Assertions.assertEquals(classLoader2, result2.getClass().getClassLoader());
        Assertions.assertNotEquals(result1.getClass(), result2.getClass());

        applicationModel.destroy();
        DubboBootstrap.getInstance().destroy();
        Thread.currentThread().setContextClassLoader(classLoader);
        Thread.currentThread().getContextClassLoader().loadClass(DemoService.class.getName());
    }

    @Test
    @DisabledForJreRange(min = JRE.JAVA_16)
    public void testDifferentClassLoaderRequest() throws Exception {
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        String basePath = DemoService.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        basePath = java.net.URLDecoder.decode(basePath, "UTF-8");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        TestClassLoader1 classLoader1 = new TestClassLoader1(basePath);
        TestClassLoader1 classLoader2 = new TestClassLoader1(basePath);
        TestClassLoader2 classLoader3 = new TestClassLoader2(classLoader2, basePath);

        ApplicationConfig applicationConfig = new ApplicationConfig("TestApp");
        ApplicationModel applicationModel = frameworkModel.newApplication();
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ModuleModel moduleModel = applicationModel.newModule();

        Class<?> clazz1 = classLoader1.loadClass(MultiClassLoaderService.class.getName(), false);
        Class<?> clazz1impl = classLoader1.loadClass(MultiClassLoaderServiceImpl.class.getName(), false);
        Class<?> requestClazzCustom1 = compileCustomRequest(classLoader1);
        Class<?> resultClazzCustom1 = compileCustomResult(classLoader1);
        classLoader1.loadedClass.put(requestClazzCustom1.getName(), requestClazzCustom1);
        classLoader1.loadedClass.put(resultClazzCustom1.getName(), resultClazzCustom1);
        AtomicReference innerRequestReference = new AtomicReference();
        AtomicReference innerResultReference = new AtomicReference();
        innerResultReference.set(resultClazzCustom1.getDeclaredConstructor().newInstance());
        Constructor<?> declaredConstructor = clazz1impl.getDeclaredConstructor(AtomicReference.class, AtomicReference.class);

        ServiceConfig serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterfaceClassLoader(classLoader1);
        serviceConfig.setInterface(clazz1);
        serviceConfig.setRegistry(new RegistryConfig(zkUrl1));
        serviceConfig.setScopeModel(moduleModel);
        serviceConfig.setRef(declaredConstructor.newInstance(innerRequestReference, innerResultReference));
        serviceConfig.export();

        Class<?> clazz2 = classLoader2.loadClass(MultiClassLoaderService.class.getName(), false);
        Class<?> requestClazzOrigin = classLoader2.loadClass(MultiClassLoaderServiceRequest.class.getName(), false);
        Class<?> requestClazzCustom2 = compileCustomRequest(classLoader2);
        Class<?> resultClazzCustom3 = compileCustomResult(classLoader3);
        classLoader2.loadedClass.put(requestClazzCustom2.getName(), requestClazzCustom2);
        classLoader3.loadedClass.put(resultClazzCustom3.getName(), resultClazzCustom3);

        ReferenceConfig<DemoService> referenceConfig1 = new ReferenceConfig<>();
        referenceConfig1.setInterface(clazz2);
        referenceConfig1.setInterfaceClassLoader(classLoader3);
        referenceConfig1.setRegistry(new RegistryConfig(zkUrl1));
        referenceConfig1.setScopeModel(moduleModel);
        referenceConfig1.setScope("remote");
        Object object1 = referenceConfig1.get();

        java.lang.reflect.Method callBean1 = object1.getClass().getDeclaredMethod("call", requestClazzOrigin);
        callBean1.setAccessible(true);
        Object result1 = callBean1.invoke(object1, requestClazzCustom2.getDeclaredConstructor().newInstance());

        Assertions.assertEquals(resultClazzCustom3, result1.getClass());
        Assertions.assertNotEquals(classLoader2, result1.getClass().getClassLoader());
        Assertions.assertEquals(classLoader1, innerRequestReference.get().getClass().getClassLoader());

        Thread.currentThread().setContextClassLoader(classLoader1);
        callBean1.invoke(object1, requestClazzCustom2.getDeclaredConstructor().newInstance());
        Assertions.assertEquals(classLoader1, Thread.currentThread().getContextClassLoader());

        applicationModel.destroy();
        DubboBootstrap.getInstance().destroy();
        Thread.currentThread().setContextClassLoader(classLoader);
        Thread.currentThread().getContextClassLoader().loadClass(DemoService.class.getName());
    }

    @Test
    void testClassLoader() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("Test"));

        ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = new ClassLoader(originClassLoader) {
        };
        Thread.currentThread().setContextClassLoader(classLoader);

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>(applicationModel.newModule());
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setProtocol(new ProtocolConfig("dubbo", -1));
        serviceConfig.setRegistry(new RegistryConfig("N/A"));
        serviceConfig.setRef(new DemoServiceImpl());
        serviceConfig.export();

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>(applicationModel.newModule());
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setRegistry(new RegistryConfig("N/A"));
        DemoService demoService = referenceConfig.get();

        demoService.sayName("Dubbo");
        Assertions.assertEquals(classLoader, Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(null);
        demoService.sayName("Dubbo");
        Assertions.assertNull(Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(originClassLoader);
        frameworkModel.destroy();
    }

    private Class<?> compileCustomRequest(ClassLoader classLoader) throws NotFoundException, CannotCompileException {
        CtClassBuilder builder = new CtClassBuilder();
        builder.setClassName(MultiClassLoaderServiceRequest.class.getName() + "A");
        builder.setSuperClassName(MultiClassLoaderServiceRequest.class.getName());
        CtClass cls = builder.build(classLoader);
        // FIXME support JDK 17
        return cls.toClass(classLoader, JavassistCompiler.class.getProtectionDomain());
    }

    private Class<?> compileCustomResult(ClassLoader classLoader) throws NotFoundException, CannotCompileException {
        CtClassBuilder builder = new CtClassBuilder();
        builder.setClassName(MultiClassLoaderServiceResult.class.getName() + "A");
        builder.setSuperClassName(MultiClassLoaderServiceResult.class.getName());
        CtClass cls = builder.build(classLoader);
        return cls.toClass(classLoader, JavassistCompiler.class.getProtectionDomain());
    }

    @Reference(methods = {@Method(name = "sayHello", timeout = 1300, retries = 4, loadbalance = "random", async = true,
        actives = 3, executes = 5, deprecated = true, sticky = true, oninvoke = "instance.i", onthrow = "instance.t", onreturn = "instance.r", cache = "c", validation = "v",
        arguments = {@Argument(index = 24, callback = true, type = "sss")})})
    private InnerTest innerTest;

    private class InnerTest {

    }

    private static class TestClassLoader extends ClassLoader {
        private String basePath;

        public TestClassLoader(ClassLoader parent, String basePath) {
            super(parent);
            this.basePath = basePath;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] bytes = loadClassData(name);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (Exception e) {
                throw new ClassNotFoundException();
            }
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> loadedClass = this.findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            } else {
                try {
                    if (name.equals("org.apache.dubbo.config.api.DemoService") || name.equals("org.apache.dubbo.config.api.DemoService$InnerClass")) {
                        Class<?> aClass = this.findClass(name);
                        if (resolve) {
                            this.resolveClass(aClass);
                        }
                        return aClass;
                    } else {
                        return super.loadClass(name, resolve);
                    }
                } catch (Exception e) {
                    return super.loadClass(name, resolve);
                }
            }
        }


        public byte[] loadClassData(String className) throws IOException {
            className = className.replaceAll("\\.", "/");
            String path = basePath + File.separator + className + ".class";
            FileInputStream fileInputStream;
            byte[] classBytes;
            fileInputStream = new FileInputStream(path);
            int length = fileInputStream.available();
            classBytes = new byte[length];
            fileInputStream.read(classBytes);
            fileInputStream.close();
            return classBytes;
        }
    }

    private static class TestClassLoader1 extends ClassLoader {
        private String basePath;

        public TestClassLoader1(String basePath) {
            this.basePath = basePath;
        }

        Map<String, Class<?>> loadedClass = new ConcurrentHashMap<>();

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] bytes = loadClassData(name);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (Exception e) {
                throw new ClassNotFoundException();
            }
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (loadedClass.containsKey(name)) {
                return loadedClass.get(name);
            }
            if (name.startsWith("demo")) {
                Class<?> aClass = this.findClass(name);
                this.loadedClass.put(name, aClass);
                if (resolve) {
                    this.resolveClass(aClass);
                }
                return aClass;
            } else {
                Class<?> loadedClass = this.findLoadedClass(name);
                if (loadedClass != null) {
                    return loadedClass;
                } else {
                    return super.loadClass(name, resolve);
                }
            }
        }


        public byte[] loadClassData(String className) throws IOException {
            className = className.replaceAll("\\.", "/");
            String path = basePath + File.separator + className + ".class";
            FileInputStream fileInputStream;
            byte[] classBytes;
            fileInputStream = new FileInputStream(path);
            int length = fileInputStream.available();
            classBytes = new byte[length];
            fileInputStream.read(classBytes);
            fileInputStream.close();
            return classBytes;
        }
    }

    private static class TestClassLoader2 extends ClassLoader {
        private String basePath;
        private TestClassLoader1 testClassLoader;

        Map<String, Class<?>> loadedClass = new ConcurrentHashMap<>();

        public TestClassLoader2(TestClassLoader1 testClassLoader, String basePath) {
            this.testClassLoader = testClassLoader;
            this.basePath = basePath;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                byte[] bytes = loadClassData(name);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (Exception e) {
                throw new ClassNotFoundException();
            }
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (loadedClass.containsKey(name)) {
                return loadedClass.get(name);
            }
            if (name.startsWith("demo.MultiClassLoaderServiceRe")) {
                Class<?> aClass = this.findClass(name);
                this.loadedClass.put(name, aClass);
                if (resolve) {
                    this.resolveClass(aClass);
                }
                return aClass;
            } else {
                return testClassLoader.loadClass(name, resolve);
            }
        }


        public byte[] loadClassData(String className) throws IOException {
            className = className.replaceAll("\\.", "/");
            String path = basePath + File.separator + className + ".class";
            FileInputStream fileInputStream;
            byte[] classBytes;
            fileInputStream = new FileInputStream(path);
            int length = fileInputStream.available();
            classBytes = new byte[length];
            fileInputStream.read(classBytes);
            fileInputStream.close();
            return classBytes;
        }
    }

}
