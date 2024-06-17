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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.AbstractWireProtocol;
import org.apache.dubbo.remoting.api.pu.ChannelHandlerPretender;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.remoting.api.ssl.ContextOperator;
import org.apache.dubbo.remoting.http12.HttpVersion;
import org.apache.dubbo.remoting.http12.netty4.HttpWriteQueueHandler;
import org.apache.dubbo.remoting.http12.netty4.h1.NettyHttp1Codec;
import org.apache.dubbo.remoting.http12.netty4.h1.NettyHttp1ConnectionHandler;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2FrameCodec;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2ProtocolSelectorHandler;
import org.apache.dubbo.remoting.utils.UrlUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.protocol.tri.h12.TripleProtocolDetector;
import org.apache.dubbo.rpc.protocol.tri.h12.http1.DefaultHttp11ServerTransportListenerFactory;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListenerFactory;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleGoAwayHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleServerConnectionHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleTailHandler;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.util.AsciiString;

@Activate
public class TripleHttp2Protocol extends AbstractWireProtocol implements ScopeModelAware {

    public static final Http2FrameLogger CLIENT_LOGGER = new Http2FrameLogger(LogLevel.DEBUG, "H2_CLIENT");

    public static final Http2FrameLogger SERVER_LOGGER = new Http2FrameLogger(LogLevel.DEBUG, "H2_SERVER");

    private FrameworkModel frameworkModel;

    public TripleHttp2Protocol() {
        super(new TripleProtocolDetector());
    }

    @Override
    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void configClientPipeline(URL url, ChannelOperator operator, ContextOperator contextOperator) {
        TripleConfig tripleConfig = ConfigManager.getProtocol(url).getTriple();
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forClient()
                .gracefulShutdownTimeoutMillis(10000)
                .initialSettings(new Http2Settings()
                        .headerTableSize(tripleConfig.getHeaderTableSize())
                        .pushEnabled(tripleConfig.getEnablePush())
                        .maxConcurrentStreams(tripleConfig.getMaxConcurrentStreams())
                        .initialWindowSize(tripleConfig.getInitialWindowSize())
                        .maxFrameSize(tripleConfig.getMaxFrameSize())
                        .maxHeaderListSize(tripleConfig.getMaxHeaderListSize()))
                .frameLogger(CLIENT_LOGGER)
                .validateHeaders(false)
                .build();
        //        codec.connection().local().flowController().frameWriter(codec.encoder().frameWriter());
        List<ChannelHandler> handlers = new ArrayList<>();
        handlers.add(new ChannelHandlerPretender(codec));
        handlers.add(new ChannelHandlerPretender(new Http2MultiplexHandler(new ChannelDuplexHandler())));
        handlers.add(new ChannelHandlerPretender(new TriplePingPongHandler(UrlUtils.getCloseTimeout(url))));
        handlers.add(new ChannelHandlerPretender(new TripleGoAwayHandler()));
        handlers.add(new ChannelHandlerPretender(new TripleTailHandler()));
        operator.configChannelHandler(handlers);
    }

    @Override
    public void configServerProtocolHandler(URL url, ChannelOperator operator) {
        String httpVersion = operator.detectResult().getAttribute(TripleProtocolDetector.HTTP_VERSION);
        List<ChannelHandler> channelHandlerPretenders = new ArrayList<>();
        try {
            // h1
            if (HttpVersion.HTTP1.getVersion().equals(httpVersion)) {
                configurerHttp1Handlers(url, channelHandlerPretenders);
                return;
            }

            // h2
            if (HttpVersion.HTTP2.getVersion().equals(httpVersion)) {
                configurerHttp2Handlers(url, channelHandlerPretenders);
            }
        } finally {
            operator.configChannelHandler(channelHandlerPretenders);
        }
    }

