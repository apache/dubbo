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
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MergeableClusterInvokerTest {

    private Directory directory = EasyMock.createMock(Directory.class);
    private Invoker firstInvoker = EasyMock.createMock(Invoker.class);
    private Invoker secondInvoker = EasyMock.createMock(Invoker.class);
    private Invocation invocation = EasyMock.createMock(Invocation.class);

    private MergeableClusterInvoker<MenuService> mergeableClusterInvoker;

    private String[] list1 = {"10", "11", "12"};
    private String[] list2 = {"20", "21", "22"};
    private String[] list3 = {"23", "24", "25"};
    private String[] list4 = {"30", "31", "32"};

    private Map<String, List<String>> firstMenuMap = new HashMap<String, List<String>>() {
        {
            put("1", Arrays.asList(list1));
            put("2", Arrays.asList(list2));
        }
    };

    private Map<String, List<String>> secondMenuMap = new HashMap<String, List<String>>() {
        {
            put("2", Arrays.asList(list3));
            put("3", Arrays.asList(list4));
        }
    };

    private Menu firstMenu = new Menu(firstMenuMap);
    private Menu secondMenu = new Menu(secondMenuMap);

    private URL url = URL.valueOf(new StringBuilder(32)
            .append("test://test/")
            .append(MenuService.class.getName()).toString());

    static void merge(Map<String, List<String>> first, Map<String, List<String>> second) {
        for (Map.Entry<String, List<String>> entry : second.entrySet()) {
            List<String> value = first.get(entry.getKey());
            if (value != null) {
                value.addAll(entry.getValue());
            } else {
                first.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
            }
        }
    }

    @Before
    public void setUp() throws Exception {

        directory = EasyMock.createMock(Directory.class);
        firstInvoker = EasyMock.createMock(Invoker.class);
        secondInvoker = EasyMock.createMock(Invoker.class);
        invocation = EasyMock.createMock(Invocation.class);

    }

    @Test
    public void testGetMenuSuccessfully() throws Exception {

        // setup
        url = url.addParameter(Constants.MERGER_KEY, ".merge");

        EasyMock.expect(invocation.getMethodName()).andReturn("getMenu").anyTimes();
        EasyMock.expect(invocation.getParameterTypes()).andReturn(new Class<?>[]{}).anyTimes();
        EasyMock.expect(invocation.getArguments()).andReturn(new Object[]{}).anyTimes();
        EasyMock.expect(invocation.getAttachments()).andReturn(new HashMap<String, String>())
                .anyTimes();
        EasyMock.expect(invocation.getInvoker()).andReturn(firstInvoker).anyTimes();
        EasyMock.replay(invocation);

        firstInvoker = (Invoker) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Invoker.class}, new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("getUrl".equals(method.getName())) {
                    return url.addParameter(Constants.GROUP_KEY, "first");
                }
                if ("getInterface".equals(method.getName())) {
                    return MenuService.class;
                }
                if ("invoke".equals(method.getName())) {
                    return new RpcResult(firstMenu);
                }
                return null;
            }
        });

        secondInvoker = (Invoker) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Invoker.class}, new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("getUrl".equals(method.getName())) {
                    return url.addParameter(Constants.GROUP_KEY, "second");
                }
                if ("getInterface".equals(method.getName())) {
                    return MenuService.class;
                }
                if ("invoke".equals(method.getName())) {
                    return new RpcResult(secondMenu);
                }
                return null;
            }
        });

        EasyMock.expect(directory.list(invocation)).andReturn(new ArrayList() {

            {
                add(firstInvoker);
                add(secondInvoker);
            }
        }).anyTimes();
        EasyMock.expect(directory.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(directory.getInterface()).andReturn(MenuService.class).anyTimes();
        EasyMock.replay(directory);

        mergeableClusterInvoker = new MergeableClusterInvoker<MenuService>(directory);

        // invoke
        Result result = mergeableClusterInvoker.invoke(invocation);
        Assert.assertTrue(result.getValue() instanceof Menu);
        Menu menu = (Menu) result.getValue();
        Map<String, List<String>> expected = new HashMap<String, List<String>>();
        merge(expected, firstMenuMap);
        merge(expected, secondMenuMap);
        TestCase.assertEquals(expected.keySet(), menu.getMenus().keySet());
        for (String key : expected.keySet()) {
            // FIXME: cannot guarantee the sequence of the merge result, check implementation in
            // MergeableClusterInvoker#invoke
            List<String> values1 = new ArrayList<String>(expected.get(key));
            List<String> values2 = new ArrayList<String>(menu.getMenus().get(key));
            Collections.sort(values1);
            Collections.sort(values2);
            TestCase.assertEquals(values1, values2);
        }
    }

    @Test
    public void testAddMenu() throws Exception {

        String menu = "first";
        List<String> menuItems = new ArrayList<String>() {
            {
                add("1");
                add("2");
            }
        };

        EasyMock.expect(invocation.getMethodName()).andReturn("addMenu").anyTimes();
        EasyMock.expect(invocation.getParameterTypes()).andReturn(
                new Class<?>[]{String.class, List.class}).anyTimes();
        EasyMock.expect(invocation.getArguments()).andReturn(new Object[]{menu, menuItems})
                .anyTimes();
        EasyMock.expect(invocation.getAttachments()).andReturn(new HashMap<String, String>())
                .anyTimes();
        EasyMock.expect(invocation.getInvoker()).andReturn(firstInvoker).anyTimes();
        EasyMock.replay(invocation);

        EasyMock.expect(firstInvoker.getUrl()).andReturn(
                url.addParameter(Constants.GROUP_KEY, "first")).anyTimes();
        EasyMock.expect(firstInvoker.getInterface()).andReturn(MenuService.class).anyTimes();
        EasyMock.expect(firstInvoker.invoke(invocation)).andReturn(new RpcResult())
                .anyTimes();
        EasyMock.expect(firstInvoker.isAvailable()).andReturn(true).anyTimes();
        EasyMock.replay(firstInvoker);

        EasyMock.expect(secondInvoker.getUrl()).andReturn(
                url.addParameter(Constants.GROUP_KEY, "second")).anyTimes();
        EasyMock.expect(secondInvoker.getInterface()).andReturn(MenuService.class).anyTimes();
        EasyMock.expect(secondInvoker.invoke(invocation)).andReturn(new RpcResult())
                .anyTimes();
        EasyMock.expect(secondInvoker.isAvailable()).andReturn(true).anyTimes();
        EasyMock.replay(secondInvoker);

        EasyMock.expect(directory.list(invocation)).andReturn(new ArrayList() {

            {
                add(firstInvoker);
                add(secondInvoker);
            }
        }).anyTimes();
        EasyMock.expect(directory.getUrl()).andReturn(url).anyTimes();
        EasyMock.expect(directory.getInterface()).andReturn(MenuService.class).anyTimes();
        EasyMock.replay(directory);

        mergeableClusterInvoker = new MergeableClusterInvoker<MenuService>(directory);

        Result result = mergeableClusterInvoker.invoke(invocation);
        Assert.assertNull(result.getValue());

    }

}
