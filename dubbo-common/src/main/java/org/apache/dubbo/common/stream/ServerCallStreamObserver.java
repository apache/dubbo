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
 * A server-side extension of {@link CallStreamObserver} that provides flow control capabilities
 * for outbound response streams. This interface mirrors gRPC's
 * {@code io.grpc.stub.ServerCallStreamObserver} for compatibility.
 *
 * <p>On the server side, this interface is obtained by casting the {@link StreamObserver}
 * parameter passed to streaming RPC methods. It allows the server to:
 * <ul>
 *   <li>Check if the response stream is ready using {@link #isReady()}</li>
 *   <li>Set a callback for when the stream becomes ready using {@link #setOnReadyHandler(Runnable)}</li>
 *   <li>Control inbound flow from the client using {@link #request(int)} and {@link #disableAutoFlowControl()}</li>
 * </ul>
 *
 * <h3>Server-Side Send Backpressure Example</h3>
 * <pre>{@code
 * @Override
 * public void serverStream(Request request, StreamObserver<Response> responseObserver) {
 *     ServerCallStreamObserver<Response> serverObserver =
 *             (ServerCallStreamObserver<Response>) responseObserver;
 *
 *     AtomicInteger sent = new AtomicInteger(0);
 *     int totalCount = 100;
 *
 *     serverObserver.setOnReadyHandler(() -> {
 *         while (serverObserver.isReady() && sent.get() < totalCount) {
 *             int seq = sent.getAndIncrement();
 *             serverObserver.onNext(createResponse(seq));
 *         }
 *         if (sent.get() >= totalCount) {
 *             serverObserver.onCompleted();
 *         }
 *     });
 * }
 * }</pre>
 *
 * <h3>Server-Side Receive Backpressure Example (for Client/Bidi Streaming)</h3>
 * <pre>{@code
 * @Override
 * public StreamObserver<Request> clientStream(StreamObserver<Response> responseObserver) {
 *     ServerCallStreamObserver<Response> serverObserver =
 *             (ServerCallStreamObserver<Response>) responseObserver;
 *
 *     // Control how many messages we receive from the client
 *     serverObserver.disableAutoFlowControl();
 *     serverObserver.request(5); // Start with 5 messages
 *
 *     return new StreamObserver<Request>() {
 *         @Override
 *         public void onNext(Request request) {
 *             process(request);
 *             serverObserver.request(1); // Request one more
 *         }
 *         // ... onError, onCompleted
 *     };
 * }
 * }</pre>
 *
 * @param <RespT> the type of messages sent to the client (response type)
 * @see CallStreamObserver
 * @see StreamObserver
 */
public interface ServerCallStreamObserver<RespT> extends CallStreamObserver<RespT> {

    default void disableAutoRequest() {
        disableAutoFlowControl();
    }
}
