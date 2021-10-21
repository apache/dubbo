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
package org.apache.dubbo.rpc.protocol.tri;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2PingFrame;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.utils.UrlUtils;

public class KeepAliveManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeepAliveManager.class);

    private final ScheduledExecutorService scheduler;
    private final PingSender pingSender;
    private final int keepAliveTimeMillis;
    private final int keepAliveTimeoutMillis;
    private State state;
    private ScheduledFuture<?> pingFuture;
    private ScheduledFuture<?> shutdownFuture;

    public KeepAliveManager(ScheduledExecutorService scheduler, URL url, ChannelHandlerContext ctx) {
        this(scheduler, url, new DefaultPingSender(ctx));
    }

    public KeepAliveManager(ScheduledExecutorService scheduler,
        URL url, PingSender pingSender) {
        this.scheduler = scheduler;
        this.pingSender = pingSender;
        this.keepAliveTimeMillis = UrlUtils.getHeartbeat(url);
        this.keepAliveTimeoutMillis = UrlUtils.getIdleTimeout(url);
    }

    public void onRead() {
        if (state == State.PING_SCHEDULED) {
            state = State.PING_DELAYED;
        } else if (state == State.PING_SENT) {
            if (Objects.nonNull(shutdownFuture)) {
                shutdownFuture.cancel(true);
                schedulePingTask(true);
            }
        }
    }

    public void onConnectionStart() {
        onConnectionActive();
    }

    public void onConnectionActive() {
        if (state == State.IDLE) {
            schedulePingTask(false);
        } else if (state == State.IDLE_AND_PING_SENT) {
            state = State.PING_SENT;
        }
    }

    public void onConnectionIdle() {
        if (state == State.PING_SENT) {
            state = State.IDLE_AND_PING_SENT;
        } else if (state == State.PING_SCHEDULED || state == State.PING_DELAYED){
            state = State.IDLE;
        }
    }

    public void onConnectionInactive() {
        if (state != State.DISCONNECTED) {
            state = State.DISCONNECTED;
            if (shutdownFuture != null) {
                shutdownFuture.cancel(false);
            }
            if (pingFuture != null) {
                pingFuture.cancel(false);
                pingFuture = null;
            }
        }
    }

    private void schedulePingTask(boolean force) {
        if (Objects.isNull(pingFuture) || force) {
            pingFuture = scheduler.schedule(new PingTask(), keepAliveTimeMillis, TimeUnit.MILLISECONDS);
            state = State.PING_SCHEDULED;
        }
    }

    public interface PingSender {
        void ping();
        void pingTimeout();
    }

    public static class DefaultPingSender implements PingSender {

        private ChannelHandlerContext ctx;

        public DefaultPingSender(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void ping() {
            ctx.writeAndFlush(new DefaultHttp2PingFrame(0xDAD)).addListener(future -> {
                if (!future.isSuccess()) {
                    LOGGER.warn("write ping error", future.cause());
                }
            });
        }

        @Override
        public void pingTimeout() {
            GracefulShutdown gracefulShutdown = new GracefulShutdown(ctx, "keep-alive timeout", ctx.newPromise());
            gracefulShutdown.gracefulShutdown();
        }
    }

    public class PingTask implements Runnable {

        @Override
        public void run() {
            if (state == State.PING_SCHEDULED) {
                pingSender.ping();
                shutdownFuture = scheduler.schedule(new ShutdownTask(), keepAliveTimeoutMillis, TimeUnit.MILLISECONDS);
                state = State.PING_SENT;
            } else if (state == State.PING_DELAYED) {
                pingFuture = scheduler.schedule(new PingTask(), keepAliveTimeMillis, TimeUnit.MILLISECONDS);
            }
        }
    }

    public class ShutdownTask implements Runnable {

        @Override
        public void run() {
            boolean needShutdown = false;
            synchronized (KeepAliveManager.this) {
                if (state != State.DISCONNECTED) {
                    state = State.DISCONNECTED;
                    needShutdown = true;
                }
            }
            if (needShutdown) {
                pingSender.pingTimeout();
            }
        }
    }

    private enum State {
        /*
         * We don't need to do any keepalives. This means the transport has no active rpcs and
         * keepAliveDuringTransportIdle == false.
         */
        IDLE,
        /*
         * We have scheduled a ping to be sent in the future. We may decide to delay it if we receive
         * some data.
         */
        PING_SCHEDULED,
        /*
         * We need to delay the scheduled keepalive ping.
         */
        PING_DELAYED,
        /*
         * The ping has been sent out. Waiting for a ping response.
         */
        PING_SENT,
        /*
         * Transport goes idle after ping has been sent.
         */
        IDLE_AND_PING_SENT,
        /*
         * The transport has been disconnected. We won't do keepalives any more.
         */
        DISCONNECTED,
    }

}
