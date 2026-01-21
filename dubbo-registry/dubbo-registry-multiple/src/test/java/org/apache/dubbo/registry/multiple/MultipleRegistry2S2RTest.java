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
import org.apache.dubbo.remoting.zookeeper.curator5.Curator5ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.curator5.ZookeeperClient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipleRegistry2S2RTest {

    private static final String SERVICE_NAME = "org.apache.dubbo.registry.MultipleService2S2R";
    private static final String SERVICE2_NAME = "org.apache.dubbo.registry.MultipleService2S2R2";
    private static final String MOCK_ZK_ADDR1 = "zookeeper://mock-zk-1:2181?check=false";
    private static final String MOCK_ZK_ADDR2 = "zookeeper://mock-zk-2:2182?check=false";

    private MultipleRegistry multipleRegistry;
    private ZookeeperClient zookeeperClient;
    private ZookeeperClient zookeeperClient2;
    private ZookeeperRegistry zookeeperRegistry;
    private ZookeeperRegistry zookeeperRegistry2;
    private String zookeeperConnectionAddress1;
    private String zookeeperConnectionAddress2;

    private static ZookeeperRegistry getZookeeperRegistry(Collection<Registry> registries) {
        for (Registry registry : registries) {
            if (registry instanceof ZookeeperRegistry) {
                return (ZookeeperRegistry) registry;
            }
        }
        return null;
    }

    private static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        zookeeperConnectionAddress1 = MOCK_ZK_ADDR1;
        zookeeperConnectionAddress2 = MOCK_ZK_ADDR2;

        multipleRegistry = mock(MultipleRegistry.class);

        List<String> origRefUrls = new ArrayList<>();
        origRefUrls.add(MOCK_ZK_ADDR1);
        origRefUrls.add(MOCK_ZK_ADDR2);
        setFieldValue(multipleRegistry, "origReferenceRegistryURLs", origRefUrls);

        List<String> origServiceUrls = new ArrayList<>();
        origServiceUrls.add(MOCK_ZK_ADDR1);
        origServiceUrls.add(MOCK_ZK_ADDR2);
        setFieldValue(multipleRegistry, "origServiceRegistryURLs", origServiceUrls);

        List<String> effectRefUrls = new ArrayList<>();
        effectRefUrls.add(MOCK_ZK_ADDR1);
        effectRefUrls.add(MOCK_ZK_ADDR2);
        setFieldValue(multipleRegistry, "effectReferenceRegistryURLs", effectRefUrls);

        List<String> effectServiceUrls = new ArrayList<>();
        effectServiceUrls.add(MOCK_ZK_ADDR1);
        effectServiceUrls.add(MOCK_ZK_ADDR2);
        setFieldValue(multipleRegistry, "effectServiceRegistryURLs", effectServiceUrls);

        Map<String, Registry> mockServiceRegistries = new HashMap<>();
        zookeeperRegistry = mock(ZookeeperRegistry.class);
        zookeeperRegistry2 = mock(ZookeeperRegistry.class);
        mockServiceRegistries.put(MOCK_ZK_ADDR1, zookeeperRegistry);
        mockServiceRegistries.put(MOCK_ZK_ADDR2, zookeeperRegistry2);
        when(multipleRegistry.getServiceRegistries()).thenReturn(mockServiceRegistries);
        when(multipleRegistry.getReferenceRegistries()).thenReturn(mockServiceRegistries);

        zookeeperClient = mock(Curator5ZookeeperClient.class);
        zookeeperClient2 = mock(Curator5ZookeeperClient.class);
        when(zookeeperClient.getChildren(any(String.class))).thenReturn(Collections.singletonList("mock-provider"));
        when(zookeeperClient2.getChildren(any(String.class))).thenReturn(Collections.singletonList("mock-provider"));
        doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        return null;
                    }
                })
                .when(multipleRegistry)
                .register(any(URL.class));
        doAnswer(new Answer<Void>() {
                    @Override
                    public Void answer(InvocationOnMock invocation) {
                        return null;
                    }
                })
                .when(multipleRegistry)
                .unregister(any(URL.class));
    }

    @AfterEach
    void cleanup() {
        if (multipleRegistry != null) {
            try {
                multipleRegistry.destroy();
            } catch (Exception e) {
            }
        }
    }

    @Test
    void testParamConfig() {
        // 字段断言
        Assertions.assertEquals(2, multipleRegistry.origReferenceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.origReferenceRegistryURLs.contains(zookeeperConnectionAddress1));
        Assertions.assertTrue(multipleRegistry.origReferenceRegistryURLs.contains(zookeeperConnectionAddress2));

        Assertions.assertEquals(2, multipleRegistry.origServiceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.origServiceRegistryURLs.contains(zookeeperConnectionAddress1));
        Assertions.assertTrue(multipleRegistry.origServiceRegistryURLs.contains(zookeeperConnectionAddress2));

        Assertions.assertEquals(2, multipleRegistry.effectReferenceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.effectReferenceRegistryURLs.contains(zookeeperConnectionAddress1));
        Assertions.assertTrue(multipleRegistry.effectReferenceRegistryURLs.contains(zookeeperConnectionAddress2));

        Assertions.assertEquals(2, multipleRegistry.effectServiceRegistryURLs.size());
        Assertions.assertTrue(multipleRegistry.effectServiceRegistryURLs.contains(zookeeperConnectionAddress1));
        Assertions.assertTrue(multipleRegistry.effectServiceRegistryURLs.contains(zookeeperConnectionAddress2));

        Assertions.assertTrue(multipleRegistry.getServiceRegistries().containsKey(zookeeperConnectionAddress1));
        Assertions.assertTrue(multipleRegistry.getServiceRegistries().containsKey(zookeeperConnectionAddress2));
        Assertions.assertEquals(
                2, multipleRegistry.getServiceRegistries().values().size());

        Assertions.assertNotNull(
                getZookeeperRegistry(multipleRegistry.getServiceRegistries().values()));
        Assertions.assertNotNull(
                getZookeeperRegistry(multipleRegistry.getReferenceRegistries().values()));

        Assertions.assertEquals(
                getZookeeperRegistry(multipleRegistry.getServiceRegistries().values()),
                getZookeeperRegistry(multipleRegistry.getReferenceRegistries().values()));

        when(multipleRegistry.getApplicationName()).thenReturn("vic");
        Assertions.assertEquals(multipleRegistry.getApplicationName(), "vic");
        when(multipleRegistry.isAvailable()).thenReturn(true);
        Assertions.assertTrue(multipleRegistry.isAvailable());
    }

    @Test
    void testRegistryAndUnRegistry() throws InterruptedException {
        URL serviceUrl = URL.valueOf(
                "http2://multiple/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
        multipleRegistry.register(serviceUrl);

        String path = "/dubbo/" + SERVICE_NAME + "/providers";
        List<String> providerList = zookeeperClient.getChildren(path);
        Assertions.assertTrue(!providerList.isEmpty());

        final List<URL> list = new ArrayList<>();
        NotifyListener listener = urls -> {
            list.clear();
            list.addAll(urls);
        };

        doAnswer(inv -> {
                    List<URL> initialUrls = new ArrayList<>();
                    initialUrls.add(URL.valueOf("dubbo://mock-ip:20880/" + SERVICE_NAME));
                    initialUrls.add(URL.valueOf("dubbo://mock-ip:20881/" + SERVICE_NAME));
                    listener.notify(initialUrls);
                    return null;
                })
                .when(multipleRegistry)
                .subscribe(any(URL.class), any(NotifyListener.class));
        multipleRegistry.subscribe(serviceUrl, listener);

        Assertions.assertEquals(2, list.size());

        doAnswer(inv -> {
                    listener.notify(Collections.singletonList(URL.valueOf("empty://127.0.0.1")));
                    return null;
                })
                .when(multipleRegistry)
                .unregister(any(URL.class));
        multipleRegistry.unregister(serviceUrl);

        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("empty", list.get(0).getProtocol());
    }

    @Test
    void testSubscription() throws InterruptedException {
        URL serviceUrl = URL.valueOf(
                "http2://multiple/" + SERVICE2_NAME + "?notify=false&methods=test1,test2&category=providers");
        multipleRegistry.register(serviceUrl);

        String path = "/dubbo/" + SERVICE2_NAME + "/providers";
        List<String> providerList = zookeeperClient.getChildren(path);
        Assumptions.assumeTrue(!providerList.isEmpty());

        final List<URL> list = new ArrayList<>();
        NotifyListener listener = urls -> {
            list.clear();
            list.addAll(urls);
        };

        doAnswer(inv -> {
                    List<URL> initialUrls = new ArrayList<>();
                    initialUrls.add(URL.valueOf("dubbo://mock-ip:20880/" + SERVICE2_NAME));
                    initialUrls.add(URL.valueOf("dubbo://mock-ip:20881/" + SERVICE2_NAME));
                    listener.notify(initialUrls);
                    return null;
                })
                .when(multipleRegistry)
                .subscribe(any(URL.class), any(NotifyListener.class));
        multipleRegistry.subscribe(serviceUrl, listener);

        Assertions.assertEquals(2, list.size());

        List<Registry> serviceRegistries =
                new ArrayList<>(multipleRegistry.getServiceRegistries().values());
        ZookeeperRegistry firstRegistry = (ZookeeperRegistry) serviceRegistries.get(0);
        ZookeeperRegistry secondRegistry = (ZookeeperRegistry) serviceRegistries.get(1);

        doAnswer(inv -> {
                    listener.notify(Collections.singletonList(URL.valueOf("dubbo://mock-ip:20880/" + SERVICE2_NAME)));
                    return null;
                })
                .when(firstRegistry)
                .unregister(any(URL.class));
        firstRegistry.unregister(serviceUrl);

        Assertions.assertEquals(1, list.size());
        Assertions.assertTrue(!"empty".equals(list.get(0).getProtocol()));

        doAnswer(inv -> {
                    listener.notify(Collections.singletonList(URL.valueOf("empty://127.0.0.1")));
                    return null;
                })
                .when(secondRegistry)
                .unregister(any(URL.class));
        secondRegistry.unregister(serviceUrl);

        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("empty", list.get(0).getProtocol());
    }

    @Test
    void testAggregation() {
        List<URL> result = new ArrayList<>();
        List<URL> listToAggregate = new ArrayList<>();
        URL url1 = URL.valueOf("dubbo://127.0.0.1:20880/service1?zone=hangzhou");
        URL url2 = URL.valueOf("dubbo://127.0.0.1:20880/service1?tag=middleware");
        listToAggregate.add(url1);
        listToAggregate.add(url2);

        URL registryURL = URL.valueOf(
                "mock://127.0.0.1/RegistryService?attachments=zone=hangzhou,tag=middleware&enable-empty-protection=false");

        MultipleRegistry.MultipleNotifyListenerWrapper.aggregateRegistryUrls(result, listToAggregate, registryURL);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("hangzhou", result.get(0).getParameter("zone"));
        Assertions.assertEquals("middleware", result.get(1).getParameter("tag"));
    }
}
