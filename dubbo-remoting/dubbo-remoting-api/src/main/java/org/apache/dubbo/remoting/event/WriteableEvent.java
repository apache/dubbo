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
 * Writeable event to resume normal operation after graceful shutdown is cancelled.
 * <p>
 * This event indicates that the server is resuming normal operation and can accept
 * new requests again. It is typically fired when a graceful shutdown is cancelled
 * before completion.
 * </p>
 *
 * <h3>Protocol-specific Behavior</h3>
 * <p>When this event is fired to a channel:</p>
 * <ul>
 *   <li><b>Dubbo protocol:</b> Sends a WRITEABLE_EVENT request to the client.
 *       The client will mark this provider as available again and can use it for new requests.</li>
 *   <li><b>Triple protocol (HTTP/2):</b> <em>Not supported.</em> HTTP/2 GOAWAY frame is a one-way
 *       notification that cannot be reversed. Once a GOAWAY frame is sent, the connection
 *       is in graceful shutdown mode and cannot be resumed.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>This is a singleton class. Use {@link #INSTANCE} to get the instance.</p>
 *
 * @see org.apache.dubbo.remoting.RemotingServer#fireChannelEvent(ChannelEvent)
 * @see ReadOnlyEvent
 * @see org.apache.dubbo.rpc.GracefulShutdown#writeable()
 * @since 3.3
 */
public class WriteableEvent implements ChannelEvent {

    /**
     * The singleton instance of WriteableEvent.
     */
    public static final WriteableEvent INSTANCE = new WriteableEvent();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private WriteableEvent() {}
}
