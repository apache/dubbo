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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

/**
 * Handshake, SSl and Protocol
 */
public class SslHandlerInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SslHandlerInitializer.class);

    public static ChannelInboundHandler sslServerHandler(URL url, NettyServerHandler serverHandler) {
        // Decorate if necessary
        return new SslServerTlsHandler(url, serverHandler);
    }

    public static ChannelInboundHandler sslClientHandler(URL url, NettyClientHandler clientHandler) {
        return new SslClientTlsHandler(url, clientHandler);
    }

    public static class SslServerTlsHandler extends ChannelInboundHandlerAdapter {

        private final SslContext sslContext;
        private final NettyServerHandler serverHandler;

        SslServerTlsHandler(URL url, NettyServerHandler serverHandler) {
            this.sslContext = SslContexts.buildServerSslContext(url);
            this.serverHandler = serverHandler;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            super.handlerAdded(ctx);

            SSLEngine sslEngine = sslContext.newEngine(ctx.alloc());
            ctx.pipeline().addFirst(new SslHandler(sslEngine, false));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("TLS negotiation failed when trying to accept new connection.", cause);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof SslHandshakeCompletionEvent) {
                SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
                if (handshakeEvent.isSuccess()) {
                    SSLSession session = ctx.pipeline().get(SslHandler.class).engine().getSession();
                    logger.info("TLS negotiation succeed with session: " + session);
                    serverHandler.handshakeCompleted(new HandshakeCompletionEvent(session, ctx));
                    // Remove after handshake success.
                    ctx.pipeline().remove(this);
                } else {
                    logger.error("TLS negotiation failed when trying to accept new connection.", handshakeEvent.cause());
                    ctx.close();
                }
            }
            super.userEventTriggered(ctx, evt);
        }
    }

    public static class SslClientTlsHandler extends ChannelInboundHandlerAdapter {

        private final SslContext sslContext;
        private final NettyClientHandler clientHandler;

        public SslClientTlsHandler(URL url, NettyClientHandler clientHandler) {
            this.sslContext = SslContexts.buildClientSslContext(url);
            this.clientHandler = clientHandler;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            SSLEngine sslEngine = sslContext.newEngine(ctx.alloc());
            ctx.pipeline().addBefore(ctx.name(), null, new SslHandler(sslEngine, false));
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof SslHandshakeCompletionEvent) {
                SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
                if (handshakeEvent.isSuccess()) {
                    SSLSession session = ctx.pipeline().get(SslHandler.class).engine().getSession();
                    logger.info("TLS negotiation succeed with session: " + session);
                    clientHandler.handshakeCompleted(new HandshakeCompletionEvent(session, ctx));
                    ctx.pipeline().remove(this);
                } else {
                    logger.error("TLS negotiation failed when trying to accept new connection.", handshakeEvent.cause());
                    ctx.fireExceptionCaught(handshakeEvent.cause());
                }
            }
        }
    }

    public static class HandshakeCompletionEvent {
        private final SSLSession sslSession;
        private final ChannelHandlerContext ctx;

        public HandshakeCompletionEvent(SSLSession sslSession, ChannelHandlerContext ctx) {
            this.sslSession = sslSession;
            this.ctx = ctx;
        }

        public SSLSession getSslSession() {
            return sslSession;
        }

        public ChannelHandlerContext getCtx() {
            return ctx;
        }
    }
}
