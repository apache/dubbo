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

import org.apache.dubbo.common.stream.ClientCallStreamObserver;
import org.apache.dubbo.common.stream.ClientResponseObserver;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCall;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Streaming API available before fault delay ends, with bounded pending writes and unchanged Triple callbacks. */
final class XdsDeferredStream extends CancelableStreamObserver<Object>
        implements ClientResponseObserver<Object, Object> {
    private final StreamObserver<Object> response;
    private final CancellationContext lifecycle;
    private final AtomicBoolean terminal = new AtomicBoolean();
    private final Bridge bridge = new Bridge();
    final ClientCallToObserverAdapter<Object> writer = new ClientCallToObserverAdapter<>(bridge, true);

    @SuppressWarnings("unchecked")
    XdsDeferredStream(StreamObserver<?> response, CancellationContext lifecycle) {
        this.response = (StreamObserver<Object>) response;
        this.lifecycle = lifecycle;
    }

    @SuppressWarnings("unchecked")
    synchronized void prepare() {
        if (response instanceof CancelableStreamObserver) {
            ((CancelableStreamObserver<Object>) response).setCancellationContext(lifecycle);
        }
        if (response instanceof ClientResponseObserver) {
            ((ClientResponseObserver<Object, Object>) response).beforeStart(writer);
        } else if (response instanceof CancelableStreamObserver) {
            ((CancelableStreamObserver<Object>) response).beforeStart(writer);
        }
    }

    @Override
    public void beforeStart(ClientCallStreamObserver<Object> actual) {
        bridge.bind(actual);
        actual.setOnReadyHandler(() -> {
            bridge.activate();
            Runnable handler = writer.getOnReadyHandler();
            if (!terminal.get() && handler != null) handler.run();
        });
    }

    @Override
    public void startRequest() {
        bridge.activate();
        if (!terminal.get() && response instanceof CancelableStreamObserver) {
            ((CancelableStreamObserver<?>) response).startRequest();
        }
    }

    @Override
    public synchronized void onNext(Object value) {
        if (!terminal.get()) response.onNext(value);
    }

    @Override
    public synchronized void onError(Throwable error) {
        if (terminal.compareAndSet(false, true)) {
            bridge.close();
            try {
                response.onError(error);
            } finally {
                lifecycle.cancel(error);
            }
        }
    }

    @Override
    public synchronized void onCompleted() {
        if (terminal.compareAndSet(false, true)) {
            bridge.close();
            try {
                response.onCompleted();
            } finally {
                lifecycle.cancel(null);
            }
        }
    }

    private final class Bridge implements ClientCall {
        private final Deque<Consumer<ClientCallStreamObserver<Object>>> pending = new ArrayDeque<>();
        private ClientCallStreamObserver<Object> actual;
        private boolean started;
        private boolean closed;
        private boolean auto = true;
        private Integer initial;
        private String compression;

        synchronized void bind(ClientCallStreamObserver<Object> delegate) {
            if (closed || lifecycle.isCancelled()) {
                delegate.onError(TriRpcStatus.CANCELLED.asException());
                return;
            }
            actual = delegate;
            if (compression != null) delegate.setCompression(compression);
            if (initial != null) delegate.disableAutoRequestWithInitial(initial);
            else if (!auto) delegate.disableAutoFlowControl();
        }

        synchronized void activate() {
            if (closed || actual == null || started) return;
            started = true;
            while (!pending.isEmpty() && !closed) pending.removeFirst().accept(actual);
        }

        synchronized void close() {
            closed = true;
            pending.clear();
            actual = null;
        }

        private synchronized void operation(Consumer<ClientCallStreamObserver<Object>> action) {
            if (closed) throw new IllegalStateException("Deferred Triple stream has terminated");
            if (started) {
                action.accept(actual);
                return;
            }
            if (pending.size() >= 128) {
                cancelByLocal(TriRpcStatus.fromCode(TriRpcStatus.Code.RESOURCE_EXHAUSTED)
                        .withDescription("Fault-delayed stream pending operation limit reached")
                        .asException());
                return;
            }
            pending.addLast(action);
        }

        public synchronized boolean isReady() {
            return !closed && started && actual != null && actual.isReady();
        }

        public void cancelByLocal(Throwable error) {
            ClientCallStreamObserver<Object> delegate;
            synchronized (this) {
                delegate = actual;
                close();
            }
            lifecycle.cancel(error == null ? TriRpcStatus.CANCELLED.asException() : error);
            if (delegate != null) delegate.onError(error == null ? TriRpcStatus.CANCELLED.asException() : error);
        }

        public void request(int count) {
            if (count <= 0) throw new IllegalArgumentException("Request count must be positive");
            operation(delegate -> delegate.request(count));
        }

        public void sendMessage(Object value) {
            operation(delegate -> delegate.onNext(value));
        }

        public void halfClose() {
            operation(ClientCallStreamObserver::onCompleted);
        }

        public synchronized boolean isAutoRequest() {
            return auto;
        }

        public synchronized void setAutoRequestWithInitial(int count) {
            if (count < 0) throw new IllegalArgumentException("Initial request count must be non-negative");
            auto = false;
            initial = count;
            if (actual != null) actual.disableAutoRequestWithInitial(count);
        }

        public synchronized void setAutoRequest(boolean enabled) {
            auto = enabled;
            initial = null;
            if (actual != null && !enabled) actual.disableAutoFlowControl();
        }

        public synchronized void setCompression(String value) {
            compression = value;
            if (actual != null) actual.setCompression(value);
        }

        public void start(RequestMetadata metadata, Listener listener) {
            throw new UnsupportedOperationException("Deferred stream is bound by Triple beforeStart");
        }
    }
}
