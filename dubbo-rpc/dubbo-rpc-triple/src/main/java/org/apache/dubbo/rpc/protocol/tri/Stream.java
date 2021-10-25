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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;

/**
 * Stream acts as a bi-directional intermediate layer for processing streaming data . It serializes object instance to
 * byte[] then send to remote, and deserializes byte[] to object instance from remote. {@link #inboundTransportObserver()}
 * and {@link #subscribe(OutboundTransportObserver)} provide {@link TransportObserver} to receive or send remote data.
 * {@link #inboundMessageObserver()} and {@link #subscribe(StreamObserver)} provide {@link StreamObserver}
 * as API for users fetching/emitting objects from/to remote peer.
 */
public interface Stream {

    Logger LOGGER = LoggerFactory.getLogger(Stream.class);

    /**
     * Register an upstream data observer to receive byte[] sent by this stream
     *
     * @param observer receives remote byte[] data
     */
    void subscribe(OutboundTransportObserver observer);

    /**
     * Get a downstream data observer for writing byte[] data to this stream
     *
     * @return an observer for writing byte[] to remote peer
     */
    TransportObserver inboundTransportObserver();

    /**
     * Register an upstream data observer to receive instance sent by this stream
     *
     * @param outboundMessageObserver receives remote byte[] data
     */
    void subscribe(StreamObserver<Object> outboundMessageObserver);

    /**
     * Get a downstream data observer for transmitting instances to application code
     *
     * @return an observer for writing byte[] to remote peer
     */
    StreamObserver<Object> inboundMessageObserver();

    /**
     * Execute a task in stream's executor
     *
     * @param runnable task to run
     */
    void execute(Runnable runnable);

}
