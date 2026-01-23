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
package org.apache.dubbo.registry.multiple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.zookeeper.ZookeeperRegistry;
import org.apache.dubbo.registry.zookeeper.ZookeeperRegistryFactory;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * 2019-04-30
 */
class MultipleRegistry2S2RTest {

    private static final String SERVICE_NAME = "org.apache.dubbo.registry.MultipleService2S2R";
    private static final String SERVICE2_NAME = "org.apache.dubbo.registry.MultipleService2S2R2";
    private static final String MOCK_ZK_ADDR_1 = "zookeeper://127.0.0.1:2181?check=false";
    private static final String MOCK_ZK_ADDR_2 = "zookeeper://127.0.0.1:2182?check=false";
    private static final URL MOCK_ZK_URL_1 = URL.valueOf(MOCK_ZK_ADDR_1);
    private static final URL MOCK_ZK_URL_2 = URL.valueOf(MOCK_ZK_ADDR_2);

    private MultipleRegistry multipleRegistry;

    private ZookeeperClient mockZkClient1;
    private ZookeeperClient mockZkClient2;
    private ZookeeperRegistry mockZkRegistry1;
    private ZookeeperRegistry mockZkRegistry2;

    @BeforeEach
    void setUp() {
        mockZkClient1 = Mockito.mock(ZookeeperClient.class);
        mockZkClient2 = Mockito.mock(ZookeeperClient.class);
        mockZkRegistry1 = Mockito.mock(ZookeeperRegistry.class);
        mockZkRegistry2 = Mockito.mock(ZookeeperRegistry.class);

        try (MockedConstruction<ZookeeperRegistryFactory> zkFactoryConstruction =
                Mockito.mockConstruction(ZookeeperRegistryFactory.class, (mockFactory, context) -> {
                    Mockito.lenient()
                            .when(mockFactory.getRegistry(MOCK_ZK_URL_1))
                            .thenReturn(mockZkRegistry1);
                    Mockito.lenient()
                            .when(mockFactory.getRegistry(MOCK_ZK_URL_2))
                            .thenReturn(mockZkRegistry2);
                })) {
            Mockito.lenient().when(mockZkRegistry1.isAvailable()).thenReturn(true);
            Mockito.lenient().when(mockZkRegistry2.isAvailable()).thenReturn(true);
            Mockito.lenient().when(mockZkRegistry1.getUrl()).thenReturn(MOCK_ZK_URL_1);
            Mockito.lenient().when(mockZkRegistry2.getUrl()).thenReturn(MOCK_ZK_URL_2);

            URL multipleUrl =
                    URL.valueOf("multiple://127.0.0.1?application=vic&enable-empty-protection=false&check=false&"
                            + MultipleRegistry.REGISTRY_FOR_SERVICE + "=" + MOCK_ZK_ADDR_1 + "," + MOCK_ZK_ADDR_2 + "&"
                            + MultipleRegistry.REGISTRY_FOR_REFERENCE + "=" + MOCK_ZK_ADDR_1 + "," + MOCK_ZK_ADDR_2);

            multipleRegistry = (MultipleRegistry) new MultipleRegistryFactory().createRegistry(multipleUrl);

            Map<URL, Registry> serviceRegistries = new HashMap<>();
            serviceRegistries.put(MOCK_ZK_URL_1, mockZkRegistry1);
            serviceRegistries.put(MOCK_ZK_URL_2, mockZkRegistry2);
            setPrivateField(multipleRegistry, "serviceRegistries", serviceRegistries);
            setPrivateField(multipleRegistry, "referenceRegistries", serviceRegistries);
        } catch (Exception e) {
            throw new RuntimeException("初始化MultipleRegistry失败", e);
        }
    }

    private void setPrivateField(Object targetObj, String fieldName, Object fieldValue) {
        try {
            Field field = targetObj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(targetObj, fieldValue);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("反射设置私有字段失败：" + fieldName, e);
        }
    }

    @Test
    void testParamConfig() {
        Assertions.assertEquals(2, multipleRegistry.origReferenceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.origReferenceRegistryURLs.contains(MOCK_ZK_ADDR_1));
        Assertions.assertTrue(multipleRegistry.origReferenceRegistryURLs.contains(MOCK_ZK_ADDR_2));

        Assertions.assertEquals(2, multipleRegistry.origServiceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.origServiceRegistryURLs.contains(MOCK_ZK_ADDR_1));
        Assertions.assertTrue(multipleRegistry.origServiceRegistryURLs.contains(MOCK_ZK_ADDR_2));

        Assertions.assertEquals(2, multipleRegistry.effectReferenceRegistryURLs.size());
        Assertions.assertEquals(2, multipleRegistry.effectServiceRegistryURLs.size());

        Assertions.assertTrue(multipleRegistry.getServiceRegistries().containsKey(MOCK_ZK_URL_1));
        Assertions.assertTrue(multipleRegistry.getServiceRegistries().containsKey(MOCK_ZK_URL_2));
        Assertions.assertEquals(2, multipleRegistry.getServiceRegistries().size());

        Assertions.assertEquals("vic", multipleRegistry.getApplicationName());
        Assertions.assertTrue(multipleRegistry.isAvailable());
    }

    @Test
    void testRegistryAndUnRegistry() {
        URL serviceUrl = URL.valueOf(
                "http2://multiple/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");

        multipleRegistry.register(serviceUrl);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).register(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).register(serviceUrl);

        NotifyListener testListener = urls -> {};
        multipleRegistry.subscribe(serviceUrl, testListener);
        Mockito.verify(mockZkRegistry1, Mockito.times(1))
                .subscribe(Mockito.eq(serviceUrl), Mockito.any(NotifyListener.class));
        Mockito.verify(mockZkRegistry2, Mockito.times(1))
                .subscribe(Mockito.eq(serviceUrl), Mockito.any(NotifyListener.class));

        multipleRegistry.unregister(serviceUrl);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).unregister(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).unregister(serviceUrl);
    }

    @Test
    void testSubscription() {
        URL serviceUrl = URL.valueOf(
                "http2://multiple/" + SERVICE2_NAME + "?notify=false&methods=test1,test2&category=providers");

        multipleRegistry.register(serviceUrl);
        multipleRegistry.subscribe(serviceUrl, urls -> {});
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).register(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).register(serviceUrl);

        List<Registry> serviceRegistries =
                new ArrayList<>(multipleRegistry.getServiceRegistries().values());
        serviceRegistries.get(0).unregister(serviceUrl);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).unregister(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.never()).unregister(serviceUrl);

        serviceRegistries.get(1).unregister(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).unregister(serviceUrl);
    }

    @Test
    void testAggregation() {
        List<URL> result = new ArrayList<>();
        List<URL> listToAggregate = new ArrayList<>();
        URL url1 = URL.valueOf("dubbo://127.0.0.1:20880/service1");
        URL url2 = URL.valueOf("dubbo://127.0.0.1:20880/service1");
        listToAggregate.add(url1);
        listToAggregate.add(url2);

        URL registryURL = URL.valueOf(
                "mock://127.0.0.1/RegistryService?attachments=zone=hangzhou,tag=middleware&enable-empty-protection=false");

        MultipleRegistry.MultipleNotifyListenerWrapper.aggregateRegistryUrls(result, listToAggregate, registryURL);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("hangzhou", result.get(0).getParameter("zone"));
        Assertions.assertEquals("middleware", result.get(0).getParameter("tag"));
        Assertions.assertEquals("hangzhou", result.get(1).getParameter("zone"));
        Assertions.assertEquals("middleware", result.get(1).getParameter("tag"));
    }
}
