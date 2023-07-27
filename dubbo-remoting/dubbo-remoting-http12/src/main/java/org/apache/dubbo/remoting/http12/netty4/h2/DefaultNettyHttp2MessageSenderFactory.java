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
package org.apache.dubbo.remoting.http12.netty4.h2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.http12.HttpChannelHolder;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMessageSender;
import org.apache.dubbo.remoting.http12.command.DataQueueCommand;
import org.apache.dubbo.remoting.http12.command.HeaderQueueCommand;
import org.apache.dubbo.remoting.http12.exception.UnsupportedMediaTypeException;
import org.apache.dubbo.remoting.http12.h2.Http2MessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2MessageSenderFactory;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DefaultNettyHttp2MessageSenderFactory implements Http2MessageSenderFactory {

    private final AbstractConnectionClient connectionClient;

    private final FrameworkModel frameworkModel;

    public DefaultNettyHttp2MessageSenderFactory(FrameworkModel frameworkModel,
                                                 AbstractConnectionClient connectionClient) {
        this.frameworkModel = frameworkModel;
        this.connectionClient = connectionClient;
    }

    @Override
    public HttpMessageSender start(Http2TransportListener responseListener) {
        Channel channel = (Channel) connectionClient.getChannel(true);
        SerializingHttpMessageSender serializingHttpMessageSender = new SerializingHttpMessageSender(channel);
        serializingHttpMessageSender.setResponseListener(responseListener);
        serializingHttpMessageSender.setCodecs(new ArrayList<>(frameworkModel.getExtensionLoader(HttpMessageCodec.class).getSupportedExtensionInstances()));
        return serializingHttpMessageSender;
    }

    private static class SerializingHttpMessageSender implements HttpMessageSender {

        private final Channel channel;

        private final SerializingExecutor executor;

        private Http2TransportListener responseListener;

        private HttpChannelHolder httpChannelHolder;

        private List<HttpMessageCodec> codecs;

        private HttpMessageCodec codec;

        private String contentType;

        private SerializingHttpMessageSender(Channel channel) {
            this.channel = channel;
            this.executor = new SerializingExecutor(channel.eventLoop());
        }

        public void setResponseListener(Http2TransportListener responseListener) {
            this.responseListener = responseListener;
        }

        public void setCodecs(List<HttpMessageCodec> codecs) {
            this.codecs = codecs;
        }

        @Override
        public CompletableFuture<Void> sendHeader(HttpHeaders httpHeaders) {
            if (this.httpChannelHolder == null) {
                CreateStreamQueueCommand createStreamQueueCommand = createHttpChannelHolder();
                this.httpChannelHolder = createStreamQueueCommand;
                this.executor.execute(createStreamQueueCommand);
            }
            this.contentType = httpHeaders.getFirst(HttpHeaderNames.CONTENT_TYPE.getName());
            HeaderQueueCommand cmd = new HeaderQueueCommand(new Http2MetadataFrame(httpHeaders));
            cmd.setHttpChannel(httpChannelHolder);
            executor.execute(cmd);
            return cmd;
        }


        @Override
        public CompletableFuture<Void> sendMessage(Object message) {
            if (codec == null) {
                this.codec = determineCodec(contentType, this.codecs);
            }
            InputStream inputStream;
            if (message == null) {
                inputStream = new ByteArrayInputStream(new byte[0]);
            } else {
                ByteBuf buffer = channel.alloc().buffer();
                OutputStream outputStream = new ByteBufOutputStream(buffer);
                try {
                    this.codec.encode(outputStream, message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                inputStream = new ByteBufInputStream(buffer, true);
            }
            Http2MessageFrame http2MessageFrame = new Http2MessageFrame(inputStream);
            DataQueueCommand cmd = new DataQueueCommand(http2MessageFrame);
            cmd.setHttpChannel(httpChannelHolder);
            executor.execute(cmd);
            return cmd;
        }

        @Override
        public CompletableFuture<Void> complete() {
            Http2MessageFrame http2MessageFrame = new Http2MessageFrame(null, true);
            DataQueueCommand cmd = new DataQueueCommand(http2MessageFrame);
            cmd.setHttpChannel(httpChannelHolder);
            executor.execute(cmd);
            return cmd;
        }

        private static HttpMessageCodec determineCodec(String contentType, List<HttpMessageCodec> codecs) throws UnsupportedMediaTypeException {
            for (HttpMessageCodec httpMessageCodec : codecs) {
                if (httpMessageCodec.support(contentType)) {
                    return httpMessageCodec;
                }
            }
            throw new UnsupportedMediaTypeException(contentType);
        }

        private CreateStreamQueueCommand createHttpChannelHolder() {
            Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(channel);
            bootstrap.handler(new ChannelInboundHandlerAdapter() {
                @Override
                public void handlerAdded(ChannelHandlerContext ctx) {
                    ChannelPipeline pipeline = ctx.pipeline();
                    pipeline.addLast(new NettyHttp2FrameCodec());
                    pipeline.addLast(new NettyHttp2FrameHandler(responseListener));
                }
            });
            return CreateStreamQueueCommand.create(bootstrap);
        }
    }
}
