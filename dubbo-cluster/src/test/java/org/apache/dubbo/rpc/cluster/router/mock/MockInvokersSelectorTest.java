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
package org.apache.dubbo.rpc.cluster.router.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.INVOCATION_NEED_MOCK;

class MockInvokersSelectorTest {
    @Test
    void test() {

        MockInvokersSelector selector = new MockInvokersSelector(URL.valueOf(""));

        // Data preparation
        Invoker<DemoService> invoker1 = Mockito.mock(Invoker.class);
        Invoker<DemoService> invoker2 = Mockito.mock(Invoker.class);
        Invoker<DemoService> invoker3 = Mockito.mock(Invoker.class);
        Mockito.when(invoker1.getUrl()).thenReturn(URL.valueOf("mock://127.0.0.1/test"));
        Mockito.when(invoker2.getUrl()).thenReturn(URL.valueOf("mock://127.0.0.1/test"));
        Mockito.when(invoker3.getUrl()).thenReturn(URL.valueOf("dubbo://127.0.0.1/test"));
        BitList<Invoker<DemoService>> providers = new BitList<>(Arrays.asList(invoker1, invoker2, invoker3));

        RpcInvocation rpcInvocation = Mockito.mock(RpcInvocation.class);

        URL consumerURL = URL.valueOf("test://127.0.0.1");

        selector.notify(providers);
        // rpcInvocation does not have an attached "invocation.need.mock" parameter, so normal invokers will be filtered out
        List<Invoker<DemoService>> invokers = selector.route(providers.clone(), consumerURL, rpcInvocation, false, new Holder<>());
        Assertions.assertEquals(invokers.size(),1);
        Assertions.assertTrue(invokers.contains(invoker3));

        // rpcInvocation have an attached "invocation.need.mock" parameter, so it will filter out the invoker whose protocol is mock
        Mockito.when(rpcInvocation.getObjectAttachmentWithoutConvert(INVOCATION_NEED_MOCK)).thenReturn("true");
        invokers = selector.route(providers.clone(), consumerURL, rpcInvocation, false, new Holder<>());
        Assertions.assertEquals(invokers.size(),2);
        Assertions.assertTrue(invokers.contains(invoker1));
        Assertions.assertTrue(invokers.contains(invoker2));

    }

    class DemoService{

    }
}
