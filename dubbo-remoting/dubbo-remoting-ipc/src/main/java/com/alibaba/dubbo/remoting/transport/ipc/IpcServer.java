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

package com.alibaba.dubbo.remoting.transport.ipc;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.netty4.NettyCodecAdapter;
import com.alibaba.dubbo.remoting.transport.netty4.NettyServer;
import com.alibaba.dubbo.remoting.transport.netty4.NettyServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.SocketAddress;

/**
 * IpcServer
 * Based on Netty4 and only Linux.
 */
public class IpcServer extends NettyServer {

    public IpcServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected Class<? extends ServerChannel> serverChannelClass() {
        return EpollServerDomainSocketChannel.class;
    }

    @Override
    protected EventLoopGroup bossGroup() {
        return new EpollEventLoopGroup(1, new DefaultThreadFactory("IpcServerBoss", true));
    }

    @Override
    protected EventLoopGroup workerGroup() {
        return new EpollEventLoopGroup(getUrl().getPositiveParameter(Constants.IO_THREADS_KEY,
                Constants.DEFAULT_IO_THREADS), new DefaultThreadFactory("IpcServerWorker", true));
    }

    @Override
    protected ChannelInitializer initializer(final NettyServerHandler nettyServerHandler) {
        return new ChannelInitializer<EpollDomainSocketChannel>() {
            @Override
            protected void initChannel(EpollDomainSocketChannel ch) throws Exception {
                NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), IpcServer.this);
                ch.pipeline()//.addLast("logging",new LoggingHandler(LogLevel.INFO))//for debug
                        .addLast("decoder", adapter.getDecoder())
                        .addLast("encoder", adapter.getEncoder())
                        .addLast("handler", nettyServerHandler);
            }
        };
    }

    @Override
    public SocketAddress getBindAddress() {
        return new DomainSocketAddress("DUBBO-IPC.tmp");
    }
}
