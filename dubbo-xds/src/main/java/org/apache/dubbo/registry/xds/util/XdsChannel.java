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
package org.apache.dubbo.registry.xds.util;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.registry.xds.XdsCertificateSigner;
import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.registry.xds.util.bootstrap.Bootstrapper;
import org.apache.dubbo.registry.xds.util.bootstrap.BootstrapperImpl;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_CREATE_CHANNEL_XDS;

public class XdsChannel {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsChannel.class);

    private static final String USE_AGENT = "use-agent";

    private URL url;

    private static final String SECURE = "secure";

    private static final String PLAINTEXT = "plaintext";

    private final ManagedChannel channel;

    public URL getUrl() {
        return url;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public XdsChannel(URL url) {
        ManagedChannel managedChannel = null;
        this.url = url;
        try {
            if (!url.getParameter(USE_AGENT, false)) {
                if(PLAINTEXT.equals(url.getParameter(SECURE))){
                    managedChannel = NettyChannelBuilder.forAddress(url.getHost(), url.getPort()).usePlaintext()
                        .build();
                }else{
                    XdsCertificateSigner signer = url.getOrDefaultApplicationModel().getExtensionLoader(XdsCertificateSigner.class)
                        .getExtension(url.getParameter("signer", "istio"));
                    XdsCertificateSigner.CertPair certPair = signer.GenerateCert(url);
                    SslContext context = GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .keyManager(new ByteArrayInputStream(certPair.getPublicKey().getBytes(StandardCharsets.UTF_8)),
                            new ByteArrayInputStream(certPair.getPrivateKey().getBytes(StandardCharsets.UTF_8)))
                        .build();
                    managedChannel = NettyChannelBuilder.forAddress(url.getHost(), url.getPort()).sslContext(context)
                        .build();
                }
            } else {
                BootstrapperImpl bootstrapper = new BootstrapperImpl();
                Bootstrapper.BootstrapInfo bootstrapInfo = bootstrapper.bootstrap();
                URLAddress address = URLAddress.parse(bootstrapInfo.servers().get(0).target(), null, false);
                EpollEventLoopGroup elg = new EpollEventLoopGroup();
                managedChannel = NettyChannelBuilder.forAddress(new DomainSocketAddress("/" + address.getPath()))
                    .eventLoopGroup(elg)
                    .channelType(EpollDomainSocketChannel.class)
                    .usePlaintext()
                    .build();
            }
        } catch (Exception e) {
            logger.error(REGISTRY_ERROR_CREATE_CHANNEL_XDS, "", "", "Error occurred when creating gRPC channel to control panel.", e);
        }
        channel = managedChannel;
    }

    public StreamObserver<DeltaDiscoveryRequest> observeDeltaDiscoveryRequest(StreamObserver<DeltaDiscoveryResponse> observer) {
        return AggregatedDiscoveryServiceGrpc.newStub(channel).deltaAggregatedResources(observer);
    }

    public StreamObserver<DiscoveryRequest> createDeltaDiscoveryRequest(StreamObserver<DiscoveryResponse> observer) {
        return AggregatedDiscoveryServiceGrpc.newStub(channel).streamAggregatedResources(observer);
    }

    public StreamObserver<io.envoyproxy.envoy.api.v2.DeltaDiscoveryRequest> observeDeltaDiscoveryRequestV2(StreamObserver<io.envoyproxy.envoy.api.v2.DeltaDiscoveryResponse> observer) {
        return io.envoyproxy.envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc.newStub(channel).deltaAggregatedResources(observer);
    }

    public StreamObserver<io.envoyproxy.envoy.api.v2.DiscoveryRequest> createDeltaDiscoveryRequestV2(StreamObserver<io.envoyproxy.envoy.api.v2.DiscoveryResponse> observer) {
        return io.envoyproxy.envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc.newStub(channel).streamAggregatedResources(observer);
    }

    public void destroy() {
        channel.shutdown();
    }
}
