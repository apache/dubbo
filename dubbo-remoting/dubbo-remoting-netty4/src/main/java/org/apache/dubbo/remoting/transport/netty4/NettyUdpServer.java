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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.WireProtocol;

import java.net.InetSocketAddress;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class NettyUdpServer {
    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(NettyUdpServer.class);
    private NioEventLoopGroup group;
    private Bootstrap bootstrap;
    private Channel channel;

    public void doOpen(
            int port,
            URL url,
            Map<String, WireProtocol> protocols,
            org.apache.dubbo.remoting.ChannelHandler handler,
            Map<String, URL> urlMapper,
            Map<String, ChannelHandler> handlerMapper) {
        try {
            group = new NioEventLoopGroup(1);
            bootstrap = new Bootstrap();
            channel = bootstrap
                    .group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(new NettyUdpServerHandler(url, protocols, handler, urlMapper, handlerMapper))
                    .bind(new InetSocketAddress(port))
                    .sync()
                    .channel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
