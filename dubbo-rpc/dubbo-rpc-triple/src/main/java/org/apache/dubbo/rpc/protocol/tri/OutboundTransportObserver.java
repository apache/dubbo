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

    @Override
    public void onMetadata(Metadata metadata, boolean endStream) {
        if (!state.allowSendMeta()) {
            throw new IllegalStateException("Metadata already sent to peer, send " + metadata + " failed!");
        }
        if (endStream) {
            state.setEndStreamSend();
        } else {
            state.setMetaSend();
        }
        doOnMetadata(metadata, endStream);
    }

    @Override
    public void onData(byte[] data, boolean endStream) {
        if (!state.allowSendData()) {
            throw new IllegalStateException("Metadata has not sent to peer!");
        }
        if (endStream) {
            state.setEndStreamSend();
        }
        doOnData(data, endStream);
    }

    @Override
    public void onError(GrpcStatus status) {
        if (!state.allowSendReset()) {
            throw new IllegalStateException("Duplicated rst!");
        }
        state.setResetSend();
        doOnCancel(status);
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

    protected abstract void doOnCancel(GrpcStatus status);

    protected abstract void doOnComplete();


    protected int calcCompressFlag(Compressor compressor) {
        if (null == compressor || IdentityCompressor.NONE.equals(compressor)) {
            return 0;
        }
        return 1;
    }

}



