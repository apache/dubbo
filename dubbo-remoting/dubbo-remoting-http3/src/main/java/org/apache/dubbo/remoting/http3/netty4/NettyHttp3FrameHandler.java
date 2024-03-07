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
package org.apache.dubbo.remoting.http3.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/*import io.netty.handler.codec.http2.Http2ResetFrame;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2FrameHandler;*/

public class NettyHttp3FrameHandler /*extends NettyHttp2FrameHandler*/ {
//    public NettyHttp3FrameHandler(H2StreamChannel h2StreamChannel, Http2TransportListener transportListener) {
//        super(h2StreamChannel, transportListener);
//    }

//    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        // todo: cancel
//        // reset frame
//        /*if (evt instanceof Http2ResetFrame) {
//            long errorCode = ((Http2ResetFrame) evt).errorCode();
//            transportListener.cancelByRemote(errorCode);
//        } else {
//            super.userEventTriggered(ctx, evt);
//        }*/
//    }
}
