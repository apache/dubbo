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
package org.apache.dubbo.rpc.cluster.support.wrapper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.metrics.event.MetricsDispatcher;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.support.MockProtocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

class MockClusterInvokerTest {

    List<Invoker<IHelloService>> invokers = new ArrayList<Invoker<IHelloService>>();

    @BeforeEach
    public void beforeMethod() {
        ApplicationModel.defaultModel().getBeanFactory().registerBean(MetricsDispatcher.class);
        invokers.clear();
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerInvoke_normal() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName());
        url = url.addParameter(REFER_KEY,
                URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                        + "&" + "mock=fail"));
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        URL mockUrl = URL.valueOf("mock://localhost/" + IHelloService.class.getName()
                + "?getSomething.mock=return aa");

        Protocol protocol = new MockProtocol();
        Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
        invokers.add(mInvoker1);

        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("something", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerInvoke_failmock() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock=fail:return null"))
                .addParameter("invoke_return_error", "true");
        URL mockUrl = URL.valueOf("mock://localhost/" + IHelloService.class.getName())
                .addParameter("mock","fail:return null")
                .addParameter("getSomething.mock","return aa")
                .addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName()))
                .addParameter("invoke_return_error", "true");

        Protocol protocol = new MockProtocol();
        Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
        Invoker<IHelloService> cluster = getClusterInvokerMock(url, mInvoker1);

        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("aa", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());
    }


    /**
     * Test if mock policy works fine: force-mock
     */
    @Test
    void testMockInvokerInvoke_forcemock() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock=force:return null"));

        URL mockUrl = URL.valueOf("mock://localhost/" + IHelloService.class.getName())
                .addParameter("mock","force:return null")
                .addParameter("getSomething.mock","return aa")
                .addParameter("getSomething3xx.mock","return xx")
                .addParameter(REFER_KEY, URL.encode(PATH_KEY + "=" + IHelloService.class.getName()));

        Protocol protocol = new MockProtocol();
        Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
        Invoker<IHelloService> cluster = getClusterInvokerMock(url, mInvoker1);

        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("aa", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());



    }

    @Test
    void testMockInvokerInvoke_forcemock_defaultreturn() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock=force"));

        Invoker<IHelloService> cluster = getClusterInvoker(url);
        URL mockUrl = URL.valueOf("mock://localhost/" + IHelloService.class.getName()
                + "?getSomething.mock=return aa&getSomething3xx.mock=return xx&sayHello.mock=return ")
                .addParameters(url.getParameters());

        Protocol protocol = new MockProtocol();
        Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
        invokers.add(mInvoker1);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        Result ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_Fock_someMethods() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getSomething.mock=fail:return x"
                                + "&" + "getSomething2.mock=force:return y"));
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("something", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("y", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething3");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("something3", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_Fock_WithOutDefault() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getSomething.mock=fail:return x"
                                + "&" + "getSomething2.mock=fail:return y"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("x", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("y", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething3");
        try {
            ret = cluster.invoke(invocation);
            Assertions.fail();
        } catch (RpcException e) {

        }
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_Fock_WithDefault() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock" + "=" + "fail:return null"
                                + "&" + "getSomething.mock" + "=" + "fail:return x"
                                + "&" + "getSomething2.mock" + "=" + "fail:return y"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("x", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("y", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething3");
        ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assertions.assertNull(ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_Fock_WithFailDefault() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock=fail:return z"
                                + "&" + "getSomething.mock=fail:return x"
                                + "&" + "getSomething2.mock=force:return y"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("x", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("y", ret.getValue());

        // If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething3");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("z", ret.getValue());

        //If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("z", ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_Fock_WithForceDefault() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock=force:return z"
                                + "&" + "getSomething.mock=fail:return x"
                                + "&" + "getSomething2.mock=force:return y"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("x", ret.getValue());

        //If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("y", ret.getValue());

        //If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething3");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("z", ret.getValue());

        //If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("z", ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_Fock_Default() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock=fail:return x"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("x", ret.getValue());

        //If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("x", ret.getValue());

        //If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assertions.assertEquals("x", ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_checkCompatible_return() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getSomething.mock=return x"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("x", ret.getValue());

        //If no mock was configured, return null directly
        invocation = new RpcInvocation();
        invocation.setMethodName("getSomething3");
        try {
            ret = cluster.invoke(invocation);
            Assertions.fail("fail invoke");
        } catch (RpcException e) {

        }
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock=true"
                                + "&" + "proxy=jdk"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("somethingmock", ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock2() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=fail"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("somethingmock", ret.getValue());
    }

    /**
     * Test if mock policy works fine: fail-mock
     */
    @Test
    void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock3() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName() + "&" + "mock=force"));
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("somethingmock", ret.getValue());
    }

    @Test
    void testMockInvokerFromOverride_Invoke_check_String() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter("getSomething.mock", "force:return 1688")
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getSomething.mock=force:return 1688"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assertions.assertTrue(ret.getValue() instanceof String, "result type must be String but was : " + ret.getValue().getClass());
        Assertions.assertEquals("1688", ret.getValue());
    }

    @Test
    void testMockInvokerFromOverride_Invoke_check_int() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getInt1.mock=force:return 1688"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getInt1");
        Result ret = cluster.invoke(invocation);
        Assertions.assertTrue(ret.getValue() instanceof Integer, "result type must be integer but was : " + ret.getValue().getClass());
        Assertions.assertEquals(new Integer(1688), (Integer) ret.getValue());
    }

    @Test
    void testMockInvokerFromOverride_Invoke_check_boolean() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getBoolean1.mock=force:return true"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getBoolean1");
        Result ret = cluster.invoke(invocation);
        Assertions.assertTrue(ret.getValue() instanceof Boolean, "result type must be Boolean but was : " + ret.getValue().getClass());
        Assertions.assertTrue(Boolean.parseBoolean(ret.getValue().toString()));
    }

    @Test
    void testMockInvokerFromOverride_Invoke_check_Boolean() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getBoolean2.mock=force:return true"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getBoolean2");
        Result ret = cluster.invoke(invocation);
        Assertions.assertTrue(Boolean.parseBoolean(ret.getValue().toString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testMockInvokerFromOverride_Invoke_check_ListString_empty() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getListString.mock=force:return empty"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getListString");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals(0, ((List<String>) ret.getValue()).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testMockInvokerFromOverride_Invoke_check_ListString() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getListString.mock=force:return [\"hi\",\"hi2\"]"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getListString");
        Result ret = cluster.invoke(invocation);
        List<String> rl = (List<String>) ret.getValue();
        Assertions.assertEquals(2, rl.size());
        Assertions.assertEquals("hi", rl.get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testMockInvokerFromOverride_Invoke_check_ListPojo_empty() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getUsers.mock=force:return empty"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getUsers");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals(0, ((List<User>) ret.getValue()).size());
    }
    @Test
    void testMockInvokerFromOverride_Invoke_check_ListPojoAsync() throws ExecutionException, InterruptedException {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
            .addParameter(REFER_KEY,
                URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                    + "&" + "getUsersAsync.mock=force"))
            .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getUsersAsync");
        invocation.setReturnType(CompletableFuture.class);
        Result ret = cluster.invoke(invocation);
        CompletableFuture<List<User>> cf = null;
        try {
            cf = (CompletableFuture<List<User>>) ret.recreate();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Assertions.assertEquals(2, cf.get().size());
        Assertions.assertEquals("Tommock", cf.get().get(0).getName());
    }


    @SuppressWarnings("unchecked")
    @Test
    void testMockInvokerFromOverride_Invoke_check_ListPojo() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getUsers.mock=force:return [{id:1, name:\"hi1\"}, {id:2, name:\"hi2\"}]"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getUsers");
        Result ret = cluster.invoke(invocation);
        List<User> rl = (List<User>) ret.getValue();
        System.out.println(rl);
        Assertions.assertEquals(2, rl.size());
        Assertions.assertEquals("hi1", rl.get(0).getName());
    }

    @Test
    void testMockInvokerFromOverride_Invoke_check_ListPojo_error() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getUsers.mock=force:return [{id:x, name:\"hi1\"}]"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getUsers");
        try {
            cluster.invoke(invocation);
        } catch (RpcException e) {
        }
    }

    @Test
    void testMockInvokerFromOverride_Invoke_force_throw() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getBoolean2.mock=force:throw "))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getBoolean2");
        try {
            cluster.invoke(invocation);
            Assertions.fail();
        } catch (RpcException e) {
            Assertions.assertFalse(e.isBiz(), "not custom exception");
        }
    }

    @Test
    void testMockInvokerFromOverride_Invoke_force_throwCustemException() throws Throwable {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getBoolean2.mock=force:throw org.apache.dubbo.rpc.cluster.support.wrapper.MyMockException"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getBoolean2");
        try {
            cluster.invoke(invocation).recreate();
            Assertions.fail();
        } catch (MyMockException e) {

        }
    }

    @Test
    void testMockInvokerFromOverride_Invoke_force_throwCustemExceptionNotFound() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "getBoolean2.mock=force:throw java.lang.RuntimeException2"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getBoolean2");
        try {
            cluster.invoke(invocation);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    void testMockInvokerFromOverride_Invoke_mock_false() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloService.class.getName())
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + IHelloService.class.getName()
                                + "&" + "mock=false"))
                .addParameter("invoke_return_error", "true");
        Invoker<IHelloService> cluster = getClusterInvoker(url);
        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getBoolean2");
        try {
            cluster.invoke(invocation);
            Assertions.fail();
        } catch (RpcException e) {
            Assertions.assertTrue(e.isTimeout());
        }
    }

    private Invoker<IHelloService> getClusterInvokerMock(URL url, Invoker<IHelloService> mockInvoker) {
        // As `javassist` have a strict restriction of argument types, request will fail if Invocation do not contains complete parameter type information
        final URL durl = url.addParameter("proxy", "jdk");
        invokers.clear();
        ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension("jdk");
        Invoker<IHelloService> invoker1 = proxy.getInvoker(new HelloService(), IHelloService.class, durl);
        invokers.add(invoker1);
        if (mockInvoker != null) {
            invokers.add(mockInvoker);
        }

        StaticDirectory<IHelloService> dic = new StaticDirectory<IHelloService>(durl, invokers, null);
        dic.buildRouterChain();
        AbstractClusterInvoker<IHelloService> cluster = new AbstractClusterInvoker(dic) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                if (durl.getParameter("invoke_return_error", false)) {
                    throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "test rpc exception");
                } else {
                    return ((Invoker<?>) invokers.get(0)).invoke(invocation);
                }
            }
        };
        return new MockClusterInvoker<IHelloService>(dic, cluster);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Invoker<IHelloService> getClusterInvoker(URL url) {
        return getClusterInvokerMock(url, null);
    }

    public interface IHelloService {
        String getSomething();

        String getSomething2();

        String getSomething3();

        String getSomething4();

        int getInt1();

        boolean getBoolean1();

        Boolean getBoolean2();

        List<String> getListString();

        List<User> getUsers();

        CompletableFuture<List<User>> getUsersAsync();

        void sayHello();
    }

    public static class HelloService implements IHelloService {
        public String getSomething() {
            return "something";
        }

        public String getSomething2() {
            return "something2";
        }

        public String getSomething3() {
            return "something3";
        }

        public String getSomething4() {
            throw new RpcException("getSomething4|RpcException");
        }

        public int getInt1() {
            return 1;
        }

        public boolean getBoolean1() {
            return false;
        }

        public Boolean getBoolean2() {
            return Boolean.FALSE;
        }

        public List<String> getListString() {
            return Arrays.asList(new String[]{"Tom", "Jerry"});
        }

        public List<User> getUsers() {
            return Arrays.asList(new User[]{new User(1, "Tom"), new User(2, "Jerry")});
        }

        @Override
        public CompletableFuture<List<User>> getUsersAsync() {
            CompletableFuture<List<User>> cf=new CompletableFuture<>();
            cf.complete(Arrays.asList(new User[]{new User(1, "Tom"), new User(2, "Jerry")}));
            return cf;
        }

        public void sayHello() {
            System.out.println("hello prety");
        }
    }

    public static class IHelloServiceMock implements IHelloService {
        public IHelloServiceMock() {

        }

        public String getSomething() {
            return "somethingmock";
        }

        public String getSomething2() {
            return "something2mock";
        }

        public String getSomething3() {
            return "something3mock";
        }

        public String getSomething4() {
            return "something4mock";
        }

        public List<String> getListString() {
            return Arrays.asList(new String[]{"Tommock", "Jerrymock"});
        }

        public List<User> getUsers() {
            return Arrays.asList(new User[]{new User(1, "Tommock"), new User(2, "Jerrymock")});
        }

        @Override
        public CompletableFuture<List<User>> getUsersAsync() {
            CompletableFuture<List<User>> cf=new CompletableFuture<>();
            cf.complete(Arrays.asList(new User[]{new User(1, "Tommock"), new User(2, "Jerrymock")}));
            return cf;
        }

        public int getInt1() {
            return 1;
        }

        public boolean getBoolean1() {
            return false;
        }

        public Boolean getBoolean2() {
            return Boolean.FALSE;
        }

        public void sayHello() {
            System.out.println("hello prety");
        }
    }

    public static class User {
        private int id;
        private String name;

        public User() {
        }

        public User(int id, String name) {
            super();
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }
}
