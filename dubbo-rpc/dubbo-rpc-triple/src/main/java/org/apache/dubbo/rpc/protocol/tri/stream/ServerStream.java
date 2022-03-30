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

public interface ServerStream extends Stream {

    interface Listener extends Stream.Listener {

        /**
         * Callback when receive headers
         *
         * @param headers headers received from remote peer
         */
        void onHeader(Map<String, Object> headers);

        void onComplete();
    }

    Future<?> complete(TriRpcStatus status, Map<String, Object> attachments);

    Future<?> sendMessage(byte[] message, int compressFlag);

}
