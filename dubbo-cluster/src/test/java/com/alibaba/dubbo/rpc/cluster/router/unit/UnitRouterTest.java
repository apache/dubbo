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
package com.alibaba.dubbo.rpc.cluster.router.unit;


import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.router.MockInvoker;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author yiji.github@hotmail.com
 */
public class UnitRouterTest {

    @Test
    public void testRoute_matchWhen() {
        Invocation invocation = new RpcInvocation();
        boolean matchWhen = router.matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"), invocation);
        Assert.assertEquals(true, matchWhen);
    }

    @Test
    public void testRoute_matchFilter() {
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService?default.serialization=fastjson"));
        Invoker<String> invoker2 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.5:20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        List<Invoker<String>> filteredInvokers = router.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());

        // we expect available invokers if nothing mactched
        Assert.assertEquals(3, filteredInvokers.size());
    }

    @Test
    public void testRoute_matchFilter0() {
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf(
                "dubbo://10.20.3.3:20880/com.foo.BarService?default.serialization=fastjson&unit=1"));
        Invoker<String> invoker2 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService?unit=2"));
        Invoker<String> invoker3 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService?unit=2"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        List<Invoker<String>> filteredInvokers = router.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService?unit=2"), new RpcInvocation());

        Assert.assertEquals(3, filteredInvokers.size());
    }

    @Test
    public void testRoute_methodRoute() {
        Invocation invocation = new RpcInvocation("getFoo", new Class<?>[0], new Object[0]);
        // More than one methods, mismatch
        boolean matchWhen = router.matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=setFoo,getFoo,findFoo"), invocation);
        Assert.assertEquals(true, matchWhen);

        // Exactly one method, match
        matchWhen = router.matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assert.assertEquals(true, matchWhen);

        // Test filter condition
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
        Invoker<String> invoker2 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService"));
        Invoker<String> invoker3 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.5:20880/com.foo.BarService"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        List<Invoker<String>> fileredInvokers1 = router.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService?methods=setFoo,getFoo,findFoo"), invocation);
        // we expect available invokers if nothing mactched
        Assert.assertEquals(3, fileredInvokers1.size());
    }

    @Test
    public void testRoute_methodRoute0() {
        Invocation invocation = new RpcInvocation("getFoo", new Class<?>[0], new Object[0]);
        // Test filter condition
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService?methods=setFoo,getFoo&unit=2"));
        Invoker<String> invoker2 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService?methods=setFoo,getFoo&unit=1"));
        Invoker<String> invoker3 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.5:20880/com.foo.BarService?methods=setFoo,getFoo&unit=2"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        List<Invoker<String>> filteredInvokers = router.route(invokers,
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?methods=setFoo,getFoo,findFoo&unit=2"), invocation);

        Assert.assertEquals(2, filteredInvokers.size());
        Assert.assertEquals(invoker1.getUrl(), filteredInvokers.get(0).getUrl());
        Assert.assertEquals(invoker3.getUrl(), filteredInvokers.get(1).getUrl());
    }

    @Test
    public void testRoute_methodRoute1() {
        Invocation invocation = new RpcInvocation("getFoo", new Class<?>[0], new Object[0]);
        // Test filter condition
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService?methods=setFoo,getFoo&unit=2"));
        Invoker<String> invoker2 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService?methods=setFoo,getFoo&unit=1"));
        Invoker<String> invoker3 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.5:20880/com.foo.BarService?methods=setFoo,getFoo&unit=2"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        // consumer have not unit value, we expect find all available invokers
        List<Invoker<String>> filteredInvokers = router.route(invokers,
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?methods=setFoo,getFoo,findFoo"), invocation);

        Assert.assertEquals(3, filteredInvokers.size());
    }

    @Test
    public void testRoute_methodRoute2() {
        Invocation invocation = new RpcInvocation("getFoo", new Class<?>[0], new Object[0]);
        // Test filter condition
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService?methods=setFoo,getFoo"));
        Invoker<String> invoker2 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService?methods=setFoo,getFoo"));
        Invoker<String> invoker3 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.5:20880/com.foo.BarService?methods=setFoo,getFoo"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        // consumer have unit value but service does't have, we expect find all available invokers
        List<Invoker<String>> filteredInvokers = router.route(invokers,
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?methods=setFoo,getFoo,findFoo&unit=1"), invocation);

        Assert.assertEquals(3, filteredInvokers.size());
    }

    @Test
    public void testRoute_methodRoute3() {
        Invocation invocation = new RpcInvocation("getFoo", new Class<?>[0], new Object[0]);
        // Test filter condition
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService?methods=setFoo,getFoo"));
        Invoker<String> invoker2 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService?methods=setFoo&unit=1"));
        Invoker<String> invoker3 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.5:20880/com.foo.BarService?methods=setFoo,getFoo&unit=1"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        // consumer have unit value but service does't have, we expect find all available invokers
        List<Invoker<String>> filteredInvokers = router.route(invokers,
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?methods=setFoo,getFoo,findFoo&unit=1"), invocation);

        Assert.assertEquals(1, filteredInvokers.size());
        Assert.assertEquals(invoker3.getUrl(), filteredInvokers.get(0).getUrl());
    }

    @Test
    public void testRoute_methodRoute4() {
        Invocation invocation = new RpcInvocation(null, new Class<?>[0], new Object[0]);
        // Test filter condition
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService?methods=setFoo,getFoo"));
        Invoker<String> invoker2 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.4:20880/com.foo.BarService?methods=setFoo,getFoo&unit=1"));
        Invoker<String> invoker3 = new MockInvoker<String>(
                URL.valueOf("dubbo://10.20.3.5:20880/com.foo.BarService?methods=setFoo,getFoo"));
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);

        // consumer have unit value but service does't have, we expect find all available invokers
        List<Invoker<String>> filteredInvokers = router.route(invokers,
                URL.valueOf("consumer://127.0.0.1/com.foo.BarService?methods=setFoo,getFoo,findFoo&unit=1"), invocation);

        // we mock method name is null, we expect all available invokers
        Assert.assertEquals(3, filteredInvokers.size());
    }

    UnitRouter router = (UnitRouter)new UnitRouterFactory().getRouter(null);

}