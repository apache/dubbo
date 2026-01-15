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
package org.apache.dubbo.remoting.transport.netty4.http2;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2SettingsFrame;
import io.netty.util.concurrent.Promise;

public class Http2ClientSettingsHandler extends SimpleChannelInboundHandler<Http2SettingsFrame> {

    private static final Logger logger = LoggerFactory.getLogger(Http2ClientSettingsHandler.class);

    private final AtomicReference<Promise<Void>> connectionPrefaceReceivedPromiseRef;

    public Http2ClientSettingsHandler(AtomicReference<Promise<Void>> connectionPrefaceReceivedPromiseRef) {
        this.connectionPrefaceReceivedPromiseRef = connectionPrefaceReceivedPromiseRef;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2SettingsFrame msg) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Receive server Http2 Settings frame of "
                    + ctx.channel().localAddress() + " -> " + ctx.channel().remoteAddress());
        }
        // connectionPrefaceReceivedPromise will be set null after first used.
        Promise<Void> connectionPrefaceReceivedPromise = connectionPrefaceReceivedPromiseRef.get();
        if (connectionPrefaceReceivedPromise == null) {
            ctx.fireChannelRead(msg);
        } else {
            // Notify the connection preface is received when first inbound http2 settings frame is arrived.
            connectionPrefaceReceivedPromise.trySuccess(null);
            ctx.pipeline().remove(this);
        }
    }
}
