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


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.FORCE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;

public class ConditionRouterTest {
    private static final String LOCAL_HOST = "127.0.0.1";
    private URL SCRIPT_URL = URL.valueOf("condition://0.0.0.0/com.foo.BarService");

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @BeforeEach
    public void setUp() throws Exception {
    }

    private URL getRouteUrl(String rule) {
        return SCRIPT_URL.addParameterAndEncoded(RULE_KEY, rule);
    }

    @Test
    public void testRoute_matchWhen() {
        Invocation invocation = new RpcInvocation();

        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(" => host = 1.2.3.4"));
        boolean matchWhen = ((ConditionStateRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertTrue(matchWhen);

        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionStateRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertTrue(matchWhen);

        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = 2.2.2.2,1.1.1.1,3.3.3.3 & host !=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionStateRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertFalse(matchWhen);

        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host !=4.4.4.4 & host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionStateRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertTrue(matchWhen);

        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host !=4.4.4.* & host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionStateRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertTrue(matchWhen);

        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = 2.2.2.2,1.1.1.*,3.3.3.3 & host != 1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionStateRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertFalse(matchWhen);

        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = 2.2.2.2,1.1.1.*,3.3.3.3 & host != 1.1.1.2 => host = 1.2.3.4"));
        matchWhen = ((ConditionStateRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assertions.assertTrue(matchWhen);
    }

    @Test
    public void testRoute_matchFilter() {
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf(
                "dubbo://10.20.3.3:20880/com.foo.BarService?serialization=fastjson"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST
                + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST
                + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        System.err.println("The localhost address: " + invoker2.getUrl().getAddress());
        System.err.println(invoker3.getUrl().getAddress());

        StateRouter router1 = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(
                "host = " + LOCAL_HOST + " => " + " host = 10.20.3.3").addParameter(FORCE_KEY,
                String.valueOf(true)));
        StateRouter router2 = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(
                "host = " + LOCAL_HOST + " => " + " host = 10.20.3.* & host != 10.20.3.3").addParameter(
                FORCE_KEY, String.valueOf(true)));
        StateRouter router3 = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(
                "host = " + LOCAL_HOST + " => " + " host = 10.20.3.3  & host != 10.20.3.3").addParameter(
                FORCE_KEY, String.valueOf(true)));
        StateRouter router4 = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(
                "host = " + LOCAL_HOST + " => " + " host = 10.20.3.2,10.20.3.3,10.20.3.4").addParameter(
                FORCE_KEY, String.valueOf(true)));
        StateRouter router5 = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(
                "host = " + LOCAL_HOST + " => " + " host != 10.20.3.3").addParameter(FORCE_KEY,
                String.valueOf(true)));
        StateRouter router6 = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(
                "host = " + LOCAL_HOST + " => " + " serialization = fastjson").addParameter(
                FORCE_KEY, String.valueOf(true)));



        List<Invoker<String>> filteredInvokers1 = router1.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        List<Invoker<String>> filteredInvokers2 = router2.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        List<Invoker<String>> filteredInvokers3 = router3.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        List<Invoker<String>> filteredInvokers4 = router4.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        List<Invoker<String>> filteredInvokers5 = router5.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        List<Invoker<String>> filteredInvokers6 = router6.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(1, filteredInvokers1.size());
        Assertions.assertEquals(0, filteredInvokers2.size());
        Assertions.assertEquals(0, filteredInvokers3.size());
        Assertions.assertEquals(1, filteredInvokers4.size());
        Assertions.assertEquals(2, filteredInvokers5.size());
        Assertions.assertEquals(1, filteredInvokers6.size());
    }

    @Test
    public void testRoute_methodRoute() {
        Invocation invocation = new RpcInvocation("getFoo", "com.foo.BarService", "", new Class<?>[0], new Object[0]);
        // More than one methods, mismatch
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("methods=getFoo => host = 1.2.3.4"));
        boolean matchWhen = ((ConditionStateRouter) router).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=setFoo,getFoo,findFoo"), invocation);
        Assertions.assertTrue(matchWhen);
        // Exactly one method, match
        matchWhen = ((ConditionStateRouter) router).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assertions.assertTrue(matchWhen);
        // Method routing and Other condition routing can work together
        StateRouter router2 = new ConditionStateRouterFactory()
                .getRouter(String.class, getRouteUrl("methods=getFoo & host!=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionStateRouter) router2).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assertions.assertFalse(matchWhen);

        StateRouter router3 = new ConditionStateRouterFactory()
                .getRouter(String.class, getRouteUrl("methods=getFoo & host=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionStateRouter) router3).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assertions.assertTrue(matchWhen);
        // Test filter condition
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST
                + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST
                + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        StateRouter router4 = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(
                "host = " + LOCAL_HOST + " & methods = getFoo => " + " host = 10.20.3.3").addParameter(
                FORCE_KEY, String.valueOf(true)));
        List<Invoker<String>> filteredInvokers1 = router4.route(invokers,
                URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), invocation, false, new Holder<>());
        Assertions.assertEquals(1, filteredInvokers1.size());

        StateRouter router5 = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(
                "host = " + LOCAL_HOST + " & methods = unvalidmethod => " + " host = 10.20.3.3")
                .addParameter(FORCE_KEY, String.valueOf(true)));
        List<Invoker<String>> filteredInvokers2 = router5.route(invokers,
                URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), invocation, false, new Holder<>());
        Assertions.assertEquals(3, filteredInvokers2.size());
        // Request a non-exists method
    }

    @Test
    public void testRoute_ReturnFalse() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = " + LOCAL_HOST + " => false"));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        originInvokers.add(new MockInvoker<String>());
        originInvokers.add(new MockInvoker<String>());
        originInvokers.add(new MockInvoker<String>());
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(0, filteredInvokers.size());
    }

    @Test
    public void testRoute_ReturnEmpty() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = " + LOCAL_HOST + " => "));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        originInvokers.add(new MockInvoker<String>());
        originInvokers.add(new MockInvoker<String>());
        originInvokers.add(new MockInvoker<String>());
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(0, filteredInvokers.size());
    }

    @Test
    public void testRoute_ReturnAll() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = " + LOCAL_HOST + " => " + " host = " + LOCAL_HOST));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        originInvokers.add(new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService")));
        originInvokers.add(new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService")));
        originInvokers.add(new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService")));
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(invokers, filteredInvokers);
    }

    @Test
    public void testRoute_HostFilter() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = " + LOCAL_HOST + " => " + " host = " + LOCAL_HOST));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    public void testRoute_Empty_HostFilter() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(" => " + " host = " + LOCAL_HOST));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    public void testRoute_False_HostFilter() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("true => " + " host = " + LOCAL_HOST));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    public void testRoute_Placeholder() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = " + LOCAL_HOST + " => " + " host = $host"));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    public void testRoute_NoForce() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = " + LOCAL_HOST + " => " + " host = 1.2.3.4"));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(invokers, filteredInvokers);
    }

    @Test
    public void testRoute_Force() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("host = " + LOCAL_HOST + " => " + " host = 1.2.3.4").addParameter(FORCE_KEY, String.valueOf(true)));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(0, filteredInvokers.size());
    }

    @Test
    public void testRoute_Arguments() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("arguments[0] = a " + " => " + " host = 1.2.3.4").addParameter(FORCE_KEY, String.valueOf(true)));
        List<Invoker<String>> originInvokers = new ArrayList<>();
        Invoker<String> invoker1 = new MockInvoker<>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<>(URL.valueOf("dubbo://" + LOCAL_HOST + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        RpcInvocation invocation = new RpcInvocation();
        String p = "a";
        invocation.setArguments(new Object[]{null});
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), invocation, false, new Holder<>());
        Assertions.assertEquals(3, fileredInvokers.size());

        invocation.setArguments(new Object[]{p});
        fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), invocation, false, new Holder<>());
        Assertions.assertEquals(0, fileredInvokers.size());

        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("arguments = b " + " => " + " host = 1.2.3.4").addParameter(FORCE_KEY, String.valueOf(true)));
        fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), invocation, false, new Holder<>());
        Assertions.assertEquals(3, fileredInvokers.size());

        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("arguments[10].inner = a " + " => " + " host = 1.2.3.4").addParameter(FORCE_KEY, String.valueOf(true)));
        fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), invocation, false, new Holder<>());
        Assertions.assertEquals(3, fileredInvokers.size());

        int integer = 1;
        invocation.setArguments(new Object[]{integer});
        router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl("arguments[0].inner = 1 " + " => " + " host = 1.2.3.4").addParameter(FORCE_KEY, String.valueOf(true)));
        fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + LOCAL_HOST + "/com.foo.BarService"), invocation, false, new Holder<>());
        Assertions.assertEquals(0, fileredInvokers.size());
    }
}
