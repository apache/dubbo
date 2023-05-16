///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.dubbo.registry.xds.util.protocol.impl;
//
//import io.envoyproxy.envoy.config.core.v3.Node;
//import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
//import io.grpc.stub.StreamObserver;
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.registry.xds.util.XdsChannel;
//import org.apache.dubbo.registry.xds.util.protocol.AbstractProtocol;
//import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
//import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;
//import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
//import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.MockedConstruction;
//import org.mockito.MockedStatic;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.any;
//import static org.mockito.Mockito.mockConstruction;
//import static org.mockito.Mockito.times;
//
//public class DsProtocolTest {
//    private XdsChannel xdsChannel;
//
//    private LdsProtocolMock ldsProtocolMock;
//    private RdsProtocolMock rdsProtocolMock;
//
//    private EdsProtocolMock edsProtocolMock;
//
//    private Map<String, ListenerResult> listenerResult;
//
//    private Set<String> routeConfigNames;
//
//    private Set<String> cluster;
//    private Map<String, RouteResult> routeResult;
//
//    private Set<Endpoint> endpoints;
//    private Map<String, EndpointResult> endpointResult;
//
//    private Map<String, Set<String>> domainMap;
//
//    private ApplicationModel applicationModel;
//    private URL url;
//
//    private Node node;
//
//    public DsProtocolTest() {
//    }
//
//    @BeforeEach
//    public void setUp() {
//        this.url = spy(URL.valueOf("xds://istiod.istio-system.svc:15012?secure=plaintext"));
//        this.node = mock(Node.class);
//
//        this.applicationModel = ApplicationModel.defaultModel();
//        xdsChannel = mock(XdsChannel.class);
//        when(xdsChannel.getUrl()).thenReturn(url);
//
//        this.ldsProtocolMock = spy(new LdsProtocolMock(xdsChannel, node, 1, applicationModel));
//        this.rdsProtocolMock = spy(new RdsProtocolMock(xdsChannel, node, 1, applicationModel));
//        this.edsProtocolMock = spy(new EdsProtocolMock(xdsChannel, node, 1, applicationModel));
//
//        // mock lister result
//        this.routeConfigNames = new HashSet<>();
//        routeConfigNames.add("15014");
//        Map<String, ListenerResult> listenerResults = new HashMap();
//        listenerResults.put(ldsProtocolMock.emptyResourceName,new ListenerResult(routeConfigNames));
//        this.listenerResult = spy(listenerResults);
//
//        // mock route result
//        this.domainMap = new HashMap<>();
//        Set<String> domainValue = new HashSet<>();
//        domainValue.add("outbound|15014||istiod.istio-system.svc.cluster.local");
//        domainMap.put("istiod.istio-system.svc", domainValue);
//        Map<String, RouteResult> routeResults = new HashMap();
//        routeResults.put("15014", new RouteResult(domainMap));
//        this.routeResult = routeResults;
//
//        // mock eds result
//        Set<String> cluster = new HashSet<>();
//        cluster.add("dubbo-samples-provider");
//        this.cluster = cluster;
//        Endpoint endpoint = new Endpoint();
//        endpoint.setWeight(1);
//        endpoint.setHealthy(true);
//        endpoint.setPortValue(50051);
//        endpoint.setAddress("10.1.1.67");
//        this.endpoints = new HashSet<>();
//        endpoints.add(endpoint);
//        Map<String, EndpointResult> endpointResults = new HashMap();
//        endpointResults.put("dubbo-samples-provider" ,new EndpointResult(endpoints));
//        this.endpointResult = endpointResults;
////        mockedStatic.close();
//    }
//
//
//    @Test
//    void testGetResource() {
//        StreamObserver<DiscoveryRequest> requestStreamObserver = mock(StreamObserver.class);
//
//        // mock lds getResource
//        when(xdsChannel.createDeltaDiscoveryRequest(any(StreamObserver.class))).thenReturn(requestStreamObserver);
//        MockedConstruction<CompletableFuture> ldsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
//            when(mock.get()).thenReturn(listenerResult);
//        });
//        Map<String, ListenerResult> ldsResult = ldsProtocolMock.getResource(null);
//
//        Assertions.assertNotNull(ldsResult);
//        verify(ldsProtocolMock, times(0)).isCacheExistResource(null);
//        ldsMocked.close();
//
//        // mock rds getResource
//        MockedConstruction<CompletableFuture> rdsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
//            when(mock.get()).thenReturn(null);
//        });
//
//        Map<String, RouteResult> rdsResult = rdsProtocolMock.getResource(routeConfigNames);
//
//        Assertions.assertNull(rdsResult);
//        Assertions.assertFalse(rdsProtocolMock.isCacheExistResource(routeConfigNames));
//
//        rdsProtocolMock.getResourcesMap().putAll(routeResult);
//        rdsResult = rdsProtocolMock.getResource(routeConfigNames);
//        Assertions.assertNotNull(rdsResult);
//        Assertions.assertTrue(rdsProtocolMock.isCacheExistResource(routeConfigNames));
//        Map<String, RouteResult> newRdsResult = routeConfigNames.stream().collect(Collectors.toMap(k -> k, v -> rdsProtocolMock.getCacheResource(v)));
//        Assertions.assertEquals(newRdsResult, rdsResult);
//        rdsMocked.close();
//
//        //mock eds getResource
//        MockedConstruction<CompletableFuture> edsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
//            when(mock.get()).thenReturn(null);
//        });
//
//        Map<String, EndpointResult> edsResult = edsProtocolMock.getResource(cluster);
//        verify(edsProtocolMock, times(1)).isCacheExistResource(cluster);
//        Assertions.assertNull(edsResult);
//        Assertions.assertFalse(edsProtocolMock.isCacheExistResource(cluster));
//        edsProtocolMock.getResourcesMap().put("dubbo-samples-provider", new EndpointResult(endpoints));
//        edsResult = edsProtocolMock.getResource(cluster);
//        Assertions.assertNotNull(edsResult);
//        Assertions.assertTrue(edsProtocolMock.isCacheExistResource(cluster));
//
//        Map<String, EndpointResult> newEdsResult = cluster.stream().collect(Collectors.toMap(k -> k, v -> edsProtocolMock.getCacheResource(v)));
//        Assertions.assertEquals(newEdsResult, endpointResult);
//        edsMocked.close();
//    }
//
//    @Test
//    void observeDsReConnect() {
//        StreamObserver<DiscoveryRequest> requestStreamObserver = mock(StreamObserver.class);
//
//        // mock lds reconnect
//        when(xdsChannel.createDeltaDiscoveryRequest(any(StreamObserver.class))).thenReturn(requestStreamObserver);
//        doNothing().when(requestStreamObserver).onNext(any());
//
//        Map<Set<String>, List<Consumer<Map<String, ListenerResult>>>> ldsMap = new HashMap<>();
//        AtomicBoolean isLdsReConnect = new AtomicBoolean(false);
//        CountDownLatch ldsCountDownLatch = new CountDownLatch(1);
////        // support multi-consumer
//        Consumer<Map<String, ListenerResult>> consumer = (listenerResult) -> {
//            isLdsReConnect.set(true);
//            ldsCountDownLatch.countDown();
//        };
//        ldsMap.compute(new HashSet<>(), (k, v) -> {
//            if (v == null) {
//                v = new ArrayList<>();
//            }
//            v.add(consumer);
//            // support multi-consumer
//            return v;
//        });
//        Assertions.assertFalse(isLdsReConnect.get());
//        ldsProtocolMock.setConsumerObserveMap(ldsMap);
//        try {
//            triggerConsumerObserveMapListener(listenerResult, ldsProtocolMock);
//            ldsProtocolMock.getResponseObserve().onError(new RuntimeException());
//            ldsCountDownLatch.await();
//            Assertions.assertTrue(isLdsReConnect.get());
//        } catch (Exception e) {
//            Assertions.assertTrue(isLdsReConnect.get());
//        }
//
//        //mock rds reconnnect
//        Map<Set<String>, List<Consumer<Map<String, RouteResult>>>> rdsMap = new HashMap<>();
//        AtomicBoolean isRdsReConnect = new AtomicBoolean(false);
//        CountDownLatch rdsCountDownLatch = new CountDownLatch(1);
//        // support multi-consumer
//        Consumer<Map<String, RouteResult>> rdsConsumer = (routeResult) -> {
//            isRdsReConnect.set(true);
//            rdsCountDownLatch.countDown();
//        };
//        rdsMap.compute(new HashSet<>(), (k, v) -> {
//            if (v == null) {
//                v = new ArrayList<>();
//            }
//            v.add(rdsConsumer);
//            // support multi-consumer
//            return v;
//        });
//        Assertions.assertFalse(isRdsReConnect.get());
//        rdsProtocolMock.setConsumerObserveMap(rdsMap);
//        try {
//            triggerConsumerObserveMapListener(routeResult, rdsProtocolMock);
//            rdsProtocolMock.getResponseObserve().onError(new RuntimeException());
//            rdsCountDownLatch.await();
//            Assertions.assertTrue(isRdsReConnect.get());
//        } catch (Exception e) {
//            Assertions.assertTrue(isRdsReConnect.get());
//        }
//
//        // mock eds
//        Map<Set<String>, List<Consumer<Map<String, EndpointResult>>>> edsMap = new HashMap<>();
//        AtomicBoolean isEdsReConnect = new AtomicBoolean(false);
//        CountDownLatch edsCountDownLatch = new CountDownLatch(1);
//        // support multi-consumer
//        Consumer<Map<String, EndpointResult>> edsConsumer = (routeResult) -> {
//            isEdsReConnect.set(true);
//            edsCountDownLatch.countDown();
//        };
//        edsMap.compute(new HashSet<>(), (k, v) -> {
//            if (v == null) {
//                v = new ArrayList<>();
//            }
//            v.add(edsConsumer);
//            // support multi-consumer
//            return v;
//        });
//        Assertions.assertFalse(isEdsReConnect.get());
//        edsProtocolMock.setConsumerObserveMap(edsMap);
//        try {
//            triggerConsumerObserveMapListener(endpointResult, edsProtocolMock);
//            edsProtocolMock.getResponseObserve().onError(new RuntimeException());
//            edsCountDownLatch.await();
//            Assertions.assertTrue(isEdsReConnect.get());
//        } catch (Exception e) {
//            Assertions.assertTrue(isEdsReConnect.get());
//        }
//    }
//
//
//    @Test
//    public void testMultiConsumer() {
//        StreamObserver<DiscoveryRequest> requestStreamObserver = mock(StreamObserver.class);
//        when(xdsChannel.createDeltaDiscoveryRequest(any(StreamObserver.class))).thenReturn(requestStreamObserver);
//        doNothing().when(requestStreamObserver).onNext(any());
//        MockedConstruction<CompletableFuture> ldsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
//            when(mock.get()).thenReturn(listenerResult);
//        });
//
//        Map<Set<String>, List<Consumer<Map<String, ListenerResult>>>> ldsMap = new HashMap<>();
//        AtomicBoolean ldsIsFirstConsumerInvoke = new AtomicBoolean(false);
//        AtomicBoolean ldsIsSecondConsumerInvoke = new AtomicBoolean(false);
//        CountDownLatch ldsCountDownLatch = new CountDownLatch(2);
//
//        // support repeat consumer
//        Consumer<Map<String, ListenerResult>> ldsFirstConsumer = (listenerResult) -> {
//            ldsIsFirstConsumerInvoke.set(true);
//            ldsCountDownLatch.countDown();
//        };
//        Consumer<Map<String, ListenerResult>> ldsSecondConsumer = (listenerResult) -> {
//            ldsIsSecondConsumerInvoke.set(true);
//            ldsCountDownLatch.countDown();
//        };
//        Set<String> ldsResourceNames = new HashSet<>();
//        ldsResourceNames.add(ldsProtocolMock.emptyResourceName);
//
//        ldsMap.computeIfAbsent(ldsResourceNames, (key) -> {
//            List<Consumer<Map<String, ListenerResult>>> consumers = new ArrayList<>();
//            consumers.add(ldsFirstConsumer);
//            consumers.add(ldsSecondConsumer);
//            return consumers;
//        });
//        Assertions.assertFalse(ldsIsFirstConsumerInvoke.get() || ldsIsSecondConsumerInvoke.get());
//        ldsProtocolMock.setConsumerObserveMap(ldsMap);
//
//        Map<String, ListenerResult> oldLdsResult = new HashMap<>();
//        Map<String, ListenerResult> newLdsResult = new HashMap<>();
//
//        oldLdsResult.put("emptyResourcesName1", new ListenerResult(routeConfigNames));
//        newLdsResult.put("emptyResourcesName", new ListenerResult(routeConfigNames));
//        newLdsResult.put("emptyResourcesName", new ListenerResult(routeConfigNames));
//        try {
//            ldsProtocolMock.getResponseObserve().discoveryResponseListener(oldLdsResult, newLdsResult);
//            ldsCountDownLatch.await();
//            Assertions.assertTrue(ldsIsFirstConsumerInvoke.get() && ldsIsSecondConsumerInvoke.get());
//        } catch (Exception e) {
//            Assertions.assertTrue(ldsIsFirstConsumerInvoke.get() && ldsIsSecondConsumerInvoke.get());
//        } finally {
//            ldsMocked.close();
//        }
//
//        // mock rds
//        MockedConstruction<CompletableFuture> rdsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
//            when(mock.get()).thenReturn(routeResult);
//        });
//
//        Map<Set<String>, List<Consumer<Map<String, RouteResult>>>> rdsMap = new HashMap<>();
//        AtomicBoolean rdsIsFirstConsumerInvoke = new AtomicBoolean(false);
//        AtomicBoolean rdsIsSecondConsumerInvoke = new AtomicBoolean(false);
//        CountDownLatch rdsCountDownLatch = new CountDownLatch(2);
//
//        // support repeat consumer
//        Consumer<Map<String, RouteResult>> rdsFirstConsumer = (routeResult) -> {
//            rdsIsFirstConsumerInvoke.set(true);
//            rdsCountDownLatch.countDown();
//        };
//        Consumer<Map<String, RouteResult>> rdsSecondConsumer = (routeResult) -> {
//            rdsIsSecondConsumerInvoke.set(true);
//            rdsCountDownLatch.countDown();
//        };
//        rdsMap.computeIfAbsent(routeConfigNames, (key) -> {
//            List<Consumer<Map<String, RouteResult>>> consumers = new ArrayList<>();
//            consumers.add(rdsFirstConsumer);
//            consumers.add(rdsSecondConsumer);
//            return consumers;
//        });
//        Assertions.assertFalse(rdsIsFirstConsumerInvoke.get() || rdsIsSecondConsumerInvoke.get());
//        rdsProtocolMock.setConsumerObserveMap(rdsMap);
//
//        Map<String, RouteResult> oldRdsResult = new HashMap<>();
//        Map<String, RouteResult> newRdsResult = new HashMap<>();
//
//        oldRdsResult.put("15013", new RouteResult(domainMap));
//        newRdsResult.put("15014", new RouteResult(domainMap));
//        newRdsResult.put("15014", new RouteResult(domainMap));
//
//        try {
//            rdsProtocolMock.getResponseObserve().discoveryResponseListener(oldRdsResult, newRdsResult);
//            rdsCountDownLatch.await();
//            Assertions.assertTrue(rdsIsFirstConsumerInvoke.get() && rdsIsSecondConsumerInvoke.get());
//        } catch (Exception e) {
//            Assertions.assertTrue(rdsIsSecondConsumerInvoke.get() && rdsIsSecondConsumerInvoke.get());
//        } finally {
//            rdsMocked.close();
//        }
//
//        // mock eds
//        MockedConstruction<CompletableFuture> edsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
//            when(mock.get()).thenReturn(endpointResult);
//        });
//
//        Map<Set<String>, List<Consumer<Map<String, EndpointResult>>>> edsMap = new HashMap<>();
//        AtomicBoolean edsIsFirstConsumerInvoke = new AtomicBoolean(false);
//        AtomicBoolean edsIsSecondConsumerInvoke = new AtomicBoolean(false);
//        CountDownLatch edsCountDownLatch = new CountDownLatch(2);
//
//        // support repeat consumer
//        Consumer<Map<String, EndpointResult>> edsFirstConsumer = (routeResult) -> {
//            edsIsFirstConsumerInvoke.set(true);
//            edsCountDownLatch.countDown();
//        };
//        Consumer<Map<String, EndpointResult>> edsSecondConsumer = (routeResult) -> {
//            edsIsSecondConsumerInvoke.set(true);
//            edsCountDownLatch.countDown();
//        };
//        edsMap.computeIfAbsent(cluster, (key) -> {
//            List<Consumer<Map<String, EndpointResult>>> consumers = new ArrayList<>();
//            consumers.add(edsFirstConsumer);
//            consumers.add(edsSecondConsumer);
//            return consumers;
//        });
//        Assertions.assertFalse(edsIsFirstConsumerInvoke.get() || edsIsSecondConsumerInvoke.get());
//        edsProtocolMock.setConsumerObserveMap(edsMap);
//
//        Map<String, EndpointResult> oldEdsResult = new HashMap<>();
//        Map<String, EndpointResult> newEdsResult = new HashMap<>();
//
//        oldEdsResult.put("dubbo-samples-provider2", new EndpointResult(endpoints));
//        newEdsResult.put("dubbo-samples-provider", new EndpointResult(endpoints));
//        newEdsResult.put("dubbo-samples-provider", new EndpointResult(endpoints));
//
//        try {
//            edsProtocolMock.getResponseObserve().discoveryResponseListener(oldEdsResult, newEdsResult);
//            edsCountDownLatch.await();
//            Assertions.assertTrue(edsIsFirstConsumerInvoke.get() && edsIsSecondConsumerInvoke.get());
//        } catch (Exception e) {
//            Assertions.assertTrue(edsIsFirstConsumerInvoke.get() && edsIsSecondConsumerInvoke.get());
//        } finally {
//            edsMocked.close();
////            mockedStatic.close();
//        }
//    }
//
//    @Test
//    void testResponseObserver() {
//        //mock lds
//        Map<Set<String>, List<Consumer<Map<String, ListenerResult>>>> ldsMap = new HashMap<>();
//        AtomicBoolean ldsIsFirstConsumerInvoke = new AtomicBoolean(false);
//
//        // support repeat consumer
//        Consumer<Map<String, ListenerResult>> ldsFirstConsumer = (listenerResult) -> {
//            ldsIsFirstConsumerInvoke.set(true);
//        };
//        Set<String> ldsResourceNames = new HashSet<>();
//        ldsResourceNames.add(ldsProtocolMock.emptyResourceName);
//
//        ldsMap.computeIfAbsent(ldsResourceNames, (key) -> {
//            List<Consumer<Map<String, ListenerResult>>> consumers = new ArrayList<>();
//            consumers.add(ldsFirstConsumer);
//            return consumers;
//        });
//        Assertions.assertFalse(ldsIsFirstConsumerInvoke.get());
//        ldsProtocolMock.setConsumerObserveMap(ldsMap);
//
//        Map<String, ListenerResult> oldLdsResult = new HashMap<>();
//        Map<String, ListenerResult> newLdsResult = new HashMap<>();
//
//        oldLdsResult.put("emptyResourcesName1", new ListenerResult(routeConfigNames));
//        newLdsResult.put("emptyResourcesName1", new ListenerResult(routeConfigNames));
//        try {
//            ldsProtocolMock.getResponseObserve().discoveryResponseListener(oldLdsResult, newLdsResult);
//            Assertions.assertFalse(ldsIsFirstConsumerInvoke.get());
//
//            newLdsResult.put("emptyResourcesName", new ListenerResult(routeConfigNames));
//            ldsProtocolMock.getResponseObserve().discoveryResponseListener(oldLdsResult, newLdsResult);
//            Assertions.assertTrue(ldsIsFirstConsumerInvoke.get());
//
//        } catch (Exception e) {
//            Assertions.assertTrue(ldsIsFirstConsumerInvoke.get());
//        }
//
//
//        // mock rds
//        Map<Set<String>, List<Consumer<Map<String, RouteResult>>>> rdsMap = new HashMap<>();
//        AtomicBoolean rdsIsFirstConsumerInvoke = new AtomicBoolean(false);
//
//        // support repeat consumer
//        Consumer<Map<String, RouteResult>> rdsFirstConsumer = (routeResult) -> {
//            rdsIsFirstConsumerInvoke.set(true);
//        };
//        rdsMap.computeIfAbsent(routeConfigNames, (key) -> {
//            List<Consumer<Map<String, RouteResult>>> consumers = new ArrayList<>();
//            consumers.add(rdsFirstConsumer);
//            return consumers;
//        });
//        rdsProtocolMock.setConsumerObserveMap(rdsMap);
//
//        Map<String, RouteResult> oldRdsResult = new HashMap<>();
//        Map<String, RouteResult> newRdsResult = new HashMap<>();
//
//        oldRdsResult.put("15013", new RouteResult(domainMap));
//        newRdsResult.put("15013", new RouteResult(domainMap));
//
//        try {
//            Assertions.assertFalse(rdsIsFirstConsumerInvoke.get());
//            rdsProtocolMock.getResponseObserve().discoveryResponseListener(oldRdsResult, newRdsResult);
//            newRdsResult.put("15014", new RouteResult(domainMap));
//            rdsProtocolMock.getResponseObserve().discoveryResponseListener(oldRdsResult, newRdsResult);
//            Assertions.assertTrue(rdsIsFirstConsumerInvoke.get());
//
//            Assertions.assertTrue(rdsIsFirstConsumerInvoke.get());
//        } catch (Exception e) {
//            Assertions.assertTrue(rdsIsFirstConsumerInvoke.get());
//        }
//
//        // mock eds
//        Map<Set<String>, List<Consumer<Map<String, EndpointResult>>>> edsMap = new HashMap<>();
//        AtomicBoolean edsIsFirstConsumerInvoke = new AtomicBoolean(false);
//
//        // support repeat consumer
//        Consumer<Map<String, EndpointResult>> edsFirstConsumer = (routeResult) -> {
//            edsIsFirstConsumerInvoke.set(true);
//        };
//        edsMap.computeIfAbsent(cluster, (key) -> {
//            List<Consumer<Map<String, EndpointResult>>> consumers = new ArrayList<>();
//            consumers.add(edsFirstConsumer);
//            return consumers;
//        });
//        Assertions.assertFalse(edsIsFirstConsumerInvoke.get());
//        edsProtocolMock.setConsumerObserveMap(edsMap);
//
//        Map<String, EndpointResult> oldEdsResult = new HashMap<>();
//        Map<String, EndpointResult> newEdsResult = new HashMap<>();
//
//        oldEdsResult.put("dubbo-samples-provider2", new EndpointResult(endpoints));
//        newEdsResult.put("dubbo-samples-provider2", new EndpointResult(endpoints));
//
//        try {
//            edsProtocolMock.getResponseObserve().discoveryResponseListener(oldEdsResult, newEdsResult);
//            Assertions.assertFalse(edsIsFirstConsumerInvoke.get());
//            newEdsResult.put("dubbo-samples-provider", new EndpointResult(endpoints));
//            edsProtocolMock.getResponseObserve().discoveryResponseListener(oldEdsResult, newEdsResult);
//            Assertions.assertTrue(edsIsFirstConsumerInvoke.get());
//        } catch (Exception e) {
//            Assertions.assertTrue(edsIsFirstConsumerInvoke.get());
//        }
//    }
//
//    private <T> void triggerConsumerObserveMapListener(Map<String, T> resultMap, AbstractProtocol protocol) {
//        CompletableFuture.runAsync(() -> {
//            while (true) {
//                Map<Set<String>, List<Consumer<Map<String, T>>>> map = protocol.getConsumerObserveMap();
//                if (map != null && map.size() > 1) {
//                    map.forEach((k, v) -> v.forEach(o -> o.accept(resultMap)));
//                    break;
//                }
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    break;
//                }
//            }
//        });
//    };
//}
