package com.alibaba.dubbo.rpc.protocol.redis2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/1/26.
 */
public class Redis2Server {

    private String host = "0.0.0.0";

    private int port = 6381;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Map<String, Object> services = new ConcurrentHashMap<>();

    public Redis2Server(String host, int port) {
        this.host = host;
        this.port = port;
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
    }

    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(64, 1024, 65536))
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                .childOption(ChannelOption.SO_SNDBUF, 1024)
                .childOption(ChannelOption.SO_RCVBUF, 1024)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch)
                            throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new RedisCommandEncoder());
                        p.addLast(new RedisCommandDecoder());
                    }
                });
        ChannelFuture channelFuture = bootstrap.bind(this.host, this.port);
        channelFuture.syncUninterruptibly();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public void addService(Object service) {
        services.put(service.getClass().getName(), service);
    }

    public void removeService(Object object) {
        services.remove(object.getClass().getName());
    }


}
