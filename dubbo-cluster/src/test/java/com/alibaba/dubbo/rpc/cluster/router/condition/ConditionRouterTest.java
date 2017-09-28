/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.router.condition;


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

public class ConditionRouterTest {

    private URL SCRIPT_URL = URL.valueOf("condition://0.0.0.0/com.foo.BarService");

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
    public void testRoute_matchWhen() {
        Invocation invocation = new RpcInvocation();

        Router router = new ConditionRouterFactory().getRouter(getRouteUrl(" => host = 1.2.3.4"));
        boolean matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"),
                invocation);
        Assert.assertEquals(true, matchWhen);

        router = new ConditionRouterFactory()
                .getRouter(getRouteUrl("host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"),
                invocation);
        Assert.assertEquals(true, matchWhen);

        router = new ConditionRouterFactory()
                .getRouter(getRouteUrl("host = 2.2.2.2,1.1.1.1,3.3.3.3 & host !=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"),
                invocation);
        Assert.assertEquals(false, matchWhen);

        router = new ConditionRouterFactory()
                .getRouter(getRouteUrl("host !=4.4.4.4 & host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"),
                invocation);
        Assert.assertEquals(true, matchWhen);

        router = new ConditionRouterFactory()
                .getRouter(getRouteUrl("host !=4.4.4.* & host = 2.2.2.2,1.1.1.1,3.3.3.3 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"),
                invocation);
        Assert.assertEquals(true, matchWhen);

        router = new ConditionRouterFactory()
                .getRouter(getRouteUrl("host = 2.2.2.2,1.1.1.*,3.3.3.3 & host != 1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"),
                invocation);
        Assert.assertEquals(false, matchWhen);

        router = new ConditionRouterFactory()
                .getRouter(getRouteUrl("host = 2.2.2.2,1.1.1.*,3.3.3.3 & host != 1.1.1.2 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router).matchWhen(URL.valueOf("consumer://1.1.1.1/com.foo.BarService"),
                invocation);
        Assert.assertEquals(true, matchWhen);
    }

    @Test
    public void testRoute_matchFilter() {
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        Invoker<String> invoker1 = new MockInvoker<String>(URL.valueOf("dubbo://10.20.3.3:20880/com.foo.BarService"));
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

        List<Invoker<String>> fileredInvokers1 = router1.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> fileredInvokers2 = router2.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> fileredInvokers3 = router3.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> fileredInvokers4 = router4.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        List<Invoker<String>> fileredInvokers5 = router5.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(1, fileredInvokers1.size());
        Assert.assertEquals(0, fileredInvokers2.size());
        Assert.assertEquals(0, fileredInvokers3.size());
        Assert.assertEquals(1, fileredInvokers4.size());
        Assert.assertEquals(2, fileredInvokers5.size());
    }

    @Test
    public void testRoute_methodRoute() {
        Invocation invocation = new RpcInvocation("getFoo", new Class<?>[0], new Object[0]);
        // 有多个方法时，没法匹配
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("methods=getFoo => host = 1.2.3.4"));
        boolean matchWhen = ((ConditionRouter) router).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=setFoo,getFoo,findFoo"), invocation);
        Assert.assertEquals(true, matchWhen);
        // 只有一个方法时，可以匹配
        matchWhen = ((ConditionRouter) router).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assert.assertEquals(true, matchWhen);
        // 方法和其他参数一起，测试无影响
        Router router2 = new ConditionRouterFactory()
                .getRouter(getRouteUrl("methods=getFoo & host!=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router2).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assert.assertEquals(false, matchWhen);

        Router router3 = new ConditionRouterFactory()
                .getRouter(getRouteUrl("methods=getFoo & host=1.1.1.1 => host = 1.2.3.4"));
        matchWhen = ((ConditionRouter) router3).matchWhen(
                URL.valueOf("consumer://1.1.1.1/com.foo.BarService?methods=getFoo"), invocation);
        Assert.assertEquals(true, matchWhen);
        // filter过滤条件测试
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
        List<Invoker<String>> fileredInvokers1 = router4.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), invocation);
        Assert.assertEquals(1, fileredInvokers1.size());

        Router router5 = new ConditionRouterFactory().getRouter(getRouteUrl(
                "host = " + NetUtils.getLocalHost() + " & methods = unvalidmethod => " + " host = 10.20.3.3")
                .addParameter(Constants.FORCE_KEY, String.valueOf(true)));
        List<Invoker<String>> fileredInvokers2 = router5.route(invokers,
                URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), invocation);
        Assert.assertEquals(3, fileredInvokers2.size());
        // 调用不存在的方法时
    }

    @Test
    public void testRoute_ReturnFalse() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => false"));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(0, fileredInvokers.size());
    }

    @Test
    public void testRoute_ReturnEmpty() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => "));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(0, fileredInvokers.size());
    }

    @Test
    public void testRoute_ReturnAll() {
        Router router = new ConditionRouterFactory().getRouter(getRouteUrl("host = " + NetUtils.getLocalHost() + " => " + " host = " + NetUtils.getLocalHost()));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        invokers.add(new MockInvoker<String>());
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(invokers, fileredInvokers);
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
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(2, fileredInvokers.size());
        Assert.assertEquals(invoker2, fileredInvokers.get(0));
        Assert.assertEquals(invoker3, fileredInvokers.get(1));
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
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(2, fileredInvokers.size());
        Assert.assertEquals(invoker2, fileredInvokers.get(0));
        Assert.assertEquals(invoker3, fileredInvokers.get(1));
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
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(2, fileredInvokers.size());
        Assert.assertEquals(invoker2, fileredInvokers.get(0));
        Assert.assertEquals(invoker3, fileredInvokers.get(1));
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
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(2, fileredInvokers.size());
        Assert.assertEquals(invoker2, fileredInvokers.get(0));
        Assert.assertEquals(invoker3, fileredInvokers.get(1));
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
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(invokers, fileredInvokers);
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
        List<Invoker<String>> fileredInvokers = router.route(invokers, URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/com.foo.BarService"), new RpcInvocation());
        Assert.assertEquals(0, fileredInvokers.size());
    }

}