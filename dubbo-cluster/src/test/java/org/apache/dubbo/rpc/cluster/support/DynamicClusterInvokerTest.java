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
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.support.wrapper.AbstractCluster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SuppressWarnings("all")
public class DynamicClusterInvokerTest {

    List<Invoker<DynamicClusterInvokerTest>> invokers = new ArrayList<>();
    URL url = URL.valueOf("test://test:11/test?default.cluster=failover&method1.cluster=failfast");
    Invoker<DynamicClusterInvokerTest> invoker = mock(Invoker.class);
    RpcInvocation invocation = new RpcInvocation();
    Directory<DynamicClusterInvokerTest> dic;

    @BeforeEach
    public void setUp() throws Exception {
        dic = mock(Directory.class);
        given(dic.getUrl()).willReturn(url);
        given(dic.getConsumerUrl()).willReturn(url);
        given(dic.list(invocation)).willReturn(invokers);
        given(dic.getInterface()).willReturn(DynamicClusterInvokerTest.class);

        given(invoker.getUrl()).willReturn(url);
        invokers.add(invoker);
    }

    @AfterEach
    public void tearDown() {

        dic = null;
        invocation = new RpcInvocation();
        invokers.clear();
    }


    @Test
    void testDynamic() {
        List<AbstractCluster> tempAbstractCluster = new ArrayList<>();

        DynamicCluster dynamicCluster = new DynamicCluster();
        dynamicCluster.setClusterBuilder((x) -> {
            switch (x) {
                case FailoverCluster.NAME:
                    FailoverCluster failoverCluster = new FailoverCluster();
                    tempAbstractCluster.add(failoverCluster);
                    return failoverCluster;
                case FailfastCluster.NAME:
                    FailfastCluster failfastCluster = new FailfastCluster();
                    tempAbstractCluster.add(failfastCluster);
                    return failfastCluster;
                case FailbackCluster.NAME:
                    FailbackCluster failbackCluster = new FailbackCluster();
                    tempAbstractCluster.add(failbackCluster);
                    return failbackCluster;
                default:
                    return null;
            }
        });

        AbstractClusterInvoker<DynamicClusterInvokerTest> dynamicClusterInvokerTestAbstractClusterInvoker = dynamicCluster.doJoin(dic);
        RpcContext.getServiceContext().setConsumerUrl(url);

        invocation.setMethodName("method1");
        dynamicClusterInvokerTestAbstractClusterInvoker.invoke(invocation);
        Assertions.assertEquals(tempAbstractCluster.get(0).getClass(), FailfastCluster.class);

        invocation.setMethodName("method2");
        dynamicClusterInvokerTestAbstractClusterInvoker.invoke(invocation);
        Assertions.assertEquals(tempAbstractCluster.get(1).getClass(), FailoverCluster.class);

        invocation.setAttachment(CLUSTER_KEY, FailbackCluster.NAME);
        dynamicClusterInvokerTestAbstractClusterInvoker.invoke(invocation);
        Assertions.assertEquals(tempAbstractCluster.get(2).getClass(), FailbackCluster.class);

    }

}
