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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;


@Activate
public class MonitorConfigValidator implements ConfigValidator<MonitorConfig> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MonitorConfigValidator.class);

    public static void validateMonitorConfig(MonitorConfig config) {
        if (config != null) {
            if (!config.isValid()) {
                logger.info("There's no valid monitor config found, if you want to open monitor statistics for Dubbo, " +
                    "please make sure your monitor is configured properly.");
            }
            ConfigValidationUtils.checkParameterName(config.getParameters());
        }
    }

    @Override
    public void validate(MonitorConfig config) {
        validateMonitorConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return MonitorConfig.class.isAssignableFrom(configClass);
    }
}
