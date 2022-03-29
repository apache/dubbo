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
package org.apache.dubbo.remoting.api;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.OS_LINUX_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.OS_NAME_KEY;
import static org.apache.dubbo.remoting.Constants.NETTY_EPOLL_ENABLE_KEY;

/**
 * {@link NettyEventLoopFactory}
 */
public class NettyEventLoopFactoryTest {

    @BeforeEach
    public void setUp() {
        System.setProperty(NETTY_EPOLL_ENABLE_KEY, "true");
    }

    @AfterEach
    public void reset() {
        System.clearProperty(NETTY_EPOLL_ENABLE_KEY);
    }

    @Test
    void eventLoopGroup() {
        if (isEpoll()) {
            EventLoopGroup eventLoopGroup = NettyEventLoopFactory.eventLoopGroup(1, "test");
            Assertions.assertTrue(eventLoopGroup instanceof EpollEventLoopGroup);

            Class<? extends SocketChannel> socketChannelClass = NettyEventLoopFactory.socketChannelClass();
            Assertions.assertEquals(socketChannelClass, EpollSocketChannel.class);

            Class<? extends ServerSocketChannel> serverSocketChannelClass = NettyEventLoopFactory.serverSocketChannelClass();
            Assertions.assertEquals(serverSocketChannelClass, EpollServerSocketChannel.class);

        } else {
            EventLoopGroup eventLoopGroup = NettyEventLoopFactory.eventLoopGroup(1, "test");
            Assertions.assertTrue(eventLoopGroup instanceof NioEventLoopGroup);

            Class<? extends SocketChannel> socketChannelClass = NettyEventLoopFactory.socketChannelClass();
            Assertions.assertEquals(socketChannelClass, NioSocketChannel.class);

            Class<? extends ServerSocketChannel> serverSocketChannelClass = NettyEventLoopFactory.serverSocketChannelClass();
            Assertions.assertEquals(serverSocketChannelClass, NioServerSocketChannel.class);
        }
    }

    private boolean isEpoll() {
        String osName = System.getProperty(OS_NAME_KEY);
        return osName.toLowerCase().contains(OS_LINUX_PREFIX) && Epoll.isAvailable();
    }

}
