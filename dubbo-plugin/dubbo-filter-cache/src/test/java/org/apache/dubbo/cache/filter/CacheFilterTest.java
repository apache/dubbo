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
package org.apache.dubbo.cache.filter;

import org.apache.dubbo.cache.CacheFactory;
import org.apache.dubbo.cache.support.expiring.ExpiringCacheFactory;
import org.apache.dubbo.cache.support.jcache.JCacheFactory;
import org.apache.dubbo.cache.support.lru.LruCacheFactory;
import org.apache.dubbo.cache.support.threadlocal.ThreadLocalCacheFactory;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CacheFilterTest {
    private RpcInvocation invocation;
    private CacheFilter cacheFilter = new CacheFilter();
    private Invoker<?> invoker = mock(Invoker.class);
    private Invoker<?> invoker1 = mock(Invoker.class);
    private Invoker<?> invoker2 = mock(Invoker.class);
    private Invoker<?> invoker3 = mock(Invoker.class);
    private Invoker<?> invoker4 = mock(Invoker.class);

    static Stream<Arguments> cacheFactories() {
        return Stream.of(
                Arguments.of("lru", new LruCacheFactory()),
                Arguments.of("jcache", new JCacheFactory()),
                Arguments.of("threadlocal", new ThreadLocalCacheFactory()),
                Arguments.of("expiring", new ExpiringCacheFactory()));
    }

    public void setUp(String cacheType, CacheFactory cacheFactory) {
        invocation = new RpcInvocation();
        cacheFilter.setCacheFactory(cacheFactory);

        URL url = URL.valueOf("test://test:11/test?cache=" + cacheType);

        given(invoker.invoke(invocation)).willReturn(AsyncRpcResult.newDefaultAsyncResult("value", invocation));
        given(invoker.getUrl()).willReturn(url);

        given(invoker1.invoke(invocation)).willReturn(AsyncRpcResult.newDefaultAsyncResult("value1", invocation));
        given(invoker1.getUrl()).willReturn(url);

        given(invoker2.invoke(invocation)).willReturn(AsyncRpcResult.newDefaultAsyncResult("value2", invocation));
        given(invoker2.getUrl()).willReturn(url);

        given(invoker3.invoke(invocation))
                .willReturn(AsyncRpcResult.newDefaultAsyncResult(new RuntimeException(), invocation));
        given(invoker3.getUrl()).willReturn(url);

        given(invoker4.invoke(invocation)).willReturn(AsyncRpcResult.newDefaultAsyncResult(invocation));
        given(invoker4.getUrl()).willReturn(url);
    }

    @ParameterizedTest
    @MethodSource("cacheFactories")
    public void testNonArgsMethod(String cacheType, CacheFactory cacheFactory) {
        setUp(cacheType, cacheFactory);
        invocation.setMethodName("echo");
        invocation.setParameterTypes(new Class<?>[] {});
        invocation.setArguments(new Object[] {});

        cacheFilter.invoke(invoker, invocation);
        cacheFilter.invoke(invoker, invocation);
        Result rpcResult1 = cacheFilter.invoke(invoker1, invocation);
        Result rpcResult2 = cacheFilter.invoke(invoker2, invocation);
        Assertions.assertEquals(rpcResult1.getValue(), rpcResult2.getValue());
        Assertions.assertEquals(rpcResult1.getValue(), "value");
    }

    @ParameterizedTest
    @MethodSource("cacheFactories")
    public void testMethodWithArgs(String cacheType, CacheFactory cacheFactory) {
        setUp(cacheType, cacheFactory);
        invocation.setMethodName("echo1");
        invocation.setParameterTypes(new Class<?>[] {String.class});
        invocation.setArguments(new Object[] {"arg1"});

        cacheFilter.invoke(invoker, invocation);
        cacheFilter.invoke(invoker, invocation);
        Result rpcResult1 = cacheFilter.invoke(invoker1, invocation);
        Result rpcResult2 = cacheFilter.invoke(invoker2, invocation);
        Assertions.assertEquals(rpcResult1.getValue(), rpcResult2.getValue());
        Assertions.assertEquals(rpcResult1.getValue(), "value");
    }

    @ParameterizedTest
    @MethodSource("cacheFactories")
    public void testException(String cacheType, CacheFactory cacheFactory) {
        setUp(cacheType, cacheFactory);
        invocation.setMethodName("echo1");
        invocation.setParameterTypes(new Class<?>[] {String.class});
        invocation.setArguments(new Object[] {"arg2"});

        cacheFilter.invoke(invoker3, invocation);
        cacheFilter.invoke(invoker3, invocation);
        Result rpcResult = cacheFilter.invoke(invoker2, invocation);
        Assertions.assertEquals(rpcResult.getValue(), "value2");
    }

    @ParameterizedTest
    @MethodSource("cacheFactories")
    public void testNull(String cacheType, CacheFactory cacheFactory) {
        setUp(cacheType, cacheFactory);
        invocation.setMethodName("echo1");
        invocation.setParameterTypes(new Class<?>[] {String.class});
        invocation.setArguments(new Object[] {"arg3"});

        cacheFilter.invoke(invoker4, invocation);
        cacheFilter.invoke(invoker4, invocation);
        Result result1 = cacheFilter.invoke(invoker1, invocation);
        Result result2 = cacheFilter.invoke(invoker2, invocation);
        Assertions.assertNull(result1.getValue());
        Assertions.assertNull(result2.getValue());
    }

    @ParameterizedTest
    @MethodSource("cacheFactories")
    void testCacheKeyPreservesCommaSeparatedArgumentBoundaries(String cacheType, CacheFactory factory) {
        assertDistinctCacheEntries(cacheType, factory, "commas", new Object[] {"a,b", "c"}, new Object[] {"a", "b,c"});
    }

    @ParameterizedTest
    @MethodSource("cacheFactories")
    void testCacheKeyDistinguishesNullFromLiteralNull(String cacheType, CacheFactory factory) {
        assertDistinctCacheEntries(cacheType, factory, "null", new Object[] {null, "x"}, new Object[] {"null", "x"});
    }

    @ParameterizedTest
    @MethodSource("cacheFactories")
    void testCacheKeyPreservesEmptyArgumentBoundaries(String cacheType, CacheFactory factory) {
        assertDistinctCacheEntries(cacheType, factory, "empty", new Object[] {"", "a,b"}, new Object[] {"a", "b"});
    }

    private static void assertDistinctCacheEntries(
            String cacheType,
            CacheFactory factory,
            String scenario,
            Object[] firstArguments,
            Object[] secondArguments) {
        CacheFilter filter = new CacheFilter();
        filter.setCacheFactory(factory);
        URL url = URL.valueOf("test://test:11/cache-key-" + scenario + "?cache=" + cacheType);
        Invoker<?> target = mock(Invoker.class);
        given(target.getUrl()).willReturn(url);

        RpcInvocation first = new RpcInvocation();
        first.setMethodName("combine");
        first.setParameterTypes(new Class<?>[] {String.class, String.class});
        first.setArguments(firstArguments);

        RpcInvocation second = new RpcInvocation();
        second.setMethodName("combine");
        second.setParameterTypes(new Class<?>[] {String.class, String.class});
        second.setArguments(secondArguments);

        given(target.invoke(first)).willReturn(AsyncRpcResult.newDefaultAsyncResult("first", first));
        given(target.invoke(second)).willReturn(AsyncRpcResult.newDefaultAsyncResult("second", second));

        Assertions.assertEquals("first", filter.invoke(target, first).getValue());
        Assertions.assertEquals("second", filter.invoke(target, second).getValue());
        Assertions.assertEquals("first", filter.invoke(target, first).getValue());
        Assertions.assertEquals("second", filter.invoke(target, second).getValue());
        // LRU-2 admits an entry only on its second write; the third call must hit.
        Assertions.assertEquals("first", filter.invoke(target, first).getValue());
        Assertions.assertEquals("second", filter.invoke(target, second).getValue());
        int expectedInvocations = "lru".equals(cacheType) ? 2 : 1;
        verify(target, times(expectedInvocations)).invoke(first);
        verify(target, times(expectedInvocations)).invoke(second);
    }
}
