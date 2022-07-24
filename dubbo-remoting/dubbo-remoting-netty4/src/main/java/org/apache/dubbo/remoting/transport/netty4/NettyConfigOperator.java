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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.api.pu.ChannelHandlerPretender;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.remoting.api.pu.DefaultCodec;

import java.net.InetSocketAddress;
import java.util.List;

public class NettyConfigOperator implements ChannelOperator {

    public NettyConfigOperator(NettyChannel channel, ChannelHandler handler) {
        this.channel = channel;
        this.handler = handler;
    }

    @Override
    public void configChannelHandler(List<ChannelHandler> handlerList) {
        if(channel instanceof NettyChannel) {
            URL url = channel.getUrl();
            Codec2 codec2 = url.getOrDefaultFrameworkModel().getExtensionLoader(Codec2.class).
                getExtension(url.getProtocol());
            if (!(codec2 instanceof DefaultCodec)){
                NettyCodecAdapter codec = new NettyCodecAdapter(codec2, channel.getUrl(), handler);
                ((NettyChannel) channel).getNioChannel().pipeline().addLast(
                    codec.getDecoder()
                ).addLast(
                    codec.getEncoder()
                );
            }

            for (ChannelHandler handler: handlerList) {
                if (handler instanceof ChannelHandlerPretender) {
                    Object realHandler = ((ChannelHandlerPretender) handler).getRealHandler();
                    if(realHandler instanceof io.netty.channel.ChannelHandler) {
                        ((NettyChannel) channel).getNioChannel().pipeline().addLast(
                            (io.netty.channel.ChannelHandler) realHandler
                        );
                    }
                }
            }

            // triple的codec和channel handler都是default的，不进行任何操作(装饰了一些transporter层的功能),
            // todo 这里需要区分客户端和服务端
            if( isClientSide(channel)){
                //todo 客户端的配置操作
            }else {
                NettyServerHandler sh = new NettyServerHandler(channel.getUrl(), handler);
                ((NettyChannel) channel).getNioChannel().pipeline().addLast(
                    sh
                );
            }
        }
    }

    private boolean isClientSide(Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        URL url = channel.getUrl();
        return url.getPort() == address.getPort() &&
            NetUtils.filterLocalHost(channel.getUrl().getIp())
                .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }

    private final Channel channel;
    private ChannelHandler handler;
}
