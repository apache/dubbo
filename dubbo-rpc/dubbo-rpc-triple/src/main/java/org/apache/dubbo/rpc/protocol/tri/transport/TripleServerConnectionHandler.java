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

package org.apache.dubbo.rpc.protocol.tri.transport;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2ChannelDuplexHandler;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;
import static org.apache.dubbo.rpc.protocol.tri.transport.GracefulShutdown.GRACEFUL_SHUTDOWN_PING;

public class TripleServerConnectionHandler extends Http2ChannelDuplexHandler {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(TripleServerConnectionHandler.class);
    // Some exceptions are not very useful and add too much noise to the log
    private static final Set<String> QUIET_EXCEPTIONS = new HashSet<>();
    private static final Set<Class<?>> QUIET_EXCEPTIONS_CLASS = new HashSet<>();

    static {
        QUIET_EXCEPTIONS.add("NativeIoException");
        QUIET_EXCEPTIONS_CLASS.add(IOException.class);
        QUIET_EXCEPTIONS_CLASS.add(SocketException.class);
    }

    private GracefulShutdown gracefulShutdown;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2PingFrame) {
            if (((Http2PingFrame) msg).content() == GRACEFUL_SHUTDOWN_PING) {
                if (gracefulShutdown == null) {
                    // this should never happen
                    logger.warn(PROTOCOL_FAILED_RESPONSE, "", "", "Received GRACEFUL_SHUTDOWN_PING Ack but gracefulShutdown is null");
                } else {
                    gracefulShutdown.secondGoAwayAndClose(ctx);
                }
            }
        } else if (msg instanceof Http2GoAwayFrame) {
            ReferenceCountUtil.release(msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private boolean isQuiteException(Throwable t) {
        if (QUIET_EXCEPTIONS_CLASS.contains(t.getClass())) {
            return true;
        }
        return QUIET_EXCEPTIONS.contains(t.getClass().getSimpleName());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // this may be change in future follow https://github.com/apache/dubbo/pull/8644
        if (isQuiteException(cause)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Channel:%s Error", ctx.channel()), cause);
            }
        } else {
            logger.warn(PROTOCOL_FAILED_RESPONSE, "", "", String.format("Channel:%s Error", ctx.channel()), cause);
        }
        ctx.close();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        if (gracefulShutdown == null) {
            gracefulShutdown = new GracefulShutdown(ctx, "app_requested", promise);
        }
        gracefulShutdown.gracefulShutdown();
    }
}
