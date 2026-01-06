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
 * A client-side extension of {@link CallStreamObserver} that provides additional functionality
 * for controlling the outbound request stream. This interface mirrors gRPC's
 * {@code io.grpc.stub.ClientCallStreamObserver} for compatibility.
 *
 * <p>This interface is typically obtained through {@link ClientResponseObserver#beforeStart(ClientCallStreamObserver)}
 * and provides methods for:
 * <ul>
 *   <li>Controlling send-side backpressure via {@link #isReady()} and {@link #setOnReadyHandler(Runnable)}</li>
 *   <li>Controlling receive-side backpressure via {@link #disableAutoRequestWithInitial(int)} and {@link #request(int)}</li>
 * </ul>
 *
 * <h3>Send-Side Backpressure (Controlling Outgoing Data Rate)</h3>
 * <pre>{@code
 * @Override
 * public void beforeStart(ClientCallStreamObserver<Request> requestStream) {
 *     requestStream.disableAutoFlowControl();
 *     requestStream.setOnReadyHandler(() -> {
 *         while (requestStream.isReady() && hasMoreData()) {
 *             requestStream.onNext(getNextRequest());
 *         }
 *     });
 * }
 * }</pre>
 *
 * <h3>Receive-Side Backpressure (Controlling Incoming Data Rate)</h3>
 * <pre>{@code
 * @Override
 * public void beforeStart(ClientCallStreamObserver<Request> requestStream) {
 *     // Request only 10 messages initially
 *     requestStream.disableAutoRequestWithInitial(10);
 * }
 *
 * @Override
 * public void onNext(Response response) {
 *     process(response);
 *     // Request more after processing
 *     requestStream.request(1);
 * }
 * }</pre>
 *
 * @param <ReqT> the type of messages sent to the server (request type)
 * @see CallStreamObserver
 * @see ClientResponseObserver
 */
public interface ClientCallStreamObserver<ReqT> extends CallStreamObserver<ReqT> {

    /**
     * Disables automatic inbound flow control and sets the initial number of messages
     * to request from the server.
     *
     * <p>By default, the runtime automatically requests messages from the server as they
     * are consumed. Calling this method switches to manual flow control mode, where the
     * client must explicitly call {@link #request(int)} to receive more messages.
     *
     * <p>This method <strong>must</strong> be called within
     * {@link ClientResponseObserver#beforeStart(ClientCallStreamObserver)} before the
     * stream starts, otherwise it has no effect.
     *
     * <p><strong>Usage:</strong>
     * <pre>{@code
     * @Override
     * public void beforeStart(ClientCallStreamObserver<Request> requestStream) {
     *     // Start with 5 messages, then request more in onNext()
     *     requestStream.disableAutoRequestWithInitial(5);
     * }
     *
     * @Override
     * public void onNext(Response response) {
     *     process(response);
     *     requestStream.request(1); // Request one more message
     * }
     * }</pre>
     *
     * @param request the initial number of messages to request from the server.
     *                A value of 0 means no messages will be delivered until {@link #request(int)} is called.
     * @see #request(int)
     * @see #disableAutoFlowControl()
     */
    void disableAutoRequestWithInitial(int request);
}
