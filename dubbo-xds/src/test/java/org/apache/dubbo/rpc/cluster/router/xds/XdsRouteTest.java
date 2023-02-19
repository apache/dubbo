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
package org.apache.dubbo.rpc.cluster.router.xds;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.xds.util.protocol.message.Endpoint;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.xds.rule.DestinationSubset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.protobuf.UInt32Value;

import io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.config.route.v3.WeightedCluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XdsRouteTest {

    private EdsEndpointManager edsEndpointManager;

    private RdsRouteRuleManager rdsRouteRuleManager;
    private Set<TracingContextProvider> tracingContextProviders;
    private URL url;

    @BeforeEach
    public void setup() {
        edsEndpointManager = Mockito.spy(EdsEndpointManager.class);
        rdsRouteRuleManager = Mockito.spy(RdsRouteRuleManager.class);
        tracingContextProviders = new HashSet<>();

        url = URL.valueOf("test://localhost/DemoInterface");
    }

    private Invoker<Object> createInvoker(String app) {
        URL url = URL.valueOf("dubbo://localhost/DemoInterface?" + (StringUtils.isEmpty(app) ? "" : "remote.application=" + app));
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    private Invoker<Object> createInvoker(String app, String address) {
        URL url = URL.valueOf("dubbo://" + address + "/DemoInterface?" + (StringUtils.isEmpty(app) ? "" : "remote.application=" + app));
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    @Test
    public void testNotifyInvoker() {
        XdsRouter<Object> xdsRouter = new XdsRouter<>(url, rdsRouteRuleManager, edsEndpointManager, true);
        xdsRouter.notify(null);
        assertEquals(0, xdsRouter.getSubscribeApplications().size());

        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(createInvoker(""), createInvoker("app1")));

        xdsRouter.notify(invokers);

        assertEquals(1, xdsRouter.getSubscribeApplications().size());
        assertTrue(xdsRouter.getSubscribeApplications().contains("app1"));
        assertEquals(invokers, xdsRouter.getInvokerList());

        verify(rdsRouteRuleManager, times(1)).subscribeRds("app1", xdsRouter);

        invokers = new BitList<>(Arrays.asList(createInvoker("app2")));
        xdsRouter.notify(invokers);
        verify(rdsRouteRuleManager, times(1)).subscribeRds("app2", xdsRouter);
        verify(rdsRouteRuleManager, times(1)).unSubscribeRds("app1", xdsRouter);
        assertEquals(invokers, xdsRouter.getInvokerList());

        xdsRouter.stop();
        verify(rdsRouteRuleManager, times(1)).unSubscribeRds("app2", xdsRouter);
    }

    @Test
    public void testRuleChange() {
        XdsRouter<Object> xdsRouter = new XdsRouter<>(url, rdsRouteRuleManager, edsEndpointManager, true);
        String appName = "app1";
        String cluster1 = "cluster-test1";
        String cluster2 = "cluster-test2";
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(createInvoker(appName)));
        xdsRouter.notify(invokers);
        String path = "/DemoInterface/call";
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(appName)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPath(path).build())
                .setRoute(RouteAction.newBuilder().setCluster(cluster1).build())
                .build()
            ).build();
        RdsVirtualHostListener hostListener = new RdsVirtualHostListener(appName, rdsRouteRuleManager);
        hostListener.parseVirtualHost(virtualHost);
        assertEquals(xdsRouter.getXdsRouteRuleMap().get(appName).size(), 1);
        verify(edsEndpointManager, times(1)).subscribeEds(cluster1, xdsRouter);

        VirtualHost virtualHost2 = VirtualHost.newBuilder()
            .addDomains(appName)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPath(path).build())
                .setRoute(RouteAction.newBuilder().setCluster("cluster-test2").build())
                .build()
            ).build();
        hostListener.parseVirtualHost(virtualHost2);
        assertEquals(xdsRouter.getXdsRouteRuleMap().get(appName).size(), 1);
        verify(edsEndpointManager, times(1)).subscribeEds(cluster2, xdsRouter);
        verify(edsEndpointManager, times(1)).unSubscribeEds(cluster1, xdsRouter);
    }


    @Test
    public void testEndpointChange() {
        XdsRouter<Object> xdsRouter = new XdsRouter<>(url, rdsRouteRuleManager, edsEndpointManager, true);
        String appName = "app1";
        String cluster1 = "cluster-test1";
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(createInvoker(appName, "1.1.1.1:20880")
            , createInvoker(appName, "2.2.2.2:20880")));
        xdsRouter.notify(invokers);
        String path = "/DemoInterface/call";
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(appName)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPath(path).build())
                .setRoute(RouteAction.newBuilder().setCluster(cluster1).build())
                .build()
            ).build();
        RdsVirtualHostListener hostListener = new RdsVirtualHostListener(appName, rdsRouteRuleManager);
        hostListener.parseVirtualHost(virtualHost);
        assertEquals(xdsRouter.getXdsRouteRuleMap().get(appName).size(), 1);
        verify(edsEndpointManager, times(1)).subscribeEds(cluster1, xdsRouter);

        Set<Endpoint> endpoints = new HashSet<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setAddress("1.1.1.1");
        endpoint1.setPortValue(20880);
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setAddress("2.2.2.2");
        endpoint2.setPortValue(20880);
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        edsEndpointManager.notifyEndpointChange(cluster1, endpoints);

        DestinationSubset<Object> objectDestinationSubset = xdsRouter.getDestinationSubsetMap().get(cluster1);
        assertEquals(invokers, objectDestinationSubset.getInvokers());
    }

    @Test
    public void testRouteNotMatch() {
        XdsRouter<Object> xdsRouter = new XdsRouter<>(url, rdsRouteRuleManager, edsEndpointManager, true);
        String appName = "app1";
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(createInvoker(appName, "1.1.1.1:20880")
            , createInvoker(appName, "2.2.2.2:20880")));
        assertEquals(invokers, xdsRouter.route(invokers.clone(), null, null, false, null));
        Holder<String> message = new Holder<>();
        xdsRouter.doRoute(invokers.clone(), null, null, true, null, message);
        assertEquals("Directly Return. Reason: xds route rule is empty.", message.get());
    }

    @Test
    public void testRoutePathMatch() {
        XdsRouter<Object> xdsRouter = new XdsRouter<>(url, rdsRouteRuleManager, edsEndpointManager, true);
        String appName = "app1";
        String cluster1 = "cluster-test1";
        Invoker<Object> invoker1 = createInvoker(appName, "1.1.1.1:20880");
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1
            , createInvoker(appName, "2.2.2.2:20880")));
        xdsRouter.notify(invokers);
        String path = "/DemoInterface/call";
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(appName)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPath(path).build())
                .setRoute(RouteAction.newBuilder().setCluster(cluster1).build())
                .build()
            ).build();
        RdsVirtualHostListener hostListener = new RdsVirtualHostListener(appName, rdsRouteRuleManager);
        hostListener.parseVirtualHost(virtualHost);
        Invocation invocation = Mockito.mock(Invocation.class);
        Invoker invoker = Mockito.mock(Invoker.class);
        URL url1 = Mockito.mock(URL.class);
        when(invoker.getUrl()).thenReturn(url1);
        when(url1.getPath()).thenReturn("DemoInterface");
        when(invocation.getInvoker()).thenReturn(invoker);
        when(invocation.getMethodName()).thenReturn("call");

        Set<Endpoint> endpoints = new HashSet<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setAddress("1.1.1.1");
        endpoint1.setPortValue(20880);
        endpoints.add(endpoint1);
        edsEndpointManager.notifyEndpointChange(cluster1, endpoints);
        BitList<Invoker<Object>> routes = xdsRouter.route(invokers.clone(), null, invocation, false, null);
        assertEquals(1, routes.size());
        assertEquals(invoker1, routes.get(0));

    }


    @Test
    public void testRouteHeadMatch() {
        XdsRouter<Object> xdsRouter = new XdsRouter<>(url, rdsRouteRuleManager, edsEndpointManager, true);
        String appName = "app1";
        String cluster1 = "cluster-test1";
        Invoker<Object> invoker1 = createInvoker(appName, "1.1.1.1:20880");
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1
            , createInvoker(appName, "2.2.2.2:20880")));
        xdsRouter.notify(invokers);
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(appName)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().addHeaders(
                        HeaderMatcher.newBuilder()
                            .setName("userId")
                            .setExactMatch("123")
                            .build()
                    ).build()
                )
                .setRoute(RouteAction.newBuilder().setCluster(cluster1).build())
                .build()
            ).build();
        RdsVirtualHostListener hostListener = new RdsVirtualHostListener(appName, rdsRouteRuleManager);
        hostListener.parseVirtualHost(virtualHost);
        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getAttachment("userId")).thenReturn("123");
        Set<Endpoint> endpoints = new HashSet<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setAddress("1.1.1.1");
        endpoint1.setPortValue(20880);
        endpoints.add(endpoint1);
        edsEndpointManager.notifyEndpointChange(cluster1, endpoints);
        BitList<Invoker<Object>> routes = xdsRouter.route(invokers.clone(), null, invocation, false, null);
        assertEquals(1, routes.size());
        assertEquals(invoker1, routes.get(0));
    }


    @Test
    public void testRouteWeightCluster() {
        XdsRouter<Object> xdsRouter = new XdsRouter<>(url, rdsRouteRuleManager, edsEndpointManager, true);
        String appName = "app1";
        String cluster1 = "cluster-test1";
        String cluster2 = "cluster-test2";
        Invoker<Object> invoker1 = createInvoker(appName, "1.1.1.1:20880");
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1
            , createInvoker(appName, "2.2.2.2:20880")));
        xdsRouter.notify(invokers);
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(appName)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().addHeaders(
                        HeaderMatcher.newBuilder()
                            .setName("userId")
                            .setExactMatch("123")
                            .build()
                    ).build()
                )
                .setRoute(RouteAction.newBuilder().setWeightedClusters(
                        WeightedCluster.newBuilder()
                            .addClusters(WeightedCluster.ClusterWeight.newBuilder().setName(cluster1)
                                .setWeight(UInt32Value.newBuilder().setValue(100).build()).build())
                            .addClusters(WeightedCluster.ClusterWeight.newBuilder().setName(cluster2)
                                .setWeight(UInt32Value.newBuilder().setValue(0).build()).build())
                            .build())
                    .build()
                ).build()).build();
        RdsVirtualHostListener hostListener = new RdsVirtualHostListener(appName, rdsRouteRuleManager);
        hostListener.parseVirtualHost(virtualHost);
        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getAttachment("userId")).thenReturn("123");
        Set<Endpoint> endpoints = new HashSet<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setAddress("1.1.1.1");
        endpoint1.setPortValue(20880);
        endpoints.add(endpoint1);
        edsEndpointManager.notifyEndpointChange(cluster1, endpoints);

        endpoints = new HashSet<>();
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setAddress("2.2.2.2");
        endpoint2.setPortValue(20880);
        endpoints.add(endpoint2);
        edsEndpointManager.notifyEndpointChange(cluster2, endpoints);

        for (int i = 0; i < 10; i++) {
            BitList<Invoker<Object>> routes = xdsRouter.route(invokers.clone(), null, invocation, false, null);
            assertEquals(1, routes.size());
            assertEquals(invoker1, routes.get(0));
        }
    }

    @Test
    public void testRouteMultiApp() {
        XdsRouter<Object> xdsRouter = new XdsRouter<>(url, rdsRouteRuleManager, edsEndpointManager, true);
        String appName1 = "app1";
        String appName2 = "app2";
        String cluster1 = "cluster-test1";
        Invoker<Object> invoker1 = createInvoker(appName2, "1.1.1.1:20880");
        Invoker<Object> invoker2 = createInvoker(appName1, "2.2.2.2:20880");
        BitList<Invoker<Object>> invokers = new BitList<>(Arrays.asList(invoker1
            , invoker2));
        xdsRouter.notify(invokers);
        assertEquals(xdsRouter.getSubscribeApplications().size(), 2);
        String path = "/DemoInterface/call";
        VirtualHost virtualHost = VirtualHost.newBuilder()
            .addDomains(appName2)
            .addRoutes(Route.newBuilder().setName("route-test")
                .setMatch(RouteMatch.newBuilder().setPath(path).build())
                .setRoute(RouteAction.newBuilder().setCluster(cluster1).build())
                .build()
            ).build();
        RdsVirtualHostListener hostListener = new RdsVirtualHostListener(appName2, rdsRouteRuleManager);
        hostListener.parseVirtualHost(virtualHost);
        Invocation invocation = Mockito.mock(Invocation.class);
        Invoker invoker = Mockito.mock(Invoker.class);
        URL url1 = Mockito.mock(URL.class);
        when(invoker.getUrl()).thenReturn(url1);
        when(url1.getPath()).thenReturn("DemoInterface");
        when(invocation.getInvoker()).thenReturn(invoker);
        when(invocation.getMethodName()).thenReturn("call");

        Set<Endpoint> endpoints = new HashSet<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setAddress("1.1.1.1");
        endpoint1.setPortValue(20880);
        endpoints.add(endpoint1);
        edsEndpointManager.notifyEndpointChange(cluster1, endpoints);
        BitList<Invoker<Object>> routes = xdsRouter.route(invokers.clone(), null, invocation, false, null);
        assertEquals(1, routes.size());
        assertEquals(invoker1, routes.get(0));
    }

}
