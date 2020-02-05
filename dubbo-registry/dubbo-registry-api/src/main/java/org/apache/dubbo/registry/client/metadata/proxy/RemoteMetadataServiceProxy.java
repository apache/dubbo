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
package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.function.ThrowableFunction;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.registry.client.metadata.URLRevisionResolver.NO_REVISION;

public class RemoteMetadataServiceProxy implements MetadataService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String serviceName;
    private String revision;


    public RemoteMetadataServiceProxy(ServiceInstance serviceInstance) {
        this.serviceName = serviceInstance.getServiceName();
        // this is ServiceInstance of registry(Provider)
        this.revision = serviceInstance.getMetadata()
                .getOrDefault(ServiceInstanceMetadataUtils.EXPORTED_SERVICES_REVISION_PROPERTY_NAME, NO_REVISION);
    }

    @Override
    public String serviceName() {
        return serviceName;
    }

    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        List<String> r = ThrowableFunction.execute(
                new ServiceMetadataIdentifier(serviceInterface, group, version, PROVIDER_SIDE, revision, protocol), m -> {
            for (MetadataReport metadataReport : MetadataReportInstance.getMetadataReports(true)) {
                try {
                    return metadataReport.getExportedURLs(m);
                } catch (Exception e) {
                    logger.error("some error happened when try to getServiceDefinition", e);
                }
            }
            return null;
        });
        return toSortedStrings(r);
    }

    private static SortedSet<String> toSortedStrings(Collection<String> values) {
        return Collections.unmodifiableSortedSet(new TreeSet<>(values));
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return ThrowableFunction.execute(new MetadataIdentifier(interfaceName,
                version, group, PROVIDER_SIDE, serviceName), m -> {
            for (MetadataReport metadataReport : MetadataReportInstance.getMetadataReports(true)) {
                try {
                    return metadataReport.getServiceDefinition(m);
                } catch (Exception e) {
                    logger.error("some error happened when try to getServiceDefinition", e);
                }
            }
            return null;
        });
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        String[] services = UrlUtils.parseServiceKey(serviceKey);
        String serviceInterface = services[0];
        // if version or group is not exist
        String version = null;
        if (services.length > 1) {
            version = services[1];
        }
        String group = null;
        if (services.length > 2) {
            group = services[2];
        }
        return ThrowableFunction.execute(new MetadataIdentifier(serviceInterface,
                version, group, PROVIDER_SIDE, serviceName), m -> {
            for (MetadataReport metadataReport : MetadataReportInstance.getMetadataReports(true)) {
                try {
                    return metadataReport.getServiceDefinition(m);
                } catch (Exception e) {
                    logger.error("some error happened when try to getServiceDefinition", e);
                }
            }
            return null;
        });
    }

}
