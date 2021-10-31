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

/**
 * An observer used for transport messaging which provides full streaming support.
 * A TransportObserver receives raw data or control messages from local/remote.
 * Implementations should prefer to extend {@link OutboundTransportObserver} and {@link InboundTransportObserver}
 * instead of this interface.
 */
public interface TransportObserver {

    /**
     * Transport metadata
     *
     * @param metadata  metadata KV paris
     * @param endStream whether this data should terminate the stream
     */
    void onMetadata(Metadata metadata, boolean endStream);

    /**
     * Transport data
     *
     * @param data      raw byte array
     * @param endStream whether this data should terminate the stream
     */
    void onData(byte[] data, boolean endStream);

    /**
     * Error
     *
     * @param status error status
     */
    void onError(GrpcStatus status);

    /**
     * Set stream completed
     */
    void onComplete();

}
