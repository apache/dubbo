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

import org.apache.dubbo.metrics.event.GlobalMetricsEventMulticaster;
import org.apache.dubbo.rpc.Exporter;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.SCOPE_KEY;

public class ScopeClusterInvokerTest {

    private final List<Invoker<DemoService>> invokers = new ArrayList<>();

    private final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private final ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private final List<Exporter<?>> exporters = new ArrayList<>();

    @BeforeEach
    void beforeMonth() {
        ApplicationModel.defaultModel().getBeanFactory().registerBean(GlobalMetricsEventMulticaster.class);
    }

    @AfterEach
    void after() throws Exception {
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        exporters.clear();
        for (Invoker<DemoService> invoker : invokers) {
            invoker.destroy();
        }
        invokers.clear();
    }

    @Test
    void testScopeNull_RemoteInvoke() {
        URL url = URL.valueOf("remote://1.2.3.4/" + DemoService.class.getName());
        url = url.addParameter(REFER_KEY,
            URL.encode(PATH_KEY + "=" + DemoService.class.getName()));
        url = url.setScopeModel(ApplicationModel.defaultModel().getDefaultModule());

        Invoker<DemoService> cluster = getClusterInvoker(url);
        invokers.add(cluster);

        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("doSomething1");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("doSomething1", ret.getValue());
    }

    @Test
    void testScopeNull_LocalInvoke() {
        URL url = URL.valueOf("remote://1.2.3.4/" + DemoService.class.getName());
        url = url.addParameter(REFER_KEY,
            URL.encode(PATH_KEY + "=" + DemoService.class.getName()));
        url = url.setScopeModel(ApplicationModel.defaultModel().getDefaultModule());

        URL injvmUrl = URL.valueOf("injvm://127.0.0.1/TestService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName());
        Exporter<?> exporter = protocol.export(proxy.getInvoker(new DemoServiceImpl(), DemoService.class, injvmUrl));
        exporters.add(exporter);


        Invoker<DemoService> cluster = getClusterInvoker(url);
        invokers.add(cluster);

        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("doSomething2");
        invocation.setParameterTypes(new Class[]{});
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("doSomething2", ret.getValue());
    }

    @Test
    void testScopeRemoteInvoke() {
        URL url = URL.valueOf("remote://1.2.3.4/" + DemoService.class.getName());
        url = url.addParameter(REFER_KEY,
            URL.encode(PATH_KEY + "=" + DemoService.class.getName()));
        url = url.addParameter(SCOPE_KEY, "remote");
        url = url.setScopeModel(ApplicationModel.defaultModel().getDefaultModule());

        URL injvmUrl = URL.valueOf("injvm://127.0.0.1/TestService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName());
        Exporter<?> exporter = protocol.export(proxy.getInvoker(new DemoServiceImpl(), DemoService.class, injvmUrl));
        exporters.add(exporter);

        Invoker<DemoService> cluster = getClusterInvoker(url);
        invokers.add(cluster);

        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("doSomething3");
        invocation.setParameterTypes(new Class[]{});
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("doSomething3", ret.getValue());
    }

    @Test
    void testScopeLocalInvoke() {
        URL url = URL.valueOf("remote://1.2.3.4/" + DemoService.class.getName());
        url = url.addParameter(REFER_KEY,
            URL.encode(PATH_KEY + "=" + DemoService.class.getName()));
        url = url.addParameter(SCOPE_KEY, "local");
        url = url.setScopeModel(ApplicationModel.defaultModel().getDefaultModule());

        URL injvmUrl = URL.valueOf("injvm://127.0.0.1/TestService")
            .addParameter(INTERFACE_KEY, DemoService.class.getName());
        Exporter<?> exporter = protocol.export(proxy.getInvoker(new DemoServiceImpl(), DemoService.class, injvmUrl));
        exporters.add(exporter);

        Invoker<DemoService> cluster = getClusterInvoker(url);

        //Configured with mock
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("doSomething4");
        invocation.setParameterTypes(new Class[]{});
        Assertions.assertTrue(cluster.isAvailable(),"");
        Result ret = cluster.invoke(invocation);
        Assertions.assertEquals("doSomething4", ret.getValue());
    }

    private Invoker<DemoService> getClusterInvoker(URL url) {
        final URL durl = url.addParameter("proxy", "jdk");
        invokers.clear();
        ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension("jdk");
        Invoker<DemoService> invoker1 = proxy.getInvoker(new DemoServiceImpl(), DemoService.class, durl);
        invokers.add(invoker1);

        StaticDirectory<DemoService> dic = new StaticDirectory<>(durl, invokers, null);
        dic.buildRouterChain();
        AbstractClusterInvoker<DemoService> cluster = new AbstractClusterInvoker(dic) {
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
        return new ScopeClusterInvoker<>(dic, cluster);
    }

    public static interface DemoService {
        String doSomething1();

        String doSomething2();

        String doSomething3();

        String doSomething4();
    }

    public static class DemoServiceImpl implements DemoService {

        @Override
        public String doSomething1() {
            return "doSomething1";
        }

        @Override
        public String doSomething2() {
            return "doSomething2";

        }

        @Override
        public String doSomething3() {
            return "doSomething3";
        }

        @Override
        public String doSomething4() {
            return "doSomething4";
        }
    }
}


