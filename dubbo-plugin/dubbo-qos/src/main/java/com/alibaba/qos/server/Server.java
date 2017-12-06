package com.alibaba.qos.server;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.qos.common.Constants;
import com.alibaba.qos.server.handler.QosProcessHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <pre>
 * 该Server监听指定的端口，能够提供telnet以及HTTP的访问
 *
 * 通过静态方法创建Server
 * 负责启动Server，绑定监听端口
 * 关闭Server
 * </pre>
 *
 * @author weipeng2k 2015年8月27日 上午11:29:27
 */
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final Server INSTANCE = new Server();

    public static final Server getInstance() {
        return INSTANCE;
    }

    private int port = Integer.parseInt(ConfigUtils.getProperty(Constants.QOS_PORT, Constants.DEFAULT_PORT + ""));

    public int getPort() {
        return port;
    }

    private EventLoopGroup boss;

    private EventLoopGroup worker;

    private Server() {
        this.welcome = DubboLogo.dubbo;
    }

    private String welcome;

    private AtomicBoolean hasStarted = new AtomicBoolean();

    /**
     * 设置telnet提供的welcome信息
     *
     * @param welcome
     */
    public void setWelcome(String welcome) {
        this.welcome = welcome;
    }

    /**
     * 启动server，绑定端口
     */
    public void start() throws Throwable {
        if (!hasStarted.compareAndSet(false, true)) {
            return;
        }
        boss = new NioEventLoopGroup(0, new DefaultThreadFactory("qos-boss", true));
        worker = new NioEventLoopGroup(0, new DefaultThreadFactory("qos-worker", true));
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, worker);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new QosProcessHandler(welcome));
            }
        });
        try {
            serverBootstrap.bind(port).sync();
            logger.info("qos-server bind localhost:" + port);
        } catch (Throwable throwable) {
            logger.error("qos-server can not bind localhost:" + port, throwable);
            throw throwable;
        }
    }

    /**
     * 关闭server
     */
    public void stop() {
        logger.info("qos-server stopped.");
        if (boss != null) {
            boss.shutdownGracefully();
        }
        if (worker != null) {
            worker.shutdownGracefully();
        }
    }
}
