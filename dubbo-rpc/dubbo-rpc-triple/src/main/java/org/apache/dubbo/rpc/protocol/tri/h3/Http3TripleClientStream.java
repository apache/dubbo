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
package org.apache.dubbo.rpc.protocol.tri.h3;

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.command.Http3CreateStreamQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.stream.AbstractTripleClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.TripleStreamChannelFuture;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleGoAwayHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleTailHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.concurrent.Executor;

import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http3.Http3RequestStreamInitializer;
import io.netty.handler.codec.quic.QuicStreamChannel;

public final class Http3TripleClientStream extends AbstractTripleClientStream {

    public Http3TripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            Channel parent,
            ClientStream.Listener listener,
            TripleWriteQueue writeQueue) {
        super(frameworkModel, executor, writeQueue, listener, parent);
    }

    /**
     * For test only
     */
    public Http3TripleClientStream(
            FrameworkModel frameworkModel,
            Executor executor,
            TripleWriteQueue writeQueue,
            ClientStream.Listener listener,
            Http2StreamChannel http2StreamChannel) {
        super(frameworkModel, executor, writeQueue, listener, http2StreamChannel);
    }

    @Override
    protected TripleStreamChannelFuture initStreamChannel0(Channel parent) {
        Http3RequestStreamInitializer initializer = new Http3RequestStreamInitializer() {
            @Override
            protected void initRequestStream(QuicStreamChannel ch) {
                ch.pipeline()
                        .addLast(Http3ClientFrameCodec.INSTANCE)
                        .addLast(new TripleCommandOutBoundHandler())
                        .addLast(new TripleHttp2ClientResponseHandler(createTransportListener()))
                        .addLast(new TripleGoAwayHandler())
                        .addLast(new TripleTailHandler());
            }
        };
        TripleStreamChannelFuture future = new TripleStreamChannelFuture(parent);
        writeQueue.enqueue(Http3CreateStreamQueueCommand.create(initializer, future));
        return future;
    }

    @Override
    protected void consumeBytes(int numBytes) {
        // HTTP/3 flow control is handled differently by QUIC layer
        // No explicit flow control needed here
    }
}
