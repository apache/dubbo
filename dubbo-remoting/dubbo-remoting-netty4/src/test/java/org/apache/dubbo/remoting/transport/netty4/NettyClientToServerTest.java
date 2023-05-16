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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.Replier;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;

/**
 * Netty4ClientToServerTest
 */
class NettyClientToServerTest extends ClientToServerTest {

    protected ExchangeServer newServer(int port, Replier<?> receiver) throws RemotingException {
        // add heartbeat cycle to avoid unstable ut.
        URL url = URL.valueOf("exchange://localhost:" + port + "?server=netty4");
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig = new ApplicationConfig("provider-app");
        applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_DEFAULT);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ConfigManager configManager = new ConfigManager(applicationModel);
        configManager.setApplication(applicationConfig);
        configManager.getApplication();
        applicationModel.setConfigManager(configManager);
        url = url.addParameter(Constants.HEARTBEAT_KEY, 600 * 1000).putAttribute(CommonConstants.SCOPE_MODEL, applicationModel);
        url = url.setScopeModel(applicationModel);
//        ModuleModel moduleModel = applicationModel.getDefaultModule();

        ModuleModel moduleModel = applicationModel.getDefaultModule();
        url = url.putAttribute(CommonConstants.SCOPE_MODEL, moduleModel);
        return Exchangers.bind(url, receiver);
    }

    protected ExchangeChannel newClient(int port) throws RemotingException {
        // add heartbeat cycle to avoid unstable ut.
        URL url = URL.valueOf("exchange://localhost:" + port + "?client=netty4&timeout=3000");
        url = url.addParameter(Constants.HEARTBEAT_KEY, 600 * 1000);
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        ApplicationConfig applicationConfig = new ApplicationConfig("provider-app");
        applicationConfig.setExecutorManagementMode(EXECUTOR_MANAGEMENT_MODE_DEFAULT);
        applicationModel.getApplicationConfigManager().setApplication(applicationConfig);
        ConfigManager configManager = new ConfigManager(applicationModel);
        configManager.setApplication(applicationConfig);
        configManager.getApplication();
        applicationModel.setConfigManager(configManager);
        url = url.setScopeModel(applicationModel);
        ModuleModel moduleModel = applicationModel.getDefaultModule();
        url = url.putAttribute(CommonConstants.SCOPE_MODEL, moduleModel);
        return Exchangers.connect(url);
    }

}
