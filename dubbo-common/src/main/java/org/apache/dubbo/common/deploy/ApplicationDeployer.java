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
package org.apache.dubbo.common.deploy;

import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.Future;

/**
 * initialize and start application instance
 */
public interface ApplicationDeployer extends Deployer<ApplicationModel> {

    /**
     * Initialize the component
     */
    void initialize() throws IllegalStateException;

    /**
     * Starts the component.
     * @return
     */
    Future start() throws IllegalStateException;

    /**
     * Stops the component.
     */
    void stop() throws IllegalStateException;

    Future getStartFuture();

    /**
     * Register application instance and start internal services
     */
    void prepareApplicationInstance();

    /**
     * Register application instance and start internal services
     */
    void prepareInternalModule();

    /**
     * Pre-processing before destroy model
     */
    void preDestroy();

    /**
     * Post-processing after destroy model
     */
    void postDestroy();

    /**
     * Indicates that the Application is initialized or not.
     */
    boolean isInitialized();

    ApplicationModel getApplicationModel();

    ReferenceCache getReferenceCache();

    /**
     * Whether start in background, do not await finish
     */
    boolean isBackground();

    /**
     * check all module state and update application state
     */
    void checkState(ModuleModel moduleModel, DeployState moduleState);

    /**
     * module state changed callbacks
     */
    void notifyModuleChanged(ModuleModel moduleModel, DeployState state);
}
