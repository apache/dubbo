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
import org.apache.dubbo.xds.protocol.AbstractProtocol;
import org.apache.dubbo.xds.protocol.XdsResourceListener;
import org.apache.dubbo.xds.resource.XdsEndpointResource;
import org.apache.dubbo.xds.resource.XdsResourceType;
import org.apache.dubbo.xds.resource.update.CdsUpdate;
import org.apache.dubbo.xds.resource.update.EdsUpdate;
import org.apache.dubbo.xds.resource.update.ValidatedResourceUpdate;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_PARSING_XDS;

public class EdsProtocol extends AbstractProtocol<EdsUpdate> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(EdsProtocol.class);

    private static final XdsEndpointResource xdsEndpointResource = XdsEndpointResource.getInstance();

    private XdsResourceListener<CdsUpdate> clusterListener = clusters -> {
        Set<String> clusterNames =
                clusters.stream().map(CdsUpdate::getClusterName).collect(Collectors.toSet());
        this.subscribeResource(clusterNames);
    };

    public EdsProtocol(AdsObserver adsObserver, Node node, int checkInterval, ApplicationModel applicationModel) {
        super(adsObserver, node, checkInterval, applicationModel);
    }

    @Override
    public String getTypeUrl() {
        return "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment";
    }

    public XdsResourceListener<CdsUpdate> getCdsListener() {
        return clusterListener;
    }

    @Override
    protected Map<String, EdsUpdate> decodeDiscoveryResponse(DiscoveryResponse response) {
        if (!getTypeUrl().equals(response.getTypeUrl())) {
            return Collections.emptyMap();
        }
        ValidatedResourceUpdate<EdsUpdate> validatedResourceUpdate =
                xdsEndpointResource.parse(XdsResourceType.xdsResourceTypeArgs, response.getResourcesList());
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
