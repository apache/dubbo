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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.http3.Http3SslContexts;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicChannelBootstrap;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import static org.apache.dubbo.remoting.http3.netty4.Constants.PIPELINE_CONFIGURATOR_KEY;

public final class NettyHttp3ConnectionClient extends AbstractNettyConnectionClient {

    private Consumer<ChannelPipeline> pipelineConfigurator;
    private AtomicReference<io.netty.channel.Channel> datagramChannel;
    private QuicChannelBootstrap bootstrap;

    public NettyHttp3ConnectionClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initConnectionClient() {
        super.initConnectionClient();
        datagramChannel = new AtomicReference<>();
        pipelineConfigurator = (Consumer<ChannelPipeline>) getUrl().getAttribute(PIPELINE_CONFIGURATOR_KEY);
        Objects.requireNonNull(pipelineConfigurator, "pipelineConfigurator should be set");
    }

    @Override
    protected void initBootstrap() throws Exception {
        io.netty.channel.ChannelHandler codec = Http3Helper.configCodec(Http3.newQuicClientCodecBuilder(), getUrl())
                .sslContext(Http3SslContexts.buildClientSslContext(getUrl()))
                .build();
        io.netty.channel.Channel nettyDatagramChannel = new Bootstrap()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeout())
                .group(NettyEventLoopFactory.NIO_EVENT_LOOP_GROUP.get())
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) {
                        ch.pipeline().addLast(codec);
                    }
                })
                .bind(0)
                .sync()
                .channel();
        datagramChannel.set(nettyDatagramChannel);
        nettyDatagramChannel.closeFuture().addListener(channelFuture -> datagramChannel.set(null));

        NettyConnectionHandler connectionHandler = new NettyConnectionHandler(this);
        bootstrap = QuicChannel.newBootstrap(nettyDatagramChannel)
                .handler(new ChannelInitializer<QuicChannel>() {
                    @Override
                    protected void initChannel(QuicChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new Http3ClientConnectionHandler());
                        pipeline.addLast(Constants.CONNECTION_HANDLER_NAME, connectionHandler);
                        pipelineConfigurator.accept(pipeline);
                        ch.closeFuture().addListener(channelFuture -> clearNettyChannel());
                    }
                })
                .remoteAddress(getConnectAddress());
    }

    @Override
    protected ChannelFuture performConnect() {
        Channel channel = getNettyDatagramChannel();
        if (channel == null) {
            return null;
        }
        ChannelPromise promise = channel.newPromise();
        GenericFutureListener<Future<QuicChannel>> listener = f -> {
            if (f.isSuccess()) {
                promise.setSuccess(null);
            } else {
                promise.setFailure(f.cause());
            }
        };
        bootstrap.connect().addListener(listener);
        return promise;
    }

    @Override
    protected void performClose() {
        super.performClose();
        io.netty.channel.Channel current = getNettyDatagramChannel();
        if (current != null) {
            current.close();
        }
        datagramChannel.set(null);
    }

    private io.netty.channel.Channel getNettyDatagramChannel() {
        return datagramChannel.get();
    }
}
