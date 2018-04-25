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
package com.alibaba.dubbo.cache.filter;

import com.alibaba.dubbo.cache.support.lru.LruCacheFactory;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CacheFilterTest {
    private static RpcInvocation invocation;
    static CacheFilter cacheFilter = new CacheFilter();
    static Invoker<?> invoker = mock(Invoker.class);
    static Invoker<?> invoker1 = mock(Invoker.class);
    static Invoker<?> invoker2 = mock(Invoker.class);

    @BeforeClass
    public static void setUp() {
        invocation = new RpcInvocation();
        cacheFilter.setCacheFactory(new LruCacheFactory());

        URL url = URL.valueOf("test://test:11/test?cache=lru");

        given(invoker.invoke(invocation)).willReturn(new RpcResult(new String("value")));
        given(invoker.getUrl()).willReturn(url);

        given(invoker1.invoke(invocation)).willReturn(new RpcResult(new String("value1")));
        given(invoker1.getUrl()).willReturn(url);

        given(invoker2.invoke(invocation)).willReturn(new RpcResult(new String("value2")));
        given(invoker2.getUrl()).willReturn(url);
    }

    @Test
    public void test_No_Arg_Method() {
        invocation.setMethodName("echo");
        invocation.setParameterTypes(new Class<?>[]{});
        invocation.setArguments(new Object[]{});

        cacheFilter.invoke(invoker, invocation);
        RpcResult rpcResult1 = (RpcResult) cacheFilter.invoke(invoker1, invocation);
        RpcResult rpcResult2 = (RpcResult) cacheFilter.invoke(invoker2, invocation);
        Assert.assertEquals(rpcResult1.getValue(), rpcResult2.getValue());
    }

    @Test
    public void test_Args_Method() {
        invocation.setMethodName("echo1");
        invocation.setParameterTypes(new Class<?>[]{String.class});
        invocation.setArguments(new Object[]{"arg1"});

        cacheFilter.invoke(invoker, invocation);
        RpcResult rpcResult1 = (RpcResult) cacheFilter.invoke(invoker1, invocation);
        RpcResult rpcResult2 = (RpcResult) cacheFilter.invoke(invoker2, invocation);
        Assert.assertEquals(rpcResult1.getValue(), rpcResult2.getValue());
    }
}
