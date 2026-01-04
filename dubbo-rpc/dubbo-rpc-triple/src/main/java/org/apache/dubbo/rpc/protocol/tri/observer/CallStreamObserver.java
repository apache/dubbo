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
package org.apache.dubbo.rpc.protocol.tri.observer;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;

/**
 * An extension of {@link StreamObserver} that provides additional functionality for flow control
 * and backpressure. This interface mirrors gRPC's {@code CallStreamObserver} for compatibility.
 *
 * <p>Key features:
 * <ul>
 *   <li>{@link #isReady()} - Check if the stream is ready to accept more messages</li>
 *   <li>{@link #setOnReadyHandler(Runnable)} - Set a callback for when the stream becomes ready</li>
 *   <li>{@link #request(int)} - Request more messages from the peer (for inbound flow control)</li>
 *   <li>{@link #disableAutoFlowControl()} - Switch to manual flow control mode</li>
 * </ul>
 *
 * @param <T> the type of value passed to the stream
 */
public interface CallStreamObserver<T> extends StreamObserver<T> {

    /**
     * Returns {@code true} if the stream is ready to accept more messages.
     *
     * <p>If {@code false} is returned, the caller should avoid calling
     * {@link StreamObserver#onNext(Object)} to prevent excessive buffering.
     * Instead, the caller should wait for the {@link #setOnReadyHandler(Runnable) onReadyHandler}
     * to be called before sending more messages.
     *
     * <p>This method is safe to call from multiple threads.
     *
     * @return {@code true} if the stream is ready for writing, {@code false} otherwise
     */
    boolean isReady();

    /**
     * Sets a callback to be invoked when the stream becomes ready for writing after
     * previously returning {@code false} from {@link #isReady()}.
     *
     * <p>The handler will be called on the event loop thread, so any long-running
     * operations should be offloaded to a separate executor.
     *
     * <p>Typical usage pattern:
     * <pre>{@code
     * observer.setOnReadyHandler(() -> {
     *     while (observer.isReady() && hasMoreData()) {
     *         observer.onNext(getNextData());
     *     }
     * });
     * }</pre>
     *
     * @param onReadyHandler the handler to invoke when the stream becomes ready
     */
    void setOnReadyHandler(Runnable onReadyHandler);

    /**
     * Requests the peer to produce {@code count} more messages to be delivered to the 'inbound'
     * {@link StreamObserver}.
     *
     * <p>This method is safe to call from multiple threads without external synchronization.
     *
     * @param count more messages
     */
    void request(int count);

    /**
     * Sets the compression algorithm to use for the call
     * <p>
     * For stream set compression needs to determine whether the metadata has been sent, and carry
     * on corresponding processing
     *
     * @param compression {@link Compressor}
     */
    void setCompression(String compression);

    /**
     * Swaps to manual flow control where no message will be delivered to {@link
     * StreamObserver#onNext(Object)} unless it is {@link #request request()}ed. Since {@code
     * request()} may not be called before the call is started, a number of initial requests may be
     * specified.
     */
    void disableAutoFlowControl();

    /**
     * Compatibility method to mirror gRPC Java
     * {@code io.grpc.stub.CallStreamObserver#disableAutoInboundFlowControl()}.
     * <p>
     * This allows code written against gRPC's {@code CallStreamObserver} API to be
     * more easily reused with Dubbo by providing an equivalent entry point that
     * delegates to {@link #disableAutoFlowControl()}.
     */
    default void disableAutoInboundFlowControl() {
        disableAutoFlowControl();
    }
}
