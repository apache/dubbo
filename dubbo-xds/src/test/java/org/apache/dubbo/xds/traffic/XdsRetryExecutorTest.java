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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.DeadlineFuture;
import org.apache.dubbo.rpc.protocol.tri.UnaryRetryState;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCall;
import org.apache.dubbo.rpc.protocol.tri.call.UnaryClientCallListener;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RetryPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class XdsRetryExecutorTest {
    @Test
    void responseHeadersCommitUnaryButTrailersOnlyFailureCanRetry() throws Exception {
        for (boolean headers : new boolean[] {true, false}) {
            try (XdsRetryExecutor executor = new XdsRetryExecutor()) {
                RpcInvocation invocation = new RpcInvocation();
                invocation.setMethodName("call");
                invocation.put(TimeoutCountDown.CALL_ATTRIBUTE, TimeoutCountDown.newCountDown(2, TimeUnit.SECONDS));
                AtomicInteger attempts = new AtomicInteger();
                XdsRetryExecutor.Call call = executor.newCall(
                        invocation,
                        XdsRetryPolicy.parse(RetryPolicy.newBuilder()
                                .setRetryOn("unavailable")
                                .build()),
                        attempt -> {
                            int number = attempts.incrementAndGet();
                            UnaryRetryState.Attempt state =
                                    (UnaryRetryState.Attempt) attempt.get(UnaryRetryState.ATTRIBUTE);
                            state.getState().remember(new byte[] {1});
                            DeadlineFuture response = DeadlineFuture.newFuture("service", "call", "peer", 1000, null);
                            UnaryClientCallListener listener = new UnaryClientCallListener(response, state);
                            if (headers || number > 1) {
                                listener.onStart(mock(ClientCall.class));
                            }
                            if (number > 1) {
                                listener.onMessage("success", 7);
                            }
                            listener.onClose(
                                    number == 1 ? TriRpcStatus.UNAVAILABLE : TriRpcStatus.OK,
                                    Collections.emptyMap(),
                                    false);
                            return new AsyncRpcResult(response, attempt);
                        });
                call.start();
                AppResponse response = call.get(2, TimeUnit.SECONDS);
                assertEquals(headers ? 1 : 2, attempts.get());
                if (headers) {
                    assertEquals(TriRpcStatus.Code.UNAVAILABLE, TriRpcStatus.getStatus(response.getException()).code);
                } else {
                    assertFalse(response.hasException());
                    assertEquals("success", response.getValue());
                }
            }
        }
    }
}
