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
package org.apache.dubbo.remoting;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

import java.util.List;

/**
 * Scope model initializer for remoting-api
 */
public class RemotingScopeModelInitializer implements ScopeModelInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RemotingScopeModelInitializer.class);

    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {

    }

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {
        applicationModel.addDestroyListener(m -> {
            try {
                List<ZookeeperTransporter> transporters = FrameworkModel.defaultModel()
                        .getExtensionLoader(ZookeeperTransporter.class).getLoadedExtensionInstances();
                if (transporters.isEmpty()) {
                    return;
                }
                // close unused zookeeper clients.
                String applicationName = applicationModel.tryGetApplicationName();
                for (ZookeeperTransporter zkTransporter : transporters) {
                    try {
                        zkTransporter.close(applicationName);
                    } catch (Exception e) {
                        logger.warn("Error encountered at " + zkTransporter
                                + " close unused zookeeper clients while " + applicationName + " is destroyed: "
                                + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error encountered at closing unused zookeeper clients: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {

    }
}
