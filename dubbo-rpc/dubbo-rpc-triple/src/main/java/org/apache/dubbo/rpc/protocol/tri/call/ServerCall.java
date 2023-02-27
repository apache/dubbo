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

import java.util.Map;

/**
 * ServerCall manipulates server details of a RPC call. Request messages are acquired by {@link
 * Listener}. Backpressure is supported by {@link #request(int)}.Response messages are sent by
 * {@link ServerCall#sendMessage(Object)}.
 */
public interface ServerCall {

    /**
     * A listener to receive request messages.
     */
    interface Listener {

        /**
         * Callback when a request message is received.
         *
         * @param message message received
         */
        void onMessage(Object message);

        /**
         * @param status when the call is canceled.
         */
        void onCancel(TriRpcStatus status);

        /**
         * Request completed.
         */
        void onComplete();
    }

    /**
     * Send message to client
     *
     * @param message message to send
     */
    void sendMessage(Object message);

    /**
     * Request more request data from the client.
     *
     * @param numMessages max number of messages
     */
    void request(int numMessages);

    /**
     * Close the call.
     *
     * @param status        status of the call to send to the client
     * @param responseAttrs response attachments
     */
    void close(TriRpcStatus status, Map<String, Object> responseAttrs);

}
