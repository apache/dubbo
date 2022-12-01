package org.apache.dubbo.remoting.transport.http;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.netty4.NettyEventLoopFactory;
import org.apache.dubbo.remoting.transport.netty4.NettyServer;
import org.apache.dubbo.remoting.transport.netty4.NettyServerHandler;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslServerTlsHandler;
import org.apache.dubbo.remoting.utils.UrlUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;


public class NettyHttpServer extends NettyServer {


    public NettyHttpServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void initServerBootstrap(NettyServerHandler nettyServerHandler) {

        getServerBootstrap().group(getBossGroup(), getWorkerGroup())
            .channel(NettyEventLoopFactory.serverSocketChannelClass())
            .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
            .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
            .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {

                    ChannelPipeline pipeline = ch.pipeline();
                    int idleTimeout = UrlUtils.getIdleTimeout(getUrl());

                    if (getUrl().getParameter(SSL_ENABLED_KEY, false)) {
                        pipeline.addLast("negotiation", new SslServerTlsHandler(getUrl()));
                    }
                    pipeline.addLast("decoder",new HttpServerCodec()); // TODO
                    pipeline.addLast("encoder",new HttpServerCodec()); // TODO
                    pipeline.addLast("aggregator",new HttpObjectAggregator(1048576)); // TODO
                    pipeline.addLast("netty-http-server-idle-handler",
                        new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS));

                    pipeline.addLast(new HttpProcessHandler()); // TODO
                }
            });
    }
}
