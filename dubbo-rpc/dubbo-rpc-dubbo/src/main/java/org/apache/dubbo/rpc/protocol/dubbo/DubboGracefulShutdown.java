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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.remoting.event.ReadOnlyEvent;
import org.apache.dubbo.remoting.event.WriteableEvent;
import org.apache.dubbo.rpc.AbstractGracefulShutdown;
import org.apache.dubbo.rpc.ProtocolServer;

import java.util.Collection;

/**
 * Dubbo protocol graceful shutdown implementation.
 * <p>
 * For Dubbo protocol, graceful shutdown sends READONLY_EVENT to all connected clients,
 * telling them that the server is going to shut down and they should switch to other providers.
 * </p>
 * <p>
 * Dubbo protocol also supports WRITEABLE_EVENT to resume normal operation if graceful shutdown
 * is cancelled.
 * </p>
 */
public class DubboGracefulShutdown extends AbstractGracefulShutdown {

    /**
     * Reference to the Dubbo protocol instance for accessing servers.
     */
    private final DubboProtocol dubboProtocol;

    /**
     * Create a new DubboGracefulShutdown instance.
     *
     * @param dubboProtocol the Dubbo protocol instance, must not be null
     */
    public DubboGracefulShutdown(DubboProtocol dubboProtocol) {
        this.dubboProtocol = dubboProtocol;
    }

    /**
     * {@inheritDoc}
     *
     * @return all active Dubbo protocol servers
     */
    @Override
    protected Collection<ProtocolServer> getServers() {
        return dubboProtocol.getServers();
    }

    /**
     * Enter read-only mode by sending READONLY_EVENT to all connected clients.
     * <p>
     * For Dubbo protocol, this fires a {@link ReadOnlyEvent} which triggers the sending
     * of READONLY_EVENT requests to all connected clients. Clients receiving this event
     * will mark this provider as unavailable and prefer other providers for new requests.
     * </p>
     */
    @Override
    public void readonly() {
        fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }

    /**
     * Resume normal operation by sending WRITEABLE_EVENT to all connected clients.
     * <p>
     * For Dubbo protocol, this fires a {@link WriteableEvent} which triggers the sending
     * of WRITEABLE_EVENT requests to all connected clients. Clients receiving this event
     * will mark this provider as available again and can use it for new requests.
     * </p>
     * <p>
     * This is useful when a graceful shutdown is cancelled before completion.
     * </p>
     */
    @Override
    public void writeable() {
        fireChannelEvent(WriteableEvent.INSTANCE);
    }
}
