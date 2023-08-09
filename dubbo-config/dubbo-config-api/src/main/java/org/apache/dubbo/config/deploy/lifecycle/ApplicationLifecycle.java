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
package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.deploy.DefaultApplicationDeployer;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ApplicationLifecycle.
 * <br>
 * Used in an application Lifecycle managing procedure, and dubbo packages
 * can implement this interface to define what to do when application status changes.
 * <br>
 * In another word, when methods like
 * {@link DefaultApplicationDeployer#start()},
 * {@link DefaultApplicationDeployer#initialize()},
 * {@link DefaultApplicationDeployer#preDestroy()},
 * {@link DefaultApplicationDeployer#postDestroy()} and
 * {@link DefaultApplicationDeployer#checkState(ModuleModel, DeployState)} etc.
 *  called, all implementations of this interface will also be called.
 */
@SPI
public interface ApplicationLifecycle extends Lifecycle {

    /**
     * Set application deployer.
     *
     * @param defaultApplicationDeployer The ApplicationDeployer that called this ApplicationLifecycle.
     */
    void setApplicationDeployer(DefaultApplicationDeployer defaultApplicationDeployer);

    /**
     * {@link ApplicationDeployer#start()}
     */
    default void start(AtomicBoolean hasPreparedApplicationInstance){}

    /**
     * {@link  ApplicationDeployer#initialize()}
     */
    default void initialize(){};

    /**
     * {@link ApplicationDeployer#preDestroy()}
     */
    default void preDestroy() {}

    /**
     * {@link ApplicationDeployer#postDestroy()}
     */
    default void postDestroy() {}

    /**
     * What to do when a module changed.
     *
     * @param changedModule changed module
     * @param moduleState module state
     */
    default void preModuleChanged(ModuleModel changedModule, DeployState moduleState, AtomicBoolean hasPreparedApplicationInstance){}

    /**
     * What to do after a module changed.
     * @param changedModule changed module
     * @param moduleState module state
     * @param newState new application state
     */
    default void postModuleChanged(ModuleModel changedModule,DeployState moduleState, DeployState newState,DeployState oldState){}

    /**
     * {@link DefaultApplicationDeployer#refreshServiceInstance()}.
     */
    default void refreshServiceInstance(){}

}
