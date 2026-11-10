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
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.rpc.Constants.MERGER_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class MergeableClusterInvokerSameServiceKeyTest {

    @SuppressWarnings("unchecked")
    @Test
    void mergeMustNotDiscardProvidersWithSameServiceKey() {
        URL url = URL.valueOf("test://test/" + MenuService.class.getName()).addParameter(MERGER_KEY, ".merge");
        URL firstUrl =
                url.addParameter(GROUP_KEY, "shared").setHost("127.0.0.1").setPort(20880);
        URL secondUrl =
                url.addParameter(GROUP_KEY, "shared").setHost("127.0.0.1").setPort(20881);
        assertEquals(firstUrl.getServiceKey(), secondUrl.getServiceKey());

        Menu firstMenu = new Menu();
        firstMenu.addMenu("1", Arrays.asList("10"));
        firstMenu.addMenu("2", Arrays.asList("20"));
        Menu secondMenu = new Menu();
        secondMenu.addMenu("2", Arrays.asList("21"));
        secondMenu.addMenu("3", Arrays.asList("30"));

        RpcInvocation invocation = mock(RpcInvocation.class);
        given(invocation.getMethodName()).willReturn("getMenu");
        given(invocation.getParameterTypes()).willReturn(new Class<?>[] {});
        given(invocation.getArguments()).willReturn(new Object[] {});
        given(invocation.getObjectAttachments()).willReturn(new HashMap<>());

        Invoker<MenuService> first = createInvoker(firstUrl, firstMenu, invocation);
        Invoker<MenuService> second = createInvoker(secondUrl, secondMenu, invocation);
        org.mockito.Mockito.doReturn(first).when(invocation).getInvoker();

        Directory<MenuService> directory = mock(Directory.class);
        given(directory.list(invocation)).willReturn(Arrays.asList(first, second));
        given(directory.getUrl()).willReturn(url);
        given(directory.getConsumerUrl()).willReturn(url);
        given(directory.getInterface()).willReturn(MenuService.class);

        Result result = new MergeableClusterInvoker<>(directory).invoke(invocation);
        Menu merged = (Menu) result.getValue();
        assertEquals(3, merged.getMenus().size());
        assertEquals(Arrays.asList("10"), merged.getMenus().get("1"));
        assertEquals(Arrays.asList("20", "21"), merged.getMenus().get("2"));
        assertEquals(Arrays.asList("30"), merged.getMenus().get("3"));
    }

    @SuppressWarnings("unchecked")
    private static Invoker<MenuService> createInvoker(URL url, Menu menu, Invocation invocation) {
        return (Invoker<MenuService>) Proxy.newProxyInstance(
                MergeableClusterInvokerSameServiceKeyTest.class.getClassLoader(),
                new Class<?>[] {Invoker.class},
                (proxy, method, args) -> {
                    if ("getUrl".equals(method.getName())) {
                        return url;
                    }
                    if ("getInterface".equals(method.getName())) {
                        return MenuService.class;
                    }
                    if ("invoke".equals(method.getName())) {
                        return AsyncRpcResult.newDefaultAsyncResult(menu, invocation);
                    }
                    return null;
                });
    }
}
