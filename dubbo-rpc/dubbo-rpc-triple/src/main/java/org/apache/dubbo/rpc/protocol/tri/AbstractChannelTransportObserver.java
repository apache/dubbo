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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Error;

public abstract class AbstractChannelTransportObserver implements TransportObserver {

    protected final TransportState state = new TransportState();

    protected final ChannelHandlerContext ctx;

    public AbstractChannelTransportObserver(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

//    public final TransportState getState() {
//        return state;
//    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream) {
        if (endStream) {
            state.setEndStreamSend();
        } else {
            state.setMetaSend();
        }
        doOnMetadata(metadata, endStream);
    }

    @Override
    public void onData(byte[] data, boolean endStream) {
        if (endStream) {
            state.setEndStreamSend();
        }
        doOnData(data, endStream);
    }

    @Override
    public void onReset(Http2Error http2Error) {
        state.setResetSend();
        doOnReset(http2Error);
    }

    @Override
    public void onComplete() {
        state.setEndStreamSend();
        doOnComplete();
    }


    protected abstract void doOnMetadata(Metadata metadata, boolean endStream);

    protected abstract void doOnData(byte[] data, boolean endStream);

    protected abstract void doOnReset(Http2Error http2Error);

    protected abstract void doOnComplete();



}



