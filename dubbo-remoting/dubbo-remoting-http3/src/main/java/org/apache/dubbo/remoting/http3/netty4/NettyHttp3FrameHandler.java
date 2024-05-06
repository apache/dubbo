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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http3.h3.Http3InputMessageFrame;
import org.apache.dubbo.remoting.http3.h3.Http3MetadataFrame;
import org.apache.dubbo.remoting.http3.h3.Http3TransportListener;

import java.io.InputStream;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;

public class NettyHttp3FrameHandler extends NettyHttp3StreamInboundHandler {
    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(NettyHttp3FrameHandler.class);

    private final Http3TransportListener transportListener;
    private final H2StreamChannel h2StreamChannel;

    public NettyHttp3FrameHandler(H2StreamChannel h2StreamChannel, Http3TransportListener transportListener) {
        this.h2StreamChannel = h2StreamChannel;
        this.transportListener = transportListener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http3MetadataFrame) {
            Http3MetadataFrame metadata = (Http3MetadataFrame) msg;
            if (metadata.headers().containsKey("reset")) { // RESET frame
                int errCode = Integer.parseInt(metadata.headers().get("reset").get(0));
                transportListener.cancelByRemote(errCode);
            } else { // HEADERS frame
                transportListener.onMetadata((Http3MetadataFrame) msg);
            }
        } else if (msg instanceof Http3InputMessageFrame) {
            transportListener.onData((Http3InputMessageFrame) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(PROTOCOL_FAILED_RESPONSE, "", "", "Exception in processing triple message", cause);
        }
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
        if (cause instanceof HttpStatusException) {
            statusCode = ((HttpStatusException) cause).getStatusCode();
        }
        h2StreamChannel.writeResetFrame(statusCode);
    }
}
