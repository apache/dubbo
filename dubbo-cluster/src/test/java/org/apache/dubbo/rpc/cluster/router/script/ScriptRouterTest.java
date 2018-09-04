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


import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

public class ScriptRouterTest {

    private URL SCRIPT_URL = URL.valueOf("script://javascript?type=javascript");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    private URL getRouteUrl(String rule) {
        return SCRIPT_URL.addParameterAndEncoded(Constants.RULE_KEY, rule);
    }

    @Test
    public void testRouteReturnAll() {
        Router router = new ScriptRouterFactory().getRouter(getRouteUrl("function route(op1,op2){return op1} route(invokers)"));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        List<Invoker<String>> filteredInvokers = router.route(invokers, invokers.get(0).getUrl(), new RpcInvocation());
        Assert.assertEquals(invokers, filteredInvokers);
    }

    @Test
    public void testRoutePickInvokers() {
        String rule = "var result = new java.util.ArrayList(invokers.size());" +
                "for (i=0;i<invokers.size(); i++){ " +
                "if (invokers.get(i).isAvailable()) {" +
                "result.add(invokers.get(i)) ;" +
                "}" +
                "} ; " +
                "return result;";
        String script = "function route(invokers,invocation,context){" + rule + "} route(invokers,invocation,context)";
        Router router = new ScriptRouterFactory().getRouter(getRouteUrl(script));

        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(false);
        Invoker<String> invoker2 = new MockInvoker<String>(true);
        Invoker<String> invoker3 = new MockInvoker<String>(true);
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        List<Invoker<String>> filteredInvokers = router.route(invokers, invokers.get(0).getUrl(), new RpcInvocation());
        Assert.assertEquals(2, filteredInvokers.size());
        Assert.assertEquals(invoker2, filteredInvokers.get(0));
        Assert.assertEquals(invoker3, filteredInvokers.get(1));
    }

    //TODO Add tests for abnormal scene
}