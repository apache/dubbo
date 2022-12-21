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

package org.apache.dubbo.registry.xds.util.protocol.impl;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.xds.XdsCertificateSigner;
import org.apache.dubbo.registry.xds.util.NodeBuilder;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;
import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;


public class DsProtocolTest {
    private XdsChannel xdsChannel;

    private LdsProtocolMock ldsProtocolMock;
    private RdsProtocolMock rdsProtocolMock;

    private EdsProtocolMock edsProtocolMock;

    private ListenerResult listenerResult;

    private Set<String> routeConfigNames;

    private Set<String> cluster;
    private RouteResult routeResult;

    private Set<Endpoint> endpoints;
    private EndpointResult endpointResult;

    private Map<String, Set<String>> domainMap;

    private ApplicationModel applicationModel;
    private URL url;

    private Node node;

    public DsProtocolTest() {
    }

    @BeforeEach
    public void setUp() {
        this.url = spy(URL.valueOf("xds://istiod.istio-system.svc:15012?secure=plaintext"));
        MockedStatic<NodeBuilder> mockedStatic = mockStatic(NodeBuilder.class);
        this.node = mock(Node.class);
        when(NodeBuilder.build()).thenReturn(node);

        this.applicationModel = ApplicationModel.defaultModel();
        xdsChannel = mock(XdsChannel.class);
        when(xdsChannel.getUrl()).thenReturn(url);

        this.ldsProtocolMock = spy(new LdsProtocolMock(xdsChannel, node, 1, applicationModel));
        this.rdsProtocolMock = spy(new RdsProtocolMock(xdsChannel, node, 1, applicationModel));
        this.edsProtocolMock = spy(new EdsProtocolMock(xdsChannel, node, 1, applicationModel));

        // mock lister result
        this.routeConfigNames = new HashSet<>();
        routeConfigNames.add("istiod.istio-system.svc");
        this.listenerResult = spy(new ListenerResult(routeConfigNames));

        // mock route result
        this.domainMap = new HashMap<>();
        Set<String> domainValue = new HashSet<>();
        domainValue.add("outbound|15014||istiod.istio-system.svc.cluster.local");
        domainMap.put("istiod.istio-system.svc", domainValue);
        this.routeResult = new RouteResult(domainMap);

        // mock eds result
        Set<String> cluster = new HashSet<>();
        cluster.add("dubbo-samples-provider");
        this.cluster = cluster;
        Endpoint endpoint = new Endpoint();
        endpoint.setWeight(1);
        endpoint.setHealthy(true);
        endpoint.setPortValue(50051);
        endpoint.setAddress("10.1.1.67");
        this.endpoints = new HashSet<>();
        endpoints.add(endpoint);
        this.endpointResult = new EndpointResult(endpoints);
        mockedStatic.close();
    }


