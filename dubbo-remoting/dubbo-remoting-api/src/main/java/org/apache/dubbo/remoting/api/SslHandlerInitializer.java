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
package org.apache.dubbo.remoting.api;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.util.List;

/**
 * Handshake, SSl and Protocol
 */
public class SslHandlerInitializer {

    private static final Logger logger = LoggerFactory.getLogger(SslHandlerInitializer.class);

    public static ChannelInboundHandler sslServerHandler(URL url, PortUnificationServerHandler portUnificationServerHandler) {
        return sslServerHandler(SslContexts.buildServerSslContext(url), portUnificationServerHandler);
    }

    public static ChannelInboundHandler sslServerHandler(SslContext sslContext, PortUnificationServerHandler portUnificationServerHandler) {
        return new SslServerTlsHandler(sslContext, portUnificationServerHandler);
    }

    public static ChannelInboundHandler sslClientHandler(URL url, ConnectionHandler connectionHandler) {
        return sslClientHandler(SslContexts.buildClientSslContext(url), connectionHandler);
    }

    public static ChannelInboundHandler sslClientHandler(SslContext sslContext, ConnectionHandler connectionHandler) {
        return new SslClientTlsHandler(sslContext, connectionHandler);
    }

    public static class SslServerTlsHandler extends ByteToMessageDecoder {

        private final SslContext sslContext;
        private final PortUnificationServerHandler portUnificationServerHandler;
        private final boolean detectSsl=false;

        SslServerTlsHandler(URL url, PortUnificationServerHandler portUnificationServerHandler) {
            this(SslContexts.buildServerSslContext(url), portUnificationServerHandler);
        }

        SslServerTlsHandler(SslContext sslContext, PortUnificationServerHandler portUnificationServerHandler) {
            this.sslContext = sslContext;
            this.portUnificationServerHandler = portUnificationServerHandler;
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
                    portUnificationServerHandler.handshakeCompleted(new HandshakeCompletionEvent(session, ctx));
                    // Remove after handshake success.
                    ctx.pipeline().remove(this);
                } else {
                    logger.error("TLS negotiation failed when trying to accept new connection.", handshakeEvent.cause());
                    ctx.close();
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            // Will use the first five bytes to detect a protocol.
            if (in.readableBytes() < 5) {
                return;
            }

            if (isSsl(in)) {
                enableSsl(ctx);
            }
        }

        private boolean isSsl(ByteBuf buf) {
            return SslHandler.isEncrypted(buf);
        }

        private void enableSsl(ChannelHandlerContext ctx) {
            ChannelPipeline p = ctx.pipeline();
            SSLEngine sslEngine = sslContext.newEngine(ctx.alloc());
            p.addFirst(new SslHandler(sslEngine, false));
            p.remove(this);
        }
    }



    public static class SslClientTlsHandler extends ChannelInboundHandlerAdapter {

        private final SslContext sslContext;
        private final ConnectionHandler clientHandler;

        public SslClientTlsHandler(URL url, ConnectionHandler clientHandler) {
            this(SslContexts.buildClientSslContext(url), clientHandler);
        }

        public SslClientTlsHandler(SslContext sslContext, ConnectionHandler clientHandler) {
            this.sslContext = sslContext;
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
