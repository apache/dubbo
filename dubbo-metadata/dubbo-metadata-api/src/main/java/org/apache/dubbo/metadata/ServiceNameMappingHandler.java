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

package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

public class ServiceNameMappingHandler {
    public static final String DUBBO_SERVICENAME_STORE = "dubbo.application.service-name.store";
    private static final Logger logger = LoggerFactory.getLogger(ServiceNameMappingHandler.class);
    private static final ServiceNameMappingStoreEnum DEFAULT_STORE_TYPE = ServiceNameMappingStoreEnum.BOTH_STORAGE;

    private final ServiceNameMapping serviceNameMapping;
    private final URL url;

    public ServiceNameMappingHandler(ServiceNameMapping serviceNameMapping, URL url) {
        this.serviceNameMapping = serviceNameMapping;
        this.url = url;
    }

    public static void map(ServiceNameMapping serviceNameMapping, URL url) {
        new ServiceNameMappingHandler(serviceNameMapping, url).init();
    }

    public void init() {
        DynamicConfiguration dynamicConfiguration = DynamicConfiguration.getDynamicConfiguration();

        ServiceNameMappingStoreEnum storeType = DEFAULT_STORE_TYPE;
        boolean hasSupportCas = dynamicConfiguration.hasSupportCas();
        if (!hasSupportCas) {
            storeType = ServiceNameMappingStoreEnum.APPLICANT_INTERFACE_STORAGE;
        }
        doMap(storeType);
    }

    public void doMap(ServiceNameMappingStoreEnum storeType) {
        if (null == storeType) {
            throw new IllegalStateException("storeType of serviceNameMapping cannot be null");
        }
        switch (storeType) {
            case INTERFACE_APPLICATION_STORAGE:
                serviceNameMapping.mapWithCas(url);
                break;
            case APPLICANT_INTERFACE_STORAGE:
                serviceNameMapping.map(url);
                break;
            case BOTH_STORAGE:
                serviceNameMapping.map(url);
                serviceNameMapping.mapWithCas(url);
                break;
            default:
                serviceNameMapping.map(url);
        }
    }
}
