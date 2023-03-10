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
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.ssl.CertManager;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.transport.netty4.ssl.SslContexts;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

public class NettyPortUnificationServerHandler extends ByteToMessageDecoder {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(
        NettyPortUnificationServerHandler.class);
    private final URL url;
    private final ChannelHandler handler;
    private final boolean detectSsl;
    private final List<WireProtocol> protocols;
    private final Map<String, Channel> dubboChannels;
    private final Map<String, URL> urlMapper;
    private final Map<String, ChannelHandler> handlerMapper;


    public NettyPortUnificationServerHandler(URL url, boolean detectSsl,
                                             List<WireProtocol> protocols, ChannelHandler handler,
                                             Map<String, Channel> dubboChannels, Map<String, URL> urlMapper, Map<String, ChannelHandler> handlerMapper) {
        this.url = url;
        this.protocols = protocols;
        this.detectSsl = detectSsl;
        this.handler = handler;
        this.dubboChannels = dubboChannels;
        this.urlMapper = urlMapper;
        this.handlerMapper = handlerMapper;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(INTERNAL_ERROR, "unknown error in remoting module", "", "Unexpected exception from downstream before protocol detected.", cause);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        if (channel != null) {
            // this is needed by some test cases
            dubboChannels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), channel);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
            if (handshakeEvent.isSuccess()) {
                SSLSession session = ctx.pipeline().get(SslHandler.class).engine().getSession();
                LOGGER.info("TLS negotiation succeed with session: " + session);
            } else {
                LOGGER.error(INTERNAL_ERROR, "", "", "TLS negotiation failed when trying to accept new connection.", handshakeEvent.cause());
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
        throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        // Will use the first five bytes to detect a protocol.
        // size of telnet command ls is 2 bytes
        if (in.readableBytes() < 2) {
            return;
        }

        CertManager certManager = url.getOrDefaultFrameworkModel().getBeanFactory().getBean(CertManager.class);
        ProviderCert providerConnectionConfig = certManager.getProviderConnectionConfig(url, ctx.channel().remoteAddress());

        if (providerConnectionConfig != null && isSsl(in)) {
            enableSsl(ctx, providerConnectionConfig);
        } else {
            for (final WireProtocol protocol : protocols) {
                in.markReaderIndex();
                ChannelBuffer buf = new NettyBackedChannelBuffer(in);
                final ProtocolDetector.Result result = protocol.detector().detect(buf);
                in.resetReaderIndex();
                switch (result) {
                    case UNRECOGNIZED:
                        continue;
                    case RECOGNIZED:
                        String protocolName = url.getOrDefaultFrameworkModel().getExtensionLoader(WireProtocol.class)
                            .getExtensionName(protocol);
                        ChannelHandler localHandler = this.handlerMapper.getOrDefault(protocolName, handler);
                        URL localURL = this.urlMapper.getOrDefault(protocolName, url);
                        channel.setUrl(localURL);
                        NettyConfigOperator operator = new NettyConfigOperator(channel, localHandler);
                        protocol.configServerProtocolHandler(url, operator);
                        ctx.pipeline().remove(this);
                    case NEED_MORE_DATA:
                        return;
                    default:
                        return;
                }
            }
            byte[] preface = new byte[in.readableBytes()];
            in.readBytes(preface);
            Set<String> supported = url.getApplicationModel()
                .getExtensionLoader(WireProtocol.class)
                .getSupportedExtensions();
            LOGGER.error(INTERNAL_ERROR, "unknown error in remoting module", "", String.format("Can not recognize protocol from downstream=%s . "
                    + "preface=%s protocols=%s", ctx.channel().remoteAddress(),
                Bytes.bytes2hex(preface),
                supported));

            // Unknown protocol; discard everything and close the connection.
            in.clear();
            ctx.close();
        }
    }

    private void enableSsl(ChannelHandlerContext ctx, ProviderCert providerConnectionConfig) {
        ChannelPipeline p = ctx.pipeline();
        SslContext sslContext = SslContexts.buildServerSslContext(providerConnectionConfig);
        p.addLast("ssl", sslContext.newHandler(ctx.alloc()));
        p.addLast("unificationA",
            new NettyPortUnificationServerHandler(url, false, protocols,
                handler, dubboChannels, urlMapper, handlerMapper));
        p.remove(this);
    }

    private boolean isSsl(ByteBuf buf) {
        // at least 5 bytes to determine if data is encrypted
        if (detectSsl && buf.readableBytes() >= 5) {
            return SslHandler.isEncrypted(buf);
        }
        return false;
    }


}
