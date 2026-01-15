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
package org.apache.dubbo.remoting;

/**
 * Channel event that can be fired to channels.
 * <p>
 * This interface represents a custom event that can be fired to all connected channels
 * through the {@link RemotingServer#fireChannelEvent(ChannelEvent)} method.
 * Different protocols can interpret and handle these events in their own way,
 * providing a generic mechanism for sending custom events across protocols.
 * </p>
 *
 * <h3>Built-in Events</h3>
 * <ul>
 *   <li>{@link org.apache.dubbo.remoting.event.ReadOnlyEvent} - Notifies clients that the server is entering
 *       read-only mode (typically for graceful shutdown)</li>
 *   <li>{@link org.apache.dubbo.remoting.event.WriteableEvent} - Notifies clients that the server is resuming
 *       normal operation (cancelling graceful shutdown)</li>
 * </ul>
 *
 * <h3>Protocol-specific Handling</h3>
 * <table border="1">
 *   <tr><th>Protocol</th><th>ReadOnlyEvent</th><th>WriteableEvent</th></tr>
 *   <tr><td>Dubbo</td><td>Sends READONLY_EVENT request</td><td>Sends WRITEABLE_EVENT request</td></tr>
 *   <tr><td>Triple (HTTP/2)</td><td>Sends GOAWAY frame</td><td>Not supported (GOAWAY is irreversible)</td></tr>
 * </table>
 *
 * @see RemotingServer#fireChannelEvent(ChannelEvent)
 * @see org.apache.dubbo.remoting.event.ReadOnlyEvent
 * @see org.apache.dubbo.remoting.event.WriteableEvent
 * @since 3.3
 */
public interface ChannelEvent {}
