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
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.rpc.InvokerListener;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;

@Activate
public class ReferenceConfigValidator implements ConfigValidator<ReferenceConfig<?>> {

    @Override
    public void validate(ReferenceConfig<?> config) {
        validateReferenceConfig(config);
    }

    public static void validateReferenceConfig(ReferenceConfig<?> config) {
        ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), InvokerListener.class, "listener", config.getListener());
        ConfigValidationUtils.checkKey(VERSION_KEY, config.getVersion());
        ConfigValidationUtils.checkKey(GROUP_KEY, config.getGroup());
        ConfigValidationUtils.checkName(CLIENT_KEY, config.getClient());

        InterfaceConfigValidator.validateAbstractInterfaceConfig(config);

        List<RegistryConfig> registries = config.getRegistries();
        if (registries != null) {
            for (RegistryConfig registry : registries) {
                registry.validate();
            }
        }

        ConsumerConfig consumerConfig = config.getConsumer();
        if (consumerConfig != null) {
            consumerConfig.validate();
        }
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ReferenceConfig.class.equals(configClass);
    }
}
