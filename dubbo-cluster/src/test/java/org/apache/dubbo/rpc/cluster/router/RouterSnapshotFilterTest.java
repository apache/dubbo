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
package org.apache.dubbo.rpc.cluster.router;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RouterSnapshotFilterTest {

    @Test
    public void test() {
        FrameworkModel frameworkModel = new FrameworkModel();
        RouterSnapshotSwitcher routerSnapshotSwitcher = frameworkModel.getBeanFactory().getBean(RouterSnapshotSwitcher.class);
        RouterSnapshotFilter routerSnapshotFilter = new RouterSnapshotFilter(frameworkModel);

        Invoker invoker = Mockito.mock(Invoker.class);
        Invocation invocation = Mockito.mock(Invocation.class);
        ServiceModel serviceModel = Mockito.mock(ServiceModel.class);
        Mockito.when(serviceModel.getServiceKey()).thenReturn("TestKey");
        Mockito.when(invocation.getServiceModel()).thenReturn(serviceModel);

        routerSnapshotFilter.invoke(invoker, invocation);
        Mockito.verify(invoker, Mockito.times(1)).invoke(invocation);
        Assertions.assertFalse(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());

        routerSnapshotSwitcher.addEnabledService("Test");
        routerSnapshotFilter.invoke(invoker, invocation);
        Assertions.assertFalse(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());

        routerSnapshotSwitcher.removeEnabledService("Test");
        routerSnapshotFilter.invoke(invoker, invocation);
        Assertions.assertFalse(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());

        routerSnapshotSwitcher.addEnabledService("TestKey");
        routerSnapshotFilter.invoke(invoker, invocation);
        Assertions.assertTrue(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());
        routerSnapshotFilter.onResponse(null, null, null);
        Assertions.assertFalse(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());

        routerSnapshotSwitcher.addEnabledService("TestKey");
        routerSnapshotFilter.invoke(invoker, invocation);
        Assertions.assertTrue(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());
        routerSnapshotFilter.onError(null, null, null);
        Assertions.assertFalse(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());

        routerSnapshotSwitcher.removeEnabledService("TestKey");
        routerSnapshotFilter.invoke(invoker, invocation);
        Assertions.assertFalse(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());
        routerSnapshotFilter.onError(null, null, null);
        Assertions.assertFalse(RpcContext.getServiceContext().isNeedPrintRouterSnapshot());
    }
}
