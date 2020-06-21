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
package org.apache.dubbo.rpc.protocol.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.grpc.interceptors.ClientInterceptor;
import org.apache.dubbo.rpc.protocol.grpc.interceptors.GrpcConfigurator;
import org.apache.dubbo.rpc.protocol.grpc.interceptors.ServerInterceptor;
import org.apache.dubbo.rpc.protocol.grpc.interceptors.ServerTransportFilter;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;
import static org.apache.dubbo.remoting.Constants.DISPATCHER_KEY;
import static org.apache.dubbo.rpc.Constants.EXECUTES_KEY;
import static org.apache.dubbo.rpc.protocol.grpc.GrpcConstants.CLIENT_INTERCEPTORS;
import static org.apache.dubbo.rpc.protocol.grpc.GrpcConstants.EXECUTOR;
import static org.apache.dubbo.rpc.protocol.grpc.GrpcConstants.MAX_CONCURRENT_CALLS_PER_CONNECTION;
import static org.apache.dubbo.rpc.protocol.grpc.GrpcConstants.MAX_INBOUND_MESSAGE_SIZE;
import static org.apache.dubbo.rpc.protocol.grpc.GrpcConstants.MAX_INBOUND_METADATA_SIZE;
import static org.apache.dubbo.rpc.protocol.grpc.GrpcConstants.SERVER_INTERCEPTORS;
import static org.apache.dubbo.rpc.protocol.grpc.GrpcConstants.TRANSPORT_FILTERS;

/**
 * Support gRPC configs in a Dubbo specific way.
 */
public class GrpcOptionsUtils {

    static ServerBuilder buildServerBuilder(URL url, NettyServerBuilder builder) {

        int maxInboundMessageSize = url.getParameter(MAX_INBOUND_MESSAGE_SIZE, 0);
        if (maxInboundMessageSize > 0) {
            builder.maxInboundMessageSize(maxInboundMessageSize);
        }

        int maxInboundMetadataSize = url.getParameter(MAX_INBOUND_METADATA_SIZE, 0);
        if (maxInboundMetadataSize > 0) {
            builder.maxInboundMetadataSize(maxInboundMetadataSize);
        }

        if (url.getParameter(SSL_ENABLED_KEY, false)) {
            builder.sslContext(buildServerSslContext(url));
        }

        int flowControlWindow = url.getParameter(MAX_INBOUND_MESSAGE_SIZE, 0);
        if (flowControlWindow > 0) {
            builder.flowControlWindow(flowControlWindow);
        }

        int maxCalls = url.getParameter(MAX_CONCURRENT_CALLS_PER_CONNECTION, url.getParameter(EXECUTES_KEY, 0));
        if (maxCalls > 0) {
            builder.maxConcurrentCallsPerConnection(maxCalls);
        }

        // server interceptors
        List<ServerInterceptor> serverInterceptors = ExtensionLoader.getExtensionLoader(ServerInterceptor.class)
                .getActivateExtension(url, SERVER_INTERCEPTORS, PROVIDER_SIDE);
        for (ServerInterceptor serverInterceptor : serverInterceptors) {
            builder.intercept(serverInterceptor);
        }

        // server filters
        List<ServerTransportFilter> transportFilters = ExtensionLoader.getExtensionLoader(ServerTransportFilter.class)
                .getActivateExtension(url, TRANSPORT_FILTERS, PROVIDER_SIDE);
        for (ServerTransportFilter transportFilter : transportFilters) {
            builder.addTransportFilter(transportFilter.grpcTransportFilter());
        }

        String thread = url.getParameter(EXECUTOR, url.getParameter(DISPATCHER_KEY));
        if ("direct".equals(thread)) {
            builder.directExecutor();
        } else {
            builder.executor(ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url));
        }

