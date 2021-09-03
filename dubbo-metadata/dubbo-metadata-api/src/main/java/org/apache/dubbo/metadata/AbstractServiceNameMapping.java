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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.Set;
import java.util.TreeSet;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

public abstract class AbstractServiceNameMapping implements ServiceNameMapping, ScopeModelAware {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected ApplicationModel applicationModel;
    private WritableMetadataService metadataService;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        metadataService = WritableMetadataService.getDefaultExtension(applicationModel);
    }

    /**
     * Get the service names from the specified Dubbo service interface, group, version and protocol
     *
     * @return
     */
    abstract public Set<String> get(URL url);

    /**
     * Get the service names from the specified Dubbo service interface, group, version and protocol
     *
     * @return
     */
    abstract public Set<String> getAndListen(URL url, MappingListener mappingListener);

    @Override
    public Set<String> getServices(URL subscribedURL) {
        Set<String> subscribedServices = new TreeSet<>();

        String serviceNames = subscribedURL.getParameter(PROVIDED_BY);
        if (StringUtils.isNotEmpty(serviceNames)) {
            logger.info(subscribedURL.getServiceInterface() + " mapping to " + serviceNames + " instructed by provided-by set by user.");
            subscribedServices.addAll(parseServices(serviceNames));
        }

        if (isEmpty(subscribedServices)) {
            Set<String> cachedServices = metadataService.getCachedMapping(ServiceNameMapping.buildMappingKey(subscribedURL));
            if(!isEmpty(cachedServices)) {
                subscribedServices.addAll(cachedServices);
            }
        }

        if (isEmpty(subscribedServices)) {
            Set<String> mappedServices = get(subscribedURL);
            logger.info(subscribedURL.getServiceInterface() + " mapping to " + mappedServices + " instructed by remote metadata center.");
            subscribedServices.addAll(mappedServices);
            metadataService.putCachedMapping(ServiceNameMapping.buildMappingKey(subscribedURL), subscribedServices);
        }
        return subscribedServices;
    }

    @Override
    public Set<String> getAndListenServices(URL registryURL, URL subscribedURL, MappingListener listener) {
        Set<String> subscribedServices = new TreeSet<>();
        Set<String> globalConfiguredSubscribingServices = parseServices(registryURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY));

        String serviceNames = subscribedURL.getParameter(PROVIDED_BY);
        if (StringUtils.isNotEmpty(serviceNames)) {
            logger.info(subscribedURL.getServiceInterface() + " mapping to " + serviceNames + " instructed by provided-by set by user.");
            subscribedServices.addAll(parseServices(serviceNames));
        }

        if (isEmpty(subscribedServices)) {
            Set<String> mappedServices = getAndListen(subscribedURL, listener);
            logger.info(subscribedURL.getServiceInterface() + " mapping to " + mappedServices + " instructed by remote metadata center.");
            subscribedServices.addAll(mappedServices);
            if (isEmpty(subscribedServices)) {
                logger.info(subscribedURL.getServiceInterface() + " mapping to " + globalConfiguredSubscribingServices + " by default.");
                subscribedServices.addAll(globalConfiguredSubscribingServices);
            }
        }

        metadataService.putCachedMapping(ServiceNameMapping.buildMappingKey(subscribedURL), subscribedServices);

        return subscribedServices;
    }

    static Set<String> parseServices(String literalServices) {
        return isBlank(literalServices) ? emptySet() :
            unmodifiableSet(of(literalServices.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(toSet()));
    }

}
