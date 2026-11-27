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

import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.DeadlineFuture;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Locally enforces a streaming call deadline and serializes terminal delivery with response messages. */
public final class DeadlineClientCallListener implements ClientCall.Listener {
    private final ClientCall call;
    private final ClientCall.Listener delegate;
    private final AtomicBoolean closed = new AtomicBoolean();
    private ExecutorService deliveryExecutor;
    private DeadlineFuture timer;

    public DeadlineClientCallListener(ClientCall call, ClientCall.Listener delegate) {
        this.call = call;
        this.delegate = delegate;
    }

    /** Arm after start() has installed the stream; completion before arming creates no timer. */
    public synchronized void arm(TimeoutCountDown deadline, ExecutorService executor) {
        if (closed.get()) {
            return;
        }
        deliveryExecutor = executor;
        long remaining = deadline.timeRemaining(TimeUnit.MILLISECONDS);
        if (remaining <= 0) {
            expire();
            return;
        }
        timer = DeadlineFuture.newFuture("stream", "stream", "", (int) Math.min(remaining, Integer.MAX_VALUE), null);
        timer.whenComplete((response, error) -> {
            if (error != null || (response != null && response.hasException())) {
                expire();
            }
        });
    }

    private void expire() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        TriRpcStatus status = TriRpcStatus.DEADLINE_EXCEEDED.withDescription("Call deadline exceeded");
        try {
            call.cancelByLocal(status.asException());
        } finally {
            Runnable terminal = () -> {
                synchronized (this) {
                    delegate.onClose(status, Collections.emptyMap(), false);
                }
            };
            // Transport cancellation must not wait for a user's in-flight onNext callback.
            if (deliveryExecutor == null) {
                terminal.run();
            } else {
                deliveryExecutor.execute(terminal);
            }
        }
    }

    @Override
    public boolean streamingResponse() {
        return delegate.streamingResponse();
    }

    @Override
    public synchronized void onStart(ClientCall call) {
        if (!closed.get()) {
            delegate.onStart(call);
        }
    }

    @Override
    public synchronized void onMessage(Object message, int length) {
        if (!closed.get()) {
            delegate.onMessage(message, length);
        }
    }

    @Override
    public void onClose(TriRpcStatus status, Map<String, Object> trailers, boolean triException) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (this) {
            if (timer != null) {
                timer.received(TriRpcStatus.OK, new AppResponse());
            }
            delegate.onClose(status, trailers, triException);
        }
    }

    @Override
    public synchronized void onReady() {
        if (!closed.get()) {
            delegate.onReady();
        }
    }
}
