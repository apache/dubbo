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
package org.apache.dubbo.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsyncRpcResultTest {
    @Test
    void cancellationRemainsCancelledAcrossRepeatedContextStagesAndReleasesTheSourceOnce() {
        try {
            RpcInvocation invocation = new RpcInvocation();
            invocation.setInvokeMode(InvokeMode.FUTURE);
            invocation.put(TimeoutCountDown.CALL_ATTRIBUTE, TimeoutCountDown.newCountDown(6000, TimeUnit.MILLISECONDS));
            CompletableFuture<AppResponse> source = new CompletableFuture<>();
            AtomicInteger callbacks = new AtomicInteger();
            AsyncRpcResult result = new AsyncRpcResult(source, invocation);
            result.whenCompleteWithContext((response, error) -> callbacks.incrementAndGet());
            result.whenCompleteWithContext((response, error) -> callbacks.incrementAndGet());
            AtomicInteger sourceReleased = new AtomicInteger();
            result.getResponseFuture().whenComplete((response, error) -> {
                if (source.isCancelled()) sourceReleased.incrementAndGet();
            });
            Future<?> future = RpcContext.getServiceContext().getFuture();
            assertTrue(future.cancel(false));
            assertTrue(future.isCancelled());
            assertTrue(result.getResponseFuture().isCancelled());
            assertTrue(source.isCancelled());
            assertEquals(2, callbacks.get());
            assertEquals(1, sourceReleased.get(), "Transport source must be cancelled before application notification");
            assertFalse(source.complete(new AppResponse("late")));
            assertEquals(2, callbacks.get());
        } finally {
            RpcContext.removeContext();
        }
    }

    @Test
    void aCompletedResponseCannotBeReplacedByLateCancellation() throws Exception {
        try {
            RpcInvocation invocation = new RpcInvocation();
            invocation.setInvokeMode(InvokeMode.FUTURE);
            invocation.put(TimeoutCountDown.CALL_ATTRIBUTE, TimeoutCountDown.newCountDown(6000, TimeUnit.MILLISECONDS));
            CompletableFuture<AppResponse> source = new CompletableFuture<>();
            AsyncRpcResult result = new AsyncRpcResult(source, invocation);
            result.whenCompleteWithContext((response, error) -> {});
            source.complete(new AppResponse("done"));
            Future<?> future = RpcContext.getServiceContext().getFuture();
            assertFalse(future.cancel(false));
            assertFalse(source.isCancelled());
            assertEquals("done", future.get(1, TimeUnit.SECONDS));
        } finally {
            RpcContext.removeContext();
        }
    }
}
