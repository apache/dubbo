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

/**
 * Provides loosely state management for write message to outbound.
 */
public abstract class OutboundTransportObserver implements TransportObserver {

    protected final TransportState state = new TransportState();
    protected final WriteQueue writeQueue;

    public OutboundTransportObserver(WriteQueue writeQueue) {
        this.writeQueue = writeQueue;
    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream) {
        checkSendMeta(metadata, endStream);
        doOnMetadata(metadata, endStream);
    }

    protected void checkSendMeta(Object metadata, boolean endStream) {
        if (endStream) {
            // trailers-only or metadata + trailers
            if (!state.allowSendEndStream()) {
                throw new IllegalStateException("Metadata endStream already sent to peer, send " + metadata + " failed!");
            }
            state.setMetaSend();
            state.setEndStreamSend();
        } else {
            // metadata
            if (!state.allowSendMeta()) {
                throw new IllegalStateException("Metadata already sent to peer, send " + metadata + " failed!");
            }
            state.setMetaSend();
        }
    }

    @Override
    public void onData(byte[] data, boolean endStream) {
        checkSendData(endStream);
        doOnData(data, endStream);
    }


    protected void checkSendData(boolean endStream) {
        if (!state.allowSendData()) {
            throw new IllegalStateException("data has not sent to peer!");
        }
        if (endStream) {
            state.setEndStreamSend();
        }
    }

    @Override
    public void onError(GrpcStatus status) {
        if (!state.allowSendReset()) {
            throw new IllegalStateException("Duplicated rst!");
        }
        state.setResetSend();
        doOnError(status);
    }

    @Override
    public void onComplete() {
        if (!state.allowSendEndStream()) {
            throw new IllegalStateException("Stream already closed!");
        }
        state.setEndStreamSend();
        doOnComplete();
    }


    protected abstract void doOnMetadata(Metadata metadata, boolean endStream);

    protected abstract void doOnData(byte[] data, boolean endStream);

    protected abstract void doOnError(GrpcStatus status);

    protected abstract void doOnComplete();

}



