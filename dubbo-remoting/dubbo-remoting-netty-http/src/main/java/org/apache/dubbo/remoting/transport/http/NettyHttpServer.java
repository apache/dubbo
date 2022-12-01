package org.apache.dubbo.remoting.transport.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;



public class NettyHttpServer {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NettyHttpServer.class);

    private EventLoopGroup boss;

    private EventLoopGroup worker;

    private final AtomicBoolean started = new AtomicBoolean();


  public void start() throws Exception {
    if (!started.compareAndSet(false, true)) {
      return;
    }
    boss = new NioEventLoopGroup(1, new DefaultThreadFactory("Dubbo-netty-http-server-boss", true));
    worker = new NioEventLoopGroup(1, new DefaultThreadFactory("Dubbo-netty-http-server-worker", true));
    ServerBootstrap serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(boss, worker);
    serverBootstrap.channel(NioServerSocketChannel.class);
    serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
    serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
    serverBootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    serverBootstrap.handler(new ChannelInitializer<ServerSocketChannel>() {
      @Override
      protected void initChannel(ServerSocketChannel ch) throws Exception {
        ChannelPipeline channelPipeline = ch.pipeline();

        channelPipeline.addLast("exceptionOnInitChannelEventHandler", new ChannelInboundHandlerAdapter() {
          @Override
          public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.warn("exception when accept new channel", cause);
            super.exceptionCaught(ctx, cause);
          }
        });
      }
    });
    serverBootstrap.childHandler(new ChannelInitializer<Channel>() {

      @Override
      protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1048576));
        pipeline.addLast(new HttpProcessHandler());
      }
    });

    try {
        int port = 2025;
      serverBootstrap.bind(port).sync(); // TODO  dynamic port
      logger.info("Dubbo-netty-http-server bind port : " + port);
    } catch (Exception exception) {
      logger.error("qos-server can not bind local port: " + 2025, exception);
    }
  }


}
