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
package org.apache.dubbo.remoting.websocket;

import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

public class FinalFragmentStreamingDecoder implements StreamingDecoder<ByteBuf> {

    private boolean inDelivery;

    private boolean pendingDelivery;

    private boolean closed;

    private boolean closing;

    protected final CompositeByteBuf accumulate = Unpooled.compositeBuffer();

    protected FragmentListener<ByteBuf> listener;

    @Override
    public void request(int numMessages) {}

    @Override
    public void decode(ByteBuf buffer) throws DecodeException {
        if (closing || closed) {
            // ignored
            return;
        }
        accumulate.addComponent(true, buffer);
        if (buffer instanceof FinalFragmentByteBuf && ((FinalFragmentByteBuf) buffer).isFinalFragment()) {
            pendingDelivery = true;
            deliver();
        }
    }

    @Override
    public void close() {
        closing = true;
        deliver();
    }

    @Override
    public void onStreamClosed() {
        if (closed) {
            return;
        }
        closed = true;
        accumulate.clear();
        accumulate.release();
    }

    @Override
    public void setFragmentListener(FragmentListener<ByteBuf> listener) {
        this.listener = listener;
    }

    private void deliver() {
        if (inDelivery) {
            return;
        }
        if (closed) {
            return;
        }
        inDelivery = true;
        try {
            if (pendingDelivery) {
                processBody();
                pendingDelivery = false;
            }
            if (closing && !closed) {
                closed = true;
                accumulate.clear();
                accumulate.release();
                listener.onClose();
            }

        } catch (IOException e) {
            throw new DecodeException(e);
        } finally {
            inDelivery = false;
        }
    }

    private void processBody() throws IOException {
        invokeListener(accumulate);
    }

    protected void invokeListener(ByteBuf buffer) {
        this.listener.onFragmentMessage(buffer);
    }
}
