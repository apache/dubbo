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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.List;
import java.util.Set;

public class PortUnificationServerHandler extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        PortUnificationServerHandler.class);

    private final SslContext sslCtx;
    private final URL url;
    private final boolean detectSsl;
    private final List<WireProtocol> protocols;
    private final DefaultChannelGroup channels = new DefaultChannelGroup(
        GlobalEventExecutor.INSTANCE);

    public PortUnificationServerHandler(URL url, List<WireProtocol> protocols) {
        this(url, null, false, protocols);
    }

    public PortUnificationServerHandler(URL url, SslContext sslCtx, boolean detectSsl,
        List<WireProtocol> protocols) {
        this.url = url;
        this.sslCtx = sslCtx;
        this.protocols = protocols;
        this.detectSsl = detectSsl;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Unexpected exception from downstream before protocol detected.", cause);
    }

    public DefaultChannelGroup getChannels() {
        return channels;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        channels.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        channels.remove(ctx.channel());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
        throws Exception {
        // Will use the first five bytes to detect a protocol.
        if (in.readableBytes() < 5) {
            return;
        }

        if (isSsl(in)) {
            enableSsl(ctx);
        } else {
            for (final WireProtocol protocol : protocols) {
                in.markReaderIndex();
                final ProtocolDetector.Result result = protocol.detector().detect(ctx, in);
                in.resetReaderIndex();
                switch (result) {
                    case UNRECOGNIZED:
                        continue;
                    case RECOGNIZED:
                        protocol.configServerPipeline(url, ctx.pipeline(), sslCtx);
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
            LOGGER.error(String.format("Can not recognize protocol from downstream=%s . "
                    + "preface=%s protocols=%s", ctx.channel().remoteAddress(),
                Bytes.bytes2hex(preface),
                supported));

            // Unknown protocol; discard everything and close the connection.
            in.clear();
            ctx.close();
        }
    }

    private void enableSsl(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("ssl", sslCtx.newHandler(ctx.alloc()));
        p.addLast("unificationA", new PortUnificationServerHandler(url, sslCtx, false, protocols));
        p.remove(this);
    }

    private boolean isSsl(ByteBuf buf) {
        if (detectSsl) {
            return SslHandler.isEncrypted(buf);
        }
        return false;
    }


}
