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
package org.apache.dubbo.registry.integration;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.CompositeConfiguration;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.registry.client.migration.MigrationRuleListener;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.support.FailoverCluster;
import org.apache.dubbo.rpc.cluster.support.MergeableCluster;
import org.apache.dubbo.rpc.cluster.support.wrapper.MockClusterWrapper;
import org.apache.dubbo.rpc.cluster.support.wrapper.ScopeClusterWrapper;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_PROTOCOL_LISTENER_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.registry.Constants.ENABLE_CONFIGURATION_LISTEN;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.remoting.Constants.CHECK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CONSUMER_URL_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistryProtocolTest {

    @AfterEach
    public void tearDown() throws IOException {
        Mockito.framework().clearInlineMocks();
        ApplicationModel.defaultModel().destroy();
    }

    /**
     * verify the generated consumer url information
     */
    @Test
    void testConsumerUrlWithoutProtocol() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "false");
        parameters.put(REGISTER_IP_KEY, "172.23.236.180");

        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURL = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        attributes.put(REFER_KEY, refer);
        attributes.put("key1", "value1");
        URL url = serviceConfigURL.addAttributes(attributes);

        RegistryFactory registryFactory = mock(RegistryFactory.class);
        Registry registry = mock(Registry.class);

        RegistryProtocol registryProtocol = new RegistryProtocol();

        MigrationRuleListener migrationRuleListener = mock(MigrationRuleListener.class);
        List<RegistryProtocolListener> registryProtocolListeners = new ArrayList<>();
        registryProtocolListeners.add(migrationRuleListener);

        ModuleModel moduleModel = Mockito.spy(ApplicationModel.defaultModel().getDefaultModule());
        moduleModel.getApplicationModel().getApplicationConfigManager().setApplication(new ApplicationConfig("application1"));
        ExtensionLoader<RegistryProtocolListener> extensionLoaderMock = mock(ExtensionLoader.class);
        Mockito.when(moduleModel.getExtensionLoader(RegistryProtocolListener.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY))
            .thenReturn(registryProtocolListeners);
        url = url.setScopeModel(moduleModel);

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Cluster cluster = mock(Cluster.class);

        Invoker<?> invoker = registryProtocol.doRefer(cluster, registry, DemoService.class, url, parameters);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);

        URL consumerUrl = ((MigrationInvoker<?>) invoker).getConsumerUrl();
        Assertions.assertTrue((consumerUrl != null));

        // verify that the protocol header of consumerUrl is set to "consumer"
        Assertions.assertEquals("consumer", consumerUrl.getProtocol());
        Assertions.assertEquals(parameters.get(REGISTER_IP_KEY), consumerUrl.getHost());
        Assertions.assertFalse(consumerUrl.getAttributes().containsKey(REFER_KEY));
        Assertions.assertEquals("value1", consumerUrl.getAttribute("key1"));
    }

    /**
     * verify that when the protocol is configured, the protocol of consumer url is the configured protocol
     */
    @Test
    void testConsumerUrlWithProtocol() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "false");
        parameters.put(REGISTER_IP_KEY, "172.23.236.180");
        parameters.put(PROTOCOL_KEY, "tri");
        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURL = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        attributes.put(REFER_KEY, refer);
        attributes.put("key1", "value1");
        URL url = serviceConfigURL.addAttributes(attributes);

        RegistryFactory registryFactory = mock(RegistryFactory.class);

        RegistryProtocol registryProtocol = new RegistryProtocol();
        Registry registry = mock(Registry.class);

        MigrationRuleListener migrationRuleListener = mock(MigrationRuleListener.class);
        List<RegistryProtocolListener> registryProtocolListeners = new ArrayList<>();
        registryProtocolListeners.add(migrationRuleListener);

        ModuleModel moduleModel = Mockito.spy(ApplicationModel.defaultModel().getDefaultModule());
        moduleModel.getApplicationModel().getApplicationConfigManager().setApplication(new ApplicationConfig("application1"));
        ExtensionLoader<RegistryProtocolListener> extensionLoaderMock = mock(ExtensionLoader.class);
        Mockito.when(moduleModel.getExtensionLoader(RegistryProtocolListener.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY))
            .thenReturn(registryProtocolListeners);
        url = url.setScopeModel(moduleModel);

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Cluster cluster = mock(Cluster.class);

        Invoker<?> invoker = registryProtocol.doRefer(cluster, registry, DemoService.class, url, parameters);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);

        URL consumerUrl = ((MigrationInvoker<?>) invoker).getConsumerUrl();
        Assertions.assertTrue((consumerUrl != null));

        // verify that the protocol of consumer url
        Assertions.assertEquals("tri", consumerUrl.getProtocol());
        Assertions.assertEquals(parameters.get(REGISTER_IP_KEY), consumerUrl.getHost());
        Assertions.assertFalse(consumerUrl.getAttributes().containsKey(REFER_KEY));
        Assertions.assertEquals("value1", consumerUrl.getAttribute("key1"));

    }

    /**
     * verify that if multiple groups are not configured, the service reference of the registration center
     * the default is FailoverCluster
     *
     * @see FailoverCluster
     */
    @Test
    void testReferWithoutGroup() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");


        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "false");

        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURL = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        attributes.put(REFER_KEY, refer);
        URL url = serviceConfigURL.addAttributes(attributes);

        RegistryFactory registryFactory = mock(RegistryFactory.class);
        Registry registry = mock(Registry.class);

        MigrationRuleListener migrationRuleListener = mock(MigrationRuleListener.class);
        List<RegistryProtocolListener> registryProtocolListeners = new ArrayList<>();
        registryProtocolListeners.add(migrationRuleListener);

        RegistryProtocol registryProtocol = new RegistryProtocol();
        ModuleModel moduleModel = Mockito.spy(ApplicationModel.defaultModel().getDefaultModule());
        moduleModel.getApplicationModel().getApplicationConfigManager().setApplication(new ApplicationConfig("application1"));
        ExtensionLoader extensionLoaderMock = mock(ExtensionLoader.class);
        Mockito.when(moduleModel.getExtensionLoader(RegistryProtocolListener.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY))
            .thenReturn(registryProtocolListeners);
        Mockito.when(moduleModel.getExtensionLoader(RegistryFactory.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getAdaptiveExtension()).thenReturn(registryFactory);
        url = url.setScopeModel(moduleModel);

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Invoker<?> invoker = registryProtocol.refer(DemoService.class, url);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);
        Assertions.assertTrue(((MigrationInvoker<?>) invoker).getCluster() instanceof ScopeClusterWrapper);
        Assertions.assertTrue(((ScopeClusterWrapper) ((MigrationInvoker<?>) invoker).getCluster()).getCluster() instanceof MockClusterWrapper);
        Assertions.assertTrue(
            ((MockClusterWrapper) ((ScopeClusterWrapper) ((MigrationInvoker<?>) invoker).getCluster()).getCluster()).getCluster() instanceof FailoverCluster);
    }

    /**
     * verify that if multiple groups are configured, the service reference of the registration center
     *
     * @see MergeableCluster
     */
    @Test
    void testReferWithGroup() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "false");

        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURL = new ServiceConfigURL(
            "registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        refer.put(GROUP_KEY, "group1,group2");
        attributes.put(REFER_KEY, refer);
        URL url = serviceConfigURL.addAttributes(attributes);

        RegistryFactory registryFactory = mock(RegistryFactory.class);
        Registry registry = mock(Registry.class);

        MigrationRuleListener migrationRuleListener = mock(MigrationRuleListener.class);
        List<RegistryProtocolListener> registryProtocolListeners = new ArrayList<>();
        registryProtocolListeners.add(migrationRuleListener);

        RegistryProtocol registryProtocol = new RegistryProtocol();
        ModuleModel moduleModel = Mockito.spy(ApplicationModel.defaultModel().getDefaultModule());
        moduleModel.getApplicationModel().getApplicationConfigManager().setApplication(new ApplicationConfig("application1"));
        ExtensionLoader extensionLoaderMock = mock(ExtensionLoader.class);
        Mockito.when(moduleModel.getExtensionLoader(RegistryProtocolListener.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY))
            .thenReturn(registryProtocolListeners);
        Mockito.when(moduleModel.getExtensionLoader(RegistryFactory.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getAdaptiveExtension()).thenReturn(registryFactory);
        url = url.setScopeModel(moduleModel);

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Invoker<?> invoker = registryProtocol.refer(DemoService.class, url);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);

        Assertions.assertTrue(((MigrationInvoker<?>) invoker).getCluster() instanceof ScopeClusterWrapper);
        Assertions.assertTrue(((ScopeClusterWrapper) ((MigrationInvoker<?>) invoker).getCluster()).getCluster() instanceof MockClusterWrapper);

        Assertions.assertTrue(
            ((MockClusterWrapper) ((ScopeClusterWrapper) ((MigrationInvoker<?>) invoker).getCluster()).getCluster()).getCluster() instanceof MergeableCluster);

    }

    /**
     * verify that the default RegistryProtocolListener will be executed
     *
     * @see MigrationRuleListener
     */
    @Test
    void testInterceptInvokerForMigrationRuleListener() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "false");

        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURL = new ServiceConfigURL(
            "registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        refer.put(GROUP_KEY, "group1,group2");
        attributes.put(REFER_KEY, refer);
        URL url = serviceConfigURL.addAttributes(attributes);

        MigrationInvoker<?> clusterInvoker = mock(MigrationInvoker.class);

        Map<String, Object> consumerAttribute = new HashMap<>(url.getAttributes());
        consumerAttribute.remove(REFER_KEY);
        URL consumerUrl = new ServiceConfigURL(parameters.get(PROTOCOL_KEY) == null ? DUBBO : parameters.get(PROTOCOL_KEY),
            null,
            null,
            parameters.get(REGISTER_IP_KEY),
            0, url.getPath(),
            parameters,
            consumerAttribute);
        url = url.putAttribute(CONSUMER_URL_KEY, consumerUrl);
        MigrationRuleListener migrationRuleListener = mock(MigrationRuleListener.class);
        List<RegistryProtocolListener> registryProtocolListeners = new ArrayList<>();
        registryProtocolListeners.add(migrationRuleListener);

        RegistryProtocol registryProtocol = new RegistryProtocol();
        ModuleModel moduleModel = Mockito.spy(ApplicationModel.defaultModel().getDefaultModule());
        moduleModel.getApplicationModel().getApplicationConfigManager().setApplication(new ApplicationConfig("application1"));
        ExtensionLoader<RegistryProtocolListener> extensionLoaderMock = mock(ExtensionLoader.class);
        Mockito.when(moduleModel.getExtensionLoader(RegistryProtocolListener.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY))
            .thenReturn(registryProtocolListeners);
        url = url.setScopeModel(moduleModel);

        registryProtocol.interceptInvoker(clusterInvoker, url, consumerUrl);
        verify(migrationRuleListener, times(1)).onRefer(registryProtocol, clusterInvoker, consumerUrl, url);
    }


    /**
     * Verify that if registry.protocol.listener is configured,
     * whether the corresponding RegistryProtocolListener will be executed normally
     *
     * @see CountRegistryProtocolListener
     */
    @Test
    void testInterceptInvokerForCustomRegistryProtocolListener() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "false");
        parameters.put(REGISTRY_PROTOCOL_LISTENER_KEY, "count");

        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURL = new ServiceConfigURL(
            "registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        refer.put(GROUP_KEY, "group1,group2");
        attributes.put(REFER_KEY, refer);
        URL url = serviceConfigURL.addAttributes(attributes);

        RegistryProtocol registryProtocol = new RegistryProtocol();
        MigrationInvoker<?> clusterInvoker = mock(MigrationInvoker.class);

        Map<String, Object> consumerAttribute = new HashMap<>(url.getAttributes());
        consumerAttribute.remove(REFER_KEY);
        URL consumerUrl = new ServiceConfigURL(parameters.get(PROTOCOL_KEY) == null ? DUBBO : parameters.get(PROTOCOL_KEY),
            null,
            null,
            parameters.get(REGISTER_IP_KEY),
            0, url.getPath(),
            parameters,
            consumerAttribute);
        url = url.putAttribute(CONSUMER_URL_KEY, consumerUrl);

        List<RegistryProtocolListener> registryProtocolListeners = new ArrayList<>();
        registryProtocolListeners.add(new CountRegistryProtocolListener());

        ModuleModel moduleModel = Mockito.spy(ApplicationModel.defaultModel().getDefaultModule());
        moduleModel.getApplicationModel().getApplicationConfigManager().setApplication(new ApplicationConfig("application1"));
        ExtensionLoader<RegistryProtocolListener> extensionLoaderMock = mock(ExtensionLoader.class);
        Mockito.when(moduleModel.getExtensionLoader(RegistryProtocolListener.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getActivateExtension(url, REGISTRY_PROTOCOL_LISTENER_KEY))
            .thenReturn(registryProtocolListeners);
        url = url.setScopeModel(moduleModel);

        registryProtocol.interceptInvoker(clusterInvoker, url, consumerUrl);

        Assertions.assertEquals(1, CountRegistryProtocolListener.getReferCounter().get());
    }

    /**
     * verify the registered consumer url
     */
    @Test
    void testRegisterConsumerUrl() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "true");
        parameters.put(REGISTER_IP_KEY, "172.23.236.180");

        Map<String, Object> attributes = new HashMap<>();
        ServiceConfigURL serviceConfigURL = new ServiceConfigURL("registry",
            "127.0.0.1",
            2181,
            "org.apache.dubbo.registry.RegistryService",
            parameters);
        Map<String, String> refer = new HashMap<>();
        attributes.put(REFER_KEY, refer);
        attributes.put("key1", "value1");
        URL url = serviceConfigURL.addAttributes(attributes);

        RegistryFactory registryFactory = mock(RegistryFactory.class);
        Registry registry = mock(Registry.class);

        ModuleModel moduleModel = Mockito.spy(ApplicationModel.defaultModel().getDefaultModule());
        moduleModel.getApplicationModel().getApplicationConfigManager().setApplication(new ApplicationConfig("application1"));
        ExtensionLoader extensionLoaderMock = mock(ExtensionLoader.class);
        Mockito.when(moduleModel.getExtensionLoader(RegistryFactory.class)).thenReturn(extensionLoaderMock);
        Mockito.when(extensionLoaderMock.getAdaptiveExtension()).thenReturn(registryFactory);
        url = url.setScopeModel(moduleModel);

        RegistryProtocol registryProtocol = new RegistryProtocol();

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Cluster cluster = mock(Cluster.class);

        Invoker<?> invoker = registryProtocol.doRefer(cluster, registry, DemoService.class, url, parameters);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);

        URL consumerUrl = ((MigrationInvoker<?>) invoker).getConsumerUrl();
        Assertions.assertTrue((consumerUrl != null));

        Map<String, String> urlParameters = consumerUrl.getParameters();
        URL urlToRegistry = new ServiceConfigURL(
            urlParameters.get(PROTOCOL_KEY) == null ? CONSUMER : urlParameters.get(PROTOCOL_KEY),
            urlParameters.remove(REGISTER_IP_KEY), 0, consumerUrl.getPath(), urlParameters);

        URL registeredConsumerUrl = urlToRegistry.addParameters(CATEGORY_KEY, CONSUMERS_CATEGORY, CHECK_KEY,
            String.valueOf(false)).setScopeModel(moduleModel);

        verify(registry,times(1)).register(registeredConsumerUrl);
    }

}
