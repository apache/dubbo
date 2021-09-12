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

import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.xds.XdsCertificateSigner;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class XdsChannel {
    private final ManagedChannel channel;
    private static final Logger logger = LoggerFactory.getLogger(XdsChannel.class);

    protected XdsChannel(URL url) {
        ManagedChannel channel1 = null;
        try {
            XdsCertificateSigner signer = url.getOrDefaultApplicationModel().getExtensionLoader(XdsCertificateSigner.class).getExtension(url.getParameter("Signer","istio"));
            XdsCertificateSigner.CertPair certPair = signer.request(url);
            SslContext context = GrpcSslContexts.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .keyManager(new ByteArrayInputStream(certPair.getPublicKey().getBytes(StandardCharsets.UTF_8)), new ByteArrayInputStream(certPair.getPrivateKey().getBytes(StandardCharsets.UTF_8)))
                    .build();
            channel1 = NettyChannelBuilder.forAddress(url.getHost(), url.getPort())
                    .sslContext(context)
                    .build();
        } catch (SSLException e) {
            logger.error("Error occurred when creating gRPC channel to control panel.", e);
        }
        channel = channel1;
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
