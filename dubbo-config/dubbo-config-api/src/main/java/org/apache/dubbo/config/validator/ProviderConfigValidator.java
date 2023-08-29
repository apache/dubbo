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
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.Exchanger;
import org.apache.dubbo.remoting.telnet.TelnetHandler;

import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.config.Constants.CONTEXTPATH_KEY;
import static org.apache.dubbo.config.Constants.STATUS_KEY;
import static org.apache.dubbo.remoting.Constants.EXCHANGER_KEY;
import static org.apache.dubbo.remoting.Constants.TELNET_KEY;
import static org.apache.dubbo.remoting.Constants.TRANSPORTER_KEY;

@Activate
public class ProviderConfigValidator implements ConfigValidator<ProviderConfig> {

    @Override
    public boolean validate(ProviderConfig config) {
        validateProviderConfig(config);
        return true;
    }

    public static void validateProviderConfig(ProviderConfig config) {
        ConfigValidationUtils.checkPathName(CONTEXTPATH_KEY, config.getContextpath());
        ConfigValidationUtils.checkExtension(config.getScopeModel(), ThreadPool.class, THREADPOOL_KEY, config.getThreadpool());
        ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), TelnetHandler.class, TELNET_KEY, config.getTelnet());
        ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), StatusChecker.class, STATUS_KEY, config.getStatus());
        ConfigValidationUtils.checkExtension(config.getScopeModel(), Transporter.class, TRANSPORTER_KEY, config.getTransporter());
        ConfigValidationUtils.checkExtension(config.getScopeModel(), Exchanger.class, EXCHANGER_KEY, config.getExchanger());
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ProviderConfig.class.equals(configClass);
    }
}
