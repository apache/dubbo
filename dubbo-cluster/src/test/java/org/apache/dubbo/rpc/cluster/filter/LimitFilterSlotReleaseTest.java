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
package org.apache.dubbo.rpc.cluster.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.filter.ActiveLimitFilter;
import org.apache.dubbo.rpc.filter.ExecuteLimitFilter;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies limit-filter accounting through the production listener lifecycle.
 *
 * @date 2026-09-08
 */
class LimitFilterSlotReleaseTest {

    private URL url;
    private Invoker<Object> target;
    private Invoker<Object> chain;

    @AfterEach
    void cleanup() {
        RpcContext.removeContext();
        if (url != null) {
            RpcStatus.removeStatus(url, "invoke");
            RpcStatus.removeStatus(url);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"actives", "executes"})
    void downstreamLimitExceptionReleasesSlot(String limit) {
        setup(limit);
        RpcException rejection = new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION, "downstream rejected");
        when(target.invoke(any())).thenThrow(rejection).thenAnswer(call -> success(call.getArgument(0)));

        assertSame(rejection, assertThrows(RpcException.class, () -> chain.invoke(invocation())));
        assertCounts(0, 1);
        assertEquals("ok", chain.invoke(invocation()).getValue());
        assertCounts(0, 1);
        verify(target, times(2)).invoke(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"actives", "executes"})
    void downstreamTimeoutExceptionReleasesSlot(String limit) {
        setup(limit);
        RpcException timeout = new RpcException(RpcException.TIMEOUT_EXCEPTION, "downstream timed out");
        when(target.invoke(any())).thenThrow(timeout).thenAnswer(call -> success(call.getArgument(0)));

        assertSame(timeout, assertThrows(RpcException.class, () -> chain.invoke(invocation())));
        assertCounts(0, 1);
        assertEquals("ok", chain.invoke(invocation()).getValue());
        assertCounts(0, 1);
        verify(target, times(2)).invoke(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"actives", "executes"})
    void asynchronousLimitExceptionReleasesSlot(String limit) {
        setup(limit);
        CompletableFuture<AppResponse> pending = new CompletableFuture<>();
        when(target.invoke(any())).thenAnswer(call -> new AsyncRpcResult(pending, call.getArgument(0)));
        chain.invoke(invocation());
        assertCounts(1, 0);

        pending.completeExceptionally(new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION, "downstream rejected"));

        assertCounts(0, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"actives", "executes"})
    void localRejectionDoesNotReleaseAnotherCallsSlot(String limit) {
        setup(limit);
        CompletableFuture<AppResponse> pending = new CompletableFuture<>();
        when(target.invoke(any())).thenAnswer(call -> new AsyncRpcResult(pending, call.getArgument(0)));
        chain.invoke(invocation());

        assertTrue(assertThrows(RpcException.class, () -> chain.invoke(invocation()))
                .isLimitExceed());
        assertCounts(1, 0);
        verify(target).invoke(any());

        pending.complete(new AppResponse("ok"));
        assertCounts(0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"actives", "executes"})
    void reusedInvocationDoesNotRetainPreviousAdmission(String limit) {
        setup(limit);
        CompletableFuture<AppResponse> pending = new CompletableFuture<>();
        when(target.invoke(any()))
                .thenAnswer(call -> success(call.getArgument(0)))
                .thenAnswer(call -> new AsyncRpcResult(pending, call.getArgument(0)));
        RpcInvocation reused = invocation();
        chain.invoke(reused);
        chain.invoke(invocation());

        assertTrue(assertThrows(RpcException.class, () -> chain.invoke(reused)).isLimitExceed());
        assertCounts(1, 0);
        verify(target, times(2)).invoke(any());

        pending.complete(new AppResponse("ok"));
        assertCounts(0, 0);
    }

    @SuppressWarnings("unchecked")
    private void setup(String limit) {
        url = URL.valueOf("test://localhost:12345/" + UUID.randomUUID() + "?" + limit + "=1&timeout=1");
        target = mock(Invoker.class);
        when(target.getUrl()).thenReturn(url);
        when(target.getInterface()).thenReturn(Object.class);
        Filter filter = "actives".equals(limit) ? new ActiveLimitFilter() : new ExecuteLimitFilter();
        chain = new FilterChainBuilder.CallbackRegistrationInvoker<>(
                new FilterChainBuilder.CopyOfFilterChainNode<>(target, target, filter),
                Collections.singletonList(filter));
    }

    @ParameterizedTest
    @ValueSource(strings = {"actives", "executes"})
    void sharedInvocationKeepsAdmissionSeparateForEachTarget(String limit) {
        setup(limit);
        URL admittedUrl = url;
        CompletableFuture<AppResponse> pending = new CompletableFuture<>();
        when(target.invoke(any())).thenAnswer(call -> new AsyncRpcResult(pending, call.getArgument(0)));
        RpcInvocation shared = invocation();
        chain.invoke(shared);

        setup(limit);
        assertTrue(RpcStatus.beginCount(url, "invoke", 1));
        try {
            assertTrue(
                    assertThrows(RpcException.class, () -> chain.invoke(shared)).isLimitExceed());
            assertCounts(1, 0);

            pending.completeExceptionally(
                    new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION, "downstream rejected"));

            assertEquals(0, RpcStatus.getStatus(admittedUrl).getActive());
            assertEquals(0, RpcStatus.getStatus(admittedUrl, "invoke").getActive());
            assertEquals(1, RpcStatus.getStatus(admittedUrl, "invoke").getFailed());
            assertCounts(1, 0);
        } finally {
            pending.complete(new AppResponse("ok"));
            RpcStatus.endCount(url, "invoke", 0, true);
            RpcStatus.removeStatus(admittedUrl, "invoke");
            RpcStatus.removeStatus(admittedUrl);
        }
    }

    private RpcInvocation invocation() {
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("invoke");
        return invocation;
    }

    private Result success(RpcInvocation invocation) {
        return AsyncRpcResult.newDefaultAsyncResult("ok", invocation);
    }

    private void assertCounts(int active, int failed) {
        assertEquals(active, RpcStatus.getStatus(url).getActive());
        assertEquals(active, RpcStatus.getStatus(url, "invoke").getActive());
        assertEquals(failed, RpcStatus.getStatus(url, "invoke").getFailed());
    }
}
