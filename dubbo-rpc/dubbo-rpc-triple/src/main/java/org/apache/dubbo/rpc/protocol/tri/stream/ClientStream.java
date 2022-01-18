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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.tri.Compressor;
import org.apache.dubbo.rpc.protocol.tri.GrpcDataDecoder;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TransportObserver;
import org.apache.dubbo.rpc.protocol.tri.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.WriteQueue;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.pack.Pack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.util.Map;

import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;

public class ClientStream extends AbstractStream implements Stream {
    private final Compressor compressor;
    private final DefaultHttp2Headers headers;
    private final long id;
    private final WriteQueue writeQueue;
    private final Pack requestPack;
    private final Pack responsePack;
    public final StreamListener listener;

    public ClientStream(long id,
                        Channel parent,
                        AsciiString scheme,
                        String path,
                        String serviceVersion,
                        String serviceGroup,
                        String application,
                        String authority,
                        String encoding,
                        String acceptEncoding,
                        String timeout,
                        Compressor compressor,
                        Map<String, Object> attachments,
                        Pack requestPack,
                        Pack responsePack,
                        StreamListener listener) {
        this.id = id;
        this.compressor = compressor;
        this.writeQueue = createWriteQueue(parent);
        this.requestPack = requestPack;
        this.responsePack = responsePack;
        this.listener=listener;
        this.headers = new DefaultHttp2Headers(false);
        this.headers.scheme(scheme)
            .authority(authority)
            .method(HttpMethod.POST.asciiName())
            .path(path)
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        setIfNotNull(headers, TripleHeaderEnum.TIMEOUT.getHeader(), timeout);
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_VERSION.getHeader(), serviceVersion);
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_GROUP.getHeader(), serviceGroup);
        setIfNotNull(headers, TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(), application);
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ENCODING.getHeader(), encoding);
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), acceptEncoding);
        if (attachments != null) {
            convertAttachment(headers, attachments);
        }
    }

    private void setIfNotNull(DefaultHttp2Headers headers, CharSequence key, CharSequence value) {
        if (key == null) {
            return;
        }
        headers.set(key, value);
    }

    WriteQueue createWriteQueue(Channel parent) {
        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(parent);
        final Future<Http2StreamChannel> future = streamChannelBootstrap.open().syncUninterruptibly();
        if (!future.isSuccess()) {
            DefaultFuture2.getFuture(id()).cancel();
            return null;
        }
        final Http2StreamChannel channel = future.getNow();
        channel.pipeline()
            .addLast(new TripleCommandOutBoundHandler())
            .addLast(new TripleHttp2ClientResponseHandler())
            .addLast(new GrpcDataDecoder(Integer.MAX_VALUE, true))
            .addLast(new TripleClientInboundHandler());
        DefaultFuture2.addTimeoutListener(id(), channel::close);
        return new WriteQueue(channel);
    }

    public void startCall() {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return;
        }
        final HeaderQueueCommand headerCmd = HeaderQueueCommand.createHeaders(headers);
        this.writeQueue.enqueue(headerCmd, false);
    }

    public void sendMessage(Object message) throws IOException {
        final byte[] data = requestPack.pack(message);

        final byte[] compress = compressor.compress(data);

        final DataQueueCommand dataCmd = DataQueueCommand.createGrpcCommand(compress, false);
        this.writeQueue.enqueue(dataCmd, false);
    }

    public void cancelByLocal(Throwable t){
        RpcContext.getCancellationContext().cancel(t);
    }
    public void cancelByRemote(){
        DefaultFuture2.getFuture(id()).cancel();
    }
    @Override
    public long id() {
        return id;
    }
    public final TransportObserver remoteObserver;

    public class ClientTransportObserver implements TransportObserver{

        private Compressor decompressor;

        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getHeader());
            if (null != messageEncoding) {
                String compressorStr = messageEncoding.toString();
                if (!DEFAULT_COMPRESSOR.equals(compressorStr)) {
                    Compressor compressor = Compressor.getCompressor(url().getOrDefaultFrameworkModel(), compressorStr);
                    if (null == compressor) {
                        throw GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                            .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr))
                            .asException();
                    } else {
                        decompressor=compressor;
                    }
                }
            }

        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {

        }

        @Override
        public void onError(GrpcStatus status) {
            // handle cancel

        }

        @Override
        public void onComplete() {

        }
    }
}