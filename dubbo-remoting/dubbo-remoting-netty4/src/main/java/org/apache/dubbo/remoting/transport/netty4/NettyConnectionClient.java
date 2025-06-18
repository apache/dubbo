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
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.transport.netty4.http2.Http2ClientSettingsHandler;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslClientTlsHandler;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslContexts;
import org.apache.dubbo.remoting.utils.UrlUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_CLIENT_CONNECT_TIMEOUT;
import static org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory.socketChannelClass;

public final class NettyConnectionClient extends AbstractNettyConnectionClient {

    private Bootstrap bootstrap;

    private AtomicReference<Promise<Void>> channelInitializedPromiseRef;

    private AtomicReference<Promise<Void>> connectionPrefaceReceivedPromiseRef;

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
        channelInitializedPromiseRef = new AtomicReference<>();
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
                ChannelPipeline pipeline = ch.pipeline();
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

                ChannelHandlerContext http2FrameCodecHandlerCtx = pipeline.context(Http2FrameCodec.class);
                if (http2FrameCodecHandlerCtx == null) {
                    // set connection preface received promise to null.
                    connectionPrefaceReceivedPromiseRef = null;
                } else {
                    // create connection preface received promise if necessary.
                    if (connectionPrefaceReceivedPromiseRef == null) {
                        connectionPrefaceReceivedPromiseRef = new AtomicReference<>();
                    }
                    connectionPrefaceReceivedPromiseRef.compareAndSet(
                            null, new DefaultPromise<>(GlobalEventExecutor.INSTANCE));
                    pipeline.addAfter(
                            http2FrameCodecHandlerCtx.name(),
                            "client-connection-preface-handler",
                            new Http2ClientSettingsHandler(connectionPrefaceReceivedPromiseRef));
                }

                // set null but do not close this client, it will be reconnecting in the future
                ch.closeFuture().addListener(channelFuture -> clearNettyChannel());
                // TODO support Socks5

                // set channel initialized promise to success if necessary.
                Promise<Void> channelInitializedPromise = channelInitializedPromiseRef.get();
                if (channelInitializedPromise != null) {
                    channelInitializedPromise.trySuccess(null);
                }
            }
        });
        this.bootstrap = bootstrap;
    }

    @Override
    protected ChannelFuture performConnect() {
        // ChannelInitializer#initChannel will be invoked by Netty client work thread.
        return bootstrap.connect();
    }

    @Override
    protected void doConnect() throws RemotingException {
        long start = System.currentTimeMillis();
        // re-create channel initialized promise if necessary.
        channelInitializedPromiseRef.compareAndSet(null, new DefaultPromise<>(GlobalEventExecutor.INSTANCE));
        super.doConnect();
        waitConnectionPreface(start);
    }

    /**
     * Wait connection preface
     * <br>
     * Http2 client should set max header list size of http2 encoder based on server connection preface before
     * sending first data frame, otherwise the http2 server might send back GO_AWAY frame and disconnect the connection
     * immediately if the size of client Headers frame is bigger than the MAX_HEADER_LIST_SIZE of server settings.<br>
     * @see <a href="https://httpwg.org/specs/rfc7540.html#ConnectionHeader">HTTP/2 Connection Preface</a><br>
     * In HTTP/2, each endpoint is required to send a connection preface as a final confirmation of the protocol
     * in use and to establish the initial settings for the HTTP/2 connection. The client and server each send a
     * different connection preface. The client connection preface starts with a sequence of 24 octets,
     * which in hex notation is:<br>
     * 0x505249202a20485454502f322e300d0a0d0a534d0d0a0d0a<br>
     * That is, the connection preface starts with the string PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n<br>
     * This sequence MUST be followed by a SETTINGS frame (Section 6.5), which MAY be empty.
     * The server connection preface consists of a potentially empty SETTINGS frame (Section 6.5) that MUST be
     * the first frame the server sends in the HTTP/2 connection.
     *
     * @param start start time of doConnect in milliseconds.
     */
    private void waitConnectionPreface(long start) throws RemotingException {
        // await channel initialization to ensure connection preface received promise had been created when necessary.
        Promise<Void> channelInitializedPromise = channelInitializedPromiseRef.get();
        long retainedTimeout = getConnectTimeout() - System.currentTimeMillis() + start;
        boolean ret = channelInitializedPromise.awaitUninterruptibly(retainedTimeout, TimeUnit.MILLISECONDS);
        // destroy channel initialized promise after used.
        channelInitializedPromiseRef.set(null);
        if (!ret || !channelInitializedPromise.isSuccess()) {
            // 6-2 Client-side channel initialization timeout
            RemotingException remotingException = new RemotingException(
                    this,
                    "client(url: " + getUrl() + ") failed to connect to server " + getConnectAddress()
                            + " client-side channel initialization timeout " + getConnectTimeout() + "ms (elapsed: "
                            + (System.currentTimeMillis() - start) + "ms) from netty client "
                            + NetUtils.getLocalHost() + " using dubbo version " + Version.getVersion());

            logger.error(
                    TRANSPORT_CLIENT_CONNECT_TIMEOUT,
                    "provider crash",
                    "",
                    "Client-side channel initialization timeout",
                    remotingException);

            throw remotingException;
        }

        // await if connection preface received promise is not null.
        if (connectionPrefaceReceivedPromiseRef == null) {
            return;
        }
        Promise<Void> connectionPrefaceReceivedPromise = connectionPrefaceReceivedPromiseRef.get();
        retainedTimeout = getConnectTimeout() - System.currentTimeMillis() + start;
        ret = connectionPrefaceReceivedPromise.awaitUninterruptibly(retainedTimeout, TimeUnit.MILLISECONDS);
        // destroy connection preface received promise after used.
        connectionPrefaceReceivedPromiseRef.set(null);
        if (!ret || !connectionPrefaceReceivedPromise.isSuccess()) {
            // 6-2 Client-side connection preface timeout
            RemotingException remotingException = new RemotingException(
                    this,
                    "client(url: " + getUrl() + ") failed to connect to server " + getConnectAddress()
                            + " client-side connection preface timeout " + getConnectTimeout()
                            + "ms (elapsed: "
                            + (System.currentTimeMillis() - start) + "ms) from netty client "
                            + NetUtils.getLocalHost()
                            + " using dubbo version "
                            + Version.getVersion());

            logger.error(
                    TRANSPORT_CLIENT_CONNECT_TIMEOUT,
                    "provider crash",
                    "",
                    "Client-side connection preface timeout",
                    remotingException);

            throw remotingException;
        }
    }
}
