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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

import java.util.List;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_DESTROY_ZOOKEEPER;

/**
 * Scope model initializer for remoting-api
 */
public class RemotingScopeModelInitializer implements ScopeModelInitializer {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RemotingScopeModelInitializer.class);

    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {

    }

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {
        applicationModel.addDestroyListener(m -> {
            // destroy zookeeper clients if any
            try {
                List<ZookeeperTransporter> transporters = applicationModel.getExtensionLoader(ZookeeperTransporter.class).getLoadedExtensionInstances();
                for (ZookeeperTransporter zkTransporter : transporters) {
                    zkTransporter.destroy();
                }
            } catch (Exception e) {
                logger.error(TRANSPORT_FAILED_DESTROY_ZOOKEEPER, "", "", "Error encountered while destroying ZookeeperTransporter: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {

    }
}
