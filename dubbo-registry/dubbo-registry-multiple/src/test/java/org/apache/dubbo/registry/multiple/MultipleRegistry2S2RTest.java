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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
            throw new RuntimeException("Failed to initialize MultipleRegistry", e);
        }
    }

    private void setPrivateField(Object targetObj, String fieldName, Object fieldValue) {
        try {
            Field field = targetObj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(targetObj, fieldValue);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set private field via Reflection:" + fieldName, e);
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
        Assertions.assertTrue(multipleRegistry.effectReferenceRegistryURLs.contains(MOCK_ZK_ADDR_1));
        Assertions.assertTrue(multipleRegistry.effectReferenceRegistryURLs.contains(MOCK_ZK_ADDR_2));

        Assertions.assertEquals(2, multipleRegistry.effectServiceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.effectServiceRegistryURLs.contains(MOCK_ZK_ADDR_1));
        Assertions.assertTrue(multipleRegistry.effectServiceRegistryURLs.contains(MOCK_ZK_ADDR_2));

        Assertions.assertTrue(multipleRegistry.getServiceRegistries().containsKey(MOCK_ZK_URL_1));
        Assertions.assertTrue(multipleRegistry.getServiceRegistries().containsKey(MOCK_ZK_URL_2));
        Assertions.assertEquals(
                2, multipleRegistry.getServiceRegistries().values().size());
        //        java.util.Iterator<Registry> registryIterable =
        // multipleRegistry.getServiceRegistries().values().iterator();
        //        Registry firstRegistry = registryIterable.next();
        //        Registry secondRegistry = registryIterable.next();
        Assertions.assertNotNull(MultipleRegistryTestUtil.getZookeeperRegistry(
                multipleRegistry.getServiceRegistries().values()));
        Assertions.assertNotNull(MultipleRegistryTestUtil.getZookeeperRegistry(
                multipleRegistry.getReferenceRegistries().values()));

        Assertions.assertEquals(
                MultipleRegistryTestUtil.getZookeeperRegistry(
                        multipleRegistry.getServiceRegistries().values()),
                MultipleRegistryTestUtil.getZookeeperRegistry(
                        multipleRegistry.getReferenceRegistries().values()));

        Assertions.assertEquals(
                MultipleRegistryTestUtil.getZookeeperRegistry(
                        multipleRegistry.getServiceRegistries().values()),
                MultipleRegistryTestUtil.getZookeeperRegistry(
                        multipleRegistry.getReferenceRegistries().values()));

        Assertions.assertEquals(multipleRegistry.getApplicationName(), "vic");

        Assertions.assertTrue(multipleRegistry.isAvailable());
    }

    @Test
    void testRegistryAndUnRegistry() throws InterruptedException {
        URL serviceUrl = URL.valueOf("http2://multiple/" + SERVICE_NAME
                + "?notify=false&methods=test1,test2&category=providers&application=vic");

        multipleRegistry.register(serviceUrl);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).register(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).register(serviceUrl);

        String path = "/dubbo/" + SERVICE_NAME + "/providers";
        Mockito.when(mockZkClient1.getChildren(path)).thenReturn(Arrays.asList("provider1"));
        List<String> providerList = mockZkClient1.getChildren(path);
        Assertions.assertTrue(!providerList.isEmpty());

        final List<URL> list = new ArrayList<>();
        multipleRegistry.subscribe(serviceUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                list.clear();
                list.addAll(urls);
            }
        });

        ArgumentCaptor<NotifyListener> captor1 = ArgumentCaptor.forClass(NotifyListener.class);
        ArgumentCaptor<NotifyListener> captor2 = ArgumentCaptor.forClass(NotifyListener.class);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).subscribe(Mockito.eq(serviceUrl), captor1.capture());
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).subscribe(Mockito.eq(serviceUrl), captor2.capture());

        List<URL> mockUrls1 = Arrays.asList(URL.valueOf("http2://127.0.0.1:20880/" + SERVICE_NAME));
        List<URL> mockUrls2 = Arrays.asList(URL.valueOf("http2://127.0.0.1:20881/" + SERVICE_NAME));
        captor1.getValue().notify(mockUrls1);
        captor2.getValue().notify(mockUrls2);

        Thread.sleep(1500);
        Assertions.assertEquals(2, list.size());

        multipleRegistry.unregister(serviceUrl);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).unregister(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).unregister(serviceUrl);

        List<URL> unregisterUrls = Arrays.asList(URL.valueOf("empty://127.0.0.1:20880/" + SERVICE_NAME));
        captor1.getValue().notify(unregisterUrls);
        captor2.getValue().notify(unregisterUrls);

        Thread.sleep(1500);
        Assertions.assertEquals(1, list.size());

        List<URL> urls = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("empty", list.get(0).getProtocol());
    }

    @Test
    void testSubscription() throws InterruptedException {
        URL serviceUrl = URL.valueOf("http2://multiple/" + SERVICE2_NAME
                + "?notify=false&methods=test1,test2&category=providers&application=vic");

        multipleRegistry.register(serviceUrl);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).register(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).register(serviceUrl);

        String path = "/dubbo/" + SERVICE2_NAME + "/providers";
        Mockito.when(mockZkClient1.getChildren(path)).thenReturn(Arrays.asList("provider1"));
        List<String> providerList = mockZkClient1.getChildren(path);
        Assumptions.assumeTrue(!providerList.isEmpty());

        final List<URL> list = new ArrayList<>();
        multipleRegistry.subscribe(serviceUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                list.clear();
                list.addAll(urls);
            }
        });

        ArgumentCaptor<NotifyListener> captor1 = ArgumentCaptor.forClass(NotifyListener.class);
        ArgumentCaptor<NotifyListener> captor2 = ArgumentCaptor.forClass(NotifyListener.class);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).subscribe(Mockito.eq(serviceUrl), captor1.capture());
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).subscribe(Mockito.eq(serviceUrl), captor2.capture());

        List<URL> mockUrls1 = Arrays.asList(URL.valueOf("http2://127.0.0.1:20880/" + SERVICE2_NAME));
        List<URL> mockUrls2 = Arrays.asList(URL.valueOf("http2://127.0.0.1:20881/" + SERVICE2_NAME));
        captor1.getValue().notify(mockUrls1);
        captor2.getValue().notify(mockUrls2);

        Thread.sleep(1500);
        Assertions.assertEquals(2, list.size());

        List<Registry> serviceRegistries =
                new ArrayList<>(multipleRegistry.getServiceRegistries().values());
        serviceRegistries.get(0).unregister(serviceUrl);
        Mockito.verify(mockZkRegistry1, Mockito.times(1)).unregister(serviceUrl);

        List<URL> unregisterUrls1 = Arrays.asList(URL.valueOf("empty://127.0.0.1:20880/" + SERVICE2_NAME));
        captor1.getValue().notify(unregisterUrls1);

        Thread.sleep(1500);
        Assertions.assertEquals(1, list.size());
        List<URL> urls1 = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertTrue(!"empty".equals(list.get(0).getProtocol()));

        serviceRegistries.get(1).unregister(serviceUrl);
        Mockito.verify(mockZkRegistry2, Mockito.times(1)).unregister(serviceUrl);
        List<URL> unregisterUrls2 = Arrays.asList(URL.valueOf("empty://127.0.0.1:20881/" + SERVICE2_NAME));
        captor2.getValue().notify(unregisterUrls2);
        Thread.sleep(1500);
        Assertions.assertEquals(1, list.size());
        List<URL> urls2 = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("empty", list.get(0).getProtocol());
    }

    @Test
    void testAggregation() {
        List<URL> result = new ArrayList<URL>();
        List<URL> listToAggregate = new ArrayList<URL>();
        URL url1 = URL.valueOf("dubbo://127.0.0.1:20880/service1");
        URL url2 = URL.valueOf("dubbo://127.0.0.1:20880/service1");
        listToAggregate.add(url1);
        listToAggregate.add(url2);

        URL registryURL = URL.valueOf(
                "mock://127.0.0.1/RegistryService?attachments=zone=hangzhou,tag=middleware&enable-empty-protection=false");

        MultipleRegistry.MultipleNotifyListenerWrapper.aggregateRegistryUrls(result, listToAggregate, registryURL);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(2, result.get(0).getParameters().size());
        Assertions.assertEquals("hangzhou", result.get(0).getParameter("zone"));
        Assertions.assertEquals("middleware", result.get(1).getParameter("tag"));
    }
}
