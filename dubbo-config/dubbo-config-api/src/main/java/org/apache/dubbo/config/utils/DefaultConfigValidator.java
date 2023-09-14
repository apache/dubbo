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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.context.ConfigValidator;

public class DefaultConfigValidator implements ConfigValidator {

    @Override
    public void validate(AbstractConfig config) {
        if (config instanceof ProtocolConfig) {
            ConfigValidationUtils.validateProtocolConfig((ProtocolConfig) config);
        } else if (config instanceof RegistryConfig) {
            ConfigValidationUtils.validateRegistryConfig((RegistryConfig) config);
        } else if (config instanceof MetadataReportConfig) {
            ConfigValidationUtils.validateMetadataConfig((MetadataReportConfig) config);
        } else if (config instanceof ProviderConfig) {
            ConfigValidationUtils.validateProviderConfig((ProviderConfig) config);
        } else if (config instanceof ConsumerConfig) {
            ConfigValidationUtils.validateConsumerConfig((ConsumerConfig) config);
        } else if (config instanceof ApplicationConfig) {
            ConfigValidationUtils.validateApplicationConfig((ApplicationConfig) config);
        } else if (config instanceof MonitorConfig) {
            ConfigValidationUtils.validateMonitorConfig((MonitorConfig) config);
        } else if (config instanceof ModuleConfig) {
            ConfigValidationUtils.validateModuleConfig((ModuleConfig) config);
        } else if (config instanceof MetricsConfig) {
            ConfigValidationUtils.validateMetricsConfig((MetricsConfig) config);
        } else if (config instanceof TracingConfig) {
            ConfigValidationUtils.validateTracingConfig((TracingConfig) config);
        } else if (config instanceof SslConfig) {
            ConfigValidationUtils.validateSslConfig((SslConfig) config);
        }
    }

}
