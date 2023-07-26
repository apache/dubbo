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
package org.apache.dubbo.metadata.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.metadata.report.support.NopMetadataReport;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_LOCAL_FILE_CACHE_ENABLED;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.metadata.report.support.Constants.METADATA_REPORT_KEY;

/**
 * Repository of MetadataReport instances that can talk to remote metadata server.
 *
 * MetadataReport instances are initiated during the beginning of deployer.start() and used by components that
 * need to interact with metadata server.
 *
 * If multiple metadata reports and registries need to be declared, it is recommended to group each two metadata report and registry together by giving them the same id:
 * <dubbo:registry id=demo1 address="registry://"/>
 * <dubbo:metadata id=demo1 address="metadata://"/>
 *
 * <dubbo:registry id=demo2 address="registry://"/>
 * <dubbo:metadata id=demo2 address="metadata://"/>
 */
public class MetadataReportInstance implements Disposable {

    private AtomicBoolean init = new AtomicBoolean(false);
    private String metadataType;

    // mapping of registry id to metadata report instance, registry instances will use this mapping to find related metadata reports
    private final Map<String, MetadataReport> metadataReports = new HashMap<>();
    private final ApplicationModel applicationModel;
    private final NopMetadataReport nopMetadataReport;

    public MetadataReportInstance(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.nopMetadataReport = new NopMetadataReport();
    }

    public void init(List<MetadataReportConfig> metadataReportConfigs) {
        if (!init.compareAndSet(false, true)) {
            return;
        }

        this.metadataType = applicationModel.getApplicationConfigManager().getApplicationOrElseThrow().getMetadataType();
        if (metadataType == null) {
            this.metadataType = DEFAULT_METADATA_STORAGE_TYPE;
        }

        MetadataReportFactory metadataReportFactory = applicationModel.getExtensionLoader(MetadataReportFactory.class).getAdaptiveExtension();
        for (MetadataReportConfig metadataReportConfig : metadataReportConfigs) {
            init(metadataReportConfig, metadataReportFactory);
        }
    }

    private void init(MetadataReportConfig config, MetadataReportFactory metadataReportFactory) {
        URL url = config.toUrl();
        if (METADATA_REPORT_KEY.equals(url.getProtocol())) {
            String protocol = url.getParameter(METADATA_REPORT_KEY, DEFAULT_DIRECTORY);
            url = URLBuilder.from(url)
                    .setProtocol(protocol)
                    .setPort(url.getParameter(PORT_KEY, url.getPort()))
                    .setScopeModel(config.getScopeModel())
                    .removeParameter(METADATA_REPORT_KEY)
                    .build();
        }
        url = url.addParameterIfAbsent(APPLICATION_KEY, applicationModel.getCurrentConfig().getName());
        url = url.addParameterIfAbsent(REGISTRY_LOCAL_FILE_CACHE_ENABLED, String.valueOf(applicationModel.getCurrentConfig().getEnableFileCache()));
        String relatedRegistryId = isEmpty(config.getRegistry()) ? (isEmpty(config.getId()) ? DEFAULT_KEY : config.getId()) : config.getRegistry();
//        RegistryConfig registryConfig = applicationModel.getConfigManager().getRegistry(relatedRegistryId)
//                .orElseThrow(() -> new IllegalStateException("Registry id " + relatedRegistryId + " does not exist."));
        MetadataReport metadataReport = metadataReportFactory.getMetadataReport(url);
        if (metadataReport != null) {
            metadataReports.put(relatedRegistryId, metadataReport);
        }
    }

    public Map<String, MetadataReport> getMetadataReports(boolean checked) {
        return metadataReports;
    }

    public MetadataReport getMetadataReport(String registryKey) {
        MetadataReport metadataReport = metadataReports.get(registryKey);
        if (metadataReport == null && metadataReports.size() > 0) {
            metadataReport = metadataReports.values().iterator().next();
        }
        return metadataReport;
    }

    public MetadataReport getNopMetadataReport() {
        return nopMetadataReport;
    }

    public String getMetadataType() {
        return metadataType;
    }

    public boolean inited() {
        return init.get();
    }

    @Override
    public void destroy() {
        metadataReports.forEach((_k, reporter) -> {
            reporter.destroy();
        });
        metadataReports.clear();
    }
}
