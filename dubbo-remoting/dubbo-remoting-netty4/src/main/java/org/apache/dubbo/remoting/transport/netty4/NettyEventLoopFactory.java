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

import org.apache.dubbo.common.resource.GlobalResourceInitializer;
import org.apache.dubbo.remoting.Constants;

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
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

import static org.apache.dubbo.common.constants.CommonConstants.OS_LINUX_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.OS_NAME_KEY;
import static org.apache.dubbo.remoting.Constants.NETTY_EPOLL_ENABLE_KEY;

public class NettyEventLoopFactory {
    /**
     * netty client bootstrap
     */
    public static final GlobalResourceInitializer<EventLoopGroup> NIO_EVENT_LOOP_GROUP = new GlobalResourceInitializer<>(() ->
        eventLoopGroup(Constants.DEFAULT_IO_THREADS, "NettyClientWorker"),
        eventLoopGroup -> eventLoopGroup.shutdownGracefully()
    );

    public static EventLoopGroup eventLoopGroup(int threads, String threadFactoryName) {
        ThreadFactory threadFactory = new DefaultThreadFactory(threadFactoryName, true);
        return shouldEpoll() ? new EpollEventLoopGroup(threads, threadFactory) :
                new NioEventLoopGroup(threads, threadFactory);
    }

    public static Class<? extends SocketChannel> socketChannelClass() {
        return shouldEpoll() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    public static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return shouldEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    private static boolean shouldEpoll() {
        if (Boolean.parseBoolean(System.getProperty(NETTY_EPOLL_ENABLE_KEY, "false"))) {
            String osName = System.getProperty(OS_NAME_KEY);
            return osName.toLowerCase().contains(OS_LINUX_PREFIX) && Epoll.isAvailable();
        }

        return false;
    }
}
