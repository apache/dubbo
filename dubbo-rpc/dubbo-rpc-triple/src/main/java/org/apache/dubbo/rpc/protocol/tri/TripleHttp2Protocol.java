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
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.remoting.api.Http2WireProtocol;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleClientHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleHttp2FrameServerHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleServerConnectionHandler;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleTailHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.SslContext;

import java.util.List;
import java.util.concurrent.Executor;

import static org.apache.dubbo.common.constants.CommonConstants.HEADER_FILTER_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_REMOTING_SERIALIZATION;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_ENABLE_PUSH_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_HEADER_TABLE_SIZE_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_INITIAL_WINDOW_SIZE_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_MAX_CONCURRENT_STREAMS_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_MAX_FRAME_SIZE_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_MAX_HEADER_LIST_SIZE_KEY;

@Activate
public class TripleHttp2Protocol extends Http2WireProtocol implements ScopeModelAware {
    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;

    @Override
    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void configServerPipeline(URL url, ChannelPipeline pipeline, SslContext sslContext) {
        final List<HeaderFilter> filters = url.getOrDefaultApplicationModel().getExtensionLoader(HeaderFilter.class).getActivateExtension(url, HEADER_FILTER_KEY);
        String defaultSerialization = url.getParameter(SERIALIZATION_KEY, DEFAULT_REMOTING_SERIALIZATION);
        final Configuration config = ConfigurationUtils.getGlobalConfiguration(applicationModel);
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forServer()
                .gracefulShutdownTimeoutMillis(10000)
                .initialSettings(new Http2Settings()
                        .headerTableSize(config.getInt(H2_SETTINGS_HEADER_TABLE_SIZE_KEY, 4096))
                        .maxConcurrentStreams(config.getInt(H2_SETTINGS_MAX_CONCURRENT_STREAMS_KEY, Integer.MAX_VALUE))
                        .initialWindowSize(config.getInt(H2_SETTINGS_INITIAL_WINDOW_SIZE_KEY, 1 << 20))
                        .maxFrameSize(config.getInt(H2_SETTINGS_MAX_FRAME_SIZE_KEY, 2 << 14))
                        .maxHeaderListSize(config.getInt(H2_SETTINGS_MAX_HEADER_LIST_SIZE_KEY, 8192)))
                .frameLogger(SERVER_LOGGER)
                .build();
        final MultipleSerialization serialization = frameworkModel
                .getExtensionLoader(MultipleSerialization.class)
                .getExtension(url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY));
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) {
                final ChannelPipeline p = ch.pipeline();
                p.addLast(new TripleCommandOutBoundHandler());
                p.addLast(new TripleHttp2FrameServerHandler(frameworkModel, lookupExecutor(url), filters, defaultSerialization, new GenericUnpack(serialization, url)));
            }
        });
        pipeline.addLast(codec, new TripleServerConnectionHandler(), handler, new TripleTailHandler());
    }


    private Executor lookupExecutor(URL url) {
        ExecutorRepository executorRepository = url.getOrDefaultApplicationModel()
                .getExtensionLoader(ExecutorRepository.class)
                .getDefaultExtension();
        Executor urlExecutor = executorRepository.getExecutor(url);
        if (urlExecutor == null) {
            urlExecutor = executorRepository.createExecutorIfAbsent(url);
        }
        return urlExecutor;
    }

    @Override
    public void configClientPipeline(URL url, ChannelPipeline pipeline, SslContext sslContext) {

        final Configuration config = ConfigurationUtils.getGlobalConfiguration(applicationModel);
        final Http2FrameCodec codec = Http2FrameCodecBuilder.forClient()
                .gracefulShutdownTimeoutMillis(10000)
                .initialSettings(new Http2Settings()
                        .headerTableSize(config.getInt(H2_SETTINGS_HEADER_TABLE_SIZE_KEY, 4096))
                        .pushEnabled(config.getBoolean(H2_SETTINGS_ENABLE_PUSH_KEY, false))
                        .maxConcurrentStreams(config.getInt(H2_SETTINGS_MAX_CONCURRENT_STREAMS_KEY, Integer.MAX_VALUE))
                        .initialWindowSize(config.getInt(H2_SETTINGS_INITIAL_WINDOW_SIZE_KEY, 1 << 20))
                        .maxFrameSize(config.getInt(H2_SETTINGS_MAX_FRAME_SIZE_KEY, 2 << 14))
                        .maxHeaderListSize(config.getInt(H2_SETTINGS_MAX_HEADER_LIST_SIZE_KEY, 8192)))
                .frameLogger(CLIENT_LOGGER)
                .build();
        final Http2MultiplexHandler handler = new Http2MultiplexHandler(new TripleClientHandler(frameworkModel));
        pipeline.addLast(codec, handler);
    }
}
