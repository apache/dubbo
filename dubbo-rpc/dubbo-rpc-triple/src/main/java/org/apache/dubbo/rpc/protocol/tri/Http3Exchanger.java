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
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.http12.netty4.HttpWriteQueueHandler;
import org.apache.dubbo.remoting.http3.netty4.NettyHttp3FrameCodec;
import org.apache.dubbo.remoting.http3.netty4.NettyHttp3ProtocolSelectorHandler;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;
import org.apache.dubbo.remoting.transport.netty4.NettyHttp3Server;
import org.apache.dubbo.remoting.utils.UrlUtils;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.protocol.tri.h3.Http3ClientFrameCodec;
import org.apache.dubbo.rpc.protocol.tri.h3.Http3TripleServerConnectionHandler;
import org.apache.dubbo.rpc.protocol.tri.h3.negotiation.Helper;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.quic.QuicStreamChannel;

import static org.apache.dubbo.remoting.http3.netty4.Constants.PIPELINE_CONFIGURATOR_KEY;

public final class Http3Exchanger {

    private static final FluentLogger LOGGER = FluentLogger.of(Http3Exchanger.class);
    private static final boolean HAS_NETTY_HTTP3 = ClassUtils.isPresent("io.netty.incubator.codec.http3.Http3");
    private static final Map<String, RemotingServer> SERVERS = new ConcurrentHashMap<>();
    private static final Map<String, AbstractConnectionClient> CLIENTS = new ConcurrentHashMap<>(16);
    private static final ChannelHandler HANDLER = new ChannelHandlerAdapter();

    private static boolean ENABLED = false;
    private static boolean NEGOTIATION_ENABLED = true;

    private Http3Exchanger() {}

    public static void init(Configuration configuration) {
        ENABLED = configuration.getBoolean(Constants.H3_SETTINGS_HTTP3_ENABLED, false);
        NEGOTIATION_ENABLED = configuration.getBoolean(Constants.H3_SETTINGS_HTTP3_NEGOTIATION, true);

        if (ENABLED && !HAS_NETTY_HTTP3) {
            throw new IllegalStateException("Class for netty http3 support not found");
        }
    }

    public static boolean isEnabled(URL url) {
        return ENABLED || HAS_NETTY_HTTP3 && url.getParameter(Constants.HTTP3_KEY, false);
    }

    public static RemotingServer bind(URL url) {
        if (isEnabled(url)) {
            return SERVERS.computeIfAbsent(url.getAddress(), addr -> {
                try {
                    URL serverUrl = url.putAttribute(PIPELINE_CONFIGURATOR_KEY, configServerPipeline(url));
                    return new NettyHttp3Server(serverUrl, HANDLER);
                } catch (RemotingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return null;
    }

    private static Consumer<ChannelPipeline> configServerPipeline(URL url) {
        NettyHttp3ProtocolSelectorHandler selectorHandler =
                new NettyHttp3ProtocolSelectorHandler(url, ScopeModelUtil.getFrameworkModel(url.getScopeModel()));
        return pipeline -> {
            pipeline.addLast(new Http3ServerConnectionHandler(new ChannelInitializer<QuicStreamChannel>() {
                @Override
                protected void initChannel(QuicStreamChannel ch) {
                    ch.pipeline()
                            .addLast(new HttpWriteQueueHandler())
                            .addLast(new FlushConsolidationHandler(64, true))
                            .addLast(NettyHttp3FrameCodec.INSTANCE)
                            .addLast(selectorHandler);
                }
            }));
            pipeline.addLast(new Http3TripleServerConnectionHandler());
        };
    }

    public static AbstractConnectionClient connect(URL url) {
        return CLIENTS.compute(url.getAddress(), (address, client) -> {
            if (client == null) {
                URL clientUrl = url.putAttribute(PIPELINE_CONFIGURATOR_KEY, configClientPipeline(url));
                AbstractConnectionClient connectionClient = NEGOTIATION_ENABLED
                        ? Helper.createAutoSwitchClient(clientUrl, HANDLER)
                        : Helper.createHttp3Client(clientUrl, HANDLER);
                connectionClient.addCloseListener(() -> CLIENTS.remove(address, connectionClient));
                client = connectionClient;
            } else {
                client.retain();
            }
            return client;
        });
    }

    private static Consumer<ChannelPipeline> configClientPipeline(URL url) {
        int heartbeat = UrlUtils.getHeartbeat(url);
        int closeTimeout = UrlUtils.getCloseTimeout(url);
        return pipeline -> {
            pipeline.addLast(Http3ClientFrameCodec.INSTANCE);
            pipeline.addLast(new IdleStateHandler(heartbeat, 0, 0, TimeUnit.MILLISECONDS));
            pipeline.addLast(new TriplePingPongHandler(closeTimeout));
        };
    }

    public static void close() {
        if (SERVERS.isEmpty()) {
            return;
        }
        ArrayList<RemotingServer> toClose = new ArrayList<>(SERVERS.values());
        SERVERS.clear();
        for (RemotingServer server : toClose) {
            try {
                server.close();
            } catch (Throwable t) {
                LOGGER.error(LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_SERVER, "Close http3 server failed", t);
            }
        }
    }
}
