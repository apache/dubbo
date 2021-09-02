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
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.Environment;
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
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMERS_CATEGORY;
import static org.apache.dubbo.registry.Constants.ENABLE_CONFIGURATION_LISTEN;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.remoting.Constants.CHECK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CONSUMER_URL_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;

public class RegistryProtocolTest {

    @AfterEach
    public void tearDown() throws IOException {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * verify the generated consumer url information
     */
    @Test
    public void testConsumerUrlWithoutProtocol() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Configuration dynamicGlobalConfiguration = mock(Configuration.class);

        Environment environment = mock(Environment.class);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);

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
        registryProtocol.setRegistryFactory(registryFactory);

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Cluster cluster = mock(Cluster.class);

        Invoker<?> invoker = registryProtocol.doRefer(cluster, registry, DemoService.class, url, parameters);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);

        URL consumerUrl = ((MigrationInvoker<?>) invoker).getConsumerUrl();
        Assertions.assertTrue((consumerUrl != null));

        // verify that the default is dubbo protocol
        Assertions.assertEquals("dubbo", consumerUrl.getProtocol());
        Assertions.assertEquals(parameters.get(REGISTER_IP_KEY), consumerUrl.getHost());
        Assertions.assertFalse(consumerUrl.getAttributes().containsKey(REFER_KEY));
        Assertions.assertEquals("value1", consumerUrl.getAttribute("key1"));

        applicationModelMockedStatic.closeOnDemand();
    }

    /**
     * verify that when the protocol is configured, the protocol of consumer url is the configured protocol
     */
    @Test
    public void testConsumerUrlWithProtocol() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Configuration dynamicGlobalConfiguration = mock(Configuration.class);

        Environment environment = mock(Environment.class);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);

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
        Registry registry = mock(Registry.class);

        RegistryProtocol registryProtocol = new RegistryProtocol();
        registryProtocol.setRegistryFactory(registryFactory);

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

        applicationModelMockedStatic.closeOnDemand();
    }

    /**
     * verify that if multiple groups are not configured, the service reference of the registration center
     * the default is FailoverCluster
     *
     * @see FailoverCluster
     */
    @Test
    public void testReferWithoutGroup() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");


        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Configuration dynamicGlobalConfiguration = mock(Configuration.class);

        Environment environment = mock(Environment.class);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);

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

        RegistryProtocol registryProtocol = new RegistryProtocol();
        registryProtocol.setRegistryFactory(registryFactory);

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Invoker<?> invoker = registryProtocol.refer(DemoService.class, url);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);
        Assertions.assertTrue(((MigrationInvoker<?>) invoker).getCluster() instanceof MockClusterWrapper);
        Assertions.assertTrue(
            ((MockClusterWrapper) ((MigrationInvoker<?>) invoker).getCluster()).getCluster() instanceof FailoverCluster);

        applicationModelMockedStatic.closeOnDemand();
    }

    /**
     * verify that if multiple groups are configured, the service reference of the registration center
     *
     * @see MergeableCluster
     */
    @Test
    public void testReferWithGroup() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Configuration dynamicGlobalConfiguration = mock(Configuration.class);

        Environment environment = mock(Environment.class);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);

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

        RegistryProtocol registryProtocol = new RegistryProtocol();
        registryProtocol.setRegistryFactory(registryFactory);

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Invoker<?> invoker = registryProtocol.refer(DemoService.class, url);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);

        Assertions.assertTrue(((MigrationInvoker<?>) invoker).getCluster() instanceof MockClusterWrapper);

        Assertions.assertTrue(
            ((MockClusterWrapper) ((MigrationInvoker<?>) invoker).getCluster()).getCluster() instanceof MergeableCluster);

        applicationModelMockedStatic.closeOnDemand();
    }

    /**
     * verify that the default RegistryProtocolListener will be executed
     *
     * @see MigrationRuleListener
     */
    @Test
    public void testInterceptInvokerForMigrationRuleListener() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Configuration dynamicGlobalConfiguration = mock(Configuration.class);

        Environment environment = mock(Environment.class);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);

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
        MigrationRuleListener migrationRuleListener = mock(MigrationRuleListener.class);
        List<RegistryProtocolListener> registryProtocolListeners = new ArrayList<>();
        registryProtocolListeners.add(migrationRuleListener);
        MockedStatic<ExtensionLoader> extensionLoaderMockedStatic = mockStatic(ExtensionLoader.class);
        ExtensionLoader extensionLoaderForRegistryProtocolListener = mock(ExtensionLoader.class);
        when(ExtensionLoader.getExtensionLoader(RegistryProtocolListener.class))
            .thenReturn(extensionLoaderForRegistryProtocolListener);
        when(extensionLoaderForRegistryProtocolListener.getActivateExtension(url, "registry.protocol.listener"))
            .thenReturn(registryProtocolListeners);

        registryProtocol.interceptInvoker(clusterInvoker, url, consumerUrl, url);
        verify(migrationRuleListener, times(1)).onRefer(registryProtocol, clusterInvoker, consumerUrl, url);

        extensionLoaderMockedStatic.closeOnDemand();
        applicationModelMockedStatic.closeOnDemand();

    }


    /**
     * Verify that if registry.protocol.listener is configured,
     * whether the corresponding RegistryProtocolListener will be executed normally
     *
     * @see CountRegistryProtocolListener
     */
    @Test
    public void testInterceptInvokerForCustomRegistryProtocolListener() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Configuration dynamicGlobalConfiguration = mock(Configuration.class);

        Environment environment = mock(Environment.class);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        parameters.put("register", "false");
        parameters.put("registry.protocol.listener", "count");

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

        registryProtocol.interceptInvoker(clusterInvoker, url, consumerUrl, url);

        Assertions.assertEquals(1, CountRegistryProtocolListener.getReferCounter().get());
        applicationModelMockedStatic.closeOnDemand();
    }

    /**
     * verify the registered consumer url
     */
    @Test
    public void testRegisterConsumerUrl() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("application1");

        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplicationOrElseThrow()).thenReturn(applicationConfig);

        CompositeConfiguration compositeConfiguration = mock(CompositeConfiguration.class);
        when(compositeConfiguration.convert(Boolean.class, ENABLE_CONFIGURATION_LISTEN, true))
            .thenReturn(true);

        Configuration dynamicGlobalConfiguration = mock(Configuration.class);

        Environment environment = mock(Environment.class);
        when(environment.getConfiguration()).thenReturn(compositeConfiguration);
        when(environment.getDynamicGlobalConfiguration()).thenReturn(dynamicGlobalConfiguration);

        MockedStatic<ApplicationModel> applicationModelMockedStatic = Mockito.mockStatic(ApplicationModel.class);
        applicationModelMockedStatic.when(ApplicationModel::getConfigManager).thenReturn(configManager);
        applicationModelMockedStatic.when(ApplicationModel::getEnvironment).thenReturn(environment);

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

        RegistryProtocol registryProtocol = new RegistryProtocol();
        registryProtocol.setRegistryFactory(registryFactory);

        when(registryFactory.getRegistry(registryProtocol.getRegistryUrl(url))).thenReturn(registry);

        Cluster cluster = mock(Cluster.class);

        Invoker<?> invoker = registryProtocol.doRefer(cluster, registry, DemoService.class, url, parameters);

        Assertions.assertTrue(invoker instanceof MigrationInvoker);

        URL consumerUrl = ((MigrationInvoker<?>) invoker).getConsumerUrl();
        Assertions.assertTrue((consumerUrl != null));

        Map<String, String> urlParameters = consumerUrl.getParameters();
        URL urlToRegistry = new ServiceConfigURL(
            urlParameters.get(PROTOCOL_KEY) == null ? DUBBO : urlParameters.get(PROTOCOL_KEY),
            urlParameters.remove(REGISTER_IP_KEY), 0, consumerUrl.getPath(), urlParameters);

        URL registeredConsumerUrl = urlToRegistry.addParameters(CATEGORY_KEY, CONSUMERS_CATEGORY, CHECK_KEY,
            String.valueOf(false));

        verify(registry,times(1)).register(registeredConsumerUrl);

        applicationModelMockedStatic.closeOnDemand();
    }

}
