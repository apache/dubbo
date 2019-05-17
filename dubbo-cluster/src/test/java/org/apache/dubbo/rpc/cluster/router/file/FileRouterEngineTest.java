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
package org.apache.dubbo.rpc.cluster.router.file;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.RouterFactory;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.RUNTIME_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class FileRouterEngineTest {
    private static boolean isScriptUnsupported = new ScriptEngineManager().getEngineByName("javascript") == null;
    List<Invoker<FileRouterEngineTest>> invokers = new ArrayList<Invoker<FileRouterEngineTest>>();
    Invoker<FileRouterEngineTest> invoker1 = mock(Invoker.class);
    Invoker<FileRouterEngineTest> invoker2 = mock(Invoker.class);
    Invocation invocation;
    StaticDirectory<FileRouterEngineTest> dic;
    Result result = new RpcResult();
    private RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getAdaptiveExtension();

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @BeforeEach
    public void setUp() throws Exception {
        invokers.add(invoker1);
        invokers.add(invoker2);
    }

    @Test
    public void testRouteNotAvailable() {
        if (isScriptUnsupported) return;
        URL url = initUrl("notAvailablerule.javascript");
        initInvocation("method1");
        initInvokers(url, true, false);
        initDic(url);

        MockClusterInvoker<FileRouterEngineTest> sinvoker = new MockClusterInvoker<FileRouterEngineTest>(
                dic, url);
        for (int i = 0; i < 100; i++) {
            sinvoker.invoke(invocation);
            Invoker<FileRouterEngineTest> invoker = sinvoker.getSelectedInvoker();
            Assertions.assertEquals(invoker2, invoker);
        }
    }

    @Test
    public void testRouteAvailable() {
        if (isScriptUnsupported) return;
        URL url = initUrl("availablerule.javascript");
        initInvocation("method1");
        initInvokers(url);
        initDic(url);

        MockClusterInvoker<FileRouterEngineTest> sinvoker = new MockClusterInvoker<FileRouterEngineTest>(
                dic, url);
        for (int i = 0; i < 100; i++) {
            sinvoker.invoke(invocation);
            Invoker<FileRouterEngineTest> invoker = sinvoker.getSelectedInvoker();
            Assertions.assertEquals(invoker1, invoker);
        }
    }

    @Test
    public void testRouteByMethodName() {
        if (isScriptUnsupported) return;
        URL url = initUrl("methodrule.javascript");
        {
            initInvocation("method1");
            initInvokers(url, true, true);
            initDic(url);

            MockClusterInvoker<FileRouterEngineTest> sinvoker = new MockClusterInvoker<FileRouterEngineTest>(
                    dic, url);
            for (int i = 0; i < 100; i++) {
                sinvoker.invoke(invocation);
                Invoker<FileRouterEngineTest> invoker = sinvoker.getSelectedInvoker();
                Assertions.assertEquals(invoker1, invoker);
            }
        }
        {
            initInvocation("method2");
            initInvokers(url, true, true);
            initDic(url);
            MockClusterInvoker<FileRouterEngineTest> sinvoker = new MockClusterInvoker<FileRouterEngineTest>(
                    dic, url);
            for (int i = 0; i < 100; i++) {
                sinvoker.invoke(invocation);
                Invoker<FileRouterEngineTest> invoker = sinvoker.getSelectedInvoker();
                Assertions.assertEquals(invoker2, invoker);
            }
        }
    }

    private URL initUrl(String filename) {
        filename = getClass().getClassLoader().getResource(getClass().getPackage().getName().replace('.', '/') + "/" + filename).toString();
        URL url = URL.valueOf(filename);
        url = url.addParameter(RUNTIME_KEY, true);
        return url;
    }

    private void initInvocation(String methodName) {
        invocation = new RpcInvocation();
        ((RpcInvocation) invocation).setMethodName(methodName);
    }

    private void initInvokers(URL url) {
        initInvokers(url, true, false);
    }

    private void initInvokers(URL url, boolean invoker1Status, boolean invoker2Status) {
        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.isAvailable()).willReturn(invoker1Status);
        given(invoker1.getUrl()).willReturn(url);
        given(invoker1.getInterface()).willReturn(FileRouterEngineTest.class);

        given(invoker2.invoke(invocation)).willReturn(result);
        given(invoker2.isAvailable()).willReturn(invoker2Status);
        given(invoker2.getUrl()).willReturn(url);
        given(invoker2.getInterface()).willReturn(FileRouterEngineTest.class);
    }

    private void initDic(URL url) {
        // FIXME: this exposes the design flaw in RouterChain
        URL dicInitUrl = URL.valueOf("consumer://localhost:20880/org.apache.dubbo.rpc.cluster.router.file.FileRouterEngineTest?application=FileRouterEngineTest");
        dic = new StaticDirectory<>(dicInitUrl, invokers);
        dic.buildRouterChain();
        dic.getRouterChain().initWithRouters(Arrays.asList(routerFactory.getRouter(url)));
    }

    static class MockClusterInvoker<T> extends AbstractClusterInvoker<T> {
        private Invoker<T> selectedInvoker;

        public MockClusterInvoker(Directory<T> directory) {
            super(directory);
        }

        public MockClusterInvoker(Directory<T> directory, URL url) {
            super(directory, url);
        }

        @Override
        protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers,
                                  LoadBalance loadbalance) throws RpcException {
            Invoker<T> invoker = select(loadbalance, invocation, invokers, null);
            selectedInvoker = invoker;
            return null;
        }

        public Invoker<T> getSelectedInvoker() {
            return selectedInvoker;
        }
    }
}
