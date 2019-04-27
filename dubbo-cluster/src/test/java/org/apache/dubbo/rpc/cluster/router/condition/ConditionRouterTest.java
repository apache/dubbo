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
package org.apache.dubbo.rpc.cluster.router.condition;


import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ConditionRouterTest {

    private URL SCRIPT_URL = URL.valueOf("condition://0.0.0.0/com.foo.BarService");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @BeforeEach
    public void setUp() throws Exception {
    }

    private URL getRouteUrl(String rule) {
        return SCRIPT_URL.addParameterAndEncoded(Constants.RULE_KEY, rule);
    }

    @Test
    public void testRoute_matchWhen() {
        Invocation invocation = new RpcInvocation();

        Router router = new ConditionRouterFactory().getRouter(getRouteUrl(" => host = 1.2.3.4"));
        boolean matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertEquals(true, matchWhen);

        router = new ConditionRouterFactory().getRouter(getRouteUrl("host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertEquals(true, matchWhen);

        router = new ConditionRouterFactory().getRouter(getRouteUrl("host = 2.2.2.2,1.1.1.1,3.3.3.3 & host !=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertEquals(false, matchWhen);

        router = new ConditionRouterFactory().getRouter(getRouteUrl("host !=4.4.4.4 & host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertEquals(true, matchWhen);

        router = new ConditionRouterFactory().getRouter(getRouteUrl("host !=4.4.4.* & host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertEquals(true, matchWhen);

        router = new ConditionRouterFactory().getRouter(getRouteUrl("host = 2.2.2.2,1.1.1.*,3.3.3.3 & host != 1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertEquals(false, matchWhen);

        router = new ConditionRouterFactory().getRouter(getRouteUrl("host = 2.2.2.2,1.1.1.*,3.3.3.3 & host != 1.1.1.2 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertEquals(true, matchWhen);
    }

    @Test
    public void testRoute_matchFilter() {
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf(
                "dubbo://10.20.3.3:20880/com.foo.BarService?default.serialization=fastjson"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost()
                + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost()
                + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        Router router1 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " => " + " host = 10.20.3.3").addParameter(Constants.FORCE_KEY,
                String.valueOf(true)));
        Router router2 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " => " + " host = 10.20.3.* & host != 10.20.3.3").addParameter(
                Constants.FORCE_KEY, String.valueOf(true)));
        Router router3 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " => " + " host = 10.20.3.3  & host != 10.20.3.3").addParameter(
                Constants.FORCE_KEY, String.valueOf(true)));
        Router router4 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " => " + " host = 10.20.3.2,10.20.3.3,10.20.3.4").addParameter(
                Constants.FORCE_KEY, String.valueOf(true)));
        Router router5 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " => " + " host != 10.20.3.3").addParameter(Constants.FORCE_KEY,
                String.valueOf(true)));
        Router router6 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " => " + " serialization = fastjson").addParameter(
                Constants.FORCE_KEY, String.valueOf(true)));

        List<Invoker<String>> filteredInvokers1 = router1.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> filteredInvokers2 = router2.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> filteredInvokers3 = router3.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> filteredInvokers4 = router4.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> filteredInvokers5 = router5.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> filteredInvokers6 = router6.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(1, filteredInvokers1.size());
        Assertions.assertEquals(0, filteredInvokers2.size());
        Assertions.assertEquals(0, filteredInvokers3.size());
        Assertions.assertEquals(1, filteredInvokers4.size());
        Assertions.assertEquals(2, filteredInvokers5.size());
        Assertions.assertEquals(1, filteredInvokers6.size());
    }

    @Test
    public void testRoute_methodRoute() {
        Invocation invocation = new RpcInvocation("getFoo", new Class<?>[0], new Object[0]);
        // More than one methods, mismatch
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("methods=getFoo => host = 1.2.3.4"));
        boolean matchWhen = ((ConditionRouter) router).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=setFoo,getFoo,findFoo"), invocation);
        Assertions.assertEquals(true, matchWhen);
        // Exactly one method, match
        matchWhen = ((ConditionRouter) router).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assertions.assertEquals(true, matchWhen);
        // Method routing and Other condition routing can work together
        Router router2 = new ConditionRouterFactory()
                .getRouter(getRouteUrl("methods=getFoo & host!=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router2).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assertions.assertEquals(false, matchWhen);

        Router router3 = new ConditionRouterFactory()
                .getRouter(getRouteUrl("methods=getFoo & host=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router3).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assertions.assertEquals(true, matchWhen);
        // Test filter condition
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost()
                + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost()
                + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        Router router4 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " & methods = getFoo => " + " host = 10.20.3.3").addParameter(
                Constants.FORCE_KEY, String.valueOf(true)));
        List<Invoker<String>> filteredInvokers1 = router4.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), invocation);
        Assertions.assertEquals(1, filteredInvokers1.size());

        Router router5 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " & methods = unvalidmethod => " + " host = 10.20.3.3")
                .addParameter(Constants.FORCE_KEY, String.valueOf(true)));
        List<Invoker<String>> filteredInvokers2 = router5.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), invocation);
        Assertions.assertEquals(3, filteredInvokers2.size());
        // Request a non-exists method
    }

    @Test
    public void testRoute_ReturnFalse() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => false"));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(0, filteredInvokers.size());
    }

    @Test
    public void testRoute_ReturnEmpty() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => "));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(0, filteredInvokers.size());
    }

    @Test
    public void testRoute_ReturnAll() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => " + " host = " + NetUtils.getLocalHost()));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        invokers.add(new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService")));
        invokers.add(new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService")));
        invokers.add(new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService")));
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(invokers, filteredInvokers);
    }

    @Test
    public void testRoute_HostFilter() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => " + " host = " + NetUtils.getLocalHost()));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    public void testRoute_Empty_HostFilter() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl(" => " + " host = " + NetUtils.getLocalHost()));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    public void testRoute_False_HostFilter() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("true => " + " host = " + NetUtils.getLocalHost()));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    public void testRoute_Placeholder() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => " + " host = $host"));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    public void testRoute_NoForce() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => " + " host = 1.2.3.4"));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(invokers, filteredInvokers);
    }

    @Test
    public void testRoute_Force() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => " + " host = 1.2.3.4").addParameter(Constants.FORCE_KEY, String.valueOf(true)));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assertions.assertEquals(0, filteredInvokers.size());
    }

}