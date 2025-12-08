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
package org.apache.dubbo.registry.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.google.common.collect.Lists;
import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceCacheBuilder;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.MockedStatic;
import org.mockito.internal.util.collections.Sets;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * {@link ZookeeperServiceDiscovery} Test
 *
 * @since 2.7.5
 */
@DisabledForJreRange(min = JRE.JAVA_16)
class ZookeeperServiceDiscoveryTest {

    private static final String SERVICE_NAME = "A";

    private static final String LOCALHOST = "127.0.0.1";

    private URL registryUrl;

    private ZookeeperServiceDiscovery discovery;
    private static String zookeeperConnectionAddress1;
    private static MockedStatic<CuratorFrameworkFactory> curatorFrameworkFactoryMockedStatic;
    private static MockedStatic<ServiceDiscoveryBuilder> serviceDiscoveryBuilderMockedStatic;

    private CuratorFramework mockCuratorFramework;
    private CuratorZookeeperClient mockCuratorZookeeperClient;
    private ServiceDiscoveryBuilder mockServiceDiscoveryBuilder;
    private ServiceDiscovery mockServiceDiscovery;
    private ServiceCacheBuilder mockServiceCacheBuilder;
    private ServiceCache mockServiceCache;
    private static final CuratorFrameworkFactory.Builder spyBuilder = spy(CuratorFrameworkFactory.builder());

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = "zookeeper://localhost:" + "2181";
        curatorFrameworkFactoryMockedStatic = mockStatic(CuratorFrameworkFactory.class);
        curatorFrameworkFactoryMockedStatic
                .when(CuratorFrameworkFactory::builder)
                .thenReturn(spyBuilder);
        serviceDiscoveryBuilderMockedStatic = mockStatic(ServiceDiscoveryBuilder.class);
    }

    @BeforeEach
    public void init() throws Exception {
        mockServiceDiscoveryBuilder = mock(ServiceDiscoveryBuilder.class);
        mockServiceDiscovery = mock(ServiceDiscovery.class);
        mockServiceCacheBuilder = mock(ServiceCacheBuilder.class);
        mockCuratorFramework = mock(CuratorFramework.class);
        mockServiceCache = mock(ServiceCache.class);

        serviceDiscoveryBuilderMockedStatic
                .when(() -> ServiceDiscoveryBuilder.builder(any()))
                .thenReturn(mockServiceDiscoveryBuilder);
        when(mockServiceDiscoveryBuilder.client(any())).thenReturn(mockServiceDiscoveryBuilder);
        when(mockServiceDiscoveryBuilder.basePath(any())).thenReturn(mockServiceDiscoveryBuilder);
        when(mockServiceDiscoveryBuilder.build()).thenReturn(mockServiceDiscovery);
        when(mockServiceDiscovery.serviceCacheBuilder()).thenReturn(mockServiceCacheBuilder);
        when(mockServiceCacheBuilder.name(any())).thenReturn(mockServiceCacheBuilder);
        when(mockServiceCacheBuilder.build()).thenReturn(mockServiceCache);
        doReturn(mockCuratorFramework).when(spyBuilder).build();
        mockCuratorZookeeperClient = mock(CuratorZookeeperClient.class);
        // mock default is started. If method need other status please replace in test method.
        when(mockCuratorFramework.getZookeeperClient()).thenReturn(mockCuratorZookeeperClient);
        when(mockCuratorFramework.getState()).thenReturn(CuratorFrameworkState.STARTED);
        when(mockCuratorZookeeperClient.isConnected()).thenReturn(true);

        this.registryUrl = URL.valueOf(zookeeperConnectionAddress1);
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig(SERVICE_NAME));
        registryUrl.setScopeModel(applicationModel);
        this.discovery = new ZookeeperServiceDiscovery(applicationModel, registryUrl);
    }

    @AfterEach
    public void close() throws Exception {
        discovery.destroy();
    }

    @Test
    void testRegistration() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);

        // Add Listener
        discovery.addServiceInstancesChangedListener(
                new ServiceInstancesChangedListener(Sets.newSet(SERVICE_NAME), discovery) {
                    @Override
                    public void onEvent(ServiceInstancesChangedEvent event) {
                        latch.countDown();
                    }
                });

        discovery.register();
        latch.await();
        List<ServiceInstance> serviceInstances = discovery.getInstances(SERVICE_NAME);
        assertEquals(0, serviceInstances.size());

        discovery.register(URL.valueOf("dubbo://1.1.2.3:20880/DemoService"));
        discovery.register();

        DefaultServiceInstance serviceInstance = (DefaultServiceInstance) discovery.getLocalInstance();
        List<org.apache.curator.x.discovery.ServiceInstance> lists =
                Lists.newArrayList(org.apache.curator.x.discovery.ServiceInstance.builder()
                        .name(SERVICE_NAME)
                        .address(
                                URL.valueOf("dubbo://1.1.2.3:20880/DemoService").getHost())
                        .port(20880)
                        .payload(new ZookeeperInstance(
                                "", serviceInstance.getServiceName(), serviceInstance.getMetadata()))
                        .build());
        when(mockServiceDiscovery.queryForInstances(any())).thenReturn(lists);
        serviceInstances = discovery.getInstances(SERVICE_NAME);

        assertTrue(serviceInstances.contains(serviceInstance));
        assertEquals(asList(serviceInstance), serviceInstances);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("message", "Hello,World");
        serviceInstance.setMetadata(metadata);

        discovery.register(URL.valueOf("dubbo://1.1.2.3:20880/DemoService1"));
        discovery.update();

        lists = Lists.newArrayList(org.apache.curator.x.discovery.ServiceInstance.builder()
                .name(SERVICE_NAME)
                .address(URL.valueOf("dubbo://1.1.2.3:20880/DemoService").getHost())
                .port(20880)
                .payload(new ZookeeperInstance("", serviceInstance.getServiceName(), metadata))
                .build());

        when(mockServiceDiscovery.queryForInstances(any())).thenReturn(lists);
        serviceInstances = discovery.getInstances(SERVICE_NAME);

        assertEquals(serviceInstance, serviceInstances.get(0));

        discovery.unregister();

        when(mockServiceDiscovery.queryForInstances(any())).thenReturn(new ArrayList<>());
        serviceInstances = discovery.getInstances(SERVICE_NAME);

        assertTrue(serviceInstances.isEmpty());
    }

    @Test
    void testRegistryCheckConnectDefault() {
        when(mockCuratorZookeeperClient.isConnected()).thenReturn(false);

        URL registryUrl = URL.valueOf(zookeeperConnectionAddress1);
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        registryUrl.setScopeModel(applicationModel);

        Assertions.assertThrowsExactly(IllegalStateException.class, () -> {
            new ZookeeperServiceDiscovery(applicationModel, registryUrl);
        });
    }

    @Test
    void testRegistryNotCheckConnect() {
        when(mockCuratorZookeeperClient.isConnected()).thenReturn(false);

        URL registryUrl = URL.valueOf(zookeeperConnectionAddress1).addParameter(CHECK_KEY, false);
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        registryUrl.setScopeModel(applicationModel);

        Assertions.assertDoesNotThrow(() -> {
            new ZookeeperServiceDiscovery(applicationModel, registryUrl);
        });
    }

    @AfterAll
    public static void afterAll() throws Exception {
        if (curatorFrameworkFactoryMockedStatic != null) {
            curatorFrameworkFactoryMockedStatic.close();
        }
        if (serviceDiscoveryBuilderMockedStatic != null) {
            serviceDiscoveryBuilderMockedStatic.close();
        }
    }
}
