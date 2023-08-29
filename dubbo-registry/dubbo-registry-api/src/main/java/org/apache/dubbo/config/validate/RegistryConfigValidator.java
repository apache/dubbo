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
package org.apache.dubbo.config.validate;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;

import static org.apache.dubbo.common.constants.CommonConstants.FILE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PASSWORD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.USERNAME_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.remoting.Constants.TRANSPORTER_KEY;

@Activate
public class RegistryConfigValidator implements ConfigValidator<RegistryConfig> {

    @Override
    public boolean validate(RegistryConfig config) {
        validateRegistryConfig(config);
        return true;
    }

    public static void validateRegistryConfig(RegistryConfig config) {
        ConfigValidationUtils.checkName(PROTOCOL_KEY, config.getProtocol());
        ConfigValidationUtils.checkName(USERNAME_KEY, config.getUsername());
        ConfigValidationUtils.checkLength(PASSWORD_KEY, config.getPassword());
        ConfigValidationUtils.checkPathLength(FILE_KEY, config.getFile());
        ConfigValidationUtils.checkName(TRANSPORTER_KEY, config.getTransporter());
        ConfigValidationUtils.checkName(SERVER_KEY, config.getServer());
        ConfigValidationUtils.checkName(CLIENT_KEY, config.getClient());
        ConfigValidationUtils.checkParameterName(config.getParameters());
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return RegistryConfig.class.isAssignableFrom(configClass);
    }
}
