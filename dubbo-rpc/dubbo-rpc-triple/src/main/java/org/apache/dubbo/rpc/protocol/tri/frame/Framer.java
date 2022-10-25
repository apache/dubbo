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

import io.netty.channel.ChannelFuture;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;

public interface Framer {
    /**
     * get the compressor used for compression.
     * @return
     */
    Compressor getCompressor();

    /**
     * Set the compressor used for compression.
     * @param compressor
     */
    void setCompressor(Compressor compressor);

    /**
     * Writes out a payload message.
     * @param cmd contains the message to be written out. It will be completely consumed.
     */
    void writePayload(byte[] cmd);

    /**
     * Closes, with flush.
     * @return
     */
    ChannelFuture close();

    /**
     * Flush the buffered payload.
     */
    void flush();
}