        // Give users the chance to customize ServerBuilder
        return getConfigurator()
                .map(configurator -> configurator.configureServerBuilder(builder, url))
                .orElse(builder);
    }

    static ManagedChannel buildManagedChannel(URL url) {

        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(url.getHost(), url.getPort());
        if (url.getParameter(SSL_ENABLED_KEY, false)) {
            builder.sslContext(buildClientSslContext(url));
        } else {
            builder.usePlaintext();
        }

        builder.disableRetry();
//        builder.directExecutor();

        // client interceptors
        List<io.grpc.ClientInterceptor> interceptors = new ArrayList<>(
                ExtensionLoader.getExtensionLoader(ClientInterceptor.class)
                        .getActivateExtension(url, CLIENT_INTERCEPTORS, CONSUMER_SIDE)
        );

        builder.intercept(interceptors);

        return getConfigurator()
                .map(configurator -> configurator.configureChannelBuilder(builder, url))
                .orElse(builder)
                .build();
    }

    static CallOptions buildCallOptions(URL url) {
        // gRPC Deadline starts counting when it's created, so we need to create and add a new Deadline for each RPC call.
//        CallOptions callOptions = CallOptions.DEFAULT
//                .withDeadline(Deadline.after(url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT), TimeUnit.MILLISECONDS));
        CallOptions callOptions = CallOptions.DEFAULT;
        return getConfigurator()
                .map(configurator -> configurator.configureCallOptions(callOptions, url))
                .orElse(callOptions);
    }

    private static SslContext buildServerSslContext(URL url) {
        ConfigManager globalConfigManager = ApplicationModel.getConfigManager();
        SslConfig sslConfig = globalConfigManager.getSsl().orElseThrow(() -> new IllegalStateException("Ssl enabled, but no ssl cert information provided!"));

        SslContextBuilder sslClientContextBuilder = null;
        try {
            String password = sslConfig.getServerKeyPassword();
            if (password != null) {
                sslClientContextBuilder = GrpcSslContexts.forServer(sslConfig.getServerKeyCertChainPathStream(),
                        sslConfig.getServerPrivateKeyPathStream(), password);
            } else {
                sslClientContextBuilder = GrpcSslContexts.forServer(sslConfig.getServerKeyCertChainPathStream(),
                        sslConfig.getServerPrivateKeyPathStream());
            }

            InputStream trustCertCollectionFilePath = sslConfig.getServerTrustCertCollectionPathStream();
            if (trustCertCollectionFilePath != null) {
                sslClientContextBuilder.trustManager(trustCertCollectionFilePath);
                sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        }
        try {
            return sslClientContextBuilder.build();
        } catch (SSLException e) {
            throw new IllegalStateException("Build SslSession failed.", e);
        }
    }

    private static SslContext buildClientSslContext(URL url) {
        ConfigManager globalConfigManager = ApplicationModel.getConfigManager();
        SslConfig sslConfig = globalConfigManager.getSsl().orElseThrow(() -> new IllegalStateException("Ssl enabled, but no ssl cert information provided!"));


        SslContextBuilder builder = GrpcSslContexts.forClient();
        try {
            InputStream trustCertCollectionFilePath = sslConfig.getClientTrustCertCollectionPathStream();
            if (trustCertCollectionFilePath != null) {
                builder.trustManager(trustCertCollectionFilePath);
            }
            InputStream clientCertChainFilePath = sslConfig.getClientKeyCertChainPathStream();
            InputStream clientPrivateKeyFilePath = sslConfig.getClientPrivateKeyPathStream();
            if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
                String password = sslConfig.getClientKeyPassword();
                if (password != null) {
                    builder.keyManager(clientCertChainFilePath, clientPrivateKeyFilePath, password);
                } else {
                    builder.keyManager(clientCertChainFilePath, clientPrivateKeyFilePath);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or find invalid certificate.", e);
        }
        try {
            return builder.build();
        } catch (SSLException e) {
            throw new IllegalStateException("Build SslSession failed.", e);
        }
    }

    private static Optional<GrpcConfigurator> getConfigurator() {
        // Give users the chance to customize ServerBuilder
        Set<GrpcConfigurator> configurators = ExtensionLoader.getExtensionLoader(GrpcConfigurator.class)
                .getSupportedExtensionInstances();
        if (CollectionUtils.isNotEmpty(configurators)) {
            return Optional.of(configurators.iterator().next());
        }
        return Optional.empty();
    }
}
