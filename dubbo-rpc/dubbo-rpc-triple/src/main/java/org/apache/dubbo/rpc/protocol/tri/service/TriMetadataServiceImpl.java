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

package org.apache.dubbo.rpc.protocol.tri.service;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.triple.metadata.AllMetaRequest;
import org.apache.dubbo.triple.metadata.AllMetaResponse;
import org.apache.dubbo.triple.metadata.DubboMetadataTriple;
import org.apache.dubbo.triple.metadata.MetaRequest;
import org.apache.dubbo.triple.metadata.MetaResponse;
import org.apache.dubbo.triple.metadata.ServiceInfo;

import java.util.Optional;


public class TriMetadataServiceImpl extends DubboMetadataTriple.MetadataImplBase {

    private final ApplicationModel applicationModel;
    private final RegistryManager registryManager;

    public TriMetadataServiceImpl() {
        this.applicationModel = ApplicationModel.defaultModel();
        registryManager = RegistryManager.getInstance(applicationModel);
    }


    @Override
    public MetaResponse getMetadata(MetaRequest metaRequest) {
        String revision = metaRequest.getRevision();
        if (StringUtils.EMPTY_STRING.equals(revision)) {
            //TODO set status
            return super.getMetadata(metaRequest);
        }

        MetadataInfo metadata = getMetadataInfo(revision);

        if (metadata == null) {
            //TODO set status
            return null;
        }

        return getMetaResponse(metadata);
    }

    @Override
    public AllMetaResponse getAllMetadata(AllMetaRequest request) {
        AllMetaResponse.Builder allMetaResponseBuilder = AllMetaResponse.newBuilder();
        for (ServiceDiscovery serviceDiscovery : registryManager.getServiceDiscoveries()) {
            MetadataInfo metadata = serviceDiscovery.getLocalMetadata();
            allMetaResponseBuilder.addAllMetadata(getMetaResponse(metadata));
        }
        return allMetaResponseBuilder.build();
    }

    private MetaResponse getMetaResponse(MetadataInfo metadata) {
        MetaResponse.Builder metadataResponseBuilder = MetaResponse.newBuilder()
            .setApp(metadata.getApp())
            .setRevision(metadata.getRevision());
        metadata.getServices().forEach((serviceName, serviceInfo) -> {
            ServiceInfo triServiceInfo = ServiceInfo.newBuilder()
                .setGroup(Optional.ofNullable(serviceInfo.getGroup()).orElse(""))
                .setName(serviceInfo.getName())
                .setPath(serviceInfo.getPath())
                .setPort(serviceInfo.getPort())
                .setVersion(Optional.ofNullable(serviceInfo.getVersion()).orElse(""))
                .setProtocol(serviceInfo.getProtocol())
                .putAllParams(serviceInfo.getAllParams()).build();
            metadataResponseBuilder.putServices(serviceName, triServiceInfo);
        });
        return metadataResponseBuilder.build();
    }

    private MetadataInfo getMetadataInfo(String revision) {
        for (ServiceDiscovery serviceDiscovery : registryManager.getServiceDiscoveries()) {
            MetadataInfo metadata = serviceDiscovery.getLocalMetadata(revision);
            if (metadata != null && revision.equals(metadata.getRevision())) {
                return metadata;
            }
        }
        return null;
    }


}
