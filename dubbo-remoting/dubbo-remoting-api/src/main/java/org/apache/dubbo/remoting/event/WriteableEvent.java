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
 * When this event is fired to a channel:
 * <ul>
 * <li>Dubbo protocol: sends a WRITEABLE_EVENT request to the client to indicate
 *     that the server is available again and can accept new requests</li>
 * <li>Triple protocol: not supported (GOAWAY cannot be reversed)</li>
 * </ul>
 */
public class WriteableEvent implements ChannelEvent {

    public static final WriteableEvent INSTANCE = new WriteableEvent();

    private WriteableEvent() {}
}
