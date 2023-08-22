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
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.util.ConfigValidationUtils;
import org.apache.dubbo.rpc.ExporterListener;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

@Activate
public class ServiceConfigValidator implements ConfigValidator<ServiceConfig<?>> {

    public static void validateServiceConfig(ServiceConfig<?> config) {

        ConfigValidationUtils.checkKey(VERSION_KEY, config.getVersion());
        ConfigValidationUtils.checkKey(GROUP_KEY, config.getGroup());
        ConfigValidationUtils.checkName(TOKEN_KEY, config.getToken());
        ConfigValidationUtils.checkPathName(PATH_KEY, config.getPath());

        ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), ExporterListener.class, "listener", config.getListener());

        InterfaceConfigValidator.validateAbstractInterfaceConfig(config);

        List<RegistryConfig> registries = config.getRegistries();
        if (registries != null) {
            for (RegistryConfig registry : registries) {
                registry.validate();
            }
        }

        List<ProtocolConfig> protocols = config.getProtocols();
        if (protocols != null) {
            for (ProtocolConfig protocol : protocols) {
                protocol.validate();
            }
        }

        ProviderConfig providerConfig = config.getProvider();
        if (providerConfig != null) {
            providerConfig.validate();
        }
    }

    @Override
    public void validate(ServiceConfig<?> config) {
        validateServiceConfig(config);
    }


    @Override
    public boolean isSupport(Class<?> configClass) {
        return ServiceConfig.class.equals(configClass);
    }
}
