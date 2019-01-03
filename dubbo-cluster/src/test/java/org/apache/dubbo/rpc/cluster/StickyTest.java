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
package org.apache.dubbo.rpc.cluster;


import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class StickyTest {

    private List<Invoker<StickyTest>> invokers = new ArrayList<Invoker<StickyTest>>();


    private Invoker<StickyTest> invoker1 = mock(Invoker.class);
    private  Invoker<StickyTest> invoker2 = mock(Invoker.class);
    private RpcInvocation invocation;
    private Directory<StickyTest> dic;
    private Result result = new RpcResult();
    private StickyClusterInvoker<StickyTest> clusterinvoker = null;
    private URL url = URL.valueOf("test://test:11/test?"
                    + "&loadbalance=roundrobin"
                    + "&" + Constants.CLUSTER_STICKY_KEY + "=true"
    );
    private int runs = 1;

    @Before
    public void setUp() throws Exception {
        dic = mock(Directory.class);
        invocation = new RpcInvocation();

        given(dic.getUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(StickyTest.class);

        invokers.add(invoker1);
        invokers.add(invoker2);

        clusterinvoker = new StickyClusterInvoker<StickyTest>(dic);

        ExtensionLoader.resetExtensionLoader(LoadBalance.class);
    }

    @Test
    public void testStickyNoCheck() {
        int count = testSticky(null, false);
        System.out.println(count);
        Assert.assertTrue(count > 0 && count <= runs);
    }

    @Test
    public void testStickyForceCheck() {
        int count = testSticky(null, true);
        Assert.assertTrue(count == 0 || count == runs);
    }

    @Test
    public void testMethodStickyNoCheck() {
        int count = testSticky("method1", false);
        System.out.println(count);
        Assert.assertTrue(count > 0 && count <= runs);
    }

    @Test
    public void testMethodStickyForceCheck() {
        int count = testSticky("method1", true);
        Assert.assertTrue(count == 0 || count == runs);
    }

    @Test
    public void testMethodsSticky() {
        for (int i = 0; i < 100; i++) {//Two different methods should always use the same invoker every time.
            int count1 = testSticky("method1", true);
            int count2 = testSticky("method2", true);
            Assert.assertTrue(count1 == count2);
        }
    }

    public int testSticky(String methodName, boolean check) {
        if (methodName == null) {
            url = url.addParameter(Constants.CLUSTER_STICKY_KEY, String.valueOf(check));
        } else {
            url = url.addParameter(methodName + "." + Constants.CLUSTER_STICKY_KEY, String.valueOf(check));
        }

        given(invoker1.invoke(invocation)).willReturn(result);
        given(invoker1.isAvailable()).willReturn(true);
        given(invoker1.getUrl()).willReturn(url.setPort(1));
        given(invoker1.getInterface()).willReturn(StickyTest.class);

        given(invoker2.invoke(invocation)).willReturn(result);
        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getUrl()).willReturn(url.setPort(2));
        given(invoker2.getInterface()).willReturn(StickyTest.class);

        invocation.setMethodName(methodName);

        int count = 0;
        for (int i = 0; i < runs; i++) {
            Assert.assertEquals(null, clusterinvoker.invoke(invocation));
            if (invoker1 == clusterinvoker.getSelectedInvoker()) {
                count++;
            }
        }
        return count;
    }


    static class StickyClusterInvoker<T> extends AbstractClusterInvoker<T> {
        private Invoker<T> selectedInvoker;

        public StickyClusterInvoker(Directory<T> directory) {
            super(directory);
        }

        public StickyClusterInvoker(Directory<T> directory, URL url) {
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