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

import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;

import java.util.Map;

/**
 * ClientCall does not care about transport layer details.
 */
public interface ClientCall {

    /**
     * Listener for receive response.
     */
    interface Listener {

        /**
         * Whether the response is streaming response.
         *
         * @return true if the response is a streaming response
         */
        boolean streamingResponse();

        /**
         * Called when the call is started, user can use this to set some configurations.
         *
         * @param call call implementation
         */
        void onStart(ClientCall call);

        /**
         * Callback when message received.
         *
         * @param message             message received
         * @param actualContentLength actual content length from body
         */
        void onMessage(Object message, int actualContentLength);

        /**
         * Callback when call is finished.
         *
         * @param status   response status
         * @param trailers response trailers
         */
        void onClose(TriRpcStatus status, Map<String, Object> trailers, boolean isReturnTriException);

        /**
         * Called when the call becomes ready for writing after previously returning false from
         * {@link ClientCall#isReady()}. This callback is invoked by the transport layer when
         * backpressure is relieved and more messages can be sent.
         *
         * <p>Implementations should use this method to resume sending messages that were
         * paused due to backpressure.
         */
        default void onReady() {}
    }

    /**
     * Returns whether the stream is ready for writing.
     * If false, the caller should avoid calling sendMessage to prevent blocking or excessive buffering.
     *
     * @return true if the stream is ready for writing
     */
    default boolean isReady() {
        return true;
    }

    /**
     * Send reset to server, no more data will be sent or received.
     *
     * @param t cause
     */
    void cancelByLocal(Throwable t);

    /**
     * Request max n message from server
     *
     * @param messageNumber max message number
     */
    void request(int messageNumber);

    /**
     * Send message to server
     *
     * @param message request to send
     */
    void sendMessage(Object message);

    /**
     * Start the call with the given metadata and response listener.
     *
     * @param metadata         request metadata
     * @param responseListener the listener to receive response
     */
    void start(RequestMetadata metadata, Listener responseListener);

    /**
     * @return true if this call is auto request
     */
    boolean isAutoRequest();

    /**
     * Enable auto request for this call with an initial number of messages to request.
     * <p>
     * This variant of auto request allows specifying how many response messages should be
     * requested from the server immediately when the call starts or auto request is enabled.
     * It is similar to {@link #setAutoRequest(boolean)} but also configures the initial
     * {@link #request(int) request} amount.
     *
     * @param initialRequest the initial number of messages to request from the server
     */
    void setAutoRequestWithInitial(int initialRequest);

    /**
     * Set auto request for this call
     *
     * @param autoRequest whether auto request is enabled
     */
    void setAutoRequest(boolean autoRequest);

    /**
     * No more data will be sent.
     */
    void halfClose();

    /**
     * Set compression algorithm for request.
     *
     * @param compression compression algorithm
     */
    void setCompression(String compression);
}
