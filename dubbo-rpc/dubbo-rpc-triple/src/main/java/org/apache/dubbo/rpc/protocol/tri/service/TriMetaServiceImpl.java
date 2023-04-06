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

import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.triple.meta.DubboMetaServiceTriple;
import org.apache.dubbo.triple.meta.MetaRequest;
import org.apache.dubbo.triple.meta.MetaResponse;
import org.apache.dubbo.triple.meta.ServiceInfo;

import java.util.Optional;


public class TriMetaServiceImpl extends DubboMetaServiceTriple.MetaServiceImplBase {

    private final ApplicationModel applicationModel = ApplicationModel.defaultModel();

    @Override
    public MetaResponse getMetadata(MetaRequest request) {
        MetadataInfo metadata = (MetadataInfo) applicationModel.getAttribute("metadata");
        if (metadata == null) {
            return super.getMetadata(request);
        }
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


}