    private void configurerHttp1Handlers(URL url, List<ChannelHandler> handlers) {
        TripleConfig tripleConfig = ConfigManager.getProtocol(url).getTriple();
        final HttpServerCodec sourceCodec = new HttpServerCodec(new HttpDecoderConfig()
                .setMaxChunkSize(tripleConfig.getMaxChunkSize())
                .setMaxHeaderSize(tripleConfig.getMaxHeaderSize())
                .setMaxInitialLineLength(tripleConfig.getMaxInitialLineLength())
                .setInitialBufferSize(tripleConfig.getInitialBufferSize()));
        handlers.add(new ChannelHandlerPretender(sourceCodec));
        // Triple protocol http1 upgrade support
        handlers.add(new ChannelHandlerPretender(new HttpServerUpgradeHandler(
                sourceCodec,
                protocol -> {
                    if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                        return new Http2ServerUpgradeCodec(
                                buildHttp2FrameCodec(tripleConfig),
                                new HttpWriteQueueHandler(),
                                new FlushConsolidationHandler(64, true),
                                new TripleServerConnectionHandler(),
                                buildHttp2MultiplexHandler(url, tripleConfig),
                                new TripleTailHandler());
                    }
                    // Not upgrade request
                    return null;
                },
                Integer.MAX_VALUE)));
        // If the upgrade was successful, remove the message from the output list
        // so that it's not propagated to the next handler. This request will
        // be propagated as a user event instead.
        handlers.add(new ChannelHandlerPretender(new HttpObjectAggregator(tripleConfig.getMaxBodySize())));
        handlers.add(new ChannelHandlerPretender(new NettyHttp1Codec()));
        handlers.add(new ChannelHandlerPretender(new NettyHttp1ConnectionHandler(
                url, frameworkModel, tripleConfig, DefaultHttp11ServerTransportListenerFactory.INSTANCE)));
    }

    private Http2MultiplexHandler buildHttp2MultiplexHandler(URL url, TripleConfig tripleConfig) {
        return new Http2MultiplexHandler(new ChannelInitializer<Http2StreamChannel>() {
            @Override
            protected void initChannel(Http2StreamChannel ch) {
                final ChannelPipeline p = ch.pipeline();
                p.addLast(new NettyHttp2FrameCodec());
                p.addLast(new NettyHttp2ProtocolSelectorHandler(
                        url, frameworkModel, tripleConfig, GenericHttp2ServerTransportListenerFactory.INSTANCE));
            }
        });
    }

    private void configurerHttp2Handlers(URL url, List<ChannelHandler> handlers) {
        TripleConfig tripleConfig = ConfigManager.getProtocol(url).getTriple();
        final Http2FrameCodec codec = buildHttp2FrameCodec(tripleConfig);
        final Http2MultiplexHandler handler = buildHttp2MultiplexHandler(url, tripleConfig);
        handlers.add(new ChannelHandlerPretender(new HttpWriteQueueHandler()));
        handlers.add(new ChannelHandlerPretender(codec));
        handlers.add(new ChannelHandlerPretender(new FlushConsolidationHandler(64, true)));
        handlers.add(new ChannelHandlerPretender(new TripleServerConnectionHandler()));
        handlers.add(new ChannelHandlerPretender(handler));
        handlers.add(new ChannelHandlerPretender(new TripleTailHandler()));
    }

    private Http2FrameCodec buildHttp2FrameCodec(TripleConfig tripleConfig) {
        return TripleHttp2FrameCodecBuilder.forServer()
                .customizeConnection((connection) ->
                        connection.remote().flowController(new TriHttp2RemoteFlowController(connection, tripleConfig)))
                .gracefulShutdownTimeoutMillis(10000)
                .initialSettings(new Http2Settings()
                        .headerTableSize(tripleConfig.getHeaderTableSize())
                        .maxConcurrentStreams(tripleConfig.getMaxConcurrentStreams())
                        .initialWindowSize(tripleConfig.getInitialWindowSize())
                        .maxFrameSize(tripleConfig.getMaxFrameSize())
                        .maxHeaderListSize(tripleConfig.getMaxHeaderListSize()))
                .frameLogger(SERVER_LOGGER)
                .validateHeaders(false)
                .build();
    }
}
