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
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;

import static org.apache.dubbo.config.Constants.NAME;
import static org.apache.dubbo.config.Constants.ORGANIZATION;
import static org.apache.dubbo.config.Constants.OWNER;

@Activate
public class ModuleConfigValidator implements ConfigValidator<ModuleConfig> {

    @Override
    public boolean validate(ModuleConfig config) {
        validateModuleConfig(config);
        return true;
    }

    public static void validateModuleConfig(ModuleConfig config) {
        if (config != null) {
            ConfigValidationUtils.checkName(NAME, config.getName());
            ConfigValidationUtils.checkName(OWNER, config.getOwner());
            ConfigValidationUtils.checkName(ORGANIZATION, config.getOrganization());
        }
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ModuleConfig.class.equals(configClass);
    }
}
