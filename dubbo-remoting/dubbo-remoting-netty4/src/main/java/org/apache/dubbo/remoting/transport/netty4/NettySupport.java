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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.dubbo.common.URL;

import java.util.concurrent.ThreadFactory;

/**
 * epoll feature, if epoll not support , nio will used.
 *
 * @since 2.7.0
 */
public class NettySupport extends AbstractSupport {

    public NettySupport(URL url) {
        super(url);
    }

    public EventLoopGroup eventLoopGroup(ThreadFactory threadFactory) {
        return epoll ? epollEventLoopGroup(threadFactory) : nioEventLoopGroup(threadFactory);
    }

    public EventLoopGroup eventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return epoll ? epollEventLoopGroup(nThreads, threadFactory) : nioEventLoopGroup(nThreads, threadFactory);
    }

    public Class serverChannel() {
        return epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    public Class clientChannel() {
        return epoll ? EpollSocketChannel.class : NioSocketChannel.class;
    }
}
