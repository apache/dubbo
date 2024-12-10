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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.DubboMetadataServiceV2Triple.MetadataServiceV2ImplBase;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataRequest;
import org.apache.dubbo.metadata.OpenAPIFormat;
import org.apache.dubbo.metadata.OpenAPIInfo;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.remoting.http12.rest.OpenAPIService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_LOAD_METADATA;
import static org.apache.dubbo.metadata.util.MetadataServiceVersionUtils.toV2;

public class MetadataServiceDelegationV2 extends MetadataServiceV2ImplBase {

    ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final FrameworkModel frameworkModel;
    private final RegistryManager registryManager;

    private URL metadataUrl;

    public static final String VERSION = "2.0.0";

    public MetadataServiceDelegationV2(ApplicationModel applicationModel) {
        frameworkModel = applicationModel.getFrameworkModel();
        registryManager = RegistryManager.getInstance(applicationModel);
    }

    @Override
    public org.apache.dubbo.metadata.MetadataInfoV2 getMetadataInfo(MetadataRequest metadataRequestV2) {
        String revision = metadataRequestV2.getRevision();
        MetadataInfo info;
        if (StringUtils.isEmpty(revision)) {
            return null;
        }

        for (ServiceDiscovery sd : registryManager.getServiceDiscoveries()) {
            info = sd.getLocalMetadata(revision);

            if (info != null && revision.equals(info.getRevision())) {
                return toV2(info);
            }
        }

        if (logger.isWarnEnabled()) {
            logger.warn(
                    REGISTRY_FAILED_LOAD_METADATA, "", "", "metadataV2 not found for revision: " + metadataRequestV2);
        }
        return null;
    }

    @Override
    public OpenAPIInfo getOpenAPIInfo(org.apache.dubbo.metadata.OpenAPIRequest request) {
        if (TripleProtocol.OPENAPI_ENABLED) {
            OpenAPIService openAPIService = frameworkModel.getBean(OpenAPIService.class);
            if (openAPIService != null) {
                OpenAPIRequest oRequest = new OpenAPIRequest();
                oRequest.setGroup(request.getGroup());
                oRequest.setVersion(request.getVersion());
                oRequest.setTag(request.getTagList().toArray(StringUtils.EMPTY_STRING_ARRAY));
                oRequest.setService(request.getServiceList().toArray(StringUtils.EMPTY_STRING_ARRAY));
                oRequest.setOpenapi(request.getOpenapi());
                OpenAPIFormat format = request.getFormat();
                if (request.hasFormat()) {
                    oRequest.setFormat(format.name());
                }
                if (request.hasPretty()) {
                    oRequest.setPretty(request.getPretty());
                }
                String document = openAPIService.getDocument(oRequest);
                return OpenAPIInfo.newBuilder().setDefinition(document).build();
            }
        }

        throw new HttpStatusException(HttpStatus.NOT_FOUND.getCode(), "OpenAPI is not available");
    }

    public URL getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(URL metadataUrl) {
        this.metadataUrl = metadataUrl;
    }
}
