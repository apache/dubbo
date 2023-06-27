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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.logging.LogLevel;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.AbstractWireProtocol;
import org.apache.dubbo.remoting.api.pu.ChannelHandlerPretender;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.remoting.api.ssl.ContextOperator;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.executor.ExecutorSupport;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleClientHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2FrameServerHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleServerConnectionHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleTailHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.HEADER_FILTER_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_ENABLE_PUSH_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_HEADER_TABLE_SIZE_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_INITIAL_WINDOW_SIZE_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_MAX_CONCURRENT_STREAMS_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_MAX_FRAME_SIZE_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_MAX_HEADER_LIST_SIZE_KEY;

@Activate
public class TripleHttp2Protocol extends AbstractWireProtocol implements ScopeModelAware {

    // 1 MiB
    private static final int MIB_1 = 1 << 20;
    private static final int MIB_8 = 1 << 23;
    private static final int KIB_32 = 1 << 15;
    private static final int DEFAULT_MAX_HEADER_LIST_SIZE = KIB_32;
    private static final int DEFAULT_SETTING_HEADER_LIST_SIZE = 4096;
    private static final int DEFAULT_MAX_FRAME_SIZE = MIB_8;
    private static final int DEFAULT_WINDOW_INIT_SIZE = MIB_8;

    public static final Http2FrameLogger CLIENT_LOGGER = new Http2FrameLogger(LogLevel.DEBUG, "H2_CLIENT");

    public static final Http2FrameLogger SERVER_LOGGER = new Http2FrameLogger(LogLevel.DEBUG, "H2_SERVER");

    private ExtensionLoader<HeaderFilter> filtersLoader;
    private FrameworkModel frameworkModel;

    public TripleHttp2Protocol() {
        super(new Http2ProtocolDetector());
    }

    @Override
    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        this.filtersLoader = frameworkModel.getExtensionLoader(HeaderFilter.class);
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void configServerProtocolHandler(URL url, ChannelOperator operator) {
        Configuration config = ConfigurationUtils.getGlobalConfiguration(url.getOrDefaultApplicationModel());
        final List<HeaderFilter> headFilters;
        if (filtersLoader != null) {
            headFilters = filtersLoader.getActivateExtension(url, HEADER_FILTER_KEY);
        } else {
            headFilters = Collections.emptyList();
        }
        final Http2FrameCodec codec = TripleHttp2FrameCodecBuilder.forServer()
            .customizeConnection((connection) -> connection.remote().flowController(new TriHttp2RemoteFlowController(connection, url.getOrDefaultApplicationModel())))
            .gracefulShutdownTimeoutMillis(10000)
            .initialSettings(new Http2Settings().headerTableSize(
                    config.getInt(H2_SETTINGS_HEADER_TABLE_SIZE_KEY, DEFAULT_SETTING_HEADER_LIST_SIZE))
                .maxConcurrentStreams(
                    config.getInt(H2_SETTINGS_MAX_CONCURRENT_STREAMS_KEY, Integer.MAX_VALUE))
                .initialWindowSize(
                    config.getInt(H2_SETTINGS_INITIAL_WINDOW_SIZE_KEY, DEFAULT_WINDOW_INIT_SIZE))
                .maxFrameSize(config.getInt(H2_SETTINGS_MAX_FRAME_SIZE_KEY, DEFAULT_MAX_FRAME_SIZE))
                .maxHeaderListSize(config.getInt(H2_SETTINGS_MAX_HEADER_LIST_SIZE_KEY,
                    DEFAULT_MAX_HEADER_LIST_SIZE)))
            .frameLogger(SERVER_LOGGER)
            .build();
        ExecutorSupport executorSupport = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel()).getExecutorSupport(url);
        codec.connection().local().flowController().frameWriter(codec.encoder().frameWriter());
        TripleWriteQueue writeQueue = new TripleWriteQueue();
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(
            new ChannelInitializer<Http2StreamChannel>() {
                @Override
                protected void initChannel(Http2StreamChannel ch) {
                    final ChannelPipeline p = ch.pipeline();
                    p.addLast(new TripleCommandOutBoundHandler());
                    p.addLast(new TripleHttp2FrameServerHandler(frameworkModel, executorSupport,
                        headFilters, ch, writeQueue));
                }
            });
        List<ChannelHandler> handlers = new ArrayList<>();
        handlers.add(new ChannelHandlerPretender(codec));
        handlers.add(new ChannelHandlerPretender(new FlushConsolidationHandler(64, true)));
        handlers.add(new ChannelHandlerPretender(new TripleServerConnectionHandler()));
        handlers.add(new ChannelHandlerPretender(handler));
        handlers.add(new ChannelHandlerPretender(new TripleTailHandler()));
        operator.configChannelHandler(handlers);


    }

    @Override
    public void configClientPipeline(URL url, ChannelOperator operator, ContextOperator contextOperator) {
        Configuration config = ConfigurationUtils.getGlobalConfiguration(url.getOrDefaultApplicationModel());
        final Http2FrameCodec codec = TripleHttp2FrameCodecBuilder.forClient()
            .customizeConnection((connection) -> connection.remote().flowController(new TriHttp2RemoteFlowController(connection, url.getOrDefaultApplicationModel())))
            .gracefulShutdownTimeoutMillis(10000)
            .initialSettings(new Http2Settings().headerTableSize(
                    config.getInt(H2_SETTINGS_HEADER_TABLE_SIZE_KEY, DEFAULT_SETTING_HEADER_LIST_SIZE))
                .pushEnabled(config.getBoolean(H2_SETTINGS_ENABLE_PUSH_KEY, false))
                .maxConcurrentStreams(
                    config.getInt(H2_SETTINGS_MAX_CONCURRENT_STREAMS_KEY, Integer.MAX_VALUE))
                .initialWindowSize(
                    config.getInt(H2_SETTINGS_INITIAL_WINDOW_SIZE_KEY, DEFAULT_WINDOW_INIT_SIZE))
                .maxFrameSize(config.getInt(H2_SETTINGS_MAX_FRAME_SIZE_KEY, DEFAULT_MAX_FRAME_SIZE))
                .maxHeaderListSize(config.getInt(H2_SETTINGS_MAX_HEADER_LIST_SIZE_KEY,
                    DEFAULT_MAX_HEADER_LIST_SIZE)))
            .frameLogger(CLIENT_LOGGER)
            .build();
        codec.connection().local().flowController().frameWriter(codec.encoder().frameWriter());
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(
            new TripleClientHandler(frameworkModel));
        List<ChannelHandler> handlers = new ArrayList<>();
        handlers.add(new ChannelHandlerPretender(codec));
        handlers.add(new ChannelHandlerPretender(handler));
        handlers.add(new ChannelHandlerPretender(new TripleTailHandler()));
        operator.configChannelHandler(handlers);
    }
}
