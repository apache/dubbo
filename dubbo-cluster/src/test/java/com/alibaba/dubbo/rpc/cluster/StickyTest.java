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
package com.alibaba.dubbo.rpc.cluster;


import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class StickyTest {

    List<Invoker<StickyTest>> invokers = new ArrayList<Invoker<StickyTest>>();


    Invoker<StickyTest> invoker1 = EasyMock.createMock(Invoker.class);
    Invoker<StickyTest> invoker2 = EasyMock.createMock(Invoker.class);
    RpcInvocation invocation;
    Directory<StickyTest> dic;
    Result result = new RpcResult();
    StickyClusterInvoker<StickyTest> clusterinvoker = null;
    URL url = URL.valueOf("test://test:11/test?"
                    + "&loadbalance=roundrobin"
//            +"&"+Constants.CLUSTER_AVAILABLE_CHECK_KEY+"=true"
                    + "&" + Constants.CLUSTER_STICKY_KEY + "=true"
    );
    int runs = 1;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        dic = EasyMock.createMock(Directory.class);
        invocation = new RpcInvocation();

        EasyMock.expect(dic.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(dic.list(invocation)).andReturn(invokers).anyTimes();
        EasyMock.expect(dic.getInterface()).andReturn(StickyTest.class).anyTimes();
        EasyMock.replay(dic);
        invokers.add(invoker1);
        invokers.add(invoker2);

        clusterinvoker = new StickyClusterInvoker<StickyTest>(dic);
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
        EasyMock.reset(invoker1);
        EasyMock.expect(invoker1.invoke(invocation)).andReturn(result).anyTimes();
        EasyMock.expect(invoker1.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(StickyTest.class).anyTimes();
        EasyMock.replay(invoker1);

        EasyMock.reset(invoker2);
        EasyMock.expect(invoker2.invoke(invocation)).andReturn(result).anyTimes();
        EasyMock.expect(invoker2.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(StickyTest.class).anyTimes();
        EasyMock.replay(invoker2);

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