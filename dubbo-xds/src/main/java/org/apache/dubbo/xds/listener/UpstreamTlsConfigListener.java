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
package org.apache.dubbo.xds.listener;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.XdsException;
import org.apache.dubbo.xds.XdsException.Type;
import org.apache.dubbo.xds.security.authn.TlsResourceResolver;
import org.apache.dubbo.xds.security.authn.UpstreamTlsConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext;

@Activate
public class UpstreamTlsConfigListener implements CdsListener {

    private static final String TRANSPORT_SOCKET_NAME = "envoy.transport_sockets.tls";

    private static final String UPSTREAM_TLS_CONFIG_NAME =
            "type.googleapis.com/envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext";

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(UpstreamTlsConfigListener.class);

    private final XdsTlsConfigRepository tlsConfigRepository;

    public UpstreamTlsConfigListener(ApplicationModel application) {
        this.tlsConfigRepository = application.getBeanFactory().getOrRegisterBean(XdsTlsConfigRepository.class);
    }

    @Override
    public void onResourceUpdate(List<Cluster> resource) {
        Map<String, UpstreamTlsConfig> configs = new ConcurrentHashMap<>(16);
        for (Cluster cluster : resource) {
            String serviceName = cluster.getName();
            try {
                if (!TRANSPORT_SOCKET_NAME.equals(cluster.getTransportSocket().getName())) {
                    // No TLS config found in this cluster.
                    configs.put(serviceName, new UpstreamTlsConfig());
                    logger.debug(
                            "No TLS config provided for this service to connect upstream cluster:" + cluster.getName());
                    continue;
                }
                String typeUrl = cluster.getTransportSocket().getTypedConfig().getTypeUrl();

                if (!UPSTREAM_TLS_CONFIG_NAME.equals(typeUrl)) {
                    logger.info("Unknown TLS config type:" + typeUrl);
                    continue;
                }

                UpstreamTlsContext tlsContext =
                        cluster.getTransportSocket().getTypedConfig().unpack(UpstreamTlsContext.class);
                CommonTlsContext commonTlsContext = tlsContext.getCommonTlsContext();

                configs.put(
                        serviceName,
                        new UpstreamTlsConfig(
                                TlsResourceResolver.resolveCommonTlsConfig(serviceName, commonTlsContext),
                                tlsContext.getSni(),
                                tlsContext.getAllowRenegotiation()));
                tlsConfigRepository.updateOutbound(configs);
            } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
                throw new XdsException(
                        Type.CDS, "Invalid UpstreamTlsContext config provided for cluster:" + cluster.getName());
            }
        }
    }
}
