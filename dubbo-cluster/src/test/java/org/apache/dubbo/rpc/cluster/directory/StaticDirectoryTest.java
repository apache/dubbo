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
package org.apache.dubbo.rpc.cluster.directory;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.condition.ConditionRouterFactory;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * StaticDirectory Test
 */
public class StaticDirectoryTest {
    private URL SCRIPT_URL = URL.valueOf("condition://0.0.0.0/com.foo.BarService");

    private URL getRouteUrl(String rule) {
        return SCRIPT_URL.addParameterAndEncoded(Constants.RULE_KEY, rule);
    }

    @Test
    public void testStaticDirectory() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl(" => " + " host = " + NetUtils.getLocalHost()));
        List<Router> routers = new ArrayList<Router>();
        routers.add(router);
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        StaticDirectory<String> staticDirectory = new StaticDirectory<>(filteredInvokers);
        boolean isAvailable = staticDirectory.isAvailable();
        Assert.assertTrue(!isAvailable);
        List<Invoker<String>> newInvokers = staticDirectory.list(new MockDirInvocation());
        Assert.assertTrue(newInvokers.size() > 0);
        staticDirectory.destroy();
        Assert.assertEquals(0, newInvokers.size());
    }
}
