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
package org.apache.dubbo.xds.protocol.impl;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.AdsObserver;
import org.apache.dubbo.xds.listener.CdsListener;
import org.apache.dubbo.xds.protocol.AbstractProtocol;
import org.apache.dubbo.xds.resource.XdsClusterResource;
import org.apache.dubbo.xds.resource.XdsResourceType;
import org.apache.dubbo.xds.resource.update.CdsUpdate;
import org.apache.dubbo.xds.resource.update.ValidatedResourceUpdate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_PARSING_XDS;

public class CdsProtocol extends AbstractProtocol<CdsUpdate> {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CdsProtocol.class);

    public void setUpdateCallback(Consumer<Set<String>> updateCallback) {
        this.updateCallback = updateCallback;
    }

    private static final XdsClusterResource xdsClusterResource = XdsClusterResource.getInstance();

    private Consumer<Set<String>> updateCallback;

    public CdsProtocol(AdsObserver adsObserver, Node node, int checkInterval, ApplicationModel applicationModel) {
        super(adsObserver, node, checkInterval, applicationModel);
        List<CdsListener> ldsListeners =
                applicationModel.getExtensionLoader(CdsListener.class).getActivateExtensions();
        ldsListeners.forEach(this::registerListen);
    }

    @Override
    public String getTypeUrl() {
        return "type.googleapis.com/envoy.config.cluster.v3.Cluster";
    }

    public void subscribeClusters() {
        subscribeResource(null);
    }

    @Override
    protected Map<String, CdsUpdate> decodeDiscoveryResponse(DiscoveryResponse response) {
        if (!getTypeUrl().equals(response.getTypeUrl())) {
            return Collections.emptyMap();
        }
        ValidatedResourceUpdate<CdsUpdate> validatedResourceUpdate =
                xdsClusterResource.parse(XdsResourceType.xdsResourceTypeArgs, response.getResourcesList());
        if (!validatedResourceUpdate.getErrors().isEmpty()) {
            logger.error(
                    REGISTRY_ERROR_PARSING_XDS,
                    validatedResourceUpdate.getErrors().toArray());
        }
        return validatedResourceUpdate.getParsedResources().entrySet().stream()
                .collect(Collectors.toConcurrentMap(
                        Entry::getKey, e -> e.getValue().getResourceUpdate()));
    }
}
