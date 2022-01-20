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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.Mockito.when;

@SuppressWarnings("all")
public class ConnectivityValidationTest {
    private Invoker invoker1;
    private Invoker invoker2;
    private Invoker invoker3;
    private Invoker invoker4;
    private Invoker invoker5;
    private Invoker invoker6;
    private Invoker invoker7;
    private Invoker invoker8;
    private Invoker invoker9;
    private Invoker invoker10;
    private Invoker invoker11;
    private Invoker invoker12;
    private Invoker invoker13;
    private Invoker invoker14;
    private Invoker invoker15;

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
        invoker6 = Mockito.mock(Invoker.class);
        invoker7 = Mockito.mock(Invoker.class);
        invoker8 = Mockito.mock(Invoker.class);
        invoker9 = Mockito.mock(Invoker.class);
        invoker10 = Mockito.mock(Invoker.class);
        invoker11 = Mockito.mock(Invoker.class);
        invoker12 = Mockito.mock(Invoker.class);
        invoker13 = Mockito.mock(Invoker.class);
        invoker14 = Mockito.mock(Invoker.class);
        invoker15 = Mockito.mock(Invoker.class);

        configInvoker(invoker1);
        configInvoker(invoker2);
        configInvoker(invoker3);
        configInvoker(invoker4);
        configInvoker(invoker5);
        configInvoker(invoker6);
        configInvoker(invoker7);
        configInvoker(invoker8);
        configInvoker(invoker9);
        configInvoker(invoker10);
        configInvoker(invoker11);
        configInvoker(invoker12);
        configInvoker(invoker13);
        configInvoker(invoker14);
        configInvoker(invoker15);

        invokerList = new LinkedList<>();
        invokerList.add(invoker1);
        invokerList.add(invoker2);
        invokerList.add(invoker3);
        invokerList.add(invoker4);
        invokerList.add(invoker5);

