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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.apache.dubbo.rpc.cluster.router.condition.ConditionStateRouterFactory;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;

/**
 * StaticDirectory Test
 */
class StaticDirectoryTest {
    private URL SCRIPT_URL = URL.valueOf("condition://0.0.0.0/com.foo.BarService");

    private URL getRouteUrl(String rule) {
        return SCRIPT_URL.addParameterAndEncoded(RULE_KEY, rule);
    }

    @Test
    void testStaticDirectory() {
        StateRouter router = new ConditionStateRouterFactory().getRouter(String.class, getRouteUrl(" => " + " host = " + NetUtils.getLocalHost()));
        List<StateRouter> routers = new ArrayList<StateRouter>();
        routers.add(router);
        List<Invoker<String>> originInvokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/com.foo.BarService"));
        originInvokers.add(invoker1);
        originInvokers.add(invoker2);
        originInvokers.add(invoker3);
        BitList<Invoker<String>> invokers = new BitList<>(originInvokers);


        List<Invoker<String>> filteredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation(), false, new Holder<>());
        StaticDirectory<String> staticDirectory = new StaticDirectory<>(filteredInvokers);
        boolean isAvailable = staticDirectory.isAvailable();
        Assertions.assertTrue(!isAvailable);
        List<Invoker<String>> newInvokers = staticDirectory.list(new MockDirInvocation());
        Assertions.assertTrue(newInvokers.size() > 0);
        staticDirectory.destroy();
        Assertions.assertEquals(0, staticDirectory.getInvokers().size());
        Assertions.assertEquals(0, staticDirectory.getValidInvokers().size());
    }
}
