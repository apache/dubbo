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

import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing stream reconnection.
 * Provides callback mechanisms for reliable streaming to handle connection failures.
 */
public interface ReconnectionManager {

    /**
     * Attempt to reconnect the underlying connection.
     * This should trigger a new connection establishment and return a new stream channel.
     *
     * @return CompletableFuture that completes with true if reconnection succeeded, false otherwise
     */
    CompletableFuture<Boolean> attemptReconnection();

    /**
     * Check if the underlying connection is currently active.
     *
     * @return true if connection is active, false otherwise
     */
    boolean isConnectionActive();

    /**
     * Get a new stream channel after successful reconnection.
     * This should only be called after successful reconnection.
     *
     * @return new stream channel, or null if no connection available
     * @deprecated Use {@link #createStreamFuture()} for type-safe channel creation
     */
    @Deprecated
    Object getNewStreamChannel();

    /**
     * Create a new TripleStreamChannelFuture after successful reconnection.
     * This provides type-safe channel creation for reliable streaming.
     *
     * @return new TripleStreamChannelFuture, or null if no connection available
     */
    TripleStreamChannelFuture createStreamFuture();
}
