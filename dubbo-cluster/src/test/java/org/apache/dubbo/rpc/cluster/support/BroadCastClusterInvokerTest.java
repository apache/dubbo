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
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.filter.DemoService;

import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @see BroadcastClusterInvoker
 */
public class BroadCastClusterInvokerTest {
    private URL url;
    private Directory<DemoService> dic;
    private Directory<DemoService> dicIncludeInvokers;
    private List<Invoker<DemoService>> invokers;
    private RpcInvocation invocation;
    private BroadcastClusterInvoker clusterInvoker;

    private MockInvoker invoker1;
    private MockInvoker invoker2;
    private MockInvoker invoker3;
    private MockInvoker invoker4;


    @BeforeEach
    public void setUp() throws Exception {


        invoker1 = new MockInvoker();
        invoker2 = new MockInvoker();
        invoker3 = new MockInvoker();
        invoker4 = new MockInvoker();
        invokers = new ArrayList<>();

        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        invokers.add(invoker4);

        url = URL.valueOf("test://127.0.0.1:8080/test");
        dic = mock(Directory.class);

        dicIncludeInvokers = new StaticDirectory<DemoService>(invokers);
        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.getInterface()).willReturn(DemoService.class);

        invocation = new RpcInvocation();
        invocation.setMethodName("test");

        clusterInvoker = new BroadcastClusterInvoker(dic);
    }


    @Test
    public void testNormal() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        // Every invoker will be called
        clusterInvoker.invoke(invocation);
        assertTrue(invoker1.isInvoked());
        assertTrue(invoker2.isInvoked());
        assertTrue(invoker3.isInvoked());
        assertTrue(invoker4.isInvoked());
    }

    @Test
    public void testEx() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        invoker1.invokeThrowEx();
        assertThrows(RpcException.class, () -> {
            clusterInvoker.invoke(invocation);
        });
        // The default failure percentage is 100, even if a certain invoker#invoke throws an exception, other invokers will still be called
        assertTrue(invoker1.isInvoked());
        assertTrue(invoker2.isInvoked());
        assertTrue(invoker3.isInvoked());
        assertTrue(invoker4.isInvoked());
    }

    @Test
    public void testFailPercent() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        // We set the failure percentage to 75, which means that when the number of call failures is 4*(75/100) = 3,
        // an exception will be thrown directly and subsequent invokers will not be called.
        url = url.addParameter("broadcast.fail.percent", 75);
        given(dic.getConsumerUrl()).willReturn(url);
        invoker1.invokeThrowEx();
        invoker2.invokeThrowEx();
        invoker3.invokeThrowEx();
        invoker4.invokeThrowEx();
        assertThrows(RpcException.class, () -> {
            clusterInvoker.invoke(invocation);
        });
        assertTrue(invoker1.isInvoked());
        assertTrue(invoker2.isInvoked());
        assertTrue(invoker3.isInvoked());
        assertFalse(invoker4.isInvoked());
    }

    @Test
    public void testMockedInvokerSelect() {
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));

        List<Invoker<DemoService>> invokers = dic.list(invocation);
        Assertions.assertEquals(4, invokers.size());
    }


    @Test
    public void testFailoverInvokerSelect(){
        given(dic.list(invocation)).willReturn(Arrays.asList(invoker1, invoker2, invoker3, invoker4));
        //Get all invokers of the current call chain, and judge whether the call is successful one by one

        invokers =  dicIncludeInvokers.getAllInvokers();

        BroadcastClusterInvoker broadcastCluster = new BroadcastClusterInvoker(dicIncludeInvokers);
        Assertions.assertDoesNotThrow( () -> {
                broadcastCluster.doInvoke(invocation, invokers, new MockLoadBalance());
            });

        try{
            broadcastCluster.invoke(new RpcInvocation("sayhello",DemoService.class.getName(),"",
                new Class<?>[0], new Object[0]));
            Thread.sleep(120*1000);
        }catch (RpcException e ){
            Assertions.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}


class MockLoadBalance implements LoadBalance {
    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        return null;
    }
}


//Set a registry address to facilitate the consumer to perform remote call testing locally
class MockRegistryInvoker implements Invoker<DemoService> {
    private static int count = 0;
    private URL url = URL.valueOf("registry://localhost:9090/org.apache.dubbo.rpc.cluster.filter.DemoService?refer=" + URL.encode("application=BroadcastClusterInvokerTest"));
    private boolean throwEx = false;
    private boolean invoked = false;


    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {

    }

    @Override
    public Class<DemoService> getInterface() {
        return DemoService.class;
    }

    public void invokeThrowEx() {
        throwEx = true;
    }

    public boolean isInvoked() {
        return invoked;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        invoked = true;
        if (throwEx) {
            throwEx = false;
            throw new RpcException();
        }
        return null;
    }
}

class MockInvoker implements Invoker<DemoService> {
    private static int count = 0;
    private URL url = URL.valueOf("test://127.0.0.1:8080/test");
    private boolean throwEx = false;
    private boolean invoked = false;

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void destroy() {

    }

    @Override
    public Class<DemoService> getInterface() {
        return DemoService.class;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        invoked = true;
        if (throwEx) {
            throwEx = false;
            throw new RpcException();
        }
        return null;
    }

    public void invokeThrowEx() {
        throwEx = true;
    }

    public boolean isInvoked() {
        return invoked;
    }
}
