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

import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.Future;

import java.net.SocketAddress;

/**
 * Stream is a bi-directional channel that manipulates the data flow between peers. Inbound data
 * from remote peer is acquired by {@link Listener}. Outbound data to remote peer is sent directly
 * by {@link Stream}. Backpressure is supported by {@link #request(int)}.
 */
public interface Stream {

    /**
     * Register a {@link Listener} to receive inbound data from remote peer.
     */
    interface Listener {

        /**
         * Callback when receive message. Note this method may be called many times if is a
         * streaming .
         *
         * @param message message received from remote peer
         */
        void onMessage(byte[] message, boolean isReturnTriException);

        /**
         * Callback when receive cancel signal.
         *
         * @param status the cancel status
         */
        void onCancelByRemote(TriRpcStatus status);

    }

    /**
     * Send headers to remote peer.
     *
     * @param headers headers to send to remote peer
     * @return future to callback when send headers is done
     */
    Future<?> sendHeader(Http2Headers headers);

    /**
     * Cancel by this peer.
     *
     * @param status cancel status to send to remote peer
     * @return future to callback when cancel is done
     */
    Future<?> cancelByLocal(TriRpcStatus status);

    /**
     * Get remote peer address.
     *
     * @return socket address of remote peer
     */
    SocketAddress remoteAddress();

    /**
     * Request n message from remote peer.
     *
     * @param n number of message
     */
    void request(int n);

}
