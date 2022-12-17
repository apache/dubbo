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
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.registry.xds.util.NodeBuilder;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.registry.xds.util.PilotExchangerMock;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.registry.xds.util.protocol.AbstractProtocol;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;
import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
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
    private RouteResult routeResult;

    private EndpointResult endpointResult;


    private final AtomicBoolean isRdsObserve = new AtomicBoolean(false);
    private final HashSet<String> domainObserveRequest = new HashSet<>();

    private final Map<String, Set<Consumer<Set<Endpoint>>>> domainObserveConsumer = new ConcurrentHashMap<>();

    private ApplicationModel applicationModel;
    private URL url;

    private Node node;

    public DsProtocolTest() {
    }

    //    @BeforeEach
//    public void setUp() {
//        // mock channel
//        URL url = URL.valueOf("xds://istiod.istio-system.svc:15012");
//        xdsChannel = mock(XdsChannel.class);
//        domainObserveConsumer.put("dubbo-samples-provider", new HashSet<>());
//        ManagedChannel managedChannel = mock(ManagedChannel.class);
//        when(xdsChannel.getChannel()).thenReturn(managedChannel);
//        StreamObserver<DiscoveryRequest> streamObserver = mock(StreamObserver.class);
//        when(xdsChannel.createDeltaDiscoveryRequest(any())).thenReturn(streamObserver);
//        Assertions.assertNotNull(xdsChannel.getChannel());
//
//        // mock lds rds eds
//        Node node = mock(Node.class);
//        ApplicationModel applicationModel = ApplicationModel.defaultModel();
//        LdsProtocol ldsProtocolMock = spy(new LdsProtocol(xdsChannel, node, 10, applicationModel));
//        this.rdsProtocolMock = spy(new RdsProtocol(xdsChannel, node, 10, applicationModel));
//        this.edsProtocolMock = spy(new EdsProtocol(xdsChannel, node, 10, applicationModel));
//
//        Set<String> routeConfigNames = new HashSet<>();
//        routeConfigNames.add("kubernetes-dashboard.kubernetes-dashboard.svc.cluster.local:443");
//        this.listenerResult = spy(new ListenerResult(routeConfigNames));
//        doReturn(new ListenerResult(routeConfigNames)).when(ldsProtocolMock.getListeners());
//
//
//
//
//        Map<String, Set<String>> domainMap = new HashMap<>();
//        Set<String> domainValue = new HashSet<>();
//        domainValue.add("outbound|15014||istiod.istio-system.svc.cluster.local");
//        domainMap.put("istiod.istio-system.svc", domainValue);
//
//        Map<String, Set<String>> domainNewMap = new HashMap<>();
//        Set<String> domainNewValue = new HashSet<>();
//        domainNewValue.add("outbound|15014||istiod.istio-system.svc.cluster.local-new");
//        domainNewMap.put("istiod.istio-system.svc", domainValue);
//        this.routeResult = new RouteResult(domainNewMap);
//
//        // Observer RDS update
//        if (CollectionUtils.isNotEmpty(listenerResult.getRouteConfigNames())) {
//            createRouteObserve();
//            isRdsObserve.set(true);
//        }
//
//        doReturn(new RouteResult(null)).when(rdsProtocolMock).getCacheResource(any());
//        doReturn(new ListenerResult(routeConfigNames)).when(ldsProtocolMock).getResource(Collections.emptySet());
//        doReturn(new ListenerResult(routeConfigNames)).when(ldsProtocolMock).getCacheResource(Collections.emptySet());
//        ldsProtocolMock.observeListeners((newListener) -> {
//            // update local cache
//            if (!newListener.equals(listenerResult)) {
//                this.listenerResult = newListener;
//                // update RDS observation
//                if (isRdsObserve.get()) {
//                    createRouteObserve();
//                }
//            }
//        });
//    }

    @BeforeEach
    public void setUp() {
        this.url = URL.valueOf("xds://istiod.istio-system.svc:15012");
        mockStatic(NodeBuilder.class);
        this.node = mock(Node.class);
        when(NodeBuilder.build()).thenReturn(node);

        this.applicationModel = ApplicationModel.defaultModel();
        xdsChannel = mock(XdsChannel.class);

        this.ldsProtocolMock = spy(new LdsProtocolMock(xdsChannel, node, 10, applicationModel));
        this.rdsProtocolMock = spy(new RdsProtocolMock(xdsChannel, node, 10, applicationModel));
        this.edsProtocolMock = spy(new EdsProtocolMock(xdsChannel, node, 10, applicationModel));

        // mock lister result
        this.routeConfigNames = new HashSet<>();
        routeConfigNames.add("istiod.istio-system.svc");
        this.listenerResult = spy(new ListenerResult(routeConfigNames));

        // mock route result
        Map<String, Set<String>> domainMap = new HashMap<>();
        Set<String> domainValue = new HashSet<>();
        domainValue.add("outbound|15014||istiod.istio-system.svc.cluster.local");
        domainMap.put("istiod.istio-system.svc", domainValue);
        this.routeResult = new RouteResult(domainMap);

        // mock eds result
        Endpoint endpoint = new Endpoint();
        endpoint.setWeight(1);
        endpoint.setHealthy(true);
        endpoint.setPortValue(50051);
        endpoint.setAddress("10.1.1.67");
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.add(endpoint);
        this.endpointResult = new EndpointResult(endpoints);
//        doReturn(ldsProtocolMock.getResource(routeConfigNames)).when(ldsProtocolMock.getListeners());
//        when(ldsProtocolMock.getListeners()).thenReturn(listenerResult);
    }


    @Test
    void testGetResource() {
        //mock rds getResource
        StreamObserver<DiscoveryRequest> requestStreamObserver = mock(StreamObserver.class);

        // mock lds getResource
        when(xdsChannel.createDeltaDiscoveryRequest(any(StreamObserver.class))).thenReturn(requestStreamObserver);
        doNothing().when(requestStreamObserver).onNext(any());
        MockedConstruction<CompletableFuture> ldsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(listenerResult);
        });
        ListenerResult ldsResult = ldsProtocolMock.getResource(null);

        verify(ldsProtocolMock, times(0)).isExistResource(null);
        ldsMocked.close();

        MockedConstruction<CompletableFuture> rdsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(routeResult);
        });

        RouteResult rdsResult = rdsProtocolMock.getResource(routeConfigNames);
        verify(rdsProtocolMock, times(1)).isExistResource(routeConfigNames);
        Assertions.assertEquals(rdsProtocolMock.isExistResource(routeConfigNames), false);

        Map<String, Set<String>> domainMap = new HashMap<>();
        Set<String> domainValue = new HashSet<>();
        domainValue.add("outbound|15014||istiod.istio-system.svc.cluster.local");
        domainMap.put("istiod.istio-system.svc", domainValue);

        rdsProtocolMock.getResourcesMap().put("istiod.istio-system.svc", domainMap);

        Assertions.assertEquals(rdsProtocolMock.isExistResource(routeConfigNames), true);
        Assertions.assertEquals(rdsProtocolMock.getCacheResource(routeConfigNames), routeResult);
        rdsMocked.close();

        //mock eds getResource
        MockedConstruction<CompletableFuture> edsMocked = mockConstruction(CompletableFuture.class, (mock, context) -> {
            when(mock.get()).thenReturn(endpointResult);
        });

        EndpointResult edsResult = edsProtocolMock.getResource(routeConfigNames);
        verify(edsProtocolMock, times(1)).isExistResource(routeConfigNames);
        Assertions.assertEquals(rdsProtocolMock.isExistResource(routeConfigNames), false);
        edsMocked.close();
    }

    @Test
    void testCache() {

        System.out.println("hello");
    }

    @Test
    void observeDsReConnect() {

    }

    @Test
    void resourceInCache() {

    }

    @Test
    void resourceNotInCache() {

    }

//    private void doObserveEndpoints(String domain) {
//        Set<String> router = routeResult.searchDomain(domain);
//        // if router is empty, do nothing
//        // observation will be created when RDS updates
//        if (CollectionUtils.isNotEmpty(router)) {
//            edsProtocolMock.observeResource(
//                router,
//                endpointResult ->
//                    // notify consumers
//                    domainObserveConsumer.get(domain).forEach(
//                        consumer1 -> consumer1.accept(endpointResult.getEndpoints())));
//            domainObserveRequest.add(domain);
//        }
//    }
}
