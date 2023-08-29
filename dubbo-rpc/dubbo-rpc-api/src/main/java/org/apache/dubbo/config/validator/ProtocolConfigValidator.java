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
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.Dispatcher;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.Exchanger;
import org.apache.dubbo.remoting.telnet.TelnetHandler;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.remoting.Constants.CODEC_KEY;
import static org.apache.dubbo.remoting.Constants.DISPATCHER_KEY;
import static org.apache.dubbo.remoting.Constants.EXCHANGER_KEY;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.remoting.Constants.TELNET_KEY;
import static org.apache.dubbo.remoting.Constants.TRANSPORTER_KEY;

@Activate
public class ProtocolConfigValidator implements ConfigValidator<ProtocolConfig> {

    @Override
    public boolean validate(ProtocolConfig config) {
        validateProtocolConfig(config);
        return true;
    }

    public static void validateProtocolConfig(ProtocolConfig config) {
        if (config != null) {
            String name = config.getName();
            ConfigValidationUtils.checkName("name", name);
            ConfigValidationUtils.checkHost(HOST_KEY, config.getHost());
            ConfigValidationUtils.checkPathName("contextpath", config.getContextpath());


            if (DUBBO_PROTOCOL.equals(name)) {
                ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Codec2.class, CODEC_KEY, config.getCodec());
                ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Serialization.class, SERIALIZATION_KEY, config.getSerialization());
                ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Transporter.class, SERVER_KEY, config.getServer());
                ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Transporter.class, CLIENT_KEY, config.getClient());
            }

            ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), TelnetHandler.class, TELNET_KEY, config.getTelnet());
            ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), StatusChecker.class, "status", config.getStatus());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), Transporter.class, TRANSPORTER_KEY, config.getTransporter());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), Exchanger.class, EXCHANGER_KEY, config.getExchanger());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), Dispatcher.class, DISPATCHER_KEY, config.getDispatcher());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), Dispatcher.class, "dispather", config.getDispather());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), ThreadPool.class, THREADPOOL_KEY, config.getThreadpool());
        }
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ProtocolConfig.class.equals(configClass);
    }
}
