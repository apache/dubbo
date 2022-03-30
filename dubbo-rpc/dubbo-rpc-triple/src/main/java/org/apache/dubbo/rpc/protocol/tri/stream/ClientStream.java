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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.rpc.TriRpcStatus;

import io.netty.util.concurrent.Future;

import java.util.Map;

public interface ClientStream extends Stream {

    interface Listener extends Stream.Listener {

        void onStart();

        /**
         * Callback when stream completed.
         *
         * @param attachments received from remote peer
         */
        void onComplete(TriRpcStatus status, Map<String, Object> attachments);

        default void onComplete(TriRpcStatus status, Map<String, Object> attachments,
            Map<String, String> reserved) {
            onComplete(status, attachments);
        }

    }

    /**
     * Send message to remote peer.
     *
     * @param message message to send to remote peer
     * @param eos     whether this is the last message
     * @return future to callback when send message is done
     */
    Future<?> sendMessage(byte[] message, int compressFlag, boolean eos);

    Future<?> halfClose();

    void request(int n);
}
