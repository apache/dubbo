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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelEvent;

import java.util.Collection;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

/**
 * Abstract base class for graceful shutdown implementations.
 * <p>
 * This class provides common functionality for graceful shutdown across different protocols.
 * It implements the {@link GracefulShutdown} interface and provides a template for
 * protocol-specific implementations.
 * </p>
 *
 * <h3>Architecture</h3>
 * <p>The graceful shutdown mechanism works as follows:</p>
 * <pre>
 *                     GracefulShutdown
 *                           │
 *                           ▼
 *               AbstractGracefulShutdown
 *                    /              \
 *                   /                \
 *   DubboGracefulShutdown    TripleGracefulShutdown
 *          │                         │
 *          ▼                         ▼
 *    READONLY_EVENT             GOAWAY Frame
 * </pre>
 *
 * <h3>Implementation Guide</h3>
 * <p>Subclasses must implement:</p>
 * <ul>
 *   <li>{@link #getServers()} - Return the collection of protocol servers to notify</li>
 *   <li>{@link #readonly()} - Fire the appropriate event to enter read-only mode</li>
 *   <li>{@link #writeable()} - Fire the appropriate event to resume normal operation (if supported)</li>
 * </ul>
 *
 * @see GracefulShutdown
 * @see org.apache.dubbo.rpc.protocol.dubbo.DubboGracefulShutdown
 * @see org.apache.dubbo.rpc.protocol.tri.TripleGracefulShutdown
 * @since 3.3
 */
public abstract class AbstractGracefulShutdown implements GracefulShutdown {

    /**
     * Logger for graceful shutdown operations.
     */
    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    /**
     * Get the collection of protocol servers that need to be notified during graceful shutdown.
     * <p>
     * Subclasses should return all active servers for the specific protocol.
     * </p>
     *
     * @return collection of protocol servers, never null
     */
    protected abstract Collection<ProtocolServer> getServers();

    /**
     * Fire a channel event to all servers and their connected channels.
     * <p>
     * This method iterates through all protocol servers returned by {@link #getServers()}
     * and fires the given event to each server's remoting server. The event will be
     * propagated to all connected channels.
     * </p>
     * <p>
     * Exceptions during event firing are logged but not propagated, ensuring that
     * failures on individual channels don't affect other channels.
     * </p>
     *
     * @param event the channel event to fire (e.g., {@link org.apache.dubbo.remoting.event.ReadOnlyEvent}
     *              or {@link org.apache.dubbo.remoting.event.WriteableEvent})
     */
    protected void fireChannelEvent(ChannelEvent event) {
        try {
            for (ProtocolServer server : getServers()) {
                server.getRemotingServer().fireChannelEvent(event);
            }
        } catch (Throwable e) {
            logger.warn(
                    TRANSPORT_FAILED_CLOSE_STREAM, "", "", "Failed to fire channel event during graceful shutdown.", e);
        }
    }
}
