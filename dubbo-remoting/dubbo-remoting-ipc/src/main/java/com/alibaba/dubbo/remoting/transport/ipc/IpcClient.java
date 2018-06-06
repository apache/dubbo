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
import com.alibaba.dubbo.remoting.transport.netty4.NettyClient;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.io.File;
import java.net.SocketAddress;

/**
 * IpcClient
 * Based on Netty4 and only Linux.
 */
public class IpcClient extends NettyClient {

    public IpcClient(final URL url, final ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected Class<? extends Channel> clientChannelClass() {
        return EpollDomainSocketChannel.class;
    }

    @Override
    protected EventLoopGroup group() {
        return new EpollEventLoopGroup(Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("IpcClientWorker", true));
    }

    @Override
    public SocketAddress getConnectAddress() {
        File f = new File(IpcServer.TMP_FILE);
        if (!f.exists()) {
            throw new RuntimeException("connect failed, connection refused: " + IpcServer.TMP_FILE);
        }
        return new DomainSocketAddress(f);
    }
}
