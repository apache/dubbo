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
package org.apache.dubbo.rpc.cluster.router.script;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;

@DisabledForJreRange(min = JRE.JAVA_16)
class ScriptStateRouterTest {

    private URL SCRIPT_URL = URL.valueOf("script://javascript?type=javascript");

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
    void testRouteReturnAll() {
        StateRouter router = new ScriptStateRouterFactory().getRouter(String.class, getRouteUrl("function route(op1,op2){return op1} route(invokers)"));
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        originInvokers.add(new MockInvoker<String>());
        originInvokers.add(new MockInvoker<String>());
        originInvokers.add(new MockInvoker<String>());
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers.clone(), invokers.get(0).getUrl(), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(invokers, filteredInvokers);
    }

    @Test
    void testRoutePickInvokers() {
        String rule = "var result = new java.util.ArrayList(invokers.size());" +
            "for (i=0;i<invokers.size(); i++){ " +
            "if (invokers.get(i).isAvailable()) {" +
            "result.add(invokers.get(i)) ;" +
            "}" +
            "} ; " +
            "return result;";
        String script = "function route(invokers,invocation,context){" + rule + "} route(invokers,invocation,context)";
        StateRouter router = new ScriptStateRouterFactory().getRouter(String.class, getRouteUrl(script));

        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(false);
        Invoker<String> invoker2 = new MockInvoker<String>(true);
        Invoker<String> invoker3 = new MockInvoker<String>(true);
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        List<Invoker<String>> filteredInvokers = router.route(invokers.clone(), invokers.get(0).getUrl(), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(2, filteredInvokers.size());
        Assertions.assertEquals(invoker2, filteredInvokers.get(0));
        Assertions.assertEquals(invoker3, filteredInvokers.get(1));
    }

    @Test
    void testRouteHostFilter() {
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        MockInvoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.134.108.1:20880/com.dubbo.HelloService"));
        MockInvoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://10.134.108.2:20880/com.dubbo.HelloService"));
        MockInvoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://10.134.108.3:20880/com.dubbo.HelloService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        String script = "function route(invokers, invocation, context){ " +
            "    var result = new java.util.ArrayList(invokers.size()); " +
            "    var targetHost = new java.util.ArrayList(); " +
            "    targetHost.add(\"10.134.108.2\"); " +
            "    for (var i = 0; i < invokers.length; i++) { " +
            "        if(targetHost.contains(invokers[i].getUrl().getHost())){ " +
            "            result.add(invokers[i]); " +
            "        } " +
            "    } " +
            "    return result; " +
            "} " +
            "route(invokers, invocation, context) ";

        StateRouter router = new ScriptStateRouterFactory().getRouter(String.class, getRouteUrl(script));
        List<Invoker<String>> routeResult = router.route(invokers.clone(), invokers.get(0).getUrl(), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(1, routeResult.size());
        Assertions.assertEquals(invoker2, routeResult.get(0));
    }

    @Test
    void testRoute_throwException() {
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        MockInvoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.134.108.1:20880/com.dubbo.HelloService"));
        MockInvoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://10.134.108.2:20880/com.dubbo.HelloService"));
        MockInvoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://10.134.108.3:20880/com.dubbo.HelloService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);

        String script = "/";
        StateRouter router = new ScriptStateRouterFactory().getRouter(String.class, getRouteUrl(script));
        List<Invoker<String>> routeResult = router.route(invokers.clone(), invokers.get(0).getUrl(), new RpcInvocation(), false, new Holder<>());
        Assertions.assertEquals(3, routeResult.size());
    }
}
