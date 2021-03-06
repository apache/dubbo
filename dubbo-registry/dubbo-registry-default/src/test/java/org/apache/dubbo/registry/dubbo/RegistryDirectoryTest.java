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
package org.apache.dubbo.registry.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.LogUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.integration.RegistryDirectory;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance;
import org.apache.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance;
import org.apache.dubbo.rpc.cluster.router.script.ScriptRouterFactory;
import org.apache.dubbo.rpc.cluster.support.wrapper.MockClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;
import org.apache.dubbo.rpc.service.GenericService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.script.ScriptEngineManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.apache.dubbo.common.constants.CommonConstants.$INVOKE;
import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.DISABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTE_PROTOCOL;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.INVOCATION_NEED_MOCK;
import static org.apache.dubbo.rpc.cluster.Constants.MOCK_PROTOCOL;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.ROUTER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.TYPE_KEY;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RegistryDirectoryTest {

    private static boolean isScriptUnsupported = new ScriptEngineManager().getEngineByName("javascript") == null;
    RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
    Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    String service = DemoService.class.getName();
    RpcInvocation invocation = new RpcInvocation();
    URL noMeaningUrl = URL.valueOf("notsupport:/" + service + "?refer=" + URL.encode("interface=" + service));
    URL SERVICEURL = URL.valueOf("dubbo://127.0.0.1:9091/" + service + "?lazy=true&side=consumer&application=mockName");
    URL SERVICEURL2 = URL.valueOf("dubbo://127.0.0.1:9092/" + service + "?lazy=true&side=consumer&application=mockName");
    URL SERVICEURL3 = URL.valueOf("dubbo://127.0.0.1:9093/" + service + "?lazy=true&side=consumer&application=mockName");
    URL SERVICEURL_DUBBO_NOPATH = URL.valueOf("dubbo://127.0.0.1:9092" + "?lazy=true&side=consumer&application=mockName");

    private Registry registry = Mockito.mock(Registry.class);

    @BeforeEach
    public void setUp() {
        ApplicationModel.setApplication("RegistryDirectoryTest");
    }

    private RegistryDirectory getRegistryDirectory(URL url) {
        RegistryDirectory registryDirectory = new RegistryDirectory(URL.class, url);
        registryDirectory.setProtocol(protocol);
        registryDirectory.setRegistry(registry);
        registryDirectory.setRouterChain(RouterChain.buildChain(url));
        Map<String, String> queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
        URL subscribeUrl = new URL(CONSUMER_PROTOCOL, "10.20.30.40", 0, url.getServiceInterface(), queryMap);
        registryDirectory.subscribe(subscribeUrl);
        // asert empty
        List invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(0, invokers.size());
        Assertions.assertFalse(registryDirectory.isAvailable());
        return registryDirectory;
    }

    private RegistryDirectory getRegistryDirectory() {
        return getRegistryDirectory(noMeaningUrl);
    }

    @Test
    public void test_Constructor_WithErrorParam() {
        try {
            new RegistryDirectory(null, null);
            fail();
        } catch (IllegalArgumentException e) {

        }
        try {
            // null url
            new RegistryDirectory(null, noMeaningUrl);
            fail();
        } catch (IllegalArgumentException e) {

        }
        try {
            // no servicekey
            new RegistryDirectory(RegistryDirectoryTest.class, URL.valueOf("dubbo://10.20.30.40:9090"));
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void test_Constructor_CheckStatus() throws Exception {
        URL url = URL.valueOf("notsupported://10.20.30.40/" + service + "?a=b").addParameterAndEncoded(REFER_KEY,
                "foo=bar&" + REGISTER_IP_KEY + "=10.20.30.40&" + INTERFACE_KEY + "=" + service);
        RegistryDirectory reg = getRegistryDirectory(url);
        Field field = reg.getClass().getSuperclass().getSuperclass().getDeclaredField("queryMap");
        ReflectUtils.makeAccessible(field);
        Map<String, String> queryMap = (Map<String, String>) field.get(reg);
        Assertions.assertEquals("bar", queryMap.get("foo"));
        URL expected = url.setProtocol(DUBBO_PROTOCOL).clearParameters()
                .addParameter("foo", "bar")
                .addParameter(REGISTER_IP_KEY, "10.20.30.40")
                .addParameter(INTERFACE_KEY, service);
        Assertions.assertEquals(expected, reg.getConsumerUrl());
    }

    @Test
    public void testNotified_Normal() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        test_Notified2invokers(registryDirectory);
        test_Notified1invokers(registryDirectory);
        test_Notified3invokers(registryDirectory);
        testforbid(registryDirectory);
    }

    /**
     * Test push only router
     */
    @Test
    public void testNotified_Normal_withRouters() {
        LogUtil.start();
        RegistryDirectory registryDirectory = getRegistryDirectory();
        test_Notified1invokers(registryDirectory);
        test_Notified_only_routers(registryDirectory);
        Assertions.assertTrue(registryDirectory.isAvailable());
        Assertions.assertTrue(LogUtil.checkNoError(), "notify no invoker urls ,should not error");
        LogUtil.stop();
        test_Notified2invokers(registryDirectory);

    }

    @Test
    public void testNotified_WithError() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        List<URL> serviceUrls = new ArrayList<URL>();
        // ignore error log
        URL badurl = URL.valueOf("notsupported://127.0.0.1/" + service);
        serviceUrls.add(badurl);
        serviceUrls.add(SERVICEURL);

        registryDirectory.notify(serviceUrls);
        Assertions.assertTrue(registryDirectory.isAvailable());
        List invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());
    }

    @Test
    public void testNotified_WithDuplicateUrls() {
        List<URL> serviceUrls = new ArrayList<URL>();
        // ignore error log
        serviceUrls.add(SERVICEURL);
        serviceUrls.add(SERVICEURL);

        RegistryDirectory registryDirectory = getRegistryDirectory();
        registryDirectory.notify(serviceUrls);
        List invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());
    }

    // forbid
    private void testforbid(RegistryDirectory registryDirectory) {
        invocation = new RpcInvocation();
        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(new URL(EMPTY_PROTOCOL, ANYHOST_VALUE, 0, service, CATEGORY_KEY, PROVIDERS_CATEGORY));
        registryDirectory.notify(serviceUrls);
        Assertions.assertFalse(registryDirectory.isAvailable(),
            "invokers size=0 ,then the registry directory is not available");
        try {
            registryDirectory.list(invocation);
            fail("forbid must throw RpcException");
        } catch (RpcException e) {
            Assertions.assertEquals(RpcException.FORBIDDEN_EXCEPTION, e.getCode());
        }
    }

    //The test call is independent of the path of the registry url
    @Test
    public void test_NotifiedDubbo1() {
        URL errorPathUrl = URL.valueOf("notsupport:/" + "xxx" + "?refer=" + URL.encode("interface=" + service));
        RegistryDirectory registryDirectory = getRegistryDirectory(errorPathUrl);
        List<URL> serviceUrls = new ArrayList<URL>();
        URL Dubbo1URL = URL.valueOf("dubbo://127.0.0.1:9098?lazy=true");
        serviceUrls.add(Dubbo1URL.addParameter("methods", "getXXX"));
        registryDirectory.notify(serviceUrls);
        Assertions.assertTrue(registryDirectory.isAvailable());

        invocation = new RpcInvocation();

        List<Invoker<DemoService>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());

        invocation.setMethodName("getXXX");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals(DemoService.class.getName(), invokers.get(0).getUrl().getPath());
    }

    // notify one invoker
    private void test_Notified_only_routers(RegistryDirectory registryDirectory) {
        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(URL.valueOf("empty://127.0.0.1/?category=routers"));
        registryDirectory.notify(serviceUrls);
    }

    // notify one invoker
    private void test_Notified1invokers(RegistryDirectory registryDirectory) {

        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1").addParameter(APPLICATION_KEY, "mockApplicationName"));// .addParameter("refer.autodestroy", "true")
        registryDirectory.notify(serviceUrls);
        Assertions.assertTrue(registryDirectory.isAvailable());

        invocation = new RpcInvocation();

        List invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());

        invocation.setMethodName("getXXX");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());

        invocation.setMethodName("getXXX1");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());

        invocation.setMethodName("getXXX2");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());
    }

    // 2 invokers===================================
    private void test_Notified2invokers(RegistryDirectory registryDirectory) {
        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));

        registryDirectory.notify(serviceUrls);
        Assertions.assertTrue(registryDirectory.isAvailable());

        invocation = new RpcInvocation();

        List invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());

        invocation.setMethodName("getXXX");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());

        invocation.setMethodName("getXXX1");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());
    }

    // 3 invoker notifications===================================
    private void test_Notified3invokers(RegistryDirectory registryDirectory) {
        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1,getXXX2,getXXX3"));

        registryDirectory.notify(serviceUrls);
        Assertions.assertTrue(registryDirectory.isAvailable());

        invocation = new RpcInvocation();

        List invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(3, invokers.size());

        invocation.setMethodName("getXXX");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(3, invokers.size());

        invocation.setMethodName("getXXX1");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(3, invokers.size());

        invocation.setMethodName("getXXX2");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(3, invokers.size());

        invocation.setMethodName("getXXX3");
        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(3, invokers.size());
    }

    @Test
    public void testParametersMerge() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        URL regurl = noMeaningUrl.addParameter("test", "reg").addParameterAndEncoded(REFER_KEY,
                "key=query&" + LOADBALANCE_KEY + "=" + LeastActiveLoadBalance.NAME);
        RegistryDirectory<RegistryDirectoryTest> registryDirectory2 = new RegistryDirectory(
                RegistryDirectoryTest.class,
                regurl);
        registryDirectory2.setProtocol(protocol);

        List<URL> serviceUrls = new ArrayList<URL>();
        // The parameters of the inspection registry need to be cleared
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
            registryDirectory.notify(serviceUrls);

            invocation = new RpcInvocation();
            List invokers = registryDirectory.list(invocation);

            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assertions.assertNull(url.getParameter("key"));
        }
        // The parameters of the provider for the inspection service need merge
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX2").addParameter("key", "provider"));

            registryDirectory.notify(serviceUrls);
            invocation = new RpcInvocation();
            List invokers = registryDirectory.list(invocation);

            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assertions.assertEquals("provider", url.getParameter("key"));
        }
        // The parameters of the test service query need to be with the providermerge.
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX3").addParameter("key", "provider"));
            registryDirectory2.setRegistry(registry);
            registryDirectory2.setRouterChain(RouterChain.buildChain(noMeaningUrl));
            registryDirectory2.subscribe(noMeaningUrl);
            registryDirectory2.notify(serviceUrls);
            invocation = new RpcInvocation();
            List invokers = registryDirectory2.list(invocation);

            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assertions.assertEquals("query", url.getParameter("key"));
        }

        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
            registryDirectory.notify(serviceUrls);

            invocation = new RpcInvocation();
            List invokers = registryDirectory.list(invocation);

            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assertions.assertFalse(url.getParameter(Constants.CHECK_KEY, false));
        }
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter(LOADBALANCE_KEY, RoundRobinLoadBalance.NAME));
            registryDirectory2.notify(serviceUrls);

            invocation = new RpcInvocation();
            invocation.setMethodName("get");
            List invokers = registryDirectory2.list(invocation);

            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assertions.assertEquals(LeastActiveLoadBalance.NAME, url.getMethodParameter("get", LOADBALANCE_KEY));
        }
        //test geturl
        {
            Assertions.assertNull(registryDirectory2.getUrl().getParameter("mock"));
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter(MOCK_KEY, "true"));
            registryDirectory2.notify(serviceUrls);

            Assertions.assertEquals("true", ((InvokerWrapper<?>) registryDirectory2.getInvokers().get(0)).getUrl().getParameter("mock"));
        }
    }

    /**
     * When destroying, RegistryDirectory should: 1. be disconnected from Registry 2. destroy all invokers
     */
    @Test
    public void testDestroy() {
        RegistryDirectory registryDirectory = getRegistryDirectory();

        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1,getXXX2,getXXX3"));

        registryDirectory.notify(serviceUrls);
        List<Invoker> invokers = registryDirectory.list(invocation);
        Assertions.assertTrue(registryDirectory.isAvailable());
        Assertions.assertTrue(invokers.get(0).isAvailable());

        registryDirectory.destroy();
        Assertions.assertFalse(registryDirectory.isAvailable());
        Assertions.assertFalse(invokers.get(0).isAvailable());
        registryDirectory.destroy();

        List<Invoker<RegistryDirectoryTest>> cachedInvokers = registryDirectory.getInvokers();
        Map<URL, Invoker<RegistryDirectoryTest>> urlInvokerMap = registryDirectory.getUrlInvokerMap();

        Assertions.assertNull(cachedInvokers);
        Assertions.assertEquals(0, urlInvokerMap.size());
        // List<U> urls = mockRegistry.getSubscribedUrls();

        RpcInvocation inv = new RpcInvocation();
        try {
            registryDirectory.list(inv);
            fail();
        } catch (RpcException e) {
            Assertions.assertTrue(e.getMessage().contains("already destroyed"));
        }
    }

    @Test
    public void testDestroy_WithDestroyRegistry() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        CountDownLatch latch = new CountDownLatch(1);
        registryDirectory.setRegistry(new MockRegistry(latch));
        registryDirectory.subscribe(URL.valueOf("consumer://" + NetUtils.getLocalHost() + "/DemoService?category=providers"));
        registryDirectory.destroy();
        Assertions.assertEquals(0, latch.getCount());
    }

    @Test
    public void testDestroy_WithDestroyRegistry_WithError() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        registryDirectory.setRegistry(new MockRegistry(true));
        registryDirectory.destroy();
    }

    @Test
    public void testDubbo1UrlWithGenericInvocation() {

        RegistryDirectory registryDirectory = getRegistryDirectory();

        List<URL> serviceUrls = new ArrayList<URL>();
        URL serviceURL = SERVICEURL_DUBBO_NOPATH.addParameter("methods", "getXXX1,getXXX2,getXXX3");
        serviceUrls.add(serviceURL);

        registryDirectory.notify(serviceUrls);

        // Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException;
        invocation = new RpcInvocation($INVOKE, GenericService.class.getName(), "", new Class[]{String.class, String[].class, Object[].class},
                new Object[]{"getXXX1", "", new Object[]{}});

        List<Invoker> invokers = registryDirectory.list(invocation);

        Assertions.assertEquals(1, invokers.size());
//        Assertions.assertEquals(
//                serviceURL.setPath(service).addParameters("check", "false", "interface", DemoService.class.getName(), REMOTE_APPLICATION_KEY, serviceURL.getParameter(APPLICATION_KEY))
//                , invokers.get(0).getUrl()
//        );

    }

    /**
     * When the first arg of a method is String or Enum, Registry server can do parameter-value-based routing.
     */
    @Disabled("Parameter routing is not available at present.")
    @Test
    public void testParmeterRoute() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1.napoli"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1.MORGAN,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1.morgan,getXXX2,getXXX3"));

        registryDirectory.notify(serviceUrls);

        invocation = new RpcInvocation($INVOKE, GenericService.class.getName(), "",
                new Class[]{String.class, String[].class, Object[].class},
                new Object[]{"getXXX1", new String[]{"Enum"}, new Object[]{Param.MORGAN}});

        List invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());
    }

    /**
     * Empty notify cause forbidden, non-empty notify cancels forbidden state
     */
    @Test
    public void testEmptyNotifyCauseForbidden() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        List invokers = null;

        List<URL> serviceUrls = new ArrayList<URL>();
        registryDirectory.notify(serviceUrls);

        RpcInvocation inv = new RpcInvocation();
        try {
            invokers = registryDirectory.list(inv);
        } catch (RpcException e) {
            Assertions.assertEquals(RpcException.FORBIDDEN_EXCEPTION, e.getCode());
            Assertions.assertFalse(registryDirectory.isAvailable());
        }

        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1,getXXX2,getXXX3"));

        registryDirectory.notify(serviceUrls);
        inv.setMethodName("getXXX2");
        invokers = registryDirectory.list(inv);
        Assertions.assertTrue(registryDirectory.isAvailable());
        Assertions.assertEquals(3, invokers.size());
    }

    /**
     * 1. notify twice, the second time notified router rules should completely replace the former one. 2. notify with
     * no router url, do nothing to current routers 3. notify with only one router url, with router=clean, clear all
     * current routers
     */
    @Test
    public void testNotifyRouterUrls() {
        if (isScriptUnsupported) return;
        RegistryDirectory registryDirectory = getRegistryDirectory();
        URL routerurl = URL.valueOf(ROUTE_PROTOCOL + "://127.0.0.1:9096/");
        URL routerurl2 = URL.valueOf(ROUTE_PROTOCOL + "://127.0.0.1:9097/");

        List<URL> serviceUrls = new ArrayList<URL>();
        // without ROUTER_KEY, the first router should not be created.
        serviceUrls.add(routerurl.addParameter(CATEGORY_KEY, ROUTERS_CATEGORY).addParameter(TYPE_KEY, "javascript").addParameter(ROUTER_KEY, "notsupported").addParameter(RULE_KEY, "function test1(){}"));
        serviceUrls.add(routerurl2.addParameter(CATEGORY_KEY, ROUTERS_CATEGORY).addParameter(TYPE_KEY, "javascript").addParameter(ROUTER_KEY,
                ScriptRouterFactory.NAME).addParameter(RULE_KEY,
                "function test1(){}"));

        // FIXME
        /*registryDirectory.notify(serviceUrls);
        RouterChain routerChain = registryDirectory.getRouterChain();
        //default invocation selector
        Assertions.assertEquals(1 + 1, routers.size());
        Assertions.assertTrue(ScriptRouter.class == routers.get(1).getClass() || ScriptRouter.class == routers.get(0).getClass());

        registryDirectory.notify(new ArrayList<URL>());
        routers = registryDirectory.getRouters();
        Assertions.assertEquals(1 + 1, routers.size());
        Assertions.assertTrue(ScriptRouter.class == routers.get(1).getClass() || ScriptRouter.class == routers.get(0).getClass());

        serviceUrls.clear();
        serviceUrls.add(routerurl.addParameter(Constants.ROUTER_KEY, Constants.ROUTER_TYPE_CLEAR));
        registryDirectory.notify(serviceUrls);
        routers = registryDirectory.getRouters();
        Assertions.assertEquals(0 + 1, routers.size());*/
    }

    /**
     * Test whether the override rule have a high priority
     * Scene: first push override , then push invoker
     */
    @Test
    public void testNotifyoverrideUrls_beforeInvoker() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        List<URL> overrideUrls = new ArrayList<URL>();
        overrideUrls.add(URL.valueOf("override://0.0.0.0?timeout=1&connections=5"));
        registryDirectory.notify(overrideUrls);
        //The registry is initially pushed to override only, and the dirctory state should be false because there is no invoker.
        Assertions.assertFalse(registryDirectory.isAvailable());

        //After pushing two provider, the directory state is restored to true
        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(SERVICEURL.addParameter("timeout", "1000"));
        serviceUrls.add(SERVICEURL2.addParameter("timeout", "1000").addParameter("connections", "10"));

        registryDirectory.notify(serviceUrls);
        Assertions.assertTrue(registryDirectory.isAvailable());

        //Start validation of parameter values

        invocation = new RpcInvocation();

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());

        Assertions.assertEquals("1", invokers.get(0).getUrl().getParameter("timeout"), "override rute must be first priority");
        Assertions.assertEquals("5", invokers.get(0).getUrl().getParameter("connections"), "override rute must be first priority");
    }

    /**
     * Test whether the override rule have a high priority
     * Scene: first push override , then push invoker
     */
    @Test
    public void testNotifyoverrideUrls_afterInvoker() {
        RegistryDirectory registryDirectory = getRegistryDirectory();

        //After pushing two provider, the directory state is restored to true
        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(SERVICEURL.addParameter("timeout", "1000"));
        serviceUrls.add(SERVICEURL2.addParameter("timeout", "1000").addParameter("connections", "10"));

        registryDirectory.notify(serviceUrls);
        Assertions.assertTrue(registryDirectory.isAvailable());

        List<URL> overrideUrls = new ArrayList<URL>();
        overrideUrls.add(URL.valueOf("override://0.0.0.0?timeout=1&connections=5"));
        registryDirectory.notify(overrideUrls);

        //Start validation of parameter values

        invocation = new RpcInvocation();

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());

        Assertions.assertEquals("1", invokers.get(0).getUrl().getParameter("timeout"), "override rute must be first priority");
        Assertions.assertEquals("5", invokers.get(0).getUrl().getParameter("connections"), "override rute must be first priority");
    }

    /**
     * Test whether the override rule have a high priority
     * Scene: push override rules with invoker
     */
    @Test
    public void testNotifyoverrideUrls_withInvoker() {
        RegistryDirectory registryDirectory = getRegistryDirectory();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.addParameter("timeout", "1000"));
        durls.add(SERVICEURL2.addParameter("timeout", "1000").addParameter("connections", "10"));
        durls.add(URL.valueOf("override://0.0.0.0?timeout=1&connections=5"));

        registryDirectory.notify(durls);
        Assertions.assertTrue(registryDirectory.isAvailable());

        //Start validation of parameter values

        invocation = new RpcInvocation();

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());

        Assertions.assertEquals("1", invokers.get(0).getUrl().getParameter("timeout"), "override rute must be first priority");
        Assertions.assertEquals("5", invokers.get(0).getUrl().getParameter("connections"), "override rute must be first priority");
    }

    /**
     * Test whether the override rule have a high priority
     * Scene: the rules of the push are the same as the parameters of the provider
     * Expectation: no need to be re-referenced
     */
    @Test
    public void testNotifyoverrideUrls_Nouse() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.addParameter("timeout", "1"));//One is the same, one is different
        durls.add(SERVICEURL2.addParameter("timeout", "1").addParameter("connections", "5"));
        registryDirectory.notify(durls);
        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());
        Map<String, Invoker<?>> map = new HashMap<>();
        map.put(invokers.get(0).getUrl().getAddress(), invokers.get(0));
        map.put(invokers.get(1).getUrl().getAddress(), invokers.get(1));

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://0.0.0.0?timeout=1&connections=5"));
        registryDirectory.notify(durls);
        Assertions.assertTrue(registryDirectory.isAvailable());

        invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());

        Map<String, Invoker<?>> map2 = new HashMap<>();
        map2.put(invokers.get(0).getUrl().getAddress(), invokers.get(0));
        map2.put(invokers.get(1).getUrl().getAddress(), invokers.get(1));

        //The parameters are different and must be rereferenced.
        Assertions.assertNotSame(map.get(SERVICEURL.getAddress()), map2.get(SERVICEURL.getAddress()),
            "object should not same");

        //The parameters can not be rereferenced
        Assertions.assertSame(map.get(SERVICEURL2.getAddress()), map2.get(SERVICEURL2.getAddress()),
            "object should not same");
    }

    /**
     * Test override rules for a certain provider
     */
    @Test
    public void testNofityOverrideUrls_Provider() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140").addParameter("timeout", "1").addParameter(SIDE_KEY, CONSUMER_SIDE));//One is the same, one is different
        durls.add(SERVICEURL2.setHost("10.20.30.141").addParameter("timeout", "2").addParameter(SIDE_KEY, CONSUMER_SIDE));
        registryDirectory.notify(durls);

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://0.0.0.0?timeout=3"));
        durls.add(URL.valueOf("override://10.20.30.141:9092?timeout=4"));
        registryDirectory.notify(durls);

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        URL aUrl = invokers.get(0).getUrl();
        URL bUrl = invokers.get(1).getUrl();
        Assertions.assertEquals(aUrl.getHost().equals("10.20.30.140") ? "3" : "4", aUrl.getParameter("timeout"));
        Assertions.assertEquals(bUrl.getHost().equals("10.20.30.141") ? "4" : "3", bUrl.getParameter("timeout"));
    }

    /**
     * Test cleanup override rules, and sent remove rules and other override rules
     * Whether the test can be restored to the providerUrl when it is pushed
     */
    @Test
    public void testNofityOverrideUrls_Clean1() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140").addParameter("timeout", "1"));
        registryDirectory.notify(durls);

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://0.0.0.0?timeout=1000"));
        registryDirectory.notify(durls);

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://0.0.0.0?timeout=3"));
        durls.add(URL.valueOf("override://0.0.0.0"));
        registryDirectory.notify(durls);

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Invoker<?> aInvoker = invokers.get(0);
        //Need to be restored to the original providerUrl
        Assertions.assertEquals("3", aInvoker.getUrl().getParameter("timeout"));
    }

    /**
     * The test clears the override rule and only sends the override cleanup rules
     * Whether the test can be restored to the providerUrl when it is pushed
     */
    @Test
    public void testNofityOverrideUrls_CleanOnly() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140").addParameter("timeout", "1"));
        registryDirectory.notify(durls);
        Assertions.assertNull(((InvokerWrapper<?>) registryDirectory.getInvokers().get(0)).getUrl().getParameter("mock"));

        //override
        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://0.0.0.0?timeout=1000&mock=fail"));
        registryDirectory.notify(durls);
        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Invoker<?> aInvoker = invokers.get(0);
        Assertions.assertEquals("1000", aInvoker.getUrl().getParameter("timeout"));
        Assertions.assertEquals("fail", ((InvokerWrapper<?>) registryDirectory.getInvokers().get(0)).getUrl().getParameter("mock"));

        //override clean
        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://0.0.0.0/dubbo.test.api.HelloService"));
        registryDirectory.notify(durls);
        invokers = registryDirectory.list(invocation);
        aInvoker = invokers.get(0);
        //Need to be restored to the original providerUrl
        Assertions.assertEquals("1", aInvoker.getUrl().getParameter("timeout"));

        Assertions.assertNull(((InvokerWrapper<?>) registryDirectory.getInvokers().get(0)).getUrl().getParameter("mock"));
    }

    /**
     * Test the simultaneous push to clear the override and the override for a certain provider
     * See if override can take effect
     */
    @Test
    public void testNofityOverrideUrls_CleanNOverride() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140").addParameter("timeout", "1"));
        registryDirectory.notify(durls);

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://0.0.0.0?timeout=3"));
        durls.add(URL.valueOf("override://0.0.0.0"));
        durls.add(URL.valueOf("override://10.20.30.140:9091?timeout=4"));
        registryDirectory.notify(durls);

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Invoker<?> aInvoker = invokers.get(0);
        Assertions.assertEquals("4", aInvoker.getUrl().getParameter("timeout"));
    }

    /**
     * Test override disables all service providers through enable=false
     * Expectation: all service providers can not be disabled through override.
     */
    @Test
    public void testNofityOverrideUrls_disabled_allProvider() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140"));
        durls.add(SERVICEURL.setHost("10.20.30.141"));
        registryDirectory.notify(durls);

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://0.0.0.0?" + ENABLED_KEY + "=false"));
        registryDirectory.notify(durls);

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        //All service providers can not be disabled through override.
        Assertions.assertEquals(2, invokers.size());
    }

    /**
     * Test override disables a specified service provider through enable=false
     * It is expected that a specified service provider can be disable.
     */
    @Test
    public void testNofityOverrideUrls_disabled_specifiedProvider() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140"));
        durls.add(SERVICEURL.setHost("10.20.30.141"));
        registryDirectory.notify(durls);

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://10.20.30.140:9091?" + DISABLED_KEY + "=true"));
        registryDirectory.notify(durls);

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("10.20.30.141", invokers.get(0).getUrl().getHost());

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("empty://0.0.0.0?" + DISABLED_KEY + "=true&" + CATEGORY_KEY + "=" + CONFIGURATORS_CATEGORY));
        registryDirectory.notify(durls);
        List<Invoker<?>> invokers2 = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers2.size());
    }

    /**
     * Test override disables a specified service provider through enable=false
     * It is expected that a specified service provider can be disable.
     */
    @Test
    public void testNofity_To_Decrease_provider() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140"));
        durls.add(SERVICEURL.setHost("10.20.30.141"));
        registryDirectory.notify(durls);

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());

        durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140"));
        registryDirectory.notify(durls);
        List<Invoker<?>> invokers2 = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers2.size());
        Assertions.assertEquals("10.20.30.140", invokers2.get(0).getUrl().getHost());

        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("empty://0.0.0.0?" + DISABLED_KEY + "=true&" + CATEGORY_KEY + "=" + CONFIGURATORS_CATEGORY));
        registryDirectory.notify(durls);
        List<Invoker<?>> invokers3 = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers3.size());
    }

    /**
     * Test override disables a specified service provider through enable=false
     * It is expected that a specified service provider can be disable.
     */
    @Test
    public void testNofity_disabled_specifiedProvider() {
        RegistryDirectory registryDirectory = getRegistryDirectory();
        invocation = new RpcInvocation();

        // Initially disable
        List<URL> durls = new ArrayList<URL>();
        durls.add(SERVICEURL.setHost("10.20.30.140").addParameter(ENABLED_KEY, "false"));
        durls.add(SERVICEURL.setHost("10.20.30.141"));
        registryDirectory.notify(durls);

        List<Invoker<?>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());
        Assertions.assertEquals("10.20.30.141", invokers.get(0).getUrl().getHost());

        //Enabled by override rule
        durls = new ArrayList<URL>();
        durls.add(URL.valueOf("override://10.20.30.140:9091?" + DISABLED_KEY + "=false"));
        registryDirectory.notify(durls);
        List<Invoker<?>> invokers2 = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers2.size());
    }

    @Test
    public void testNotifyRouterUrls_Clean() {
        if (isScriptUnsupported) return;
        RegistryDirectory registryDirectory = getRegistryDirectory();
        URL routerurl = URL.valueOf(ROUTE_PROTOCOL + "://127.0.0.1:9096/").addParameter(ROUTER_KEY,
                "javascript").addParameter(RULE_KEY,
                "function test1(){}").addParameter(ROUTER_KEY,
                "script"); // FIX
        // BAD

        List<URL> serviceUrls = new ArrayList<URL>();
        // without ROUTER_KEY, the first router should not be created.
        serviceUrls.add(routerurl);
        registryDirectory.notify(serviceUrls);
        // FIXME
       /* List routers = registryDirectory.getRouters();
        Assertions.assertEquals(1 + 1, routers.size());

        serviceUrls.clear();
        serviceUrls.add(routerurl.addParameter(Constants.ROUTER_KEY, Constants.ROUTER_TYPE_CLEAR));
        registryDirectory.notify(serviceUrls);
        routers = registryDirectory.getRouters();
        Assertions.assertEquals(0 + 1, routers.size());*/
    }

    /**
     * Test mock provider distribution
     */
    @Test
    public void testNotify_MockProviderOnly() {
        RegistryDirectory registryDirectory = getRegistryDirectory();

        List<URL> serviceUrls = new ArrayList<URL>();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL.setProtocol(MOCK_PROTOCOL));

        registryDirectory.notify(serviceUrls);
        Assertions.assertTrue(registryDirectory.isAvailable());
        invocation = new RpcInvocation();

        List invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());

        RpcInvocation mockinvocation = new RpcInvocation();
        mockinvocation.setAttachment(INVOCATION_NEED_MOCK, "true");
        invokers = registryDirectory.list(mockinvocation);
        Assertions.assertEquals(1, invokers.size());
    }

    // mock protocol

    //Test the matching of protocol and select only the matched protocol for refer
    @Test
    public void test_Notified_acceptProtocol0() {
        URL errorPathUrl = URL.valueOf("notsupport:/xxx?refer=" + URL.encode("interface=" + service));
        RegistryDirectory registryDirectory = getRegistryDirectory(errorPathUrl);
        List<URL> serviceUrls = new ArrayList<URL>();
        URL dubbo1URL = URL.valueOf("dubbo://127.0.0.1:9098?lazy=true&methods=getXXX");
        URL dubbo2URL = URL.valueOf("injvm://127.0.0.1:9099?lazy=true&methods=getXXX");
        serviceUrls.add(dubbo1URL);
        serviceUrls.add(dubbo2URL);
        registryDirectory.notify(serviceUrls);

        invocation = new RpcInvocation();

        List<Invoker<DemoService>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());
    }

    //Test the matching of protocol and select only the matched protocol for refer
    @Test
    public void test_Notified_acceptProtocol1() {
        URL errorPathUrl = URL.valueOf("notsupport:/xxx");
        errorPathUrl = errorPathUrl.addParameterAndEncoded(REFER_KEY, "interface=" + service + "&protocol=dubbo");
        RegistryDirectory registryDirectory = getRegistryDirectory(errorPathUrl);
        List<URL> serviceUrls = new ArrayList<URL>();
        URL dubbo1URL = URL.valueOf("dubbo://127.0.0.1:9098?lazy=true&methods=getXXX");
        URL dubbo2URL = URL.valueOf("injvm://127.0.0.1:9098?lazy=true&methods=getXXX");
        serviceUrls.add(dubbo1URL);
        serviceUrls.add(dubbo2URL);
        registryDirectory.notify(serviceUrls);

        invocation = new RpcInvocation();

        List<Invoker<DemoService>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(1, invokers.size());
    }

    //Test the matching of protocol and select only the matched protocol for refer
    @Test
    public void test_Notified_acceptProtocol2() {
        URL errorPathUrl = URL.valueOf("notsupport:/xxx");
        errorPathUrl = errorPathUrl.addParameterAndEncoded(REFER_KEY, "interface=" + service + "&protocol=dubbo,injvm");
        RegistryDirectory registryDirectory = getRegistryDirectory(errorPathUrl);
        List<URL> serviceUrls = new ArrayList<URL>();
        URL dubbo1URL = URL.valueOf("dubbo://127.0.0.1:9098?lazy=true&methods=getXXX");
        URL dubbo2URL = URL.valueOf("injvm://127.0.0.1:9099?lazy=true&methods=getXXX");
        serviceUrls.add(dubbo1URL);
        serviceUrls.add(dubbo2URL);
        registryDirectory.notify(serviceUrls);

        invocation = new RpcInvocation();

        List<Invoker<DemoService>> invokers = registryDirectory.list(invocation);
        Assertions.assertEquals(2, invokers.size());
    }

    @Test
    public void test_Notified_withGroupFilter() {
        URL directoryUrl = noMeaningUrl.addParameterAndEncoded(REFER_KEY, "interface" + service + "&group=group1,group2");
        RegistryDirectory directory = this.getRegistryDirectory(directoryUrl);
        URL provider1 = URL.valueOf("dubbo://10.134.108.1:20880/" + service + "?methods=getXXX&group=group1&mock=false&application=mockApplication");
        URL provider2 = URL.valueOf("dubbo://10.134.108.1:20880/" + service + "?methods=getXXX&group=group2&mock=false&application=mockApplication");

        List<URL> providers = new ArrayList<>();
        providers.add(provider1);
        providers.add(provider2);
        directory.notify(providers);

        invocation = new RpcInvocation();
        invocation.setMethodName("getXXX");
        List<Invoker<DemoService>> invokers = directory.list(invocation);

        Assertions.assertEquals(2, invokers.size());
        Assertions.assertTrue(invokers.get(0) instanceof MockClusterInvoker);
        Assertions.assertTrue(invokers.get(1) instanceof MockClusterInvoker);

        directoryUrl = noMeaningUrl.addParameterAndEncoded(REFER_KEY, "interface" + service + "&group=group1");
        directory = this.getRegistryDirectory(directoryUrl);
        directory.notify(providers);

        invokers = directory.list(invocation);

        Assertions.assertEquals(2, invokers.size());
        Assertions.assertFalse(invokers.get(0) instanceof MockClusterInvoker);
        Assertions.assertFalse(invokers.get(1) instanceof MockClusterInvoker);
    }

    enum Param {
        MORGAN,
    }

    private interface DemoService {
    }

    private static class MockRegistry implements Registry {

        CountDownLatch latch;
        boolean destroyWithError;

        public MockRegistry(CountDownLatch latch) {
            this.latch = latch;
        }

        public MockRegistry(boolean destroyWithError) {
            this.destroyWithError = destroyWithError;
        }

        @Override
        public void register(URL url) {

        }

        @Override
        public void unregister(URL url) {

        }

        @Override
        public void subscribe(URL url, NotifyListener listener) {

        }

        @Override
        public void unsubscribe(URL url, NotifyListener listener) {
            if (latch != null) latch.countDown();
        }

        @Override
        public List<URL> lookup(URL url) {
            return null;
        }

        public URL getUrl() {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void destroy() {
            if (destroyWithError) {
                throw new RpcException("test exception ignore.");
            }
        }
    }
}
