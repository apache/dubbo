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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.Mockito.when;

@SuppressWarnings("all")
public class ConnectivityValidationTest {
    private Invoker invoker1;
    private Invoker invoker2;
    private Invoker invoker3;
    private Invoker invoker4;
    private Invoker invoker5;

    private List<Invoker> invokerList;

    private StaticDirectory directory;
    private ConnectivityClusterInvoker clusterInvoker;

    @BeforeEach
    public void setup() {
        invoker1 = Mockito.mock(Invoker.class);
        invoker2 = Mockito.mock(Invoker.class);
        invoker3 = Mockito.mock(Invoker.class);
        invoker4 = Mockito.mock(Invoker.class);
        invoker5 = Mockito.mock(Invoker.class);

        when(invoker1.isAvailable()).thenReturn(true);
        when(invoker2.isAvailable()).thenReturn(true);
        when(invoker3.isAvailable()).thenReturn(true);
        when(invoker4.isAvailable()).thenReturn(true);
        when(invoker5.isAvailable()).thenReturn(true);

        when(invoker1.getUrl()).thenReturn(URL.valueOf(""));
        when(invoker2.getUrl()).thenReturn(URL.valueOf(""));
        when(invoker3.getUrl()).thenReturn(URL.valueOf(""));
        when(invoker4.getUrl()).thenReturn(URL.valueOf(""));
        when(invoker5.getUrl()).thenReturn(URL.valueOf(""));

        invokerList = new LinkedList<>();
        invokerList.add(invoker1);
        invokerList.add(invoker2);
        invokerList.add(invoker3);
        invokerList.add(invoker4);
        invokerList.add(invoker5);

        directory = new StaticDirectory(invokerList);
        clusterInvoker = new ConnectivityClusterInvoker(directory);
    }

    @BeforeAll
    public static void setupClass() {
        System.setProperty(CommonConstants.RECONNECT_TASK_PERIOD, "1");
    }

    @AfterAll
    public static void clearAfterClass() {
        System.clearProperty(CommonConstants.RECONNECT_TASK_PERIOD);
    }

    @Test
    public void testBasic() throws InterruptedException {
        Invocation invocation = new RpcInvocation();
        LoadBalance loadBalance = new RandomLoadBalance();

        Assertions.assertEquals(5, directory.list(invocation).size());

        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, invokerList, Collections.emptyList()));

        when(invoker1.isAvailable()).thenReturn(false);
        when(invoker2.isAvailable()).thenReturn(false);
        when(invoker3.isAvailable()).thenReturn(false);
        when(invoker4.isAvailable()).thenReturn(false);
        when(invoker5.isAvailable()).thenReturn(false);

        clusterInvoker.select(loadBalance, invocation, invokerList, Collections.emptyList());
        Assertions.assertEquals(0, directory.list(invocation).size());

        when(invoker1.isAvailable()).thenReturn(true);
        Thread.sleep(10);
        Assertions.assertEquals(1, directory.list(invocation).size());
        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, invokerList, Collections.emptyList()));

        when(invoker2.isAvailable()).thenReturn(true);
        Thread.sleep(10);
        Assertions.assertEquals(2, directory.list(invocation).size());
        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, invokerList, Collections.emptyList()));

        invokerList.remove(invoker5);
        directory.notify(invokerList);
        when(invoker2.isAvailable()).thenReturn(true);
        Thread.sleep(10);
        Assertions.assertEquals(2, directory.list(invocation).size());
        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, invokerList, Collections.emptyList()));

        when(invoker3.isAvailable()).thenReturn(true);
        when(invoker4.isAvailable()).thenReturn(true);

        Thread.sleep(10);
        Assertions.assertEquals(4, directory.list(invocation).size());
        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, invokerList, Collections.emptyList()));
    }

    private static class ConnectivityClusterInvoker<T> extends AbstractClusterInvoker<T> {
        public ConnectivityClusterInvoker(Directory<T> directory) {
            super(directory);
        }

        @Override
        public Invoker<T> select(LoadBalance loadbalance, Invocation invocation, List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {
            return super.select(loadbalance, invocation, invokers, selected);
        }

        @Override
        protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
            return null;
        }
    }

    private static class RandomLoadBalance implements LoadBalance {
        @Override
        public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
            return CollectionUtils.isNotEmpty(invokers) ? invokers.get(ThreadLocalRandom.current().nextInt(invokers.size())) : null;
        }
    }
}
