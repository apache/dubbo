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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AbstractLoadBalanceTest {

    private AbstractLoadBalance balance = new AbstractLoadBalance() {
        @Override
        protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
            return null;
        }
    };

    @Test
    void testGetWeight() {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("say");

        Invoker invoker1 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        URL url1 = new ServiceConfigURL("", "", 0, "DemoService", new HashMap<>());
        url1 = url1.addParameter(TIMESTAMP_KEY, System.currentTimeMillis() - Integer.MAX_VALUE - 1);
        given(invoker1.getUrl()).willReturn(url1);

        Invoker invoker2 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        URL url2 = new ServiceConfigURL("", "", 0, "DemoService", new HashMap<>());
        url2 = url2.addParameter(TIMESTAMP_KEY, System.currentTimeMillis() - 10 * 60 * 1000L - 1);
        given(invoker2.getUrl()).willReturn(url2);

        Assertions.assertEquals(balance.getWeight(invoker1, invocation), balance.getWeight(invoker2, invocation));
    }

    @Test
    void testGetRegistryWeight() {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("say");

        Invoker invoker1 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        URL url1 = new ServiceConfigURL("", "", 0, "DemoService", new HashMap<>());
        given(invoker1.getUrl()).willReturn(url1);

        ClusterInvoker invoker2 = mock(ClusterInvoker.class, Mockito.withSettings().stubOnly());
        URL url2 = new ServiceConfigURL("", "", 0, "org.apache.dubbo.registry.RegistryService", new HashMap<>());
        url2 = url2.addParameter(WEIGHT_KEY, 20);
        URL registryUrl2 = new ServiceConfigURL("", "", 0, "org.apache.dubbo.registry.RegistryService", new HashMap<>());
        registryUrl2 = registryUrl2.addParameter(WEIGHT_KEY, 30);
        given(invoker2.getUrl()).willReturn(url2);
        given(invoker2.getRegistryUrl()).willReturn(registryUrl2);

        Assertions.assertEquals(100, balance.getWeight(invoker1, invocation));
        Assertions.assertEquals(30, balance.getWeight(invoker2, invocation));
    }
}
