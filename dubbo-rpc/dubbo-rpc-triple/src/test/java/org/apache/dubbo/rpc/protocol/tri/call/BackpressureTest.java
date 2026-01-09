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

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.observer.ClientCallToObserverAdapter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for backpressure implementation: isReady() and onReadyHandler.
 */
@Timeout(10) // 10 seconds timeout for all tests to prevent hanging
class BackpressureTest {

    /**
     * Test that ClientCallToObserverAdapter stores onReadyHandler locally.
     */
    @Test
    void testSetOnReadyHandlerStoresLocally() {
        MockClientCall mockCall = new MockClientCall();
        ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);

        assertNull(adapter.getOnReadyHandler());

        Runnable handler = () -> {};
        adapter.setOnReadyHandler(handler);

        assertNotNull(adapter.getOnReadyHandler());
        assertEquals(handler, adapter.getOnReadyHandler());
    }

    /**
     * Test that isReady() delegates to ClientCall.isReady().
     */
    @Test
    void testIsReadyDelegatesToClientCall() {
        MockClientCall mockCall = new MockClientCall();
        ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);

        mockCall.setReady(true);
        assertTrue(adapter.isReady());

        mockCall.setReady(false);
        assertFalse(adapter.isReady());
    }

    /**
     * Test that ObserverToClientCallListenerAdapter.onReady() triggers the onReadyHandler.
     */
    @Test
    void testOnReadyTriggersHandler() {
        MockClientCall mockCall = new MockClientCall();
        ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);

        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        adapter.setOnReadyHandler(() -> handlerCalled.set(true));

        // Create listener and set request adapter
        MockStreamObserver mockObserver = new MockStreamObserver();
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);
        listener.setRequestAdapter(adapter);

        // Trigger onReady
        listener.onReady();

        assertTrue(handlerCalled.get());
    }

    /**
     * Test that onReady does nothing when no handler is set.
     */
    @Test
    void testOnReadyWithNoHandler() {
        MockStreamObserver mockObserver = new MockStreamObserver();
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);

        // No adapter set - should not throw
        listener.onReady();

        // Adapter set but no handler - should not throw
        MockClientCall mockCall = new MockClientCall();
        ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);
        listener.setRequestAdapter(adapter);
        listener.onReady();
    }

    /**
     * Test that onReadyHandler can be triggered multiple times.
     */
    @Test
    void testOnReadyHandlerMultipleTriggers() {
        MockClientCall mockCall = new MockClientCall();
        ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);

        AtomicInteger triggerCount = new AtomicInteger(0);
        adapter.setOnReadyHandler(triggerCount::incrementAndGet);

        MockStreamObserver mockObserver = new MockStreamObserver();
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);
        listener.setRequestAdapter(adapter);

        // Trigger multiple times
        listener.onReady();
        listener.onReady();
        listener.onReady();

        assertEquals(3, triggerCount.get());
    }

    /**
     * Test ClientCall.Listener.onReady() default implementation.
     */
    @Test
    void testClientCallListenerOnReadyDefault() {
        ClientCall.Listener listener = new ClientCall.Listener() {
            @Override
            public boolean streamingResponse() {
                return true;
            }

            @Override
            public void onStart(ClientCall call) {}

            @Override
            public void onMessage(Object message, int actualContentLength) {}

            @Override
            public void onClose(TriRpcStatus status, Map<String, Object> trailers, boolean isReturnTriException) {}
        };

        // Default implementation should not throw
        listener.onReady();
    }

    /**
     * Test disableAutoFlowControl delegates to ClientCall.setAutoRequest(false).
     */
    @Test
    void testDisableAutoFlowControl() {
        MockClientCall mockCall = new MockClientCall();
        ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);

        assertTrue(mockCall.isAutoRequest());
        adapter.disableAutoFlowControl();
        assertFalse(mockCall.isAutoRequest());
    }

    /**
     * Test disableAutoRequestWithInitial delegates to ClientCall.setAutoRequestWithInitial().
     */
    @Test
    void testDisableAutoRequestWithInitial() {
        MockClientCall mockCall = new MockClientCall();
        ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);

        adapter.disableAutoRequestWithInitial(5);
        assertEquals(5, mockCall.getInitialRequest());
        assertFalse(mockCall.isAutoRequest());
    }

    /**
     * Test request() delegates to ClientCall.request().
     */
    @Test
    void testRequestDelegation() {
        MockClientCall mockCall = new MockClientCall();
        ClientCallToObserverAdapter<Object> adapter = new ClientCallToObserverAdapter<>(mockCall, true);

        adapter.request(10);
        assertEquals(10, mockCall.getRequestedCount());
    }

    /**
     * Test that ObserverToClientCallListenerAdapter.streamingResponse() returns true.
     */
    @Test
    void testStreamingResponseReturnsTrue() {
        MockStreamObserver mockObserver = new MockStreamObserver();
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(mockObserver);
        assertTrue(listener.streamingResponse());
    }

    /**
     * Test onNext calls delegate.onNext().
     */
    @Test
    void testOnNextCallsDelegate() {
        AtomicBoolean onNextCalled = new AtomicBoolean(false);
        StreamObserver<Object> delegate = new StreamObserver<Object>() {
            @Override
            public void onNext(Object data) {
                onNextCalled.set(true);
            }

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onCompleted() {}
        };

        MockClientCall mockCall = new MockClientCall();
        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(delegate);
        // Must call onStart first to initialize the call reference
        listener.onStart(mockCall);
        listener.onMessage("test", 4);

        assertTrue(onNextCalled.get());
    }

    /**
     * Test onClose with OK status calls delegate.onCompleted().
     */
    @Test
    void testOnCloseWithOkStatus() {
        AtomicBoolean onCompletedCalled = new AtomicBoolean(false);
        StreamObserver<Object> delegate = new StreamObserver<Object>() {
            @Override
            public void onNext(Object data) {}

            @Override
            public void onError(Throwable throwable) {}

            @Override
            public void onCompleted() {
                onCompletedCalled.set(true);
            }
        };

        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(delegate);
        listener.onClose(TriRpcStatus.OK, null, false);

        assertTrue(onCompletedCalled.get());
    }

    /**
     * Test onClose with error status calls delegate.onError().
     */
    @Test
    void testOnCloseWithErrorStatus() {
        AtomicBoolean onErrorCalled = new AtomicBoolean(false);
        StreamObserver<Object> delegate = new StreamObserver<Object>() {
            @Override
            public void onNext(Object data) {}

            @Override
            public void onError(Throwable throwable) {
                onErrorCalled.set(true);
            }

            @Override
            public void onCompleted() {}
        };

        ObserverToClientCallListenerAdapter listener = new ObserverToClientCallListenerAdapter(delegate);
        listener.onClose(TriRpcStatus.INTERNAL.withDescription("error"), null, false);

        assertTrue(onErrorCalled.get());
    }

    /**
     * Mock ClientCall for testing.
     */
    private static class MockClientCall implements ClientCall {
        private boolean ready = true;
        private boolean autoRequest = true;
        private int initialRequest = 0;
        private int requestedCount = 0;

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public int getInitialRequest() {
            return initialRequest;
        }

        public int getRequestedCount() {
            return requestedCount;
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public void cancelByLocal(Throwable t) {}

        @Override
        public void request(int messageNumber) {
            this.requestedCount = messageNumber;
        }

        @Override
        public void sendMessage(Object message) {}

        @Override
        public void start(org.apache.dubbo.rpc.protocol.tri.RequestMetadata metadata, Listener responseListener) {
            // No-op for mock
        }

        @Override
        public boolean isAutoRequest() {
            return autoRequest;
        }

        @Override
        public void setAutoRequestWithInitial(int initialRequest) {
            this.initialRequest = initialRequest;
            this.autoRequest = false;
        }

        @Override
        public void setAutoRequest(boolean autoRequest) {
            this.autoRequest = autoRequest;
        }

        @Override
        public void halfClose() {}

        @Override
        public void setCompression(String compression) {}
    }

    /**
     * Mock StreamObserver for testing.
     */
    private static class MockStreamObserver implements StreamObserver<Object> {
        @Override
        public void onNext(Object data) {}

        @Override
        public void onError(Throwable throwable) {}

        @Override
        public void onCompleted() {}
    }
}
