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

    private final TripleProtocol tripleProtocol;

    public TripleGracefulShutdown(TripleProtocol tripleProtocol) {
        this.tripleProtocol = tripleProtocol;
    }

    @Override
    protected Collection<ProtocolServer> getServers() {
        return tripleProtocol.getServers();
    }

    @Override
    public void readonly() {
        fireChannelEvent(ReadOnlyEvent.INSTANCE);
    }

    @Override
    public void writeable() {
        // Triple protocol (HTTP/2) doesn't support writeable event
        // because GOAWAY is a one-way notification that cannot be reversed.
        // Once a GOAWAY frame is sent, the connection is in graceful shutdown mode.
    }
}
