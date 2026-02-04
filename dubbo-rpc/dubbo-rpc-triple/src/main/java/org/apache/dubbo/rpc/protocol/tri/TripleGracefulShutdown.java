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

import org.apache.dubbo.remoting.event.ReadOnlyEvent;
import org.apache.dubbo.rpc.AbstractGracefulShutdown;
import org.apache.dubbo.rpc.ProtocolServer;

import java.util.Collection;

/**
 * Triple protocol graceful shutdown implementation.
 * <p>
 * For Triple protocol (HTTP/2), graceful shutdown sends GOAWAY frames to all connected clients,
 * telling them not to send new requests. Existing streams can continue until completion.
 * </p>
 * <p>
 * Triple protocol does not support writeable event because GOAWAY is a one-way notification
 * that cannot be reversed. Once a GOAWAY frame is sent, the connection is in graceful shutdown mode.
 * </p>
 */
public class TripleGracefulShutdown extends AbstractGracefulShutdown {

    /**
     * Reference to the Triple protocol instance for accessing servers.
     */
    private final TripleProtocol tripleProtocol;

    /**
     * Create a new TripleGracefulShutdown instance.
     *
     * @param tripleProtocol the Triple protocol instance, must not be null
     */
    public TripleGracefulShutdown(TripleProtocol tripleProtocol) {
        this.tripleProtocol = tripleProtocol;
    }

    /**
     * {@inheritDoc}
     *
     * @return all active Triple protocol servers
     */
    @Override
    protected Collection<ProtocolServer> getServers() {
        return tripleProtocol.getServers();
    }

    /**
     * Enter read-only mode by sending GOAWAY frames to all connected clients.
     * <p>
     * For Triple protocol (HTTP/2), this fires a {@link ReadOnlyEvent} which triggers
     * the sending of GOAWAY frames. The GOAWAY frame with NO_ERROR code tells clients
     * that the server will not accept new streams but existing streams can continue.
     * </p>
     */
    @Override
    public void readonly() {
        fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }

    /**
     * Resume normal operation (not supported for Triple protocol).
     * <p>
     * Triple protocol (HTTP/2) doesn't support writeable event because GOAWAY is a one-way
     * notification that cannot be reversed. Once a GOAWAY frame is sent, the connection
     * is in graceful shutdown mode and the only way to resume is to establish a new connection.
     * </p>
     * <p>
     * This method is intentionally empty and does nothing.
     * </p>
     */
    @Override
    public void writeable() {
        // Triple protocol (HTTP/2) doesn't support writeable event
        // because GOAWAY is a one-way notification that cannot be reversed.
        // Once a GOAWAY frame is sent, the connection is in graceful shutdown mode.
        logger.info("writeable() is not supported for Triple protocol (HTTP/2). GOAWAY cannot be reversed.");
    }
}
