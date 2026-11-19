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
package org.apache.dubbo.remoting.http12.h2;

import org.apache.dubbo.remoting.http12.HttpChannel;

import java.util.concurrent.CompletableFuture;

public interface H2StreamChannel extends HttpChannel {

    CompletableFuture<Void> writeResetFrame(long errorCode);

    @Override
    default Http2OutputMessage newOutputMessage() {
        return this.newOutputMessage(false);
    }

    Http2OutputMessage newOutputMessage(boolean endStream);

    /**
     * Consume bytes from the local flow controller to trigger WINDOW_UPDATE frames.
     * This method should be called when data has been processed and more data can be received.
     *
     * @param numBytes the number of bytes to consume
     * @throws Exception if an error occurs during consumption
     */
    void consumeBytes(int numBytes) throws Exception;

    /**
     * Returns whether the stream is ready for writing. If false, the caller should avoid
     * calling {@link #writeMessage(org.apache.dubbo.remoting.http12.HttpOutputMessage)}
     * to avoid blocking or excessive buffering.
     *
     * <p>This method is used for outgoing flow control / backpressure. When the underlying
     * transport buffer is full, this returns false.
     *
     * @return true if the stream is ready for writing
     */
    boolean isReady();
}
