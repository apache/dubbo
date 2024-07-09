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
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslClientTlsHandler;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslContexts;
import org.apache.dubbo.remoting.utils.UrlUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory.socketChannelClass;

public final class NettyConnectionClient extends AbstractNettyConnectionClient {

    private Bootstrap bootstrap;

    public NettyConnectionClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void initConnectionClient() {
        protocol = getUrl().getOrDefaultFrameworkModel()
                .getExtensionLoader(WireProtocol.class)
                .getExtension(getUrl().getProtocol());
        super.initConnectionClient();
    }

    protected void initBootstrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(NettyEventLoopFactory.NIO_EVENT_LOOP_GROUP.get())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .remoteAddress(getConnectAddress())
                .channel(socketChannelClass());

        NettyConnectionHandler connectionHandler = new NettyConnectionHandler(this);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeout());
        SslContext sslContext = SslContexts.buildClientSslContext(getUrl());
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                NettyChannel nettyChannel = NettyChannel.getOrAddChannel(ch, getUrl(), getChannelHandler());
                final ChannelPipeline pipeline = ch.pipeline();
                NettySslContextOperator nettySslContextOperator = new NettySslContextOperator();

                if (sslContext != null) {
                    pipeline.addLast("negotiation", new SslClientTlsHandler(sslContext));
                }

                //                pipeline.addLast("logging", new LoggingHandler(LogLevel.INFO)); //for debug

                int heartbeat = UrlUtils.getHeartbeat(getUrl());
                pipeline.addLast("client-idle-handler", new IdleStateHandler(heartbeat, 0, 0, MILLISECONDS));

                pipeline.addLast(Constants.CONNECTION_HANDLER_NAME, connectionHandler);

                NettyConfigOperator operator = new NettyConfigOperator(nettyChannel, getChannelHandler());
                protocol.configClientPipeline(getUrl(), operator, nettySslContextOperator);
                // set null but do not close this client, it will be reconnecting in the future
                ch.closeFuture().addListener(channelFuture -> clearNettyChannel());
                // TODO support Socks5
            }
        });
        this.bootstrap = bootstrap;
    }

    @Override
    protected ChannelFuture performConnect() {
        return bootstrap.connect();
    }
}
