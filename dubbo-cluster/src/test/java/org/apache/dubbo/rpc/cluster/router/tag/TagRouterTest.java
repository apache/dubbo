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
package org.apache.dubbo.rpc.cluster.router.tag;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TagRouterTest {

    private URL tagUrl = new URL("tag"
            , Constants.ANYHOST_VALUE, 0
            , Constants.ANY_VALUE)
            .addParameters(
                    Constants.RUNTIME_KEY, "true"
            );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @After
    public void teardown() throws Exception {
        RpcContext.getContext().clearAttachments();
    }

    @Test
    public void testRoute_matchTag() {

        RpcContext.getContext().setAttachment(Constants.REQUEST_TAG_KEY, "red");

        List<Invoker<String>> invokers = new ArrayList<>();
        Invoker<String> redInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.1:20880/com.foo.BarService?tag=red"));
        Invoker<String> yellowInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.2:20880/com.foo.BarService?tag=yellow"));
        Invoker<String> blueInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.3:20880/com.foo.BarService?tag=blue"));
        Invoker<String> defaultInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.4:20880/com.foo.BarService"));

        invokers.add(redInvoker);
        invokers.add(yellowInvoker);
        invokers.add(blueInvoker);
        invokers.add(defaultInvoker);

        Router tagRouter = new TagRouterFactory().getRouter(tagUrl);
        List<Invoker<String>> filteredInvokers = tagRouter.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertTrue(filteredInvokers.contains(redInvoker));
        Assert.assertFalse(filteredInvokers.contains(yellowInvoker));
        Assert.assertFalse(filteredInvokers.contains(blueInvoker));
        Assert.assertFalse(filteredInvokers.contains(defaultInvoker));
    }

    @Test
    public void testRoute_matchDefault() {

        RpcContext.getContext().setAttachment(Constants.REQUEST_TAG_KEY, "");

        List<Invoker<String>> invokers = new ArrayList<>();
        Invoker<String> redInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.1:20880/com.foo.BarService?tag=red"));
        Invoker<String> yellowInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.2:20880/com.foo.BarService?tag=yellow"));
        Invoker<String> blueInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.3:20880/com.foo.BarService?tag=blue"));
        Invoker<String> defaultInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.4:20880/com.foo.BarService"));

        invokers.add(redInvoker);
        invokers.add(yellowInvoker);
        invokers.add(blueInvoker);
        invokers.add(defaultInvoker);

        Router tagRouter = new TagRouterFactory().getRouter(tagUrl);
        List<Invoker<String>> filteredInvokers = tagRouter.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertTrue(filteredInvokers.contains(defaultInvoker));
        Assert.assertFalse(filteredInvokers.contains(yellowInvoker));
        Assert.assertFalse(filteredInvokers.contains(blueInvoker));
        Assert.assertFalse(filteredInvokers.contains(redInvoker));
    }

    @Test
    public void testRoute_requestWithTag_shouldDowngrade() {

        RpcContext.getContext().setAttachment(Constants.REQUEST_TAG_KEY, "black");

        List<Invoker<String>> invokers = new ArrayList<>();
        Invoker<String> redInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.1:20880/com.foo.BarService?tag=red"));
        Invoker<String> yellowInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.2:20880/com.foo.BarService?tag=yellow"));
        Invoker<String> blueInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.3:20880/com.foo.BarService?tag=blue"));
        Invoker<String> defaultInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.4:20880/com.foo.BarService"));

        invokers.add(redInvoker);
        invokers.add(yellowInvoker);
        invokers.add(blueInvoker);
        invokers.add(defaultInvoker);

        Router tagRouter = new TagRouterFactory().getRouter(tagUrl);
        List<Invoker<String>> filteredInvokers = tagRouter.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertTrue(filteredInvokers.contains(defaultInvoker));
        Assert.assertFalse(filteredInvokers.contains(yellowInvoker));
        Assert.assertFalse(filteredInvokers.contains(blueInvoker));
        Assert.assertFalse(filteredInvokers.contains(redInvoker));
    }

    @Test
    public void testRoute_requestWithoutTag_shouldNotDowngrade() {

        RpcContext.getContext().setAttachment(Constants.REQUEST_TAG_KEY, "");

        List<Invoker<String>> invokers = new ArrayList<>();
        Invoker<String> redInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.1:20880/com.foo.BarService?tag=red"));
        Invoker<String> yellowInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.2:20880/com.foo.BarService?tag=yellow"));
        Invoker<String> blueInvoker = new MockInvoker<>(URL.valueOf(
                "dubbo://10.20.3.3:20880/com.foo.BarService?tag=blue"));

        invokers.add(redInvoker);
        invokers.add(yellowInvoker);
        invokers.add(blueInvoker);

        Router tagRouter = new TagRouterFactory().getRouter(tagUrl);
        List<Invoker<String>> filteredInvokers = tagRouter.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(0, filteredInvokers.size());
    }

    @Test
    public void testRoute_createBySpi() {
        URL zkProvider = URL.valueOf("zookeeper://10.20.3.1:20880/com.foo.BarService?router=tag");
        String parameter = zkProvider.getParameter(Constants.ROUTER_KEY);
        RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getExtension(parameter);
        Router tagRouter = routerFactory.getRouter(zkProvider);
        Assert.assertTrue(tagRouter instanceof TagRouter);
    }

}
