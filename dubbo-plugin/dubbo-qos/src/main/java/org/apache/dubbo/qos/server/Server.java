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
package org.apache.dubbo.qos.server;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.server.handler.QosProcessHandler;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_FAILED_START_SERVER;

/**
 * A server serves for both telnet access and http access
 * <ul>
 * <li>static initialize server</li>
 * <li>start server and bind port</li>
 * <li>close server</li>
 * </ul>
 */
public class Server {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(Server.class);

    private String host;

    private int port;

    private boolean acceptForeignIp = true;
    private String acceptForeignIpWhitelist = StringUtils.EMPTY_STRING;

    private EventLoopGroup boss;

    private EventLoopGroup worker;

    private FrameworkModel frameworkModel;

    public Server(FrameworkModel frameworkModel) {
        this.welcome = DubboLogo.DUBBO;
        this.frameworkModel = frameworkModel;
    }

    private String welcome;

    private AtomicBoolean started = new AtomicBoolean();

    /**
     * welcome message
     */
    public void setWelcome(String welcome) {
        this.welcome = welcome;
    }

    public int getPort() {
        return port;
    }

    /**
     * start server, bind port
     */
    public void start() throws Throwable {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        boss = new NioEventLoopGroup(1, new DefaultThreadFactory("qos-boss", true));
        worker = new NioEventLoopGroup(0, new DefaultThreadFactory("qos-worker", true));
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, worker);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new QosProcessHandler(frameworkModel, welcome, acceptForeignIp, acceptForeignIpWhitelist));
            }
        });
        try {
            if (StringUtils.isBlank(host)) {
                serverBootstrap.bind(port).sync();
            } else {
                serverBootstrap.bind(host, port).sync();
            }

            logger.info("qos-server bind localhost:" + port);
        } catch (Throwable throwable) {
            logger.error(QOS_FAILED_START_SERVER, "", "", "qos-server can not bind localhost:" + port, throwable);
            throw throwable;
        }
    }

    /**
     * close server
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isAcceptForeignIp() {
        return acceptForeignIp;
    }

    public void setAcceptForeignIp(boolean acceptForeignIp) {
        this.acceptForeignIp = acceptForeignIp;
    }

    public void setAcceptForeignIpWhitelist(String acceptForeignIpWhitelist) {
        this.acceptForeignIpWhitelist = acceptForeignIpWhitelist;
    }

    public String getWelcome() {
        return welcome;
    }

    public boolean isStarted() {
        return started.get();
    }
}
