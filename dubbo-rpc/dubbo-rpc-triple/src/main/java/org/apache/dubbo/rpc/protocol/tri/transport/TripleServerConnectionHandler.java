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
import org.apache.dubbo.remoting.event.ReadOnlyEvent;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2ChannelDuplexHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2PingFrame;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_RESPONSE;
import static org.apache.dubbo.rpc.protocol.tri.transport.GracefulShutdown.GRACEFUL_SHUTDOWN_PING;

public class TripleServerConnectionHandler extends Http2ChannelDuplexHandler {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(TripleServerConnectionHandler.class);
    // Some exceptions are not very useful and add too much noise to the log
    private static final Set<String> QUIET_EXCEPTIONS = new HashSet<>();
    private static final Set<Class<?>> QUIET_EXCEPTIONS_CLASS = new HashSet<>();

    static {
        QUIET_EXCEPTIONS.add("NativeIoException");
        QUIET_EXCEPTIONS_CLASS.add(IOException.class);
        QUIET_EXCEPTIONS_CLASS.add(SocketException.class);
    }

    private GracefulShutdown gracefulShutdown;

    private final long maxConnectionAge;

    private final long maxConnectionAgeGrace;

    private ScheduledFuture<?> maxConnectionAgeFuture;

    private ScheduledFuture<?> maxConnectionAgeGraceFuture;

    public TripleServerConnectionHandler() {
        this(-1L, 10_000L);
    }

    public TripleServerConnectionHandler(long maxConnectionAge, long maxConnectionAgeGrace) {
        this.maxConnectionAge = maxConnectionAge;
        this.maxConnectionAgeGrace = maxConnectionAgeGrace;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if (maxConnectionAge > 0) {
            // Apply +/-10% jitter (same as gRPC-java) to avoid mass simultaneous
            // reconnections when many connections are established at the same time.
            long jitteredAge = maxConnectionAge
                    + ThreadLocalRandom.current().nextLong(-maxConnectionAge / 10, maxConnectionAge / 10 + 1);
            maxConnectionAgeFuture =
                    ctx.executor().schedule(() -> onMaxConnectionAgeReached(ctx), jitteredAge, TimeUnit.MILLISECONDS);
        }
    }

    private void onMaxConnectionAgeReached(ChannelHandlerContext ctx) {
        if (!ctx.channel().isActive()) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info(
                    "Connection {} reached max connection age {}ms, sending GOAWAY to trigger client migration",
                    ctx.channel(),
                    maxConnectionAge);
        }
        // Advisory GOAWAY (last-stream-id = MAX_INT): no new streams on this connection,
        // in-flight streams keep running. Clients will migrate to a new connection.
        GracefulShutdown.sendGoAwayFrame(ctx);
        maxConnectionAgeGraceFuture = ctx.executor()
                .schedule(
                        () -> {
                            if (!ctx.channel().isActive()) {
                                return;
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                        "Connection age grace period ({}ms) elapsed, closing connection {}",
                                        maxConnectionAgeGrace,
                                        ctx.channel());
                            }
                            // Close via the channel (not ctx) so the close request traverses
                            // this handler's close() override and performs a graceful shutdown
                            // (final GOAWAY + PING) instead of an abrupt close.
                            ctx.channel().close();
                        },
                        maxConnectionAgeGrace,
                        TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2PingFrame) {
            if (((Http2PingFrame) msg).content() == GRACEFUL_SHUTDOWN_PING) {
                if (gracefulShutdown == null) {
                    // this should never happen
                    logger.warn(
                            PROTOCOL_FAILED_RESPONSE,
                            "",
                            "",
                            "Received GRACEFUL_SHUTDOWN_PING Ack but gracefulShutdown is null");
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

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cancelMaxConnectionAgeTasks();
        super.channelInactive(ctx);
        // reset all active stream on connection close
        forEachActiveStream(stream -> {
            // ignore remote side close
            if (!stream.state().remoteSideOpen()) {
                return true;
            }
            DefaultHttp2ResetFrame resetFrame = new DefaultHttp2ResetFrame(Http2Error.NO_ERROR).stream(stream);
            ctx.fireChannelRead(resetFrame);
            return true;
        });
    }

    private void cancelMaxConnectionAgeTasks() {
        if (maxConnectionAgeFuture != null) {
            maxConnectionAgeFuture.cancel(false);
        }
        if (maxConnectionAgeGraceFuture != null) {
            maxConnectionAgeGraceFuture.cancel(false);
        }
    }

    private boolean isQuiteException(Throwable t) {
        if (QUIET_EXCEPTIONS_CLASS.contains(t.getClass())) {
            return true;
        }
        return QUIET_EXCEPTIONS.contains(t.getClass().getSimpleName());
    }

    /**
     * Handle user events triggered on the channel.
     * <p>
     * This method specifically handles {@link ReadOnlyEvent} for graceful shutdown:
     * </p>
     * <ul>
     *   <li>When a {@link ReadOnlyEvent} is received, send a GOAWAY frame to the client
     *       indicating that the server is entering read-only mode and will not accept new streams.</li>
     *   <li>Other events are delegated to the superclass handler.</li>
     * </ul>
     * <p>
     * Note: Unlike the full graceful shutdown process (triggered by {@code close()}),
     * the ReadOnlyEvent only sends a GOAWAY frame without closing the connection.
     * This allows existing streams to complete while preventing new streams.
     * </p>
     *
     * @param ctx the channel handler context
     * @param evt the user event
     * @throws Exception if an error occurs during event handling
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ReadOnlyEvent) {
            GracefulShutdown.sendGoAwayFrame(ctx);
            if (logger.isDebugEnabled()) {
                logger.debug("Sent GOAWAY frame for graceful shutdown (ReadOnlyEvent) on channel: " + ctx.channel());
            }
            return;
        }
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
