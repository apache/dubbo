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
package org.apache.dubbo.qos.server.handler;


import org.apache.dubbo.qos.common.QosConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

public class LocalHostPermitHandler extends ChannelHandlerAdapter {

    // true means to accept foreign IP
    private  boolean acceptForeignIp;

    public LocalHostPermitHandler(boolean acceptForeignIp) {
        this.acceptForeignIp = acceptForeignIp;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (!acceptForeignIp) {
            if (!((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().isLoopbackAddress()) {
                ByteBuf cb = Unpooled.wrappedBuffer((QosConstants.BR_STR + "Foreign Ip Not Permitted."
                        + QosConstants.BR_STR).getBytes());
                ctx.writeAndFlush(cb).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
