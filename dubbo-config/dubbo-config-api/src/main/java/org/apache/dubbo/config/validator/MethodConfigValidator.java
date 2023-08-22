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
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.rpc.Constants.FAIL_PREFIX;
import static org.apache.dubbo.rpc.Constants.FORCE_PREFIX;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.Constants.RETURN_PREFIX;
import static org.apache.dubbo.rpc.Constants.THROW_PREFIX;

@Activate
public class MethodConfigValidator implements ConfigValidator<MethodConfig> {

    public static void validateMethodConfig(MethodConfig config) {
        ConfigValidationUtils.checkExtension(config.getScopeModel(), LoadBalance.class, LOADBALANCE_KEY, config.getLoadbalance());
        ConfigValidationUtils.checkParameterName(config.getParameters());
        ConfigValidationUtils.checkMethodName("name", config.getName());

        String mock = config.getMock();
        if (isNotEmpty(mock)) {
            if (mock.startsWith(RETURN_PREFIX) || mock.startsWith(THROW_PREFIX + " ")) {
                ConfigValidationUtils.checkLength(MOCK_KEY, mock);
            } else if (mock.startsWith(FAIL_PREFIX) || mock.startsWith(FORCE_PREFIX)) {
                ConfigValidationUtils.checkNameHasSymbol(MOCK_KEY, mock);
            } else {
                ConfigValidationUtils.checkName(MOCK_KEY, mock);
            }
        }
    }

    @Override
    public void validate(MethodConfig config) {
        validateMethodConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return MethodConfig.class.equals(configClass);
    }
}