    @Test
    void testGetResource() {
        StreamObserver<DiscoveryRequest> requestStreamObserver = mock(StreamObserver.class);

        // mock lds getResource
        when(xdsChannel.createDeltaDiscoveryRequest(any(StreamObserver.class))).thenReturn(requestStreamObserver);
        doNothing().when(requestStreamObserver).onNext(any());
        MockedConstruction<CompletableFuture> ldsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(listenerResult);
        });
        ListenerResult ldsResult = ldsProtocolMock.getResource(null);

        ldsMocked.close();

        verify(ldsProtocolMock, times(0)).isExistResource(null);


        // mock rds getResource
        MockedConstruction<CompletableFuture> rdsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(null);
        });

        RouteResult rdsResult = rdsProtocolMock.getResource(routeConfigNames);
        verify(rdsProtocolMock, times(1)).isExistResource(routeConfigNames);
        Assertions.assertFalse(rdsProtocolMock.isExistResource(routeConfigNames));

        Assertions.assertNull(rdsResult);
        Assertions.assertFalse(rdsProtocolMock.isExistResource(routeConfigNames));
        Assertions.assertEquals(rdsProtocolMock.getCacheResource(routeConfigNames), rdsResult);
        rdsProtocolMock.getResourcesMap().put("istiod.istio-system.svc", domainMap);
        rdsResult = rdsProtocolMock.getResource(routeConfigNames);
        Assertions.assertNotNull(rdsResult);
        Assertions.assertTrue(rdsProtocolMock.isExistResource(routeConfigNames));
        Assertions.assertEquals(rdsProtocolMock.getCacheResource(routeConfigNames), rdsResult);
        rdsMocked.close();

        //mock eds getResource
        MockedConstruction<CompletableFuture> edsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(null);
        });

        EndpointResult edsResult = edsProtocolMock.getResource(cluster);
        verify(edsProtocolMock, times(1)).isExistResource(cluster);
        Assertions.assertNull(edsResult);
        Assertions.assertFalse(edsProtocolMock.isExistResource(cluster));
        edsProtocolMock.getResourcesMap().put("dubbo-samples-provider", endpoints);
        edsResult = edsProtocolMock.getResource(cluster);
        Assertions.assertNotNull(edsResult);
        Assertions.assertTrue(edsProtocolMock.isExistResource(cluster));
        Assertions.assertEquals(edsProtocolMock.getCacheResource(cluster), endpointResult);
        edsMocked.close();
    }

    @Test
    void observeDsReConnect() {
        // mock lds reconnect
        StreamObserver<DiscoveryRequest> requestStreamObserver = mock(StreamObserver.class);
        when(xdsChannel.createDeltaDiscoveryRequest(any(StreamObserver.class))).thenReturn(requestStreamObserver);
        doNothing().when(requestStreamObserver).onNext(any());

        MockedConstruction<CompletableFuture> ldsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(listenerResult);
        });

        ldsProtocolMock.setObserveResourcesName(new HashSet<>());

        Map<Set<String>, List<Consumer<ListenerResult>>> ldsMap = new HashMap<>();
        AtomicBoolean isLdsReConnect = new AtomicBoolean(false);
        CountDownLatch ldsCountDownLatch = new CountDownLatch(1);
        // support multi-consumer
        Consumer<ListenerResult> consumer = (listenerResult) -> {
            isLdsReConnect.set(true);
            ldsCountDownLatch.countDown();
        };
        when(ldsProtocolMock.getResource(any())).thenReturn(listenerResult);
        ldsMap.compute(new HashSet<>(), (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(consumer);
            // support multi-consumer
            return v;
        });
        Assertions.assertFalse(isLdsReConnect.get());
        ldsProtocolMock.setConsumerObserveMap(ldsMap);
        mockStatic(NettyChannelBuilder.class);

        // mock managedChannel
        ManagedChannel managedChannel = mock(ManagedChannel.class);
        NettyChannelBuilder nettyChannelBuilder = mock(NettyChannelBuilder.class);
        when(NettyChannelBuilder.forAddress(url.getHost(), url.getPort())).thenReturn(nettyChannelBuilder);
        when(nettyChannelBuilder.usePlaintext()).thenReturn(nettyChannelBuilder);
        when(nettyChannelBuilder.build()).thenReturn(managedChannel);
        when(xdsChannel.getChannel()).thenReturn(managedChannel);

        try {
            ldsProtocolMock.getResponseObserve().onError(new RuntimeException());
            ldsCountDownLatch.await();
            Assertions.assertTrue(isLdsReConnect.get());
        } catch (Exception e) {
            Assertions.assertTrue(isLdsReConnect.get());
        } finally {
            ldsMocked.close();
        }

        //mock rds reconnnect
        MockedConstruction<CompletableFuture> rdsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(routeConfigNames);
        });
        rdsProtocolMock.setObserveResourcesName(new HashSet<>());
        Map<Set<String>, List<Consumer<RouteResult>>> rdsMap = new HashMap<>();
        AtomicBoolean isRdsReConnect = new AtomicBoolean(false);
        CountDownLatch rdsCountDownLatch = new CountDownLatch(1);
        // support multi-consumer
        Consumer<RouteResult> rdsConsumer = (routeResult) -> {
            isRdsReConnect.set(true);
            rdsCountDownLatch.countDown();
        };
        when(rdsProtocolMock.getResource(any())).thenReturn(routeResult);
        rdsMap.compute(new HashSet<>(), (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(rdsConsumer);
            // support multi-consumer
            return v;
        });
        Assertions.assertFalse(isRdsReConnect.get());
        rdsProtocolMock.setConsumerObserveMap(rdsMap);
        try {
            rdsProtocolMock.getResponseObserve().onError(new RuntimeException());
            rdsCountDownLatch.await();
            Assertions.assertTrue(isRdsReConnect.get());
        } catch (Exception e) {
            Assertions.assertTrue(isRdsReConnect.get());
        } finally {
            rdsMocked.close();
        }

        // mock eds
        MockedConstruction<CompletableFuture> edsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(endpointResult);
        });
        edsProtocolMock.setObserveResourcesName(new HashSet<>());
        Map<Set<String>, List<Consumer<EndpointResult>>> edsMap = new HashMap<>();
        AtomicBoolean isEdsReConnect = new AtomicBoolean(false);
        CountDownLatch edsCountDownLatch = new CountDownLatch(1);
        // support multi-consumer
        Consumer<RouteResult> edsConsumer = (routeResult) -> {
            isEdsReConnect.set(true);
            edsCountDownLatch.countDown();
        };
        when(edsProtocolMock.getResource(any())).thenReturn(endpointResult);
        rdsMap.compute(new HashSet<>(), (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(edsConsumer);
            // support multi-consumer
            return v;
        });
        Assertions.assertFalse(isEdsReConnect.get());
        edsProtocolMock.setConsumerObserveMap(edsMap);
        try {
            edsProtocolMock.getResponseObserve().onError(new RuntimeException());
            edsCountDownLatch.await();
            Assertions.assertTrue(isEdsReConnect.get());
        } catch (Exception e) {
            Assertions.assertTrue(isEdsReConnect.get());
        } finally {
            edsMocked.close();
        }
    }

    @Test
    public void testMultiConsumer() {
        StreamObserver<DiscoveryRequest> requestStreamObserver = mock(StreamObserver.class);
        when(xdsChannel.createDeltaDiscoveryRequest(any(StreamObserver.class))).thenReturn(requestStreamObserver);
        doNothing().when(requestStreamObserver).onNext(any());

        MockedConstruction<CompletableFuture> ldsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(listenerResult);
        });

        Map<Set<String>, List<Consumer<ListenerResult>>> ldsMap = new HashMap<>();
        AtomicBoolean ldsIsFirstConsumerInvoke = new AtomicBoolean(false);
        AtomicBoolean ldsIsSecondConsumerInvoke = new AtomicBoolean(false);
        CountDownLatch ldsCountDownLatch = new CountDownLatch(2);

        // support repeat consumer
        Consumer<ListenerResult> ldsFirstConsumer = (listenerResult) -> {
            ldsIsFirstConsumerInvoke.set(true);
            ldsCountDownLatch.countDown();
        };
        Consumer<ListenerResult> ldsSecondConsumer = (listenerResult) -> {
            ldsIsSecondConsumerInvoke.set(true);
            ldsCountDownLatch.countDown();
        };
        ldsMap.computeIfAbsent(new HashSet<>(), (key) -> {
            List<Consumer<ListenerResult>> consumers = new ArrayList<>();
            consumers.add(ldsFirstConsumer);
            consumers.add(ldsSecondConsumer);
            return consumers;
        });
        Assertions.assertFalse(ldsIsFirstConsumerInvoke.get() || ldsIsSecondConsumerInvoke.get());
        ldsProtocolMock.setConsumerObserveMap(spy(ldsMap));
        MockedStatic<NettyChannelBuilder> mockedStatic = mockStatic(NettyChannelBuilder.class);

        ManagedChannel managedChannel = mock(ManagedChannel.class);
        NettyChannelBuilder nettyChannelBuilder = mock(NettyChannelBuilder.class);

        when(NettyChannelBuilder.forAddress(url.getHost(), url.getPort())).thenReturn(nettyChannelBuilder);
        when(nettyChannelBuilder.usePlaintext()).thenReturn(nettyChannelBuilder);

        when(nettyChannelBuilder.build()).thenReturn(managedChannel);

        when(xdsChannel.getChannel()).thenReturn(managedChannel);
        try {
            ldsProtocolMock.observeResource(new HashSet<>(), ldsFirstConsumer, false);
            ldsCountDownLatch.await();
            Assertions.assertTrue(ldsIsFirstConsumerInvoke.get() && ldsIsSecondConsumerInvoke.get());
        } catch (Exception e) {
            Assertions.assertTrue(ldsIsFirstConsumerInvoke.get() && ldsIsSecondConsumerInvoke.get());
        } finally {
            ldsMocked.close();
        }

        // mock rds
        MockedConstruction<CompletableFuture> rdsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(routeResult);
        });

        Map<Set<String>, List<Consumer<RouteResult>>> rdsMap = new HashMap<>();
        AtomicBoolean rdsIsFirstConsumerInvoke = new AtomicBoolean(false);
        AtomicBoolean rdsIsSecondConsumerInvoke = new AtomicBoolean(false);
        CountDownLatch rdsCountDownLatch = new CountDownLatch(2);

        // support repeat consumer
        Consumer<RouteResult> rdsFirstConsumer = (routeResult) -> {
            rdsIsFirstConsumerInvoke.set(true);
            rdsCountDownLatch.countDown();
        };
        Consumer<RouteResult> rdsSecondConsumer = (routeResult) -> {
            rdsIsSecondConsumerInvoke.set(true);
            rdsCountDownLatch.countDown();
        };
        rdsMap.computeIfAbsent(routeConfigNames, (key) -> {
            List<Consumer<RouteResult>> consumers = new ArrayList<>();
            consumers.add(rdsFirstConsumer);
            consumers.add(rdsSecondConsumer);
            return consumers;
        });
        Assertions.assertFalse(rdsIsFirstConsumerInvoke.get() || rdsIsSecondConsumerInvoke.get());
        rdsProtocolMock.setConsumerObserveMap(spy(rdsMap));
        try {
            rdsProtocolMock.observeResource(routeConfigNames, rdsFirstConsumer, false);
            rdsCountDownLatch.await();
            Assertions.assertTrue(rdsIsFirstConsumerInvoke.get() && rdsIsSecondConsumerInvoke.get());
        } catch (Exception e) {
            Assertions.assertTrue(rdsIsSecondConsumerInvoke.get() && rdsIsSecondConsumerInvoke.get());
        } finally {
            rdsMocked.close();
        }

        // mock eds
        MockedConstruction<CompletableFuture> edsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(endpointResult);
        });

        Map<Set<String>, List<Consumer<EndpointResult>>> edsMap = new HashMap<>();
        AtomicBoolean edsIsFirstConsumerInvoke = new AtomicBoolean(false);
        AtomicBoolean edsIsSecondConsumerInvoke = new AtomicBoolean(false);
        CountDownLatch edsCountDownLatch = new CountDownLatch(2);

        // support repeat consumer
        Consumer<EndpointResult> edsFirstConsumer = (routeResult) -> {
            edsIsFirstConsumerInvoke.set(true);
            edsCountDownLatch.countDown();
        };
        Consumer<EndpointResult> edsSecondConsumer = (routeResult) -> {
            edsIsSecondConsumerInvoke.set(true);
            edsCountDownLatch.countDown();
        };
        edsMap.computeIfAbsent(cluster, (key) -> {
            List<Consumer<EndpointResult>> consumers = new ArrayList<>();
            consumers.add(edsFirstConsumer);
            consumers.add(edsSecondConsumer);
            return consumers;
        });
        Assertions.assertFalse(edsIsFirstConsumerInvoke.get() || edsIsSecondConsumerInvoke.get());
        edsProtocolMock.setConsumerObserveMap(spy(edsMap));
        try {
            edsProtocolMock.observeResource(cluster, edsFirstConsumer, false);
            edsCountDownLatch.await();
            Assertions.assertTrue(edsIsFirstConsumerInvoke.get() && edsIsSecondConsumerInvoke.get());
        } catch (Exception e) {
            Assertions.assertTrue(edsIsFirstConsumerInvoke.get() && edsIsSecondConsumerInvoke.get());
        } finally {
            edsMocked.close();
            mockedStatic.close();
        }
    }

    @Test
    void testLockInterupt() {
        // future.get之前 遍历
        // mock lds reconnect
        StreamObserver<DiscoveryRequest> requestStreamObserver = mock(StreamObserver.class);

        // mock lds getResource
        when(xdsChannel.createDeltaDiscoveryRequest(any(StreamObserver.class))).thenReturn(requestStreamObserver);
        doNothing().when(requestStreamObserver).onNext(any());
        MockedConstruction<CompletableFuture> ldsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(listenerResult);
            when(mock.get()).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                    return null;
                }
            });
        });
        ListenerResult ldsResult = ldsProtocolMock.getResource(null);


//        verify(ldsProtocolMock, times(0)).isExistResource(null);

        //decode 时候completefuture不在末尾
    }
}
