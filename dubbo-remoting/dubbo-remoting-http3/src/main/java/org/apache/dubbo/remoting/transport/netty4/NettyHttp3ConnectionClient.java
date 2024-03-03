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
import org.apache.dubbo.remoting.utils.UrlUtils;

import java.util.concurrent.atomic.AtomicReference;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicChannelBootstrap;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class NettyHttp3ConnectionClient extends AbstractNettyConnectionClient {

    private AtomicReference<io.netty.channel.Channel> datagramChannel;

    private QuicChannelBootstrap bootstrap;

    public NettyHttp3ConnectionClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void initConnectionClient() {
        super.initConnectionClient();
        datagramChannel = new AtomicReference<>();
    }

    @Override
    protected void initBootstrap() throws Exception {
        QuicSslContext context = QuicSslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocols(Http3.supportedApplicationProtocols())
                .build();
        int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
        io.netty.channel.ChannelHandler codec = Helper.configCodec(Http3.newQuicClientCodecBuilder(), getUrl())
                .maxIdleTimeout(idleTimeout, MILLISECONDS)
                .sslContext(context)
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

        int heartbeat = UrlUtils.getHeartbeat(getUrl());
        NettyConnectionHandler connectionHandler = new NettyConnectionHandler(this);
        bootstrap = QuicChannel.newBootstrap(nettyDatagramChannel)
                .handler(new ChannelInitializer<QuicChannel>() {
                    @Override
                    protected void initChannel(QuicChannel ch) {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(heartbeat, 0, 0, MILLISECONDS))
                                .addLast(Constants.CONNECTION_HANDLER_NAME, connectionHandler)
                                .addLast(new Http3ClientConnectionHandler());

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
