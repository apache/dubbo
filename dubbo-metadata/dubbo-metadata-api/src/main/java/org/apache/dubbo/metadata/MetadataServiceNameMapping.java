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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.model.ApplicationModel.getName;

public class MetadataServiceNameMapping implements ServiceNameMapping {
    private static final List<String> IGNORED_SERVICE_INTERFACES = asList(MetadataService.class.getName());

    @Override
    public void map(URL url) {
        String serviceInterface = url.getServiceInterface();
        String group = url.getParameter(GROUP_KEY);
        String version = url.getParameter(VERSION_KEY);
        String protocol = url.getProtocol();

        if (IGNORED_SERVICE_INTERFACES.contains(serviceInterface)) {
            return;
        }

        List<MetadataReport> metadataReports = MetadataReportInstance.getMetadataReports(true);
        metadataReports.forEach(reporter -> {
            reporter.registerServiceAppMapping(ServiceNameMapping.buildGroup(serviceInterface, group, version, protocol), getName(), url);
        });
    }

    @Override
    public Set<String> get(URL url) {
        String serviceInterface = url.getServiceInterface();
        String group = url.getParameter(GROUP_KEY);
        String version = url.getParameter(VERSION_KEY);
        String protocol = url.getProtocol();

        List<MetadataReport> metadataReports = MetadataReportInstance.getMetadataReports(true);
        Set<String> serviceNames = new LinkedHashSet<>();
        for (MetadataReport reporter : metadataReports) {
            Set<String> apps = reporter.getServiceAppMapping(ServiceNameMapping.buildGroup(serviceInterface, group, version, protocol), url);
            if (CollectionUtils.isNotEmpty(apps)) {
                serviceNames.addAll(apps);
                break;
            }
        }
        return serviceNames;
    }
}
