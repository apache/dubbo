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

import com.alibaba.dubbo.cache.CacheFactory;
import com.alibaba.dubbo.cache.support.jcache.JCacheFactory;
import com.alibaba.dubbo.cache.support.lru.LruCacheFactory;
import com.alibaba.dubbo.cache.support.threadlocal.ThreadLocalCacheFactory;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.junit.runners.Parameterized.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class CacheFilterTest {
    private RpcInvocation invocation;
    private CacheFilter cacheFilter = new CacheFilter();
    private Invoker<?> invoker = mock(Invoker.class);
    private Invoker<?> invoker1 = mock(Invoker.class);
    private Invoker<?> invoker2 = mock(Invoker.class);
    private String cacheType;
    private CacheFactory cacheFactory;

    public CacheFilterTest(String cacheType, CacheFactory cacheFactory) {
        this.cacheType = cacheType;
        this.cacheFactory = cacheFactory;
    }

    @Parameters
    public static List<Object[]> cacheFactories() {
        return Arrays.asList(new Object[][]{
                {"lru", new LruCacheFactory()},
                {"jcache", new JCacheFactory()},
                {"threadlocal", new ThreadLocalCacheFactory()}
        });
    }

    @Before
    public void setUp() throws Exception {
        invocation = new RpcInvocation();
        cacheFilter.setCacheFactory(this.cacheFactory);

        URL url = URL.valueOf("test://test:11/test?cache=" + this.cacheType);

        given(invoker.invoke(invocation)).willReturn(new RpcResult("value"));
        given(invoker.getUrl()).willReturn(url);

        given(invoker1.invoke(invocation)).willReturn(new RpcResult("value1"));
        given(invoker1.getUrl()).willReturn(url);

        given(invoker2.invoke(invocation)).willReturn(new RpcResult("value2"));
        given(invoker2.getUrl()).willReturn(url);

    }

    @Test
    public void testNonArgsMethod() {
        invocation.setMethodName("echo");
        invocation.setParameterTypes(new Class<?>[]{});
        invocation.setArguments(new Object[]{});

        cacheFilter.invoke(invoker, invocation);
        RpcResult rpcResult1 = (RpcResult) cacheFilter.invoke(invoker1, invocation);
        RpcResult rpcResult2 = (RpcResult) cacheFilter.invoke(invoker2, invocation);
        Assert.assertEquals(rpcResult1.getValue(), rpcResult2.getValue());
    }

    @Test
    public void testMethodWithArgs() {
        invocation.setMethodName("echo1");
        invocation.setParameterTypes(new Class<?>[]{String.class});
        invocation.setArguments(new Object[]{"arg1"});

        cacheFilter.invoke(invoker, invocation);
        RpcResult rpcResult1 = (RpcResult) cacheFilter.invoke(invoker1, invocation);
        RpcResult rpcResult2 = (RpcResult) cacheFilter.invoke(invoker2, invocation);
        Assert.assertEquals(rpcResult1.getValue(), rpcResult2.getValue());
    }
}
