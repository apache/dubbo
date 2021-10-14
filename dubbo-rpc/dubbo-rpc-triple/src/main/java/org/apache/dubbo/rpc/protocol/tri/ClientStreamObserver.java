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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.stream.StreamObserver;

public interface ClientStreamObserver<T> extends StreamObserver<T> {

    /**
     * Requests the peer to produce {@code count} more messages to be delivered to the 'inbound'
     * {@link StreamObserver}.
     *
     * @param count more messages
     */
    default void request(int count) {
        // todo support
    }

    /**
     * Swaps to manual flow control where no message will be delivered to {@link
     * StreamObserver#onNext(Object)} unless it is {@link #request request()}ed.
     */
    default void disableAutoRequestWithInitial(int request) {
        //  todo support
    }

    /**
     * Sets the compression algorithm to use for the call
     *
     * @param compression {@link Compressor}
     */
    void setCompression(String compression);


}
