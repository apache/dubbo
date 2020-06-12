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
package org.apache.dubbo.metadata.store.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.support.ConfigCenterBasedMetadataReportFactory;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.configcenter.support.zookeeper.ZookeeperDynamicConfiguration.CONFIG_BASE_PATH_PARAM_NAME;

/**
 * ZookeeperRegistryFactory.
 *
 * @revised 2.7.8 {@link ConfigCenterBasedMetadataReportFactory}
 */
public class ZookeeperMetadataReportFactory extends ConfigCenterBasedMetadataReportFactory {

    public ZookeeperMetadataReportFactory() {
        super(KeyTypeEnum.PATH);
    }

    /**
     * @param url The {@link URL} of metadata report
     * @return non-null
     * @since 2.7.8
     */
    @Override
    public MetadataReport getMetadataReport(URL url) {
        // Change the "config base path"
        URL newURL = url.addParameter(CONFIG_BASE_PATH_PARAM_NAME, PATH_SEPARATOR);
        return super.getMetadataReport(newURL);
    }
}
