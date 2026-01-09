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
package org.apache.dubbo.common.stream;

/**
 * A client-side {@link StreamObserver} that provides a callback to receive a reference to the
 * outbound request stream observer before the call starts. This interface mirrors gRPC's
 * {@code io.grpc.stub.ClientResponseObserver} for compatibility.
 *
 * <p>This interface is used for advanced flow control scenarios where the client needs to:
 * <ul>
 *   <li>Configure flow control settings before the stream starts</li>
 *   <li>Set up an {@link CallStreamObserver#setOnReadyHandler(Runnable) onReadyHandler} for send-side backpressure</li>
 *   <li>Control the rate of receiving messages using {@link ClientCallStreamObserver#disableAutoRequestWithInitial(int)}</li>
 * </ul>
 *
 * <h3>Example Usage</h3>
 * <pre>{@code
 * // Client streaming with backpressure
 * ClientResponseObserver<DataChunk, Response> responseObserver =
 *         new ClientResponseObserver<DataChunk, Response>() {
 *     @Override
 *     public void beforeStart(ClientCallStreamObserver<DataChunk> requestStream) {
 *         // Disable auto flow control for manual send control
 *         requestStream.disableAutoFlowControl();
 *
 *         // Set up onReadyHandler for send-side backpressure
 *         requestStream.setOnReadyHandler(() -> {
 *             while (requestStream.isReady() && hasMoreData()) {
 *                 requestStream.onNext(getNextChunk());
 *             }
 *         });
 *     }
 *
 *     @Override
 *     public void onNext(Response response) { ... }
 *
 *     @Override
 *     public void onError(Throwable t) { ... }
 *
 *     @Override
 *     public void onCompleted() { ... }
 * };
 *
 * service.clientStream(responseObserver);
 * }</pre>
 *
 * @param <ReqT> the type of messages sent to the server (request type)
 * @param <RespT> the type of messages received from the server (response type)
 * @see ClientCallStreamObserver
 * @see CallStreamObserver
 */
public interface ClientResponseObserver<ReqT, RespT> extends StreamObserver<RespT> {

    /**
     * Called by the runtime prior to the start of a call to provide a reference to the
     * {@link ClientCallStreamObserver} for the outbound request stream.
     *
     * <p>This callback is invoked <strong>before</strong> the underlying stream is created,
     * allowing the client to configure flow control settings that take effect from the
     * beginning of the call.
     *
     * <p><strong>Allowed operations in this callback:</strong>
     * <ul>
     *   <li>{@link ClientCallStreamObserver#setOnReadyHandler(Runnable)} - Set handler for send-side backpressure</li>
     *   <li>{@link ClientCallStreamObserver#disableAutoRequestWithInitial(int)} - Configure receive-side backpressure</li>
     *   <li>{@link CallStreamObserver#disableAutoFlowControl()} - Disable automatic flow control</li>
     * </ul>
     *
     * <p><strong>Note:</strong> Do not call {@link StreamObserver#onNext(Object)} or
     * {@link StreamObserver#onCompleted()} within this callback. Data should only be sent
     * after the stream is ready (via the {@code onReadyHandler}).
     *
     * @param requestStream the {@link ClientCallStreamObserver} for sending requests to the server
     */
    void beforeStart(final ClientCallStreamObserver<ReqT> requestStream);
}
