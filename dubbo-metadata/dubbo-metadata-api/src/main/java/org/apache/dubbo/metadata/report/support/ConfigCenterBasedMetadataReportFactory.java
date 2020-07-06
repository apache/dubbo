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
package org.apache.dubbo.metadata.report.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportFactory;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.config.configcenter.TreePathDynamicConfiguration.CONFIG_BASE_PATH_PARAM_NAME;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_PATH_TAG;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

/**
 * The abstract implementation of {@link MetadataReportFactory} based on
 * {@link DynamicConfiguration the config-center infrastructure}
 *
 * @see MetadataReportFactory
 * @see MetadataReport
 * @see DynamicConfiguration
 * @since 2.7.8
 */
public abstract class ConfigCenterBasedMetadataReportFactory implements MetadataReportFactory {

    /**
     * org.apache.dubbo.metadata.report.MetadataReport
     */
    private static final String URL_PATH = MetadataReport.class.getName();

    // Registry Collection Map<metadataAddress, MetadataReport>
    private static final Map<String, ConfigCenterBasedMetadataReport> metadataReportCache = new ConcurrentHashMap();

    private final KeyTypeEnum keyType;

    public ConfigCenterBasedMetadataReportFactory(KeyTypeEnum keyType) {
        if (keyType == null) {
            throw new NullPointerException("The keyType argument must not be null!");
        }
        this.keyType = keyType;
    }

    @Override
    public ConfigCenterBasedMetadataReport getMetadataReport(URL url) {

        url = url.setPath(URL_PATH).removeParameters(EXPORT_KEY, REFER_KEY);

        final URL actualURL;
        if (url.getParameter(CONFIG_BASE_PATH_PARAM_NAME) == null) {
            actualURL = url.addParameter(CONFIG_BASE_PATH_PARAM_NAME, DEFAULT_PATH_TAG);
        } else {
            actualURL = url;
        }

        String key = actualURL.toServiceString();
        // Lock the metadata access process to ensure a single instance of the metadata instance
        return metadataReportCache.computeIfAbsent(key, k -> new ConfigCenterBasedMetadataReport(actualURL, keyType));
    }

    /**
     * Get {@link KeyTypeEnum the key type}
     *
     * @return non-null
     */
    protected KeyTypeEnum getKeyType() {
        return keyType;
    }
}
