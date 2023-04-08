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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.triple.metadata.AllMetaRequest;
import org.apache.dubbo.triple.metadata.AllMetaResponse;
import org.apache.dubbo.triple.metadata.DubboMetadataTriple;
import org.apache.dubbo.triple.metadata.MetaRequest;
import org.apache.dubbo.triple.metadata.MetaResponse;
import org.apache.dubbo.triple.metadata.ResponseStatus;
import org.apache.dubbo.triple.metadata.ServiceInfo;


import java.util.List;
import java.util.Optional;


public class TriMetadataServiceImpl extends DubboMetadataTriple.MetadataImplBase {


    @Override
    public MetaResponse getMetadata(MetaRequest metaRequest) {
        MetadataService metadataService = getMetadataService();
        if (metadataService == null) {
            return MetaResponse.newBuilder().setStatus(ResponseStatus.SERVICE_NOT_REGISTER).build();
        }
        return getMetaResponse(metadataService.getMetadataInfo(metaRequest.getRevision()));
    }

    @Override
    public AllMetaResponse getAllMetadata(AllMetaRequest request) {
        MetadataService metadataService = getMetadataService();
        if (metadataService == null) {
            return AllMetaResponse.newBuilder().setStatus(ResponseStatus.SERVICE_NOT_REGISTER).build();
        }
        AllMetaResponse.Builder allMetaResponseBuilder = AllMetaResponse.newBuilder();
        List<MetadataInfo> metadataInfos = metadataService.getMetadataInfos();
        if (CollectionUtils.isEmpty(metadataInfos)) {
            return AllMetaResponse.newBuilder().setStatus(ResponseStatus.NO_METADATA).build();
        }
        for (MetadataInfo metadataInfo : metadataInfos) {
            allMetaResponseBuilder.addAllMetadata(getMetaResponse(metadataInfo));
        }
        return allMetaResponseBuilder.setStatus(ResponseStatus.SUCCESS).build();
    }

    private MetaResponse getMetaResponse(MetadataInfo metadata) {
        if (metadata == null) {
            return MetaResponse.newBuilder().setStatus(ResponseStatus.REVISION_UN_FIND).build();
        }
        MetaResponse.Builder metadataResponseBuilder = MetaResponse.newBuilder()
            .setApp(metadata.getApp())
            .setRevision(metadata.getRevision())
            .setStatus(ResponseStatus.SUCCESS);
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

    private MetadataService getMetadataService() {
        return ApplicationModel.defaultModel().getBeanFactory().getBean(MetadataService.class);
    }


}
