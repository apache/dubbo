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
package org.apache.dubbo.rpc.protocol.rest.netty.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.remoting.http.ssl.JdkSSLContextFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.util.List;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

public class SslServerTlsHandler extends ByteToMessageDecoder {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SslServerTlsHandler.class);

    private final URL url;

    private final boolean sslDetected;

    public SslServerTlsHandler(URL url) {
        this.url = url;
        this.sslDetected = false;
    }

    public SslServerTlsHandler(URL url, boolean sslDetected) {
        this.url = url;
        this.sslDetected = sslDetected;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(INTERNAL_ERROR, "unknown error in remoting module", "", "TLS negotiation failed when trying to accept new connection.", cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
            if (handshakeEvent.isSuccess()) {
                SSLSession session = ctx.pipeline().get(SslHandler.class).engine().getSession();
                logger.info("TLS negotiation succeed with: " + session.getPeerHost());
                // Remove after handshake success.
                ctx.pipeline().remove(this);
            } else {
                logger.error(INTERNAL_ERROR, "", "", "TLS negotiation failed when trying to accept new connection.", handshakeEvent.cause());
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        // Will use the first five bytes to detect a protocol.
        if (byteBuf.readableBytes() < 5) {
            return;
        }

        if (sslDetected) {
            return;
        }

        CertManager certManager = url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
        ProviderCert providerConnectionConfig = certManager.getProviderConnectionConfig(url, channelHandlerContext.channel().remoteAddress());

        if (providerConnectionConfig == null) {
            ChannelPipeline p = channelHandlerContext.pipeline();
            p.remove(this);
            return;
        }

        if (isSsl(byteBuf)) {
            enableSsl(channelHandlerContext, buildSslContext(providerConnectionConfig));
            return;
        }

        if (providerConnectionConfig.getAuthPolicy() == AuthPolicy.NONE) {
            ChannelPipeline p = channelHandlerContext.pipeline();
            p.remove(this);
        }

        logger.error(INTERNAL_ERROR, "", "", "TLS negotiation failed when trying to accept new connection.");
        channelHandlerContext.close();
    }

    private Object buildSslContext(ProviderCert providerConnectionConfig) {

        try {
            // first to parse pem file

            if (providerConnectionConfig.isPem()) {
                return SslContexts.buildServerSslContext(providerConnectionConfig);

            } else {
                // build context by jdk
                return new JdkSSLContextFactory().buildSSLContext(providerConnectionConfig);
            }
        } catch (Throwable e) {
            logger.error("", e.getMessage(), "", "Rest SslServerTlsHandler build ssl context failed cause: ", e);
            throw e;
        }
    }

    private boolean isSsl(ByteBuf buf) {
        return SslHandler.isEncrypted(buf);
    }

    private void enableSsl(ChannelHandlerContext ctx, Object sslContext) {

        SslHandler sslHandler = null;
        if (sslContext instanceof SslContext) {
            sslHandler = ((SslContext) sslContext).newHandler(ctx.alloc());
        } else {

            SSLEngine sslEngine = ((SSLContext) sslContext).createSSLEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(true);

            sslHandler = new SslHandler(sslEngine);
        }

        ChannelPipeline p = ctx.pipeline();

        p.addAfter(ctx.name(), null, sslHandler);
        p.addLast("unificationA", new SslServerTlsHandler(url, true));
        p.remove(this);
    }

}
