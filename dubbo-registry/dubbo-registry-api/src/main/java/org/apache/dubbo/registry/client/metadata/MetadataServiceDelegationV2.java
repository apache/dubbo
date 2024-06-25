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
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.support.RegistryManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_LOAD_METADATA;
import static org.apache.dubbo.metadata.util.MetadataServiceVersionUtils.toV2;

public class MetadataServiceDelegationV2 extends MetadataServiceV2ImplBase {

    ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final ApplicationModel applicationModel;

    private final RegistryManager registryManager;

    private URL metadataUrl;

    public static final String VERSION = "2.0.0";

    public MetadataServiceDelegationV2(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.registryManager = RegistryManager.getInstance(applicationModel);
    }

    @Override
    public org.apache.dubbo.metadata.MetadataInfoV2 getMetadataInfo(org.apache.dubbo.metadata.Revision revisionV2) {
        String revision = revisionV2.getValue();
        MetadataInfo info = null;
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
            logger.warn(REGISTRY_FAILED_LOAD_METADATA, "", "", "metadataV2 not found for revision: " + revisionV2);
        }
        return null;
    }

    public URL getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(URL metadataUrl) {
        this.metadataUrl = metadataUrl;
    }
}
