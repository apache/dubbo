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
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

class MockProviderRpcExceptionTest {

    List<Invoker<IHelloRpcService>> invokers = new ArrayList<Invoker<IHelloRpcService>>();

    @BeforeEach
    public void beforeMethod() {
        ApplicationModel.defaultModel().getBeanFactory().registerBean(MetricsDispatcher.class);
        invokers.clear();
    }

    /**
     * Test if mock policy works fine: ProviderRpcException
     */
    @Test
    void testMockInvokerProviderRpcException() {
        URL url = URL.valueOf("remote://1.2.3.4/" + IHelloRpcService.class.getName());
        url = url.addParameter(MOCK_KEY, "true").addParameter("invoke_return_error", "true")
                .addParameter(REFER_KEY,
                        URL.encode(PATH_KEY + "=" + MockProviderRpcExceptionTest.IHelloRpcService.class.getName()
                                + "&" + "mock=true"
                                + "&" + "proxy=jdk"));
        Invoker<IHelloRpcService> cluster = getClusterInvoker(url);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("getSomething4");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("something4mock", ret.getValue());

    }


    private Invoker<IHelloRpcService> getClusterInvokerMock(URL url, Invoker<IHelloRpcService> mockInvoker) {
        // As `javassist` have a strict restriction of argument types, request will fail if Invocation do not contains complete parameter type information
        final URL durl = url.addParameter("proxy", "jdk");
        invokers.clear();
        ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension("jdk");
        Invoker<IHelloRpcService> invoker1 = proxy.getInvoker(new HelloRpcService(), IHelloRpcService.class, durl);
        invokers.add(invoker1);
        if (mockInvoker != null) {
            invokers.add(mockInvoker);
        }

        StaticDirectory<IHelloRpcService> dic = new StaticDirectory<IHelloRpcService>(durl, invokers, null);
        dic.buildRouterChain();
        AbstractClusterInvoker<IHelloRpcService> cluster = new AbstractClusterInvoker(dic) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                if (durl.getParameter("invoke_return_error", false)) {
                    throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "test rpc exception ");
                } else {
                    return ((Invoker<?>) invokers.get(0)).invoke(invocation);
                }
            }
        };
        return new MockClusterInvoker<IHelloRpcService>(dic, cluster);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Invoker<IHelloRpcService> getClusterInvoker(URL url) {
        return getClusterInvokerMock(url, null);
    }

    public static interface IHelloRpcService {
        String getSomething();

        String getSomething2();

        String getSomething3();

        String getSomething4();

        int getInt1();

        boolean getBoolean1();

        Boolean getBoolean2();

        public List<String> getListString();

        public List<User> getUsers();

        void sayHello();
    }

    public static class HelloRpcService implements IHelloRpcService {
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

        public void sayHello() {
            System.out.println("hello prety");
        }
    }

    public static class IHelloRpcServiceMock implements IHelloRpcService {
        public IHelloRpcServiceMock() {

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
