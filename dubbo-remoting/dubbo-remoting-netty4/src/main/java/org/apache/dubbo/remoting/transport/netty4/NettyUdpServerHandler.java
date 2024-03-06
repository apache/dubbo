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
import org.apache.dubbo.remoting.api.DatagramProtocolDetector;
import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.api.WireProtocol;

import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

public class NettyUdpServerHandler extends ChannelInboundHandlerAdapter {
    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(NettyUdpServerHandler.class);
    private final URL url;
    private final ChannelHandler handler;
    private final Map<String, WireProtocol> protocols;
    private final Map<String, URL> urlMapper;
    private final Map<String, ChannelHandler> handlerMapper;

    public NettyUdpServerHandler(
            URL url,
            Map<String, WireProtocol> protocols,
            ChannelHandler handler,
            Map<String, URL> urlMapper,
            Map<String, ChannelHandler> handlerMapper) {
        this.url = url;
        this.protocols = protocols;
        this.handler = handler;
        this.urlMapper = urlMapper;
        this.handlerMapper = handlerMapper;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket datagram = (DatagramPacket) msg;
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);

        for (Map.Entry<String, WireProtocol> entry : protocols.entrySet()) {
            String name = entry.getKey();
            WireProtocol protocol = entry.getValue();

            DatagramProtocolDetector detector = protocol.datagramDetector();
            // datagramDetector being null means that the protocol doesn't support udp
            if (null == detector) {
                continue;
            }
            final ProtocolDetector.Result result = detector.detect(datagram);
            switch (result.flag()) {
                case UNRECOGNIZED:
                    continue;
                case RECOGNIZED:
                    ChannelHandler localHandler = this.handlerMapper.getOrDefault(name, handler);
                    URL localURL = this.urlMapper.getOrDefault(name, url);
                    channel.setUrl(localURL);
                    NettyConfigOperator operator = new NettyConfigOperator(channel, localHandler);
                    operator.setDetectResult(result);
                    protocol.configServerProtocolHandler(url, operator);
                    ctx.pipeline().remove(this);
                case NEED_MORE_DATA:
                    return;
                default:
                    return;
            }
        }
    }
}
