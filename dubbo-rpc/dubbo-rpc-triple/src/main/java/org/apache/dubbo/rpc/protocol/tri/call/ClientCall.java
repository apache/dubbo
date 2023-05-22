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
         * Called when the call is started, user can use this to set some configurations.
         *
         * @param call call implementation
         */
        void onStart(ClientCall call);

        /**
         * Callback when message received.
         *
         * @param message message received
         */
        void onMessage(Object message);

        /**
         * Callback when call is finished.
         *
         * @param status   response status
         * @param trailers response trailers
         */
        void onClose(TriRpcStatus status, Map<String, Object> trailers, boolean isReturnTriException);
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
     * @param metadata         request metadata
     * @param responseListener the listener to receive response
     * @return the stream observer representing the request sink
     */
    StreamObserver<Object> start(RequestMetadata metadata,
        Listener responseListener);

    /**
     * @return true if this call is auto request
     */
    boolean isAutoRequest();

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
