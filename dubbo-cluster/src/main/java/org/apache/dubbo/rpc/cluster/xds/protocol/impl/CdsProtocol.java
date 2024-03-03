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
package org.apache.dubbo.rpc.cluster.xds.protocol.impl;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.cluster.xds.AdsObserver;
import org.apache.dubbo.rpc.cluster.xds.protocol.AbstractProtocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_RESPONSE_XDS;

public class CdsProtocol extends AbstractProtocol<String> {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CdsProtocol.class);

    public void setUpdateCallback(Consumer<Set<String>> updateCallback) {
        this.updateCallback = updateCallback;
    }

    private Consumer<Set<String>> updateCallback;

    public CdsProtocol(AdsObserver adsObserver, Node node, int checkInterval) {
        super(adsObserver, node, checkInterval);
    }

    @Override
    public String getTypeUrl() {
        return "type.googleapis.com/envoy.config.cluster.v3.Cluster";
    }

    public void subscribeClusters() {
        subscribeResource(null);
    }

    @Override
    protected Map<String, String> decodeDiscoveryResponse(DiscoveryResponse response) {
        if (getTypeUrl().equals(response.getTypeUrl())) {
            Set<String> set = response.getResourcesList().stream()
                    .map(CdsProtocol::unpackCluster)
                    .filter(Objects::nonNull)
                    .map(Cluster::getName)
                    .collect(Collectors.toSet());
            updateCallback.accept(set);
            // Map<String, ListenerResult> listenerDecodeResult = new ConcurrentHashMap<>();
            // listenerDecodeResult.put(emptyResourceName, new ListenerResult(set));
            // return listenerDecodeResult;
        }
        return new HashMap<>();
    }

    private static Cluster unpackCluster(Any any) {
        try {
            return any.unpack(Cluster.class);
        } catch (InvalidProtocolBufferException e) {
            logger.error(REGISTRY_ERROR_RESPONSE_XDS, "", "", "Error occur when decode xDS response.", e);
            return null;
        }
    }

    private static HttpConnectionManager unpackHttpConnectionManager(Any any) {
        try {
            if (!any.is(HttpConnectionManager.class)) {
                return null;
            }
            return any.unpack(HttpConnectionManager.class);
        } catch (InvalidProtocolBufferException e) {
            logger.error(REGISTRY_ERROR_RESPONSE_XDS, "", "", "Error occur when decode xDS response.", e);
            return null;
        }
    }
}
