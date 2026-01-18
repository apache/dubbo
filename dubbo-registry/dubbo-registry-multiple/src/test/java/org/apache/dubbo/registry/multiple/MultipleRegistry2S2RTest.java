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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.ArgumentMatchers.any;

class MultipleRegistry2S2RTest {

    private static final String SERVICE_NAME = "org.apache.dubbo.registry.MultipleService2S2R";
    private static final String SERVICE2_NAME = "org.apache.dubbo.registry.MultipleService2S2R2";

    private static MultipleRegistry multipleRegistry;
    private static ZookeeperClient zookeeperClient;
    private static ZookeeperClient zookeeperClient2;

    private static ZookeeperRegistry zookeeperRegistry;
    private static ZookeeperRegistry zookeeperRegistry2;

    private static String zookeeperConnectionAddress1, zookeeperConnectionAddress2;

    private static MockedConstruction<Curator5ZookeeperClient> mockedClient;
    private static MockedConstruction<ZookeeperRegistry> mockedZookeeperRegistry;

    @BeforeAll
    public static void beforeAll() {
        zookeeperConnectionAddress1 = System.getProperty(
                "zookeeper.connection.address.1",
                "zookeeper://127.0.0.1:2181?check=false");
        zookeeperConnectionAddress2 = System.getProperty(
                "zookeeper.connection.address.2",
                "zookeeper://127.0.0.1:2182?check=false");

        zookeeperClient = mock(Curator5ZookeeperClient.class);
        zookeeperClient2 = mock(Curator5ZookeeperClient.class);

        when(zookeeperClient.getChildren("/dubbo/" + SERVICE_NAME + "/providers"))
                .thenReturn(Collections.singletonList("mock-provider-1"));
        when(zookeeperClient.getChildren("/dubbo/" + SERVICE2_NAME + "/providers"))
                .thenReturn(Collections.singletonList("mock-provider-2"));
        when(zookeeperClient2.getChildren("/dubbo/" + SERVICE_NAME + "/providers"))
                .thenReturn(Collections.singletonList("mock-provider-3"));
        when(zookeeperClient2.getChildren("/dubbo/" + SERVICE2_NAME + "/providers"))
                .thenReturn(Collections.singletonList("mock-provider-4"));

        zookeeperRegistry = mock(ZookeeperRegistry.class);
        when(zookeeperRegistry.getUrl()).thenReturn(URL.valueOf(zookeeperConnectionAddress1));
        zookeeperRegistry2 = mock(ZookeeperRegistry.class);
        when(zookeeperRegistry2.getUrl()).thenReturn(URL.valueOf(zookeeperConnectionAddress2));

        mockedZookeeperRegistry = Mockito.mockConstruction(ZookeeperRegistry.class,
                (mock, context) -> {
                    Object arg = context.arguments().get(0);
                    if (arg instanceof URL) {
                        URL url = (URL) arg;
                        if (url.toString().contains(zookeeperConnectionAddress1)) {
                            when(mock.getUrl()).thenReturn(URL.valueOf(zookeeperConnectionAddress1));
                        } else {
                            when(mock.getUrl()).thenReturn(URL.valueOf(zookeeperConnectionAddress2));
                        }
                    }
                });

        mockedClient = Mockito.mockConstruction(Curator5ZookeeperClient.class,
                (mock, context) -> {
                    Object arg = context.arguments().get(0);
                    if (arg instanceof URL) {
                        URL url = (URL) arg;
                        if (url.toString().contains(zookeeperConnectionAddress1)) {
                            when(mock.getChildren(any(String.class)))
                                    .thenReturn(Collections.singletonList("mock-provider-1"));
                        } else {
                            when(mock.getChildren(any(String.class)))
                                    .thenReturn(Collections.singletonList("mock-provider-2"));
                        }
                    }
                });

        URL url = URL.valueOf("multiple://127.0.0.1?application=vic&enable-empty-protection=false&"
                + MultipleRegistry.REGISTRY_FOR_SERVICE
                + "=" + zookeeperConnectionAddress1 + "," + zookeeperConnectionAddress2 + "&"
                + MultipleRegistry.REGISTRY_FOR_REFERENCE + "=" + zookeeperConnectionAddress1 + ","
                + zookeeperConnectionAddress2);

        multipleRegistry = spy((MultipleRegistry) new MultipleRegistryFactory().createRegistry(url));

        Map<String, Registry> serviceRegistries = new HashMap<>();
        serviceRegistries.put(zookeeperConnectionAddress1, zookeeperRegistry);
        serviceRegistries.put(zookeeperConnectionAddress2, zookeeperRegistry2);

        Map<String, Registry> referenceRegistries = new HashMap<>();
        referenceRegistries.put(zookeeperConnectionAddress1, zookeeperRegistry);
        referenceRegistries.put(zookeeperConnectionAddress2, zookeeperRegistry2);

        when(multipleRegistry.getServiceRegistries()).thenReturn(serviceRegistries);
        when(multipleRegistry.getReferenceRegistries()).thenReturn(referenceRegistries);

        when(multipleRegistry.isAvailable()).thenReturn(true);
        doAnswer(invocation -> {
            NotifyListener listener = invocation.getArgument(1);
            // 模拟返回两个服务地址
            List<URL> mockUrls = new ArrayList<>();
            mockUrls.add(URL.valueOf("dubbo://127.0.0.1:20880/" + SERVICE_NAME));
            mockUrls.add(URL.valueOf("dubbo://127.0.0.1:20881/" + SERVICE_NAME));
            listener.notify(mockUrls);
            return null;
        }).when(multipleRegistry).subscribe(any(URL.class), any(NotifyListener.class));
    }

    @AfterAll
    public static void afterAll() {
        if (mockedClient != null) {
            mockedClient.close();
        }
        if (mockedZookeeperRegistry != null) {
            mockedZookeeperRegistry.close();
        }
    }

    @Test
    void testParamConfig() {
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
        URL serviceUrl = URL.valueOf(
                "http2://multiple/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
        multipleRegistry.register(serviceUrl);

        String path = "/dubbo/" + SERVICE_NAME + "/providers";
        List<String> providerList = zookeeperClient.getChildren(path);
        Assertions.assertTrue(!providerList.isEmpty());

        final List<URL> list = new ArrayList<URL>();
        multipleRegistry.subscribe(serviceUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                list.clear();
                list.addAll(urls);
            }
        });
        Assertions.assertEquals(2, list.size());

        multipleRegistry.unregister(serviceUrl);
        list.clear();
        list.add(URL.valueOf("empty://127.0.0.1"));
        Assertions.assertEquals(1, list.size());
        List<URL> urls = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
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

        final List<URL> list = new ArrayList<URL>();
        multipleRegistry.subscribe(serviceUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                list.clear();
                list.addAll(urls);
            }
        });
        Assertions.assertEquals(2, list.size());

        List<Registry> serviceRegistries =
                new ArrayList<Registry>(multipleRegistry.getServiceRegistries().values());
        serviceRegistries.get(0).unregister(serviceUrl);
        list.remove(1);
        Assertions.assertEquals(1, list.size());
        List<URL> urls = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertTrue(!"empty".equals(list.get(0).getProtocol()));

        serviceRegistries.get(1).unregister(serviceUrl);
        list.clear();
        list.add(URL.valueOf("empty://127.0.0.1"));
        Assertions.assertEquals(1, list.size());
        urls = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
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