        directory = new StaticDirectory(invokerList);
        clusterInvoker = new ConnectivityClusterInvoker(directory);
    }

    @AfterEach
    public void tearDown() {
        clusterInvoker.destroy();
    }

    private void configInvoker(Invoker invoker) {
        when(invoker.getUrl()).thenReturn(URL.valueOf(""));
        when(invoker.isAvailable()).thenReturn(true);
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

        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList()));

        when(invoker1.isAvailable()).thenReturn(false);
        when(invoker2.isAvailable()).thenReturn(false);
        when(invoker3.isAvailable()).thenReturn(false);
        when(invoker4.isAvailable()).thenReturn(false);
        when(invoker5.isAvailable()).thenReturn(false);

        clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList());
        Assertions.assertEquals(0, directory.list(invocation).size());

        when(invoker1.isAvailable()).thenReturn(true);
        Set<Invoker> invokerSet = new HashSet<>();
        invokerSet.add(invoker1);
        waitRefresh(invokerSet);
        Assertions.assertEquals(1, directory.list(invocation).size());
        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList()));

        when(invoker2.isAvailable()).thenReturn(true);
        invokerSet.add(invoker2);
        waitRefresh(invokerSet);
        Assertions.assertEquals(2, directory.list(invocation).size());
        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList()));

        invokerList.remove(invoker5);
        directory.notify(invokerList);
        when(invoker2.isAvailable()).thenReturn(true);
        waitRefresh(invokerSet);
        Assertions.assertEquals(2, directory.list(invocation).size());
        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList()));

        when(invoker3.isAvailable()).thenReturn(true);
        when(invoker4.isAvailable()).thenReturn(true);
        invokerSet.add(invoker3);
        invokerSet.add(invoker4);
        waitRefresh(invokerSet);
        Assertions.assertEquals(4, directory.list(invocation).size());
        Assertions.assertNotNull(clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList()));
    }

    @Test
    public void testRetry() throws InterruptedException {
        Invocation invocation = new RpcInvocation();
        LoadBalance loadBalance = new RandomLoadBalance();

        invokerList.clear();
        invokerList.add(invoker1);
        invokerList.add(invoker2);
        directory.notify(invokerList);

        Assertions.assertEquals(2, directory.list(invocation).size());

        when(invoker1.isAvailable()).thenReturn(false);
        Assertions.assertEquals(invoker2, clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.singletonList(invoker2)));
        Assertions.assertEquals(1, directory.list(invocation).size());

        when(invoker1.isAvailable()).thenReturn(true);
        Set<Invoker> invokerSet = new HashSet<>();
        invokerSet.add(invoker1);
        waitRefresh(invokerSet);
        Assertions.assertEquals(2, directory.list(invocation).size());
    }

    @Test
    public void testRandomSelect() throws InterruptedException {
        Invocation invocation = new RpcInvocation();
        LoadBalance loadBalance = new RandomLoadBalance();

        invokerList.add(invoker6);
        invokerList.add(invoker7);
        invokerList.add(invoker8);
        invokerList.add(invoker9);
        invokerList.add(invoker10);
        invokerList.add(invoker11);
        invokerList.add(invoker12);
        invokerList.add(invoker13);
        invokerList.add(invoker14);
        invokerList.add(invoker15);

        directory.notify(invokerList);

        Assertions.assertEquals(15, directory.list(invocation).size());

        when(invoker2.isAvailable()).thenReturn(false);
        when(invoker3.isAvailable()).thenReturn(false);
        when(invoker4.isAvailable()).thenReturn(false);
        when(invoker5.isAvailable()).thenReturn(false);
        when(invoker6.isAvailable()).thenReturn(false);
        when(invoker7.isAvailable()).thenReturn(false);
        when(invoker8.isAvailable()).thenReturn(false);
        when(invoker9.isAvailable()).thenReturn(false);
        when(invoker10.isAvailable()).thenReturn(false);
        when(invoker11.isAvailable()).thenReturn(false);
        when(invoker12.isAvailable()).thenReturn(false);
        when(invoker13.isAvailable()).thenReturn(false);
        when(invoker14.isAvailable()).thenReturn(false);
        when(invoker15.isAvailable()).thenReturn(false);
        for (int i = 0; i < 15; i++) {
            clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList());
        }
        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(invoker1, clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList()));
        }

        when(invoker1.isAvailable()).thenReturn(false);
        clusterInvoker.select(loadBalance, invocation, directory.list(invocation), Collections.emptyList());
        Assertions.assertEquals(0, directory.list(invocation).size());

        when(invoker1.isAvailable()).thenReturn(true);
        when(invoker2.isAvailable()).thenReturn(true);
        when(invoker3.isAvailable()).thenReturn(true);
        when(invoker4.isAvailable()).thenReturn(true);
        when(invoker5.isAvailable()).thenReturn(true);
        when(invoker6.isAvailable()).thenReturn(true);
        when(invoker7.isAvailable()).thenReturn(true);
        when(invoker8.isAvailable()).thenReturn(true);
        when(invoker9.isAvailable()).thenReturn(true);
        when(invoker10.isAvailable()).thenReturn(true);
        when(invoker11.isAvailable()).thenReturn(true);
        when(invoker12.isAvailable()).thenReturn(true);
        when(invoker13.isAvailable()).thenReturn(true);
        when(invoker14.isAvailable()).thenReturn(true);
        when(invoker15.isAvailable()).thenReturn(true);

        Set<Invoker> invokerSet = new HashSet<>();
        invokerSet.add(invoker1);
        invokerSet.add(invoker2);
        invokerSet.add(invoker3);
        invokerSet.add(invoker4);
        invokerSet.add(invoker5);
        invokerSet.add(invoker6);
        invokerSet.add(invoker7);
        invokerSet.add(invoker8);
        invokerSet.add(invoker9);
        invokerSet.add(invoker10);
        invokerSet.add(invoker11);
        invokerSet.add(invoker12);
        invokerSet.add(invoker13);
        invokerSet.add(invoker14);
        invokerSet.add(invoker15);
        waitRefresh(invokerSet);
        Assertions.assertTrue(directory.list(invocation).size() > 1);
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

    private void waitRefresh(Set<Invoker> invokerSet) throws InterruptedException {
        directory.checkConnectivity();
        while (true) {
            List<Invoker> reconnectList = directory.getInvokersToReconnect();
            if (reconnectList.stream().anyMatch(invoker -> invokerSet.contains(invoker))) {
                Thread.sleep(10);
                continue;
            }
            break;
        }
    }

    private static class RandomLoadBalance implements LoadBalance {
        @Override
        public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
            return CollectionUtils.isNotEmpty(invokers) ? invokers.get(ThreadLocalRandom.current().nextInt(invokers.size())) : null;
        }
    }
}
