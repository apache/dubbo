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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Codec;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.pu.ChannelHandlerPretender;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.remoting.api.pu.DefaultCodec;
import org.apache.dubbo.remoting.transport.codec.CodecAdapter;

import java.util.List;

public class NettyConfigOperator implements ChannelOperator {

    private final Channel channel;
    private ChannelHandler handler;

    public NettyConfigOperator(NettyChannel channel, ChannelHandler handler) {
        this.channel = channel;
        this.handler = handler;
    }

    @Override
    public void configChannelHandler(List<ChannelHandler> handlerList) {
        URL url = channel.getUrl();
        Codec2 codec2;
        String codecName = url.getParameter(Constants.CODEC_KEY);
        if (StringUtils.isEmpty(codecName)) {
            // codec extension name must stay the same with protocol name
            codecName = url.getProtocol();
        }
        if (url.getOrDefaultFrameworkModel().getExtensionLoader(Codec2.class).hasExtension(codecName)) {
            codec2 = url.getOrDefaultFrameworkModel().getExtensionLoader(Codec2.class).getExtension(codecName);
        } else if(url.getOrDefaultFrameworkModel().getExtensionLoader(Codec.class).hasExtension(codecName)){
            codec2 = new CodecAdapter(url.getOrDefaultFrameworkModel().getExtensionLoader(Codec.class)
                .getExtension(codecName));
        }else {
            codec2 = url.getOrDefaultFrameworkModel().getExtensionLoader(Codec2.class).getExtension("default");
        }

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

        // todo distinguish between client and server channel
        if( isClientSide(channel)){
            //todo config client channel handler
        }else {
            NettyServerHandler sh = new NettyServerHandler(channel.getUrl(), handler);
            ((NettyChannel) channel).getNioChannel().pipeline().addLast(
                sh
            );
        }
    }

    private boolean isClientSide(Channel channel) {
        return channel.getUrl().getSide("").equalsIgnoreCase(CommonConstants.CONSUMER);
    }

}
