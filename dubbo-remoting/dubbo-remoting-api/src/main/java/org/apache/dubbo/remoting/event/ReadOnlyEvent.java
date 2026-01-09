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
package org.apache.dubbo.remoting.event;

import org.apache.dubbo.remoting.ChannelEvent;

/**
 * Read-only event for graceful shutdown.
 * <p>
 * This event indicates that the server is entering read-only mode and will not accept
 * new requests. It is typically fired during graceful shutdown to notify connected clients
 * that they should switch to other available providers.
 * </p>
 *
 * <h3>Protocol-specific Behavior</h3>
 * <p>When this event is fired to a channel:</p>
 * <ul>
 *   <li><b>Dubbo protocol:</b> Sends a READONLY_EVENT request to the client.
 *       The client will mark this provider as unavailable and prefer other providers for new requests.</li>
 *   <li><b>Triple protocol (HTTP/2):</b> Sends an HTTP/2 GOAWAY frame with NO_ERROR code.
 *       The GOAWAY frame indicates that the server will not accept new streams (requests)
 *       but existing streams can continue until completion.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>This is a singleton class. Use {@link #INSTANCE} to get the instance.</p>
 *
 * @see org.apache.dubbo.remoting.RemotingServer#fireChannelEvent(ChannelEvent)
 * @see WriteableEvent
 * @see org.apache.dubbo.rpc.GracefulShutdown#readonly()
 * @since 3.3
 */
public class ReadOnlyEvent implements ChannelEvent {

    /**
     * The singleton instance of ReadOnlyEvent.
     */
    public static final ReadOnlyEvent INSTANCE = new ReadOnlyEvent();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private ReadOnlyEvent() {}
}
