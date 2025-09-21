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
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.remoting.http12.exception.DecodeException;

public abstract class AbstractLengthFieldStreamingDecoder<T, A> implements StreamingDecoder<T> {

    private long pendingDeliveries;
    private boolean inDelivery = false;
    private boolean closing;
    private boolean closed;
    protected DecodeState state = DecodeState.HEADER;
    protected final A accumulate;
    protected FragmentListener<T> listener;
    protected final int lengthFieldOffset;
    protected final int lengthFieldLength;
    protected final int fixedHeaderLength;
    protected int requiredLength;

    protected AbstractLengthFieldStreamingDecoder(A accumulate, int lengthFieldOffset, int lengthFieldLength) {
        this.accumulate = accumulate;
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.requiredLength = lengthFieldOffset + lengthFieldLength;
        this.fixedHeaderLength = requiredLength;
    }

    @Override
    public final void decode(T input) throws DecodeException {
        if (closing || closed) {
            return;
        }
        addInputToAccumulate(input);
        deliver();
    }

    @Override
    public final void request(int numMessages) {
        pendingDeliveries += numMessages;
        deliver();
    }

    @Override
    public final void close() {
        closing = true;
        deliver();
    }

    @Override
    public final void onStreamClosed() {
        if (closed) {
            return;
        }
        closed = true;
        releaseAccumulate();
    }

    @Override
    public final void setFragmentListener(FragmentListener<T> listener) {
        this.listener = listener;
    }

    private void deliver() {
        if (inDelivery || closed) {
            return;
        }
        inDelivery = true;
        try {
            while (pendingDeliveries > 0 && hasEnoughBytes()) {
                switch (state) {
                    case HEADER:
                        processHeader();
                        break;
                    case PAYLOAD:
                        processBody();
                        pendingDeliveries--;
                        break;
                    default:
                        throw new AssertionError("Invalid state: " + state);
                }
            }
            if (closing && !closed) {
                closed = true;
                releaseAccumulate();
                listener.onClose();
            }
        } catch (Exception e) {
            throw new DecodeException(e);
        } finally {
            inDelivery = false;
        }
    }

    protected abstract void addInputToAccumulate(T input);

    protected abstract void releaseAccumulate();

    protected abstract boolean hasEnoughBytes();

    protected abstract void processHeader() throws Exception;

    protected abstract void processBody() throws Exception;

    public abstract void invokeListener(T rawMessage);

    protected enum DecodeState {
        HEADER,
        PAYLOAD
    }
}
