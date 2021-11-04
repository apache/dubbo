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

package org.apache.dubbo.rpc.cluster.router.mesh.route;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.VsDestinationGroup;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRule;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRuleSpec;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.Subset;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.DubboMatchRequest;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.DubboRoute;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.DubboRouteDetail;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.VirtualServiceRule;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.VirtualServiceSpec;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.destination.DubboDestination;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.destination.DubboRouteDestination;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.DubboMethodMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.StringMatch;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MeshRuleRouterTest {

    @Test
    public void containMapKeyValue() {
        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);

        Map<String, String> originMap = new HashMap<>();

        originMap.put("key1", "value1");
        originMap.put("key2", "value2");
        originMap.put("key3", "value3");

        Map<String, String> inputMap = new HashMap<>();

        inputMap.put("key1", "value1");
        inputMap.put("key2", "value2");

        assertTrue(meshRuleRouter.containMapKeyValue(originMap, inputMap));

        inputMap.put("key4", "value4");
        assertFalse(meshRuleRouter.containMapKeyValue(originMap, inputMap));


        assertTrue(meshRuleRouter.containMapKeyValue(originMap, null));
        assertTrue(meshRuleRouter.containMapKeyValue(originMap, new HashMap<>()));

    }

    @Test
    public void computeSubsetMap() {
        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);

        List<Invoker<?>> invokers = new ArrayList<>();

        //--
        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test1");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }

        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test2");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test3");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        //--

        List<DestinationRule> destinationRules = new ArrayList<>();

        //--
        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test1");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test1");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }

        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test2");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test2");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }


        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test4");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test4");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }


        //--


        Map<String, List<Invoker<?>>> result = meshRuleRouter.computeSubsetMap(invokers, destinationRules);

        assertTrue(result.size() == 3);
        assertTrue(result.containsKey("test1"));
        assertTrue(result.containsKey("test2"));
        assertTrue(result.containsKey("test4"));

        assertTrue(result.get("test1").size() == 1);
        assertTrue(result.get("test2").size() == 1);
        assertTrue(result.get("test4").size() == 0);

    }

    @Test
    public void computeSubset() {

        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);


        meshRuleRouter.setInvokerList(null);
        meshRuleRouter.computeSubset();

        assertNull(meshRuleRouter.getSubsetMap());

        List<Invoker<?>> invokers = new ArrayList<>();

        //--
        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test1");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }

        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test2");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test3");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        meshRuleRouter.setInvokerList(invokers);

        meshRuleRouter.computeSubset();

        assertNull(meshRuleRouter.getSubsetMap());


        VsDestinationGroup vsDestinationGroup = new VsDestinationGroup();

        List<DestinationRule> destinationRules = new ArrayList<>();

        //--
        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test1");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test1");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }

        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test2");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test2");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }


        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test4");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test4");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }


        vsDestinationGroup.setDestinationRuleList(destinationRules);

        meshRuleRouter.setVsDestinationGroup(vsDestinationGroup);


        meshRuleRouter.computeSubset();

        assertNotNull(meshRuleRouter.getSubsetMap());
        assertTrue(meshRuleRouter.getSubsetMap().size() == 3);
    }

    @Test
    public void findMatchDubboRouteDetail() {

        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);

        Invocation invocation = mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("sayHello");
        when(invocation.getArguments()).thenReturn(new Object[]{"qinliujie"});
        when(invocation.getCompatibleParamSignatures()).thenReturn(new String[]{String.class.getName()});

        assertNull(meshRuleRouter.findMatchDubboRouteDetail(new ArrayList<>(), invocation));

        //--
        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            dubboRouteDetailList.add(dubboRouteDetail);

            DubboRouteDetail result = meshRuleRouter.findMatchDubboRouteDetail(dubboRouteDetailList, invocation);
            assertNotNull(result);
            assertEquals("test", result.getName());
        }

        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHi");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);
            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            dubboRouteDetailList.add(dubboRouteDetail);

            DubboRouteDetail result = meshRuleRouter.findMatchDubboRouteDetail(dubboRouteDetailList, invocation);
            assertNull(result);
        }


        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            {
                DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
                dubboRouteDetail.setName("test");

                List<DubboMatchRequest> match = new ArrayList<>();

                DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
                DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
                StringMatch stringMatch = new StringMatch();
                stringMatch.setExact("sayHi");
                dubboMethodMatch.setName_match(stringMatch);

                dubboMatchRequest.setMethod(dubboMethodMatch);
                match.add(dubboMatchRequest);

                dubboRouteDetail.setMatch(match);

                dubboRouteDetailList.add(dubboRouteDetail);
            }


            {
                DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
                dubboRouteDetail.setName("test2");

                List<DubboMatchRequest> match = new ArrayList<>();

                DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
                DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
                StringMatch stringMatch = new StringMatch();
                stringMatch.setExact("sayHello");
                dubboMethodMatch.setName_match(stringMatch);

                dubboMatchRequest.setMethod(dubboMethodMatch);
                match.add(dubboMatchRequest);

                dubboRouteDetail.setMatch(match);

                dubboRouteDetailList.add(dubboRouteDetail);
            }

            DubboRouteDetail result = meshRuleRouter.findMatchDubboRouteDetail(dubboRouteDetailList, invocation);
            assertNotNull(result);
            assertEquals("test2", result.getName());
        }

    }

    @Test
    public void getDubboRouteDestination() {
        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);

        Invocation invocation = mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("sayHello");
        when(invocation.getArguments()).thenReturn(new Object[]{"qinliujie"});
        when(invocation.getCompatibleParamSignatures()).thenReturn(new String[]{String.class.getName()});

        DubboRoute dubboRoute = new DubboRoute();

        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHi");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            dubboRouteDestinations.add(new DubboRouteDestination());
            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            assertNull(meshRuleRouter.getDubboRouteDestination(dubboRoute, invocation));
        }


        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            dubboRouteDestinations.add(new DubboRouteDestination());
            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            assertNotNull(meshRuleRouter.getDubboRouteDestination(dubboRoute, invocation));
        }
    }

    @Test
    public void getDubboRoute() {

        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);

        Invocation invocation = mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("sayHello");
        when(invocation.getArguments()).thenReturn(new Object[]{"qinliujie"});
        when(invocation.getCompatibleParamSignatures()).thenReturn(new String[]{String.class.getName()});
        when(invocation.getServiceName()).thenReturn("demoService");

        DubboRoute dubboRoute = new DubboRoute();

        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            dubboRouteDestinations.add(new DubboRouteDestination());
            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);


            dubboRoute.setServices(new ArrayList<>());

            VirtualServiceRule virtualServiceRule = new VirtualServiceRule();
            //virtualServiceRule.


            VirtualServiceSpec spec = new VirtualServiceSpec();
            List<DubboRoute> dubbo = new ArrayList<>();
            dubbo.add(dubboRoute);

            spec.setDubbo(dubbo);

            virtualServiceRule.setSpec(spec);
            DubboRoute result = meshRuleRouter.getDubboRoute(virtualServiceRule, invocation);

            assertNotNull(result);
        }


        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            dubboRouteDestinations.add(new DubboRouteDestination());
            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            List<StringMatch> serviceMatchList = new ArrayList<>();
            StringMatch serviceNameMatch = new StringMatch();
            serviceNameMatch.setExact("otherService");

            serviceMatchList.add(serviceNameMatch);

            dubboRoute.setServices(serviceMatchList);

            VirtualServiceRule virtualServiceRule = new VirtualServiceRule();
            //virtualServiceRule.


            VirtualServiceSpec spec = new VirtualServiceSpec();
            List<DubboRoute> dubbo = new ArrayList<>();
            dubbo.add(dubboRoute);

            spec.setDubbo(dubbo);
            virtualServiceRule.setSpec(spec);

            DubboRoute result = meshRuleRouter.getDubboRoute(virtualServiceRule, invocation);

            assertNull(result);
        }


        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            dubboRouteDestinations.add(new DubboRouteDestination());
            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            List<StringMatch> serviceMatchList = new ArrayList<>();
            StringMatch serviceNameMatch = new StringMatch();
            serviceNameMatch.setRegex(".*");

            serviceMatchList.add(serviceNameMatch);

            dubboRoute.setServices(serviceMatchList);

            VirtualServiceRule virtualServiceRule = new VirtualServiceRule();
            //virtualServiceRule.


            VirtualServiceSpec spec = new VirtualServiceSpec();
            List<DubboRoute> dubbo = new ArrayList<>();
            dubbo.add(dubboRoute);

            spec.setDubbo(dubbo);
            virtualServiceRule.setSpec(spec);
            DubboRoute result = meshRuleRouter.getDubboRoute(virtualServiceRule, invocation);

            assertNotNull(result);
        }

        {
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHi");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            dubboRouteDestinations.add(new DubboRouteDestination());
            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            List<StringMatch> serviceMatchList = new ArrayList<>();
            StringMatch serviceNameMatch = new StringMatch();
            serviceNameMatch.setRegex(".*");

            serviceMatchList.add(serviceNameMatch);

            dubboRoute.setServices(serviceMatchList);

            VirtualServiceRule virtualServiceRule = new VirtualServiceRule();
            //virtualServiceRule.


            VirtualServiceSpec spec = new VirtualServiceSpec();
            List<DubboRoute> dubbo = new ArrayList<>();
            dubbo.add(dubboRoute);

            spec.setDubbo(dubbo);
            virtualServiceRule.setSpec(spec);
            DubboRoute result = meshRuleRouter.getDubboRoute(virtualServiceRule, invocation);

            assertNotNull(result);
        }


    }

    @Test
    public void testNotify() {

        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);


        meshRuleRouter.setInvokerList(null);
        meshRuleRouter.computeSubset();

        assertNull(meshRuleRouter.getSubsetMap());

        List<Invoker<?>> invokers = new ArrayList<>();

        //--
        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test1");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }

        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test2");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test3");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }

        VsDestinationGroup vsDestinationGroup = new VsDestinationGroup();

        List<DestinationRule> destinationRules = new ArrayList<>();

        //--
        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test1");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test1");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }

        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test2");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test2");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }


        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test4");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test4");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }


        vsDestinationGroup.setDestinationRuleList(destinationRules);

        meshRuleRouter.setVsDestinationGroup(vsDestinationGroup);

        meshRuleRouter.notify((List) invokers);

        assertNotNull(meshRuleRouter.getSubsetMap());

    }

    @Test
    public void route() {
        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);

        List<Invoker<?>> inputInvokers = new ArrayList<>();

        URL inputURL = mock(URL.class);

        Invocation invocation = mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("sayHello");
        when(invocation.getArguments()).thenReturn(new Object[]{"qinliujie"});
        when(invocation.getCompatibleParamSignatures()).thenReturn(new String[]{String.class.getName()});
        when(invocation.getServiceName()).thenReturn("demoService");


        List<Invoker<?>> invokers = new ArrayList<>();

        //--
        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test1");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }

        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test2");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test3");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        meshRuleRouter.setInvokerList(invokers);


        VsDestinationGroup vsDestinationGroup = new VsDestinationGroup();

        List<DestinationRule> destinationRules = new ArrayList<>();

        //--
        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test1");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test1");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }

        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test2");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test2");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }

        vsDestinationGroup.setDestinationRuleList(destinationRules);

        meshRuleRouter.setVsDestinationGroup(vsDestinationGroup);


        {
            DubboRoute dubboRoute = new DubboRoute();
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            dubboRouteDestinations.add(new DubboRouteDestination());
            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            List<StringMatch> serviceMatchList = new ArrayList<>();
            StringMatch serviceNameMatch = new StringMatch();
            serviceNameMatch.setExact("otherService");

            serviceMatchList.add(serviceNameMatch);

            dubboRoute.setServices(serviceMatchList);

            VirtualServiceRule virtualServiceRule = new VirtualServiceRule();
            //virtualServiceRule.


            VirtualServiceSpec spec = new VirtualServiceSpec();
            List<DubboRoute> dubbo = new ArrayList<>();
            dubbo.add(dubboRoute);

            spec.setDubbo(dubbo);
            virtualServiceRule.setSpec(spec);

            List<VirtualServiceRule> virtualServiceRuleList = new ArrayList<>();
            virtualServiceRuleList.add(virtualServiceRule);
            vsDestinationGroup.setVirtualServiceRuleList(virtualServiceRuleList);
            meshRuleRouter.computeSubset();
            assertEquals(inputInvokers, meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult());
        }


        {
            DubboRoute dubboRoute = new DubboRoute();
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            DubboRouteDestination dubboRouteDestination = new DubboRouteDestination();
            DubboDestination destination = new DubboDestination();
            destination.setSubset("test1");
            dubboRouteDestination.setDestination(destination);
            dubboRouteDestinations.add(dubboRouteDestination);


            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            List<StringMatch> serviceMatchList = new ArrayList<>();
            StringMatch serviceNameMatch = new StringMatch();
            serviceNameMatch.setRegex(".*");

            serviceMatchList.add(serviceNameMatch);

            dubboRoute.setServices(serviceMatchList);

            VirtualServiceRule virtualServiceRule = new VirtualServiceRule();
            //virtualServiceRule.


            VirtualServiceSpec spec = new VirtualServiceSpec();
            List<DubboRoute> dubbo = new ArrayList<>();
            dubbo.add(dubboRoute);

            spec.setDubbo(dubbo);
            virtualServiceRule.setSpec(spec);

            List<VirtualServiceRule> virtualServiceRuleList = new ArrayList<>();
            virtualServiceRuleList.add(virtualServiceRule);
            vsDestinationGroup.setVirtualServiceRuleList(virtualServiceRuleList);
            meshRuleRouter.computeSubset();
            assertNotEquals(inputInvokers, meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult());
            assertEquals(1, meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult().size());

            Map<String, String> invokerParameterMap = new HashMap<>();
            invokerParameterMap.put("env", "test1");

            assertEquals(invokerParameterMap, ((Invoker) meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult().get(0)).getUrl().getServiceParameters(url.getProtocolServiceKey()));
        }
    }


    @Test
    public void routeFallback() {
        URL url = mock(URL.class);
        when(url.getServiceKey()).thenReturn("test");
        MeshRuleRouter meshRuleRouter = new MeshRuleRouter(url);

        List<Invoker<?>> inputInvokers = new ArrayList<>();

        URL inputURL = mock(URL.class);

        Invocation invocation = mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("sayHello");
        when(invocation.getArguments()).thenReturn(new Object[]{"qinliujie"});
        when(invocation.getCompatibleParamSignatures()).thenReturn(new String[]{String.class.getName()});
        when(invocation.getServiceName()).thenReturn("demoService");


        List<Invoker<?>> invokers = new ArrayList<>();

        //--
        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test1");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }

        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test2");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        {
            Invoker<?> invoker1 = mock(Invoker.class);
            URL invoker1URL = mock(URL.class);
            Map<String, String> invoker1Map = new HashMap<>();
            invoker1Map.put("env", "test3");

            when(invoker1URL.getServiceParameters(url.getProtocolServiceKey())).thenReturn(invoker1Map);
            when(invoker1.getUrl()).thenReturn(invoker1URL);

            invokers.add(invoker1);
        }


        meshRuleRouter.setInvokerList(invokers);


        VsDestinationGroup vsDestinationGroup = new VsDestinationGroup();

        List<DestinationRule> destinationRules = new ArrayList<>();

        //--
        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test1");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test1");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }

        {
            DestinationRule destinationRule1 = new DestinationRule();

            DestinationRuleSpec destinationRuleSpec = new DestinationRuleSpec();
            destinationRuleSpec.setHost("test1");

            List<Subset> subsetList = new ArrayList<>();

            Subset subset = new Subset();
            subset.setName("test2");

            Map<String, String> subsetTest1Lables = new HashMap<>();
            subsetTest1Lables.put("env", "test2");
            subset.setLabels(subsetTest1Lables);

            subsetList.add(subset);

            destinationRuleSpec.setSubsets(subsetList);

            destinationRule1.setSpec(destinationRuleSpec);
            destinationRules.add(destinationRule1);
        }

        vsDestinationGroup.setDestinationRuleList(destinationRules);

        meshRuleRouter.setVsDestinationGroup(vsDestinationGroup);


        {
            DubboRoute dubboRoute = new DubboRoute();
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            DubboRouteDestination dubboRouteDestination = new DubboRouteDestination();
            DubboDestination destination = new DubboDestination();
            destination.setSubset("test5");


            DubboRouteDestination fallbackDubboRouteDestination = new DubboRouteDestination();
            DubboDestination fallbackDestination = new DubboDestination();
            fallbackDestination.setSubset("test1");
            fallbackDubboRouteDestination.setDestination(fallbackDestination);


            destination.setFallback(fallbackDubboRouteDestination);


            dubboRouteDestination.setDestination(destination);
            dubboRouteDestinations.add(dubboRouteDestination);


            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            List<StringMatch> serviceMatchList = new ArrayList<>();
            StringMatch serviceNameMatch = new StringMatch();
            serviceNameMatch.setRegex(".*");

            serviceMatchList.add(serviceNameMatch);

            dubboRoute.setServices(serviceMatchList);

            VirtualServiceRule virtualServiceRule = new VirtualServiceRule();
            //virtualServiceRule.


            VirtualServiceSpec spec = new VirtualServiceSpec();
            List<DubboRoute> dubbo = new ArrayList<>();
            dubbo.add(dubboRoute);

            spec.setDubbo(dubbo);
            virtualServiceRule.setSpec(spec);

            List<VirtualServiceRule> virtualServiceRuleList = new ArrayList<>();
            virtualServiceRuleList.add(virtualServiceRule);
            vsDestinationGroup.setVirtualServiceRuleList(virtualServiceRuleList);
            meshRuleRouter.computeSubset();
            assertNotEquals(inputInvokers, meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult());
            assertEquals(1, meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult().size());

            Map<String, String> invokerParameterMap = new HashMap<>();
            invokerParameterMap.put("env", "test1");

            assertEquals(invokerParameterMap, ((Invoker) meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult().get(0)).getUrl().getServiceParameters(url.getProtocolServiceKey()));
        }

        {
            DubboRoute dubboRoute = new DubboRoute();
            List<DubboRouteDetail> dubboRouteDetailList = new ArrayList<>();
            DubboRouteDetail dubboRouteDetail = new DubboRouteDetail();
            dubboRouteDetail.setName("test");

            List<DubboMatchRequest> match = new ArrayList<>();

            DubboMatchRequest dubboMatchRequest = new DubboMatchRequest();
            DubboMethodMatch dubboMethodMatch = new DubboMethodMatch();
            StringMatch stringMatch = new StringMatch();
            stringMatch.setExact("sayHello");
            dubboMethodMatch.setName_match(stringMatch);

            dubboMatchRequest.setMethod(dubboMethodMatch);

            match.add(dubboMatchRequest);

            dubboRouteDetail.setMatch(match);

            List<DubboRouteDestination> dubboRouteDestinations = new ArrayList<>();
            DubboRouteDestination dubboRouteDestination = new DubboRouteDestination();
            DubboDestination destination = new DubboDestination();
            destination.setSubset("test5");


            DubboRouteDestination fallbackDubboRouteDestination = new DubboRouteDestination();
            DubboDestination fallbackDestination = new DubboDestination();
            fallbackDestination.setSubset("test11");
            fallbackDubboRouteDestination.setDestination(fallbackDestination);


            destination.setFallback(fallbackDubboRouteDestination);


            dubboRouteDestination.setDestination(destination);
            dubboRouteDestinations.add(dubboRouteDestination);


            dubboRouteDetail.setRoute(dubboRouteDestinations);

            dubboRouteDetailList.add(dubboRouteDetail);
            dubboRoute.setRoutedetail(dubboRouteDetailList);
            List<StringMatch> serviceMatchList = new ArrayList<>();
            StringMatch serviceNameMatch = new StringMatch();
            serviceNameMatch.setRegex(".*");

            serviceMatchList.add(serviceNameMatch);

            dubboRoute.setServices(serviceMatchList);

            VirtualServiceRule virtualServiceRule = new VirtualServiceRule();
            //virtualServiceRule.


            VirtualServiceSpec spec = new VirtualServiceSpec();
            List<DubboRoute> dubbo = new ArrayList<>();
            dubbo.add(dubboRoute);

            spec.setDubbo(dubbo);
            virtualServiceRule.setSpec(spec);

            List<VirtualServiceRule> virtualServiceRuleList = new ArrayList<>();
            virtualServiceRuleList.add(virtualServiceRule);
            vsDestinationGroup.setVirtualServiceRuleList(virtualServiceRuleList);
            meshRuleRouter.computeSubset();

            assertNull(meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult());

            meshRuleRouter.setSubsetMap(null);
            assertNotNull(meshRuleRouter.route((List) inputInvokers, inputURL, invocation, false).getResult());
        }
    }
}
