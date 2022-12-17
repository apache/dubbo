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

        verify(ldsProtocolMock, times(0)).isExistResource(null);
        ldsMocked.close();

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
    void testDecodeDiscoveryResponse() {

    }
    @Test
    void observeDsReConnect() {

    }
}
