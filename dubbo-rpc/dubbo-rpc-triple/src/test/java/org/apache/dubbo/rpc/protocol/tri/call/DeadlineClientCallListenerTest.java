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
package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.TriRpcStatus;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeadlineClientCallListenerTest {
    @Test
    void deadlineClosesStreamAfterAResponseMessageAndSuppressesLateCallbacks() throws Exception {
        ClientCall call = mock(ClientCall.class);
        ClientCall.Listener delegate = mock(ClientCall.Listener.class);
        CountDownLatch closed = new CountDownLatch(1);
        doAnswer(invocation -> {
                    closed.countDown();
                    return null;
                })
                .when(delegate)
                .onClose(any(), anyMap(), anyBoolean());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            DeadlineClientCallListener listener = new DeadlineClientCallListener(call, delegate);
            listener.onMessage("first", 5);
            listener.arm(TimeoutCountDown.newCountDown(100, TimeUnit.MILLISECONDS), executor);
            assertTrue(closed.await(2, TimeUnit.SECONDS));
            listener.onMessage("late", 4);
            listener.onClose(TriRpcStatus.OK, Collections.emptyMap(), false);
            verify(call).cancelByLocal(any());
            verify(delegate).onMessage("first", 5);
            verify(delegate, never()).onMessage("late", 4);
            verify(delegate)
                    .onClose(
                            argThat(status -> status.code == TriRpcStatus.Code.DEADLINE_EXCEEDED), anyMap(), eq(false));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void aBlockedObserverDoesNotDelayTransportCancellationOrReceiveConcurrentClose() throws Exception {
        ClientCall call = mock(ClientCall.class);
        ClientCall.Listener delegate = mock(ClientCall.Listener.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        doAnswer(invocation -> {
                    entered.countDown();
                    release.await(2, TimeUnit.SECONDS);
                    return null;
                })
                .when(delegate)
                .onMessage("blocked", 7);
        doAnswer(invocation -> {
                    cancelled.countDown();
                    return null;
                })
                .when(call)
                .cancelByLocal(any());
        doAnswer(invocation -> {
                    finished.countDown();
                    return null;
                })
                .when(delegate)
                .onClose(any(), anyMap(), anyBoolean());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        DeadlineClientCallListener listener = new DeadlineClientCallListener(call, delegate);
        Thread message = new Thread(() -> listener.onMessage("blocked", 7));
        try {
            listener.arm(TimeoutCountDown.newCountDown(150, TimeUnit.MILLISECONDS), executor);
            message.start();
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertTrue(cancelled.await(1, TimeUnit.SECONDS));
            verify(delegate, never()).onClose(any(), anyMap(), anyBoolean());
            release.countDown();
            assertTrue(finished.await(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            message.join(2000);
            executor.shutdownNow();
        }
    }

    @Test
    void completionBeforeArmingAndExpiredBudgetDoNotProduceDuplicateClose() {
        ClientCall call = mock(ClientCall.class);
        ClientCall.Listener delegate = mock(ClientCall.Listener.class);
        DeadlineClientCallListener complete = new DeadlineClientCallListener(call, delegate);
        complete.onClose(TriRpcStatus.OK, Collections.emptyMap(), false);
        complete.arm(TimeoutCountDown.newCountDown(0, TimeUnit.MILLISECONDS), null);
        verify(call, never()).cancelByLocal(any());
        verify(delegate).onClose(TriRpcStatus.OK, Collections.emptyMap(), false);
        DeadlineClientCallListener expired = new DeadlineClientCallListener(call, delegate);
        expired.arm(TimeoutCountDown.newCountDown(0, TimeUnit.MILLISECONDS), null);
        expired.onClose(TriRpcStatus.OK, Collections.emptyMap(), false);
        verify(call).cancelByLocal(any());
        verify(delegate)
                .onClose(argThat(status -> status.code == TriRpcStatus.Code.DEADLINE_EXCEEDED), anyMap(), eq(false));
    }
}
