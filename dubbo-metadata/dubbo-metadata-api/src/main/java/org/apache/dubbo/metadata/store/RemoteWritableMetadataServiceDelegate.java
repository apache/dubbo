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
package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.WritableMetadataService;

import java.util.SortedSet;
import java.util.function.BiFunction;

/**
 * 2019-08-14
 *
 * @since 2.7.5
 */
public class RemoteWritableMetadataServiceDelegate implements WritableMetadataService {
    InMemoryWritableMetadataService defaultWritableMetadataService;
    RemoteWritableMetadataService remoteWritableMetadataService;

    public RemoteWritableMetadataServiceDelegate() {
        defaultWritableMetadataService = (InMemoryWritableMetadataService) WritableMetadataService.getExtension("local");
        remoteWritableMetadataService = new RemoteWritableMetadataService(defaultWritableMetadataService);
    }

    private WritableMetadataService getDefaultWritableMetadataService() {
        return defaultWritableMetadataService;
    }

    @Override
    public boolean exportURL(URL url) {
        return doFunction(WritableMetadataService::exportURL, url);
    }

    @Override
    public boolean unexportURL(URL url) {
        return doFunction(WritableMetadataService::unexportURL, url);
    }

    @Override
    public boolean subscribeURL(URL url) {
        return doFunction(WritableMetadataService::subscribeURL, url);
    }

    @Override
    public boolean unsubscribeURL(URL url) {
        return doFunction(WritableMetadataService::unsubscribeURL, url);
    }

    @Override
    public boolean refreshMetadata(String exportedRevision, String subscribedRevision) {
        boolean result = true;
        result &= defaultWritableMetadataService.refreshMetadata(exportedRevision, subscribedRevision);
        result &= remoteWritableMetadataService.refreshMetadata(exportedRevision, subscribedRevision);
        return result;
    }

    @Override
    public void publishServiceDefinition(URL providerUrl) {
        defaultWritableMetadataService.publishServiceDefinition(providerUrl);
        remoteWritableMetadataService.publishServiceDefinition(providerUrl);
    }

    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        return getDefaultWritableMetadataService().getExportedURLs(serviceInterface, group, version, protocol);
    }

    @Override
    public SortedSet<String> getSubscribedURLs() {
        return getDefaultWritableMetadataService().getSubscribedURLs();
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return getDefaultWritableMetadataService().getServiceDefinition(interfaceName, version, group);
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        return getDefaultWritableMetadataService().getServiceDefinition(serviceKey);
    }

    private boolean doFunction(BiFunction<WritableMetadataService, URL, Boolean> func, URL url) {
        return func.apply(defaultWritableMetadataService, url) && func.apply(remoteWritableMetadataService, url);
    }
}
