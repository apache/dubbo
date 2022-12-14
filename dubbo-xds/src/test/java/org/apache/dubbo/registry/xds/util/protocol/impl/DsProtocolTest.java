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
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
import org.apache.dubbo.registry.xds.util.protocol.message.RouteResult;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class DsProtocolTest {
    private XdsChannel xdsChannel;

    private RdsProtocol rdsProtocolMock;

    private EdsProtocol edsProtocolMock;

    private ListenerResult listenerResult;

    private RouteResult routeResult;

    private final AtomicBoolean isRdsObserve = new AtomicBoolean(false);
    private final HashSet<String> domainObserveRequest = new HashSet<>();

    private final Map<String, Set<Consumer<Set<Endpoint>>>> domainObserveConsumer = new ConcurrentHashMap<>();

    public DsProtocolTest() {
    }

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        URL url = URL.valueOf("xds://istiod.istio-system.svc:15012");
        xdsChannel = mock(XdsChannel.class);
        domainObserveConsumer.put("dubbo-samples-provider", new HashSet<>());
        ManagedChannel managedChannel = mock(ManagedChannel.class);
        when(xdsChannel.getChannel()).thenReturn(managedChannel);
        StreamObserver<DiscoveryRequest> streamObserver = mock(StreamObserver.class);
        when(xdsChannel.createDeltaDiscoveryRequest(any())).thenReturn(streamObserver);
        Assertions.assertNotNull(xdsChannel.getChannel());
        int pollingTimeout = url.getParameter("pollingTimeout", 10);
        Node node = mock(Node.class);
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        LdsProtocol ldsProtocolMock = spy(new LdsProtocol(xdsChannel, node, 10, applicationModel));
        this.rdsProtocolMock = spy(new RdsProtocol(xdsChannel, node, 10, applicationModel));
        this.edsProtocolMock = spy(new EdsProtocol(xdsChannel, node, 10, applicationModel));

        Set<String> routeConfigNames = new HashSet<>();
        routeConfigNames.add("kubernetes-dashboard.kubernetes-dashboard.svc.cluster.local:443");
        this.listenerResult = spy(new ListenerResult(routeConfigNames));

        Map<String, Set<String>> domainMap = new HashMap<>();
        Set<String> domainValue = new HashSet<>();
        domainValue.add("outbound|15014||istiod.istio-system.svc.cluster.local");
        domainMap.put("istiod.istio-system.svc", domainValue);

        Map<String, Set<String>> domainNewMap = new HashMap<>();
        Set<String> domainNewValue = new HashSet<>();
        domainNewValue.add("outbound|15014||istiod.istio-system.svc.cluster.local-new");
        domainNewMap.put("istiod.istio-system.svc", domainValue);
        this.routeResult = new RouteResult(domainNewMap);

        // Observer RDS update
        if (CollectionUtils.isNotEmpty(listenerResult.getRouteConfigNames())) {
            createRouteObserve();
            isRdsObserve.set(true);
        }

        doReturn(new RouteResult(null)).when(rdsProtocolMock).getCacheResource(any());
        doReturn(new ListenerResult(routeConfigNames)).when(ldsProtocolMock).getResource(Collections.emptySet());
        doReturn(new ListenerResult(routeConfigNames)).when(ldsProtocolMock).getCacheResource(Collections.emptySet());
        ldsProtocolMock.observeListeners((newListener) -> {
            // update local cache
            if (!newListener.equals(listenerResult)) {
                this.listenerResult = newListener;
                // update RDS observation
                if (isRdsObserve.get()) {
                    createRouteObserve();
                }
            }
        });
    }

    void createRouteObserve() {
        doReturn(new RouteResult(null)).when(rdsProtocolMock).getResource(any());
//        doReturn()
        this.rdsProtocolMock.observeResource(listenerResult.getRouteConfigNames(), (newResult) -> {
            // check if observed domain update ( will update endpoint observation )
            domainObserveConsumer.forEach((domain, consumer) -> {
                Set<String> newRoute = newResult.searchDomain(domain);
                if (!routeResult.searchDomain(domain).equals(newRoute)) {
                    // routers in observed domain has been updated
//                    Long domainRequest = domainObserveRequest.get(domain);
                    // router list is empty when observeEndpoints() called and domainRequest has not been created yet
                    // create new observation
                    doObserveEndpoints(domain);
                }
            });
        });
    }

    @Test
    void observeDsConnect() {
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

    private void doObserveEndpoints(String domain) {
        Set<String> router = routeResult.searchDomain(domain);
        // if router is empty, do nothing
        // observation will be created when RDS updates
        if (CollectionUtils.isNotEmpty(router)) {
            edsProtocolMock.observeResource(
                router,
                endpointResult ->
                    // notify consumers
                    domainObserveConsumer.get(domain).forEach(
                        consumer1 -> consumer1.accept(endpointResult.getEndpoints())));
            domainObserveRequest.add(domain);
        }
    }
}
