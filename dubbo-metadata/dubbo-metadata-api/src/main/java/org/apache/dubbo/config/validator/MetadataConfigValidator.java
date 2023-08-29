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
package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.context.ConfigValidator;

import static org.apache.dubbo.common.utils.StringUtils.isEmpty;

@Activate
public class MetadataConfigValidator implements ConfigValidator<MetadataReportConfig> {

    @Override
    public boolean validate(MetadataReportConfig config) {
       return validateMetadataConfig(config);
    }

    public static boolean validateMetadataConfig(MetadataReportConfig metadataReportConfig) {
        if (!isValidMetadataConfig(metadataReportConfig)) {
            return false;
        }

        String address = metadataReportConfig.getAddress();
        String protocol = metadataReportConfig.getProtocol();

        if ((isEmpty(address) || !address.contains("://")) && isEmpty(protocol)) {
            throw new IllegalArgumentException("Please specify valid protocol or address for metadata report " + address);
        }
        return true;
    }

    public static boolean isValidMetadataConfig(MetadataReportConfig metadataReportConfig) {
        if (metadataReportConfig == null) {
            return false;
        }

        if (Boolean.FALSE.equals(metadataReportConfig.getReportMetadata()) &&
            Boolean.FALSE.equals(metadataReportConfig.getReportDefinition())) {
            return false;
        }

        return !isEmpty(metadataReportConfig.getAddress());
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return MetadataReportConfig.class.equals(configClass);
    }
}
