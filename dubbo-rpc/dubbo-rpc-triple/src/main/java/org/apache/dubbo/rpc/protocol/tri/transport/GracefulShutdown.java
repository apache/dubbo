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

import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.util.concurrent.Future;

/**
 * HTTP/2 graceful shutdown handler for Triple protocol.
 * <p>
 * This class implements the HTTP/2 graceful shutdown mechanism as defined in RFC 7540.
 * The graceful shutdown process consists of:
 * </p>
 * <ol>
 *   <li><b>First GOAWAY:</b> Send a GOAWAY frame with last-stream-id set to MAX_VALUE,
 *       indicating that no streams will be rejected immediately.</li>
 *   <li><b>PING frame:</b> Send a PING frame to ensure the first GOAWAY has been received.</li>
 *   <li><b>Timeout:</b> Wait for PING ACK or timeout (10 seconds by default).</li>
 *   <li><b>Second GOAWAY:</b> Send a final GOAWAY with the actual last processed stream ID.</li>
 *   <li><b>Close:</b> Close the connection.</li>
 * </ol>
 *
 * <h3>Usage</h3>
 * <p>There are two ways to use this class:</p>
 * <ul>
 *   <li><b>Static method:</b> Use {@link #sendGoAwayFrame(ChannelHandlerContext)} to only send a GOAWAY
 *       frame without closing the connection. This is used for read-only events during graceful shutdown.</li>
 *   <li><b>Instance method:</b> Create an instance and call {@link #gracefulShutdown()} to perform
 *       the full graceful shutdown process including connection close.</li>
 * </ul>
 *
 * @see <a href="https://httpwg.org/specs/rfc7540.html#GOAWAY">RFC 7540 - GOAWAY</a>
 * @since 3.3
 */
public class GracefulShutdown {

    /**
     * Magic value for graceful shutdown PING frame.
     * Used to identify the PING frame sent during graceful shutdown.
     */
    static final long GRACEFUL_SHUTDOWN_PING = 0x97ACEF001L;

    /**
     * Timeout for waiting PING ACK during graceful shutdown (10 seconds).
     */
    private static final long GRACEFUL_SHUTDOWN_PING_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(10);

    /**
     * Message sent in the GOAWAY frame for read-only events.
     */
    private static final String READONLY_GOAWAY_MESSAGE = "server_readonly";

    /**
     * Channel handler context for writing frames.
     */
    private final ChannelHandlerContext ctx;

    /**
     * Original promise to complete when graceful shutdown is done.
     */
    private final ChannelPromise originPromise;

    /**
     * Custom message to include in the final GOAWAY frame.
     */
    private final String goAwayMessage;

    /**
     * Flag indicating whether PING ACK has been received or timeout has occurred.
     */
    private boolean pingAckedOrTimeout;

    /**
     * Scheduled future for the PING timeout.
     */
    private Future<?> pingFuture;

    /**
     * Create a new GracefulShutdown instance.
     *
     * @param ctx           the channel handler context
     * @param goAwayMessage the message to include in the final GOAWAY frame
     * @param originPromise the promise to complete when shutdown is done
     */
    public GracefulShutdown(ChannelHandlerContext ctx, String goAwayMessage, ChannelPromise originPromise) {
        this.ctx = ctx;
        this.goAwayMessage = goAwayMessage;
        this.originPromise = originPromise;
    }

    /**
     * Send GOAWAY frame to notify the client that the server is going to shutdown.
     * This method only sends the GOAWAY frame without closing the connection.
     * <p>
     * The GOAWAY frame with extraStreamIds set to Integer.MAX_VALUE indicates that
     * the server will not accept new streams but existing streams can continue.
     * </p>
     *
     * @param ctx the channel handler context
     */
    public static void sendGoAwayFrame(ChannelHandlerContext ctx) {
        Http2GoAwayFrame goAwayFrame = new DefaultHttp2GoAwayFrame(
                Http2Error.NO_ERROR, ByteBufUtil.writeAscii(ctx.alloc(), READONLY_GOAWAY_MESSAGE));
        goAwayFrame.setExtraStreamIds(Integer.MAX_VALUE);
        ctx.writeAndFlush(goAwayFrame);
    }

    /**
     * Start the full graceful shutdown process.
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     *   <li>Send the first GOAWAY frame with extraStreamIds = MAX_VALUE</li>
     *   <li>Schedule a timeout task (10 seconds)</li>
     *   <li>Send a PING frame with magic value to verify client received GOAWAY</li>
     * </ol>
     * <p>
     * The shutdown continues in {@link #secondGoAwayAndClose(ChannelHandlerContext)} when
     * either PING ACK is received or timeout occurs.
     * </p>
     */
    public void gracefulShutdown() {
        sendGoAwayFrame(ctx);

        pingFuture = ctx.executor()
                .schedule(() -> secondGoAwayAndClose(ctx), GRACEFUL_SHUTDOWN_PING_TIMEOUT_NANOS, TimeUnit.NANOSECONDS);

        Http2PingFrame pingFrame = new DefaultHttp2PingFrame(GRACEFUL_SHUTDOWN_PING, false);
        ctx.writeAndFlush(pingFrame);
    }

    /**
     * Send the second GOAWAY frame and close the connection.
     * <p>
     * This method is called either when PING ACK is received or when the timeout expires.
     * It sends a final GOAWAY frame with the custom message and then closes the connection.
     * </p>
     * <p>
     * This method is idempotent - it will only execute once even if called multiple times.
     * </p>
     *
     * @param ctx the channel handler context
     */
    void secondGoAwayAndClose(ChannelHandlerContext ctx) {
        if (pingAckedOrTimeout) {
            return;
        }
        pingAckedOrTimeout = true;

        pingFuture.cancel(false);

        try {
            Http2GoAwayFrame goAwayFrame = new DefaultHttp2GoAwayFrame(
                    Http2Error.NO_ERROR, ByteBufUtil.writeAscii(this.ctx.alloc(), this.goAwayMessage));
            ChannelFuture future = ctx.writeAndFlush(goAwayFrame, ctx.newPromise());
            if (future.isDone()) {
                ctx.close(originPromise);
            } else {
                future.addListener((ChannelFutureListener) f -> ctx.close(originPromise));
            }
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
    }
}
