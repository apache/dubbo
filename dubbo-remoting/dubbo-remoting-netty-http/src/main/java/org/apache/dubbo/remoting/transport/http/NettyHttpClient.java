package org.apache.dubbo.remoting.transport.http;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.netty4.NettyClient;
import org.apache.dubbo.remoting.transport.netty4.NettyClientHandler;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslClientTlsHandler;
import org.apache.dubbo.remoting.utils.UrlUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_CONNECT_TIMEOUT;
import static org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory.socketChannelClass;


public class NettyHttpClient extends NettyClient {


    public NettyHttpClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void initBootstrap(NettyClientHandler nettyClientHandler) {


        getBootstrap().group(getEventLoopGroup())
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .channel(socketChannelClass());

        getBootstrap().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(DEFAULT_CONNECT_TIMEOUT, getConnectTimeout()));
        getBootstrap().handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                int heartbeatInterval = UrlUtils.getHeartbeat(getUrl());

                if (getUrl().getParameter(SSL_ENABLED_KEY, false)) {
                    ch.pipeline().addLast("negotiation", new SslClientTlsHandler(getUrl()));
                }

                ch.pipeline()
                    .addLast("decoder", new HttpServerCodec()) // TODO
                    .addLast("encoder", new HttpServerCodec()) // TODO
                    .addLast("aggregator",new HttpObjectAggregator(1048576)) // TODO
                    .addLast("client-idle-handler", new IdleStateHandler(heartbeatInterval, 0, 0, MILLISECONDS))
                    .addLast("handler", new HttpProcessHandler()); // TODO

            }
        });
    }
}
