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

package org.apache.dubbo.rpc.protocol.tri.frame;

import io.netty.buffer.ByteBuf;

public interface Deframer {

    /**
     * Adds the given data to this deframer and attempts delivery to the listener.
     *
     * @param data the raw data read from the remote endpoint. Must be non-null.
     */
    void deframe(ByteBuf data);

    /**
     * Requests up to the given number of messages from the call. No additional messages will be
     * delivered.
     *
     * <p>If {@link #close()} has been called, this method will have no effect.
     *
     * @param numMessages the requested number of messages to be delivered to the listener.
     */
    void request(int numMessages);

    /**
     * Closes this deframer and frees any resources. After this method is called, additional calls
     * will have no effect.
     */
    void close();
}
