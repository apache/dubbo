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

package org.apache.dubbo.rpc.cluster.router.address;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.router.MockInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


public class AddressRouterTest {


    @Test
    public void testAddressRouteSelector() {
        Router router = new AddressRouterFactory().getRouter(URL.valueOf("url"));
        List<Invoker<String>> invokers = new ArrayList<Invoker<String>>();
        invokers.add(new MockInvoker<>(new URL("dubbo", "129.34.56.7", 8809), true));
        invokers.add(new MockInvoker<>(new URL("dubbo", "129.34.56.8", 8809), true));
        invokers.add(new MockInvoker<>(new URL("dubbo", "129.34.56.9", 8809), true));
        Invocation invocation = new RpcInvocation();
        Address address = new Address("129.34.56.9", 8809);
        invocation.setObjectAttachment("address", address);
        List<Invoker<String>> list = router.route(invokers, URL.valueOf("url"), invocation);
        Assertions.assertEquals(address.getIp(), list.get(0).getUrl().getHost());
    }
}
